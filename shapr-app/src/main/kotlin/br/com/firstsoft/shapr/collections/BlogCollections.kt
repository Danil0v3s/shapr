package br.com.firstsoft.shapr.collections

import br.com.firstsoft.shapr.dsl.*
import br.com.firstsoft.shapr.dsl.builders.*

/**
 * Blog-related collections (Posts, Categories)
 */
val blogCollections = shapr {
    
    collection("Post") {
        slug = "posts"
        
        access {
            create = public()
            read = public()
            update = public()
            delete = public()
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
}
