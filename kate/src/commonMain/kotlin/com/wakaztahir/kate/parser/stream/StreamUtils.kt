package com.wakaztahir.kate.parser.stream

import com.wakaztahir.kate.model.LazyBlock

internal class UnexpectedEndOfStream(message: String) : Exception(message)

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

internal fun SourceStream.parseTextUntilConsumedNew(str: String): String? {
    var text = ""
    val previous = pointer
    while (!hasEnded) {
        if (currentChar == str[0] && increment(str)) {
            return text
        } else {
            text += currentChar
        }
        incrementPointer()
    }
    decrementPointer(pointer - previous)
    return null
}

internal fun SourceStream.incrementUntilConsumed(str: String): Boolean {
    val previous = pointer
    while (!hasEnded) {
        if (currentChar == str[0] && increment(str)) {
            return true
        }
        incrementPointer()
    }
    decrementPointer(pointer - previous)
    return false
}

internal fun SourceStream.incrementUntil(str: String): Boolean {
    return if (incrementUntilConsumed(str)) {
        decrementPointer(str.length)
        true
    } else {
        false
    }
}

internal inline fun <T> SourceStream.resetIfNull(perform: SourceStream.() -> T?): T? {
    val previous = pointer
    val value = perform()
    if (value == null) decrementPointer(pointer - previous)
    return value
}

internal fun SourceStream.escapeSpaces() {
    if (increment(' ')) escapeSpaces()
}

internal inline fun SourceStream.incrementWhile(block: SourceStream.() -> Boolean) {
    while (!hasEnded) {
        if (!block()) {
            break
        }
        incrementPointer()
    }
}

internal inline fun SourceStream.parseTextWhile(block: SourceStream.() -> Boolean): String {
    var text = ""
    incrementWhile {
        if (block()) {
            text += currentChar
            true
        } else {
            false
        }
    }
    return text
}

internal fun SourceStream.printLeft() = resetIfNull {
    println(parseTextWhile { true })
    null
}

internal fun SourceStream.printLeftAscii() = resetIfNull {
    for (char in parseTextWhile { true }) println("$char:${char.code}")
    null
}

internal fun SourceStream.parseTextUntilConsumed(str: String): String {
    return parseTextWhile {
        currentChar != str[0] || !increment(str)
    }
}

internal inline fun SourceStream.incrementUntilDirectiveWithSkip(
    skip: String,
    canIncrementDirective: (skips: Int) -> String?,
): String? {
    var skips = 0
    while (!hasEnded) {
        if (currentChar == '@') {
            if (increment(skip)) {
                skips++
                continue
            } else {
                val incremented = canIncrementDirective(skips)
                if (incremented != null) {
                    if (skips == 0) {
                        return incremented
                    } else {
                        skips--
                        continue
                    }
                }
            }
        }
        incrementPointer()
    }
    return null
}

inline fun SourceStream.readStream(startPointer: Int, limit: Int, block: () -> Unit) {
    val previous = pointer
    setPointerAt(startPointer)
    while (!hasEnded && pointer < limit) {
        block()
        incrementPointer()
    }
    setPointerAt(previous)
}

fun SourceStream.getErrorInfoAtCurrentPointer(): Pair<Int, Int> {
    val pointerAt = pointer
    var lineNumber = 1
    var charIndex = 0
    readStream(0, pointerAt) {
        charIndex++
        if (currentChar == '\n') {
            lineNumber++
            charIndex = 0
        }
    }
    return Pair(lineNumber, charIndex)
}

fun SourceStream.printErrorLineNumberAndCharacterIndex() {
    val errorInfo = getErrorInfoAtCurrentPointer()
    println("Error : Line Number : ${errorInfo.first} , Character Index : ${errorInfo.second}")
    println("Un-parsed Code : ")
    printLeft()
}

internal fun LazyBlock.escapeBlockSpacesForward() {

    val previous = source.pointer
    while (!source.hasEnded) {
        when (source.currentChar) {

            '\r' -> {
                source.incrementPointer()
                if (source.currentChar == '\n') source.incrementPointer()
                return
            }

            '\n' -> {
                source.incrementPointer()
                return
            }

            ' ' -> {
                source.incrementPointer()
                continue
            }

            else -> {
                break
            }
        }
    }

    source.setPointerAt(previous)
    if (source.currentChar == ' ') {
        source.incrementPointer()
        return
    }

}

internal fun LazyBlock.escapeBlockSpacesBackward() {

    val previous = source.pointer
    var currentIndentationLevel = indentationLevel
    while (!source.hasEnded) {
        source.decrementPointer()
        when (source.currentChar) {

            '\r' -> {
                return
            }

            '\n' -> {
                source.decrementPointer()
                if (source.currentChar != '\r') source.incrementPointer()
                return
            }

            ' ' -> {
                continue
            }

            '\t' -> {
                if (currentIndentationLevel > 0) {
                    currentIndentationLevel--
                } else {
                    return
                }
            }

            else -> {
                break
            }
        }
    }


    source.setPointerAt(previous)
    source.decrementPointer()
    if (source.currentChar != ' ') {
        source.incrementPointer()
        return
    }

}

internal fun String.escapeBlockSpacesBackward(indentationLevel: Int): String {
    var currentIndentationLevel = indentationLevel
    var i = length
    while (i > 0) {
        i--
        when (this[i]) {

            '\r' -> {
                return substring(0, i)
            }

            '\n' -> {
                if (this[i - 1] == '\r') i--
                return substring(0, i)
            }

            ' ' -> {
                continue
            }

            '\t' -> {
                if (currentIndentationLevel > 0) {
                    currentIndentationLevel--
                } else {
                    return substring(0, i)
                }
            }

            else -> {
                break
            }
        }
    }

    i = length - 1
    if (this[i] == ' ') {
        return substring(0, i)
    }

    return this

}