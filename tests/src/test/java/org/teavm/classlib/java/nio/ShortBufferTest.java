/*
 *  Copyright 2017 Alexey Andreev.
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
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.InvalidMarkException;
import java.nio.ReadOnlyBufferException;
import java.nio.ShortBuffer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
public class ShortBufferTest {
    @Test
    public void allocatesSimple() {
        ShortBuffer buffer = ShortBuffer.allocate(100);
        assertThat(buffer.isDirect(), is(false));
        assertThat(buffer.isReadOnly(), is(false));
        assertThat(buffer.hasArray(), is(true));
        assertThat(buffer.capacity(), is(100));
        assertThat(buffer.position(), is(0));
        assertThat(buffer.limit(), is(100));
        try {
            buffer.reset();
            fail("Mark is expected to be undefined");
        } catch (InvalidMarkException e) {
            // ok
        }
    }

    @Test
    @SkipPlatform({ TestPlatform.WASI, TestPlatform.WEBASSEMBLY})
    public void bulkTransferDirect() {
        var buffer = ByteBuffer.allocateDirect(20).asShortBuffer();
        var shorts = new short[] { 1, 2, 3 };
        buffer.put(0, shorts);
        var shortsCopy = new short[shorts.length];
        buffer.get(0, shortsCopy);
        assertArrayEquals(shorts, shortsCopy);
    }

    @Test
    public void bulkTransferRelative() {
        var arr = new short[5];
        var buffer = ShortBuffer.wrap(arr);
        var src = ShortBuffer.wrap(new short[] { 1, 2, 3 });
        buffer.put(src);
        assertArrayEquals(new short[] { 1, 2, 3, 0, 0 }, arr);
        assertEquals(3, buffer.position());
        assertEquals(3, src.position());

        assertThrows(BufferOverflowException.class, () -> buffer.put(ShortBuffer.wrap(new short[] { 4, 5, 6 })));
        assertThrows(ReadOnlyBufferException.class, () -> buffer.rewind().asReadOnlyBuffer()
                .put(ShortBuffer.wrap(new short[] { 4, 5, 6 })));
    }

    @Test(expected = IllegalArgumentException.class)
    public void errorIfAllocatingBufferOfNegativeSize() {
        ShortBuffer.allocate(-1);
    }

    @Test
    public void wrapsArray() {
        short[] array = new short[100];
        ShortBuffer buffer = ShortBuffer.wrap(array, 10, 70);
        assertThat(buffer.isDirect(), is(false));
        assertThat(buffer.isReadOnly(), is(false));
        assertThat(buffer.hasArray(), is(true));
        assertThat(buffer.array(), is(array));
        assertThat(buffer.arrayOffset(), is(0));
        assertThat(buffer.capacity(), is(100));
        assertThat(buffer.position(), is(10));
        assertThat(buffer.limit(), is(80));
        try {
            buffer.reset();
            fail("Mark is expected to be undefined");
        } catch (InvalidMarkException e) {
            // ok
        }
        array[0] = 23;
        assertThat(buffer.get(0), is((short) 23));
        buffer.put(1, (short) 24);
        assertThat(array[1], is((short) 24));
    }

    @Test
    public void errorWhenWrappingWithWrongParameters() {
        short[] array = new short[100];
        try {
            ShortBuffer.wrap(array, -1, 10);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            ShortBuffer.wrap(array, 101, 10);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            ShortBuffer.wrap(array, 98, 3);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            ShortBuffer.wrap(array, 98, -1);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
    }

    @Test
    public void wrapsArrayWithoutOffset() {
        short[] array = new short[100];
        ShortBuffer buffer = ShortBuffer.wrap(array);
        assertThat(buffer.position(), is(0));
        assertThat(buffer.limit(), is(100));
    }

    @Test
    public void createsSlice() {
        ShortBuffer buffer = ShortBuffer.allocate(100);
        buffer.put(new short[60]);
        buffer.flip();
        buffer.put(new short[15]);
        ShortBuffer slice = buffer.slice();
        assertThat(slice.array(), is(buffer.array()));
        assertThat(slice.position(), is(0));
        assertThat(slice.capacity(), is(45));
        assertThat(slice.limit(), is(45));
        assertThat(slice.isDirect(), is(false));
        assertThat(slice.isReadOnly(), is(false));
        slice.put(3, (short) 23);
        assertThat(buffer.get(18), is((short) 23));
        slice.put((short) 24);
        assertThat(buffer.get(15), is((short) 24));
        buffer.put(16, (short) 25);
        assertThat(slice.get(1), is((short) 25));
    }

    @Test
    public void slicePropertiesSameWithOriginal() {
        ShortBuffer buffer = ShortBuffer.allocate(100).asReadOnlyBuffer().slice();
        assertThat(buffer.isReadOnly(), is(true));
    }

    @Test
    public void createsDuplicate() {
        ShortBuffer buffer = ShortBuffer.allocate(100);
        buffer.put(new short[60]);
        buffer.flip();
        buffer.put(new short[15]);
        ShortBuffer duplicate = buffer.duplicate();
        assertThat(duplicate.array(), is(buffer.array()));
        assertThat(duplicate.position(), is(15));
        assertThat(duplicate.capacity(), is(100));
        assertThat(duplicate.limit(), is(60));
        assertThat(duplicate.isDirect(), is(false));
        assertThat(duplicate.isReadOnly(), is(false));
        duplicate.put(3, (short) 23);
        assertThat(buffer.get(3), is((short) 23));
        duplicate.put((short) 24);
        assertThat(buffer.get(15), is((short) 24));
        buffer.put(1, (short) 25);
        assertThat(duplicate.get(1), is((short) 25));
        assertThat(duplicate.array(), is(sameInstance(buffer.array())));
    }

    @Test
    public void getsShort() {
        short[] array = { 2, 3, 5, 7 };
        ShortBuffer buffer = ShortBuffer.wrap(array);
        assertThat(buffer.get(), is((short) 2));
        assertThat(buffer.get(), is((short) 3));
        buffer = buffer.slice();
        assertThat(buffer.get(), is((short) 5));
        assertThat(buffer.get(), is((short) 7));
    }

    @Test
    public void gettingShortFromEmptyBufferCausesError() {
        short[] array = { 2, 3, 5, 7 };
        ShortBuffer buffer = ShortBuffer.wrap(array);
        buffer.limit(2);
        buffer.get();
        buffer.get();
        try {
            buffer.get();
            fail("Should have thrown error");
        } catch (BufferUnderflowException e) {
            // ok
        }
    }

    @Test
    public void putsShort() {
        short[] array = new short[4];
        ShortBuffer buffer = ShortBuffer.wrap(array);
        buffer.put((short) 2).put((short) 3).put((short) 5).put((short) 7);
        assertThat(array, is(new short[] { 2, 3, 5, 7 }));
    }

    @Test
    public void puttingShortToEmptyBufferCausesError() {
        short[] array = new short[4];
        ShortBuffer buffer = ShortBuffer.wrap(array);
        buffer.limit(2);
        buffer.put((short) 2).put((short) 3);
        try {
            buffer.put((short) 5);
            fail("Should have thrown error");
        } catch (BufferOverflowException e) {
            assertThat(array[2], is((short) 0));
        }
    }

    @Test(expected = ReadOnlyBufferException.class)
    public void puttingShortToReadOnlyBufferCausesError() {
        short[] array = new short[4];
        ShortBuffer buffer = ShortBuffer.wrap(array).asReadOnlyBuffer();
        buffer.put((short) 2);
    }

    @Test
    public void getsShortFromGivenLocation() {
        short[] array = { 2, 3, 5, 7 };
        ShortBuffer buffer = ShortBuffer.wrap(array);
        assertThat(buffer.get(0), is((short) 2));
        assertThat(buffer.get(1), is((short) 3));
        buffer.get();
        buffer = buffer.slice();
        assertThat(buffer.get(1), is((short) 5));
        assertThat(buffer.get(2), is((short) 7));
    }

    @Test
    public void gettingShortFromWrongLocationCausesError() {
        short[] array = { 2, 3, 5, 7 };
        ShortBuffer buffer = ShortBuffer.wrap(array);
        buffer.limit(3);
        try {
            buffer.get(-1);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            buffer.get(3);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
    }

    @Test
    public void putsShortToGivenLocation() {
        short[] array = new short[4];
        ShortBuffer buffer = ShortBuffer.wrap(array);
        buffer.put(0, (short) 2);
        buffer.put(1, (short) 3);
        buffer.get();
        buffer = buffer.slice();
        buffer.put(1, (short) 5);
        buffer.put(2, (short) 7);
        assertThat(array, is(new short[] { 2, 3, 5, 7 }));
    }

    @Test
    public void puttingShortToWrongLocationCausesError() {
        short[] array = new short[4];
        ShortBuffer buffer = ShortBuffer.wrap(array);
        buffer.limit(3);
        try {
            buffer.put(-1, (short) 2);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            buffer.put(3, (short) 2);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
    }

    @Test(expected = ReadOnlyBufferException.class)
    public void puttingShortToGivenLocationOfReadOnlyBufferCausesError() {
        short[] array = new short[4];
        ShortBuffer buffer = ShortBuffer.wrap(array).asReadOnlyBuffer();
        buffer.put(0, (short) 2);
    }

    @Test
    public void getsShorts() {
        short[] array = { 2, 3, 5, 7 };
        ShortBuffer buffer = ShortBuffer.wrap(array);
        buffer.get();
        short[] receiver = new short[2];
        buffer.get(receiver, 0, 2);
        assertThat(buffer.position(), is(3));
        assertThat(receiver, is(new short[] { 3, 5 }));
    }

    @Test
    public void gettingShortsFromEmptyBufferCausesError() {
        short[] array = { 2, 3, 5, 7 };
        ShortBuffer buffer = ShortBuffer.wrap(array);
        buffer.limit(3);
        short[] receiver = new short[4];
        try {
            buffer.get(receiver, 0, 4);
            fail("Error expected");
        } catch (BufferUnderflowException e) {
            assertThat(receiver, is(new short[4]));
            assertThat(buffer.position(), is(0));
        }
    }

    @Test
    public void gettingShortsWithIllegalArgumentsCausesError() {
        short[] array = { 2, 3, 5, 7 };
        ShortBuffer buffer = ShortBuffer.wrap(array);
        short[] receiver = new short[4];
        try {
            buffer.get(receiver, 0, 5);
        } catch (IndexOutOfBoundsException e) {
            assertThat(receiver, is(new short[4]));
            assertThat(buffer.position(), is(0));
        }
        try {
            buffer.get(receiver, -1, 3);
        } catch (IndexOutOfBoundsException e) {
            assertThat(receiver, is(new short[4]));
            assertThat(buffer.position(), is(0));
        }
        try {
            buffer.get(receiver, 6, 3);
        } catch (IndexOutOfBoundsException e) {
            assertThat(receiver, is(new short[4]));
            assertThat(buffer.position(), is(0));
        }
    }

    @Test
    public void putsShorts() {
        short[] array = new short[4];
        ShortBuffer buffer = ShortBuffer.wrap(array);
        buffer.get();
        short[] data = { 2, 3 };
        buffer.put(data, 0, 2);
        assertThat(buffer.position(), is(3));
        assertThat(array, is(new short[] { 0, 2, 3, 0 }));
    }

    @Test
    public void compacts() {
        short[] array = { 2, 3, 5, 7 };
        ShortBuffer buffer = ShortBuffer.wrap(array);
        buffer.get();
        buffer.mark();
        buffer.compact();
        assertThat(array, is(new short[] { 3, 5, 7, 7 }));
        assertThat(buffer.position(), is(3));
        assertThat(buffer.limit(), is(4));
        assertThat(buffer.capacity(), is(4));
        try {
            buffer.reset();
            fail("Exception expected");
        } catch (InvalidMarkException e) {
            // ok
        }
    }

    @Test
    public void marksPosition() {
        short[] array = { 2, 3, 5, 7 };
        ShortBuffer buffer = ShortBuffer.wrap(array);
        buffer.position(1);
        buffer.mark();
        buffer.position(2);
        buffer.reset();
        assertThat(buffer.position(), is(1));
    }

    @Test
    public void putEmptyArray() {
        ShortBuffer sb = ShortBuffer.allocate(0);
        sb.put(new short[0]);
        sb.get(new short[0]);
    }

    @Test
    public void bulkPut() {
        var buffer = ShortBuffer.allocate(100);
        buffer.put(new short[] { 1, 2, 3 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer.put(1, new short[] { 4, 5, 6 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(4, buffer.get(1));
        assertEquals(5, buffer.get(2));
        assertEquals(6, buffer.get(3));

        buffer.put(0, new short[] { 7, 8, 9, 10 }, 1, 2);
        assertEquals(8, buffer.get(0));
        assertEquals(9, buffer.get(1));
        assertEquals(5, buffer.get(2));
        assertEquals(6, buffer.get(3));
    }

    @Test
    public void bulkPutWrapper() {
        var byteBuffer = ByteBuffer.allocate(100);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        var buffer = byteBuffer.asShortBuffer();

        buffer.put(new short[] { 1, 2, 3 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer.put(1, new short[] { 4, 5, 6 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(4, buffer.get(1));
        assertEquals(5, buffer.get(2));
        assertEquals(6, buffer.get(3));
        assertEquals(0, byteBuffer.get(0));
        assertEquals(1, byteBuffer.get(1));

        buffer.put(0, new short[] { 7, 8, 9, 10 }, 1, 2);
        assertEquals(8, buffer.get(0));
        assertEquals(9, buffer.get(1));
        assertEquals(5, buffer.get(2));
        assertEquals(6, buffer.get(3));

        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer = byteBuffer.asShortBuffer();

        buffer.put(new short[] { 1, 2, 3 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer.put(1, new short[] { 4, 5, 6 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(4, buffer.get(1));
        assertEquals(5, buffer.get(2));
        assertEquals(6, buffer.get(3));
        assertEquals(1, byteBuffer.get(0));
        assertEquals(0, byteBuffer.get(1));

        buffer.put(0, new short[] { 7, 8, 9, 10 }, 1, 2);
        assertEquals(8, buffer.get(0));
        assertEquals(9, buffer.get(1));
        assertEquals(5, buffer.get(2));
        assertEquals(6, buffer.get(3));
    }

    @Test
    public void bulkPutBuffer() {
        var buffer = ShortBuffer.allocate(100);
        buffer.put(ShortBuffer.wrap(new short[] { 1, 2, 3 }));

        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer.put(1, ShortBuffer.wrap(new short[] { 4, 5, 6 }), 1, 2);
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(5, buffer.get(1));
        assertEquals(6, buffer.get(2));
    }

    @Test
    public void bulkPutBufferWrapper() {
        var buffer = ByteBuffer.allocate(100).order(ByteOrder.BIG_ENDIAN).asShortBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] { 0, 1, 0, 2, 0, 3 })
                .order(ByteOrder.BIG_ENDIAN)
                .asShortBuffer());

        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] { 0, 1, 0, 2, 0, 3 })
                .order(ByteOrder.BIG_ENDIAN)
                .asShortBuffer());

        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer = ByteBuffer.allocate(100).order(ByteOrder.BIG_ENDIAN).asShortBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] { 1, 0, 2, 0, 3, 0 })
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer());

        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] { 1, 0, 2, 0, 3, 0 })
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer());

        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));
    }

    @Test
    public void bulkGet() {
        var buffer = ShortBuffer.wrap(new short[] { 1, 2, 3, 4, 5, 6 });
        var arr = new short[3];

        buffer.get(arr);
        assertArrayEquals(new short[] { 1, 2, 3 }, arr);
        assertEquals(3, buffer.position());

        buffer.get(1, arr);
        assertArrayEquals(new short[] { 2, 3, 4 }, arr);
        assertEquals(3, buffer.position());

        buffer.get(4, arr, 1, 2);
        assertArrayEquals(new short[] { 2, 5, 6 }, arr);
        assertEquals(3, buffer.position());
    }

    @Test
    public void bulkGetWrapper() {
        var buffer = ByteBuffer.wrap(new byte[] { 1, 0, 2, 0, 3, 0, 4, 0, 5, 0, 6, 0 })
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer();
        var arr = new short[3];

        buffer.get(arr);
        assertArrayEquals(new short[] { 1, 2, 3 }, arr);
        assertEquals(3, buffer.position());

        buffer.get(1, arr);
        assertArrayEquals(new short[] { 2, 3, 4 }, arr);
        assertEquals(3, buffer.position());

        buffer.get(4, arr, 1, 2);
        assertArrayEquals(new short[] { 2, 5, 6 }, arr);
        assertEquals(3, buffer.position());

        buffer = ByteBuffer.wrap(new byte[] { 0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 0, 6 })
                .order(ByteOrder.BIG_ENDIAN)
                .asShortBuffer();

        buffer.get(arr);
        assertArrayEquals(new short[] { 1, 2, 3 }, arr);
        assertEquals(3, buffer.position());

        buffer.get(1, arr);
        assertArrayEquals(new short[] { 2, 3, 4 }, arr);
        assertEquals(3, buffer.position());

        buffer.get(4, arr, 1, 2);
        assertArrayEquals(new short[] { 2, 5, 6 }, arr);
        assertEquals(3, buffer.position());
    }
}
