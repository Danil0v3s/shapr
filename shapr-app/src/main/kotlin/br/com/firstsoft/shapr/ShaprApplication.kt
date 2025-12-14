package br.com.firstsoft.shapr

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(scanBasePackages = ["br.com.firstsoft.shapr", "br.com.firstsoft.shapr.generated.controller"])
@EnableJpaRepositories(basePackages = ["br.com.firstsoft.shapr.generated.repository"])
class ShaprApplication

fun main(args: Array<String>) {
    runApplication<ShaprApplication>(*args)
}

