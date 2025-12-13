package br.com.firstsoft.shapr.runtime.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * Default security configuration for Shapr CMS.
 * Can be overridden by the application.
 */
@Configuration
@EnableWebSecurity
open class SecurityConfig {

    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                // Allow all requests - authorization is handled by AuthUtil in controllers
                auth.anyRequest().permitAll()
            }
            .httpBasic { }

        return http.build()
    }
}
