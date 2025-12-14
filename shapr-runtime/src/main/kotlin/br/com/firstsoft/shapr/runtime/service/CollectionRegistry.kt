package br.com.firstsoft.shapr.runtime.service

import br.com.firstsoft.shapr.dsl.CollectionDefinition
import br.com.firstsoft.shapr.dsl.ShaprConfig
import org.springframework.stereotype.Service

/**
 * Utility function to pluralize a word (matches DSL logic).
 */
private fun pluralize(name: String): String = when {
    name.endsWith("y") && !name.endsWith("ay") && !name.endsWith("ey") &&
            !name.endsWith("oy") && !name.endsWith("uy") -> name.dropLast(1) + "ies"
    name.endsWith("s") || name.endsWith("x") || name.endsWith("z") ||
            name.endsWith("ch") || name.endsWith("sh") -> name + "es"
    else -> name + "s"
}

/**
 * Service to resolve CollectionDefinition by slug.
 * Used for query validation and collection metadata access.
 */
@Service
class CollectionRegistry(
    private val shaprConfig: ShaprConfig
) {
    private val collectionsBySlug: Map<String, CollectionDefinition> by lazy {
        shaprConfig.collections.associateBy { it.slug }
    }
    
    private val collectionsByName: Map<String, CollectionDefinition> by lazy {
        shaprConfig.collections.associateBy { it.name }
    }
    
    /**
     * Get a collection definition by its slug.
     * @param slug The collection slug (e.g., "posts", "categories")
     * @return The CollectionDefinition or null if not found
     */
    fun getBySlug(slug: String): CollectionDefinition? {
        return collectionsBySlug[slug]
    }
    
    /**
     * Get a collection definition by its name.
     * @param name The collection name (e.g., "Post", "Category")
     * @return The CollectionDefinition or null if not found
     */
    fun getByName(name: String): CollectionDefinition? {
        return collectionsByName[name]
    }
    
    /**
     * Get all collection definitions.
     */
    fun getAll(): List<CollectionDefinition> {
        return shaprConfig.collections
    }
    
    /**
     * Check if a collection exists by slug.
     */
    fun exists(slug: String): Boolean {
        return collectionsBySlug.containsKey(slug)
    }
    
    /**
     * Get collection by entity class.
     * Resolves the collection slug from the entity class name.
     * 
     * @param entityClass The entity class (e.g., Post::class)
     * @return The CollectionDefinition or null if not found
     */
    fun getByEntityClass(entityClass: Class<*>): CollectionDefinition? {
        val className = entityClass.simpleName
        
        // Try to find by matching entity class name to collection name
        // Entity class name should match collection name (e.g., "Post" -> "Post" collection)
        val byName = getByName(className)
        if (byName != null) {
            return byName
        }
        
        // Try to find by converting class name to slug
        // Convert "Post" -> "posts", "Category" -> "categories"
        val slug = classNameToSlug(className)
        return getBySlug(slug)
    }
    
    /**
     * Convert entity class name to collection slug.
     * Example: "Post" -> "posts", "Category" -> "categories"
     */
    private fun classNameToSlug(className: String): String {
        // Convert PascalCase to lowercase
        val lowercase = className
            .replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .lowercase()
        
        // Pluralize using the same logic as the DSL
        return pluralize(lowercase)
    }
}

