package br.com.firstsoft.shapr.collections

import br.com.firstsoft.shapr.dsl.schema.*

/**
 * Product-related collections
 */

@ShaprCol(name = "Product", slug = "products")
class Product : ShaprCollection() {

    var name: String = ""

    @Textarea
    var description: String? = null

    var price: Double = 0.0

    var stock: Int = 0

    @Default(boolValue = true)
    var active: Boolean = true

    @Relationship("categories")
    var category: Long? = null
}
