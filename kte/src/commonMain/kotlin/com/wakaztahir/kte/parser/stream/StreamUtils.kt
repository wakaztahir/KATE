package com.wakaztahir.kte.parser.stream

internal class UnexpectedEndOfStream(message: String) : Throwable(message)

internal fun SourceStream.unexpected(): UnexpectedEndOfStream {
    return UnexpectedEndOfStream("unexpected end of stream at pointer : $pointer")
}

internal fun SourceStream.unexpected(expected: String): UnexpectedEndOfStream {
    return UnexpectedEndOfStream("unexpected end of stream , expected $expected at pointer : $pointer")
}

internal fun SourceStream.increment(char: Char): Boolean {
    return if (!hasEnded && currentChar == char) {
        return incrementPointer()
    } else {
        false
    }
}

internal fun SourceStream.increment(str: String, throwOnUnexpectedEOS: Boolean = false): Boolean {
    require(str.length > 1) {
        println("$str should be more than a single character")
    }
    val previous = pointer
    while (!hasEnded && pointer - previous < str.length) {
        val current = pointer - previous
        if (str[current] != currentChar) {
            if (!decrementPointer(current)) {
                break
            }
            return false
        } else {
            if (!incrementPointer()) {
                break
            }
        }
    }
    val current = pointer - previous
    return if (current == str.length) {
        true
    } else {
        if (throwOnUnexpectedEOS) {
            throw UnexpectedEndOfStream("unexpected end of stream , expected $str")
        } else {
            decrementPointer(current)
            false
        }
    }
}

internal fun SourceStream.increment(
    until: () -> Boolean,
    stopIf: (Char) -> Boolean
): Boolean {
    val previous = pointer
    while (!hasEnded) {
        if (until()) {
            return true
        }
        if (stopIf(currentChar)) {
            decrementPointer(pointer - previous)
            return false
        }
        incrementPointer()
    }
    decrementPointer(pointer - previous)
    return false
}

internal fun SourceStream.incrementUntil(
    str: String,
    stopIf: (Char) -> Boolean = { false }
): Boolean {
    return increment(until = { currentChar == str[0] && increment(str) }, stopIf = stopIf)
}

internal inline fun <T> SourceStream.resetIfNull(perform: SourceStream.() -> T?): T? {
    val previous = pointer
    val value = perform()
    if (value == null) decrementPointer(pointer - previous)
    return value
}

internal inline fun <T> SourceStream.resetIfNullWithText(
    condition: () -> Boolean,
    perform: SourceStream.(String) -> T?
): T? {
    return resetIfNull {
        var text = ""
        while (!hasEnded && condition()) {
            text += currentChar
            incrementPointer()
        }
        perform(text)
    }
}

internal fun SourceStream.escapeSpaces() {
    if (increment(' ')) {
        escapeSpaces()
    }
}

internal inline fun SourceStream.parseTextWhile(block: SourceStream.() -> Boolean): String {
    var text = ""
    while (!hasEnded) {
        if (block()) {
            text += currentChar
        } else {
            return text
        }
        incrementPointer()
    }
    return text
}

internal fun SourceStream.printLeft() = resetIfNull {
    println(parseTextWhile { true })
    null
}

internal fun SourceStream.parseTextUntil(char: Char): String {
    return parseTextWhile { currentChar != char }
}

internal fun SourceStream.parseTextUntilConsumed(str: String): String {
    return parseTextWhile {
        currentChar != str[0] || !increment(str)
    }
}

internal fun SourceStream.parseTextUntil(vararg strings: String): String {
    return parseTextWhile {
        var unFound = true
        for (str in strings) {
            if (currentChar == str[0] && increment(str)) {
                decrementPointer(str.length)
                unFound = false
                break
            }
        }
        unFound
    }
}