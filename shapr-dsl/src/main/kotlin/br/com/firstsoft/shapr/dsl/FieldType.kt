package br.com.firstsoft.shapr.dsl

import kotlin.reflect.KClass

/**
 * Sealed class hierarchy representing all supported field types.
 * Maps to JPA column types and database schemas.
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
        val defaultValue: String? = null
    ) : FieldType()
    
    /** Textarea field - maps to TEXT */
    data class Textarea(
        override val name: String,
        val required: Boolean = false,
        val defaultValue: String? = null
    ) : FieldType()
    
    /** Number field - maps to NUMERIC or BIGINT */
    data class Number(
        override val name: String,
        val integerOnly: Boolean = false,
        val min: kotlin.Number? = null,
        val max: kotlin.Number? = null,
        val required: Boolean = false,
        val defaultValue: kotlin.Number? = null
    ) : FieldType()
    
    /** Checkbox field - maps to BOOLEAN */
    data class Checkbox(
        override val name: String,
        val defaultValue: Boolean = false
    ) : FieldType()
    
    /** Email field - maps to VARCHAR with email validation */
    data class Email(
        override val name: String,
        val required: Boolean = false,
        val unique: Boolean = false
    ) : FieldType()
    
    /** Date field - maps to TIMESTAMP */
    data class Date(
        override val name: String,
        val required: Boolean = false,
        val defaultNow: Boolean = false,
        val dateOnly: Boolean = false
    ) : FieldType()
    
    /** Relationship field - maps to foreign key or join table */
    data class Relationship(
        override val name: String,
        val relationTo: String,
        val hasMany: Boolean = false,
        val required: Boolean = false
    ) : FieldType()
}

/**
 * Extension to get the JPA/Kotlin type for a field
 */
fun FieldType.toKotlinType(): KClass<*> = when (this) {
    is FieldType.Text -> String::class
    is FieldType.Textarea -> String::class
    is FieldType.Number -> if (integerOnly) Long::class else Double::class
    is FieldType.Checkbox -> Boolean::class
    is FieldType.Email -> String::class
    is FieldType.Date -> java.time.Instant::class
    is FieldType.Relationship -> if (hasMany) Set::class else Any::class
}

/**
 * Extension to check if field is nullable
 */
fun FieldType.isNullable(): Boolean = when (this) {
    is FieldType.Text -> !required
    is FieldType.Textarea -> !required
    is FieldType.Number -> !required
    is FieldType.Checkbox -> false
    is FieldType.Email -> !required
    is FieldType.Date -> !required
    is FieldType.Relationship -> !required && !hasMany
}
