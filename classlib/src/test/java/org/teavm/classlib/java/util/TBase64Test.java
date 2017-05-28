package org.teavm.classlib.java.util;

import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertEquals;

public class TBase64Test {
    private final static String CHARSET = "UTF-8";

    @Test
    public void decoderWorks() {
        assertEquals("q", decode("cQ=="));
        assertEquals("qw", decode("cXc="));
        assertEquals("qwe", decode("cXdl"));
        assertEquals("qwer", decode("cXdlcg=="));
        assertEquals("qwert", decode("cXdlcnQ="));
        assertEquals("qwerty", decode("cXdlcnR5"));
        assertEquals("qwertyu", decode("cXdlcnR5dQ=="));
    }

    @Test
    public void encoderWorks() {
        assertEquals("cQ==", encode("q"));
        assertEquals("cXc=", encode("qw"));
        assertEquals("cXdl", encode("qwe"));
        assertEquals("cXdlcg==", encode("qwer"));
        assertEquals("cXdlcnQ=", encode("qwert"));
        assertEquals("cXdlcnR5", encode("qwerty"));
        assertEquals("cXdlcnR5dQ==", encode("qwertyu"));
    }

    private String decode(String text) {
        try {
            return new String(TBase64.getDecoder().decode(text), CHARSET);
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private String encode(String text) {
        try {
            return new String(TBase64.getEncoder().encode(text.getBytes(CHARSET)), CHARSET);
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }
}
