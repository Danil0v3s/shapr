package br.com.firstsoft.shapr.dsl.query

/**
 * Options for find operations, matching Payload's find API.
 */
data class FindOptions(
    val collection: String,
    val where: Where? = null,
    val limit: Int? = null,
    val page: Int? = null,
    val sort: String? = null,
    val pagination: Boolean = true
)

