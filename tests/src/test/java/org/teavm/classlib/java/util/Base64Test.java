/*
 *  Copyright 2023 ihromant.
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
package org.teavm.classlib.java.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class Base64Test {
    @Test
    public void decoderWorks() {
        assertEquals("q", decode("cQ=="));
        assertEquals("qw", decode("cXc="));
        assertEquals("qwe", decode("cXdl"));
        assertEquals("qwer", decode("cXdlcg=="));
        assertEquals("qwert", decode("cXdlcnQ="));
        assertEquals("qwerty", decode("cXdlcnR5"));
        assertEquals("qwertyu", decode("cXdlcnR5dQ=="));
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
    public void urlDecoderWorks() {
        assertEquals("q", decode("cQ"));
        assertEquals("qw", decode("cXc"));
        assertEquals("qwe", decode("cXdl"));
        assertEquals("qwer", decode("cXdlcg"));
        assertEquals("qwerty", decode("cXdlcnR5"));
        assertEquals("qwertyu", decode("cXdlcnR5dQ"));
        assertEquals("юзернейм:пароль", decode("0Y7Qt9C10YDQvdC10LnQvDrQv9Cw0YDQvtC70Yw"));
    }

    @Test
    public void urlEncoderWorks() {
        assertEquals("cQ", encodeNoPad("q"));
        assertEquals("cXc", encodeNoPad("qw"));
        assertEquals("cXdl", encodeNoPad("qwe"));
        assertEquals("cXdlcg", encodeNoPad("qwer"));
        assertEquals("cXdlcnQ", encodeNoPad("qwert"));
        assertEquals("cXdlcnR5", encodeNoPad("qwerty"));
        assertEquals("cXdlcnR5dQ", encodeNoPad("qwertyu"));
        assertEquals("0Y7Qt9C10YDQvdC10LnQvDrQv9Cw0YDQvtC70Yw", encodeNoPad("юзернейм:пароль"));
    }

    @Test
    public void decoderRejectsNonAsciiInput() {
        var bytes = new byte[][] { { 'Q', 'U', (byte) 0x80, 'A' }, { 'Q', 'U', (byte) 0xFF, 'A' } };
        for (var text : bytes) {
            assertThrows(IllegalArgumentException.class, () -> Base64.getDecoder().decode(text));
            assertThrows(IllegalArgumentException.class, () -> Base64.getUrlDecoder().decode(text));
        }
        for (var text : new String[] { "QU\u00E9A", "QU\u0142B" }) {
            assertThrows(IllegalArgumentException.class, () -> Base64.getDecoder().decode(text), text);
        }
    }

    @Test
    public void decoderRejectsIllegalCharacters() {
        for (var text : new String[] { "QU*A", "QU A", "QU=A", "QU_A", "QU-A", "QQ==QQ==" }) {
            assertThrows(IllegalArgumentException.class, () -> Base64.getDecoder().decode(text), text);
        }
        for (var text : new String[] { "QU+A", "QU/A" }) {
            assertThrows(IllegalArgumentException.class, () -> Base64.getUrlDecoder().decode(text), text);
        }
    }

    @Test
    public void decoderRejectsWrongPadding() {
        for (var text : new String[] { "QUJDR", "QUJDR===", "QQ=", "QQ===", "QUI==", "QUJD=", "QUJD====", "==" }) {
            assertThrows(IllegalArgumentException.class, () -> Base64.getDecoder().decode(text), text);
        }
    }

    private String decode(String text) {
        return new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
    }

    private String encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private String encodeNoPad(String text) {
        return Base64.getEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }
}
