package br.com.firstsoft.shapr.config

import br.com.firstsoft.shapr.dsl.ShaprConfig
import br.com.firstsoft.shapr.dsl.CollectionDefinition
import br.com.firstsoft.shapr.dsl.schema.ShaprCol
import br.com.firstsoft.shapr.dsl.schema.ShaprCollection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter

/**
 * Configuration class that exposes the Shapr DSL collections as a Spring bean.
 * This allows the SchemaController to access the collection definitions.
 *
 * Collections are automatically discovered by scanning for classes annotated
 * with @ShaprCol that extend ShaprCollection.
 *
 * Note: The actual JPA entities, repositories, and controllers are generated
 * at compile-time by the Shapr Gradle plugin. This runtime config is used
 * for schema introspection endpoints.
 */
@Configuration
class ShaprConfiguration {

    @Bean
    fun shaprConfig(): ShaprConfig {
        // Scan for @ShaprCol annotated classes to build runtime config
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AnnotationTypeFilter(ShaprCol::class.java))

        val collections = scanner.findCandidateComponents("br.com.firstsoft.shapr.collections")
            .mapNotNull { beanDef ->
                try {
                    val clazz = Class.forName(beanDef.beanClassName)
                    val annotation = clazz.getAnnotation(ShaprCol::class.java)
                    if (annotation != null) {
                        CollectionDefinition(
                            name = annotation.name,
                            slug = annotation.slug.ifEmpty { annotation.name.lowercase() + "s" },
                            timestamps = annotation.timestamps,
                            softDelete = annotation.softDelete
                        )
                    } else null
                } catch (e: Exception) {
                    null
                }
            }

        return ShaprConfig(collections = collections)
    }
}

