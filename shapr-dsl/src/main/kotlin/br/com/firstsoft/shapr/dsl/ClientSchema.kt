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
    is FieldType.Code -> "code"
    is FieldType.Number -> "number"
    is FieldType.Checkbox -> "checkbox"
    is FieldType.Email -> "email"
    is FieldType.Date -> "date"
    is FieldType.Json -> "json"
    is FieldType.RichText -> "richText"
    is FieldType.Point -> "point"
    is FieldType.Select -> "select"
    is FieldType.Radio -> "radio"
    is FieldType.Relationship -> "relationship"
    is FieldType.Upload -> "upload"
    is FieldType.Array -> "array"
    is FieldType.Blocks -> "blocks"
    is FieldType.Group -> "group"
    is FieldType.Tab -> "tab"
}

/**
 * Check if a field type is required.
 */
fun FieldType.isRequired(): Boolean = when (this) {
    is FieldType.Text -> required
    is FieldType.Textarea -> required
    is FieldType.Code -> required
    is FieldType.Number -> required
    is FieldType.Email -> required
    is FieldType.Date -> required
    is FieldType.Json -> required
    is FieldType.RichText -> required
    is FieldType.Point -> required
    is FieldType.Select -> required
    is FieldType.Radio -> required
    is FieldType.Relationship -> required
    is FieldType.Upload -> required
    is FieldType.Array -> required
    is FieldType.Blocks -> required
    is FieldType.Checkbox -> false
    is FieldType.Group -> false
    is FieldType.Tab -> false
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
        "defaultValue" to defaultValue,
        "localized" to localized,
        "hasMany" to hasMany
    )
    is FieldType.Textarea -> mapOf(
        "defaultValue" to defaultValue,
        "localized" to localized
    )
    is FieldType.Code -> mapOf(
        "language" to language,
        "localized" to localized
    )
    is FieldType.Number -> mapOf(
        "integerOnly" to integerOnly,
        "min" to min,
        "max" to max,
        "defaultValue" to defaultValue,
        "localized" to localized,
        "hasMany" to hasMany
    )
    is FieldType.Checkbox -> mapOf(
        "defaultValue" to defaultValue,
        "localized" to localized
    )
    is FieldType.Email -> mapOf(
        "localized" to localized
    )
    is FieldType.Date -> mapOf(
        "dateOnly" to dateOnly,
        "defaultNow" to defaultNow,
        "localized" to localized
    )
    is FieldType.Json -> mapOf(
        "localized" to localized
    )
    is FieldType.RichText -> mapOf(
        "localized" to localized
    )
    is FieldType.Point -> mapOf(
        "localized" to localized
    )
    is FieldType.Select -> mapOf(
        "options" to options.map { opt ->
            when (opt) {
                is FieldType.SelectOption.StringOption -> opt.value
                is FieldType.SelectOption.LabeledOption -> mapOf("value" to opt.value, "label" to opt.label)
            }
        },
        "hasMany" to hasMany,
        "defaultValue" to defaultValue,
        "localized" to localized
    )
    is FieldType.Radio -> mapOf(
        "options" to options.map { opt ->
            when (opt) {
                is FieldType.SelectOption.StringOption -> opt.value
                is FieldType.SelectOption.LabeledOption -> mapOf("value" to opt.value, "label" to opt.label)
            }
        },
        "defaultValue" to defaultValue,
        "localized" to localized
    )
    is FieldType.Relationship -> mapOf(
        "relationTo" to relationTo,
        "hasMany" to hasMany,
        "localized" to localized
    )
    is FieldType.Upload -> mapOf(
        "relationTo" to relationTo,
        "hasMany" to hasMany,
        "localized" to localized
    )
    is FieldType.Array -> mapOf(
        "fields" to fields.map { it.toClientSchema() },
        "minRows" to minRows,
        "maxRows" to maxRows,
        "localized" to localized
    )
    is FieldType.Blocks -> mapOf(
        "blocks" to blocks.map { block ->
            mapOf(
                "slug" to block.slug,
                "fields" to block.fields.map { it.toClientSchema() }
            )
        },
        "minRows" to minRows,
        "maxRows" to maxRows,
        "localized" to localized
    )
    is FieldType.Group -> mapOf(
        "fields" to fields.map { it.toClientSchema() },
        "localized" to localized
    )
    is FieldType.Tab -> mapOf(
        "fields" to fields.map { it.toClientSchema() },
        "localized" to localized
    )
}

