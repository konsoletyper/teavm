/*
 *  Copyright 2015 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.impl;

import static org.junit.Assert.*;
import java.io.UnsupportedEncodingException;
import org.junit.Test;

public class Base64Test {
    @Test
    public void decoderWorks() {
        assertEquals("q", decode("cQ=="));
        assertEquals("q", decode("cQ"));
        assertEquals("qw", decode("cXc="));
        assertEquals("qw", decode("cXc"));
        assertEquals("qwe", decode("cXdl"));
        assertEquals("qwer", decode("cXdlcg=="));
        assertEquals("qwer", decode("cXdlcg"));
        assertEquals("qwert", decode("cXdlcnQ="));
        assertEquals("qwerty", decode("cXdlcnR5"));
        assertEquals("qwertyu", decode("cXdlcnR5dQ=="));
        assertEquals("qwertyu", decode("cXdlcnR5dQ"));
        assertEquals("юзернейм:пароль", decode("0Y7Qt9C10YDQvdC10LnQvDrQv9Cw0YDQvtC70Yw="));
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
        assertEquals("0Y7Qt9C10YDQvdC10LnQvDrQv9Cw0YDQvtC70Yw=", encode("юзернейм:пароль"));
    }

    @Test
    public void encoderNoPadWorks() {
        assertEquals("cQ", encodeNoPad("q"));
        assertEquals("cXc", encodeNoPad("qw"));
        assertEquals("cXdl", encodeNoPad("qwe"));
        assertEquals("cXdlcg", encodeNoPad("qwer"));
        assertEquals("cXdlcnQ", encodeNoPad("qwert"));
        assertEquals("cXdlcnR5", encodeNoPad("qwerty"));
        assertEquals("cXdlcnR5dQ", encodeNoPad("qwertyu"));
    }

    private String decode(String text) {
        try {
            return new String(Base64Impl.decode(text.getBytes("UTF-8")), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private String encode(String text) {
        try {
            return new String(Base64Impl.encode(text.getBytes("UTF-8"), true), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private String encodeNoPad(String text) {
        try {
            return new String(Base64Impl.encode(text.getBytes("UTF-8"), false), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }
}
