package com.wakaztahir.kte.model

import com.wakaztahir.kte.dsl.ModelProvider
import com.wakaztahir.kte.parser.stream.DestinationStream
import com.wakaztahir.kte.parser.stream.SourceStream


internal enum class ConditionType {

    Equals {
        override fun verifyCompare(result: Int) = result == 0
    },
    NotEquals {
        override fun verifyCompare(result: Int) = result != 0
    },
    GreaterThan {
        override fun verifyCompare(result: Int) = result == 1
    },
    LessThan {
        override fun verifyCompare(result: Int) = result == -1
    },
    GreaterThanEqualTo {
        override fun verifyCompare(result: Int) = result == 1 || result == 0
    },
    LessThanEqualTo {
        override fun verifyCompare(result: Int) = result == -1 || result == 0
    };

    abstract fun verifyCompare(result: Int): Boolean

}

interface Condition {
    fun evaluate(context: ModelProvider): Boolean
}

internal class LogicalCondition(
    val propertyFirst: DynamicProperty,
    val type: ConditionType,
    val propertySecond: DynamicProperty
) : Condition {
    override fun evaluate(context: ModelProvider): Boolean {
        return type.verifyCompare(propertyFirst.getValue(context).compareAny(propertySecond.getValue(context)))
    }
}

internal class ReferencedBoolean(val value: DynamicProperty) : Condition {
    override fun evaluate(context: ModelProvider): Boolean {
        val value = value.getValue(context)
        if (value is BooleanValue) {
            return value.value
        } else {
            throw IllegalStateException("referenced value is not a boolean value inside the conditions")
        }
    }
}

internal class EvaluatedCondition(val value: Boolean) : Condition {
    override fun evaluate(context: ModelProvider): Boolean {
        return value
    }
}

enum class IfType(val order: Int) {
    If(0),
    ElseIf(1),
    Else(2)
}

internal class SingleIf(
    val condition: Condition,
    val type: IfType,
    val blockValue: LazyBlockSlice,
) : CodeGen {
    override fun generateTo(block: LazyBlock, source: SourceStream, destination: DestinationStream) {
        destination.write(blockValue.getValueAsString(source))
    }
}


internal class IfStatement(private val ifs: MutableList<SingleIf>) : AtDirective {

    val singleIfs: List<SingleIf> get() = ifs

    private fun sortByOrder() {
        ifs.sortBy { it.type.order }
    }

    fun evaluate(context: ModelProvider): SingleIf? {
        sortByOrder()
        for (iffy in ifs) {
            if (iffy.condition.evaluate(context)) {
                return iffy
            }
        }
        return null
    }

    override fun generateTo(block: LazyBlock, source: SourceStream, destination: DestinationStream) {
        evaluate(block.model)?.generateTo(block,source,destination)
    }
}