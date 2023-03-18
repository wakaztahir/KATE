package com.wakaztahir.kte.parser

import com.wakaztahir.kte.model.*
import com.wakaztahir.kte.parser.stream.*
import com.wakaztahir.kte.parser.stream.increment

fun SourceStream.parseDynamicValue(): DynamicValue<*>? {

    // Booleans
    if (increment("true")) return BooleanValue(true)
    if (increment("false")) return BooleanValue(false)

    // Floats & Ints
    resetIfNullWithText(
        condition = {
            currentChar == '.' || currentChar == 'f' || currentChar.isDigit()
        },
        perform = {
            if (it.contains('.')) {
                if (it.lastOrNull() == 'f') {
                    return@resetIfNullWithText it.substringBeforeLast('f').toFloatOrNull()
                        ?.let { value -> FloatValue(value) }
                } else {
                    return@resetIfNullWithText null
                }
            } else {
                return@resetIfNullWithText it.toIntOrNull()?.let { value -> IntValue(value) }
            }
        }
    )?.let { return it }

    // Strings
    parseStringValue()?.let { return it }

    return null

}

internal fun SourceStream.parseStringValue(): StringValue? {
    if (currentChar == '\"' && increment("\"")) {
        val value = StringValue(parseTextUntil('\"'))
        increment("\"")
        return value
    }
    return null
}