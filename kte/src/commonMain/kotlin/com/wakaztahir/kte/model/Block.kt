package com.wakaztahir.kte.model

import com.wakaztahir.kte.dsl.ModelDsl
import com.wakaztahir.kte.dsl.ModelObject
import com.wakaztahir.kte.dsl.ScopedModelObject
import com.wakaztahir.kte.dsl.TemplateModel
import com.wakaztahir.kte.parser.stream.SourceStream

interface LazyBlock {

    val model: TemplateModel

    fun canIterate(stream: SourceStream): Boolean

}


class LazyBlockSlice(
    val startPointer: Int,
    val length: Int,
    parent: TemplateModel
) : LazyBlock {

    override val model: TemplateModel = ScopedModelObject(parent = parent, ModelObject())

    override fun canIterate(stream: SourceStream): Boolean {
        return stream.pointer < startPointer + length
    }

    fun getValueAsString(stream: SourceStream): String {
        val previous = stream.pointer
        stream.setPointerAt(startPointer)
        var text = ""
        while (stream.pointer < startPointer + length) {
            text += stream.currentChar
            stream.incrementPointer()
        }
        stream.setPointerAt(previous)
        return text
    }
}