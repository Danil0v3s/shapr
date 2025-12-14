package br.com.firstsoft.shapr.dsl.hooks

import br.com.firstsoft.shapr.dsl.CollectionDefinition

/**
 * Represents the operation type for hooks
 */
enum class HookOperationType {
    CREATE,
    UPDATE,
    DELETE,
    READ,
    READ_DISTINCT,
    COUNT
}

/**
 * Base context interface for hooks.
 * Can be extended with custom context data.
 */
interface HookContext {
    /**
     * Custom data that can be passed through hooks
     */
    val customData: Map<String, Any?>
}

/**
 * Default implementation of HookContext
 */
data class DefaultHookContext(
    override val customData: Map<String, Any?> = emptyMap()
) : HookContext

/**
 * Arguments passed to beforeOperation hooks
 */
data class BeforeOperationArgs<T : Any>(
    val collection: CollectionDefinition,
    val operation: HookOperationType,
    val context: HookContext,
    val data: T? = null,
    val id: Any? = null,
    val query: Map<String, Any?>? = null
)

/**
 * Arguments passed to beforeValidate hooks
 */
data class BeforeValidateArgs<T : Any>(
    val collection: CollectionDefinition,
    val operation: HookOperationType,
    val context: HookContext,
    val data: T?,
    val originalDoc: T? = null
)

/**
 * Arguments passed to beforeChange hooks
 */
data class BeforeChangeArgs<T : Any>(
    val collection: CollectionDefinition,
    val operation: HookOperationType,
    val context: HookContext,
    val data: T,
    val originalDoc: T? = null
)

/**
 * Arguments passed to afterChange hooks
 */
data class AfterChangeArgs<T : Any>(
    val collection: CollectionDefinition,
    val operation: HookOperationType,
    val context: HookContext,
    val data: T,
    val doc: T,
    val previousDoc: T? = null
)

/**
 * Arguments passed to beforeRead hooks
 */
data class BeforeReadArgs<T : Any>(
    val collection: CollectionDefinition,
    val context: HookContext,
    val doc: T,
    val query: Map<String, Any?>? = null
)

/**
 * Arguments passed to afterRead hooks
 */
data class AfterReadArgs<T : Any>(
    val collection: CollectionDefinition,
    val context: HookContext,
    val doc: T,
    val findMany: Boolean = false,
    val query: Map<String, Any?>? = null
)

/**
 * Arguments passed to beforeDelete hooks
 */
data class BeforeDeleteArgs(
    val collection: CollectionDefinition,
    val context: HookContext,
    val id: Any
)

/**
 * Arguments passed to afterDelete hooks
 */
data class AfterDeleteArgs<T : Any>(
    val collection: CollectionDefinition,
    val context: HookContext,
    val doc: T,
    val id: Any
)
