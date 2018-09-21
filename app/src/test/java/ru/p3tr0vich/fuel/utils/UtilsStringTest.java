package ru.p3tr0vich.fuel.utils;

import android.support.annotation.NonNull;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilsStringTest {

    @NonNull
    private String doEncodeDecode(String s) {
        return UtilsString.decodeLineBreaks(UtilsString.encodeLineBreaks(s));
    }

    private void checkAssert(String s) throws AssertionError {
        assertEquals(s, s, doEncodeDecode(s));
    }

    @Test
    public void testEncodeDecode() throws AssertionError {
        checkAssert("\n");

        checkAssert("\\n");

        checkAssert("xxx\nyyy");

        checkAssert("xxx\\nyyy");

        checkAssert("xxx\\n\nyyy");
    }
}