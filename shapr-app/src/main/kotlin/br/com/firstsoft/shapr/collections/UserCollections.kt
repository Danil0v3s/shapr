package br.com.firstsoft.shapr.collections

import br.com.firstsoft.shapr.dsl.*
import br.com.firstsoft.shapr.dsl.builders.*

/**
 * User-related collections
 */
val userCollections = shapr {
    
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
