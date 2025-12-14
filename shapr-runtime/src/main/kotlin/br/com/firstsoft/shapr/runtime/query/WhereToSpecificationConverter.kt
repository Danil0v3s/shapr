package br.com.firstsoft.shapr.runtime.query

import br.com.firstsoft.shapr.dsl.CollectionDefinition
import br.com.firstsoft.shapr.dsl.query.Where
import br.com.firstsoft.shapr.dsl.query.WhereField
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification

/**
 * Converts Payload-style Where queries to Spring Data JPA Specifications.
 */
class WhereToSpecificationConverter<T>(
    private val collection: CollectionDefinition,
    private val objectMapper: ObjectMapper
) {
    
    /**
     * Convert a Where query to a JPA Specification.
     */
    fun convert(where: Where?): Specification<T> {
        if (where == null || !where.hasConditions()) {
            return Specification.where(null)
        }
        
        return Specification { root: Root<T>, query: CriteriaQuery<*>?, cb: CriteriaBuilder ->
            buildPredicate(where, root, query, cb)
        }
    }
    
    /**
     * Recursively build a predicate from a Where clause.
     */
    private fun buildPredicate(
        where: Where,
        root: Root<T>,
        query: CriteriaQuery<*>?,
        cb: CriteriaBuilder
    ): Predicate? {
        val predicates = mutableListOf<Predicate>()
        
        // Handle AND conditions
        where.and?.let { andConditions ->
            val andPredicates = andConditions.mapNotNull { condition ->
                buildPredicate(condition, root, query, cb)
            }
            if (andPredicates.isNotEmpty()) {
                predicates.add(cb.and(*andPredicates.toTypedArray()))
            }
        }
        
        // Handle OR conditions
        where.or?.let { orConditions ->
            val orPredicates = orConditions.mapNotNull { condition ->
                buildPredicate(condition, root, query, cb)
            }
            if (orPredicates.isNotEmpty()) {
                predicates.add(cb.or(*orPredicates.toTypedArray()))
            }
        }
        
        // Handle field conditions
        where.getFieldConditions().forEach { (path, conditionValue) ->
            try {
                // Validate the path
                QueryPathValidator.validatePath(path, collection)
                
                // Convert condition value to WhereField
                val whereField = convertToWhereField(conditionValue)
                
                if (whereField.hasOperators()) {
                    // Resolve the path (handles nested properties)
                    val expression = if (NestedPropertyResolver.isNested(path)) {
                        NestedPropertyResolver.resolvePath(root, path, collection, cb)
                    } else {
                        root.get<Any>(path)
                    }
                    
                    // Apply operators
                    val operatorPredicates = QueryOperatorHandler.applyOperators(whereField, expression, cb)
                    predicates.addAll(operatorPredicates)
                }
            } catch (e: Exception) {
                throw IllegalArgumentException("Error processing field condition '$path': ${e.message}", e)
            }
        }
        
        // Combine all predicates with AND
        return when {
            predicates.isEmpty() -> null
            predicates.size == 1 -> predicates.first()
            else -> cb.and(*predicates.toTypedArray())
        }
    }
    
    /**
     * Convert a condition value (from JSON) to a WhereField.
     * The value can be:
     * - A WhereField object (already parsed)
     * - A Map (from JSON deserialization)
     * - A direct value (treated as equals)
     */
    private fun convertToWhereField(value: Any): WhereField {
        return when (value) {
            is WhereField -> value
            is Map<*, *> -> {
                // Convert map to WhereField
                objectMapper.convertValue(value, WhereField::class.java)
            }
            else -> {
                // Direct value - treat as equals
                WhereField(equals = value)
            }
        }
    }
}

