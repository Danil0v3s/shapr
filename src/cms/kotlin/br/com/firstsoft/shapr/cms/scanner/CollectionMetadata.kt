package br.com.firstsoft.shapr.cms.scanner

import kotlin.reflect.KClass

data class CollectionMetadata(
    val entityClass: KClass<*>,
    val entityName: String,
    val idType: KClass<*>,
    val path: String,
    val createRoles: List<String>,
    val readRoles: List<String>,
    val updateRoles: List<String>,
    val deleteRoles: List<String>
) {
    val repositoryName: String get() = "${entityName}Repository"
    val controllerName: String get() = "${entityName}Controller"
    val repositoryBeanName: String get() = repositoryName.replaceFirstChar { it.lowercase() }
    val controllerBeanName: String get() = controllerName.replaceFirstChar { it.lowercase() }

    fun isWildcard(roles: List<String>) = roles.contains("*")

    companion object {
        fun normalize(raw: Array<String>): List<String> =
            if (raw.isEmpty()) listOf("admin") else raw.toList()
    }
}
