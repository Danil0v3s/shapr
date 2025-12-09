package br.com.firstsoft.shapr.cms.config

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(
    basePackages = [
        "br.com.firstsoft.shapr.generated.controller"
    ]
)
class CollectionAutoConfiguration
