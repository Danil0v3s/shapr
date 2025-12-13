package br.com.firstsoft.shapr.runtime.auth

import br.com.firstsoft.shapr.dsl.AccessRule
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder

/**
 * Utility object for handling authentication and authorization checks.
 */
object AuthUtil {

    /**
     * Ensures the current user is authenticated.
     * @throws AccessDeniedException if user is not authenticated
     */
    fun requireAuthenticated() {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated || auth.principal == "anonymousUser") {
            throw AccessDeniedException("Authentication required")
        }
    }

    /**
     * Checks if the current user has any of the specified roles.
     * @param roles List of role names (without ROLE_ prefix)
     * @throws AccessDeniedException if user doesn't have required role
     */
    fun requireRole(roles: List<String>) {
        // "*" means public access - no authentication required
        if (roles.contains("*")) return

        requireAuthenticated()

        val auth = SecurityContextHolder.getContext().authentication
        val userRoles = auth?.authorities?.map { it.authority } ?: listOf()

        if (userRoles.none { it in roles || "ROLE_$it" in roles || it.removePrefix("ROLE_") in roles }) {
            throw AccessDeniedException("Missing required role: $roles")
        }
    }
    
    /**
     * Checks access based on an AccessRule from the DSL.
     * @param rule The access rule to check
     * @throws AccessDeniedException if access is denied
     */
    fun checkAccess(rule: AccessRule) {
        when (rule) {
            is AccessRule.Public -> { /* Allow all */ }
            is AccessRule.Authenticated -> requireAuthenticated()
            is AccessRule.Roles -> requireRole(rule.roles)
            is AccessRule.Deny -> throw AccessDeniedException("Access denied")
        }
    }
    
    /**
     * Gets the current authenticated user's username, or null if not authenticated.
     */
    fun getCurrentUsername(): String? {
        val auth = SecurityContextHolder.getContext().authentication
        return if (auth != null && auth.isAuthenticated && auth.principal != "anonymousUser") {
            auth.name
        } else null
    }
    
    /**
     * Gets the current user's roles.
     */
    fun getCurrentRoles(): List<String> {
        val auth = SecurityContextHolder.getContext().authentication
        return auth?.authorities?.map { it.authority.removePrefix("ROLE_") } ?: emptyList()
    }
}
