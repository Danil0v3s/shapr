package br.com.firstsoft.shapr.codegen

import br.com.firstsoft.shapr.dsl.*

/**
 * Parser for extracting collection definitions from annotated Kotlin classes.
 * Parses @ShaprCol and @Block annotated classes and their field properties.
 */
object ClassParser {

    fun parse(content: String): ShaprConfig {
        // First, parse all @Block definitions so we can reference them
        val blocks = parseBlocks(content)

        val collections = mutableListOf<CollectionDefinition>()

        // Find all @ShaprCol annotations and their classes
        val collectionPattern = Regex(
            """@ShaprCol\s*\(\s*name\s*=\s*"([^"]+)"(?:,\s*slug\s*=\s*"([^"]+)")?\s*(?:,[^)]*)*\)\s*class\s+(\w+)\s*(?::\s*ShaprCollection\s*\(\s*\))?\s*\{"""
        )

        for (match in collectionPattern.findAll(content)) {
            val name = match.groupValues[1]
            val slug = match.groupValues[2].ifEmpty { pluralize(name).lowercase() }
            val className = match.groupValues[3]

            val bodyStart = match.range.last + 1
            val body = extractBalancedBlock(content, bodyStart)

            if (body != null) {
                val fields = parseClassFields(body, blocks)
                collections.add(CollectionDefinition(name = name, slug = slug, fields = fields, timestamps = true))
            }
        }

        return ShaprConfig(collections = collections)
    }

    /**
     * Parse all @Block annotated classes and return a map of className -> BlockConfig
     */
    private fun parseBlocks(content: String): Map<String, FieldType.BlockConfig> {
        val blocks = mutableMapOf<String, FieldType.BlockConfig>()

        val blockPattern = Regex(
            """@Block\s*\(\s*slug\s*=\s*"([^"]+)"\s*(?:,[^)]*)*\)\s*class\s+(\w+)\s*(?::\s*ShaprBlock\s*\(\s*\))?\s*\{"""
        )

        for (match in blockPattern.findAll(content)) {
            val slug = match.groupValues[1]
            val className = match.groupValues[2]

            val bodyStart = match.range.last + 1
            val body = extractBalancedBlock(content, bodyStart)

            if (body != null) {
                val fields = parseClassFields(body, emptyMap()) // Blocks don't have nested blocks
                blocks[className] = FieldType.BlockConfig(slug = slug, fields = fields)
            }
        }

        return blocks
    }
    
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
        return if (depth == 0) content.substring(startPos, pos - 1) else null
    }
    
    private fun parseClassFields(body: String, blocks: Map<String, FieldType.BlockConfig>): List<FieldDefinition> {
        val fields = mutableListOf<FieldDefinition>()

        // Match: optional annotations + var fieldName: Type = defaultValue
        val propertyPattern = Regex(
            """((?:@\w+(?:\([^)]*\))?\s*)*)var\s+(\w+)\s*:\s*(\w+)(\?)?(?:\s*=\s*[^@\n]+)?"""
        )

        for (match in propertyPattern.findAll(body)) {
            val annotations = match.groupValues[1]
            val fieldName = match.groupValues[2]
            val typeName = match.groupValues[3]
            val nullable = match.groupValues[4] == "?"
            val required = !nullable

            val fieldType = determineFieldType(fieldName, typeName, annotations, required, blocks)
            if (fieldType != null) {
                fields.add(FieldDefinition(name = fieldName, type = fieldType))
            }
        }

        return fields
    }
    
    private fun determineFieldType(
        name: String, typeName: String, annotations: String, required: Boolean,
        blocks: Map<String, FieldType.BlockConfig>
    ): FieldType? {
        val unique = annotations.contains("@Unique")
        val localized = annotations.contains("@Localized")
        val indexed = annotations.contains("@Indexed")
        val hasMany = annotations.contains("@HasMany")
        val maxLen = Regex("""@MaxLength\s*\(\s*(\d+)\s*\)""").find(annotations)?.groupValues?.get(1)?.toInt() ?: 255

        // Check for @BlocksOf - e.g., @BlocksOf(HeroBlock::class, ContentBlock::class)
        Regex("""@BlocksOf\s*\(([^)]+)\)""").find(annotations)?.let { match ->
            val blockClassNames = Regex("""(\w+)::class""").findAll(match.groupValues[1])
                .map { it.groupValues[1] }.toList()
            val blockConfigs = blockClassNames.mapNotNull { blocks[it] }
            if (blockConfigs.isNotEmpty()) {
                return FieldType.Blocks(name = name, blocks = blockConfigs, localized = localized)
            }
        }

        // Check for relationship - supports multiple targets: @Relationship("posts", "pages") or @Relationship("categories")
        Regex("""@Relationship\s*\(([^)]+)\)""").find(annotations)?.let { match ->
            val targets = Regex(""""([^"]+)"""").findAll(match.groupValues[1]).map { it.groupValues[1] }.toList()
            if (targets.isNotEmpty()) {
                return FieldType.Relationship(name, targets, hasMany, required, localized)
            }
        }

        // Check for upload - supports multiple targets
        Regex("""@Upload\s*\(([^)]+)\)""").find(annotations)?.let { match ->
            val targets = Regex(""""([^"]+)"""").findAll(match.groupValues[1]).map { it.groupValues[1] }.toList()
            if (targets.isNotEmpty()) {
                return FieldType.Upload(name, targets, hasMany, required, localized)
            }
        }

        // Check for type overrides
        if (annotations.contains("@Textarea")) return FieldType.Textarea(name, required, localized)
        if (annotations.contains("@RichText")) return FieldType.RichText(name, required, localized)
        if (annotations.contains("@Json")) return FieldType.Json(name, required, localized)
        if (annotations.contains("@Email")) return FieldType.Email(name, required, unique, localized, indexed)
        if (annotations.contains("@Point")) return FieldType.Point(name, required, localized)
        if (annotations.contains("@Code")) return FieldType.Code(name, required, localized, "")

        // Check for select/radio with options
        Regex("""@Options\s*\(\s*([^)]+)\s*\)""").find(annotations)?.let { opts ->
            val options = Regex(""""([^"]+)"""").findAll(opts.groupValues[1])
                .map { FieldType.SelectOption.StringOption(it.groupValues[1]) }.toList()
            return if (annotations.contains("@Radio")) FieldType.Radio(name, options, required, localized)
            else FieldType.Select(name, options, required, localized, indexed, hasMany)
        }

        // Determine by Kotlin type
        return when (typeName) {
            "String" -> FieldType.Text(name, maxLen, 0, required, unique, localized, indexed, hasMany, null)
            "Int", "Long" -> FieldType.Number(name, true, null, null, required, localized, indexed, hasMany)
            "Double", "Float" -> FieldType.Number(name, false, null, null, required, localized, indexed, hasMany)
            "Boolean" -> {
                val defaultVal = Regex("""@Default\s*\(\s*boolValue\s*=\s*(true|false)\s*\)""")
                    .find(annotations)?.groupValues?.get(1)?.toBoolean() ?: false
                FieldType.Checkbox(name, localized, defaultVal)
            }
            "Instant", "LocalDateTime", "LocalDate" -> FieldType.Date(name, required, localized)
            "List" -> null // List types are handled by @BlocksOf, @ArrayOf etc.
            else -> null
        }
    }
}

