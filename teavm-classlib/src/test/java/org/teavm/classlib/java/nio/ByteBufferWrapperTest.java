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
import static org.junit.Assert.assertThat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ByteBufferWrapperTest {
    @Test
    public void wrapsIntoShortBuffer() {
        byte[] array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.limit(80);
        buffer.get(new byte[10]);
        buffer = buffer.slice();
        buffer.put(0, (byte)0x23);
        buffer.put(1, (byte)0x24);
        ShortBuffer wrapper = buffer.asShortBuffer();
        assertThat(wrapper.capacity(), is(35));
        assertThat(wrapper.position(), is(0));
        assertThat(wrapper.limit(), is(35));
        assertThat(wrapper.get(0), is((short)0x2324));
    }

    @Test
    public void wrapsIntoIntBuffer() {
        byte[] array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.limit(70);
        buffer.get(new byte[10]);
        buffer = buffer.slice();
        buffer.put(0, (byte)0x23);
        buffer.put(1, (byte)0x24);
        buffer.put(2, (byte)0x25);
        IntBuffer wrapper = buffer.asIntBuffer();
        assertThat(wrapper.capacity(), is(15));
        assertThat(wrapper.position(), is(0));
        assertThat(wrapper.limit(), is(15));
        assertThat(wrapper.get(0), is(0x23242500));
    }

    @Test
    public void endiannesWorks() {
        byte[] array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(0, (byte)0x23);
        buffer.put(1, (byte)0x24);
        ShortBuffer wrapper = buffer.asShortBuffer();
        assertThat(wrapper.get(0), is((short)0x2423));
    }

    @Test
    public void changesInWrapperSeenInBuffer() {
        byte[] array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        ShortBuffer wrapper = buffer.asShortBuffer();
        wrapper.put(0, (short)0x2324);
        assertThat(buffer.get(0), is((byte)0x23));
        assertThat(buffer.get(1), is((byte)0x24));
    }

    @Test
    public void changesInBufferSeenInWrapper() {
        byte[] array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        ShortBuffer wrapper = buffer.asShortBuffer();
        buffer.put(1, (byte)0x24);
        assertThat(wrapper.get(0), is((short)0x0024));
    }
}
