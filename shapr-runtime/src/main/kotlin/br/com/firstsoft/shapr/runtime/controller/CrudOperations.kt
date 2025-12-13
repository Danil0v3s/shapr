package br.com.firstsoft.shapr.runtime.controller

import br.com.firstsoft.shapr.dsl.AccessControl
import br.com.firstsoft.shapr.runtime.auth.AuthUtil
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.http.ResponseEntity

/**
 * Base interface for CRUD operations with access control.
 * Generated controllers implement this interface.
 */
interface CrudOperations<T : Any, ID : Any> {
    val repository: JpaRepository<T, ID>
    val access: AccessControl
    
    fun list(): List<T> {
        AuthUtil.checkAccess(access.read)
        return repository.findAll()
    }
    
    fun getById(id: ID): ResponseEntity<T> {
        AuthUtil.checkAccess(access.read)
        return repository.findById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())
    }
    
    fun create(entity: T): T {
        AuthUtil.checkAccess(access.create)
        return repository.save(entity)
    }
    
    fun update(id: ID, entity: T): ResponseEntity<T> {
        AuthUtil.checkAccess(access.update)
        return if (repository.existsById(id)) {
            ResponseEntity.ok(repository.save(entity))
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    fun delete(id: ID): ResponseEntity<Void> {
        AuthUtil.checkAccess(access.delete)
        return if (repository.existsById(id)) {
            repository.deleteById(id)
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
    override val access: AccessControl
) : CrudOperations<T, ID>
