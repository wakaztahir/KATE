package com.wakaztahir.kte.model

class RawBlock(val value: String) : CodeGen {
    override fun generateCode(): String {
        return value
    }
}