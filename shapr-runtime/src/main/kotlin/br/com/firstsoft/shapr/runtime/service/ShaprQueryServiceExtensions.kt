package br.com.firstsoft.shapr.runtime.service

import br.com.firstsoft.shapr.dsl.query.FindOptionsBuilder
import br.com.firstsoft.shapr.dsl.query.PaginatedDocs

/**
 * Extension function to enable generic type inference for find operations.
 * 
 * Usage:
 * ```kotlin
 * val posts = queryService.find<Post> {
 *     where {
 *         field("title") {
 *             equals("My Post")
 *         }
 *     }
 * }
 * ```
 */
inline fun <reified T : Any> ShaprQueryService.find(noinline block: FindOptionsBuilder.() -> Unit): PaginatedDocs<T> {
    return find(T::class, block)
}

