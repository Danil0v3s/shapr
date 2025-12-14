package br.com.firstsoft.shapr.runtime.hooks

import br.com.firstsoft.shapr.dsl.CollectionDefinition
import br.com.firstsoft.shapr.dsl.hooks.*
import br.com.firstsoft.shapr.dsl.query.ShaprQueryService
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

/**
 * Service responsible for executing collection hooks.
 * This service handles both hook instances (with DI) and function-based hooks.
 * 
 * This is a Spring component that can be injected into controllers and services.
 * It automatically discovers hooks from the HookRegistry.
 */
@Component
class HookExecutor @Autowired constructor(
    private val hookRegistry: HookRegistry,
    @Lazy @Autowired(required = false) private val queryService: ShaprQueryService? = null
) {
    
    /**
     * Execute beforeOperation hooks
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> executeBeforeOperation(
        collection: CollectionDefinition,
        operation: HookOperationType,
        context: HookContext,
        data: T? = null,
        id: Any? = null,
        query: Map<String, Any?>? = null
    ): BeforeOperationArgs<T>? {
        // Create context with queryService
        val contextWithQueryService = if (context.queryService == null && queryService != null) {
            DefaultHookContext(context.customData, queryService)
        } else {
            context
        }
        
        var args = BeforeOperationArgs(
            collection = collection,
            operation = operation,
            context = contextWithQueryService,
            data = data,
            id = id,
            query = query
        )
        
        // Execute DSL-registered hooks first
        val dslHooks = collection.hooks as? CollectionHooksConfig<T>
        dslHooks?.beforeOperation?.forEach { hook ->
            val result = hook(args)
            if (result == null) return null // Operation cancelled
            args = result
        }
        
        // Get auto-discovered hooks from registry
        val discoveredHooks = hookRegistry.getHooksForCollection(collection)
        for (hook in discoveredHooks) {
            val result = runBlocking {
                (hook as CollectionHooks<T>).beforeOperation(args)
            }
            if (result == null) return null // Operation cancelled
            args = result
        }
        
        return args
    }
    
    /**
     * Execute beforeValidate hooks
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> executeBeforeValidate(
        collection: CollectionDefinition,
        operation: HookOperationType,
        context: HookContext,
        data: T?,
        originalDoc: T? = null
    ): T? {
        // Create context with queryService
        val contextWithQueryService = if (context.queryService == null && queryService != null) {
            DefaultHookContext(context.customData, queryService)
        } else {
            context
        }
        
        var result = data
        
        // Execute DSL-registered hooks first
        val dslHooks = collection.hooks as? CollectionHooksConfig<T>
        dslHooks?.beforeValidate?.forEach { hook ->
            val hookResult = hook(BeforeValidateArgs(collection, operation, contextWithQueryService, result, originalDoc))
            if (hookResult != null) {
                result = hookResult
            }
        }
        
        // Get auto-discovered hooks from registry
        val discoveredHooks = hookRegistry.getHooksForCollection(collection)
        for (hook in discoveredHooks) {
            val hookResult = runBlocking {
                (hook as CollectionHooks<T>).beforeValidate(
                    BeforeValidateArgs(collection, operation, contextWithQueryService, result, originalDoc)
                )
            }
            if (hookResult != null) {
                result = hookResult
            }
        }
        
        return result
    }
    
    /**
     * Execute beforeChange hooks
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> executeBeforeChange(
        collection: CollectionDefinition,
        operation: HookOperationType,
        context: HookContext,
        data: T,
        originalDoc: T? = null
    ): T {
        // Create context with queryService
        val contextWithQueryService = if (context.queryService == null && queryService != null) {
            DefaultHookContext(context.customData, queryService)
        } else {
            context
        }
        
        var result = data
        
        // Execute DSL-registered hooks first
        val dslHooks = collection.hooks as? CollectionHooksConfig<T>
        dslHooks?.beforeChange?.forEach { hook ->
            result = hook(BeforeChangeArgs(collection, operation, contextWithQueryService, result, originalDoc))
        }
        
        // Get auto-discovered hooks from registry
        val discoveredHooks = hookRegistry.getHooksForCollection(collection)
        for (hook in discoveredHooks) {
            result = runBlocking {
                (hook as CollectionHooks<T>).beforeChange(
                    BeforeChangeArgs(collection, operation, contextWithQueryService, result, originalDoc)
                )
            }
        }
        
        return result
    }
    
    /**
     * Execute afterChange hooks
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> executeAfterChange(
        collection: CollectionDefinition,
        operation: HookOperationType,
        context: HookContext,
        data: T,
        doc: T,
        previousDoc: T? = null
    ): T {
        // Create context with queryService
        val contextWithQueryService = if (context.queryService == null && queryService != null) {
            DefaultHookContext(context.customData, queryService)
        } else {
            context
        }
        
        var result = doc
        
        // Execute DSL-registered hooks first
        val dslHooks = collection.hooks as? CollectionHooksConfig<T>
        dslHooks?.afterChange?.forEach { hook ->
            result = hook(AfterChangeArgs(collection, operation, contextWithQueryService, data, result, previousDoc))
        }
        
        // Get auto-discovered hooks from registry
        val discoveredHooks = hookRegistry.getHooksForCollection(collection)
        for (hook in discoveredHooks) {
            result = runBlocking {
                (hook as CollectionHooks<T>).afterChange(
                    AfterChangeArgs(collection, operation, contextWithQueryService, data, result, previousDoc)
                )
            }
        }
        
        return result
    }
    
    /**
     * Execute beforeRead hooks
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> executeBeforeRead(
        collection: CollectionDefinition,
        context: HookContext,
        doc: T,
        query: Map<String, Any?>? = null
    ): T {
        // Create context with queryService
        val contextWithQueryService = if (context.queryService == null && queryService != null) {
            DefaultHookContext(context.customData, queryService)
        } else {
            context
        }
        
        var result = doc
        
        // Execute DSL-registered hooks first
        val dslHooks = collection.hooks as? CollectionHooksConfig<T>
        dslHooks?.beforeRead?.forEach { hook ->
            result = hook(BeforeReadArgs(collection, contextWithQueryService, result, query))
        }
        
        // Get auto-discovered hooks from registry
        val discoveredHooks = hookRegistry.getHooksForCollection(collection)
        for (hook in discoveredHooks) {
            result = runBlocking {
                (hook as CollectionHooks<T>).beforeRead(
                    BeforeReadArgs(collection, contextWithQueryService, result, query)
                )
            }
        }
        
        return result
    }
    
    /**
     * Execute afterRead hooks
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> executeAfterRead(
        collection: CollectionDefinition,
        context: HookContext,
        doc: T,
        findMany: Boolean = false,
        query: Map<String, Any?>? = null
    ): T {
        // Create context with queryService
        val contextWithQueryService = if (context.queryService == null && queryService != null) {
            DefaultHookContext(context.customData, queryService)
        } else {
            context
        }
        
        var result = doc
        
        // Execute DSL-registered hooks first
        val dslHooks = collection.hooks as? CollectionHooksConfig<T>
        dslHooks?.afterRead?.forEach { hook ->
            result = hook(AfterReadArgs(collection, contextWithQueryService, result, findMany, query))
        }
        
        // Get auto-discovered hooks from registry
        val discoveredHooks = hookRegistry.getHooksForCollection(collection)
        for (hook in discoveredHooks) {
            result = runBlocking {
                (hook as CollectionHooks<T>).afterRead(
                    AfterReadArgs(collection, contextWithQueryService, result, findMany, query)
                )
            }
        }
        
        return result
    }
    
    /**
     * Execute beforeDelete hooks
     */
    @Suppress("UNCHECKED_CAST")
    fun executeBeforeDelete(
        collection: CollectionDefinition,
        context: HookContext,
        id: Any
    ) {
        // Create context with queryService
        val contextWithQueryService = if (context.queryService == null && queryService != null) {
            DefaultHookContext(context.customData, queryService)
        } else {
            context
        }
        
        val args = BeforeDeleteArgs(collection, contextWithQueryService, id)
        
        // Execute DSL-registered hooks first
        val dslHooks = collection.hooks
        if (dslHooks != null) {
            @Suppress("UNCHECKED_CAST")
            val typedHooks = dslHooks as? CollectionHooksConfig<*>
            typedHooks?.beforeDelete?.forEach { hook ->
                hook(args)
            }
        }
        
        // Get auto-discovered hooks from registry
        val discoveredHooks = hookRegistry.getHooksForCollection(collection)
        for (hook in discoveredHooks) {
            runBlocking {
                try {
                    // Call beforeDelete - the generic type is already matched by the registry
                    val method = hook::class.java.getMethod("beforeDelete", BeforeDeleteArgs::class.java)
                    method.invoke(hook, args)
                } catch (e: Exception) {
                    // Hook doesn't implement beforeDelete or error occurred - ignore
                }
            }
        }
    }
    
    /**
     * Execute afterDelete hooks
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> executeAfterDelete(
        collection: CollectionDefinition,
        context: HookContext,
        doc: T,
        id: Any
    ) {
        // Create context with queryService
        val contextWithQueryService = if (context.queryService == null && queryService != null) {
            DefaultHookContext(context.customData, queryService)
        } else {
            context
        }
        
        val args = AfterDeleteArgs(collection, contextWithQueryService, doc, id)
        
        // Execute DSL-registered hooks first
        val dslHooks = collection.hooks as? CollectionHooksConfig<T>
        dslHooks?.afterDelete?.forEach { hook ->
            hook(args)
        }
        
        // Get auto-discovered hooks from registry
        val discoveredHooks = hookRegistry.getHooksForCollection(collection)
        for (hook in discoveredHooks) {
            runBlocking {
                try {
                    // Call afterDelete - the generic type is already matched by the registry
                    val method = hook::class.java.getMethod("afterDelete", AfterDeleteArgs::class.java)
                    method.invoke(hook, args)
                } catch (e: Exception) {
                    // Hook doesn't implement afterDelete or error occurred - ignore
                }
            }
        }
    }
}
