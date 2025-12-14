package br.com.firstsoft.shapr.dsl.query

/**
 * Represents field-level query operators matching Payload's WhereField structure.
 * Each operator maps to a specific query condition.
 */
data class WhereField(
    val equals: Any? = null,
    val not_equals: Any? = null,
    val contains: String? = null,
    val like: String? = null,
    val not_like: String? = null,
    val greater_than: Number? = null,
    val greater_than_equal: Number? = null,
    val less_than: Number? = null,
    val less_than_equal: Number? = null,
    val `in`: List<Any>? = null,
    val not_in: List<Any>? = null,
    val all: List<Any>? = null,
    val exists: Boolean? = null,
    val near: String? = null,
    val within: String? = null,
    val intersects: String? = null
) {
    /**
     * Check if this WhereField has any operators set.
     */
    fun hasOperators(): Boolean {
        return equals != null ||
                not_equals != null ||
                contains != null ||
                like != null ||
                not_like != null ||
                greater_than != null ||
                greater_than_equal != null ||
                less_than != null ||
                less_than_equal != null ||
                `in` != null ||
                not_in != null ||
                all != null ||
                exists != null ||
                near != null ||
                within != null ||
                intersects != null
    }
}

