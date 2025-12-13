package br.com.firstsoft.shapr.dsl

/**
 * Client-facing schema representations for frontend consumption.
 */

data class ClientCollectionSchema(
    val name: String,
    val slug: String,
    val labels: Labels,
    val fields: List<ClientFieldSchema>,
    val access: ClientAccessControl,
    val admin: CollectionAdminConfig,
    val timestamps: Boolean
)

data class ClientFieldSchema(
    val name: String,
    val type: String,
    val label: String,
    val required: Boolean,
    val unique: Boolean,
    val config: Map<String, Any?>
)

data class ClientAccessControl(
    val create: String,
    val read: String,
    val update: String,
    val delete: String
)

/**
 * Convert a CollectionDefinition to a client-facing schema.
 */
fun CollectionDefinition.toClientSchema() = ClientCollectionSchema(
    name = name,
    slug = slug,
    labels = labels,
    fields = fields.map { it.toClientSchema() },
    access = access.toClientSchema(),
    admin = admin,
    timestamps = timestamps
)

/**
 * Convert a FieldDefinition to a client-facing schema.
 */
fun FieldDefinition.toClientSchema() = ClientFieldSchema(
    name = name,
    type = type.typeName(),
    label = label ?: name.replaceFirstChar { it.uppercase() },
    required = type.isRequired(),
    unique = type.isUnique(),
    config = type.toConfigMap()
)

/**
 * Convert AccessControl to a client-facing schema.
 */
fun AccessControl.toClientSchema() = ClientAccessControl(
    create = create.toClientString(),
    read = read.toClientString(),
    update = update.toClientString(),
    delete = delete.toClientString()
)

/**
 * Convert AccessRule to a string representation for the client.
 */
fun AccessRule.toClientString(): String = when (this) {
    is AccessRule.Public -> "public"
    is AccessRule.Authenticated -> "authenticated"
    is AccessRule.Deny -> "deny"
    is AccessRule.Roles -> "roles:${roles.joinToString(",")}"
}

/**
 * Get the type name for a FieldType.
 */
fun FieldType.typeName(): String = when (this) {
    is FieldType.Text -> "text"
    is FieldType.Textarea -> "textarea"
    is FieldType.Number -> "number"
    is FieldType.Checkbox -> "checkbox"
    is FieldType.Email -> "email"
    is FieldType.Date -> "date"
    is FieldType.Relationship -> "relationship"
}

/**
 * Check if a field type is required.
 */
fun FieldType.isRequired(): Boolean = when (this) {
    is FieldType.Text -> required
    is FieldType.Textarea -> required
    is FieldType.Number -> required
    is FieldType.Email -> required
    is FieldType.Date -> required
    is FieldType.Relationship -> required
    is FieldType.Checkbox -> false
}

/**
 * Check if a field type has a unique constraint.
 */
fun FieldType.isUnique(): Boolean = when (this) {
    is FieldType.Text -> unique
    is FieldType.Email -> unique
    else -> false
}

/**
 * Convert field type configuration to a map for the client.
 */
fun FieldType.toConfigMap(): Map<String, Any?> = when (this) {
    is FieldType.Text -> mapOf(
        "maxLength" to maxLength,
        "minLength" to minLength,
        "defaultValue" to defaultValue
    )
    is FieldType.Textarea -> mapOf(
        "defaultValue" to defaultValue
    )
    is FieldType.Number -> mapOf(
        "integerOnly" to integerOnly,
        "min" to min,
        "max" to max,
        "defaultValue" to defaultValue
    )
    is FieldType.Checkbox -> mapOf(
        "defaultValue" to defaultValue
    )
    is FieldType.Email -> emptyMap()
    is FieldType.Date -> mapOf(
        "dateOnly" to dateOnly,
        "defaultNow" to defaultNow
    )
    is FieldType.Relationship -> mapOf(
        "relationTo" to relationTo,
        "hasMany" to hasMany
    )
}

