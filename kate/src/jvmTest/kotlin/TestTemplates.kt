import com.wakaztahir.kate.InputSourceStream
import com.wakaztahir.kate.OutputDestinationStream
import com.wakaztahir.kate.TemplateContext
import com.wakaztahir.kate.model.model.MutableKTEObject
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TestTemplates {

    private fun getObject(): MutableKTEObject {
        return MutableKTEObject {
            putValue("mathTestClassName", "MathsClass")
            putObjects("arithmetic") {
                putObject {
                    putValue("funName", "sumsTwoVars")
                    putValue("first", 4)
                    putValue("second", 6)
                    putValue("symbolName", "+")
                    putValue("returnType", "Int")
                }
                putObject {
                    putValue("funName", "subtractTwoVars")
                    putValue("first", 6)
                    putValue("second", 4)
                    putValue("symbolName", "-")
                    putValue("returnType", "Int")
                }
                putObject {
                    putValue("funName", "multiplyTwoVars")
                    putValue("first", 4)
                    putValue("second", 6)
                    putValue("symbolName", "*")
                    putValue("returnType", "Int")
                }
                putObject {
                    putValue("funName", "divideTwoVars")
                    putValue("first", 4)
                    putValue("second", 2)
                    putValue("symbolName", "/")
                    putValue("returnType", "Int")
                }
            }
        }
    }

    private fun output(path: String): OutputDestinationStream {
        val file = File("src/jvmTest/resources/$path")
        println(file.absolutePath)
        val outputStream = file.outputStream()
        return OutputDestinationStream(outputStream)
    }

    @Test
    fun testInputSourceStream() {

        val input = object {}.javaClass.getResource("schema/main.kate")!!.openStream()
        val reader = input.bufferedReader()
        val text = reader.readText()
        reader.close()
        val sourceStream = InputSourceStream(
            inputStream = object {}.javaClass.getResource("schema/main.kate")!!.openStream(),
            model = getObject()
        )
        assertFalse(sourceStream.hasEnded)
        val other = sourceStream.getValueAsString(0)
        assertEquals(text, other)
    }

    private fun testTemplate(inputPath: String, outputPath: String) {
        val model = getObject()
        val embedding = InputSourceStream.RelativeResourceEmbeddingManager("schema", object {}.javaClass)
        val context = TemplateContext(
            stream = InputSourceStream(
                inputStream = object {}.javaClass.getResource("schema/$inputPath")!!.openStream(),
                model = model,
                embeddingManager = embedding,
            )
        )
        val output = output("output/$outputPath")
        context.generateTo(output)
        output.outputStream.close()
    }

    @Test
    fun testGenerateTemplates() {
        testTemplate("main.kate", "main.kt")
        testTemplate("test.kt.kate", "test.kt")
    }

}