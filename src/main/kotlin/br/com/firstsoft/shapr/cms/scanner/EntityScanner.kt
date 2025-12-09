package br.com.firstsoft.shapr.cms.scanner

import br.com.firstsoft.shapr.cms.annotation.Collection
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

object EntityScanner {

    fun scan(basePackage: String): List<CollectionMetadata> {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AnnotationTypeFilter(Entity::class.java))

        return scanner.findCandidateComponents(basePackage)
            .mapNotNull { beanDef ->
                val clazz = Class.forName(beanDef.beanClassName).kotlin
                clazz.findAnnotation<Collection>()?.let { collection ->
                    createMetadata(clazz, collection)
                }
            }
    }

    fun scanFromClasses(classes: List<Class<*>>): List<CollectionMetadata> {
        return classes.mapNotNull { clazz ->
            val kClass = clazz.kotlin
            val entityAnnotation = kClass.findAnnotation<Entity>()
            val collectionAnnotation = kClass.findAnnotation<Collection>()

            if (entityAnnotation != null && collectionAnnotation != null) {
                createMetadata(kClass, collectionAnnotation)
            } else null
        }
    }

    private fun createMetadata(clazz: KClass<*>, collection: Collection): CollectionMetadata {
        val entityName = clazz.simpleName ?: throw IllegalStateException("Entity class must have a name")
        val idType = findIdType(clazz)
        val path = collection.path.ifEmpty { pluralize(entityName).lowercase() }

        return CollectionMetadata(
            entityClass = clazz,
            entityName = entityName,
            idType = idType,
            path = path,
            auth = collection.auth
        )
    }

    private fun findIdType(clazz: KClass<*>): KClass<*> {
        val idProperty = clazz.memberProperties.find { prop ->
            prop.javaField?.isAnnotationPresent(Id::class.java) == true
        } ?: throw IllegalStateException("Entity ${clazz.simpleName} must have an @Id field")

        return idProperty.returnType.classifier as? KClass<*>
            ?: throw IllegalStateException("Cannot determine ID type for ${clazz.simpleName}")
    }

    private fun pluralize(name: String): String {
        return when {
            name.endsWith("y") && !name.endsWith("ay") && !name.endsWith("ey") &&
                    !name.endsWith("oy") && !name.endsWith("uy") -> name.dropLast(1) + "ies"
            name.endsWith("s") || name.endsWith("x") || name.endsWith("z") ||
                    name.endsWith("ch") || name.endsWith("sh") -> name + "es"
            else -> name + "s"
        }
    }
}
