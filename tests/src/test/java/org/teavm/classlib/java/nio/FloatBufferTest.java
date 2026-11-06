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
import java.nio.FloatBuffer;
import java.nio.InvalidMarkException;
import java.nio.ReadOnlyBufferException;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class FloatBufferTest {
    @Test
    public void allocatesSimple() {
        FloatBuffer buffer = FloatBuffer.allocate(100);
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
        var buffer = ByteBuffer.allocateDirect(40).asFloatBuffer();
        var floats = new float[] { 1, 2, 3 };
        buffer.put(0, floats);
        var floatsCopy = new float[floats.length];
        buffer.get(0, floatsCopy);
        assertArrayEquals(floats, floatsCopy, 0.1f);
    }

    @Test
    public void bulkTransferRelative() {
        var arr = new float[5];
        var buffer = FloatBuffer.wrap(arr);
        var src = FloatBuffer.wrap(new float[] { 1.0f, 2.0f, 3.0f });
        buffer.put(src);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 0.0f, 0.0f }, arr, 0.0f);
        assertEquals(3, buffer.position());
        assertEquals(3, src.position());

        assertThrows(BufferOverflowException.class, () -> buffer.put(
                FloatBuffer.wrap(new float[] { 4.0f, 5.0f, 6.0f })));
        assertThrows(ReadOnlyBufferException.class, () -> buffer.rewind().asReadOnlyBuffer()
                .put(FloatBuffer.wrap(new float[] { 4.0f, 5.0f, 6.0f })));
    }


    @Test
    public void errorIfAllocatingBufferOfNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> FloatBuffer.allocate(-1));
    }

    @Test
    public void wrapsArray() {
        float[] array = new float[100];
        FloatBuffer buffer = FloatBuffer.wrap(array, 10, 70);
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
        assertEquals((float) 23, buffer.get(0));
        buffer.put(1, 24);
        assertEquals((float) 24, array[1]);
    }

    @Test
    public void errorWhenWrappingWithWrongParameters() {
        float[] array = new float[100];
        try {
            FloatBuffer.wrap(array, -1, 10);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            FloatBuffer.wrap(array, 101, 10);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            FloatBuffer.wrap(array, 98, 3);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            FloatBuffer.wrap(array, 98, -1);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
    }

    @Test
    public void wrapsArrayWithoutOffset() {
        float[] array = new float[100];
        FloatBuffer buffer = FloatBuffer.wrap(array);
        assertEquals(0, buffer.position());
        assertEquals(100, buffer.limit());
    }

    @Test
    public void createsSlice() {
        FloatBuffer buffer = FloatBuffer.allocate(100);
        buffer.put(new float[60]);
        buffer.flip();
        buffer.put(new float[15]);
        FloatBuffer slice = buffer.slice();
        assertArrayEquals(buffer.array(), slice.array());
        assertEquals(0, slice.position());
        assertEquals(45, slice.capacity());
        assertEquals(45, slice.limit());
        assertFalse(slice.isDirect());
        assertFalse(slice.isReadOnly());
        slice.put(3, 23);
        assertEquals((float) 23, buffer.get(18));
        slice.put(24);
        assertEquals((float) 24, buffer.get(15));
        buffer.put(16, 25);
        assertEquals((float) 25, slice.get(1));
    }

    @Test
    public void slicePropertiesSameWithOriginal() {
        FloatBuffer buffer = FloatBuffer.allocate(100).asReadOnlyBuffer().slice();
        assertTrue(buffer.isReadOnly());
    }

    @Test
    public void createsDuplicate() {
        FloatBuffer buffer = FloatBuffer.allocate(100);
        buffer.put(new float[60]);
        buffer.flip();
        buffer.put(new float[15]);
        FloatBuffer duplicate = buffer.duplicate();
        assertArrayEquals(buffer.array(), duplicate.array());
        assertEquals(15, duplicate.position());
        assertEquals(100, duplicate.capacity());
        assertEquals(60, duplicate.limit());
        assertFalse(duplicate.isDirect());
        assertFalse(duplicate.isReadOnly());
        duplicate.put(3, 23);
        assertEquals((float) 23, buffer.get(3));
        duplicate.put(24);
        assertEquals((float) 24, buffer.get(15));
        buffer.put(1, 25);
        assertEquals((float) 25, duplicate.get(1));
        assertSame(buffer.array(), duplicate.array());
    }

    @Test
    public void getsFloat() {
        float[] array = { 2, 3, 5, 7 };
        FloatBuffer buffer = FloatBuffer.wrap(array);
        assertEquals((float) 2, buffer.get());
        assertEquals((float) 3, buffer.get());
        buffer = buffer.slice();
        assertEquals((float) 5, buffer.get());
        assertEquals((float) 7, buffer.get());
    }

    @Test
    public void gettingFloatFromEmptyBufferCausesError() {
        float[] array = { 2, 3, 5, 7 };
        FloatBuffer buffer = FloatBuffer.wrap(array);
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
    public void putsFloat() {
        float[] array = new float[4];
        FloatBuffer buffer = FloatBuffer.wrap(array);
        buffer.put(2).put(3).put(5).put(7);
        assertArrayEquals(new float[] { 2, 3, 5, 7 }, array);
    }

    @Test
    public void puttingFloatToEmptyBufferCausesError() {
        float[] array = new float[4];
        FloatBuffer buffer = FloatBuffer.wrap(array);
        buffer.limit(2);
        buffer.put(2).put(3);
        try {
            buffer.put(5);
            fail("Should have thrown error");
        } catch (BufferOverflowException e) {
            assertEquals((float) 0, array[2]);
        }
    }

    @Test
    public void puttingFloatToReadOnlyBufferCausesError() {
        float[] array = new float[4];
        FloatBuffer buffer = FloatBuffer.wrap(array).asReadOnlyBuffer();
        assertThrows(ReadOnlyBufferException.class, () -> buffer.put(2));
    }

    @Test
    public void getsFloatFromGivenLocation() {
        float[] array = { 2, 3, 5, 7 };
        FloatBuffer buffer = FloatBuffer.wrap(array);
        assertEquals((float) 2, buffer.get(0));
        assertEquals((float) 3, buffer.get(1));
        buffer.get();
        buffer = buffer.slice();
        assertEquals((float) 5, buffer.get(1));
        assertEquals((float) 7, buffer.get(2));
    }

    @Test
    public void gettingFloatFromWrongLocationCausesError() {
        float[] array = { 2, 3, 5, 7 };
        FloatBuffer buffer = FloatBuffer.wrap(array);
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
    public void putsFloatToGivenLocation() {
        float[] array = new float[4];
        FloatBuffer buffer = FloatBuffer.wrap(array);
        buffer.put(0, 2);
        buffer.put(1, 3);
        buffer.get();
        buffer = buffer.slice();
        buffer.put(1, 5);
        buffer.put(2, 7);
        assertArrayEquals(new float[] { 2, 3, 5, 7 }, array);
    }

    @Test
    public void puttingFloatToWrongLocationCausesError() {
        float[] array = new float[4];
        FloatBuffer buffer = FloatBuffer.wrap(array);
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
    public void puttingFloatToGivenLocationOfReadOnlyBufferCausesError() {
        float[] array = new float[4];
        FloatBuffer buffer = FloatBuffer.wrap(array).asReadOnlyBuffer();
        assertThrows(ReadOnlyBufferException.class, () -> buffer.put(0, 2));
    }

    @Test
    public void getsFloats() {
        float[] array = { 2, 3, 5, 7 };
        FloatBuffer buffer = FloatBuffer.wrap(array);
        buffer.get();
        float[] receiver = new float[2];
        buffer.get(receiver, 0, 2);
        assertEquals(3, buffer.position());
        assertArrayEquals(new float[] { 3, 5 }, receiver);
    }

    @Test
    public void gettingFloatsFromEmptyBufferCausesError() {
        float[] array = { 2, 3, 5, 7 };
        FloatBuffer buffer = FloatBuffer.wrap(array);
        buffer.limit(3);
        float[] receiver = new float[4];
        try {
            buffer.get(receiver, 0, 4);
            fail("Error expected");
        } catch (BufferUnderflowException e) {
            assertArrayEquals(new float[4], receiver);
            assertEquals(0, buffer.position());
        }
    }

    @Test
    public void gettingFloatsWithIllegalArgumentsCausesError() {
        float[] array = { 2, 3, 5, 7 };
        FloatBuffer buffer = FloatBuffer.wrap(array);
        float[] receiver = new float[4];
        try {
            buffer.get(receiver, 0, 5);
        } catch (IndexOutOfBoundsException e) {
            assertArrayEquals(new float[4], receiver);
            assertEquals(0, buffer.position());
        }
        try {
            buffer.get(receiver, -1, 3);
        } catch (IndexOutOfBoundsException e) {
            assertArrayEquals(new float[4], receiver);
            assertEquals(0, buffer.position());
        }
        try {
            buffer.get(receiver, 6, 3);
        } catch (IndexOutOfBoundsException e) {
            assertArrayEquals(new float[4], receiver);
            assertEquals(0, buffer.position());
        }
    }

    @Test
    public void putsFloats() {
        float[] array = new float[4];
        FloatBuffer buffer = FloatBuffer.wrap(array);
        buffer.get();
        float[] data = { 2, 3 };
        buffer.put(data, 0, 2);
        assertEquals(3, buffer.position());
        assertArrayEquals(new float[] { 0, 2, 3, 0 }, array);
    }

    @Test
    public void compacts() {
        float[] array = { 2, 3, 5, 7 };
        FloatBuffer buffer = FloatBuffer.wrap(array);
        buffer.get();
        buffer.mark();
        buffer.compact();
        assertArrayEquals(new float[] { 3, 5, 7, 7 }, array);
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
        float[] array = { 2, 3, 5, 7 };
        FloatBuffer buffer = FloatBuffer.wrap(array);
        buffer.position(1);
        buffer.mark();
        buffer.position(2);
        buffer.reset();
        assertEquals(1, buffer.position());
    }

    @Test
    public void putEmptyArray() {
        FloatBuffer fb = FloatBuffer.allocate(0);
        fb.put(new float[0]);
        fb.get(new float[0]);
    }

    @Test
    public void bulkPut() {
        var buffer = FloatBuffer.allocate(100);
        buffer.put(new float[] { 1, 2, 3 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(2, buffer.get(1), 0.1f);
        assertEquals(3, buffer.get(2), 0.1f);

        buffer.put(1, new float[] { 4, 5, 6 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(4, buffer.get(1), 0.1f);
        assertEquals(5, buffer.get(2), 0.1f);
        assertEquals(6, buffer.get(3), 0.1f);

        buffer.put(0, new float[] { 7, 8, 9, 10 }, 1, 2);
        assertEquals(8, buffer.get(0), 0.1f);
        assertEquals(9, buffer.get(1), 0.1f);
        assertEquals(5, buffer.get(2), 0.1f);
        assertEquals(6, buffer.get(3), 0.1f);
    }

    @Test
    public void bulkPutWrapper() {
        var byteBuffer = ByteBuffer.allocate(100);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        var buffer = byteBuffer.asFloatBuffer();

        buffer.put(new float[] { 1, 2, 3 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(2, buffer.get(1), 0.1f);
        assertEquals(3, buffer.get(2), 0.1f);

        buffer.put(1, new float[] { 4, 5, 6 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(4, buffer.get(1), 0.1f);
        assertEquals(5, buffer.get(2), 0.1f);
        assertEquals(6, buffer.get(3), 0.1f);
        assertEquals((byte) 0x3f, byteBuffer.get(0));
        assertEquals((byte) 0x80, byteBuffer.get(1));
        assertEquals(0, byteBuffer.get(2));
        assertEquals(0, byteBuffer.get(3));

        buffer.put(0, new float[] { 7, 8, 9, 10 }, 1, 2);
        assertEquals(8, buffer.get(0), 0.1f);
        assertEquals(9, buffer.get(1), 0.1f);
        assertEquals(5, buffer.get(2), 0.1f);
        assertEquals(6, buffer.get(3), 0.1f);

        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer = byteBuffer.asFloatBuffer();

        buffer.put(new float[] { 1, 2, 3 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(2, buffer.get(1), 0.1f);
        assertEquals(3, buffer.get(2), 0.1f);

        buffer.put(1, new float[] { 4, 5, 6 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(4, buffer.get(1), 0.1f);
        assertEquals(5, buffer.get(2), 0.1f);
        assertEquals(6, buffer.get(3), 0.1f);
        assertEquals(0, byteBuffer.get(0));
        assertEquals(0, byteBuffer.get(1));
        assertEquals((byte) 0x80, byteBuffer.get(2));
        assertEquals((byte) 0x3f, byteBuffer.get(3));

        buffer.put(0, new float[] { 7, 8, 9, 10 }, 1, 2);
        assertEquals(8, buffer.get(0), 0.1f);
        assertEquals(9, buffer.get(1), 0.1f);
        assertEquals(5, buffer.get(2), 0.1f);
        assertEquals(6, buffer.get(3), 0.1f);
    }

    @Test
    public void bulkPutBuffer() {
        var buffer = FloatBuffer.allocate(100);
        buffer.put(FloatBuffer.wrap(new float[] { 1, 2, 3 }));

        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(2, buffer.get(1), 0.1f);
        assertEquals(3, buffer.get(2), 0.1f);

        buffer.put(1, FloatBuffer.wrap(new float[] { 4, 5, 6 }), 1, 2);
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(5, buffer.get(1), 0.1f);
        assertEquals(6, buffer.get(2), 0.1f);
    }

    @Test
    public void bulkPutBufferWrapper() {
        var buffer = ByteBuffer.allocate(100).order(ByteOrder.BIG_ENDIAN).asFloatBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] {
                        0x3f, (byte) 0x80, 0x00, 0x00,
                        0x40, 0x00, 0x00, 0x00,
                        0x40, 0x40, 0x00, 0x00
                })
                .order(ByteOrder.BIG_ENDIAN)
                .asFloatBuffer());

        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(2, buffer.get(1), 0.1f);
        assertEquals(3, buffer.get(2), 0.1f);

        buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] {
                        0x3f, (byte) 0x80, 0x00, 0x00,
                        0x40, 0x00, 0x00, 0x00,
                        0x40, 0x40, 0x00, 0x00
                })
                .order(ByteOrder.BIG_ENDIAN)
                .asFloatBuffer());

        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(2, buffer.get(1), 0.1f);
        assertEquals(3, buffer.get(2), 0.1f);

        buffer = ByteBuffer.allocate(100).order(ByteOrder.BIG_ENDIAN).asFloatBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] {
                        0x00, 0x00, (byte) 0x80, 0x3f,
                        0x00, 0x00, 0x00, 0x40,
                        0x00, 0x00, 0x40, 0x40
                })
                .order(ByteOrder.LITTLE_ENDIAN)
                .asFloatBuffer());

        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(2, buffer.get(1), 0.1f);
        assertEquals(3, buffer.get(2), 0.1f);

        buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] {
                        0x00, 0x00, (byte) 0x80, 0x3f,
                        0x00, 0x00, 0x00, 0x40,
                        0x00, 0x00, 0x40, 0x40
                })
                .order(ByteOrder.LITTLE_ENDIAN)
                .asFloatBuffer());

        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(2, buffer.get(1), 0.1f);
        assertEquals(3, buffer.get(2), 0.1f);
    }

    @Test
    public void bulkGet() {
        var buffer = FloatBuffer.wrap(new float[] { 1, 2, 3, 4, 5, 6 });
        var arr = new float[3];

        buffer.get(arr);
        assertArrayEquals(new float[] { 1, 2, 3 }, arr, 0.1f);
        assertEquals(3, buffer.position());

        buffer.get(1, arr);
        assertArrayEquals(new float[] { 2, 3, 4 }, arr, 0.1f);
        assertEquals(3, buffer.position());

        buffer.get(4, arr, 1, 2);
        assertArrayEquals(new float[] { 2, 5, 6 }, arr, 0.1f);
        assertEquals(3, buffer.position());
    }

    @Test
    public void bulkGetWrapper() {
        var buffer = ByteBuffer.wrap(new byte[] {
                        0x00, 0x00, (byte) 0x80, 0x3f,
                        0x00, 0x00, 0x00, 0x40,
                        0x00, 0x00, 0x40, 0x40,
                        0x00, 0x00, (byte) 0x80, 0x40,
                        0x00, 0x00, (byte) 0xA0, 0x40,
                        0x00, 0x00, (byte) 0xC0, 0x40
                })
                .order(ByteOrder.LITTLE_ENDIAN)
                .asFloatBuffer();
        var arr = new float[3];

        buffer.get(arr);
        assertArrayEquals(new float[] { 1, 2, 3 }, arr, 0.1f);
        assertEquals(3, buffer.position());

        buffer.get(1, arr);
        assertArrayEquals(new float[] { 2, 3, 4 }, arr, 0.1f);
        assertEquals(3, buffer.position());

        buffer.get(4, arr, 1, 2);
        assertArrayEquals(new float[] { 2, 5, 6 }, arr, 0.1f);
        assertEquals(3, buffer.position());

        buffer = ByteBuffer.wrap(new byte[] {
                        0x3f, (byte) 0x80, 0x00, 0x00,
                        0x40, 0x00, 0x00, 0x00,
                        0x40, 0x40, 0x00, 0x00,
                        0x40, (byte) 0x80, 0x00, 0x00,
                        0x40, (byte) 0xA0, 0x00, 0x00,
                        0x40, (byte) 0xC0, 0x00, 0x00
                })
                .order(ByteOrder.BIG_ENDIAN)
                .asFloatBuffer();

        buffer.get(arr);
        assertArrayEquals(new float[] { 1, 2, 3 }, arr, 0.1f);
        assertEquals(3, buffer.position());

        buffer.get(1, arr);
        assertArrayEquals(new float[] { 2, 3, 4 }, arr, 0.1f);
        assertEquals(3, buffer.position());

        buffer.get(4, arr, 1, 2);
        assertArrayEquals(new float[] { 2, 5, 6 }, arr, 0.1f);
        assertEquals(3, buffer.position());
    }

    @Test
    public void putsBufferViewToView() {
        var dst = ByteBuffer.allocate(32).asFloatBuffer();
        dst.put(1);
        var src = ByteBuffer.allocate(16).asFloatBuffer();
        src.put(new float[] { 2, 3 });
        src.flip();
        dst.put(src);
        dst.flip();

        var check = new float[3];
        dst.get(0, check);
        assertArrayEquals(new float[] { 1, 2, 3 }, check, 0.1f);
    }
}
