package br.com.firstsoft.shapr.runtime.controller

import br.com.firstsoft.shapr.dsl.AccessControl
import br.com.firstsoft.shapr.dsl.CollectionDefinition
import br.com.firstsoft.shapr.dsl.hooks.DefaultHookContext
import br.com.firstsoft.shapr.dsl.hooks.HookContext
import br.com.firstsoft.shapr.dsl.hooks.HookOperationType
import br.com.firstsoft.shapr.runtime.auth.AuthUtil
import br.com.firstsoft.shapr.runtime.hooks.HookExecutor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.http.ResponseEntity

/**
 * Base interface for CRUD operations with access control and hooks.
 * Generated controllers implement this interface.
 */
interface CrudOperations<T : Any, ID : Any> {
    val repository: JpaRepository<T, ID>
    val access: AccessControl
    val collection: CollectionDefinition?
    val hookExecutor: HookExecutor?
    
    /**
     * Get the hook context for the current operation.
     * Override this to provide custom context data.
     */
    fun getHookContext(): HookContext = DefaultHookContext()
    
    fun list(): List<T> {
        AuthUtil.checkAccess(access.read)
        
        val context = getHookContext()
        val allDocs = repository.findAll()
        
        // Execute hooks for each document
        return allDocs.map { doc ->
            var processedDoc = doc
            
            // Execute beforeRead hooks
            collection?.let { coll ->
                hookExecutor?.executeBeforeRead(coll, context, processedDoc)
            } ?: processedDoc
            
            // Execute afterRead hooks
            collection?.let { coll ->
                hookExecutor?.executeAfterRead(coll, context, processedDoc, findMany = true)
            } ?: processedDoc
        }
    }
    
    fun getById(id: ID): ResponseEntity<T> {
        AuthUtil.checkAccess(access.read)
        
        val context = getHookContext()
        
        return repository.findById(id)
            .map { doc ->
                var processedDoc = doc
                
                // Execute beforeRead hooks
                collection?.let { coll ->
                    processedDoc = hookExecutor?.executeBeforeRead(coll, context, processedDoc) ?: processedDoc
                }
                
                // Execute afterRead hooks
                collection?.let { coll ->
                    processedDoc = hookExecutor?.executeAfterRead(coll, context, processedDoc, findMany = false)
                        ?: processedDoc
                }
                
                ResponseEntity.ok(processedDoc)
            }
            .orElse(ResponseEntity.notFound().build())
    }
    
    fun create(entity: T): T {
        AuthUtil.checkAccess(access.create)
        
        val context = getHookContext()
        var data = entity
        
        // Execute beforeOperation hooks
        collection?.let { coll ->
            val beforeOpArgs = hookExecutor?.executeBeforeOperation(
                coll,
                HookOperationType.CREATE,
                context,
                data = data
            )
            if (beforeOpArgs == null) {
                throw IllegalStateException("Operation cancelled by beforeOperation hook")
            }
            data = beforeOpArgs.data ?: data
        }
        
        // Execute beforeValidate hooks
        collection?.let { coll ->
            val validatedData = hookExecutor?.executeBeforeValidate(
                coll,
                HookOperationType.CREATE,
                context,
                data
            )
            if (validatedData != null) {
                data = validatedData
            }
        }
        
        // Execute beforeChange hooks
        collection?.let { coll ->
            data = hookExecutor?.executeBeforeChange(
                coll,
                HookOperationType.CREATE,
                context,
                data
            ) ?: data
        }
        
        // Save to database
        val savedDoc = repository.save(data)
        
        // Execute afterChange hooks
        collection?.let { coll ->
            return hookExecutor?.executeAfterChange(
                coll,
                HookOperationType.CREATE,
                context,
                data,
                savedDoc
            ) ?: savedDoc
        }
        
        return savedDoc
    }
    
    fun update(id: ID, entity: T): ResponseEntity<T> {
        AuthUtil.checkAccess(access.update)
        
        val context = getHookContext()
        
        return if (repository.existsById(id)) {
            val originalDoc = repository.findById(id).orElse(null)
            
            var data = entity
            
            // Execute beforeOperation hooks
            collection?.let { coll ->
                val beforeOpArgs = hookExecutor?.executeBeforeOperation(
                    coll,
                    HookOperationType.UPDATE,
                    context,
                    data = data,
                    id = id
                )
                if (beforeOpArgs == null) {
                    throw IllegalStateException("Operation cancelled by beforeOperation hook")
                }
                data = beforeOpArgs.data ?: data
            }
            
            // Execute beforeValidate hooks
            collection?.let { coll ->
                val validatedData = hookExecutor?.executeBeforeValidate(
                    coll,
                    HookOperationType.UPDATE,
                    context,
                    data,
                    originalDoc
                )
                if (validatedData != null) {
                    data = validatedData
                }
            }
            
            // Execute beforeChange hooks
            collection?.let { coll ->
                data = hookExecutor?.executeBeforeChange(
                    coll,
                    HookOperationType.UPDATE,
                    context,
                    data,
                    originalDoc
                ) ?: data
            }
            
            // Save to database
            val savedDoc = repository.save(data)
            
            // Execute afterChange hooks
            collection?.let { coll ->
                val processedDoc = hookExecutor?.executeAfterChange(
                    coll,
                    HookOperationType.UPDATE,
                    context,
                    data,
                    savedDoc,
                    originalDoc
                ) ?: savedDoc
                ResponseEntity.ok(processedDoc)
            } ?: ResponseEntity.ok(savedDoc)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    fun delete(id: ID): ResponseEntity<Void> {
        AuthUtil.checkAccess(access.delete)
        
        val context = getHookContext()
        
        return if (repository.existsById(id)) {
            val doc = repository.findById(id).orElse(null)
            
            // Execute beforeDelete hooks
            collection?.let { coll ->
                hookExecutor?.executeBeforeDelete(coll, context, id)
            }
            
            // Delete from database
            repository.deleteById(id)
            
            // Execute afterDelete hooks
            doc?.let { deletedDoc ->
                collection?.let { coll ->
                    hookExecutor?.executeAfterDelete(coll, context, deletedDoc, id)
                }
            }
            
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}

/**
 * Abstract base controller that provides default CRUD implementations.
 */
abstract class BaseCrudController<T : Any, ID : Any>(
    override val repository: JpaRepository<T, ID>,
    override val access: AccessControl,
    override val collection: CollectionDefinition? = null,
    override val hookExecutor: HookExecutor? = null
) : CrudOperations<T, ID>
