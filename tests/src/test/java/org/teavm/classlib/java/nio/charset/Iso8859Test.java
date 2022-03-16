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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class Iso8859Test {
    private static String hex = CharsetTestCommon.bytesToHex(convert(CharsetTestCommon.asciiText));

    private static byte[] convert(String value) {
        byte[] result = new byte[value.length()];
        for (int i = 0; i < value.length(); ++i) {
            result[i] = (byte) value.charAt(i);
        }
        return result;
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
    public void encodeUnmappable() {
        Charset charset = StandardCharsets.ISO_8859_1;
        CharsetTestCommon.checkUnmappable(charset, "ц", 1);
        CharsetTestCommon.checkUnmappable(charset, "\uD800\uDC00b", 2);
    }

    @Test
    public void decodeNonAscii() {
        assertEquals("£", new String(new byte[] { (byte) 0xA3 }, StandardCharsets.ISO_8859_1));
    }

    @Test
    public void encodeNonAscii() {
        assertArrayEquals(new byte[] { (byte) 0xA3 }, "£".getBytes(StandardCharsets.ISO_8859_1));
    }

    private void runEncode(int inSize, int outSize) {
        CharsetTestCommon.runEncode(hex, CharsetTestCommon.asciiText, StandardCharsets.ISO_8859_1, inSize, outSize);
    }

    private void runDecode(int inSize, int outSize) {
        CharsetTestCommon.runDecode(hex, CharsetTestCommon.asciiText, StandardCharsets.ISO_8859_1, inSize, outSize);
    }
}
