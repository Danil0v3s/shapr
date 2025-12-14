package br.com.firstsoft.shapr.dsl.query

import kotlin.reflect.KClass

/**
 * Interface for querying Shapr collections.
 * This interface is defined in the DSL module to provide type safety
 * and inversion of dependencies for hooks.
 */
interface ShaprQueryService {
    /**
     * Find documents using entity class.
     * The collection slug is automatically resolved from the entity class.
     * 
     * Example:
     * ```kotlin
     * val posts = queryService.find(Post::class) {
     *     where {
     *         field("title") {
     *             equals("My Post")
     *         }
     *     }
     *     limit = 10
     *     page = 1
     * }
     * ```
     * 
     * @param T The entity type (e.g., Post, Category)
     * @param entityClass The entity class (e.g., Post::class)
     * @param block DSL builder for query options (collection is auto-resolved)
     * @return Paginated results matching Payload format
     */
    fun <T : Any> find(entityClass: KClass<T>, block: FindOptionsBuilder.() -> Unit): PaginatedDocs<T>
    
    /**
     * Find documents using DSL builder (legacy method with explicit collection).
     * 
     * Example:
     * ```kotlin
     * queryService.find {
     *     collection = "posts"
     *     where {
     *         field("title") {
     *             equals("My Post")
     *         }
     *     }
     *     limit = 10
     *     page = 1
     * }
     * ```
     * 
     * @param block DSL builder for FindOptions
     * @return Paginated results matching Payload format
     */
    fun find(block: FindOptionsBuilder.() -> Unit): PaginatedDocs<Any>
    
    /**
     * Find documents matching the query options using collection slug.
     * 
     * @param options Query options (collection slug, where, pagination, sorting)
     * @return Paginated results matching Payload format
     */
    fun find(options: FindOptions): PaginatedDocs<Any>
    
    /**
     * Find documents matching the query options.
     * 
     * @param entityClass The entity class to query
     * @param options Query options (where, pagination, sorting)
     * @return Paginated results matching Payload format
     */
    fun <T : Any> find(
        entityClass: KClass<T>,
        options: FindOptions
    ): PaginatedDocs<T>
}
