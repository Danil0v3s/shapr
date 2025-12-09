package br.com.firstsoft.shapr.entity

import br.com.firstsoft.shapr.cms.annotation.Collection
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
@Collection
data class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val name: String = "",
    val price: Double = 0.0
)
