import com.wakaztahir.kte.TemplateContext
import com.wakaztahir.kte.model.ModelDirective
import com.wakaztahir.kte.model.ModelReference
import com.wakaztahir.kte.parser.parseDynamicProperty
import com.wakaztahir.kte.parser.parseModelDirective
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelDirectiveTest {

    private inline fun TemplateContext.testDirective(block: (ModelDirective) -> Unit) {
        val previous = stream.pointer
        block(stream.parseDynamicProperty()!!.getReferencedProperty()!! as ModelDirective)
        stream.decrementPointer(stream.pointer - previous)
        block(stream.parseModelDirective()!!)
    }

    @Test
    fun testParseModelDirective() {
        val context = TemplateContext("@model.firstProp.secondProp.@thirdCall().fourthProp.@fifthProp(true,false)")
        context.testDirective { directive ->
            assertEquals("firstProp", directive.propertyPath[0].name)
            assertEquals("secondProp", directive.propertyPath[1].name)
            assertEquals("thirdCall", directive.propertyPath[2].name)
            assertEquals("fourthProp", directive.propertyPath[3].name)
            val call = directive.propertyPath[4] as ModelReference.FunctionCall
            assertEquals("fifthProp", call.name)
            assertEquals(true, call.parametersList[0].getStoredValue()!!.value)
            assertEquals(false, call.parametersList[1].getStoredValue()!!.value)
        }
    }

}