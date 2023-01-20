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
package org.teavm.classlib.impl;

import static org.junit.Assert.*;
import java.io.UnsupportedEncodingException;
import org.junit.Test;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class Base64Test {
    @Test
    public void decoderWorks() {
        Decoder decoder = Base64.decoder();
        assertEquals("q", decoder.decode("cQ=="));
        assertEquals("qw", decoder.decode("cXc="));
        assertEquals("qwe", decoder.decode("cXdl"));
        assertEquals("qwer", decoder.decode("cXdlcg=="));
        assertEquals("qwert", decoder.decode("cXdlcnQ="));
        assertEquals("qwerty", decoder.decode("cXdlcnR5"));
        assertEquals("qwertyu", decoder.decode("cXdlcnR5dQ=="));
        assertEquals("юзернейм:пароль", decoder.decode("0Y7Qt9C10YDQvdC10LnQvDrQv9Cw0YDQvtC70Yw="));
    }

    @Test
    public void encoderWorks() {
        Encoder encoder = Base64.encoder();
        assertEquals("cQ==", encoder.encode("q"));
        assertEquals("cXc=", encoder.encode("qw"));
        assertEquals("cXdl", encoder.encode("qwe"));
        assertEquals("cXdlcg==", encoder.encode("qwer"));
        assertEquals("cXdlcnQ=", encoder.encode("qwert"));
        assertEquals("cXdlcnR5", encoder.encode("qwerty"));
        assertEquals("cXdlcnR5dQ==", encoder.encode("qwertyu"));
        assertEquals("0Y7Qt9C10YDQvdC10LnQvDrQv9Cw0YDQvtC70Yw=", encoder.encode("юзернейм:пароль"));
    }

    @Test
    public void urlDecoderWorks() {
        Decoder decoder = Base64.urlDecoder();
        assertEquals("q", decoder.decode("cQ"));
        assertEquals("qw", decoder.decode("cXc"));
        assertEquals("qwe", decoder.decode("cXdl"));
        assertEquals("qwer", decoder.decode("cXdlcg"));
        assertEquals("qwerty", decoder.decode("cXdlcnR5"));
        assertEquals("qwertyu", decoder.decode("cXdlcnR5dQ"));
        assertEquals("юзернейм:пароль", decoder.decode("0Y7Qt9C10YDQvdC10LnQvDrQv9Cw0YDQvtC70Yw"));
    }

    @Test
    public void urlEncoderWorks() {
        Encoder encoder = Base64.urlEncoder();
        assertEquals("cQ", encoder.encodeNoPad("q"));
        assertEquals("cXc", encoder.encodeNoPad("qw"));
        assertEquals("cXdl", encoder.encodeNoPad("qwe"));
        assertEquals("cXdlcg", encoder.encodeNoPad("qwer"));
        assertEquals("cXdlcnQ", encoder.encodeNoPad("qwert"));
        assertEquals("cXdlcnR5", encoder.encodeNoPad("qwerty"));
        assertEquals("cXdlcnR5dQ", encoder.encodeNoPad("qwertyu"));
        assertEquals("0Y7Qt9C10YDQvdC10LnQvDrQv9Cw0YDQvtC70Yw", encoder.encode("юзернейм:пароль"));
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
