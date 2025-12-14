package br.com.firstsoft.shapr.dsl

import br.com.firstsoft.shapr.dsl.hooks.CollectionHooksConfig

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
    val softDelete: Boolean = false,
    val hooks: CollectionHooksConfig<*>? = null
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
) {
    /**
     * Merges this config with another config, combining their collections.
     * Validates that all slugs are unique after merging.
     * 
     * @param other The other ShaprConfig to merge with
     * @return A new ShaprConfig containing all collections from both configs
     * @throws IllegalArgumentException if duplicate slugs are found after merging
     */
    fun merge(other: ShaprConfig): ShaprConfig {
        val mergedCollections = (this.collections + other.collections)
        validateUniqueSlugs(mergedCollections)
        return ShaprConfig(collections = mergedCollections)
    }
    
    /**
     * Merges multiple ShaprConfig instances into one.
     * Validates that all slugs are unique after merging.
     * 
     * @param configs The ShaprConfig instances to merge
     * @return A new ShaprConfig containing all collections from all configs
     * @throws IllegalArgumentException if duplicate slugs are found after merging
     */
    companion object {
        fun mergeAll(vararg configs: ShaprConfig): ShaprConfig {
            val allCollections = configs.flatMap { it.collections }
            validateUniqueSlugs(allCollections)
            return ShaprConfig(collections = allCollections)
        }
        
        /**
         * Validates that all collection slugs are unique.
         * Throws IllegalArgumentException if duplicate slugs are found.
         */
        private fun validateUniqueSlugs(collections: List<CollectionDefinition>) {
            val slugCounts = collections.groupingBy { it.slug }.eachCount()
            val duplicates = slugCounts.filter { it.value > 1 }
            
            if (duplicates.isNotEmpty()) {
                val duplicateSlugs = duplicates.keys.joinToString(", ")
                val duplicateDetails = duplicates.map { (slug, count) ->
                    val collectionNames = collections.filter { it.slug == slug }.joinToString(", ") { it.name }
                    "  - Slug '$slug' is used by $count collection(s): $collectionNames"
                }.joinToString("\n")
                
                throw IllegalArgumentException(
                    "Duplicate collection slugs found: $duplicateSlugs\n" +
                    "Each collection must have a unique slug.\n" +
                    "Duplicate details:\n$duplicateDetails"
                )
            }
        }
    }
}

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
