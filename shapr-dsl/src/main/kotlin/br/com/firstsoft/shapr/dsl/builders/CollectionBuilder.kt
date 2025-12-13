package br.com.firstsoft.shapr.dsl.builders

import br.com.firstsoft.shapr.dsl.*

/**
 * Builder for access control configuration
 */
@ShaprDsl
class AccessBuilder {
    var create: AccessRule = AccessRule.Roles(listOf("admin"))
    var read: AccessRule = AccessRule.Roles(listOf("admin"))
    var update: AccessRule = AccessRule.Roles(listOf("admin"))
    var delete: AccessRule = AccessRule.Roles(listOf("admin"))
    
    internal fun build(): AccessControl = AccessControl(
        create = create,
        read = read,
        update = update,
        delete = delete
    )
}

/**
 * Builder for collection admin configuration
 */
@ShaprDsl
class CollectionAdminBuilder {
    var useAsTitle: String? = null
    var defaultColumns: List<String> = listOf("id")
    var hidden: Boolean = false
    var group: String? = null
    var description: String? = null
    
    internal fun build(): CollectionAdminConfig = CollectionAdminConfig(
        useAsTitle = useAsTitle,
        defaultColumns = defaultColumns,
        hidden = hidden,
        group = group,
        description = description
    )
}

/**
 * Builder for a single collection
 */
@ShaprDsl
class CollectionBuilder(private val name: String) {
    var slug: String = name.lowercase().let { pluralize(it) }
    var singularLabel: String = name
    var pluralLabel: String = pluralize(name)
    var timestamps: Boolean = true
    var softDelete: Boolean = false
    
    private var fields: List<FieldDefinition> = emptyList()
    private var access: AccessControl = AccessControl()
    private var admin: CollectionAdminConfig = CollectionAdminConfig()
    
    /**
     * Define fields for this collection
     */
    fun fields(block: FieldsBuilder.() -> Unit) {
        fields = FieldsBuilder().apply(block).build()
    }
    
    /**
     * Define access control rules
     */
    fun access(block: AccessBuilder.() -> Unit) {
        access = AccessBuilder().apply(block).build()
    }
    
    /**
     * Configure admin panel settings
     */
    fun admin(block: CollectionAdminBuilder.() -> Unit) {
        admin = CollectionAdminBuilder().apply(block).build()
    }
    
    internal fun build(): CollectionDefinition = CollectionDefinition(
        name = name,
        slug = slug,
        labels = Labels(singular = singularLabel, plural = pluralLabel),
        fields = fields,
        access = access,
        admin = admin,
        timestamps = timestamps,
        softDelete = softDelete
    )
}

/**
 * Builder for the root Shapr configuration
 */
@ShaprDsl
class ShaprBuilder {
    private val collections = mutableListOf<CollectionDefinition>()
    
    /**
     * Define a new collection
     */
    fun collection(name: String, block: CollectionBuilder.() -> Unit) {
        collections.add(CollectionBuilder(name).apply(block).build())
    }
    
    internal fun build(): ShaprConfig = ShaprConfig(collections = collections.toList())
}

/**
 * Entry point for the Shapr DSL.
 * 
 * Usage:
 * ```kotlin
 * val config = shapr {
 *     collection("Post") {
 *         slug = "posts"
 *         
 *         access {
 *             read = public()
 *             create = roles("admin", "editor")
 *         }
 *         
 *         fields {
 *             text("title") { required = true }
 *             textarea("content")
 *             date("publishedAt")
 *         }
 *     }
 * }
 * ```
 */
fun shapr(block: ShaprBuilder.() -> Unit): ShaprConfig {
    return ShaprBuilder().apply(block).build()
}
