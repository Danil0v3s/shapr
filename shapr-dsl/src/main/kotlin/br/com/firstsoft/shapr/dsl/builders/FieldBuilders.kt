package br.com.firstsoft.shapr.dsl.builders

import br.com.firstsoft.shapr.dsl.*

/**
 * DSL marker to prevent scope leaking
 */
@DslMarker
annotation class ShaprDsl

/**
 * Builder for text fields
 */
@ShaprDsl
class TextFieldBuilder(private val name: String) {
    var maxLength: Int = 255
    var minLength: Int = 0
    var required: Boolean = false
    var unique: Boolean = false
    var defaultValue: String? = null
    var label: String? = null
    var description: String? = null
    
    internal fun build(): FieldDefinition = FieldDefinition(
        name = name,
        type = FieldType.Text(
            name = name,
            maxLength = maxLength,
            minLength = minLength,
            required = required,
            unique = unique,
            defaultValue = defaultValue
        ),
        label = label,
        description = description
    )
}

/**
 * Builder for textarea fields
 */
@ShaprDsl
class TextareaFieldBuilder(private val name: String) {
    var required: Boolean = false
    var defaultValue: String? = null
    var label: String? = null
    var description: String? = null
    
    internal fun build(): FieldDefinition = FieldDefinition(
        name = name,
        type = FieldType.Textarea(
            name = name,
            required = required,
            defaultValue = defaultValue
        ),
        label = label,
        description = description
    )
}

/**
 * Builder for number fields
 */
@ShaprDsl
class NumberFieldBuilder(private val name: String) {
    var integerOnly: Boolean = false
    var min: Number? = null
    var max: Number? = null
    var required: Boolean = false
    var defaultValue: Number? = null
    var label: String? = null
    var description: String? = null
    
    internal fun build(): FieldDefinition = FieldDefinition(
        name = name,
        type = FieldType.Number(
            name = name,
            integerOnly = integerOnly,
            min = min,
            max = max,
            required = required,
            defaultValue = defaultValue
        ),
        label = label,
        description = description
    )
}

/**
 * Builder for checkbox fields
 */
@ShaprDsl
class CheckboxFieldBuilder(private val name: String) {
    var defaultValue: Boolean = false
    var label: String? = null
    var description: String? = null
    
    internal fun build(): FieldDefinition = FieldDefinition(
        name = name,
        type = FieldType.Checkbox(
            name = name,
            defaultValue = defaultValue
        ),
        label = label,
        description = description
    )
}

/**
 * Builder for email fields
 */
@ShaprDsl
class EmailFieldBuilder(private val name: String) {
    var required: Boolean = false
    var unique: Boolean = false
    var label: String? = null
    var description: String? = null
    
    internal fun build(): FieldDefinition = FieldDefinition(
        name = name,
        type = FieldType.Email(
            name = name,
            required = required,
            unique = unique
        ),
        label = label,
        description = description
    )
}

/**
 * Builder for date fields
 */
@ShaprDsl
class DateFieldBuilder(private val name: String) {
    var required: Boolean = false
    var defaultNow: Boolean = false
    var dateOnly: Boolean = false
    var label: String? = null
    var description: String? = null
    
    internal fun build(): FieldDefinition = FieldDefinition(
        name = name,
        type = FieldType.Date(
            name = name,
            required = required,
            defaultNow = defaultNow,
            dateOnly = dateOnly
        ),
        label = label,
        description = description
    )
}

/**
 * Builder for relationship fields
 */
@ShaprDsl
class RelationshipFieldBuilder(private val name: String) {
    var relationTo: String = ""
    var hasMany: Boolean = false
    var required: Boolean = false
    var label: String? = null
    var description: String? = null
    
    internal fun build(): FieldDefinition {
        require(relationTo.isNotBlank()) { "relationTo must be specified for relationship field '$name'" }
        return FieldDefinition(
            name = name,
            type = FieldType.Relationship(
                name = name,
                relationTo = relationTo,
                hasMany = hasMany,
                required = required
            ),
            label = label,
            description = description
        )
    }
}

/**
 * Builder for the fields block in a collection
 */
@ShaprDsl
class FieldsBuilder {
    private val fields = mutableListOf<FieldDefinition>()
    
    fun text(name: String, block: TextFieldBuilder.() -> Unit = {}) {
        fields.add(TextFieldBuilder(name).apply(block).build())
    }
    
    fun textarea(name: String, block: TextareaFieldBuilder.() -> Unit = {}) {
        fields.add(TextareaFieldBuilder(name).apply(block).build())
    }
    
    fun number(name: String, block: NumberFieldBuilder.() -> Unit = {}) {
        fields.add(NumberFieldBuilder(name).apply(block).build())
    }
    
    fun checkbox(name: String, block: CheckboxFieldBuilder.() -> Unit = {}) {
        fields.add(CheckboxFieldBuilder(name).apply(block).build())
    }
    
    fun email(name: String, block: EmailFieldBuilder.() -> Unit = {}) {
        fields.add(EmailFieldBuilder(name).apply(block).build())
    }
    
    fun date(name: String, block: DateFieldBuilder.() -> Unit = {}) {
        fields.add(DateFieldBuilder(name).apply(block).build())
    }
    
    fun relationship(name: String, block: RelationshipFieldBuilder.() -> Unit) {
        fields.add(RelationshipFieldBuilder(name).apply(block).build())
    }
    
    internal fun build(): List<FieldDefinition> = fields.toList()
}
