package br.com.firstsoft.shapr.dsl

/**
 * Represents access control rules for a collection.
 * Each operation can have different role requirements.
 */
data class AccessControl(
    val create: AccessRule = AccessRule.Roles(listOf("admin")),
    val read: AccessRule = AccessRule.Roles(listOf("admin")),
    val update: AccessRule = AccessRule.Roles(listOf("admin")),
    val delete: AccessRule = AccessRule.Roles(listOf("admin"))
)

/**
 * Represents an access rule for a single operation.
 */
sealed class AccessRule {
    /** Public access - no authentication required */
    data object Public : AccessRule()
    
    /** Authenticated - any logged-in user */
    data object Authenticated : AccessRule()
    
    /** Role-based access - requires one of the specified roles */
    data class Roles(val roles: List<String>) : AccessRule()
    
    /** Deny all access */
    data object Deny : AccessRule()
}

/**
 * Helper functions for creating access rules
 */
fun public(): AccessRule = AccessRule.Public

fun authenticated(): AccessRule = AccessRule.Authenticated

fun roles(vararg roles: String): AccessRule = AccessRule.Roles(roles.toList())

fun deny(): AccessRule = AccessRule.Deny
