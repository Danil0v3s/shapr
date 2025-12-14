package br.com.firstsoft.shapr.dsl.query

/**
 * Paginated response matching Payload's PaginatedDocs format.
 */
data class PaginatedDocs<T>(
    val docs: List<T>,
    val totalDocs: Long,
    val limit: Int,
    val totalPages: Int,
    val page: Int?,
    val pagingCounter: Int,
    val hasPrevPage: Boolean,
    val hasNextPage: Boolean,
    val prevPage: Int?,
    val nextPage: Int?
) {
    companion object {
        /**
         * Create an empty paginated result.
         */
        fun <T> empty(limit: Int = 10): PaginatedDocs<T> {
            return PaginatedDocs(
                docs = emptyList(),
                totalDocs = 0,
                limit = limit,
                totalPages = 0,
                page = 1,
                pagingCounter = 1,
                hasPrevPage = false,
                hasNextPage = false,
                prevPage = null,
                nextPage = null
            )
        }
    }
}

