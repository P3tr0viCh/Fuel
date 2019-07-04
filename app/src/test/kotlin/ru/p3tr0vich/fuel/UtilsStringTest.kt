package ru.p3tr0vich.fuel

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.p3tr0vich.fuel.utils.UtilsString

class UtilsStringTest {

    private fun doEncodeDecode(s: String): String {
        return UtilsString.decodeLineBreaks(UtilsString.encodeLineBreaks(s))
    }

    @Throws(AssertionError::class)
    private fun checkAssert(s: String) {
        assertEquals(s, s, doEncodeDecode(s))
    }

    @Test
    @Throws(AssertionError::class)
    fun testEncodeDecode() {
        checkAssert("\n")

        checkAssert("\\n")

        checkAssert("xxx\nyyy")

        checkAssert("xxx\\nyyy")

        checkAssert("xxx\\n\nyyy")
    }
}