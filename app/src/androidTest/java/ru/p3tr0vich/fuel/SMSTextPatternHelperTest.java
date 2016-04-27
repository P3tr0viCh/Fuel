package ru.p3tr0vich.fuel;

import android.support.annotation.Nullable;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.regex.PatternSyntaxException;

public class SMSTextPatternHelperTest extends InstrumentationTestCase {

    private String pattern;

    @Nullable
    private Float check(@Nullable String message) throws PatternSyntaxException {
        return SMSTextPatternHelper.getValue(pattern, message);
    }

    @SmallTest
    public void testPositive() {
        pattern = "\\\\(яяя|ююю) \\@ \\(xxx\\) @yyy \\(zz[z]+";

        assertEquals(123.456f, check("\\яяя @ (xxx) 123.456yyy (zz[z]+"));
        assertEquals(123.456f, check("\\яяя @ (XXX) 123,456yyy (zz[z]+"));
        assertEquals(123.456f, check("\\ююю @ (xxx) 123.456yyy (zz[z]+"));
        assertEquals(123.456f, check("\\яяя1234 @ (xxx) 123.456yyy (zz[z]+"));
        assertEquals(123.456f, check("ggg \\яяя @ (xxx) 123.456yyy (zz[z]+ jjj"));
        assertEquals(123.456f, check("qwq \\яяяe232 @ (xxx)ccc 123.456yyy (zz[z]+"));
        assertEquals(0.456f, check("\\яяя @ (xxx) .456yyy (zz[z]+"));
        assertEquals(123.0f, check("\\яяя @ (xxx) 123yyy (zz[z]+"));

        pattern = "xxx(яяя|)@yyy";

        assertEquals(123.456f, check("xxxяяя123.456yyy"));
        assertEquals(123.456f, check("xxx123.456YYY"));
    }

    @SmallTest
    public void testNegative() {
        pattern = "\\\\(яяя|ююю) \\@ \\(xxx\\) @yyy \\(zz[z]+";

        // В начале эээ вместо яяя или ююю
        assertNull(check("\\эээ @ (xxx) 123.456yyy (zz[z]+"));
        // Нет скобок у xxx
        assertNull(check("\\ююю @ xxx 123.456yyy (zz[z]+"));
        // Нет числа
        assertNull(check("\\яяя @ (xxx) yyy (zz[z]+"));
        // Между числом и yyy точка, 123. не равно 123.0
        assertNull(check("\\яяя @ (xxx) 123.yyy (zz[z]+"));
        // Нет слэша перед яяя
        assertNull(check("яяя @ (xxx) 123.456yyy (zz[z]+ kkk"));
        // Нет @ между яяя и (xxx)
        assertNull(check("\\яяя (xxx) 123.456yyy (zz[z]+"));
        // Пробел между числом и yyy
        assertNull(check("\\яяя @ (xxx) 123.456 yyy (zz[z]+"));
        // Нет \яяя в начале выражения
        assertNull(check(" @ (xxx) 123.456yyy (zz[z]+"));
        // Нет (zz[z]+ в конце выражения
        assertNull(check("\\яяя @ (xxx) 123.456yyy"));

        pattern = "xxx(яяя|)@yyy";

        // Между xxx и числом должно быть или эээ или ничего
        assertNull(check("xxxэээ123.456yyy"));
    }


    @SmallTest
    public void testException() {
        // Нет закрывающей скобки
        pattern = "(xxx|yyy @zzz";

        try {
            check("xxx");
            fail();
        } catch (Throwable e) {
            assertTrue(e instanceof PatternSyntaxException);
        }

        // Нет открывающей скобки
        pattern = "xxx|yyy) @zzz";

        try {
            check("xxx");
            fail();
        } catch (Throwable e) {
            assertTrue(e instanceof PatternSyntaxException);
        }
    }
}