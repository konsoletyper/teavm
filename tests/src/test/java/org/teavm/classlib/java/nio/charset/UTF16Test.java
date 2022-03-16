/*
 *  Copyright 2021 Alexey Andreev.
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
package org.teavm.classlib.java.nio.charset;

import static org.junit.Assert.assertEquals;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class UTF16Test {
    private static String hexLE = CharsetTestCommon.bytesToHex(convertLE(CharsetTestCommon.text));
    private static String hexBE = CharsetTestCommon.bytesToHex(convertBE(CharsetTestCommon.text));
    private static String hexLEBom = "FFFE" + hexLE;
    private static String hexBEBom = "FEFF" + hexBE;
    private static boolean littleEndian = "0".getBytes(StandardCharsets.UTF_16)[0] == 0xFF;

    private static byte[] convertLE(String text) {
        byte[] data = new byte[text.length() * 2];
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            data[i * 2] = (byte) (c & 0xFF);
            data[i * 2 + 1] = (byte) ((c >> 8) & 0xFF);
        }
        return data;
    }

    private static byte[] convertBE(String text) {
        byte[] data = new byte[text.length() * 2];
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            data[i * 2] = (byte) ((c >> 8) & 0xFF);
            data[i * 2 + 1] = (byte) (c & 0xFF);
        }
        return data;
    }

    @Test
    public void encode1() {
        runEncode(600, 600);
    }

    @Test
    public void encode2() {
        runEncode(600, 100);
    }

    @Test
    public void encode3() {
        runEncode(100, 600);
    }

    @Test
    public void encode4() {
        runEncode(600, 99);
    }

    @Test
    public void decode1() {
        runDecode(600, 600);
    }

    @Test
    public void decode2() {
        runDecode(600, 100);
    }

    @Test
    public void decode3() {
        runDecode(100, 600);
    }

    @Test
    public void decode4() {
        runDecode(99, 600);
    }

    @Test
    public void encodeMalformedSurrogate() {
        CharsetTestCommon.checkMalformed(StandardCharsets.UTF_16LE, "\uD800\uD800", 1);
        CharsetTestCommon.checkMalformed(StandardCharsets.UTF_16LE, "\uD800a", 1);
        CharsetTestCommon.checkMalformed(StandardCharsets.UTF_16LE, "\uDC00\uD800", 1);
    }

    @Test
    public void encodeSurrogate() {
        String hex = CharsetTestCommon.bytesToHex("\uD800\uDC00".getBytes(StandardCharsets.UTF_16BE));
        assertEquals("D800DC00", hex);
    }

    @Test
    public void decodeSurrogate() {
        String hex = CharsetTestCommon.bytesToHex("\uD800\uDC00".getBytes(StandardCharsets.UTF_16BE));
        assertEquals("D800DC00", hex);
    }

    @Test
    public void decodeMalformedSurrogate() {
        CharsetTestCommon.checkMalformed(StandardCharsets.UTF_16BE, CharsetTestCommon.hexToBytes("D800D800"), 4);
        CharsetTestCommon.checkMalformed(StandardCharsets.UTF_16BE, CharsetTestCommon.hexToBytes("D8000041"), 4);
        CharsetTestCommon.checkMalformed(StandardCharsets.UTF_16BE, CharsetTestCommon.hexToBytes("DC00D800"), 2);
    }

    private void runEncode(int inSize, int outSize) {
        CharsetTestCommon.runEncode(hexLE, CharsetTestCommon.text, StandardCharsets.UTF_16LE, inSize, outSize);
        CharsetTestCommon.runEncode(hexBE, CharsetTestCommon.text, StandardCharsets.UTF_16BE, inSize, outSize);
        CharsetTestCommon.runEncode(littleEndian ? hexLEBom : hexBEBom, CharsetTestCommon.text,
                StandardCharsets.UTF_16, inSize, outSize);
    }

    private void runDecode(int inSize, int outSize) {
        CharsetTestCommon.runDecode(hexLE, CharsetTestCommon.text, StandardCharsets.UTF_16LE, inSize, outSize);
        CharsetTestCommon.runDecode(hexBE, CharsetTestCommon.text, StandardCharsets.UTF_16BE, inSize, outSize);
        CharsetTestCommon.runDecode(hexBEBom, CharsetTestCommon.text, StandardCharsets.UTF_16, inSize, outSize);
        CharsetTestCommon.runDecode(hexLEBom, CharsetTestCommon.text, StandardCharsets.UTF_16, inSize, outSize);
        CharsetTestCommon.runDecode(hexBE, CharsetTestCommon.text, StandardCharsets.UTF_16, inSize, outSize);
    }
}
