package br.com.firstsoft.shapr.entity

import br.com.firstsoft.shapr.cms.annotation.Collection
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
@Collection(readAuth = ["*"])  // Public read, admin-only for create/update/delete
data class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val name: String = "",
    val description: String = ""
)
