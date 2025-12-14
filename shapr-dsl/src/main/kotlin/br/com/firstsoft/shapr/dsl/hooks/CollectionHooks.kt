package br.com.firstsoft.shapr.dsl.hooks

import br.com.firstsoft.shapr.dsl.CollectionDefinition

/**
 * Base interface for collection hooks with generics.
 * Implement this interface to create custom hooks for a specific entity type.
 * 
 * Example:
 * ```kotlin
 * @Component
 * class PostHooks : CollectionHooks<Post> {
 *     override fun beforeChange(args: BeforeChangeArgs<Post>): Post {
 *         // Modify data before saving
 *         return args.data.copy(title = args.data.title.uppercase())
 *     }
 * }
 * ```
 */
interface CollectionHooks<T : Any> {
    /**
     * Called before any operation is executed.
     * Can modify the operation arguments or cancel the operation.
     * 
     * @return Modified args or null to cancel operation
     */
    suspend fun beforeOperation(args: BeforeOperationArgs<T>): BeforeOperationArgs<T>? {
        return args
    }

    /**
     * Called before validation occurs (for create/update operations).
     * Can modify the data before validation.
     * 
     * @return Modified data or null to skip validation
     */
    suspend fun beforeValidate(args: BeforeValidateArgs<T>): T? {
        return args.data
    }

    /**
     * Called after validation but before the change is persisted.
     * This is the last chance to modify data before it's saved.
     * 
     * @return Modified data to be saved
     */
    suspend fun beforeChange(args: BeforeChangeArgs<T>): T {
        return args.data
    }

    /**
     * Called after a document has been changed (created or updated).
     * The change has already been persisted to the database.
     * 
     * @return Modified document (optional, can return the same doc)
     */
    suspend fun afterChange(args: AfterChangeArgs<T>): T {
        return args.doc
    }

    /**
     * Called before a document is read from the database.
     * Can modify the document before it's returned.
     * 
     * @return Modified document
     */
    suspend fun beforeRead(args: BeforeReadArgs<T>): T {
        return args.doc
    }

    /**
     * Called after a document has been read from the database.
     * Can modify the document before it's returned to the caller.
     * 
     * @return Modified document
     */
    suspend fun afterRead(args: AfterReadArgs<T>): T {
        return args.doc
    }

    /**
     * Called before a document is deleted.
     * Can cancel the deletion by throwing an exception.
     */
    suspend fun beforeDelete(args: BeforeDeleteArgs) {
        // Default: allow deletion
    }

    /**
     * Called after a document has been deleted.
     * The document has already been removed from the database.
     */
    suspend fun afterDelete(args: AfterDeleteArgs<T>) {
        // Default: no action
    }
}

/**
 * Abstract base class for collection hooks.
 * Provides default implementations for all hooks (no-op).
 * Override only the hooks you need.
 * 
 * Example:
 * ```kotlin
 * @Component
 * class PostHooks : AbstractCollectionHooks<Post>() {
 *     override suspend fun beforeChange(args: BeforeChangeArgs<Post>): Post {
 *         return args.data.copy(updatedAt = Instant.now())
 *     }
 * }
 * ```
 */
abstract class AbstractCollectionHooks<T : Any> : CollectionHooks<T> {
    override suspend fun beforeOperation(args: BeforeOperationArgs<T>): BeforeOperationArgs<T>? = args
    override suspend fun beforeValidate(args: BeforeValidateArgs<T>): T? = args.data
    override suspend fun beforeChange(args: BeforeChangeArgs<T>): T = args.data
    override suspend fun afterChange(args: AfterChangeArgs<T>): T = args.doc
    override suspend fun beforeRead(args: BeforeReadArgs<T>): T = args.doc
    override suspend fun afterRead(args: AfterReadArgs<T>): T = args.doc
    override suspend fun beforeDelete(args: BeforeDeleteArgs) {}
    override suspend fun afterDelete(args: AfterDeleteArgs<T>) {}
}
