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
        
        println("HookRegistry: Found ${hookBeans.size} hook bean(s)")
        
        hookBeans.values.forEach { hook ->
            allHooks.add(hook)
            
            // Extract the entity type from the hook's generic type parameter
            val entityType = extractEntityType(hook)
            if (entityType != null) {
                val entityName = entityType.simpleName
                println("HookRegistry: Registered hook ${hook.javaClass.simpleName} for entity: $entityName")
                hooksByEntityName.getOrPut(entityName) { mutableListOf() }.add(hook)
            } else {
                println("HookRegistry: WARNING - Could not extract entity type from hook ${hook.javaClass.simpleName}")
                println("  Hook class: ${hook.javaClass.name}")
                println("  Superclass: ${hook.javaClass.superclass?.name}")
                println("  Interfaces: ${hook.javaClass.interfaces.joinToString { it.name }}")
            }
        }
        
        println("HookRegistry: Total hooks registered: ${hooksByEntityName.size}")
        hooksByEntityName.forEach { (entityName, hooks) ->
            println("  $entityName: ${hooks.size} hook(s)")
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
        if (byName.isNotEmpty()) {
            println("HookRegistry: Found ${byName.size} hook(s) for collection '${collection.name}' by name")
            return byName
        }
        
        // Try entity name derived from slug (singularize and PascalCase)
        val entityNameFromSlug = slugToClassName(collection.slug)
        val bySlugEntity = getHooksForEntity(entityNameFromSlug)
        if (bySlugEntity.isNotEmpty()) {
            println("HookRegistry: Found ${bySlugEntity.size} hook(s) for collection '${collection.name}' by slug entity name '$entityNameFromSlug'")
            return bySlugEntity
        }
        
        // Try slug directly (for cases where hook is named after slug)
        val bySlug = getHooksForEntity(collection.slug)
        if (bySlug.isNotEmpty()) {
            println("HookRegistry: Found ${bySlug.size} hook(s) for collection '${collection.name}' by slug '${collection.slug}'")
            return bySlug
        }
        
        println("HookRegistry: No hooks found for collection '${collection.name}' (slug: '${collection.slug}')")
        println("  Tried: '${collection.name}', '$entityNameFromSlug', '${collection.slug}'")
        println("  Available entity names: ${hooksByEntityName.keys.joinToString()}")
        return emptyList()
    }
    
    /**
     * Get all discovered hooks
     */
    fun getAllHooks(): List<CollectionHooks<*>> = allHooks.toList()
    
    /**
     * Extract the entity type from a CollectionHooks implementation.
     * Uses reflection to read the generic type parameter, but only once during initialization.
     * Handles both direct interface implementations and abstract base class extensions.
     */
    private fun extractEntityType(hook: CollectionHooks<*>): Class<*>? {
        return try {
            val hookClass = hook.javaClass
            
            // First, try to find the generic type from the superclass (for AbstractCollectionHooks)
            // This is the most common case: class PostHooks : AbstractCollectionHooks<Post>()
            var genericSuperclass = hookClass.genericSuperclass
            if (genericSuperclass is java.lang.reflect.ParameterizedType) {
                val rawType = genericSuperclass.rawType as? Class<*>
                // Check if superclass name contains "CollectionHooks" (AbstractCollectionHooks)
                if (rawType != null && rawType.simpleName.contains("CollectionHooks")) {
                    val typeArgs = genericSuperclass.actualTypeArguments
                    if (typeArgs.isNotEmpty()) {
                        val typeArg = typeArgs[0]
                        val entityType = when (typeArg) {
                            is Class<*> -> typeArg
                            is java.lang.reflect.ParameterizedType -> typeArg.rawType as? Class<*>
                            else -> null
                        }
                        if (entityType != null) return entityType
                    }
                }
            }
            
            // If not found in superclass, try interfaces
            hookClass.genericInterfaces.forEach { genericInterface ->
                if (genericInterface is java.lang.reflect.ParameterizedType) {
                    val rawType = genericInterface.rawType as? Class<*>
                    // Check if this is CollectionHooks interface
                    if (rawType == CollectionHooks::class.java || 
                        (rawType != null && rawType.simpleName == "CollectionHooks" && 
                         rawType.packageName.contains("shapr"))) {
                        val typeArgs = genericInterface.actualTypeArguments
                        if (typeArgs.isNotEmpty()) {
                            val typeArg = typeArgs[0]
                            val entityType = when (typeArg) {
                                is Class<*> -> typeArg
                                is java.lang.reflect.ParameterizedType -> typeArg.rawType as? Class<*>
                                else -> null
                            }
                            if (entityType != null) return entityType
                        }
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            println("HookRegistry: Error extracting entity type from ${hook.javaClass.name}: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
