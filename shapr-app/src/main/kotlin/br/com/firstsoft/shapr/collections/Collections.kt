package br.com.firstsoft.shapr.collections

import br.com.firstsoft.shapr.dsl.*
import br.com.firstsoft.shapr.dsl.builders.*

/**
 * Shapr CMS Collection Definitions
 * 
 * This file defines all collections in the CMS using the Shapr DSL.
 * The code generator reads this file and generates:
 * - JPA Entity classes
 * - Spring Data JPA Repositories
 * - REST Controllers with access control
 */
val collections = shapr {
    
    collection("Post") {
        slug = "posts"
        
        access {
            create = public()
            read = public()
            update = roles("editor")
            delete = roles("admin")
        }
        
        fields {
            text("title") {
                required = true
                maxLength = 200
            }
            textarea("content")
            date("publishedAt")
            number("views") {
                integerOnly = true
            }
        }
        
        admin {
            useAsTitle = "title"
            defaultColumns = listOf("id", "title", "publishedAt")
        }
    }
    
    collection("Category") {
        slug = "categories"
        
        access {
            create = roles("admin")
            read = public()
            update = roles("admin")
            delete = roles("admin")
        }
        
        fields {
            text("name") {
                required = true
                unique = true
            }
            textarea("description")
        }
        
        admin {
            useAsTitle = "name"
        }
    }
    
    collection("Product") {
        slug = "products"
        
        access {
            create = public()
            read = public()
            update = roles("admin")
            delete = roles("admin")
        }
        
        fields {
            text("name") {
                required = true
            }
            textarea("description")
            number("price") {
                required = true
            }
            number("stock") {
                integerOnly = true
            }
            checkbox("active") {
                defaultValue = true
            }
            relationship("category") {
                relationTo = "categories"
            }
        }
        
        admin {
            useAsTitle = "name"
            defaultColumns = listOf("id", "name", "price", "active")
        }
    }
    
    collection("User") {
        slug = "users"
        
        access {
            create = roles("admin")
            read = authenticated()
            update = roles("admin")
            delete = roles("admin")
        }
        
        fields {
            text("username") {
                required = true
                unique = true
            }
            email("email") {
                required = true
                unique = true
            }
            text("firstName")
            text("lastName")
            checkbox("active") {
                defaultValue = true
            }
        }
        
        admin {
            useAsTitle = "username"
            defaultColumns = listOf("id", "username", "email", "active")
        }
    }
}

