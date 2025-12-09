package br.com.firstsoft.shapr.cms.scanner

import br.com.firstsoft.shapr.cms.annotation.Collection
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

object CollectionMetaResolver {

    fun resolve(entityClass: KClass<*>): CollectionMetadata {
        val annotation = entityClass.findAnnotation<Collection>()
            ?: error("@Collection annotation missing for ${entityClass.simpleName}")

        val entityName = entityClass.simpleName
            ?: throw IllegalStateException("Entity class must have a name")

        return CollectionMetadata(
            entityClass = entityClass,
            entityName = entityName,
            idType = Long::class, // Default, actual resolution done by EntityScanner
            path = if (annotation.name.isBlank()) pluralize(entityName).lowercase() else annotation.name,
            createRoles = normalize(annotation.createAuth),
            readRoles = normalize(annotation.readAuth),
            updateRoles = normalize(annotation.updateAuth),
            deleteRoles = normalize(annotation.deleteAuth)
        )
    }

    private fun normalize(auth: Array<String>): List<String> =
        if (auth.isEmpty()) listOf("admin") else auth.toList()

    private fun pluralize(name: String): String = when {
        name.endsWith("y") && !name.endsWith("ay") && !name.endsWith("ey") &&
                !name.endsWith("oy") && !name.endsWith("uy") -> name.dropLast(1) + "ies"
        name.endsWith("s") || name.endsWith("x") || name.endsWith("z") ||
                name.endsWith("ch") || name.endsWith("sh") -> name + "es"
        else -> name + "s"
    }
}
