package br.com.firstsoft.shapr.runtime.query

import br.com.firstsoft.shapr.dsl.CollectionDefinition
import br.com.firstsoft.shapr.dsl.FieldType
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.From
import jakarta.persistence.criteria.Path

/**
 * Resolves nested properties (dot notation) in query paths.
 * Handles relationship traversal using JPA joins.
 */
object NestedPropertyResolver {
    
    /**
     * Resolve a path (potentially nested) to a JPA Path expression.
     * Supports dot notation like "author.name" for relationship fields.
     * 
     * @param root The root entity path
     * @param path The field path (e.g., "title" or "author.name")
     * @param collection The collection definition for field validation
     * @param cb CriteriaBuilder for creating joins
     * @return The resolved Path expression
     */
    fun <T> resolvePath(
        root: From<T, T>,
        path: String,
        collection: CollectionDefinition,
        cb: CriteriaBuilder
    ): Path<*> {
        val parts = path.split(".")
        
        if (parts.size == 1) {
            // Simple field - no nesting
            return root.get<Any>(path)
        }
        
        // Nested path - traverse relationships
        var currentPath: Path<*> = root
        var currentCollection = collection
        
        for (i in parts.indices) {
            val part = parts[i]
            val isLast = i == parts.size - 1
            
            // Find the field in the current collection
            val field = currentCollection.fields.find { it.name == part }
                ?: throw IllegalArgumentException("Field '$part' not found in collection '${currentCollection.name}'")
            
            if (isLast) {
                // Last part - return the path
                return currentPath.get<Any>(part)
            } else {
                // Intermediate part - must be a relationship
                if (field.type !is FieldType.Relationship) {
                    throw IllegalArgumentException("Field '$part' in path '$path' is not a relationship field")
                }
                
                val relationship = field.type as FieldType.Relationship
                // For now, we'll use a join
                // In a full implementation, we'd need to resolve the related collection
                // and create proper joins
                currentPath = currentPath.get<Any>(part)
                
                // Note: Full relationship resolution would require:
                // 1. Resolving the related collection by relationship.relationTo
                // 2. Creating proper JPA joins
                // 3. Continuing traversal with the related collection's fields
                // This is a simplified version - full implementation would need CollectionRegistry
            }
        }
        
        return currentPath
    }
    
    /**
     * Check if a path is nested (contains dots).
     */
    fun isNested(path: String): Boolean {
        return path.contains(".")
    }
    
    /**
     * Get the root field name from a nested path.
     * E.g., "author.name" -> "author"
     */
    fun getRootField(path: String): String {
        return path.split(".").first()
    }
}

