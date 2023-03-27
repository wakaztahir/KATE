package com.wakaztahir.kte.parser.stream

import com.wakaztahir.kte.model.LazyBlock
import com.wakaztahir.kte.model.PlaceholderBlock

abstract class SourceStream : LazyBlock {

    protected object NoEmbeddings : EmbeddingManager {
        override val embeddedStreams: MutableMap<String, Boolean> = mutableMapOf()
        override fun provideStream(block: LazyBlock, path: String): SourceStream? {
            return null
        }
    }

    protected class DefaultPlaceholderManager : PlaceholderManager {
        override val placeholders: MutableList<PlaceholderBlock> = mutableListOf()
        override val undefinedPlaceholders: MutableList<PlaceholderBlock> = mutableListOf()
        override val placeholderListeners = mutableMapOf<String, PlaceholderManager.PlaceholderEventListener>()
    }

    abstract val embeddingManager: EmbeddingManager
    abstract val placeholderManager: PlaceholderManager

    override val source: SourceStream
        get() = this

    override val allowTextOut: Boolean
        get() = true

    abstract val pointer: Int

    abstract val currentChar: Char

    abstract val hasEnded: Boolean

    abstract fun incrementPointer(): Boolean

    abstract fun decrementPointer(decrease: Int = 1): Boolean

    abstract fun setPointerAt(position: Int): Boolean

    override fun canIterate(): Boolean = !hasEnded

}