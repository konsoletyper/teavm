/*
 *  Copyright 2026 Alexey Andreev.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class ChecksumTest {
    private static final byte[] DATA = "hello, checksum".getBytes(StandardCharsets.US_ASCII);

    @Test
    public void updateByteBuffer() {
        for (ByteBuffer buffer : new ByteBuffer[] { ByteBuffer.allocate(DATA.length + 4),
                ByteBuffer.allocateDirect(DATA.length + 4) }) {
            buffer.put(new byte[2]).put(DATA).flip().position(2);
            CRC32 crc = new CRC32();
            crc.update(buffer);
            assertEquals(crcOf(DATA), crc.getValue());
            assertEquals(buffer.limit(), buffer.position());

            buffer.position(2);
            Adler32 adler = new Adler32();
            adler.update(buffer);
            assertEquals(adlerOf(DATA), adler.getValue());
        }
    }

    @Test
    public void defaultUpdateByteArray() {
        CountingChecksum checksum = new CountingChecksum();
        checksum.update(DATA);
        assertEquals(DATA.length, checksum.getValue());
    }

    private static long crcOf(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data, 0, data.length);
        return crc.getValue();
    }

    private static long adlerOf(byte[] data) {
        Adler32 adler = new Adler32();
        adler.update(data, 0, data.length);
        return adler.getValue();
    }

    // Implements only the abstract methods, so update(byte[]) comes from the interface.
    static class CountingChecksum implements Checksum {
        private long count;

        @Override
        public void update(int b) {
            count++;
        }

        @Override
        public void update(byte[] b, int off, int len) {
            count += len;
        }

        @Override
        public long getValue() {
            return count;
        }

        @Override
        public void reset() {
            count = 0;
        }
    }
}
