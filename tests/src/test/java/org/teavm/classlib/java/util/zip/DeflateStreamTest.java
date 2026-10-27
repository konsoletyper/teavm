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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.teavm.classlib.java.util.zip.ZipTestUtil.readHex;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
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
        var uncompressed = input.readAllBytes();
        assertEquals("hello\n", new String(uncompressed));
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
        var uncompressed = input.readAllBytes();
        assertEquals(longString, new String(uncompressed, StandardCharsets.UTF_8));
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

    // A second flush with nothing new to flush made zlib report Z_BUF_ERROR, which used to throw.
    @Test
    public void gzipFlushTwiceRoundTrips() throws IOException {
        var bytes = new ByteArrayOutputStream();
        var gzip = new GZIPOutputStream(bytes);
        var data = longString.getBytes(StandardCharsets.UTF_8);
        gzip.write(data, 0, 100);
        gzip.flush();
        gzip.flush();
        gzip.write(data, 100, data.length - 100);
        gzip.flush();
        gzip.close();
        var input = new GZIPInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        assertArrayEquals(data, input.readAllBytes());
    }

    @Test
    public void streamsRoundTripWithSmallBuffersAndOneByteReads() throws IOException {
        var data = (longString + longString).getBytes(StandardCharsets.UTF_8);
        for (int bufSize : new int[] { 1, 2, 7, 512 }) {
            var bytes = new ByteArrayOutputStream();
            var out = new DeflaterOutputStream(bytes, new Deflater(), bufSize);
            for (int i = 0; i < data.length; i += 13) {
                out.write(data, i, Math.min(13, data.length - i));
            }
            out.close();
            var in = new InflaterInputStream(new OneByteInputStream(bytes.toByteArray()), new Inflater(), bufSize);
            assertArrayEquals(data, in.readAllBytes(), "buffer " + bufSize);
        }
    }

    // A stream cut short at any position must end in EOFException, not loop or return a short result.
    @Test
    public void truncatedStreamsThrowAtEveryPosition() throws IOException {
        var data = longString.getBytes(StandardCharsets.UTF_8);
        var zlibBytes = new ByteArrayOutputStream();
        try (var out = new DeflaterOutputStream(zlibBytes)) {
            out.write(data);
        }
        var zlib = zlibBytes.toByteArray();
        for (int len = 0; len < zlib.length; len++) {
            var truncated = Arrays.copyOf(zlib, len);
            var in = new InflaterInputStream(new ByteArrayInputStream(truncated));
            assertThrows(EOFException.class, () -> readToEnd(in), "zlib length " + len);
        }

        var gzipBytes = new ByteArrayOutputStream();
        try (var out = new GZIPOutputStream(gzipBytes)) {
            out.write(data);
        }
        var gzip = gzipBytes.toByteArray();
        for (int len = 0; len < gzip.length; len++) {
            var truncated = Arrays.copyOf(gzip, len);
            assertThrows(EOFException.class, () -> readToEnd(new GZIPInputStream(new ByteArrayInputStream(truncated))),
                    "gzip length " + len);
        }

        var zipBytes = new ByteArrayOutputStream();
        try (var out = new ZipOutputStream(zipBytes)) {
            out.putNextEntry(new ZipEntry("test.txt"));
            out.write(data);
        }
        var zip = zipBytes.toByteArray();
        int dataStart = 30 + (zip[26] & 0xFF) + ((zip[27] & 0xFF) << 8) + (zip[28] & 0xFF) + ((zip[29] & 0xFF) << 8);
        // The entry's compressed length is what a raw inflater consumes before it finishes.
        var raw = new Inflater(true);
        raw.setInput(zip, dataStart, zip.length - dataStart);
        try {
            raw.inflate(new byte[data.length + 10]);
        } catch (DataFormatException e) {
            throw new AssertionError(e);
        }
        assertTrue(raw.finished());
        int dataEnd = dataStart + (int) raw.getBytesRead();
        for (int len = dataStart; len < dataEnd; len++) {
            var in = new ZipInputStream(new EndOnceInputStream(Arrays.copyOf(zip, len)));
            assertEquals("test.txt", in.getNextEntry().getName());
            assertThrows(EOFException.class, () -> readToEnd(in), "zip length " + len);
        }
    }

    // Reads to -1, and fails rather than spinning if read keeps returning 0.
    private static void readToEnd(InputStream in) throws IOException {
        var buf = new byte[64];
        int reads = 0;
        while (in.read(buf) != -1) {
            if (++reads > 10000) {
                throw new AssertionError("read made no progress");
            }
        }
    }

    @Test
    public void damagedZlibHeaderFailsTheStream() {
        var in = new InflaterInputStream(new ByteArrayInputStream(new byte[] { 0x78, 0x00, 0x03, 0x00 }));
        assertThrows(IOException.class, in::readAllBytes);
    }

    // Fails a reader that keeps reading after the end, so a loop that ignores the end fails instead of hanging.
    private static class EndOnceInputStream extends ByteArrayInputStream {
        private int ends;

        EndOnceInputStream(byte[] data) {
            super(data);
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) {
            int n = super.read(b, off, len);
            if (n == -1 && ++ends > 100) {
                throw new AssertionError("read past the end of the input " + ends + " times");
            }
            return n;
        }
    }

    private static class OneByteInputStream extends ByteArrayInputStream {
        OneByteInputStream(byte[] data) {
            super(data);
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) {
            return super.read(b, off, Math.min(len, 1));
        }
    }
}
