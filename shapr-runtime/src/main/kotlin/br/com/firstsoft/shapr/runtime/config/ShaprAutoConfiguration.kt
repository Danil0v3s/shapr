package br.com.firstsoft.shapr.runtime.config

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.ComponentScan

/**
 * Auto-configuration for Shapr CMS runtime components.
 */
@AutoConfiguration
@ComponentScan(basePackages = ["br.com.firstsoft.shapr.runtime"])
open class ShaprAutoConfiguration
