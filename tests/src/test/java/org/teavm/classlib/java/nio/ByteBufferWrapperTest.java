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
package org.teavm.classlib.java.nio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class ByteBufferWrapperTest {
    @Test
    public void wrapsIntoCharBuffer() {
        byte[] array = new byte[100];
        var buffer = ByteBuffer.wrap(array);
        buffer.limit(80);
        buffer.get(new byte[10]);
        buffer = buffer.slice();
        buffer.put(0, (byte) 0x23);
        buffer.put(1, (byte) 0x24);

        var wrapper = buffer.asCharBuffer();
        assertEquals(35, wrapper.capacity());
        assertEquals(0, wrapper.position());
        assertEquals(35, wrapper.limit());
        assertEquals(0x2324, wrapper.get(0));

        wrapper.put(0, (char) 0x2526);
        assertEquals(0x25, buffer.get(0));
        assertEquals(0x26, buffer.get(1));
    }

    @Test
    public void wrapsIntoShortBuffer() {
        byte[] array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.limit(80);
        buffer.get(new byte[10]);
        buffer = buffer.slice();
        buffer.put(0, (byte) 0x23);
        buffer.put(1, (byte) 0x24);

        ShortBuffer wrapper = buffer.asShortBuffer();
        assertEquals(35, wrapper.capacity());
        assertEquals(0, wrapper.position());
        assertEquals(35, wrapper.limit());
        assertEquals((short) 0x2324, wrapper.get(0));

        wrapper.put(0, (short) 0x2526);
        assertEquals((byte) 0x25, buffer.get(0));
        assertEquals((byte) 0x26, buffer.get(1));
    }

    @Test
    public void wrapsIntoIntBuffer() {
        byte[] array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.limit(70);
        buffer.get(new byte[10]);
        buffer = buffer.slice();
        buffer.put(0, (byte) 0x23);
        buffer.put(1, (byte) 0x24);
        buffer.put(2, (byte) 0x25);

        IntBuffer wrapper = buffer.asIntBuffer();
        assertEquals(15, wrapper.capacity());
        assertEquals(0, wrapper.position());
        assertEquals(15, wrapper.limit());
        assertEquals(0x23242500, wrapper.get(0));

        wrapper.put(0, 0x26272829);
        assertEquals((byte) 0x26, buffer.get(0));
        assertEquals((byte) 0x27, buffer.get(1));
        assertEquals((byte) 0x28, buffer.get(2));
        assertEquals((byte) 0x29, buffer.get(3));
    }

    @Test
    public void wrapsIntoLongBuffer() {
        byte[] array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.limit(50);
        buffer.get(new byte[10]);
        buffer = buffer.slice();
        buffer.put(0, (byte) 0x23);
        buffer.put(1, (byte) 0x24);
        buffer.put(2, (byte) 0x25);
        buffer.put(3, (byte) 0x26);
        buffer.put(4, (byte) 0x27);
        buffer.put(5, (byte) 0x28);
        buffer.put(6, (byte) 0x29);
        buffer.put(7, (byte) 0x2A);

        LongBuffer wrapper = buffer.asLongBuffer();
        assertEquals(5, wrapper.capacity());
        assertEquals(0, wrapper.position());
        assertEquals(5, wrapper.limit());
        assertEquals(0x232425262728292AL, wrapper.get(0));

        wrapper.put(0, 0x2B2C2D2E2F303132L);
        assertEquals((byte) 0x2B, buffer.get(0));
        assertEquals((byte) 0x2C, buffer.get(1));
        assertEquals((byte) 0x2D, buffer.get(2));
        assertEquals((byte) 0x2E, buffer.get(3));
        assertEquals((byte) 0x2F, buffer.get(4));
        assertEquals((byte) 0x30, buffer.get(5));
        assertEquals((byte) 0x31, buffer.get(6));
        assertEquals((byte) 0x32, buffer.get(7));
    }

    @Test
    public void wrapsIntoFloatBuffer() {
        byte[] array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.limit(70);
        buffer.get(new byte[10]);
        buffer = buffer.slice();
        buffer.put(0, (byte) 0x40);
        buffer.put(1, (byte) 0x49);
        buffer.put(2, (byte) 0x0F);
        buffer.put(3, (byte) 0xD0);

        FloatBuffer wrapper = buffer.asFloatBuffer();
        assertEquals(15, wrapper.capacity());
        assertEquals(0, wrapper.position());
        assertEquals(15, wrapper.limit());
        assertEquals(3.14159, wrapper.get(0), 0.00001);

        wrapper.put(0, 2.71828F);
        assertEquals((byte) 0x40, buffer.get(0));
        assertEquals((byte) 0x2D, buffer.get(1));
        assertEquals((byte) 0xF8, buffer.get(2));
        assertEquals(0x40, buffer.get(3) & 0xF0);
    }

    @Test
    public void wrapsIntoDoubleBuffer() {
        var array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.limit(70);
        buffer.get(new byte[10]);
        buffer = buffer.slice();
        buffer.put(0, (byte) 0x40);
        buffer.put(1, (byte) 0x09);
        buffer.put(2, (byte) 0x21);
        buffer.put(3, (byte) 0xf9);
        buffer.put(4, (byte) 0xf0);
        buffer.put(5, (byte) 0x1b);
        buffer.put(6, (byte) 0x86);
        buffer.put(7, (byte) 0x6E);

        var wrapper = buffer.asDoubleBuffer();
        assertEquals(7, wrapper.capacity());
        assertEquals(0, wrapper.position());
        assertEquals(7, wrapper.limit());
        assertEquals(3.14159, wrapper.get(0), 0.00001);

        wrapper.put(0, 2.71828);
        assertEquals((byte) 0x40, buffer.get(0));
        assertEquals((byte) 0x05, buffer.get(1));
        assertEquals((byte) 0xbf, buffer.get(2));
        assertEquals((byte) 0x09, buffer.get(3));
        assertEquals((byte) 0x95, buffer.get(4));
        assertEquals((byte) 0xAA, buffer.get(5));
    }

    @Test
    public void shortEndiannessWorks() {
        byte[] array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(0, (byte) 0x23);
        buffer.put(1, (byte) 0x24);
        ShortBuffer wrapper = buffer.asShortBuffer();
        assertEquals((short) 0x2423, wrapper.get(0));
    }

    @Test
    public void intEndiannessWorks() {
        byte[] array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(0, (byte) 0x23);
        buffer.put(1, (byte) 0x24);
        buffer.put(2, (byte) 0x25);
        buffer.put(3, (byte) 0x26);
        IntBuffer wrapper = buffer.asIntBuffer();
        assertEquals(0x26252423, wrapper.get(0));
    }

    @Test
    public void changesInWrapperSeenInBuffer() {
        byte[] array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        ShortBuffer wrapper = buffer.asShortBuffer();
        wrapper.put(0, (short) 0x2324);
        assertEquals((byte) 0x23, buffer.get(0));
        assertEquals((byte) 0x24, buffer.get(1));
    }

    @Test
    public void changesInBufferSeenInWrapper() {
        byte[] array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        ShortBuffer wrapper = buffer.asShortBuffer();
        buffer.put(1, (byte) 0x24);
        assertEquals((short) 0x0024, wrapper.get(0));
    }
}
