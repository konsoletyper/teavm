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
import java.nio.IntBuffer;
import java.nio.InvalidMarkException;
import java.nio.ReadOnlyBufferException;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class IntBufferTest {
    @Test
    public void allocatesSimple() {
        IntBuffer buffer = IntBuffer.allocate(100);
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
        var buffer = ByteBuffer.allocateDirect(20).asIntBuffer();
        var ints = new int[] { 1, 2, 3 };
        buffer.put(0, ints);
        var intsCopy = new int[ints.length];
        buffer.get(0, intsCopy);
        assertArrayEquals(ints, intsCopy);
    }

    @Test
    public void bulkTransferRelative() {
        var arr = new int[5];
        var buffer = IntBuffer.wrap(arr);
        var src = IntBuffer.wrap(new int[] { 1, 2, 3 });
        buffer.put(src);
        assertArrayEquals(new int[] { 1, 2, 3, 0, 0 }, arr);
        assertEquals(3, buffer.position());
        assertEquals(3, src.position());

        assertThrows(BufferOverflowException.class, () -> buffer.put(IntBuffer.wrap(new int[] { 4, 5, 6 })));
        assertThrows(ReadOnlyBufferException.class, () -> buffer.rewind().asReadOnlyBuffer()
                .put(IntBuffer.wrap(new int[] { 4, 5, 6 })));
    }

    @Test
    public void errorIfAllocatingBufferOfNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> IntBuffer.allocate(-1));
    }

    @Test
    public void wrapsArray() {
        int[] array = new int[100];
        IntBuffer buffer = IntBuffer.wrap(array, 10, 70);
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
        assertEquals(23, buffer.get(0));
        buffer.put(1, 24);
        assertEquals(24, array[1]);
    }

    @Test
    public void errorWhenWrappingWithWrongParameters() {
        int[] array = new int[100];
        try {
            IntBuffer.wrap(array, -1, 10);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            IntBuffer.wrap(array, 101, 10);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            IntBuffer.wrap(array, 98, 3);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            IntBuffer.wrap(array, 98, -1);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
    }

    @Test
    public void wrapsArrayWithoutOffset() {
        int[] array = new int[100];
        IntBuffer buffer = IntBuffer.wrap(array);
        assertEquals(0, buffer.position());
        assertEquals(100, buffer.limit());
    }

    @Test
    public void createsSlice() {
        IntBuffer buffer = IntBuffer.allocate(100);
        buffer.put(new int[60]);
        buffer.flip();
        buffer.put(new int[15]);
        IntBuffer slice = buffer.slice();
        assertArrayEquals(buffer.array(), slice.array());
        assertEquals(0, slice.position());
        assertEquals(45, slice.capacity());
        assertEquals(45, slice.limit());
        assertFalse(slice.isDirect());
        assertFalse(slice.isReadOnly());
        slice.put(3, 23);
        assertEquals(23, buffer.get(18));
        slice.put(24);
        assertEquals(24, buffer.get(15));
        buffer.put(16, 25);
        assertEquals(25, slice.get(1));
    }


    @Test
    public void sliceOfSlice() {
        var buffer = IntBuffer.allocate(100);
        buffer.put(new int[10]);
        var slice1 = buffer.slice();
        slice1.put(new int[15]);
        var slice2 = slice1.slice();

        System.out.println("slice2 = " + slice2);
        assertEquals(25, slice2.arrayOffset());
        assertEquals(75, slice2.capacity());
    }

    @Test
    public void slicePropertiesSameWithOriginal() {
        IntBuffer buffer = IntBuffer.allocate(100).asReadOnlyBuffer().slice();
        assertTrue(buffer.isReadOnly());
    }

    @Test
    public void createsDuplicate() {
        IntBuffer buffer = IntBuffer.allocate(100);
        buffer.put(new int[60]);
        buffer.flip();
        buffer.put(new int[15]);
        IntBuffer duplicate = buffer.duplicate();
        assertArrayEquals(buffer.array(), duplicate.array());
        assertEquals(15, duplicate.position());
        assertEquals(100, duplicate.capacity());
        assertEquals(60, duplicate.limit());
        assertFalse(duplicate.isDirect());
        assertFalse(duplicate.isReadOnly());
        duplicate.put(3, 23);
        assertEquals(23, buffer.get(3));
        duplicate.put(24);
        assertEquals(24, buffer.get(15));
        buffer.put(1, 25);
        assertEquals(25, duplicate.get(1));
        assertSame(buffer.array(), duplicate.array());
    }

    @Test
    public void getsInt() {
        int[] array = { 2, 3, 5, 7 };
        IntBuffer buffer = IntBuffer.wrap(array);
        assertEquals(2, buffer.get());
        assertEquals(3, buffer.get());
        buffer = buffer.slice();
        assertEquals(5, buffer.get());
        assertEquals(7, buffer.get());
    }

    @Test
    public void gettingIntFromEmptyBufferCausesError() {
        int[] array = { 2, 3, 5, 7 };
        IntBuffer buffer = IntBuffer.wrap(array);
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
    public void putsInt() {
        int[] array = new int[4];
        IntBuffer buffer = IntBuffer.wrap(array);
        buffer.put(2).put(3).put(5).put(7);
        assertArrayEquals(new int[] { 2, 3, 5, 7 }, array);
    }

    @Test
    public void puttingIntToEmptyBufferCausesError() {
        int[] array = new int[4];
        IntBuffer buffer = IntBuffer.wrap(array);
        buffer.limit(2);
        buffer.put(2).put(3);
        try {
            buffer.put(5);
            fail("Should have thrown error");
        } catch (BufferOverflowException e) {
            assertEquals(0, array[2]);
        }
    }

    @Test
    public void puttingIntToReadOnlyBufferCausesError() {
        int[] array = new int[4];
        IntBuffer buffer = IntBuffer.wrap(array).asReadOnlyBuffer();
        assertThrows(ReadOnlyBufferException.class, () -> buffer.put(2));
    }

    @Test
    public void getsIntFromGivenLocation() {
        int[] array = { 2, 3, 5, 7 };
        IntBuffer buffer = IntBuffer.wrap(array);
        assertEquals(2, buffer.get(0));
        assertEquals(3, buffer.get(1));
        buffer.get();
        buffer = buffer.slice();
        assertEquals(5, buffer.get(1));
        assertEquals(7, buffer.get(2));
    }

    @Test
    public void gettingIntFromWrongLocationCausesError() {
        int[] array = { 2, 3, 5, 7 };
        IntBuffer buffer = IntBuffer.wrap(array);
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
    public void putsIntToGivenLocation() {
        int[] array = new int[4];
        IntBuffer buffer = IntBuffer.wrap(array);
        buffer.put(0, 2);
        buffer.put(1, 3);
        buffer.get();
        buffer = buffer.slice();
        buffer.put(1, 5);
        buffer.put(2, 7);
        assertArrayEquals(new int[] { 2, 3, 5, 7 }, array);
    }

    @Test
    public void puttingIntToWrongLocationCausesError() {
        int[] array = new int[4];
        IntBuffer buffer = IntBuffer.wrap(array);
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
    public void puttingIntToGivenLocationOfReadOnlyBufferCausesError() {
        int[] array = new int[4];
        IntBuffer buffer = IntBuffer.wrap(array).asReadOnlyBuffer();
        assertThrows(ReadOnlyBufferException.class, () -> buffer.put(0, 2));
    }

    @Test
    public void getsInts() {
        int[] array = { 2, 3, 5, 7 };
        IntBuffer buffer = IntBuffer.wrap(array);
        buffer.get();
        int[] receiver = new int[2];
        buffer.get(receiver, 0, 2);
        assertEquals(3, buffer.position());
        assertArrayEquals(new int[] { 3, 5 }, receiver);
    }

    @Test
    public void gettingIntsFromEmptyBufferCausesError() {
        int[] array = { 2, 3, 5, 7 };
        IntBuffer buffer = IntBuffer.wrap(array);
        buffer.limit(3);
        int[] receiver = new int[4];
        try {
            buffer.get(receiver, 0, 4);
            fail("Error expected");
        } catch (BufferUnderflowException e) {
            assertArrayEquals(new int[4], receiver);
            assertEquals(0, buffer.position());
        }
    }

    @Test
    public void gettingIntsWithIllegalArgumentsCausesError() {
        int[] array = { 2, 3, 5, 7 };
        IntBuffer buffer = IntBuffer.wrap(array);
        int[] receiver = new int[4];
        try {
            buffer.get(receiver, 0, 5);
        } catch (IndexOutOfBoundsException e) {
            assertArrayEquals(new int[4], receiver);
            assertEquals(0, buffer.position());
        }
        try {
            buffer.get(receiver, -1, 3);
        } catch (IndexOutOfBoundsException e) {
            assertArrayEquals(new int[4], receiver);
            assertEquals(0, buffer.position());
        }
        try {
            buffer.get(receiver, 6, 3);
        } catch (IndexOutOfBoundsException e) {
            assertArrayEquals(new int[4], receiver);
            assertEquals(0, buffer.position());
        }
    }

    @Test
    public void putsInts() {
        int[] array = new int[4];
        IntBuffer buffer = IntBuffer.wrap(array);
        buffer.get();
        int[] data = { 2, 3 };
        buffer.put(data, 0, 2);
        assertEquals(3, buffer.position());
        assertArrayEquals(new int[] {0, 2, 3, 0 }, array);
    }

    @Test
    public void compacts() {
        int[] array = { 2, 3, 5, 7 };
        IntBuffer buffer = IntBuffer.wrap(array);
        buffer.get();
        buffer.mark();
        buffer.compact();
        assertArrayEquals(new int[] { 3, 5, 7, 7 }, array);
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
        int[] array = { 2, 3, 5, 7 };
        IntBuffer buffer = IntBuffer.wrap(array);
        buffer.position(1);
        buffer.mark();
        buffer.position(2);
        buffer.reset();
        assertEquals(1, buffer.position());
    }

    @Test
    public void putEmptyArray() {
        IntBuffer ib = IntBuffer.allocate(0);
        ib.put(new int[0]);
        ib.get(new int[0]);
    }

    @Test
    public void bulkPut() {
        var buffer = IntBuffer.allocate(100);
        buffer.put(new int[] { 1, 2, 3 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer.put(1, new int[] { 4, 5, 6 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(4, buffer.get(1));
        assertEquals(5, buffer.get(2));
        assertEquals(6, buffer.get(3));

        buffer.put(0, new int[] { 7, 8, 9, 10 }, 1, 2);
        assertEquals(8, buffer.get(0));
        assertEquals(9, buffer.get(1));
        assertEquals(5, buffer.get(2));
        assertEquals(6, buffer.get(3));
    }

    @Test
    public void bulkPutWrapper() {
        var byteBuffer = ByteBuffer.allocate(100);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        var buffer = byteBuffer.asIntBuffer();

        buffer.put(new int[] { 1, 2, 3 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer.put(1, new int[] { 4, 5, 6 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(4, buffer.get(1));
        assertEquals(5, buffer.get(2));
        assertEquals(6, buffer.get(3));
        assertEquals(0, byteBuffer.get(0));
        assertEquals(1, byteBuffer.get(3));

        buffer.put(0, new int[] { 7, 8, 9, 10 }, 1, 2);
        assertEquals(8, buffer.get(0));
        assertEquals(9, buffer.get(1));
        assertEquals(5, buffer.get(2));
        assertEquals(6, buffer.get(3));

        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer = byteBuffer.asIntBuffer();

        buffer.put(new int[] { 1, 2, 3 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer.put(1, new int[] { 4, 5, 6 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(4, buffer.get(1));
        assertEquals(5, buffer.get(2));
        assertEquals(6, buffer.get(3));
        assertEquals(1, byteBuffer.get(0));
        assertEquals(0, byteBuffer.get(3));

        buffer.put(0, new int[] { 7, 8, 9, 10 }, 1, 2);
        assertEquals(8, buffer.get(0));
        assertEquals(9, buffer.get(1));
        assertEquals(5, buffer.get(2));
        assertEquals(6, buffer.get(3));
    }

    @Test
    public void bulkPutBuffer() {
        var buffer = IntBuffer.allocate(100);
        buffer.put(IntBuffer.wrap(new int[] { 1, 2, 3 }));

        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer.put(1, IntBuffer.wrap(new int[] { 4, 5, 6 }), 1, 2);
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0));
        assertEquals(5, buffer.get(1));
        assertEquals(6, buffer.get(2));
    }

    @Test
    public void bulkPutBufferWrapper() {
        var buffer = ByteBuffer.allocate(100).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] { 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 3 })
                .order(ByteOrder.BIG_ENDIAN)
                .asIntBuffer());

        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] { 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 3 })
                .order(ByteOrder.BIG_ENDIAN)
                .asIntBuffer());

        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer = ByteBuffer.allocate(100).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] { 1, 0, 0, 0, 2, 0, 0, 0, 3, 0, 0, 0 })
                .order(ByteOrder.LITTLE_ENDIAN)
                .asIntBuffer());

        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));

        buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] { 1, 0, 0, 0, 2, 0, 0, 0, 3, 0, 0, 0 })
                .order(ByteOrder.LITTLE_ENDIAN)
                .asIntBuffer());

        assertEquals(1, buffer.get(0));
        assertEquals(2, buffer.get(1));
        assertEquals(3, buffer.get(2));
    }

    @Test
    public void bulkGet() {
        var buffer = IntBuffer.wrap(new int[] { 1, 2, 3, 4, 5, 6 });
        var arr = new int[3];

        buffer.get(arr);
        assertArrayEquals(new int[] { 1, 2, 3 }, arr);
        assertEquals(3, buffer.position());

        buffer.get(1, arr);
        assertArrayEquals(new int[] { 2, 3, 4 }, arr);
        assertEquals(3, buffer.position());

        buffer.get(4, arr, 1, 2);
        assertArrayEquals(new int[] { 2, 5, 6 }, arr);
        assertEquals(3, buffer.position());
    }

    @Test
    public void bulkGetWrapper() {
        var buffer = ByteBuffer.wrap(new byte[] { 1, 0, 0, 0, 2, 0, 0, 0, 3, 0, 0, 0, 4, 0, 0, 0, 5, 0, 0, 0,
                        6, 0, 0, 0 })
                .order(ByteOrder.LITTLE_ENDIAN)
                .asIntBuffer();
        var arr = new int[3];

        buffer.get(arr);
        assertArrayEquals(new int[] { 1, 2, 3 }, arr);
        assertEquals(3, buffer.position());

        buffer.get(1, arr);
        assertArrayEquals(new int[] { 2, 3, 4 }, arr);
        assertEquals(3, buffer.position());

        buffer.get(4, arr, 1, 2);
        assertArrayEquals(new int[] { 2, 5, 6 }, arr);
        assertEquals(3, buffer.position());

        buffer = ByteBuffer.wrap(new byte[] { 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 3, 0, 0, 0, 4, 0, 0, 0, 5,
                        0, 0, 0, 6 })
                .order(ByteOrder.BIG_ENDIAN)
                .asIntBuffer();

        buffer.get(arr);
        assertArrayEquals(new int[] { 1, 2, 3 }, arr);
        assertEquals(3, buffer.position());

        buffer.get(1, arr);
        assertArrayEquals(new int[] { 2, 3, 4 }, arr);
        assertEquals(3, buffer.position());

        buffer.get(4, arr, 1, 2);
        assertArrayEquals(new int[] { 2, 5, 6 }, arr);
        assertEquals(3, buffer.position());
    }
    
    @Test
    public void putsBufferViewToView() {
        var dst = ByteBuffer.allocate(16).asIntBuffer();
        dst.put(1);
        var src = ByteBuffer.allocate(8).asIntBuffer();
        src.put(new int[] { 2, 3 });
        src.flip();
        dst.put(src);
        dst.flip();

        var check = new int[3];
        dst.get(0, check);
        assertArrayEquals(new int[] { 1, 2, 3 }, check);
    }
}
