package br.com.firstsoft.shapr.cms.scanner

import kotlin.reflect.KClass

data class CollectionMetadata(
    val entityClass: KClass<*>,
    val entityName: String,
    val idType: KClass<*>,
    val path: String,
    val auth: Boolean
) {
    val repositoryName: String get() = "${entityName}Repository"
    val controllerName: String get() = "${entityName}Controller"
    val repositoryBeanName: String get() = repositoryName.replaceFirstChar { it.lowercase() }
    val controllerBeanName: String get() = controllerName.replaceFirstChar { it.lowercase() }
}
