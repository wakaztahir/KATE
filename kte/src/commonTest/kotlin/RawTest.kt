import com.wakaztahir.kte.TemplateContext
import com.wakaztahir.kte.parser.parseRawBlock
import com.wakaztahir.kte.parser.stream.TextStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RawTest {
    @Test
    fun testRawBlock(){
        val context = TemplateContext(TextStream("@raw there's something raw here @endraw"))
        val block = context.parseRawBlock()
        assertNotEquals(null,block)
        assertEquals(" there's something raw here ",block!!.value)
    }
}