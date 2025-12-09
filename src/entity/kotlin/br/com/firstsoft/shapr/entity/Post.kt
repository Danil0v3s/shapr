package br.com.firstsoft.shapr.entity

import br.com.firstsoft.shapr.cms.annotation.Collection
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.Instant

@Entity
@Collection(
    name = "posts",
    readAuth = ["*"],
    updateAuth = ["editor"],
    deleteAuth = ["admin"]
)
data class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val publishedAt: Instant? = null
)
