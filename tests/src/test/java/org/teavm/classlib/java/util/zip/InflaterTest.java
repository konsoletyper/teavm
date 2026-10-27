/*
 *  Copyright 2026 Ethan Resnick.
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.teavm.classlib.java.util.zip.ZipTestUtil.readHex;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

// Inflater.inflate(byte[], int, int), Java SE 21 Javadoc: "A return value of 0 indicates that needsInput() or
// needsDictionary() should be called in order to determine if more input data or a preset dictionary is required."
// It throws "DataFormatException - if the compressed data format is invalid". RFC 1950 section 2.2 defines an
// invalid zlib header: CMF*256 + FLG is not a multiple of 31 (FCHECK), CM is not 8 (deflate), or CINFO is above 7
// (window larger than 32K). FDICT (bit 5 of FLG) means a 4-byte DICTID, the Adler-32 of the preset dictionary, follows.
// These tests also run on the JVM, which checks their expectations against the JDK's zlib.
@TeaVMTest
public class InflaterTest {
    private static final byte[] TEXT = ("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod "
            + "tempor incididunt ut labore et dolore magna aliqua. Lorem ipsum dolor sit amet.")
            .getBytes(StandardCharsets.UTF_8);

    // Python: zlib.compressobj(zdict=b'hello world'), compressing b'hello world hello world!'. Header 78 bb has FDICT.
    private static final byte[] DICT_STREAM = readHex("78bb1a0b045dcb40301590d88a0072e108fa");
    private static final byte[] DICT = "hello world".getBytes(StandardCharsets.UTF_8);
    private static final int DICT_ADLER = 0x1a0b045d;

    @Test
    public void damagedHeaderIsRejected() {
        for (var header : new byte[][] { { 0x78, 0x00 }, { 0x79, (byte) 0x9c } }) {
            var inflater = new Inflater();
            inflater.setInput(new byte[] { header[0], header[1], 1, 2, 3 });
            try {
                inflater.inflate(new byte[10]);
                fail();
            } catch (DataFormatException e) {
                // ok
            }
        }
    }

    // Every 2-byte zlib header, followed by an empty final fixed block (03 00) and the empty stream's Adler-32
    // (00 00 00 01). With FDICT, those last four bytes 03 00 00 00 are read as the DICTID instead.
    // Each header is also given split across two inflate calls when it passes FCHECK, which leaves the method,
    // window size and FDICT checks to decide, and for a sample of the headers that fail FCHECK, at every CMF.
    @Test
    public void everyTwoByteHeaderIsAcceptedOrRejectedAsRfc1950Says() {
        var mismatches = new ArrayList<String>();
        for (int h = 0; h < 65536; h++) {
            int cmf = h >> 8;
            int flg = h & 0xFF;
            boolean valid = h % 31 == 0 && (cmf & 0x0F) == 8 && (cmf >> 4) <= 7;
            String expected = !valid ? "rejected" : (flg & 0x20) != 0 ? "dictionary 3000000" : "finished";
            int splits = h % 31 == 0 || h % 31 == 15 ? 1 : 0;
            for (int split = 0; split <= splits; split++) {
                String actual = inflateHeader(cmf, flg, split);
                if (!expected.equals(actual) && mismatches.size() < 20) {
                    mismatches.add(Integer.toHexString(h) + " split " + split + ": " + actual + ", expected "
                            + expected);
                }
            }
        }
        assertEquals("[]", mismatches.toString());
    }

    // split 0 gives the whole input at once. split 1 first gives the inflater only the CMF byte, so the
    // header reaches it across two inflate calls.
    private static String inflateHeader(int cmf, int flg, int split) {
        var input = new byte[] { (byte) cmf, (byte) flg, 0x03, 0x00, 0x00, 0x00, 0x00, 0x01 };
        var inflater = new Inflater();
        var out = new byte[16];
        try {
            if (split > 0) {
                inflater.setInput(input, 0, split);
                if (inflater.inflate(out) != 0 || !inflater.needsInput() || inflater.needsDictionary()) {
                    return "progress on " + split + " header bytes";
                }
                inflater.setInput(input, split, input.length - split);
            } else {
                inflater.setInput(input);
            }
            int n = inflater.inflate(out);
            if (n != 0) {
                return "output " + n;
            }
            if (inflater.needsDictionary()) {
                return "dictionary " + Integer.toHexString(inflater.getAdler());
            }
            return inflater.finished() && inflater.getRemaining() == 0 ? "finished" : "unfinished";
        } catch (DataFormatException e) {
            return "rejected";
        } finally {
            inflater.end();
        }
    }

    @Test
    public void truncatedStreamAtEveryPositionAsksForInput() throws DataFormatException {
        var compressed = deflate(TEXT, false);
        for (int len = 0; len < compressed.length; len++) {
            var inflater = new Inflater();
            inflater.setInput(compressed, 0, len);
            var out = new byte[TEXT.length + 10];
            int total = 0;
            int n;
            while ((n = inflater.inflate(out, total, out.length - total)) > 0) {
                total += n;
            }
            assertFalse(inflater.finished(), "length " + len);
            assertFalse(inflater.needsDictionary(), "length " + len);
            assertTrue(inflater.needsInput(), "length " + len);
            assertArrayEquals(Arrays.copyOf(TEXT, total), Arrays.copyOf(out, total), "length " + len);
            // No input left, so no progress: the result is 0 again, not an error.
            assertEquals(0, inflater.inflate(out, total, out.length - total), "length " + len);
            inflater.end();
        }
    }

    @Test
    public void byteAtATimeFromAnOffsetInflatesCompletely() throws DataFormatException {
        for (boolean nowrap : new boolean[] { false, true }) {
            var compressed = deflate(TEXT, nowrap);
            var input = new byte[compressed.length + 14];
            System.arraycopy(compressed, 0, input, 7, compressed.length);
            Arrays.fill(input, 0, 7, (byte) 0x55);
            var inflater = new Inflater(nowrap);
            var result = new ByteArrayOutputStream();
            var out = new byte[3];
            for (int i = 0; i < compressed.length && !inflater.finished(); i++) {
                inflater.setInput(input, 7 + i, 1);
                int n;
                while ((n = inflater.inflate(out)) > 0) {
                    result.write(out, 0, n);
                }
            }
            // A raw (nowrap) stream may need one extra dummy byte to finish, as the Inflater Javadoc notes.
            if (nowrap && !inflater.finished()) {
                inflater.setInput(new byte[1]);
                int n;
                while ((n = inflater.inflate(out)) > 0) {
                    result.write(out, 0, n);
                }
            }
            assertTrue(inflater.finished(), "nowrap " + nowrap);
            assertArrayEquals(TEXT, result.toByteArray(), "nowrap " + nowrap);
        }
    }

    @Test
    public void inflateBeforeAnyInputReturnsZero() throws DataFormatException {
        var inflater = new Inflater();
        assertEquals(0, inflater.inflate(new byte[10]));
        assertTrue(inflater.needsInput());
        assertFalse(inflater.finished());
    }

    // A raw stream has no zlib header. Its first two bytes here, 2b 4a, would fail every header check.
    @Test
    public void nowrapSkipsHeaderCheck() throws DataFormatException {
        var inflater = new Inflater(true);
        // Python: zlib.compressobj(wbits=-15), compressing b'raw data raw data'.
        inflater.setInput(readHex("2b4a2c5748492c495428823200"));
        var out = new byte[40];
        int n = inflater.inflate(out);
        assertEquals("raw data raw data", new String(out, 0, n, StandardCharsets.UTF_8));
    }

    @Test
    public void resetForgetsTheHeader() throws DataFormatException {
        var good = deflate(TEXT, false);
        var inflater = new Inflater();
        var out = new byte[TEXT.length + 10];

        inflater.setInput(new byte[] { 0x78, 0x00, 1, 2, 3 });
        assertThrows(DataFormatException.class, () -> inflater.inflate(out));

        inflater.reset();
        inflater.setInput(good);
        assertEquals(TEXT.length, inflater.inflate(out));
        assertTrue(inflater.finished());

        // The header bytes of the good stream must not stand in for the bad header after a reset.
        inflater.reset();
        inflater.setInput(new byte[] { 0x78 });
        assertEquals(0, inflater.inflate(out));
        inflater.setInput(new byte[] { 0x00, 1, 2, 3 });
        assertThrows(DataFormatException.class, () -> inflater.inflate(out));
    }

    // The 2 header bytes and the 4 DICTID bytes, given in two parts split at every position.
    @Test
    public void presetDictionary() throws DataFormatException {
        for (int split = 0; split <= 6; split++) {
            var inflater = new Inflater();
            var out = new byte[40];
            inflater.setInput(DICT_STREAM, 0, split);
            assertEquals(0, inflater.inflate(out), "split " + split);
            assertEquals(split == 6, inflater.needsDictionary(), "split " + split);
            assertTrue(inflater.needsInput(), "split " + split);
            if (split < 6) {
                inflater.setInput(DICT_STREAM, split, 6 - split);
                assertEquals(0, inflater.inflate(out), "split " + split);
                assertTrue(inflater.needsDictionary(), "split " + split);
            }
            assertEquals(DICT_ADLER, inflater.getAdler(), "split " + split);
            // Still needs the dictionary: 0 again, not an error.
            assertEquals(0, inflater.inflate(out), "split " + split);
            assertTrue(inflater.needsDictionary(), "split " + split);

            inflater.setDictionary(DICT);
            assertFalse(inflater.needsDictionary(), "split " + split);
            inflater.setInput(DICT_STREAM, 6, DICT_STREAM.length - 6);
            int n = inflater.inflate(out);
            assertEquals("hello world hello world!", new String(out, 0, n, StandardCharsets.UTF_8),
                    "split " + split);
            assertTrue(inflater.finished(), "split " + split);
        }
    }

    private static byte[] deflate(byte[] data, boolean nowrap) {
        var deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, nowrap);
        deflater.setInput(data);
        deflater.finish();
        var out = new ByteArrayOutputStream();
        var buf = new byte[64];
        while (!deflater.finished()) {
            int n = deflater.deflate(buf);
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
