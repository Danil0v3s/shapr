package br.com.firstsoft.shapr.runtime.auth

import br.com.firstsoft.shapr.dsl.AccessContext
import br.com.firstsoft.shapr.dsl.AccessControl
import br.com.firstsoft.shapr.dsl.AccessFunction
import br.com.firstsoft.shapr.dsl.AccessResult
import br.com.firstsoft.shapr.dsl.AccessRule
import br.com.firstsoft.shapr.dsl.CollectionDefinition
import br.com.firstsoft.shapr.dsl.FieldAccess
import br.com.firstsoft.shapr.dsl.FieldAccessContext
import br.com.firstsoft.shapr.dsl.query.Where
import br.com.firstsoft.shapr.runtime.query.WhereToSpecificationConverter
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service

/**
 * Service for handling document-level and field-level access control.
 * This is used at runtime by generated controllers.
 */
@Service
class AccessControlService(
    private val objectMapper: ObjectMapper
) {
    
    /**
     * Evaluate access control for a read operation.
     * Returns a JPA Specification that filters documents based on access rules.
     * 
     * @param collection The collection definition
     * @param id Optional document ID (for single document reads)
     * @return JPA Specification to filter results, or null if no filtering needed
     * @throws AccessDeniedException if access is denied
     */
    fun <T> evaluateReadAccess(
        collection: CollectionDefinition,
        id: Any? = null
    ): Specification<T>? {
        val accessControl = collection.access
        
        // First check static access rule
        checkStaticAccess(accessControl.read)
        
        // Then check dynamic access function if present
        val dynamicAccess = accessControl.readAccess
        if (dynamicAccess != null) {
            val whereClause = AuthUtil.checkDynamicAccess(dynamicAccess, id = id)
            if (whereClause != null) {
                val converter = WhereToSpecificationConverter<T>(collection, objectMapper)
                return converter.convert(whereClause)
            }
        }
        
        return null
    }
    
    /**
     * Evaluate access control for an update operation.
     * Returns a JPA Specification that filters which documents can be updated.
     */
    fun <T> evaluateUpdateAccess(
        collection: CollectionDefinition,
        id: Any? = null
    ): Specification<T>? {
        val accessControl = collection.access
        
        checkStaticAccess(accessControl.update)
        
        val dynamicAccess = accessControl.updateAccess
        if (dynamicAccess != null) {
            val whereClause = AuthUtil.checkDynamicAccess(dynamicAccess, id = id)
            if (whereClause != null) {
                val converter = WhereToSpecificationConverter<T>(collection, objectMapper)
                return converter.convert(whereClause)
            }
        }
        
        return null
    }
    
    /**
     * Evaluate access control for a delete operation.
     * Returns a JPA Specification that filters which documents can be deleted.
     */
    fun <T> evaluateDeleteAccess(
        collection: CollectionDefinition,
        id: Any? = null
    ): Specification<T>? {
        val accessControl = collection.access
        
        checkStaticAccess(accessControl.delete)
        
        val dynamicAccess = accessControl.deleteAccess
        if (dynamicAccess != null) {
            val whereClause = AuthUtil.checkDynamicAccess(dynamicAccess, id = id)
            if (whereClause != null) {
                val converter = WhereToSpecificationConverter<T>(collection, objectMapper)
                return converter.convert(whereClause)
            }
        }
        
        return null
    }
    
    /**
     * Check static access rule.
     */
    private fun checkStaticAccess(rule: AccessRule) {
        AuthUtil.checkAccess(rule)
    }
    
    /**
     * Filter fields from a document based on field-level access control.
     * Returns a new map with only the accessible fields.
     */
    fun filterFields(
        doc: Map<String, Any?>,
        collection: CollectionDefinition,
        operation: FieldOperation = FieldOperation.READ
    ): Map<String, Any?> {
        val accessContext = AuthUtil.buildAccessContext(doc = doc)
        val result = mutableMapOf<String, Any?>()
        
        for ((fieldName, value) in doc) {
            val fieldDef = collection.fields.find { it.name == fieldName }
            val fieldAccess = fieldDef?.access
            
            if (fieldAccess == null) {
                // No field-level access, include the field
                result[fieldName] = value
            } else {
                val ctx = FieldAccessContext(
                    accessContext = accessContext,
                    doc = doc,
                    fieldName = fieldName
                )
                
                val hasAccess = when (operation) {
                    FieldOperation.READ -> fieldAccess.read?.invoke(ctx) ?: true
                    FieldOperation.UPDATE -> fieldAccess.update?.invoke(ctx) ?: true
                    FieldOperation.CREATE -> fieldAccess.create?.invoke(ctx) ?: true
                }
                
                if (hasAccess) {
                    result[fieldName] = value
                }
            }
        }
        
        return result
    }
    
    enum class FieldOperation {
        READ, UPDATE, CREATE
    }
}

