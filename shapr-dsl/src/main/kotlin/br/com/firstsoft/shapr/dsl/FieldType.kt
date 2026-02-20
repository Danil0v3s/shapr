package br.com.firstsoft.shapr.dsl

import kotlin.reflect.KClass

/**
 * Sealed class hierarchy representing all supported field types.
 * Maps to JPA column types and database schemas.
 * Follows Payload CMS field type patterns for relational database mapping.
 */
sealed class FieldType {
    abstract val name: String

    /** Text field - maps to VARCHAR */
    data class Text(
        override val name: String,
        val maxLength: Int = 255,
        val minLength: Int = 0,
        val required: Boolean = false,
        val unique: Boolean = false,
        val localized: Boolean = false,
        val index: Boolean = false,
        val hasMany: Boolean = false,
        val defaultValue: String? = null
    ) : FieldType()

    /** Textarea field - maps to TEXT */
    data class Textarea(
        override val name: String,
        val required: Boolean = false,
        val localized: Boolean = false,
        val index: Boolean = false,
        val defaultValue: String? = null
    ) : FieldType()

    /** Code field - maps to TEXT (for code snippets) */
    data class Code(
        override val name: String,
        val required: Boolean = false,
        val localized: Boolean = false,
        val language: String? = null
    ) : FieldType()

    /** Number field - maps to NUMERIC or BIGINT */
    data class Number(
        override val name: String,
        val integerOnly: Boolean = false,
        val min: kotlin.Number? = null,
        val max: kotlin.Number? = null,
        val required: Boolean = false,
        val localized: Boolean = false,
        val index: Boolean = false,
        val hasMany: Boolean = false,
        val defaultValue: kotlin.Number? = null
    ) : FieldType()

    /** Checkbox field - maps to BOOLEAN */
    data class Checkbox(
        override val name: String,
        val localized: Boolean = false,
        val defaultValue: Boolean = false
    ) : FieldType()

    /** Email field - maps to VARCHAR with email validation */
    data class Email(
        override val name: String,
        val required: Boolean = false,
        val unique: Boolean = false,
        val localized: Boolean = false,
        val index: Boolean = false
    ) : FieldType()

    /** Date field - maps to TIMESTAMP(3) WITH TIME ZONE */
    data class Date(
        override val name: String,
        val required: Boolean = false,
        val localized: Boolean = false,
        val index: Boolean = false,
        val defaultNow: Boolean = false,
        val dateOnly: Boolean = false
    ) : FieldType()

    /** JSON field - maps to JSONB */
    data class Json(
        override val name: String,
        val required: Boolean = false,
        val localized: Boolean = false
    ) : FieldType()

    /** Rich Text field - maps to JSONB (Lexical/Slate format) */
    data class RichText(
        override val name: String,
        val required: Boolean = false,
        val localized: Boolean = false
    ) : FieldType()

    /** Point field - maps to PostGIS GEOMETRY type */
    data class Point(
        override val name: String,
        val required: Boolean = false,
        val localized: Boolean = false
    ) : FieldType()

    /** Select option - can be string or object with value/label */
    sealed class SelectOption {
        data class StringOption(val value: String) : SelectOption()
        data class LabeledOption(val value: String, val label: String) : SelectOption()
    }

    /** Select field - maps to ENUM or separate table for hasMany */
    data class Select(
        override val name: String,
        val options: List<SelectOption>,
        val required: Boolean = false,
        val localized: Boolean = false,
        val index: Boolean = false,
        val hasMany: Boolean = false,
        val defaultValue: String? = null
    ) : FieldType()

    /** Radio field - maps to ENUM (single value only) */
    data class Radio(
        override val name: String,
        val options: List<SelectOption>,
        val required: Boolean = false,
        val localized: Boolean = false,
        val defaultValue: String? = null
    ) : FieldType()

    /**
     * Relationship field - maps to foreign key or _rels table
     * - Single relationTo + not hasMany → FK column ({name}_id)
     * - Multiple relationTo OR hasMany → _rels table (polymorphic)
     */
    data class Relationship(
        override val name: String,
        val relationTo: List<String>,
        val hasMany: Boolean = false,
        val required: Boolean = false,
        val localized: Boolean = false
    ) : FieldType() {
        constructor(
            name: String,
            relationTo: String,
            hasMany: Boolean = false,
            required: Boolean = false,
            localized: Boolean = false
        ) : this(name, listOf(relationTo), hasMany, required, localized)
    }

    /** Upload field - same as relationship but for media collections */
    data class Upload(
        override val name: String,
        val relationTo: List<String>,
        val hasMany: Boolean = false,
        val required: Boolean = false,
        val localized: Boolean = false
    ) : FieldType() {
        constructor(
            name: String,
            relationTo: String,
            hasMany: Boolean = false,
            required: Boolean = false,
            localized: Boolean = false
        ) : this(name, listOf(relationTo), hasMany, required, localized)
    }

    /**
     * Array field - creates separate table with _order, _parent_id
     * Table name: {parentTable}_{fieldName}
     */
    data class Array(
        override val name: String,
        val fields: List<FieldDefinition>,
        val required: Boolean = false,
        val localized: Boolean = false,
        val minRows: Int? = null,
        val maxRows: Int? = null,
        val dbName: String? = null
    ) : FieldType()

    /**
     * Block configuration for BlocksField
     */
    data class BlockConfig(
        val slug: String,
        val fields: List<FieldDefinition>,
        val dbName: String? = null
    )

    /**
     * Blocks field - creates separate table per block type
     * Table name: {parentTable}_blocks_{blockSlug}
     * Includes _order, _parent_id, _path, block_name columns
     */
    data class Blocks(
        override val name: String,
        val blocks: List<BlockConfig>,
        val required: Boolean = false,
        val localized: Boolean = false,
        val minRows: Int? = null,
        val maxRows: Int? = null
    ) : FieldType()

    /**
     * Group field - flattens fields into parent table with column prefix
     * Column naming: {groupName}_{fieldName}
     */
    data class Group(
        override val name: String,
        val fields: List<FieldDefinition>,
        val localized: Boolean = false
    ) : FieldType()

    /**
     * Tab field - same as Group, flattens fields with column prefix
     * Used for UI organization in admin panel
     */
    data class Tab(
        override val name: String,
        val fields: List<FieldDefinition>,
        val localized: Boolean = false
    ) : FieldType()
}

/**
 * Extension to get the JPA/Kotlin type for a field
 */
fun FieldType.toKotlinType(): KClass<*> = when (this) {
    is FieldType.Text -> String::class
    is FieldType.Textarea -> String::class
    is FieldType.Code -> String::class
    is FieldType.Number -> if (integerOnly) Long::class else Double::class
    is FieldType.Checkbox -> Boolean::class
    is FieldType.Email -> String::class
    is FieldType.Date -> java.time.Instant::class
    is FieldType.Json -> String::class // Stored as JSON string
    is FieldType.RichText -> String::class // Stored as JSON string
    is FieldType.Point -> Any::class // PostGIS Point type
    is FieldType.Select -> if (hasMany) Set::class else String::class
    is FieldType.Radio -> String::class
    is FieldType.Relationship -> if (hasMany) Set::class else Any::class
    is FieldType.Upload -> if (hasMany) Set::class else Any::class
    is FieldType.Array -> List::class
    is FieldType.Blocks -> List::class
    is FieldType.Group -> Any::class // Flattened, no direct type
    is FieldType.Tab -> Any::class // Flattened, no direct type
}

/**
 * Extension to check if field is nullable
 */
fun FieldType.isNullable(): Boolean = when (this) {
    is FieldType.Text -> !required
    is FieldType.Textarea -> !required
    is FieldType.Code -> !required
    is FieldType.Number -> !required
    is FieldType.Checkbox -> false
    is FieldType.Email -> !required
    is FieldType.Date -> !required
    is FieldType.Json -> !required
    is FieldType.RichText -> !required
    is FieldType.Point -> !required
    is FieldType.Select -> !required && !hasMany
    is FieldType.Radio -> !required
    is FieldType.Relationship -> !required && !hasMany
    is FieldType.Upload -> !required && !hasMany
    is FieldType.Array -> !required
    is FieldType.Blocks -> !required
    is FieldType.Group -> false // Groups are flattened, not nullable
    is FieldType.Tab -> false // Tabs are flattened, not nullable
}

/**
 * Extension to check if field is localized
 */
fun FieldType.isLocalized(): Boolean = when (this) {
    is FieldType.Text -> localized
    is FieldType.Textarea -> localized
    is FieldType.Code -> localized
    is FieldType.Number -> localized
    is FieldType.Checkbox -> localized
    is FieldType.Email -> localized
    is FieldType.Date -> localized
    is FieldType.Json -> localized
    is FieldType.RichText -> localized
    is FieldType.Point -> localized
    is FieldType.Select -> localized
    is FieldType.Radio -> localized
    is FieldType.Relationship -> localized
    is FieldType.Upload -> localized
    is FieldType.Array -> localized
    is FieldType.Blocks -> localized
    is FieldType.Group -> localized
    is FieldType.Tab -> localized
}

/**
 * Extension to check if field creates a separate table (array, blocks, hasMany select)
 */
fun FieldType.createsSeparateTable(): Boolean = when (this) {
    is FieldType.Array -> true
    is FieldType.Blocks -> true
    is FieldType.Select -> hasMany
    is FieldType.Text -> hasMany
    is FieldType.Number -> hasMany
    else -> false
}

/**
 * Extension to check if field uses _rels table (polymorphic/hasMany relationships)
 */
fun FieldType.usesRelsTable(): Boolean = when (this) {
    is FieldType.Relationship -> hasMany || relationTo.size > 1
    is FieldType.Upload -> hasMany || relationTo.size > 1
    else -> false
}
