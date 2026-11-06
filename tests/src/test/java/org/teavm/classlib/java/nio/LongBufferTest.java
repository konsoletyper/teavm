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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.InvalidMarkException;
import java.nio.LongBuffer;
import java.nio.ReadOnlyBufferException;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class LongBufferTest {
    @Test
    public void allocatesSimple() {
        LongBuffer buffer = LongBuffer.allocate(100);
        assertFalse(buffer.isDirect());
        assertFalse(buffer.isReadOnly());
        assertTrue(buffer.hasArray());
        assertEquals(100, buffer.capacity());
        assertEquals(0, buffer.position());
        assertEquals(100, buffer.limit());
        try {
            buffer.reset();
            fail("Mark is expected to be undefined");
        } catch (InvalidMarkException e) {
            // ok
        }
    }

    @Test
    public void bulkTransferDirect() {
        var buffer = ByteBuffer.allocateDirect(40).asLongBuffer();
        var longs = new long[] { 1, 2, 3 };
        buffer.put(0, longs);
        var longsCopy = new long[longs.length];
        buffer.get(0, longsCopy);
        assertArrayEquals(longs, longsCopy);
    }

    @Test
    public void bulkTransferRelative() {
        var arr = new long[5];
        var buffer = LongBuffer.wrap(arr);
        var src = LongBuffer.wrap(new long[] { 1L, 2L, 3L });
        buffer.put(src);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 0L, 0L }, arr);
        assertEquals(3, buffer.position());
        assertEquals(3, src.position());

        assertThrows(BufferOverflowException.class, () -> buffer.put(LongBuffer.wrap(new long[] { 4L, 5L, 6L })));
        assertThrows(ReadOnlyBufferException.class, () -> buffer.rewind().asReadOnlyBuffer()
                .put(LongBuffer.wrap(new long[] { 4L, 5L, 6L })));
    }


    @Test
    public void errorIfAllocatingBufferOfNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> LongBuffer.allocate(-1));
    }

    @Test
    public void wrapsArray() {
        long[] array = new long[100];
        LongBuffer buffer = LongBuffer.wrap(array, 10, 70);
        assertFalse(buffer.isDirect());
        assertFalse(buffer.isReadOnly());
        assertTrue(buffer.hasArray());
        assertArrayEquals(array, buffer.array());
        assertEquals(0, buffer.arrayOffset());
        assertEquals(100, buffer.capacity());
        assertEquals(10, buffer.position());
        assertEquals(80, buffer.limit());
        try {
            buffer.reset();
            fail("Mark is expected to be undefined");
        } catch (InvalidMarkException e) {
            // ok
        }
        array[0] = 23;
        assertEquals((long) 23, buffer.get(0));
        buffer.put(1, 24);
        assertEquals((long) 24, array[1]);
    }

    @Test
    public void errorWhenWrappingWithWrongParameters() {
        long[] array = new long[100];
        try {
            LongBuffer.wrap(array, -1, 10);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            LongBuffer.wrap(array, 101, 10);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            LongBuffer.wrap(array, 98, 3);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            LongBuffer.wrap(array, 98, -1);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
    }

    @Test
    public void wrapsArrayWithoutOffset() {
        long[] array = new long[100];
        LongBuffer buffer = LongBuffer.wrap(array);
        assertEquals(0, buffer.position());
        assertEquals(100, buffer.limit());
    }

    @Test
    public void createsSlice() {
        LongBuffer buffer = LongBuffer.allocate(100);
        buffer.put(new long[60]);
        buffer.flip();
        buffer.put(new long[15]);
        LongBuffer slice = buffer.slice();
        assertArrayEquals(buffer.array(), slice.array());
        assertEquals(0, slice.position());
        assertEquals(45, slice.capacity());
        assertEquals(45, slice.limit());
        assertFalse(slice.isDirect());
        assertFalse(slice.isReadOnly());
        slice.put(3, 23);
        assertEquals((long) 23, buffer.get(18));
        slice.put(24);
        assertEquals((long) 24, buffer.get(15));
        buffer.put(16, 25);
        assertEquals((long) 25, slice.get(1));
    }

    @Test
    public void slicePropertiesSameWithOriginal() {
        LongBuffer buffer = LongBuffer.allocate(100).asReadOnlyBuffer().slice();
        assertTrue(buffer.isReadOnly());
    }

    @Test
    public void createsDuplicate() {
        LongBuffer buffer = LongBuffer.allocate(100);
        buffer.put(new long[60]);
        buffer.flip();
        buffer.put(new long[15]);
        LongBuffer duplicate = buffer.duplicate();
        assertArrayEquals(buffer.array(), duplicate.array());
        assertEquals(15, duplicate.position());
        assertEquals(100, duplicate.capacity());
        assertEquals(60, duplicate.limit());
        assertFalse(duplicate.isDirect());
        assertFalse(duplicate.isReadOnly());
        duplicate.put(3, 23);
        assertEquals((long) 23, buffer.get(3));
        duplicate.put(24);
        assertEquals((long) 24, buffer.get(15));
        buffer.put(1, 25);
        assertEquals((long) 25, duplicate.get(1));
        assertSame(buffer.array(), duplicate.array());
    }

    @Test
    public void getsLong() {
        long[] array = { 2, 3, 5, 7 };
        LongBuffer buffer = LongBuffer.wrap(array);
        assertEquals((long) 2, buffer.get());
        assertEquals((long) 3, buffer.get());
        buffer = buffer.slice();
        assertEquals((long) 5, buffer.get());
        assertEquals((long) 7, buffer.get());
    }

    @Test
    public void gettingLongFromEmptyBufferCausesError() {
        long[] array = { 2, 3, 5, 7 };
        LongBuffer buffer = LongBuffer.wrap(array);
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
    public void putsLong() {
        long[] array = new long[4];
        LongBuffer buffer = LongBuffer.wrap(array);
        buffer.put(2).put(3).put(5).put(7);
        assertArrayEquals(new long[] { 2, 3, 5, 7 }, array);
    }

    @Test
    public void puttingLongToEmptyBufferCausesError() {
        long[] array = new long[4];
        LongBuffer buffer = LongBuffer.wrap(array);
        buffer.limit(2);
        buffer.put(2).put(3);
        try {
            buffer.put(5);
            fail("Should have thrown error");
        } catch (BufferOverflowException e) {
            assertEquals((long) 0, array[2]);
        }
    }

    @Test
    public void puttingLongToReadOnlyBufferCausesError() {
        long[] array = new long[4];
        LongBuffer buffer = LongBuffer.wrap(array).asReadOnlyBuffer();
        assertThrows(ReadOnlyBufferException.class, () -> buffer.put(2));
    }

    @Test
    public void getsLongFromGivenLocation() {
        long[] array = { 2, 3, 5, 7 };
        LongBuffer buffer = LongBuffer.wrap(array);
        assertEquals((long) 2, buffer.get(0));
        assertEquals((long) 3, buffer.get(1));
        buffer.get();
        buffer = buffer.slice();
        assertEquals((long) 5, buffer.get(1));
        assertEquals((long) 7, buffer.get(2));
    }

    @Test
    public void gettingLongFromWrongLocationCausesError() {
        long[] array = { 2, 3, 5, 7 };
        LongBuffer buffer = LongBuffer.wrap(array);
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
    public void putsLongToGivenLocation() {
        long[] array = new long[4];
        LongBuffer buffer = LongBuffer.wrap(array);
        buffer.put(0, 2);
        buffer.put(1, 3);
        buffer.get();
        buffer = buffer.slice();
        buffer.put(1, 5);
        buffer.put(2, 7);
        assertArrayEquals(new long[] { 2, 3, 5, 7 }, array);
    }

    @Test
    public void puttingLongToWrongLocationCausesError() {
        long[] array = new long[4];
        LongBuffer buffer = LongBuffer.wrap(array);
        buffer.limit(3);
        try {
            buffer.put(-1, 2);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            buffer.put(3, 2);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
    }

    @Test
    public void puttingLongToGivenLocationOfReadOnlyBufferCausesError() {
        long[] array = new long[4];
        LongBuffer buffer = LongBuffer.wrap(array).asReadOnlyBuffer();
        assertThrows(ReadOnlyBufferException.class, () -> buffer.put(0, 2));
    }

    @Test
    public void getsLongs() {
        long[] array = { 2, 3, 5, 7 };
        LongBuffer buffer = LongBuffer.wrap(array);
        buffer.get();
        long[] receiver = new long[2];
        buffer.get(receiver, 0, 2);
        assertEquals(3, buffer.position());
        assertArrayEquals(new long[] { 3, 5 }, receiver);
    }

    @Test
    public void gettingLongsFromEmptyBufferCausesError() {
        long[] array = { 2, 3, 5, 7 };
        LongBuffer buffer = LongBuffer.wrap(array);
        buffer.limit(3);
        long[] receiver = new long[4];
        try {
            buffer.get(receiver, 0, 4);
            fail("Error expected");
        } catch (BufferUnderflowException e) {
            assertArrayEquals(new long[4], receiver);
            assertEquals(0, buffer.position());
        }
    }

    @Test
    public void gettingLongsWithIllegalArgumentsCausesError() {
        long[] array = { 2, 3, 5, 7 };
        LongBuffer buffer = LongBuffer.wrap(array);
        long[] receiver = new long[4];
        try {
            buffer.get(receiver, 0, 5);
        } catch (IndexOutOfBoundsException e) {
            assertArrayEquals(new long[4], receiver);
            assertEquals(0, buffer.position());
        }
        try {
            buffer.get(receiver, -1, 3);
        } catch (IndexOutOfBoundsException e) {
            assertArrayEquals(new long[4], receiver);
            assertEquals(0, buffer.position());
        }
        try {
            buffer.get(receiver, 6, 3);
        } catch (IndexOutOfBoundsException e) {
            assertArrayEquals(new long[4], receiver);
            assertEquals(0, buffer.position());
        }
    }

    @Test
    public void putsLongs() {
        long[] array = new long[4];
        LongBuffer buffer = LongBuffer.wrap(array);
        buffer.get();
        long[] data = { 2, 3 };
        buffer.put(data, 0, 2);
        assertEquals(3, buffer.position());
        assertArrayEquals(new long[] { 0, 2, 3, 0 }, array);
    }

    @Test
    public void compacts() {
        long[] array = { 2, 3, 5, 7 };
        LongBuffer buffer = LongBuffer.wrap(array);
        buffer.get();
        buffer.mark();
        buffer.compact();
        assertArrayEquals(new long[] { 3, 5, 7, 7 }, array);
        assertEquals(3, buffer.position());
        assertEquals(4, buffer.limit());
        assertEquals(4, buffer.capacity());
        try {
            buffer.reset();
            fail("Exception expected");
        } catch (InvalidMarkException e) {
            // ok
        }
    }

    @Test
    public void marksPosition() {
        long[] array = { 2, 3, 5, 7 };
        LongBuffer buffer = LongBuffer.wrap(array);
        buffer.position(1);
        buffer.mark();
        buffer.position(2);
        buffer.reset();
        assertEquals(1, buffer.position());
    }

    @Test
    public void putEmptyArray() {
        LongBuffer lb = LongBuffer.allocate(0);
        lb.put(new long[0]);
        lb.get(new long[0]);
    }

    @Test
    public void bulkPut() {
        var buffer = LongBuffer.allocate(100);
        buffer.put(new long[] { 1, 2, 3 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer.put(1, new long[] { 4, 5, 6 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(4, buffer.get(1));
        assertEquals(5, buffer.get(2));
        assertEquals(6, buffer.get(3));

        buffer.put(0, new long[] { 7, 8, 9, 10 }, 1, 2);
        assertEquals(8, buffer.get(0));
        assertEquals(9, buffer.get(1));
        assertEquals(5, buffer.get(2));
        assertEquals(6, buffer.get(3));
    }

    @Test
    public void bulkPutWrapper() {
        var byteBuffer = ByteBuffer.allocate(100);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        var buffer = byteBuffer.asLongBuffer();

        buffer.put(new long[] { 1, 2, 3 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer.put(1, new long[] { 4, 5, 6 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(4, buffer.get(1));
        assertEquals(5, buffer.get(2));
        assertEquals(6, buffer.get(3));
        assertEquals(0, byteBuffer.get(0));
        assertEquals(1, byteBuffer.get(7));

        buffer.put(0, new long[] { 7, 8, 9, 10 }, 1, 2);
        assertEquals(8, buffer.get(0));
        assertEquals(9, buffer.get(1));
        assertEquals(5, buffer.get(2));
        assertEquals(6, buffer.get(3));

        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer = byteBuffer.asLongBuffer();

        buffer.put(new long[] { 1, 2, 3 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer.put(1, new long[] { 4, 5, 6 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(4, buffer.get(1));
        assertEquals(5, buffer.get(2));
        assertEquals(6, buffer.get(3));
        assertEquals(1, byteBuffer.get(0));
        assertEquals(0, byteBuffer.get(7));

        buffer.put(0, new long[] { 7, 8, 9, 10 }, 1, 2);
        assertEquals(8, buffer.get(0));
        assertEquals(9, buffer.get(1));
        assertEquals(5, buffer.get(2));
        assertEquals(6, buffer.get(3));
    }

    @Test
    public void bulkPutBuffer() {
        var buffer = LongBuffer.allocate(100);
        buffer.put(LongBuffer.wrap(new long[] { 1, 2, 3 }));

        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer.put(1, LongBuffer.wrap(new long[] { 4, 5, 6 }), 1, 2);
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(5, buffer.get(1));
        assertEquals(6, buffer.get(2));
    }

    @Test
    public void bulkPutBufferWrapper() {
        var buffer = ByteBuffer.allocate(100).order(ByteOrder.BIG_ENDIAN).asLongBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] {
                        0, 0, 0, 0, 0, 0, 0, 1,
                        0, 0, 0, 0, 0, 0, 0, 2,
                        0, 0, 0, 0, 0, 0, 0, 3 })
                .order(ByteOrder.BIG_ENDIAN)
                .asLongBuffer());

        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] {
                        0, 0, 0, 0, 0, 0, 0, 1,
                        0, 0, 0, 0, 0, 0, 0, 2,
                        0, 0, 0, 0, 0, 0, 0, 3 })
                .order(ByteOrder.BIG_ENDIAN)
                .asLongBuffer());

        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer = ByteBuffer.allocate(100).order(ByteOrder.BIG_ENDIAN).asLongBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] {
                        1, 0, 0, 0, 0, 0, 0, 0,
                        2, 0, 0, 0, 0, 0, 0, 0,
                        3, 0, 0, 0, 0, 0, 0, 0 })
                .order(ByteOrder.LITTLE_ENDIAN)
                .asLongBuffer());

        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] {
                        1, 0, 0, 0, 0, 0, 0, 0,
                        2, 0, 0, 0, 0, 0, 0, 0,
                        3, 0, 0, 0, 0, 0, 0, 0 })
                .order(ByteOrder.LITTLE_ENDIAN)
                .asLongBuffer());

        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));
    }

    @Test
    public void bulkGet() {
        var buffer = LongBuffer.wrap(new long[] { 1, 2, 3, 4, 5, 6 });
        var arr = new long[3];

        buffer.get(arr);
        assertArrayEquals(new long[] { 1, 2, 3 }, arr);
        assertEquals(3, buffer.position());

        buffer.get(1, arr);
        assertArrayEquals(new long[] { 2, 3, 4 }, arr);
        assertEquals(3, buffer.position());

        buffer.get(4, arr, 1, 2);
        assertArrayEquals(new long[] { 2, 5, 6 }, arr);
        assertEquals(3, buffer.position());
    }

    @Test
    public void bulkGetWrapper() {
        var buffer = ByteBuffer.wrap(new byte[] {
                        1, 0, 0, 0, 0, 0, 0, 0,
                        2, 0, 0, 0, 0, 0, 0, 0,
                        3, 0, 0, 0, 0, 0, 0, 0,
                        4, 0, 0, 0, 0, 0, 0, 0,
                        5, 0, 0, 0, 0, 0, 0, 0,
                        6, 0, 0, 0, 0, 0, 0, 0 })
                .order(ByteOrder.LITTLE_ENDIAN)
                .asLongBuffer();
        var arr = new long[3];

        buffer.get(arr);
        assertArrayEquals(new long[] { 1, 2, 3 }, arr);
        assertEquals(3, buffer.position());

        buffer.get(1, arr);
        assertArrayEquals(new long[] { 2, 3, 4 }, arr);
        assertEquals(3, buffer.position());

        buffer.get(4, arr, 1, 2);
        assertArrayEquals(new long[] { 2, 5, 6 }, arr);
        assertEquals(3, buffer.position());

        buffer = ByteBuffer.wrap(new byte[] {
                        0, 0, 0, 0, 0, 0, 0, 1,
                        0, 0, 0, 0, 0, 0, 0, 2,
                        0, 0, 0, 0, 0, 0, 0, 3,
                        0, 0, 0, 0, 0, 0, 0, 4,
                        0, 0, 0, 0, 0, 0, 0, 5,
                        0, 0, 0, 0, 0, 0, 0, 6 })
                .order(ByteOrder.BIG_ENDIAN)
                .asLongBuffer();

        buffer.get(arr);
        assertArrayEquals(new long[] { 1, 2, 3 }, arr);
        assertEquals(3, buffer.position());

        buffer.get(1, arr);
        assertArrayEquals(new long[] { 2, 3, 4 }, arr);
        assertEquals(3, buffer.position());

        buffer.get(4, arr, 1, 2);
        assertArrayEquals(new long[] { 2, 5, 6 }, arr);
        assertEquals(3, buffer.position());
    }

    @Test
    public void putsBufferViewToView() {
        var dst = ByteBuffer.allocate(32).asLongBuffer();
        dst.put(1);
        var src = ByteBuffer.allocate(16).asLongBuffer();
        src.put(new long[] { 2, 3 });
        src.flip();
        dst.put(src);
        dst.flip();

        var check = new long[3];
        dst.get(0, check);
        assertArrayEquals(new long[] { 1, 2, 3 }, check);
    }
}
