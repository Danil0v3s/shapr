package br.com.firstsoft.shapr.cms.aot

import br.com.firstsoft.shapr.cms.annotation.Collection
import br.com.firstsoft.shapr.cms.scanner.CollectionMetadata
import br.com.firstsoft.shapr.cms.scanner.EntityScanner
import org.springframework.aot.generate.GenerationContext
import org.springframework.aot.hint.MemberCategory
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory

/**
 * AOT processor for @Collection entities.
 * 
 * This processor:
 * 1. Scans for entities annotated with @Collection
 * 2. Registers runtime hints for native image compilation
 * 3. Ensures generated repositories and controllers are properly registered
 */
class CollectionAotProcessor : BeanFactoryInitializationAotProcessor {

    companion object {
        private const val BASE_PACKAGE = "br.com.firstsoft.shapr"
        private const val GENERATED_REPO_PACKAGE = "br.com.firstsoft.shapr.generated.repository"
        private const val GENERATED_CTRL_PACKAGE = "br.com.firstsoft.shapr.generated.controller"
    }

    override fun processAheadOfTime(
        beanFactory: ConfigurableListableBeanFactory
    ): BeanFactoryInitializationAotContribution? {
        val collections = try {
            EntityScanner.scan(BASE_PACKAGE)
        } catch (e: Exception) {
            emptyList()
        }

        if (collections.isEmpty()) {
            return null
        }

        return CollectionAotContribution(collections)
    }
}

private class CollectionAotContribution(
    private val collections: List<CollectionMetadata>
) : BeanFactoryInitializationAotContribution {

    companion object {
        private const val GENERATED_REPO_PACKAGE = "br.com.firstsoft.shapr.generated.repository"
        private const val GENERATED_CTRL_PACKAGE = "br.com.firstsoft.shapr.generated.controller"
    }

    override fun applyTo(
        generationContext: GenerationContext,
        beanFactoryInitializationCode: BeanFactoryInitializationCode
    ) {
        val hints = generationContext.runtimeHints

        for (collection in collections) {
            // Register hints for the entity class
            hints.reflection().registerType(collection.entityClass.java) { hint ->
                hint.withMembers(
                    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS,
                    MemberCategory.DECLARED_FIELDS,
                    MemberCategory.PUBLIC_FIELDS
                )
            }

            // Register hints for generated repository
            try {
                val repoClass = Class.forName("$GENERATED_REPO_PACKAGE.${collection.repositoryName}")
                hints.reflection().registerType(repoClass) { hint ->
                    hint.withMembers(
                        MemberCategory.INVOKE_PUBLIC_METHODS,
                        MemberCategory.INVOKE_DECLARED_METHODS
                    )
                }
                hints.proxies().registerJdkProxy(repoClass)
            } catch (_: ClassNotFoundException) {
                // Repository not yet generated, skip
            }

            // Register hints for generated controller
            try {
                val ctrlClass = Class.forName("$GENERATED_CTRL_PACKAGE.${collection.controllerName}")
                hints.reflection().registerType(ctrlClass) { hint ->
                    hint.withMembers(
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_METHODS,
                        MemberCategory.DECLARED_FIELDS
                    )
                }
            } catch (_: ClassNotFoundException) {
                // Controller not yet generated, skip
            }
        }

        // Register hints for the Collection annotation
        hints.reflection().registerType(Collection::class.java) { hint ->
            hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS)
        }
    }
}
