package br.com.firstsoft.shapr.config

import br.com.firstsoft.shapr.collections.blogCollections
import br.com.firstsoft.shapr.collections.productCollections
import br.com.firstsoft.shapr.collections.userCollections
import br.com.firstsoft.shapr.dsl.ShaprConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration class that exposes the Shapr DSL collections as a Spring bean.
 * This allows the SchemaController to access the collection definitions.
 * 
 * Collections can be defined in multiple files. Import each collection config
 * and merge them here using ShaprConfig.mergeAll().
 * 
 * Example:
 * ```kotlin
 * import br.com.firstsoft.shapr.collections.*
 * 
 * @Bean
 * fun shaprConfig(): ShaprConfig = ShaprConfig.mergeAll(
 *     blogCollections,
 *     productCollections,
 *     userCollections
 * )
 * ```
 * 
 * Or use the combined collections export:
 * ```kotlin
 * @Bean
 * fun shaprConfig(): ShaprConfig = collections
 * ```
 */
@Configuration
class ShaprConfiguration {
    
    @Bean
    fun shaprConfig(): ShaprConfig = ShaprConfig.mergeAll(
        blogCollections,
        productCollections,
        userCollections
    )
}

