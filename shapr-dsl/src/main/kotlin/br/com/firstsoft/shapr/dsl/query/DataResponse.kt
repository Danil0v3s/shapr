package br.com.firstsoft.shapr.dsl.query

/**
 * Response wrapper for single item endpoints.
 * Format: { "data": ... }
 */
data class DataResponse<T>(
    val data: T
)

