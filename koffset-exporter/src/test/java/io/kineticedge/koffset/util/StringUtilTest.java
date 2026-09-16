package io.kineticedge.koffset.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilTest {

    @Test
    void testIsBlank() {
        assertTrue(StringUtil.isBlank(null));
        assertTrue(StringUtil.isBlank(""));
        assertTrue(StringUtil.isBlank(" "));
        assertTrue(StringUtil.isBlank("\t"));
        assertTrue(StringUtil.isBlank("\n"));
        assertTrue(StringUtil.isBlank("\r"));

        assertFalse(StringUtil.isBlank("a"));
        assertFalse(StringUtil.isBlank(" a"));
        assertFalse(StringUtil.isBlank(" a "));
    }

    @Test
    void testIsNotBlank() {
        assertFalse(StringUtil.isNotBlank(null));
        assertFalse(StringUtil.isNotBlank(""));
        assertFalse(StringUtil.isNotBlank(" "));
        assertFalse(StringUtil.isNotBlank("\t"));
        assertFalse(StringUtil.isNotBlank("\n"));
        assertFalse(StringUtil.isNotBlank("\r"));

        assertTrue(StringUtil.isNotBlank("a"));
        assertTrue(StringUtil.isNotBlank(" a"));
        assertTrue(StringUtil.isNotBlank(" a "));
    }

}