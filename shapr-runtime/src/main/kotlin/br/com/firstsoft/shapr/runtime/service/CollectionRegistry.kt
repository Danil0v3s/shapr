package br.com.firstsoft.shapr.runtime.service

import br.com.firstsoft.shapr.dsl.CollectionDefinition
import br.com.firstsoft.shapr.dsl.ShaprConfig
import org.springframework.stereotype.Service

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
}

