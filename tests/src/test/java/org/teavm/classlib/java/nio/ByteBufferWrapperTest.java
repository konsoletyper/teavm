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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class ByteBufferWrapperTest {
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
        assertThat(wrapper.capacity(), is(35));
        assertThat(wrapper.position(), is(0));
        assertThat(wrapper.limit(), is(35));
        assertThat(wrapper.get(0), is((short) 0x2324));

        wrapper.put(0, (short) 0x2526);
        assertThat(buffer.get(0), is((byte) 0x25));
        assertThat(buffer.get(1), is((byte) 0x26));
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
        assertThat(wrapper.capacity(), is(15));
        assertThat(wrapper.position(), is(0));
        assertThat(wrapper.limit(), is(15));
        assertThat(wrapper.get(0), is(0x23242500));

        wrapper.put(0, 0x26272829);
        assertThat(buffer.get(0), is((byte) 0x26));
        assertThat(buffer.get(1), is((byte) 0x27));
        assertThat(buffer.get(2), is((byte) 0x28));
        assertThat(buffer.get(3), is((byte) 0x29));
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
        assertThat(wrapper.capacity(), is(5));
        assertThat(wrapper.position(), is(0));
        assertThat(wrapper.limit(), is(5));
        assertThat(wrapper.get(0), is(0x232425262728292AL));

        wrapper.put(0, 0x2B2C2D2E2F303132L);
        assertThat(buffer.get(0), is((byte) 0x2B));
        assertThat(buffer.get(1), is((byte) 0x2C));
        assertThat(buffer.get(2), is((byte) 0x2D));
        assertThat(buffer.get(3), is((byte) 0x2E));
        assertThat(buffer.get(4), is((byte) 0x2F));
        assertThat(buffer.get(5), is((byte) 0x30));
        assertThat(buffer.get(6), is((byte) 0x31));
        assertThat(buffer.get(7), is((byte) 0x32));
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
        assertThat(wrapper.capacity(), is(15));
        assertThat(wrapper.position(), is(0));
        assertThat(wrapper.limit(), is(15));
        assertEquals(3.14159, wrapper.get(0), 0.00001);

        wrapper.put(0, 2.71828F);
        assertThat(buffer.get(0), is((byte) 0x40));
        assertThat(buffer.get(1), is((byte) 0x2D));
        assertThat(buffer.get(2), is((byte) 0xF8));
        assertThat(buffer.get(3) & 0xF0, is(0x40));
    }

    @Test
    public void shortEndiannesWorks() {
        byte[] array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(0, (byte) 0x23);
        buffer.put(1, (byte) 0x24);
        ShortBuffer wrapper = buffer.asShortBuffer();
        assertThat(wrapper.get(0), is((short) 0x2423));
    }

    @Test
    public void intEndiannesWorks() {
        byte[] array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(0, (byte) 0x23);
        buffer.put(1, (byte) 0x24);
        buffer.put(2, (byte) 0x25);
        buffer.put(3, (byte) 0x26);
        IntBuffer wrapper = buffer.asIntBuffer();
        assertThat(wrapper.get(0), is(0x26252423));
    }

    @Test
    public void changesInWrapperSeenInBuffer() {
        byte[] array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        ShortBuffer wrapper = buffer.asShortBuffer();
        wrapper.put(0, (short) 0x2324);
        assertThat(buffer.get(0), is((byte) 0x23));
        assertThat(buffer.get(1), is((byte) 0x24));
    }

    @Test
    public void changesInBufferSeenInWrapper() {
        byte[] array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        ShortBuffer wrapper = buffer.asShortBuffer();
        buffer.put(1, (byte) 0x24);
        assertThat(wrapper.get(0), is((short) 0x0024));
    }
}
