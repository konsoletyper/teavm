/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.java.util.zip;

import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class GZIPInputStreamTest {
    @Test
    public void gzipInputWorks() throws IOException {
        String hex = "1f8b08086c1d59540003746561766d2d7a6970000b494d0cf355708ff20c5028cf2fca2e5648cbcc" +
                "4b05005727a59115000000";
        byte[] data = new byte[hex.length() / 2];
        for (int i = 0; i < data.length; ++i) {
            int h = Character.digit(hex.charAt(i * 2), 16);
            int l = Character.digit(hex.charAt(i * 2 + 1), 16);
            data[i] = (byte)((h << 4) | l);
        }
        GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(data));
        byte[] uncompressed = new byte[500];
        int offset = 0;
        while (true) {
            int read = input.read(uncompressed, offset, uncompressed.length - offset);
            if (read <= 0) {
                break;
            }
            offset += read;
        }
        assertEquals(21, offset);
        assertEquals("TeaVM GZIP works fine", new String(uncompressed, 0, 21));
    }
}
