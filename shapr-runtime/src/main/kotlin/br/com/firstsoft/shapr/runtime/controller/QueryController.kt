package br.com.firstsoft.shapr.runtime.controller

import br.com.firstsoft.shapr.dsl.query.FindOptions
import br.com.firstsoft.shapr.dsl.query.Where
import br.com.firstsoft.shapr.runtime.auth.AuthUtil
import br.com.firstsoft.shapr.runtime.service.CollectionRegistry
import br.com.firstsoft.shapr.runtime.service.ShaprQueryService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for Payload-style query operations.
 * Provides GET endpoint for querying collections with where clauses, pagination, and sorting.
 */
@RestController
@RequestMapping("/api")
class QueryController(
    private val queryService: ShaprQueryService,
    private val collectionRegistry: CollectionRegistry,
    private val objectMapper: ObjectMapper
) {
    
    /**
     * Query a collection with Payload-style query API.
     * 
     * GET /api/{collection}?where={json}&limit=10&page=1&sort=-createdAt
     * 
     * @param collection The collection slug (e.g., "posts", "categories")
     * @param where JSON string of Where query (optional)
     * @param limit Number of results per page (default: 10)
     * @param page Page number (default: 1)
     * @param sort Sort string (e.g., "-createdAt" for descending, "title" for ascending)
     * @param pagination Whether to paginate (default: true)
     * @return Paginated results matching Payload format
     */
    @GetMapping("/{collection}")
    fun find(
        @PathVariable collection: String,
        @RequestParam(required = false) where: String?,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false, defaultValue = "true") pagination: Boolean
    ): ResponseEntity<*> {
        // Check collection exists
        val collectionDef = collectionRegistry.getBySlug(collection)
            ?: return ResponseEntity.notFound().build<Any>()
        
        // Check access control
        AuthUtil.checkAccess(collectionDef.access.read)
        
        // Parse where clause
        val whereClause = try {
            if (where != null && where.isNotBlank()) {
                objectMapper.readValue(where, Where::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "Invalid where clause: ${e.message}"))
        }
        
        // Build query options
        val options = FindOptions(
            collection = collection,
            where = whereClause,
            limit = limit,
            page = page,
            sort = sort,
            pagination = pagination
        )
        
        // Execute query
        return try {
            val result = queryService.find(options)
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            ResponseEntity.badRequest()
                .body(mapOf("error" to e.message))
        }
    }
}

