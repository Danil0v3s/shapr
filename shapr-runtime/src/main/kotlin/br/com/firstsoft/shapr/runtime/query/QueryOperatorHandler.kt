package br.com.firstsoft.shapr.runtime.query

import br.com.firstsoft.shapr.dsl.query.WhereField
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Predicate
import java.time.Instant

/**
 * Handles conversion of Payload query operators to JPA Criteria API predicates.
 */
object QueryOperatorHandler {
    
    /**
     * Apply operators from a WhereField to a JPA Expression.
     * Returns a list of predicates (one per operator).
     */
    fun applyOperators(
        field: WhereField,
        expression: Expression<*>,
        cb: CriteriaBuilder
    ): List<Predicate> {
        val predicates = mutableListOf<Predicate>()
        
        // Equals
        field.equals?.let { value ->
            predicates.add(cb.equal(expression, convertValue(value)))
        }
        
        // Not equals
        field.not_equals?.let { value ->
            predicates.add(cb.notEqual(expression, convertValue(value)))
        }
        
        // Contains (case-insensitive LIKE)
        field.contains?.let { value ->
            val likePattern = "%${escapeLikePattern(value)}%"
            @Suppress("UNCHECKED_CAST")
            val stringExpr = expression as jakarta.persistence.criteria.Expression<String>
            predicates.add(cb.like(cb.lower(stringExpr), likePattern.lowercase()))
        }
        
        // Like (all words present - simplified to contains for now)
        field.like?.let { value ->
            val likePattern = "%${escapeLikePattern(value)}%"
            @Suppress("UNCHECKED_CAST")
            val stringExpr = expression as jakarta.persistence.criteria.Expression<String>
            predicates.add(cb.like(cb.lower(stringExpr), likePattern.lowercase()))
        }
        
        // Not like
        field.not_like?.let { value ->
            val likePattern = "%${escapeLikePattern(value)}%"
            @Suppress("UNCHECKED_CAST")
            val stringExpr = expression as jakarta.persistence.criteria.Expression<String>
            predicates.add(cb.not(cb.like(cb.lower(stringExpr), likePattern.lowercase())))
        }
        
        // Greater than
        field.greater_than?.let { value ->
            when (expression.javaType) {
                Number::class.java, Long::class.java, Int::class.java, Double::class.java, Float::class.java -> {
                    @Suppress("UNCHECKED_CAST")
                    val numberExpr = expression as jakarta.persistence.criteria.Expression<Number>
                    predicates.add(cb.gt(numberExpr, value))
                }
                else -> {
                    // For dates/timestamps
                    if (value is Number) {
                        val instant = Instant.ofEpochMilli(value.toLong())
                        @Suppress("UNCHECKED_CAST")
                        val instantExpr = expression as jakarta.persistence.criteria.Expression<Instant>
                        predicates.add(cb.greaterThan(instantExpr, instant))
                    }
                }
            }
        }
        
        // Greater than or equal
        field.greater_than_equal?.let { value ->
            when (expression.javaType) {
                Number::class.java, Long::class.java, Int::class.java, Double::class.java, Float::class.java -> {
                    @Suppress("UNCHECKED_CAST")
                    val numberExpr = expression as jakarta.persistence.criteria.Expression<Number>
                    predicates.add(cb.ge(numberExpr, value))
                }
                else -> {
                    if (value is Number) {
                        val instant = Instant.ofEpochMilli(value.toLong())
                        @Suppress("UNCHECKED_CAST")
                        val instantExpr = expression as jakarta.persistence.criteria.Expression<Instant>
                        predicates.add(cb.greaterThanOrEqualTo(instantExpr, instant))
                    }
                }
            }
        }
        
        // Less than
        field.less_than?.let { value ->
            when (expression.javaType) {
                Number::class.java, Long::class.java, Int::class.java, Double::class.java, Float::class.java -> {
                    @Suppress("UNCHECKED_CAST")
                    val numberExpr = expression as jakarta.persistence.criteria.Expression<Number>
                    predicates.add(cb.lt(numberExpr, value))
                }
                else -> {
                    if (value is Number) {
                        val instant = Instant.ofEpochMilli(value.toLong())
                        @Suppress("UNCHECKED_CAST")
                        val instantExpr = expression as jakarta.persistence.criteria.Expression<Instant>
                        predicates.add(cb.lessThan(instantExpr, instant))
                    }
                }
            }
        }
        
        // Less than or equal
        field.less_than_equal?.let { value ->
            when (expression.javaType) {
                Number::class.java, Long::class.java, Int::class.java, Double::class.java, Float::class.java -> {
                    @Suppress("UNCHECKED_CAST")
                    val numberExpr = expression as jakarta.persistence.criteria.Expression<Number>
                    predicates.add(cb.le(numberExpr, value))
                }
                else -> {
                    if (value is Number) {
                        val instant = Instant.ofEpochMilli(value.toLong())
                        @Suppress("UNCHECKED_CAST")
                        val instantExpr = expression as jakarta.persistence.criteria.Expression<Instant>
                        predicates.add(cb.lessThanOrEqualTo(instantExpr, instant))
                    }
                }
            }
        }
        
        // In
        field.`in`?.let { values ->
            if (values.isNotEmpty()) {
                val convertedValues = values.map { convertValue(it) }
                predicates.add(expression.`in`(convertedValues))
            }
        }
        
        // Not in
        field.not_in?.let { values ->
            if (values.isNotEmpty()) {
                val convertedValues = values.map { convertValue(it) }
                predicates.add(cb.not(expression.`in`(convertedValues)))
            }
        }
        
        // All (for array fields - check if all values are in the array)
        // This is a simplified implementation - full support would require array handling
        field.all?.let { values ->
            if (values.isNotEmpty()) {
                // For now, treat as AND of IN conditions for each value
                val allPredicates = values.map { value ->
                    cb.equal(expression, convertValue(value))
                }
                predicates.add(cb.and(*allPredicates.toTypedArray()))
            }
        }
        
        // Exists
        field.exists?.let { exists ->
            if (exists) {
                predicates.add(cb.isNotNull(expression))
            } else {
                predicates.add(cb.isNull(expression))
            }
        }
        
        // Note: near, within, intersects are for geo fields and would require
        // spatial extensions to JPA - leaving as TODO for future implementation
        
        return predicates
    }
    
    /**
     * Convert a value to the appropriate type for JPA.
     */
    private fun convertValue(value: Any): Any {
        return when (value) {
            is String -> value
            is Number -> value
            is Boolean -> value
            is Instant -> value
            else -> value
        }
    }
    
    /**
     * Escape special characters in LIKE patterns.
     */
    private fun escapeLikePattern(pattern: String): String {
        return pattern
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }
}

