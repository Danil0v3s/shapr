package br.com.firstsoft.shapr.collections

import br.com.firstsoft.shapr.dsl.*
import br.com.firstsoft.shapr.dsl.builders.*

/**
 * Product-related collections
 */
val productCollections = shapr {
    
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
}
