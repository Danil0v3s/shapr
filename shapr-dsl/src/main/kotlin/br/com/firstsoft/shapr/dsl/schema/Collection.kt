package br.com.firstsoft.shapr.dsl.schema

import kotlin.reflect.KClass

/**
 * Marks a class as a Shapr Collection.
 *
 * Example:
 * ```kotlin
 * @ShaprCol(name = "Post", slug = "posts")
 * class Post : ShaprCollection() {
 *     var title: String = ""
 *     var content: String? = null
 *     var views: Int = 0
 *     var isPublished: Boolean = false
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ShaprCol(
    val name: String,
    val slug: String = "", // If empty, derives from name (pluralized, lowercase)
    val singularLabel: String = "", // If empty, uses name
    val pluralLabel: String = "", // If empty, pluralizes name
    val timestamps: Boolean = true,
    val softDelete: Boolean = false,
    val group: String = "" // Admin panel grouping
)

/**
 * Base class for all Shapr collections.
 * Extend this class and annotate with @Collection.
 */
abstract class ShaprCollection {
    // ID is always present - will be generated
    // Timestamps (createdAt, updatedAt) are added based on @Collection.timestamps
}

/**
 * Marks a class as a Block definition for use in blocks fields.
 * 
 * Example:
 * ```kotlin
 * @Block(slug = "hero")
 * class HeroBlock : ShaprBlock() {
 *     var heading: String = ""
 *     var subheading: String? = null
 *     var backgroundImage: Long? = null // FK to media
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Block(
    val slug: String,
    val singularLabel: String = "",
    val pluralLabel: String = ""
)

/**
 * Base class for all Shapr blocks.
 */
abstract class ShaprBlock

/**
 * Marks a class as an Array item definition.
 * 
 * Example:
 * ```kotlin
 * @ArrayItem
 * class TagItem {
 *     var name: String = ""
 *     var color: String? = null
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ArrayItem

/**
 * Marks a class as a Group definition (flattened into parent table with prefix).
 * 
 * Example:
 * ```kotlin
 * @Group
 * class SeoFields {
 *     var metaTitle: String? = null
 *     var metaDescription: String? = null
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Group

