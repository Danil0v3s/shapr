package br.com.firstsoft.shapr.dsl.hooks

import br.com.firstsoft.shapr.dsl.CollectionDefinition

/**
 * Configuration for collection hooks.
 * Hooks can be provided as instances (for dependency injection) or as function references.
 */
data class CollectionHooksConfig<T : Any>(
    /**
     * Hook instances (typically Spring beans with @Component annotation)
     * These will be injected via dependency injection.
     */
    val hookInstances: List<CollectionHooks<T>> = emptyList(),
    
    /**
     * Function-based hooks (for simple use cases without DI)
     */
    val beforeOperation: List<(BeforeOperationArgs<T>) -> BeforeOperationArgs<T>?> = emptyList(),
    val beforeValidate: List<(BeforeValidateArgs<T>) -> T?> = emptyList(),
    val beforeChange: List<(BeforeChangeArgs<T>) -> T> = emptyList(),
    val afterChange: List<(AfterChangeArgs<T>) -> T> = emptyList(),
    val beforeRead: List<(BeforeReadArgs<T>) -> T> = emptyList(),
    val afterRead: List<(AfterReadArgs<T>) -> T> = emptyList(),
    val beforeDelete: List<(BeforeDeleteArgs) -> Unit> = emptyList(),
    val afterDelete: List<(AfterDeleteArgs<T>) -> Unit> = emptyList()
)

/**
 * Builder for collection hooks configuration
 */
class CollectionHooksBuilder<T : Any> {
    private val hookInstances = mutableListOf<CollectionHooks<T>>()
    private val beforeOperationHooks = mutableListOf<(BeforeOperationArgs<T>) -> BeforeOperationArgs<T>?>()
    private val beforeValidateHooks = mutableListOf<(BeforeValidateArgs<T>) -> T?>()
    private val beforeChangeHooks = mutableListOf<(BeforeChangeArgs<T>) -> T>()
    private val afterChangeHooks = mutableListOf<(AfterChangeArgs<T>) -> T>()
    private val beforeReadHooks = mutableListOf<(BeforeReadArgs<T>) -> T>()
    private val afterReadHooks = mutableListOf<(AfterReadArgs<T>) -> T>()
    private val beforeDeleteHooks = mutableListOf<(BeforeDeleteArgs) -> Unit>()
    private val afterDeleteHooks = mutableListOf<(AfterDeleteArgs<T>) -> Unit>()

    /**
     * Add a hook instance (for dependency injection)
     */
    fun hook(instance: CollectionHooks<T>) {
        hookInstances.add(instance)
    }

    /**
     * Add a beforeOperation hook function
     */
    fun beforeOperation(hook: (BeforeOperationArgs<T>) -> BeforeOperationArgs<T>?) {
        beforeOperationHooks.add(hook)
    }
    
    /**
     * Add multiple beforeOperation hook functions
     */
    fun beforeOperation(hooks: Array<(BeforeOperationArgs<T>) -> BeforeOperationArgs<T>?>) {
        beforeOperationHooks.addAll(hooks)
    }

    /**
     * Add a beforeValidate hook function
     */
    fun beforeValidate(hook: (BeforeValidateArgs<T>) -> T?) {
        beforeValidateHooks.add(hook)
    }
    
    /**
     * Add multiple beforeValidate hook functions
     */
    fun beforeValidate(hooks: Array<(BeforeValidateArgs<T>) -> T?>) {
        beforeValidateHooks.addAll(hooks)
    }

    /**
     * Add a beforeChange hook function
     */
    fun beforeChange(hook: (BeforeChangeArgs<T>) -> T) {
        beforeChangeHooks.add(hook)
    }
    
    /**
     * Add multiple beforeChange hook functions
     */
    fun beforeChange(hooks: Array<(BeforeChangeArgs<T>) -> T>) {
        beforeChangeHooks.addAll(hooks)
    }

    /**
     * Add an afterChange hook function
     */
    fun afterChange(hook: (AfterChangeArgs<T>) -> T) {
        afterChangeHooks.add(hook)
    }
    
    /**
     * Add multiple afterChange hook functions
     */
    fun afterChange(hooks: Array<(AfterChangeArgs<T>) -> T>) {
        afterChangeHooks.addAll(hooks)
    }

    /**
     * Add a beforeRead hook function
     */
    fun beforeRead(hook: (BeforeReadArgs<T>) -> T) {
        beforeReadHooks.add(hook)
    }
    
    /**
     * Add multiple beforeRead hook functions
     */
    fun beforeRead(hooks: Array<(BeforeReadArgs<T>) -> T>) {
        beforeReadHooks.addAll(hooks)
    }

    /**
     * Add an afterRead hook function
     */
    fun afterRead(hook: (AfterReadArgs<T>) -> T) {
        afterReadHooks.add(hook)
    }
    
    /**
     * Add multiple afterRead hook functions
     */
    fun afterRead(hooks: Array<(AfterReadArgs<T>) -> T>) {
        afterReadHooks.addAll(hooks)
    }

    /**
     * Add a beforeDelete hook function
     */
    fun beforeDelete(hook: (BeforeDeleteArgs) -> Unit) {
        beforeDeleteHooks.add(hook)
    }
    
    /**
     * Add multiple beforeDelete hook functions
     */
    fun beforeDelete(hooks: Array<(BeforeDeleteArgs) -> Unit>) {
        beforeDeleteHooks.addAll(hooks)
    }

    /**
     * Add an afterDelete hook function
     */
    fun afterDelete(hook: (AfterDeleteArgs<T>) -> Unit) {
        afterDeleteHooks.add(hook)
    }
    
    /**
     * Add multiple afterDelete hook functions
     */
    fun afterDelete(hooks: Array<(AfterDeleteArgs<T>) -> Unit>) {
        afterDeleteHooks.addAll(hooks)
    }

    internal fun build(): CollectionHooksConfig<T> = CollectionHooksConfig(
        hookInstances = hookInstances.toList(),
        beforeOperation = beforeOperationHooks.toList(),
        beforeValidate = beforeValidateHooks.toList(),
        beforeChange = beforeChangeHooks.toList(),
        afterChange = afterChangeHooks.toList(),
        beforeRead = beforeReadHooks.toList(),
        afterRead = afterReadHooks.toList(),
        beforeDelete = beforeDeleteHooks.toList(),
        afterDelete = afterDeleteHooks.toList()
    )
}
