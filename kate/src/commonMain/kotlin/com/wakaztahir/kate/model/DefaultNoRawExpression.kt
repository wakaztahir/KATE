package com.wakaztahir.kate.model

import com.wakaztahir.kate.model.model.MutableKATEObject
import com.wakaztahir.kate.model.model.ReferencedOrDirectValue
import com.wakaztahir.kate.parser.stream.DestinationStream
import com.wakaztahir.kate.parser.stream.SourceStream
import com.wakaztahir.kate.tokenizer.NodeTokenizer

class DefaultNoRawExpression(
    val source: SourceStream,
    val value: ReferencedOrDirectValue,
    val model : MutableKATEObject,
) : CodeGen {

    override fun <T> selectNode(tokenizer: NodeTokenizer<T>): T = tokenizer.defaultNoRawExpression

    override fun generateTo(destination: DestinationStream) {
        val value = value.getKATEValue()
        PlaceholderInvocation(
            placeholderName = value.getKnownKATEType().getPlaceholderName(),
            definitionName = null,
            paramValue = value,
            placeholderManager = source.placeholderManager,
            invocationModel = model,
        ).generateTo(destination = destination)
    }

}