package br.com.firstsoft.shapr.runtime.hooks

import br.com.firstsoft.shapr.dsl.CollectionDefinition
import br.com.firstsoft.shapr.dsl.hooks.*
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
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
    private val hookRegistry: HookRegistry
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
        var args = BeforeOperationArgs(
            collection = collection,
            operation = operation,
            context = context,
            data = data,
            id = id,
            query = query
        )
        
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
        var result = data
        
        // Get auto-discovered hooks from registry
        val discoveredHooks = hookRegistry.getHooksForCollection(collection)
        for (hook in discoveredHooks) {
            val hookResult = runBlocking {
                (hook as CollectionHooks<T>).beforeValidate(
                    BeforeValidateArgs(collection, operation, context, result, originalDoc)
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
        var result = data
        
        // Get auto-discovered hooks from registry
        val discoveredHooks = hookRegistry.getHooksForCollection(collection)
        for (hook in discoveredHooks) {
            result = runBlocking {
                (hook as CollectionHooks<T>).beforeChange(
                    BeforeChangeArgs(collection, operation, context, result, originalDoc)
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
        var result = doc
        
        // Get auto-discovered hooks from registry
        val discoveredHooks = hookRegistry.getHooksForCollection(collection)
        for (hook in discoveredHooks) {
            result = runBlocking {
                (hook as CollectionHooks<T>).afterChange(
                    AfterChangeArgs(collection, operation, context, data, result, previousDoc)
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
        var result = doc
        
        // Get auto-discovered hooks from registry
        val discoveredHooks = hookRegistry.getHooksForCollection(collection)
        for (hook in discoveredHooks) {
            result = runBlocking {
                (hook as CollectionHooks<T>).beforeRead(
                    BeforeReadArgs(collection, context, result, query)
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
        var result = doc
        
        // Get auto-discovered hooks from registry
        val discoveredHooks = hookRegistry.getHooksForCollection(collection)
        for (hook in discoveredHooks) {
            result = runBlocking {
                (hook as CollectionHooks<T>).afterRead(
                    AfterReadArgs(collection, context, result, findMany, query)
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
        val args = BeforeDeleteArgs(collection, context, id)
        
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
        val args = AfterDeleteArgs(collection, context, doc, id)
        
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
