package br.com.firstsoft.shapr.codegen.generators

import br.com.firstsoft.shapr.codegen.GeneratedFile
import br.com.firstsoft.shapr.dsl.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

/**
 * Generates REST Controller classes using KotlinPoet.
 */
class ControllerGenerator(private val basePackage: String) {

    private val entityPackage = "$basePackage.entity"
    private val repositoryPackage = "$basePackage.repository"
    private val controllerPackage = "$basePackage.controller"

    // Spring annotations
    private val restControllerAnnotation = ClassName("org.springframework.web.bind.annotation", "RestController")
    private val requestMappingAnnotation = ClassName("org.springframework.web.bind.annotation", "RequestMapping")
    private val getMappingAnnotation = ClassName("org.springframework.web.bind.annotation", "GetMapping")
    private val postMappingAnnotation = ClassName("org.springframework.web.bind.annotation", "PostMapping")
    private val putMappingAnnotation = ClassName("org.springframework.web.bind.annotation", "PutMapping")
    private val deleteMappingAnnotation = ClassName("org.springframework.web.bind.annotation", "DeleteMapping")
    private val pathVariableAnnotation = ClassName("org.springframework.web.bind.annotation", "PathVariable")
    private val requestBodyAnnotation = ClassName("org.springframework.web.bind.annotation", "RequestBody")
    private val autowiredAnnotation = ClassName("org.springframework.beans.factory.annotation", "Autowired")
    private val responseEntityClass = ClassName("org.springframework.http", "ResponseEntity")

    // Shapr runtime classes
    private val authUtilClass = ClassName("br.com.firstsoft.shapr.runtime.auth", "AuthUtil")
    private val accessRuleClass = ClassName("br.com.firstsoft.shapr.dsl", "AccessRule")
    private val accessControlServiceClass = ClassName("br.com.firstsoft.shapr.runtime.auth", "AccessControlService")
    private val hookExecutorClass = ClassName("br.com.firstsoft.shapr.runtime.hooks", "HookExecutor")
    private val hookContextClass = ClassName("br.com.firstsoft.shapr.dsl.hooks", "DefaultHookContext")
    private val hookOperationTypeClass = ClassName("br.com.firstsoft.shapr.dsl.hooks", "HookOperationType")
    private val shaprConfigClass = ClassName("br.com.firstsoft.shapr.dsl", "ShaprConfig")
    private val paginatedDocsClass = ClassName("br.com.firstsoft.shapr.dsl.query", "PaginatedDocs")
    private val dataResponseClass = ClassName("br.com.firstsoft.shapr.dsl.query", "DataResponse")

    fun generate(collection: CollectionDefinition): GeneratedFile {
        val entityClassName = slugToClassName(collection.slug)
        val repositoryName = "${entityClassName}Repository"
        val controllerName = "${entityClassName}Controller"

        val entityType = ClassName(entityPackage, entityClassName)
        val repositoryType = ClassName(repositoryPackage, repositoryName)

        val classBuilder = TypeSpec.classBuilder(controllerName)
            .addAnnotation(restControllerAnnotation)
            .addAnnotation(
                AnnotationSpec.builder(requestMappingAnnotation)
                    .addMember("%S", "/api/${collection.slug}")
                    .build()
            )

        // Constructor with dependencies injection
        val constructorBuilder = FunSpec.constructorBuilder()
            .addAnnotation(autowiredAnnotation)
            .addParameter("repository", repositoryType)
            .addParameter("hookExecutor", hookExecutorClass.copy(nullable = true))
            .addParameter("config", shaprConfigClass)
            .addParameter("accessControlService", accessControlServiceClass)

        classBuilder.primaryConstructor(constructorBuilder.build())

        // Repository property
        classBuilder.addProperty(
            PropertySpec.builder("repository", repositoryType)
                .initializer("repository")
                .addModifiers(KModifier.PRIVATE)
                .build()
        )

        // HookExecutor property
        classBuilder.addProperty(
            PropertySpec.builder("hookExecutor", hookExecutorClass.copy(nullable = true))
                .initializer("hookExecutor")
                .addModifiers(KModifier.PRIVATE)
                .build()
        )

        // AccessControlService property
        classBuilder.addProperty(
            PropertySpec.builder("accessControlService", accessControlServiceClass)
                .initializer("accessControlService")
                .addModifiers(KModifier.PRIVATE)
                .build()
        )

        // Collection property
        val collectionDefClass = ClassName("br.com.firstsoft.shapr.dsl", "CollectionDefinition")
        classBuilder.addProperty(
            PropertySpec.builder("collection", collectionDefClass.copy(nullable = true))
                .initializer("config.collections.find { it.slug == %S }", collection.slug)
                .addModifiers(KModifier.PRIVATE)
                .build()
        )

        // Add CRUD methods
        classBuilder.addFunction(buildListMethod(entityType))
        classBuilder.addFunction(buildGetByIdMethod(entityType))
        classBuilder.addFunction(buildCreateMethod(entityType, collection.access.create))
        classBuilder.addFunction(buildUpdateMethod(entityType))
        classBuilder.addFunction(buildDeleteMethod(entityType))

        val file = FileSpec.builder(controllerPackage, controllerName)
            .addFileComment("Generated by Shapr CMS - DO NOT EDIT")
            .addType(classBuilder.build())
            .build()

        return GeneratedFile(
            packageName = controllerPackage,
            fileName = controllerName,
            content = file.toString()
        )
    }
    
    private fun buildListMethod(entityType: ClassName): FunSpec {
        val paginatedType = paginatedDocsClass.parameterizedBy(entityType)

        return FunSpec.builder("list")
            .addAnnotation(getMappingAnnotation)
            .returns(paginatedType)
            .addStatement("val context = %T()", hookContextClass)
            .addComment("Evaluate read access - may return a filter specification")
            .addStatement("val coll = collection ?: throw IllegalStateException(\"Collection not found\")")
            .addStatement("val spec = accessControlService.evaluateReadAccess<%T>(coll)", entityType)
            .addComment("Fetch docs with optional filter")
            .addStatement("val allDocs = if (spec != null) repository.findAll(spec) else repository.findAll()")
            .beginControlFlow("val processedDocs = allDocs.map { doc ->")
            .addStatement("var processedDoc = doc")
            .addStatement("processedDoc = hookExecutor?.executeBeforeRead(coll, context, processedDoc) ?: processedDoc")
            .addStatement("processedDoc = hookExecutor?.executeAfterRead(coll, context, processedDoc, findMany = true) ?: processedDoc")
            .addStatement("processedDoc")
            .endControlFlow()
            .addStatement("return %T(", paginatedDocsClass)
            .addStatement("    docs = processedDocs,")
            .addStatement("    totalDocs = processedDocs.size.toLong(),")
            .addStatement("    limit = processedDocs.size,")
            .addStatement("    totalPages = 1,")
            .addStatement("    page = 1,")
            .addStatement("    pagingCounter = 1,")
            .addStatement("    hasPrevPage = false,")
            .addStatement("    hasNextPage = false,")
            .addStatement("    prevPage = null,")
            .addStatement("    nextPage = null")
            .addStatement(")")
            .build()
    }
    
    private fun buildGetByIdMethod(entityType: ClassName): FunSpec {
        val dataResponseType = dataResponseClass.parameterizedBy(entityType)
        val responseType = responseEntityClass.parameterizedBy(dataResponseType)
        val specClass = ClassName("org.springframework.data.jpa.domain", "Specification")
        val specType = specClass.parameterizedBy(entityType)

        return FunSpec.builder("getById")
            .addAnnotation(
                AnnotationSpec.builder(getMappingAnnotation)
                    .addMember("%S", "/{id}")
                    .build()
            )
            .addParameter(
                ParameterSpec.builder("id", Long::class)
                    .addAnnotation(pathVariableAnnotation)
                    .build()
            )
            .returns(responseType)
            .addStatement("val context = %T()", hookContextClass)
            .addStatement("val coll = collection ?: throw IllegalStateException(\"Collection not found\")")
            .addComment("Evaluate read access with document ID")
            .addStatement("val accessSpec = accessControlService.evaluateReadAccess<%T>(coll, id)", entityType)
            .addComment("Combine with ID filter to get specific document")
            .addStatement("val idSpec = %T.where<%T> { root, _, cb -> cb.equal(root.get<Long>(\"id\"), id) }", specClass, entityType)
            .addStatement("val combinedSpec = if (accessSpec != null) idSpec.and(accessSpec) else idSpec")
            .addStatement("val doc = repository.findOne(combinedSpec).orElse(null)")
            .beginControlFlow("return if (doc != null)")
            .addStatement("var processedDoc = doc")
            .addStatement("processedDoc = hookExecutor?.executeBeforeRead(coll, context, processedDoc) ?: processedDoc")
            .addStatement("processedDoc = hookExecutor?.executeAfterRead(coll, context, processedDoc, findMany = false) ?: processedDoc")
            .addStatement("%T.ok(%T(processedDoc))", responseEntityClass, dataResponseClass)
            .nextControlFlow("else")
            .addStatement("%T.notFound().build()", responseEntityClass)
            .endControlFlow()
            .build()
    }
    
    private fun buildCreateMethod(entityType: ClassName, accessRule: AccessRule): FunSpec {
        val dataResponseType = dataResponseClass.parameterizedBy(entityType)
        val responseType = responseEntityClass.parameterizedBy(dataResponseType)
        
        return FunSpec.builder("create")
            .addAnnotation(postMappingAnnotation)
            .addParameter(
                ParameterSpec.builder("entity", entityType)
                    .addAnnotation(requestBodyAnnotation)
                    .build()
            )
            .returns(responseType)
            .addStatement("%T.checkAccess(%L)", authUtilClass, accessRuleToCodeBlock(accessRule))
            .addStatement("val context = %T()", hookContextClass)
            .addStatement("var data = entity")
            .beginControlFlow("collection?.let { coll ->")
            .addStatement("val beforeOpArgs = hookExecutor?.executeBeforeOperation(coll, %T.CREATE, context, data = data)", hookOperationTypeClass)
            .beginControlFlow("if (beforeOpArgs == null)")
            .addStatement("throw IllegalStateException(\"Operation cancelled by beforeOperation hook\")")
            .endControlFlow()
            .addStatement("data = beforeOpArgs.data ?: data")
            .endControlFlow()
            .beginControlFlow("collection?.let { coll ->")
            .addStatement("val validatedData = hookExecutor?.executeBeforeValidate(coll, %T.CREATE, context, data)", hookOperationTypeClass)
            .beginControlFlow("if (validatedData != null)")
            .addStatement("data = validatedData")
            .endControlFlow()
            .endControlFlow()
            .beginControlFlow("collection?.let { coll ->")
            .addStatement("data = hookExecutor?.executeBeforeChange(coll, %T.CREATE, context, data) ?: data", hookOperationTypeClass)
            .endControlFlow()
            .addStatement("val savedDoc = repository.save(data)")
            .beginControlFlow("collection?.let { coll ->")
            .addStatement("val processedDoc = hookExecutor?.executeAfterChange(coll, %T.CREATE, context, data, savedDoc) ?: savedDoc", hookOperationTypeClass)
            .addStatement("return %T.ok(%T(processedDoc))", responseEntityClass, dataResponseClass)
            .endControlFlow()
            .addStatement("return %T.ok(%T(savedDoc))", responseEntityClass, dataResponseClass)
            .build()
    }
    
    private fun buildUpdateMethod(entityType: ClassName): FunSpec {
        val dataResponseType = dataResponseClass.parameterizedBy(entityType)
        val responseType = responseEntityClass.parameterizedBy(dataResponseType)
        val specClass = ClassName("org.springframework.data.jpa.domain", "Specification")

        return FunSpec.builder("update")
            .addAnnotation(
                AnnotationSpec.builder(putMappingAnnotation)
                    .addMember("%S", "/{id}")
                    .build()
            )
            .addParameter(
                ParameterSpec.builder("id", Long::class)
                    .addAnnotation(pathVariableAnnotation)
                    .build()
            )
            .addParameter(
                ParameterSpec.builder("entity", entityType)
                    .addAnnotation(requestBodyAnnotation)
                    .build()
            )
            .returns(responseType)
            .addStatement("val context = %T()", hookContextClass)
            .addStatement("val coll = collection ?: throw IllegalStateException(\"Collection not found\")")
            .addComment("Evaluate update access - may throw AccessDeniedException or return filter")
            .addStatement("val accessSpec = accessControlService.evaluateUpdateAccess<%T>(coll, id)", entityType)
            .addComment("Check if document exists and user has access")
            .addStatement("val idSpec = %T.where<%T> { root, _, cb -> cb.equal(root.get<Long>(\"id\"), id) }", specClass, entityType)
            .addStatement("val combinedSpec = if (accessSpec != null) idSpec.and(accessSpec) else idSpec")
            .addStatement("val originalDoc = repository.findOne(combinedSpec).orElse(null)")
            .beginControlFlow("return if (originalDoc != null)")
            .addStatement("var data = entity")
            .addStatement("val beforeOpArgs = hookExecutor?.executeBeforeOperation(coll, %T.UPDATE, context, data = data, id = id)", hookOperationTypeClass)
            .beginControlFlow("if (beforeOpArgs == null)")
            .addStatement("throw IllegalStateException(\"Operation cancelled by beforeOperation hook\")")
            .endControlFlow()
            .addStatement("data = beforeOpArgs.data ?: data")
            .addStatement("val validatedData = hookExecutor?.executeBeforeValidate(coll, %T.UPDATE, context, data, originalDoc)", hookOperationTypeClass)
            .beginControlFlow("if (validatedData != null)")
            .addStatement("data = validatedData")
            .endControlFlow()
            .addStatement("data = hookExecutor?.executeBeforeChange(coll, %T.UPDATE, context, data, originalDoc) ?: data", hookOperationTypeClass)
            .addStatement("val savedDoc = repository.save(data)")
            .addStatement("val processedDoc = hookExecutor?.executeAfterChange(coll, %T.UPDATE, context, data, savedDoc, originalDoc) ?: savedDoc", hookOperationTypeClass)
            .addStatement("%T.ok(%T(processedDoc))", responseEntityClass, dataResponseClass)
            .nextControlFlow("else")
            .addStatement("%T.notFound().build()", responseEntityClass)
            .endControlFlow()
            .build()
    }

    private fun buildDeleteMethod(entityType: ClassName): FunSpec {
        val responseType = responseEntityClass.parameterizedBy(UNIT)

        return FunSpec.builder("delete")
            .addAnnotation(
                AnnotationSpec.builder(deleteMappingAnnotation)
                    .addMember("%S", "/{id}")
                    .build()
            )
            .addParameter(
                ParameterSpec.builder("id", Long::class)
                    .addAnnotation(pathVariableAnnotation)
                    .build()
            )
            .returns(responseType)
            .addStatement("val context = %T()", hookContextClass)
            .addStatement("val coll = collection ?: throw IllegalStateException(\"Collection not found\")")
            .addComment("Evaluate delete access - may throw AccessDeniedException or return filter")
            .addStatement("accessControlService.evaluateDeleteAccess<%T>(coll, id)", entityType)
            .beginControlFlow("return if (repository.existsById(id))")
            .addStatement("val doc = repository.findById(id).orElse(null)")
            .addStatement("hookExecutor?.executeBeforeDelete(coll, context, id)")
            .addStatement("repository.deleteById(id)")
            .beginControlFlow("doc?.let { deletedDoc ->")
            .addStatement("hookExecutor?.executeAfterDelete(coll, context, deletedDoc, id)")
            .endControlFlow()
            .addStatement("%T.noContent().build()", responseEntityClass)
            .nextControlFlow("else")
            .addStatement("%T.notFound().build()", responseEntityClass)
            .endControlFlow()
            .build()
    }
    
    private fun accessRuleToCodeBlock(rule: AccessRule): CodeBlock {
        return when (rule) {
            is AccessRule.Public -> CodeBlock.of("%T.Public", accessRuleClass)
            is AccessRule.Authenticated -> CodeBlock.of("%T.Authenticated", accessRuleClass)
            is AccessRule.Deny -> CodeBlock.of("%T.Deny", accessRuleClass)
            is AccessRule.Roles -> {
                val rolesList = rule.roles.joinToString(", ") { "\"$it\"" }
                CodeBlock.of("%T.Roles(listOf($rolesList))", accessRuleClass)
            }
        }
    }
}
