package com.wakaztahir.kate.model.model

import com.wakaztahir.kate.model.*

interface ReferencedValue : KATEValue {

    fun toPlaceholderInvocation(model: MutableKATEObject, endPointer: Int): PlaceholderInvocation? {
        val value = getKATEValue(model)
        val type = value.getKateType(model) ?: return null
        return PlaceholderInvocation(
            placeholderName = type,
            definitionName = null,
            paramValue = value,
            invocationEndPointer = endPointer
        )
    }

}