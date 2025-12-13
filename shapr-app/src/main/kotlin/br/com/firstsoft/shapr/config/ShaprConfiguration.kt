package br.com.firstsoft.shapr.config

import br.com.firstsoft.shapr.collections.collections
import br.com.firstsoft.shapr.dsl.ShaprConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration class that exposes the Shapr DSL collections as a Spring bean.
 * This allows the SchemaController to access the collection definitions.
 */
@Configuration
class ShaprConfiguration {
    
    @Bean
    fun shaprConfig(): ShaprConfig = collections
}

