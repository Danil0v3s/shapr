package br.com.firstsoft.shapr.cms.auth

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder

object AuthUtil {

    fun requireAuthenticated() {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated || auth.principal == "anonymousUser") {
            throw AccessDeniedException("Authentication required")
        }
    }

    fun requireRole(roles: List<String>) {
        // "*" means public access - no authentication required
        if (roles.contains("*")) return

        requireAuthenticated()

        val auth = SecurityContextHolder.getContext().authentication
        val userRoles = auth?.authorities?.map { it.authority } ?: listOf()

        if (userRoles.none { it in roles || "ROLE_$it" in roles }) {
            throw AccessDeniedException("Missing required role: $roles")
        }
    }
}
