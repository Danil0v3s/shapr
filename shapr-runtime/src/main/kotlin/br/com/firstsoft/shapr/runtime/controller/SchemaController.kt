package br.com.firstsoft.shapr.runtime.controller

import br.com.firstsoft.shapr.dsl.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller that exposes collection schemas to the frontend.
 * The frontend can use this to dynamically build forms and list views.
 */
@RestController
@RequestMapping("/api/_schema")
class SchemaController(
    private val config: ShaprConfig
) {
    /**
     * Get all collection schemas.
     */
    @GetMapping
    fun getSchema(): SchemaResponse {
        return SchemaResponse(
            collections = config.collections.map { it.toClientSchema() }
        )
    }
    
    /**
     * Get a specific collection schema by slug.
     */
    @GetMapping("/{slug}")
    fun getCollectionSchema(@PathVariable slug: String): ResponseEntity<ClientCollectionSchema> {
        val collection = config.collections.find { it.slug == slug }
        return if (collection != null) {
            ResponseEntity.ok(collection.toClientSchema())
        } else {
            ResponseEntity.notFound().build()
        }
    }
}

/**
 * Response wrapper for the schema endpoint.
 */
data class SchemaResponse(
    val collections: List<ClientCollectionSchema>
)

