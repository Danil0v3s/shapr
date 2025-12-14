package br.com.firstsoft.shapr.runtime.service

import br.com.firstsoft.shapr.dsl.CollectionDefinition
import br.com.firstsoft.shapr.dsl.hooks.DefaultHookContext
import br.com.firstsoft.shapr.dsl.hooks.HookContext
import br.com.firstsoft.shapr.dsl.query.FindOptions
import br.com.firstsoft.shapr.dsl.query.FindOptionsBuilder
import br.com.firstsoft.shapr.dsl.query.PaginatedDocs
import br.com.firstsoft.shapr.dsl.query.ShaprQueryService
import br.com.firstsoft.shapr.dsl.query.Where
import br.com.firstsoft.shapr.dsl.query.findOptions
import br.com.firstsoft.shapr.dsl.slugToClassName
import br.com.firstsoft.shapr.runtime.hooks.HookExecutor
import br.com.firstsoft.shapr.runtime.query.WhereToSpecificationConverter
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Root
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

/**
 * Service providing Payload-style query API for Shapr collections.
 * Supports where clauses, pagination, and sorting.
 */
@Service
internal class ShaprQueryServiceImpl(
    private val entityManager: EntityManager,
    private val collectionRegistry: CollectionRegistry,
    private val objectMapper: ObjectMapper,
    @Autowired(required = false) private val hookExecutor: HookExecutor? = null
) : ShaprQueryService {
    
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
    override fun <T : Any> find(entityClass: KClass<T>, block: FindOptionsBuilder.() -> Unit): PaginatedDocs<T> {
        val collection = collectionRegistry.getByEntityClass(entityClass.java)
            ?: throw IllegalArgumentException(
                "Collection not found for entity class '${entityClass.simpleName}'. " +
                "Make sure the entity class name matches a collection name."
            )
        
        val options = findOptions {
            this.collection = collection.slug
            block()
        }
        
        return find(entityClass, options)
    }
    
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
    override fun find(block: FindOptionsBuilder.() -> Unit): PaginatedDocs<Any> {
        val options = findOptions(block)
        return find(options)
    }
    
    /**
     * Find documents matching the query options using collection slug.
     * 
     * @param options Query options (collection slug, where, pagination, sorting)
     * @return Paginated results matching Payload format
     */
    @Suppress("UNCHECKED_CAST")
    override fun find(options: FindOptions): PaginatedDocs<Any> {
        val collection = collectionRegistry.getBySlug(options.collection)
            ?: throw IllegalArgumentException("Collection '${options.collection}' not found")
        
        // Resolve entity class from collection slug
        // Convention: entity class name = slugToClassName(collection.slug)
        val entityClassName = slugToClassName(collection.slug)
        val entityClass = try {
            // Try to find entity in common packages
            val packages = listOf(
                "br.com.firstsoft.shapr.generated.entity",
                "br.com.firstsoft.shapr.entity"
            )
            
            var foundClass: Class<*>? = null
            for (pkg in packages) {
                try {
                    foundClass = Class.forName("$pkg.$entityClassName")
                    break
                } catch (e: ClassNotFoundException) {
                    // Continue searching
                }
            }
            
            foundClass ?: throw IllegalArgumentException(
                "Entity class for collection '${options.collection}' not found. " +
                "Expected class name: $entityClassName"
            )
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Cannot resolve entity class for collection '${options.collection}': ${e.message}",
                e
            )
        }
        
        return find(entityClass.kotlin as KClass<Any>, options)
    }
    
    /**
     * Find documents matching the query options.
     * 
     * @param entityClass The entity class to query
     * @param options Query options (where, pagination, sorting)
     * @return Paginated results matching Payload format
     */
    override fun <T : Any> find(
        entityClass: KClass<T>,
        options: FindOptions
    ): PaginatedDocs<T> {
        val collection = collectionRegistry.getBySlug(options.collection)
            ?: throw IllegalArgumentException("Collection '${options.collection}' not found")
        
        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(entityClass.java)
        val root = cq.from(entityClass.java)
        
        // Build where clause
        val converter = WhereToSpecificationConverter<T>(collection, objectMapper)
        val specification = converter.convert(options.where)
        
        // Apply where predicate
        specification?.toPredicate(root, cq, cb)?.let {
            cq.where(it)
        }
        
        // Apply sorting
        val sortOrders = parseSort(options.sort, root, cb)
        if (sortOrders.isNotEmpty()) {
            cq.orderBy(sortOrders)
        }
        
        // Count query for pagination
        val countQuery = cb.createQuery(Long::class.java)
        val countRoot = countQuery.from(entityClass.java)
        specification?.toPredicate(countRoot, countQuery, cb)?.let {
            countQuery.where(it)
        }
        countQuery.select(cb.count(countRoot))
        val totalDocs = entityManager.createQuery(countQuery).singleResult
        
        // Apply pagination
        val usePagination = options.pagination && (options.limit ?: 10) > 0
        val limit = options.limit ?: 10
        val page = options.page ?: 1
        val pageRequest = if (usePagination) {
            PageRequest.of(page - 1, limit) // Spring uses 0-based pages
        } else {
            PageRequest.of(0, Int.MAX_VALUE)
        }
        
        val typedQuery: TypedQuery<T> = entityManager.createQuery(cq)
        if (usePagination) {
            typedQuery.firstResult = pageRequest.offset.toInt()
            typedQuery.maxResults = pageRequest.pageSize
        }
        
        var docs = typedQuery.resultList
        
        // Execute hooks for each document
        val context: HookContext = DefaultHookContext(queryService = this)
        docs = docs.map { doc ->
            var processedDoc = doc
            
            // Execute beforeRead hooks
            hookExecutor?.executeBeforeRead(collection, context, processedDoc)?.let {
                processedDoc = it
            }
            
            // Execute afterRead hooks
            hookExecutor?.executeAfterRead(collection, context, processedDoc, findMany = true)?.let {
                processedDoc = it
            }
            
            processedDoc
        }
        
        // Calculate pagination metadata
        val totalPages = if (usePagination && limit > 0) {
            ((totalDocs + limit - 1) / limit).toInt()
        } else {
            1
        }
        
        val hasPrevPage = page > 1
        val hasNextPage = page < totalPages
        val prevPage = if (hasPrevPage) page - 1 else null
        val nextPage = if (hasNextPage) page + 1 else null
        
        return PaginatedDocs(
            docs = docs,
            totalDocs = totalDocs,
            limit = limit,
            totalPages = totalPages,
            page = page,
            pagingCounter = (page - 1) * limit + 1,
            hasPrevPage = hasPrevPage,
            hasNextPage = hasNextPage,
            prevPage = prevPage,
            nextPage = nextPage
        )
    }
    
    /**
     * Parse sort string (e.g., "-createdAt" or "title") into JPA Order objects.
     */
    private fun <T> parseSort(
        sortString: String?,
        root: Root<T>,
        cb: CriteriaBuilder
    ): List<Order> {
        if (sortString.isNullOrBlank()) {
            return emptyList()
        }
        
        return sortString.split(",").mapNotNull { sortField ->
            val trimmed = sortField.trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            
            val descending = trimmed.startsWith("-")
            val fieldName = if (descending) trimmed.substring(1) else trimmed
            
            try {
                val path = root.get<Any>(fieldName)
                if (descending) {
                    cb.desc(path)
                } else {
                    cb.asc(path)
                }
            } catch (e: Exception) {
                // Invalid field - skip
                null
            }
        }
    }
}

