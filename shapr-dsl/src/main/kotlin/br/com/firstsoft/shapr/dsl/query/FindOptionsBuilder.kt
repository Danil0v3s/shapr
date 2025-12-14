package br.com.firstsoft.shapr.dsl.query

/**
 * Builder DSL for creating FindOptions programmatically.
 */
fun findOptions(block: FindOptionsBuilder.() -> Unit): FindOptions {
    val builder = FindOptionsBuilder()
    builder.block()
    return builder.build()
}

/**
 * Builder for FindOptions with DSL support.
 */
class FindOptionsBuilder {
    var collection: String? = null
    var where: Where? = null
    var limit: Int? = null
    var page: Int? = null
    var sort: String? = null
    var pagination: Boolean = true
    
    /**
     * Build a where clause using DSL.
     */
    fun where(block: WhereBuilder.() -> Unit) {
        val whereBuilder = WhereBuilder()
        whereBuilder.block()
        this.where = whereBuilder.build()
    }
    
    fun build(): FindOptions {
        val collectionName = collection 
            ?: throw IllegalArgumentException("Collection name is required")
        
        return FindOptions(
            collection = collectionName,
            where = where,
            limit = limit,
            page = page,
            sort = sort,
            pagination = pagination
        )
    }
}

/**
 * Builder for Where clauses with DSL support.
 */
class WhereBuilder {
    private val conditions = mutableMapOf<String, WhereField>()
    private var andConditions: MutableList<Where>? = null
    private var orConditions: MutableList<Where>? = null
    
    /**
     * Add a field condition.
     */
    fun field(name: String, block: WhereFieldBuilder.() -> Unit) {
        val fieldBuilder = WhereFieldBuilder()
        fieldBuilder.block()
        conditions[name] = fieldBuilder.build()
    }
    
    /**
     * Add AND conditions.
     */
    fun and(block: WhereBuilder.() -> Unit) {
        if (andConditions == null) {
            andConditions = mutableListOf()
        }
        val builder = WhereBuilder()
        builder.block()
        andConditions!!.add(builder.build())
    }
    
    /**
     * Add OR conditions.
     */
    fun or(block: WhereBuilder.() -> Unit) {
        if (orConditions == null) {
            orConditions = mutableListOf()
        }
        val builder = WhereBuilder()
        builder.block()
        orConditions!!.add(builder.build())
    }
    
    fun build(): Where {
        val where = Where(and = andConditions, or = orConditions)
        conditions.forEach { (key, value) ->
            where.setField(key, value)
        }
        return where
    }
}

/**
 * Builder for WhereField with DSL support.
 */
class WhereFieldBuilder {
    private var equals: Any? = null
    private var not_equals: Any? = null
    private var contains: String? = null
    private var like: String? = null
    private var not_like: String? = null
    private var greater_than: Number? = null
    private var greater_than_equal: Number? = null
    private var less_than: Number? = null
    private var less_than_equal: Number? = null
    private var inValues: List<Any>? = null
    private var not_in: List<Any>? = null
    private var all: List<Any>? = null
    private var exists: Boolean? = null
    private var near: String? = null
    private var within: String? = null
    private var intersects: String? = null
    
    fun equals(value: Any) { this.equals = value }
    fun notEquals(value: Any) { this.not_equals = value }
    fun contains(value: String) { this.contains = value }
    fun like(value: String) { this.like = value }
    fun notLike(value: String) { this.not_like = value }
    fun greaterThan(value: Number) { this.greater_than = value }
    fun greaterThanEqual(value: Number) { this.greater_than_equal = value }
    fun lessThan(value: Number) { this.less_than = value }
    fun lessThanEqual(value: Number) { this.less_than_equal = value }
    fun in_(values: List<Any>) { this.inValues = values }
    fun notIn(values: List<Any>) { this.not_in = values }
    fun all(values: List<Any>) { this.all = values }
    fun exists(value: Boolean) { this.exists = value }
    fun near(value: String) { this.near = value }
    fun within(value: String) { this.within = value }
    fun intersects(value: String) { this.intersects = value }
    
    fun build(): WhereField {
        return WhereField(
            equals = equals,
            not_equals = not_equals,
            contains = contains,
            like = like,
            not_like = not_like,
            greater_than = greater_than,
            greater_than_equal = greater_than_equal,
            less_than = less_than,
            less_than_equal = less_than_equal,
            `in` = inValues,
            not_in = not_in,
            all = all,
            exists = exists,
            near = near,
            within = within,
            intersects = intersects
        )
    }
}

