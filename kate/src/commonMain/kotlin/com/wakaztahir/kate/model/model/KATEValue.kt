package com.wakaztahir.kate.model.model

import com.wakaztahir.kate.model.KATEType
import com.wakaztahir.kate.model.ModelReference
import com.wakaztahir.kate.parser.ArithmeticOperatorType

interface KATEValue : ReferencedOrDirectValue {

    fun getModelReference(reference: ModelReference): KATEValue?

    fun getKnownKATEType(): KATEType

    override fun getKATEValue(model: KATEObject): KATEValue = this

    override fun toString(): String

    fun operate(operator: ArithmeticOperatorType, other: KATEValue): KATEValue

    fun compareTo(other: KATEValue): Int

}