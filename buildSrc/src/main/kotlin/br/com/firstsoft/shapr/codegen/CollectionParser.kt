package br.com.firstsoft.shapr.codegen

import br.com.firstsoft.shapr.dsl.*

/**
 * Parser for extracting collection definitions from Kotlin DSL source files.
 * 
 * This parser uses bracket-counting to properly handle nested blocks.
 */
object CollectionParser {
    
    fun parse(content: String): ShaprConfig {
        val collections = mutableListOf<CollectionDefinition>()
        
        // Find all collection definitions
        val collectionStarts = Regex("""collection\s*\(\s*"([^"]+)"\s*\)\s*\{""").findAll(content)
        
        for (start in collectionStarts) {
            val name = start.groupValues[1]
            val bodyStart = start.range.last + 1
            val body = extractBalancedBlock(content, bodyStart)
            
            if (body != null) {
                val collection = parseCollection(name, body)
                collections.add(collection)
            }
        }
        
        return ShaprConfig(collections = collections)
    }
    
    /**
     * Extracts content between balanced braces starting from the given position.
     */
    private fun extractBalancedBlock(content: String, startPos: Int): String? {
        var depth = 1
        var pos = startPos
        
        while (pos < content.length && depth > 0) {
            when (content[pos]) {
                '{' -> depth++
                '}' -> depth--
            }
            pos++
        }
        
        return if (depth == 0) {
            content.substring(startPos, pos - 1)
        } else null
    }
    
    private fun parseCollection(name: String, body: String): CollectionDefinition {
        val slug = Regex("""slug\s*=\s*"([^"]+)"""").find(body)?.groupValues?.get(1) 
            ?: pluralize(name).lowercase()
        
        val timestamps = Regex("""timestamps\s*=\s*(true|false)""").find(body)?.groupValues?.get(1)?.toBoolean() 
            ?: true
        
        val access = parseAccess(body)
        val fields = parseFields(body)
        
        return CollectionDefinition(
            name = name,
            slug = slug,
            fields = fields,
            access = access,
            timestamps = timestamps
        )
    }
    
    private fun parseAccess(body: String): AccessControl {
        val accessStart = Regex("""access\s*\{""").find(body) ?: return AccessControl()
        val accessBody = extractBalancedBlock(body, accessStart.range.last + 1) ?: return AccessControl()
        
        return AccessControl(
            create = parseAccessRule(accessBody, "create"),
            read = parseAccessRule(accessBody, "read"),
            update = parseAccessRule(accessBody, "update"),
            delete = parseAccessRule(accessBody, "delete")
        )
    }
    
    private fun parseAccessRule(accessBlock: String, operation: String): AccessRule {
        val pattern = Regex("""$operation\s*=\s*(\w+)\s*\(([^)]*)\)""")
        val match = pattern.find(accessBlock)
        
        return when {
            match == null -> AccessRule.Roles(listOf("admin"))
            match.groupValues[1] == "public" -> AccessRule.Public
            match.groupValues[1] == "authenticated" -> AccessRule.Authenticated
            match.groupValues[1] == "deny" -> AccessRule.Deny
            match.groupValues[1] == "roles" -> {
                val rolesStr = match.groupValues[2]
                val roles = Regex(""""([^"]+)"""").findAll(rolesStr)
                    .map { it.groupValues[1] }
                    .toList()
                AccessRule.Roles(roles.ifEmpty { listOf("admin") })
            }
            else -> AccessRule.Roles(listOf("admin"))
        }
    }
    
    private fun parseFields(body: String): List<FieldDefinition> {
        val fieldsStart = Regex("""fields\s*\{""").find(body) ?: return emptyList()
        val fieldsBody = extractBalancedBlock(body, fieldsStart.range.last + 1) ?: return emptyList()
        
        val fields = mutableListOf<FieldDefinition>()
        
        // Match field definitions with optional configuration blocks
        val fieldPattern = Regex("""(text|textarea|number|checkbox|email|date|relationship)\s*\(\s*"([^"]+)"\s*\)(\s*\{)?""")
        
        for (match in fieldPattern.findAll(fieldsBody)) {
            val type = match.groupValues[1]
            val name = match.groupValues[2]
            val hasBlock = match.groupValues[3].isNotBlank()
            
            val config = if (hasBlock) {
                extractBalancedBlock(fieldsBody, match.range.last + 1) ?: ""
            } else ""
            
            val field = parseField(type, name, config)
            fields.add(field)
        }
        
        return fields
    }
    
    private fun parseField(type: String, name: String, config: String): FieldDefinition {
        val required = config.contains("required = true") || config.contains("required=true")
        val unique = config.contains("unique = true") || config.contains("unique=true")
        val maxLength = Regex("""maxLength\s*=\s*(\d+)""").find(config)?.groupValues?.get(1)?.toInt() ?: 255
        val hasMany = config.contains("hasMany = true") || config.contains("hasMany=true")
        val relationTo = Regex("""relationTo\s*=\s*"([^"]+)"""").find(config)?.groupValues?.get(1) ?: ""
        val integerOnly = config.contains("integerOnly = true") || config.contains("integerOnly=true")
        val defaultNow = config.contains("defaultNow = true") || config.contains("defaultNow=true")
        val defaultValueBool = Regex("""defaultValue\s*=\s*(true|false)""").find(config)?.groupValues?.get(1)?.toBoolean() ?: false
        
        val fieldType: FieldType = when (type) {
            "text" -> FieldType.Text(
                name = name,
                maxLength = maxLength,
                required = required,
                unique = unique
            )
            "textarea" -> FieldType.Textarea(
                name = name,
                required = required
            )
            "number" -> FieldType.Number(
                name = name,
                integerOnly = integerOnly,
                required = required
            )
            "checkbox" -> FieldType.Checkbox(
                name = name,
                defaultValue = defaultValueBool
            )
            "email" -> FieldType.Email(
                name = name,
                required = required,
                unique = unique
            )
            "date" -> FieldType.Date(
                name = name,
                required = required,
                defaultNow = defaultNow
            )
            "relationship" -> FieldType.Relationship(
                name = name,
                relationTo = relationTo,
                hasMany = hasMany,
                required = required
            )
            else -> FieldType.Text(name = name)
        }
        
        return FieldDefinition(
            name = name,
            type = fieldType
        )
    }
}
