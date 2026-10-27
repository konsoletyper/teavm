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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

// Deflater.deflate(byte[], int, int, int), Java SE 21 Javadoc: "A return value of 0 in this flush mode [NO_FLUSH]
// indicates that needsInput() should be called in order to determine if more input data is required." With SYNC_FLUSH
// "all pending output in the deflater is flushed, to the specified output buffer, so that an inflater that works on
// compressed data can get all input data available so far". It throws "IllegalArgumentException - if the flush mode
// is invalid".
@TeaVMTest
public class DeflaterTest {
    @Test
    public void callWithoutProgressReturnsZero() {
        var deflater = new Deflater();
        var out = new byte[100];
        deflater.setInput("hello hello hello".getBytes(StandardCharsets.UTF_8));
        var n = deflater.deflate(out, 0, out.length, Deflater.SYNC_FLUSH);
        assertTrue(n > 4);
        assertArrayEquals(new byte[] { 0, 0, -1, -1 }, Arrays.copyOfRange(out, n - 4, n));
        assertEquals(0, deflater.deflate(out, 0, out.length, Deflater.SYNC_FLUSH));
        assertEquals(0, deflater.deflate(out));
    }

    @Test
    public void invalidFlushModeIsRejected() {
        var deflater = new Deflater();
        deflater.setInput(new byte[] { 1, 2, 3 });
        // 1 is zlib's Z_PARTIAL_FLUSH and 4 its Z_FINISH; neither is a Deflater flush mode.
        for (int mode : new int[] { -1, 1, 4, 5 }) {
            assertThrows(IllegalArgumentException.class, () -> deflater.deflate(new byte[100], 0, 100, mode));
        }
    }

    @Test
    public void flushedOutputInflatesToAllInputSoFar() throws DataFormatException {
        for (int mode : new int[] { Deflater.SYNC_FLUSH, Deflater.FULL_FLUSH }) {
            var deflater = new Deflater();
            var inflater = new Inflater();
            var out = new byte[200];
            var inflated = new byte[200];
            var expected = new ByteArrayOutputStream();
            var actual = new ByteArrayOutputStream();
            for (int chunk = 0; chunk < 5; chunk++) {
                var input = ("chunk " + chunk + " of text, chunk " + chunk + ";").getBytes(StandardCharsets.UTF_8);
                expected.write(input, 0, input.length);
                deflater.setInput(input);
                int n = deflater.deflate(out, 0, out.length, mode);
                assertTrue(n < out.length, "mode " + mode);
                assertTrue(deflater.needsInput(), "mode " + mode);
                assertArrayEquals(new byte[] { 0, 0, -1, -1 }, Arrays.copyOfRange(out, n - 4, n), "mode " + mode);
                // Nothing new to flush.
                assertEquals(0, deflater.deflate(out, 0, out.length, mode), "mode " + mode);
                inflater.setInput(out, 0, n);
                int m;
                while ((m = inflater.inflate(inflated)) > 0) {
                    actual.write(inflated, 0, m);
                }
                assertArrayEquals(expected.toByteArray(), actual.toByteArray(), "mode " + mode);
            }
        }
    }

    // After finish(), deflate(..., NO_FLUSH) finishes the stream, as the JDK's does.
    @Test
    public void flushModeAfterFinishStillFinishes() throws DataFormatException {
        var input = "finish me, finish me".getBytes(StandardCharsets.UTF_8);
        var deflater = new Deflater();
        deflater.setInput(input);
        deflater.finish();
        var out = new byte[100];
        int n = deflater.deflate(out, 0, out.length, Deflater.NO_FLUSH);
        assertTrue(deflater.finished());
        var inflater = new Inflater();
        inflater.setInput(out, 0, n);
        var result = new byte[100];
        int m = inflater.inflate(result);
        assertTrue(inflater.finished());
        assertArrayEquals(input, Arrays.copyOf(result, m));
    }

    @Test
    public void deflateWithoutInputEventuallyReturnsZero() {
        var deflater = new Deflater();
        deflater.setInput(new byte[0]);
        var out = new byte[100];
        int calls = 0;
        while (deflater.deflate(out) > 0) {
            assertTrue(++calls < 3);
        }
        assertTrue(deflater.needsInput());
        assertEquals(0, deflater.deflate(out));
    }
}
