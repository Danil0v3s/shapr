package br.com.firstsoft.shapr.codegen.generators

import br.com.firstsoft.shapr.codegen.GeneratedFile
import br.com.firstsoft.shapr.dsl.*
import com.squareup.kotlinpoet.*

/**
 * Generates JPA entities for _rels tables following Payload's pattern.
 * _rels tables store polymorphic or hasMany relationships.
 * Table naming: {parentTable}_rels
 * Columns: id, order, parent_id, path, plus FK column for each possible target collection.
 */
class RelsTableGenerator(private val basePackage: String) {
    
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
    
    /**
     * Generate a _rels table entity if the collection has polymorphic or hasMany relationships.
     * Returns null if no such relationships exist.
     */
    fun generate(collection: CollectionDefinition): GeneratedFile? {
        val relsFields = collection.fields.filter { it.type.usesRelsTable() }
        if (relsFields.isEmpty()) return null
        
        val parentClassName = slugToClassName(collection.slug)
        val className = "${parentClassName}Rels"
        val tableName = "${collection.slug}_rels"
        
        // Collect all target collections from all polymorphic/hasMany relationships
        val targetCollections: List<String> = relsFields.flatMap { field ->
            when (val type = field.type) {
                is FieldType.Relationship -> type.relationTo
                is FieldType.Upload -> type.relationTo
                else -> emptyList()
            }
        }.distinct()
        
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
        
        // order column
        constructorBuilder.addParameter(ParameterSpec.builder("order", Int::class.asTypeName().copy(nullable = true)).defaultValue("null").build())
        classBuilder.addProperty(PropertySpec.builder("order", Int::class.asTypeName().copy(nullable = true)).initializer("order")
            .addAnnotation(AnnotationSpec.builder(columnAnnotation).addMember("name = %S", "order").build())
            .build())
        
        // _parent relationship
        val parentType = ClassName(entityPackage, parentClassName).copy(nullable = true)
        constructorBuilder.addParameter(ParameterSpec.builder("parent", parentType).defaultValue("null").build())
        classBuilder.addProperty(PropertySpec.builder("parent", parentType).initializer("parent")
            .addAnnotation(AnnotationSpec.builder(manyToOneAnnotation)
                .addMember("fetch = %T.LAZY", fetchTypeClass).build())
            .addAnnotation(AnnotationSpec.builder(joinColumnAnnotation)
                .addMember("name = %S", "parent_id").addMember("nullable = false").build())
            .build())
        
        // path column - identifies which field this relationship belongs to
        val pathType = String::class.asTypeName().copy(nullable = true)
        constructorBuilder.addParameter(ParameterSpec.builder("path", pathType).defaultValue("null").build())
        classBuilder.addProperty(PropertySpec.builder("path", pathType).initializer("path")
            .addAnnotation(AnnotationSpec.builder(columnAnnotation)
                .addMember("name = %S", "path").addMember("nullable = false").build())
            .build())
        
        // _locale (optional, for localized relationships)
        val localeType = String::class.asTypeName().copy(nullable = true)
        constructorBuilder.addParameter(ParameterSpec.builder("locale", localeType).defaultValue("null").build())
        classBuilder.addProperty(PropertySpec.builder("locale", localeType).initializer("locale")
            .addAnnotation(AnnotationSpec.builder(columnAnnotation)
                .addMember("name = %S", "_locale").addMember("length = 10").build())
            .build())
        
        // FK column for each target collection (e.g., posts_id, categories_id)
        targetCollections.forEach { targetSlug ->
            val targetClassName = slugToClassName(targetSlug)
            val fkName = "${targetSlug}_id"
            val propName = "${toCamelCase(targetSlug)}Id"
            
            val fkType = Long::class.asTypeName().copy(nullable = true)
            constructorBuilder.addParameter(ParameterSpec.builder(propName, fkType).defaultValue("null").build())
            classBuilder.addProperty(PropertySpec.builder(propName, fkType).initializer(propName)
                .addAnnotation(AnnotationSpec.builder(columnAnnotation)
                    .addMember("name = %S", fkName).build())
                .build())
        }
        
        classBuilder.primaryConstructor(constructorBuilder.build())
        
        val file = FileSpec.builder(entityPackage, className)
            .addFileComment("Generated by Shapr CMS - DO NOT EDIT")
            .addType(classBuilder.build())
            .build()
        
        return GeneratedFile(entityPackage, className, file.toString())
    }
    
    private fun toCamelCase(str: String): String {
        return str.split("_", "-").mapIndexed { index, part ->
            if (index == 0) part.lowercase() else part.replaceFirstChar { it.uppercase() }
        }.joinToString("")
    }
}

