import com.wakaztahir.kte.KTEDelicateFunction
import com.wakaztahir.kte.TemplateContext
import com.wakaztahir.kte.parser.parseRawBlock
import com.wakaztahir.kte.parser.stream.TextSourceStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RawTest {
    @Test
    fun testRawBlock() {
        val context = TemplateContext("@raw there's something raw here @endraw")
        val block = context.stream.parseRawBlock()
        assertNotEquals(null, block)
        assertEquals("there's something raw here", block!!.value)
        context.updateStream("@rawvalue@endraw")
        context.stream.parseRawBlock()
        assertEquals(16,context.stream.pointer)
    }
    @OptIn(KTEDelicateFunction::class)
    @Test
    fun testRawCodeGen() {
        val context = TemplateContext("@raw there's something raw here @endraw")
        assertEquals("there's something raw here",context.getDestinationAsString())
    }
}