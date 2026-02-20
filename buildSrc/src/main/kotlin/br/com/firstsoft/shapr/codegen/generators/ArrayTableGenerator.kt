package br.com.firstsoft.shapr.codegen.generators

import br.com.firstsoft.shapr.codegen.GeneratedFile
import br.com.firstsoft.shapr.dsl.*
import com.squareup.kotlinpoet.*

/**
 * Generates JPA entities for array field tables following Payload's pattern.
 * Array tables have: _order (int), _parent_id (FK), _locale (optional), plus array item fields.
 * Table naming: {parentTable}_{arrayFieldName}
 */
class ArrayTableGenerator(private val basePackage: String) {
    
    private val entityPackage = "$basePackage.entity"
    
    private val entityAnnotation = ClassName("jakarta.persistence", "Entity")
    private val tableAnnotation = ClassName("jakarta.persistence", "Table")
    private val idAnnotation = ClassName("jakarta.persistence", "Id")
    private val generatedValueAnnotation = ClassName("jakarta.persistence", "GeneratedValue")
    private val generationTypeClass = ClassName("jakarta.persistence", "GenerationType")
    private val columnAnnotation = ClassName("jakarta.persistence", "Column")
    private val manyToOneAnnotation = ClassName("jakarta.persistence", "ManyToOne")
    private val joinColumnAnnotation = ClassName("jakarta.persistence", "JoinColumn")
    private val fetchTypeClass = ClassName("jakarta.persistence", "FetchType")
    private val instantClass = ClassName("java.time", "Instant")
    
    /**
     * Generate array table entities for all array fields in a collection.
     * Returns a list of generated files (one per array field).
     */
    fun generate(collection: CollectionDefinition): List<GeneratedFile> {
        val result = mutableListOf<GeneratedFile>()
        val parentClassName = slugToClassName(collection.slug)
        
        collection.fields.forEach { field ->
            if (field.type is FieldType.Array) {
                val arrayType = field.type as FieldType.Array
                result.add(generateArrayTable(collection.slug, parentClassName, field.name, arrayType))
            }
        }
        
        return result
    }
    
    private fun generateArrayTable(
        parentSlug: String,
        parentClassName: String,
        fieldName: String,
        arrayType: FieldType.Array
    ): GeneratedFile {
        val tableName = arrayType.dbName ?: "${parentSlug}_${toSnakeCase(fieldName)}"
        val className = "${parentClassName}${toPascalCase(fieldName)}"
        
        val classBuilder = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.DATA)
            .addAnnotation(entityAnnotation)
            .addAnnotation(AnnotationSpec.builder(tableAnnotation).addMember("name = %S", tableName).build())
        
        val constructorBuilder = FunSpec.constructorBuilder()
        
        // ID
        constructorBuilder.addParameter(ParameterSpec.builder("id", Long::class).defaultValue("0").build())
        classBuilder.addProperty(PropertySpec.builder("id", Long::class).initializer("id")
            .addAnnotation(idAnnotation)
            .addAnnotation(AnnotationSpec.builder(generatedValueAnnotation)
                .addMember("strategy = %T.IDENTITY", generationTypeClass).build())
            .build())
        
        // _order
        constructorBuilder.addParameter(ParameterSpec.builder("order", Int::class).defaultValue("0").build())
        classBuilder.addProperty(PropertySpec.builder("order", Int::class).initializer("order")
            .addAnnotation(AnnotationSpec.builder(columnAnnotation)
                .addMember("name = %S", "_order").addMember("nullable = false").build())
            .build())
        
        // _parent relationship
        val parentType = ClassName(entityPackage, parentClassName).copy(nullable = true)
        constructorBuilder.addParameter(ParameterSpec.builder("parent", parentType).defaultValue("null").build())
        classBuilder.addProperty(PropertySpec.builder("parent", parentType).initializer("parent")
            .addAnnotation(AnnotationSpec.builder(manyToOneAnnotation)
                .addMember("fetch = %T.LAZY", fetchTypeClass).build())
            .addAnnotation(AnnotationSpec.builder(joinColumnAnnotation)
                .addMember("name = %S", "_parent_id").addMember("nullable = false").build())
            .build())
        
        // _locale (if array is localized)
        if (arrayType.localized) {
            val localeType = String::class.asTypeName().copy(nullable = true)
            constructorBuilder.addParameter(ParameterSpec.builder("locale", localeType).defaultValue("null").build())
            classBuilder.addProperty(PropertySpec.builder("locale", localeType).initializer("locale")
                .addAnnotation(AnnotationSpec.builder(columnAnnotation)
                    .addMember("name = %S", "_locale").addMember("length = 10").build())
                .build())
        }
        
        // Array item fields
        arrayType.fields.forEach { field -> addArrayItemField(field, constructorBuilder, classBuilder) }
        
        classBuilder.primaryConstructor(constructorBuilder.build())
        
        val file = FileSpec.builder(entityPackage, className)
            .addFileComment("Generated by Shapr CMS - DO NOT EDIT")
            .addType(classBuilder.build())
            .build()
        
        return GeneratedFile(entityPackage, className, file.toString())
    }
    
    private fun addArrayItemField(field: FieldDefinition, constructorBuilder: FunSpec.Builder, classBuilder: TypeSpec.Builder) {
        val name = field.name
        val type = field.type
        
        val (colType, colDef) = when (type) {
            is FieldType.Text -> Pair(String::class.asTypeName().copy(nullable = !type.required), null)
            is FieldType.Textarea, is FieldType.Code -> Pair(String::class.asTypeName().copy(nullable = true), "TEXT")
            is FieldType.Number -> Pair(
                if (type.integerOnly) Long::class.asTypeName().copy(nullable = !type.required)
                else Double::class.asTypeName().copy(nullable = !type.required), null)
            is FieldType.Checkbox -> Pair(Boolean::class.asTypeName(), null)
            is FieldType.Email -> Pair(String::class.asTypeName().copy(nullable = !type.required), null)
            is FieldType.Date -> Pair(instantClass.copy(nullable = !type.required), null)
            is FieldType.Json, is FieldType.RichText -> Pair(String::class.asTypeName().copy(nullable = true), "JSONB")
            is FieldType.Point -> Pair(String::class.asTypeName().copy(nullable = true), "geometry(Point,4326)")
            is FieldType.Select, is FieldType.Radio -> Pair(String::class.asTypeName().copy(nullable = true), null)
            else -> Pair(String::class.asTypeName().copy(nullable = true), null) // Default for complex types
        }
        
        val defaultValue = if (colType.isNullable) "null" else when (type) {
            is FieldType.Checkbox -> "${type.defaultValue}"
            is FieldType.Number -> if (type.integerOnly) "0L" else "0.0"
            else -> "\"\""
        }
        
        constructorBuilder.addParameter(ParameterSpec.builder(name, colType).defaultValue(defaultValue).build())
        
        val colBuilder = AnnotationSpec.builder(columnAnnotation).addMember("nullable = %L", colType.isNullable)
        if (colDef != null) colBuilder.addMember("columnDefinition = %S", colDef)
        if (type is FieldType.Text) colBuilder.addMember("length = %L", type.maxLength)
        
        classBuilder.addProperty(PropertySpec.builder(name, colType).initializer(name)
            .addAnnotation(colBuilder.build()).build())
    }
    
    private fun toSnakeCase(str: String) = str.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    private fun toPascalCase(str: String) = str.replaceFirstChar { it.uppercase() }
}

