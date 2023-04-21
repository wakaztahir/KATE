package com.wakaztahir.kate.model

sealed class KATEType {

    protected val actualType get() = if (this is NullableKateType) this.actual else this

    abstract fun getPlaceholderName() : kotlin.String

    abstract fun getKATEType(): kotlin.String

    abstract fun satisfies(type: KATEType): kotlin.Boolean

    override fun toString(): kotlin.String = getKATEType()

    override fun equals(other: kotlin.Any?): kotlin.Boolean {
        if (this === other) return true
        if (other !is KATEType) return false
        return this::class == other::class
    }

    override fun hashCode(): kotlin.Int = this::class.hashCode()

    class NullableKateType(val actual: KATEType) : KATEType() {

        override fun getPlaceholderName(): kotlin.String = actual.getPlaceholderName()

        override fun getKATEType(): kotlin.String = actual.getKATEType() + '?'

        override fun satisfies(type: KATEType): kotlin.Boolean {
            if (type !is NullableKateType) return false
            return this.actual.satisfies(type.actual)
        }

    }

    class Any : KATEType() {

        override fun getPlaceholderName(): kotlin.String = "any"

        override fun getKATEType(): kotlin.String = "any"

        override fun satisfies(type: KATEType): kotlin.Boolean = type.actualType is Any

    }

    class Unit : KATEType(){

        override fun getPlaceholderName(): kotlin.String = "unit"

        override fun getKATEType(): kotlin.String = "unit"

        override fun satisfies(type: KATEType): kotlin.Boolean = type.actualType is Unit

    }

    class Char : KATEType() {

        override fun getPlaceholderName(): kotlin.String = "char"

        override fun getKATEType(): kotlin.String = "char"

        override fun satisfies(type: KATEType): kotlin.Boolean = type.actualType.let { it is Any || it is Char }

    }

    class String : KATEType() {

        override fun getPlaceholderName(): kotlin.String = "string"

        override fun getKATEType(): kotlin.String = "string"

        override fun satisfies(type: KATEType): kotlin.Boolean = type.actualType.let { it is Any || it is String }

    }

    class Int : KATEType() {

        override fun getPlaceholderName(): kotlin.String = "int"

        override fun getKATEType(): kotlin.String = "int"

        override fun satisfies(type: KATEType): kotlin.Boolean = type.actualType.let { it is Any || it is Int }

    }

    class Double : KATEType() {

        override fun getPlaceholderName(): kotlin.String = "double"

        override fun getKATEType(): kotlin.String = "double"

        override fun satisfies(type: KATEType): kotlin.Boolean = type.actualType.let { it is Any || it is Double }

    }

    class Long : KATEType() {

        override fun getPlaceholderName(): kotlin.String = "long"

        override fun getKATEType(): kotlin.String = "long"

        override fun satisfies(type: KATEType): kotlin.Boolean = type.actualType.let { it is Any || it is Long }

    }

    class Boolean : KATEType() {

        override fun getPlaceholderName(): kotlin.String = "boolean"

        override fun getKATEType(): kotlin.String = "boolean"

        override fun satisfies(type: KATEType): kotlin.Boolean = type.actualType.let { it is Any || it is Boolean }

    }

    open class List(val itemType: KATEType) : KATEType() {

        override fun getPlaceholderName(): kotlin.String = "list"

        override fun getKATEType(): kotlin.String = "list<${itemType.getKATEType()}>"

        override fun satisfies(type: KATEType): kotlin.Boolean = type.actualType.let { it is Any || it is List }

    }

    class MutableList(itemType: KATEType) : List(itemType) {

        override fun getPlaceholderName(): kotlin.String = "mutable_list"

        override fun getKATEType(): kotlin.String = "mutable_list<${itemType.getKATEType()}>"

        override fun satisfies(type: KATEType): kotlin.Boolean = type.actualType.let { it is Any || it is MutableList }

    }

    class Object(val itemType: KATEType) : KATEType() {

        override fun getPlaceholderName(): kotlin.String = "object"

        override fun getKATEType(): kotlin.String = "object<${itemType.getKATEType()}>"

        override fun satisfies(type: KATEType): kotlin.Boolean = type.actualType.let { it is Any || it is Object }

    }

}