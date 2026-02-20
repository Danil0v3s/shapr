package br.com.firstsoft.shapr.collections

import br.com.firstsoft.shapr.dsl.schema.*

/**
 * User-related collections
 */

@ShaprCol(name = "User", slug = "users")
class User : ShaprCollection() {

    @Unique
    var username: String = ""

    @Email
    @Unique
    var email: String = ""

    var firstName: String? = null

    var lastName: String? = null

    @Default(boolValue = true)
    var active: Boolean = true
}
