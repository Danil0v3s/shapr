package br.com.firstsoft.shapr.dsl.schema

import kotlin.reflect.KClass

// ============================================================================
// Text Field Customizations
// ============================================================================

/** Sets maximum length for String fields (default: 255) */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class MaxLength(val value: Int)

/** Sets minimum length for String fields */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class MinLength(val value: Int)

/** Marks field as unique in the database */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Unique

/** Creates a database index on this field */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Indexed

/** Marks this field as localized (stored in _locales table) */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Localized

/** Stores multiple values (creates separate table or _rels table) */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class HasMany

// ============================================================================
// Field Type Overrides (when Kotlin type isn't enough)
// ============================================================================

/** Marks String field as textarea (TEXT column instead of VARCHAR) */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Textarea

/** Marks String field as code editor */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Code(val language: String = "")

/** Marks String field as rich text (JSONB column) */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class RichText

/** Marks String field as JSON (JSONB column) */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Json

/** Marks String field as email with validation */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Email

/** Marks field as geographic point (PostGIS GEOMETRY) */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Point

// ============================================================================
// Select/Radio Fields
// ============================================================================

/** Defines options for a select field */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Options(vararg val value: String)

/** Renders as radio buttons instead of dropdown */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Radio

// ============================================================================
// Relationship Fields
// ============================================================================

/** 
 * Marks a Long/Long? field as a relationship to another collection.
 * The field stores the foreign key ID.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Relationship(
    vararg val relationTo: String // Target collection slug(s)
)

/**
 * Marks a Long/Long? field as an upload relationship (media).
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Upload(
    vararg val relationTo: String // Target media collection slug(s)
)

// ============================================================================
// Complex Fields (Arrays, Blocks, Groups)
// ============================================================================

/**
 * Marks a List field as an array with typed items.
 * Creates a separate table: {parentTable}_{fieldName}
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ArrayOf(
    val itemClass: KClass<*>,
    val minRows: Int = 0,
    val maxRows: Int = Int.MAX_VALUE,
    val dbName: String = "" // Custom table name
)

/**
 * Marks a List field as a blocks field.
 * Creates separate tables per block type: {parentTable}_blocks_{blockSlug}
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class BlocksOf(
    vararg val blockClasses: KClass<*>,
    val minRows: Int = 0,
    val maxRows: Int = Int.MAX_VALUE
)

/**
 * Marks a property as a group (embedded fields with prefix).
 * Fields are flattened into parent table with column prefix.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class GroupOf(val groupClass: KClass<*>)

// ============================================================================
// Admin UI Hints (don't affect schema, only admin panel)
// ============================================================================

/** Sets the label shown in admin UI */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Label(val value: String)

/** Sets the description/help text shown in admin UI */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Description(val value: String)

/** Hides field from admin UI */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Hidden

/** Field is read-only in admin UI */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ReadOnly

