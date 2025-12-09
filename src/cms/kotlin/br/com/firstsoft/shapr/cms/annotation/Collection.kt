package br.com.firstsoft.shapr.cms.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Collection(
    val path: String = "",
    val auth: Boolean = true
)
