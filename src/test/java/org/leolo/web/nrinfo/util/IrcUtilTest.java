package org.leolo.web.nrinfo.util;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.testng.Assert;
import scala.Tuple3;

import static org.junit.Assert.*;

public class IrcUtilTest {

    @Test
    public void testHostmask() {
        //Empty input
        assertNull(IrcUtil.splitHostmask(null));
        assertNull(IrcUtil.splitHostmask(""));
        assertNull(IrcUtil.splitHostmask("    "));
        assertNull(IrcUtil.splitHostmask("\t"));
        assertNull(IrcUtil.splitHostmask("  \t"));

        //Missing delim
        assertException(RuntimeException.class, ()->{IrcUtil.splitHostmask("test@host");}, "Incorrect format");
        assertException(RuntimeException.class, ()->{IrcUtil.splitHostmask("test!host");}, "Incorrect format");
        assertException(RuntimeException.class, ()->{IrcUtil.splitHostmask("testhost");}, "Incorrect format");

        //Incorrect order
        assertException(RuntimeException.class, ()->{IrcUtil.splitHostmask("test@host!nick");}, "Incorrect format");

        //Normal Case
        Tuple3<String, String, String> result = IrcUtil.splitHostmask("nick!ident@host");
        assertEquals("nick", result._1());
        assertEquals("ident", result._2());
        assertEquals("host", result._3());
        result = IrcUtil.splitHostmask("nick!~ident@host");
        assertEquals("nick", result._1());
        assertEquals("~ident", result._2());
        assertEquals("host", result._3());

        //Match masks
        result = IrcUtil.splitHostmask("*!*@host");
        assertEquals("*", result._1());
        assertEquals("*", result._2());
        assertEquals("host", result._3());
        result = IrcUtil.splitHostmask("*!~*@host");
        assertEquals("*", result._1());
        assertEquals("~*", result._2());
        assertEquals("host", result._3());
    }

    private <T extends Throwable> void assertException(Class<T> expectedThrowable, ThrowingRunnable runnable, String message) {
        Exception e = assertThrows(Exception.class, runnable);
        assertEquals(message, e.getMessage());
    }
}