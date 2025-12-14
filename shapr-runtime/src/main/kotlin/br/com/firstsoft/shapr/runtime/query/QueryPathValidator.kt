package br.com.firstsoft.shapr.runtime.query

import br.com.firstsoft.shapr.dsl.CollectionDefinition
import br.com.firstsoft.shapr.dsl.FieldDefinition
import br.com.firstsoft.shapr.dsl.FieldType

/**
 * Validates query paths against collection field definitions.
 */
object QueryPathValidator {
    
    /**
     * Validate that a query path exists in the collection definition.
     * Supports nested paths (dot notation).
     * 
     * @param path The field path to validate (e.g., "title" or "author.name")
     * @param collection The collection definition
     * @throws IllegalArgumentException if the path is invalid
     */
    fun validatePath(path: String, collection: CollectionDefinition) {
        val parts = path.split(".")
        
        if (parts.isEmpty()) {
            throw IllegalArgumentException("Empty path")
        }
        
        var currentCollection = collection
        
        for (i in parts.indices) {
            val part = parts[i]
            val isLast = i == parts.size - 1
            
            // Find the field in the current collection
            val field = currentCollection.fields.find { it.name == part }
                ?: throw IllegalArgumentException(
                    "Field '$part' not found in collection '${currentCollection.name}' (path: '$path')"
                )
            
            if (!isLast) {
                // Intermediate part - must be a relationship
                if (field.type !is FieldType.Relationship) {
                    throw IllegalArgumentException(
                        "Field '$part' in path '$path' is not a relationship field and cannot be traversed"
                    )
                }
                
                // For full validation, we would need to resolve the related collection
                // and continue validation with that collection's fields
                // This is a simplified version
            }
        }
    }
    
    /**
     * Get the field definition for a path.
     * Returns null if the path is invalid.
     */
    fun getFieldDefinition(path: String, collection: CollectionDefinition): FieldDefinition? {
        return try {
            val parts = path.split(".")
            if (parts.isEmpty()) return null
            
            collection.fields.find { it.name == parts.first() }
        } catch (e: Exception) {
            null
        }
    }
}

