package br.com.firstsoft.shapr.dsl.query

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Main query type representing Payload's Where clause structure.
 * Supports:
 * - Field conditions: { "fieldName": { "equals": "value" } }
 * - AND/OR logic: { "and": [...], "or": [...] }
 * - Nested properties: { "author.name": { "equals": "John" } }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Where @JsonCreator constructor(
    @JsonProperty("and") val and: List<Where>? = null,
    @JsonProperty("or") val or: List<Where>? = null
) {
    private val fields: MutableMap<String, Any> = mutableMapOf()
    
    /**
     * Get all field conditions (excluding and/or).
     */
    fun getFieldConditions(): Map<String, Any> {
        return fields.toMap()
    }
    
    /**
     * Check if this Where clause has any conditions.
     */
    fun hasConditions(): Boolean {
        return (and != null && and.isNotEmpty()) ||
                (or != null && or.isNotEmpty()) ||
                fields.isNotEmpty()
    }
    
    /**
     * Jackson annotation to capture any additional fields (field conditions).
     */
    @JsonAnySetter
    fun setField(key: String, value: Any) {
        if (key != "and" && key != "or") {
            fields[key] = value
        }
    }
    
    companion object {
        /**
         * Create an empty Where clause.
         */
        fun empty(): Where = Where()
        
        /**
         * Create a Where clause with AND conditions.
         */
        fun and(vararg conditions: Where): Where {
            return Where(and = conditions.toList())
        }
        
        /**
         * Create a Where clause with OR conditions.
         */
        fun or(vararg conditions: Where): Where {
            return Where(or = conditions.toList())
        }
    }
}

