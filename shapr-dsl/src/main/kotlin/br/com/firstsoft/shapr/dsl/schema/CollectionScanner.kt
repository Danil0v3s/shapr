package br.com.firstsoft.shapr.dsl.schema

import br.com.firstsoft.shapr.dsl.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

/**
 * Scans annotated classes and converts them to CollectionDefinition objects.
 */
object CollectionScanner {
    
    fun scan(collectionClass: KClass<*>): CollectionDefinition {
        val annotation = collectionClass.findAnnotation<ShaprCol>()
            ?: throw IllegalArgumentException("${collectionClass.simpleName} missing @ShaprCol")
        
        val name = annotation.name
        val slug = annotation.slug.ifEmpty { pluralize(name).lowercase() }
        val fields = collectionClass.memberProperties
            .filter { it.name != "id" }
            .mapNotNull { scanProperty(it) }
        
        return CollectionDefinition(
            name = name, slug = slug,
            labels = Labels(annotation.singularLabel.ifEmpty { name }, annotation.pluralLabel.ifEmpty { pluralize(name) }),
            fields = fields, timestamps = annotation.timestamps, softDelete = annotation.softDelete,
            admin = CollectionAdminConfig(group = annotation.group.ifEmpty { null })
        )
    }
    
    fun scanAll(vararg classes: KClass<*>): ShaprConfig = ShaprConfig(classes.map { scan(it) })
    
    private fun scanProperty(prop: KProperty1<*, *>): FieldDefinition? {
        val name = prop.name
        val isNullable = prop.returnType.isMarkedNullable
        val kotlinType = prop.returnType.jvmErasure
        val fieldType = determineFieldType(prop, name, kotlinType, !isNullable) ?: return null
        return FieldDefinition(name = name, type = fieldType)
    }
    
    private fun determineFieldType(prop: KProperty1<*, *>, name: String, kType: KClass<*>, required: Boolean): FieldType? {
        val localized = prop.hasAnnotation<Localized>()
        val indexed = prop.hasAnnotation<Indexed>()
        val unique = prop.hasAnnotation<Unique>()
        val hasMany = prop.hasAnnotation<HasMany>()
        
        // Relationships
        prop.findAnnotation<Relationship>()?.let {
            return FieldType.Relationship(name, it.relationTo.toList(), hasMany, required, localized)
        }
        prop.findAnnotation<Upload>()?.let {
            return FieldType.Upload(name, it.relationTo.toList(), hasMany, required, localized)
        }
        
        // Complex types
        prop.findAnnotation<ArrayOf>()?.let {
            return FieldType.Array(
                name = name, fields = scanItemClass(it.itemClass), required = false,
                localized = localized, minRows = it.minRows, maxRows = it.maxRows,
                dbName = it.dbName.ifEmpty { null }
            )
        }
        prop.findAnnotation<BlocksOf>()?.let {
            return FieldType.Blocks(
                name = name, blocks = it.blockClasses.map { b -> scanBlock(b) },
                required = false, localized = localized, minRows = it.minRows, maxRows = it.maxRows
            )
        }
        prop.findAnnotation<GroupOf>()?.let {
            return FieldType.Group(name = name, fields = scanItemClass(it.groupClass), localized = localized)
        }

        // By Kotlin type
        return when (kType) {
            String::class -> stringFieldType(prop, name, required, localized, indexed, unique, hasMany)
            Int::class, Long::class -> FieldType.Number(
                name = name, integerOnly = true, min = null, max = null,
                required = required, localized = localized, index = indexed, hasMany = hasMany
            )
            Double::class, Float::class -> FieldType.Number(
                name = name, integerOnly = false, min = null, max = null,
                required = required, localized = localized, index = indexed, hasMany = hasMany
            )
            Boolean::class -> FieldType.Checkbox(
                name = name, localized = localized,
                defaultValue = prop.findAnnotation<Default>()?.boolValue ?: false
            )
            Instant::class, LocalDateTime::class, LocalDate::class -> FieldType.Date(
                name = name, required = required, localized = localized
            )
            else -> null
        }
    }

    private fun stringFieldType(prop: KProperty1<*, *>, name: String, req: Boolean, loc: Boolean, idx: Boolean, uniq: Boolean, many: Boolean): FieldType {
        if (prop.hasAnnotation<Textarea>() || prop.hasAnnotation<Code>()) {
            return FieldType.Code(name = name, required = req, localized = loc, language = prop.findAnnotation<Code>()?.language)
        }
        if (prop.hasAnnotation<RichText>()) return FieldType.RichText(name = name, required = req, localized = loc)
        if (prop.hasAnnotation<Json>()) return FieldType.Json(name = name, required = req, localized = loc)
        if (prop.hasAnnotation<Email>()) return FieldType.Email(name = name, required = req, unique = uniq, localized = loc, index = idx)
        if (prop.hasAnnotation<Point>()) return FieldType.Point(name = name, required = req, localized = loc)

        prop.findAnnotation<Options>()?.let { opts ->
            val options = opts.value.map { FieldType.SelectOption.StringOption(it) }
            return if (prop.hasAnnotation<Radio>()) FieldType.Radio(name = name, options = options, required = req, localized = loc)
            else FieldType.Select(name = name, options = options, required = req, localized = loc, index = idx, hasMany = many)
        }

        val maxLen = prop.findAnnotation<MaxLength>()?.value ?: 255
        val minLen = prop.findAnnotation<MinLength>()?.value ?: 0
        return FieldType.Text(name = name, maxLength = maxLen, minLength = minLen, required = req, unique = uniq, localized = loc, index = idx, hasMany = many)
    }

    private fun scanItemClass(cls: KClass<*>) = cls.memberProperties.mapNotNull { scanProperty(it) }

    private fun scanBlock(cls: KClass<*>): FieldType.BlockConfig {
        val ann = cls.findAnnotation<Block>() ?: throw IllegalArgumentException("${cls.simpleName} missing @Block")
        return FieldType.BlockConfig(slug = ann.slug, fields = scanItemClass(cls))
    }
}

/** Default value annotation */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Default(val boolValue: Boolean = false, val stringValue: String = "", val intValue: Int = 0)

