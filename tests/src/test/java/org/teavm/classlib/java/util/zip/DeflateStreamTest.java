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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class DeflateStreamTest {
    private static final String longString = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, "
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. "
            + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea "
            + "commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse "
            + "cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, "
            + "sunt in culpa qui officia deserunt mollit anim id est laborum.";

    @Test
    public void gzipInputWorks() throws IOException {
        byte[] data = readHex("1f8b08086c1d59540003746561766d2d7a6970000b494d0cf355708ff20c5028cf2fca2e5648cbcc"
                + "4b05005727a59115000000");
        GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(data));
        byte[] uncompressed = new byte[500];
        int length = readFully(input, uncompressed);
        assertEquals(21, length);
        assertEquals("TeaVM GZIP works fine", new String(uncompressed, 0, 21));
    }

    @Test
    public void zipInputWorks() throws IOException {
        byte[] data = readHex("504b03040a00000000002e7c695220303a36060000000600000008000000746573742e74787468656c6c"
                + "6f0a504b01023f030a00000000002e7c695220303a360600000006000000080000000000000000000000b48100000000"
                + "746573742e747874504b05060000000001000100360000002c0000000000");
        ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(data));
        ZipEntry entry = input.getNextEntry();
        assertEquals("test.txt", entry.getName());
        byte[] uncompressed = new byte[500];
        int length = readFully(input, uncompressed);
        assertEquals("hello\n", new String(uncompressed, 0, length));
        assertNull(input.getNextEntry());
    }

    @Test
    public void zipOutputWorks() throws IOException {
        ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
        ZipOutputStream zipOutput = new ZipOutputStream(byteArrayOutput);
        zipOutput.putNextEntry(new ZipEntry("test.txt"));
        zipOutput.write(longString.getBytes(StandardCharsets.UTF_8));
        zipOutput.close();
        byte[] data = byteArrayOutput.toByteArray();

        ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(data));
        ZipEntry entry = input.getNextEntry();
        assertEquals("test.txt", entry.getName());
        byte[] uncompressed = new byte[5000];
        int length = readFully(input, uncompressed);
        assertEquals(longString, new String(uncompressed, 0, length, StandardCharsets.UTF_8));
        assertNull(input.getNextEntry());
    }

    private static int readFully(InputStream input, byte[] target) throws IOException {
        int offset = 0;
        while (true) {
            int read = input.read(target, offset, target.length - offset);
            if (read <= 0) {
                break;
            }
            offset += read;
        }
        return offset;
    }

    private static byte[] readHex(String hex) {
        byte[] data = new byte[hex.length() / 2];
        for (int i = 0; i < data.length; ++i) {
            int h = Character.digit(hex.charAt(i * 2), 16);
            int l = Character.digit(hex.charAt(i * 2 + 1), 16);
            data[i] = (byte) ((h << 4) | l);
        }
        return data;
    }
}
