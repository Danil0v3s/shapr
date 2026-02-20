package br.com.firstsoft.shapr.collections

import br.com.firstsoft.shapr.dsl.schema.*

/**
 * Order collection - for testing document-level access control.
 * Regular users can only see their own orders, admins can see all orders.
 */
@ShaprCol(name = "Order", slug = "orders")
class Order : ShaprCollection() {
    
    /** The username of the user who owns this order */
    var ownerId: String = ""
    
    /** Order description */
    var description: String? = null
    
    /** Total amount */
    var total: Double = 0.0
    
    /** Order status */
    @Options("pending", "processing", "shipped", "delivered", "cancelled")
    var status: String = "pending"
}

