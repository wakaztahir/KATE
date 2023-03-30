package com.wakaztahir.kate.parser

import com.wakaztahir.kate.dsl.ScopedModelObject
import com.wakaztahir.kate.model.*
import com.wakaztahir.kate.model.ConditionType
import com.wakaztahir.kate.model.model.*
import com.wakaztahir.kate.parser.stream.*
import com.wakaztahir.kate.parser.stream.increment
import com.wakaztahir.kate.parser.stream.parseTextWhile

internal sealed interface ForLoop : BlockContainer {

    val blockValue: LazyBlockSlice

    val model: MutableKTEObject
        get() = blockValue.model

    override fun getBlockValue(model: KTEObject): LazyBlock {
        return blockValue
    }

    fun iterate(block: (iteration: Int) -> Unit)

    override fun generateTo(block: LazyBlock, destination: DestinationStream) {
        iterate {
            blockValue.generateTo(destination = destination)
        }
    }

    class ConditionalFor(
        val condition: Condition,
        override val blockValue: LazyBlockSlice
    ) : ForLoop {
        override fun iterate(block: (iteration: Int) -> Unit) {
            var i = 0
            while (condition.evaluate(model)) {
                block(i)
                i++
            }
        }
    }

    class IterableFor(
        val indexConstName: String?,
        val elementConstName: String,
        val listProperty: ReferencedValue,
        override val blockValue: LazyBlockSlice
    ) : ForLoop {

        private fun store(value: Int) {
            if (indexConstName != null) {
                model.putValue(indexConstName, value)
            }
        }

        private fun store(value: KTEValue) {
            model.putValue(elementConstName, value)
        }

        private fun remove() {
            if (indexConstName != null) {
                model.removeKey(indexConstName)
            }
            model.removeKey(elementConstName)
        }

        override fun iterate(block: (iteration: Int) -> Unit) {
            var index = 0
            val iterable = listProperty.asNullableList(model)
                ?: throw IllegalStateException("list property is not iterable in for loop")
            val total = iterable.collection.size
            while (index < total) {
                store(index)
                store(iterable.collection.getOrElse(index) {
                    throw IllegalStateException("element at $index in for loop not found")
                })
                block(index)
                index++
            }
            remove()
        }
    }

    class NumberedFor(
        val variableName: String,
        val initializer: ReferencedValue,
        val conditionType: ConditionType,
        val conditional: ReferencedValue,
        val arithmeticOperatorType: ArithmeticOperatorType,
        val incrementer: ReferencedValue,
        override val blockValue: LazyBlockSlice
    ) : ForLoop {

        private fun ReferencedValue.intVal(context: MutableKTEObject): Int {
            (asNullablePrimitive(context) as? IntValue)?.value?.let { return it }
                ?: throw IllegalStateException("for loop variable must be an integer")
        }

        private fun MutableKTEObject.storeIndex(value: Int) {
            putValue(variableName, value)
        }

        private fun MutableKTEObject.removeIndex() {
            removeKey(variableName)
        }

        override fun iterate(block: (iteration: Int) -> Unit) {
            var i = initializer.intVal(model)
            val conditionValue = conditional.intVal(model)
            val incrementerValue = incrementer.intVal(model)
            while (conditionType.verifyCompare(i.compareTo(conditionValue))) {
                model.storeIndex(i)
                block(i)
                i = arithmeticOperatorType.operate(i, incrementerValue)
            }
            model.removeIndex()
        }
    }

}

private class ForLoopLazyBlockSlice(
    parentBlock: LazyBlock,
    startPointer: Int,
    length: Int,
    blockEndPointer: Int,
    parent: ScopedModelObject,
    allowTextOut: Boolean,
    indentationLevel: Int
) : LazyBlockSlice(
    parentBlock = parentBlock,
    startPointer = startPointer,
    length = length,
    model = parent,
    blockEndPointer = blockEndPointer,
    isWriteUnprocessedTextEnabled = allowTextOut,
    indentationLevel = indentationLevel
) {

    var hasBroken: Boolean = false

    override fun canIterate(): Boolean {
        return super.canIterate() && !hasBroken
    }

    fun SourceStream.parseBreakForAtDirective(): Boolean {
        return if (currentChar == '@' && increment("@breakfor")) {
            hasBroken = true
            true
        } else {
            false
        }
    }

    override fun parseNestedAtDirective(block: LazyBlock): CodeGen? {
        if (source.parseBreakForAtDirective()) return KTEUnit
        return super.parseNestedAtDirective(block)
    }

}

private fun LazyBlock.parseForBlockValue(): ForLoopLazyBlockSlice {
    val slice = parseBlockSlice(
        startsWith = "@for",
        endsWith = "@endfor",
        allowTextOut = isWriteUnprocessedTextEnabled,
        inheritModel = false
    )
    return ForLoopLazyBlockSlice(
        parentBlock = this,
        startPointer = slice.startPointer,
        length = slice.length,
        blockEndPointer = slice.blockEndPointer,
        parent = slice.model as ScopedModelObject,
        allowTextOut = slice.isWriteUnprocessedTextEnabled,
        indentationLevel = indentationLevel + 1
    )
}

private fun LazyBlock.parseConditionalFor(): ForLoop.ConditionalFor? {
    val condition = source.parseCondition()
    if (condition != null) {
        source.increment(')')
        source.increment(' ')
        val blockValue = this@parseConditionalFor.parseForBlockValue()
        return ForLoop.ConditionalFor(
            condition = condition,
            blockValue = blockValue
        )
    }
    return null
}

private fun SourceStream.parseListReferencedValue(): ReferencedValue? {
    parseVariableReference()?.let { return it }
    parseListDefinition()?.let { return it }
    parseMutableListDefinition()?.let { return it }
    return null
}

private fun LazyBlock.parseIterableForLoopAfterVariable(variableName: String): ForLoop.IterableFor? {
    var secondVariableName: String? = null
    if (source.increment(',')) {
        secondVariableName = source.parseTextWhile { currentChar.isVariableName() }
    }
    source.escapeSpaces()
    if (source.increment(':')) {
        source.escapeSpaces()
        val referencedValue = source.parseListReferencedValue()
        source.escapeSpaces()
        if (!source.increment(')')) {
            throw IllegalStateException("expected ) , got ${source.currentChar}")
        }
        source.increment(' ')
        val blockValue = this@parseIterableForLoopAfterVariable.parseForBlockValue()
        if (referencedValue != null) {
            return ForLoop.IterableFor(
                indexConstName = secondVariableName,
                elementConstName = variableName,
                listProperty = referencedValue,
                blockValue = blockValue
            )
        }
    }
    return null
}

private class NumberedForLoopIncrementer(
    val operatorType: ArithmeticOperatorType,
    val incrementerValue: ReferencedValue
)

private fun SourceStream.parseNumberOrReference(): ReferencedValue? {
    parseVariableReference()?.let { return it }
    parseNumberValue()?.let { return it }
    return null
}

private fun SourceStream.parseNumberedForLoopIncrementer(variableName: String): NumberedForLoopIncrementer {
    val incrementalConst = parseTextWhile { currentChar.isVariableName() }
    if (incrementalConst == variableName) {
        val operator = parseArithmeticOperator()
        if (operator != null) {
            val singleIncrement =
                if (operator == ArithmeticOperatorType.Plus || operator == ArithmeticOperatorType.Minus) operator else null
            val incrementer = if (singleIncrement != null && increment(singleIncrement.char)) {
                IntValue(1)
            } else {
                parseNumberOrReference()
            }
                ?: throw IllegalStateException("expected number property or value or '+' or '-' , got $currentChar in for loop incrementer")
            return NumberedForLoopIncrementer(
                operatorType = operator,
                incrementerValue = incrementer
            )
        } else {
            throw IllegalStateException("expected '+','-','/','*','%' , got $currentChar in for loop condition")
        }
    } else {
        throw IllegalStateException("incremental variable is different : $incrementalConst != $variableName")
    }
}

private fun LazyBlock.parseNumberedForLoopAfterVariable(variableName: String): ForLoop.NumberedFor? {
    if (source.increment('=')) {
        source.escapeSpaces()
        val initializer = source.parseAnyExpressionOrValue()
            ?: throw IllegalStateException("unexpected ${source.currentChar} , expected a number or property")
        if (source.increment(';')) {
            val conditionalConst = source.parseTextWhile { currentChar.isVariableName() }
            if (conditionalConst == variableName) {
                val conditionType = source.parseConditionType()
                    ?: throw IllegalStateException("expected conditional operator , got ${source.currentChar}")
                val conditional = source.parseAnyExpressionOrValue()
                    ?: throw IllegalStateException("expected number property of value got ${source.currentChar}")
                if (source.increment(';')) {
                    val incrementer = source.parseNumberedForLoopIncrementer(variableName)
                    if (!source.increment(')')) {
                        throw IllegalStateException("expected ) , got ${source.currentChar}")
                    }
                    source.increment(' ')
                    val blockValue = this@parseNumberedForLoopAfterVariable.parseForBlockValue()
                    return ForLoop.NumberedFor(
                        variableName = variableName,
                        initializer = initializer,
                        conditionType = conditionType,
                        conditional = conditional,
                        arithmeticOperatorType = incrementer.operatorType,
                        incrementer = incrementer.incrementerValue,
                        blockValue = blockValue
                    )
                } else {
                    throw IllegalStateException("unexpected ${source.currentChar} , expected ';'")
                }
            } else {
                throw IllegalStateException("conditional variable is different : $conditionalConst != $variableName")
            }
        } else {
            throw IllegalStateException("unexpected ${source.currentChar} , expected ';'")
        }
    }
    return null
}

internal fun LazyBlock.parseForLoop(): ForLoop? {
    if (source.currentChar == '@' && source.increment("@for(")) {

        parseConditionalFor()?.let { return it }

        val variableName = source.parseVariableName()
        if (variableName != null) {

            source.escapeSpaces()

            // Parsing the numbered loop
            parseNumberedForLoopAfterVariable(variableName)?.let { return it }

            // Parsing the iterable loop
            parseIterableForLoopAfterVariable(variableName)?.let { return it }

        }

    }
    return null
}