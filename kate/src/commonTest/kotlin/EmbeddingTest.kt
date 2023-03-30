import com.wakaztahir.kate.TemplateContext
import com.wakaztahir.kate.model.asPrimitive
import com.wakaztahir.kate.parser.parseVariableDeclaration
import com.wakaztahir.kate.parser.parseEmbedding
import com.wakaztahir.kate.parser.stream.TextSourceStream
import kotlin.test.Test
import kotlin.test.assertEquals

class EmbeddingTest {
    @Test
    fun testParseEmbeddingPath() {
        val context = TemplateContext("@embed ./current.kte")
        val path = context.stream.parseEmbedding()!!.path
        assertEquals("./current.kte", path)
        context.embedStream(path, TextSourceStream("@var var1 = \"hello-world\"",))
        val ref = context.getEmbeddedStream(path)!!.parseVariableDeclaration()!!
        assertEquals("var1", ref.variableName)
        assertEquals("hello-world", ref.variableValue.asPrimitive(context.stream.model).value)
    }
}