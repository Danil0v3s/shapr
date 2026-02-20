package br.com.firstsoft.shapr.dsl

/**
 * Represents a field definition within a collection.
 * Contains all metadata needed to generate JPA entity properties.
 */
data class FieldDefinition(
    val name: String,
    val type: FieldType,
    val label: String? = null,
    val description: String? = null,
    val admin: FieldAdminConfig = FieldAdminConfig(),
    /** Field-level access control. If null, inherits from collection access. */
    val access: FieldAccess? = null
)

/**
 * Admin-specific configuration for a field.
 */
data class FieldAdminConfig(
    val hidden: Boolean = false,
    val readOnly: Boolean = false,
    val position: FieldPosition = FieldPosition.MAIN,
    val width: String? = null
)

/**
 * Position of the field in the admin UI.
 */
enum class FieldPosition {
    MAIN,
    SIDEBAR
}
