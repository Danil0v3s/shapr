package br.com.firstsoft.shapr.cms.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Collection(
    val name: String = "",
    val createAuth: Array<String> = [],
    val readAuth: Array<String> = [],
    val updateAuth: Array<String> = [],
    val deleteAuth: Array<String> = []
)
