package br.com.firstsoft.shapr.dsl

/**
 * Represents a complete collection definition.
 * This is the main data class that holds all configuration for a collection.
 */
data class CollectionDefinition(
    val name: String,
    val slug: String = name.lowercase().let { pluralize(it) },
    val labels: Labels = Labels(
        singular = name,
        plural = pluralize(name)
    ),
    val fields: List<FieldDefinition> = emptyList(),
    val access: AccessControl = AccessControl(),
    val admin: CollectionAdminConfig = CollectionAdminConfig(),
    val timestamps: Boolean = true,
    val softDelete: Boolean = false
)

/**
 * Labels for the collection in singular and plural forms.
 */
data class Labels(
    val singular: String,
    val plural: String
)

/**
 * Admin-specific configuration for a collection.
 */
data class CollectionAdminConfig(
    val useAsTitle: String? = null,
    val defaultColumns: List<String> = listOf("id"),
    val hidden: Boolean = false,
    val group: String? = null,
    val description: String? = null
)

/**
 * Container for multiple collection definitions.
 */
data class ShaprConfig(
    val collections: List<CollectionDefinition> = emptyList()
)

/**
 * Utility function to pluralize a word.
 */
internal fun pluralize(name: String): String = when {
    name.endsWith("y") && !name.endsWith("ay") && !name.endsWith("ey") &&
            !name.endsWith("oy") && !name.endsWith("uy") -> name.dropLast(1) + "ies"
    name.endsWith("s") || name.endsWith("x") || name.endsWith("z") ||
            name.endsWith("ch") || name.endsWith("sh") -> name + "es"
    else -> name + "s"
}

/**
 * Utility function to singularize a word (basic implementation).
 */
internal fun singularize(name: String): String = when {
    name.endsWith("ies") -> name.dropLast(3) + "y"
    name.endsWith("es") && (name.dropLast(2).endsWith("s") || 
            name.dropLast(2).endsWith("x") || 
            name.dropLast(2).endsWith("z") ||
            name.dropLast(2).endsWith("ch") ||
            name.dropLast(2).endsWith("sh")) -> name.dropLast(2)
    name.endsWith("s") -> name.dropLast(1)
    else -> name
}

/**
 * Utility function to convert slug to class name (PascalCase).
 */
fun slugToClassName(slug: String): String {
    return singularize(slug)
        .split("-", "_")
        .joinToString("") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
}
