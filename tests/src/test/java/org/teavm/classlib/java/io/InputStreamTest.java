/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.classlib.java.io;

import static org.testng.Assert.assertEquals;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class InputStreamTest {
    @Test
    @SuppressWarnings("resource")
    public void readAll() throws IOException {
        var is = new ChunkedInputStream(17, 1000);
        var bytes = is.readAllBytes();
        assertEquals(17_000, bytes.length);
        for (var i = 0; i < bytes.length; ++i) {
            assertEquals(i % 17, bytes[i], "wrong element at position " + i);
        }
    }

    @Test
    @SuppressWarnings("resource")
    public void readAllWithLargeChunks() throws IOException {
        var is = new ChunkedInputStream(3000, 2);
        var bytes = is.readAllBytes();
        assertEquals(6000, bytes.length);
        for (var i = 0; i < bytes.length; ++i) {
            assertEquals((byte) (i % 3000), bytes[i], "wrong element at position " + i);
        }
    }

    @Test
    @SuppressWarnings("resource")
    public void readN() throws IOException {
        var is = new ChunkedInputStream(17, 1000);
        var bytes = is.readNBytes(5000);
        assertEquals(5000, bytes.length);
        for (var i = 0; i < bytes.length; ++i) {
            assertEquals(i % 17, bytes[i], "wrong element at position " + i);
        }
    }

    @Test
    @SuppressWarnings("resource")
    public void transfer() throws IOException {
        var is = new ChunkedInputStream(17, 1000);
        var os = new ByteArrayOutputStream();
        is.transferTo(os);
        var bytes = os.toByteArray();
        assertEquals(17_000, bytes.length);
        for (var i = 0; i < bytes.length; ++i) {
            assertEquals(i % 17, bytes[i], "wrong element at position " + i);
        }
    }

    @Test
    @SuppressWarnings("resource")
    public void skipN() throws IOException {
        var is = new ChunkedInputStream(17, 1000);
        is.skipNBytes(19);
        assertEquals(2, is.read());
        assertEquals(17_000 - 20, is.readAllBytes().length);
    }

    static class ChunkedInputStream extends InputStream {
        private int chunkSize;
        private int chunkRepeats;
        private int lastValue;

        ChunkedInputStream(int chunkSize, int chunkRepeats) {
            this.chunkSize = chunkSize;
            this.chunkRepeats = chunkRepeats;
        }

        @Override
        public int read() throws IOException {
            if (chunkRepeats == 0) {
                return -1;
            }
            var result = lastValue++;
            if (lastValue == chunkSize) {
                lastValue = 0;
                chunkRepeats--;
            }
            return result;
        }

        @Override
        public long skip(long n) throws IOException {
            if (chunkRepeats == 0) {
                return -1;
            }
            var value = lastValue;
            n = Math.min(n, chunkSize - value);
            value += (int) n;
            if (value == chunkSize) {
                value = 0;
                chunkRepeats--;
            }
            lastValue = value;
            return n;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (chunkRepeats == 0) {
                return -1;
            }
            var value = lastValue;
            len = Math.min(len, chunkSize - value);
            for (var i = 0; i < len; ++i) {
                b[off++] = (byte) value++;
            }
            if (value == chunkSize) {
                value = 0;
                chunkRepeats--;
            }
            lastValue = value;
            return len;
        }
    }
}
