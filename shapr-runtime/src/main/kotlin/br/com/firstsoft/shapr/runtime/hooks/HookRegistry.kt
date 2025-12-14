package br.com.firstsoft.shapr.runtime.hooks

import br.com.firstsoft.shapr.dsl.CollectionDefinition
import br.com.firstsoft.shapr.dsl.hooks.CollectionHooks
import br.com.firstsoft.shapr.dsl.slugToClassName
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

/**
 * Registry that auto-discovers and manages collection hooks.
 * Automatically finds all Spring beans that implement CollectionHooks interface.
 */
@Component
class HookRegistry @Autowired constructor(
    private val applicationContext: ApplicationContext
) {
    /**
     * Map of entity class name to hook instances.
     * The key is the simple class name of the entity type.
     */
    private val hooksByEntityName = mutableMapOf<String, MutableList<CollectionHooks<*>>>()
    
    /**
     * All discovered hook instances
     */
    private val allHooks = mutableListOf<CollectionHooks<*>>()
    
    @PostConstruct
    fun discoverHooks() {
        // Find all beans that implement CollectionHooks interface
        val hookBeans = applicationContext.getBeansOfType(CollectionHooks::class.java)
        
        hookBeans.values.forEach { hook ->
            allHooks.add(hook)
            
            // Extract the entity type from the hook's generic type parameter
            val entityType = extractEntityType(hook)
            if (entityType != null) {
                val entityName = entityType.simpleName
                hooksByEntityName.getOrPut(entityName) { mutableListOf() }.add(hook)
            }
        }
    }
    
    /**
     * Get hooks for a specific entity type by class name
     */
    fun getHooksForEntity(entityName: String): List<CollectionHooks<*>> {
        return hooksByEntityName[entityName]?.toList() ?: emptyList()
    }
    
    /**
     * Get hooks for a collection by matching the collection name or slug to entity name.
     * Tries multiple matching strategies:
     * 1. Collection name (e.g., "Post")
     * 2. Entity name derived from slug (e.g., "posts" -> "Post")
     * 3. Slug directly (e.g., "posts")
     */
    fun getHooksForCollection(collection: CollectionDefinition): List<CollectionHooks<*>> {
        // Try collection name first (most common case)
        val byName = getHooksForEntity(collection.name)
        if (byName.isNotEmpty()) return byName
        
        // Try entity name derived from slug (singularize and PascalCase)
        val entityNameFromSlug = slugToClassName(collection.slug)
        val bySlugEntity = getHooksForEntity(entityNameFromSlug)
        if (bySlugEntity.isNotEmpty()) return bySlugEntity
        
        // Try slug directly (for cases where hook is named after slug)
        return getHooksForEntity(collection.slug)
    }
    
    /**
     * Get all discovered hooks
     */
    fun getAllHooks(): List<CollectionHooks<*>> = allHooks.toList()
    
    /**
     * Extract the entity type from a CollectionHooks implementation.
     * Uses reflection to read the generic type parameter, but only once during initialization.
     */
    private fun extractEntityType(hook: CollectionHooks<*>): Class<*>? {
        return try {
            // Get the generic interface type
            val hookInterface = hook.javaClass.genericInterfaces
                .firstOrNull { it is java.lang.reflect.ParameterizedType && 
                              (it as java.lang.reflect.ParameterizedType).rawType == CollectionHooks::class.java }
                as? java.lang.reflect.ParameterizedType
            
            // Extract the type argument (the entity type)
            val typeArgs = hookInterface?.actualTypeArguments
            if (typeArgs != null && typeArgs.isNotEmpty()) {
                val entityType = typeArgs[0]
                when (entityType) {
                    is Class<*> -> entityType
                    is java.lang.reflect.ParameterizedType -> entityType.rawType as? Class<*>
                    else -> null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
