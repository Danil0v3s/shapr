package br.com.firstsoft.shapr.dsl

import br.com.firstsoft.shapr.dsl.query.Where

/**
 * Context passed to access control functions.
 * Contains information about the current user and request.
 */
data class AccessContext(
    /** The authenticated user's ID, or null if not authenticated */
    val userId: Any? = null,
    /** The authenticated user's username, or null if not authenticated */
    val username: String? = null,
    /** The authenticated user's roles */
    val roles: List<String> = emptyList(),
    /** The document ID being accessed (for read/update/delete operations) */
    val id: Any? = null,
    /** The document data (available in field-level access for read operations) */
    val doc: Any? = null,
    /** Additional custom data that can be passed through */
    val customData: Map<String, Any?> = emptyMap()
) {
    /** Check if the user is authenticated */
    fun isAuthenticated(): Boolean = userId != null || username != null

    /** Check if the user has any of the specified roles */
    fun hasRole(vararg requiredRoles: String): Boolean {
        val normalizedRequired = requiredRoles.map { it.lowercase().removePrefix("role_") }
        val normalizedUser = roles.map { it.lowercase().removePrefix("role_") }
        return normalizedRequired.any { it in normalizedUser }
    }

    /** Check if user is admin */
    fun isAdmin(): Boolean = hasRole("admin")
}

/**
 * Result of an access control function.
 * Can be:
 * - Boolean: true = allow all, false = deny all
 * - Where: A query constraint to filter documents (document-level access)
 */
sealed class AccessResult {
    /** Allow access to all documents */
    data object Allow : AccessResult()

    /** Deny access to all documents */
    data object Deny : AccessResult()

    /** Filter access using a Where query (row-level security) */
    data class Filter(val where: Where) : AccessResult()

    companion object {
        /** Create an AccessResult from a boolean */
        fun fromBoolean(allowed: Boolean): AccessResult = if (allowed) Allow else Deny

        /** Create an AccessResult from a Where clause */
        fun fromWhere(where: Where): AccessResult = Filter(where)
    }
}

/**
 * Type alias for access control functions.
 * Access functions receive an AccessContext and return an AccessResult.
 */
typealias AccessFunction = (AccessContext) -> AccessResult

/**
 * Represents access control rules for a collection.
 * Each operation can have either a static AccessRule or a dynamic AccessFunction.
 *
 * For document-level filtering (row-level security), use AccessFunction which can return
 * a Where clause to filter which documents the user can see.
 *
 * Example for owner-based access:
 * ```kotlin
 * val access = AccessControl(
 *     read = dynamicAccess { ctx ->
 *         if (ctx.isAdmin()) AccessResult.Allow
 *         else AccessResult.Filter(whereEquals("ownerId", ctx.userId))
 *     }
 * )
 * ```
 */
data class AccessControl(
    val create: AccessRule = AccessRule.Roles(listOf("admin")),
    val read: AccessRule = AccessRule.Roles(listOf("admin")),
    val update: AccessRule = AccessRule.Roles(listOf("admin")),
    val delete: AccessRule = AccessRule.Roles(listOf("admin")),
    /** Dynamic access function for read operations (supports row-level filtering) */
    val readAccess: AccessFunction? = null,
    /** Dynamic access function for update operations (supports row-level filtering) */
    val updateAccess: AccessFunction? = null,
    /** Dynamic access function for delete operations (supports row-level filtering) */
    val deleteAccess: AccessFunction? = null
)

/**
 * Represents a static access rule for a single operation.
 * Static rules are evaluated at compile time and don't support row-level filtering.
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
 * Field-level access control context.
 * Contains information about the document and field being accessed.
 */
data class FieldAccessContext(
    /** The access context with user information */
    val accessContext: AccessContext,
    /** The full document data */
    val doc: Any? = null,
    /** Sibling data (for fields within arrays or groups) */
    val siblingData: Map<String, Any?>? = null,
    /** The field name being accessed */
    val fieldName: String = ""
)

/**
 * Type alias for field-level access control functions.
 * Field access functions can only return boolean (no query constraints).
 */
typealias FieldAccessFunction = (FieldAccessContext) -> Boolean

/**
 * Field-level access control.
 * Controls whether a field can be read or updated.
 * Unlike collection access, field access only supports boolean results (no Where clauses).
 */
data class FieldAccess(
    /** Can the field be read? If null, inherits from collection access */
    val read: FieldAccessFunction? = null,
    /** Can the field be updated? If null, inherits from collection access */
    val update: FieldAccessFunction? = null,
    /** Can the field be set on create? If null, defaults to true */
    val create: FieldAccessFunction? = null
)

// ============================================================================
// Helper functions for creating access rules
// ============================================================================

/** Public access - no authentication required */
fun public(): AccessRule = AccessRule.Public

/** Authenticated - any logged-in user */
fun authenticated(): AccessRule = AccessRule.Authenticated

/** Role-based access - requires one of the specified roles */
fun roles(vararg roles: String): AccessRule = AccessRule.Roles(roles.toList())

/** Deny all access */
fun deny(): AccessRule = AccessRule.Deny

/**
 * Create a dynamic access function.
 * Use this for document-level access control (row-level security).
 *
 * Example:
 * ```kotlin
 * val readAccess = dynamicAccess { ctx ->
 *     when {
 *         ctx.isAdmin() -> AccessResult.Allow
 *         !ctx.isAuthenticated() -> AccessResult.Deny
 *         else -> AccessResult.Filter(whereEquals("ownerId", ctx.userId))
 *     }
 * }
 * ```
 */
fun dynamicAccess(fn: (AccessContext) -> AccessResult): AccessFunction = fn

/**
 * Create a Where clause for filtering by a field equals a value.
 * Convenience function for row-level security.
 */
fun whereEquals(field: String, value: Any?): Where {
    val where = Where()
    where.setField(field, mapOf("equals" to value))
    return where
}

/**
 * Create field access that allows only admins.
 */
fun adminOnlyField(): FieldAccess = FieldAccess(
    read = { ctx -> ctx.accessContext.isAdmin() },
    update = { ctx -> ctx.accessContext.isAdmin() }
)

/**
 * Create field access that allows the owner or admins.
 * @param ownerField The field name containing the owner ID
 */
fun ownerOrAdminField(ownerField: String = "ownerId"): FieldAccess = FieldAccess(
    read = { ctx ->
        ctx.accessContext.isAdmin() ||
        (ctx.doc as? Map<*, *>)?.get(ownerField) == ctx.accessContext.userId
    },
    update = { ctx ->
        ctx.accessContext.isAdmin() ||
        (ctx.doc as? Map<*, *>)?.get(ownerField) == ctx.accessContext.userId
    }
)
