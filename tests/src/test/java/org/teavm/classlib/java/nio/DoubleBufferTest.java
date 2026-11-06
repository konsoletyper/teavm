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
import java.nio.DoubleBuffer;
import java.nio.InvalidMarkException;
import java.nio.ReadOnlyBufferException;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class DoubleBufferTest {
    @Test
    public void allocatesSimple() {
        DoubleBuffer buffer = DoubleBuffer.allocate(100);
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
        var buffer = ByteBuffer.allocateDirect(40).asDoubleBuffer();
        var doubles = new double[] { 1, 2, 3 };
        buffer.put(0, doubles);
        var dobulesCopy = new double[doubles.length];
        buffer.get(0, dobulesCopy);
        assertArrayEquals(doubles, dobulesCopy, 0.1);
    }

    @Test
    public void bulkTransferRelative() {
        var arr = new double[5];
        var buffer = DoubleBuffer.wrap(arr);
        var src = DoubleBuffer.wrap(new double[] { 1.0, 2.0, 3.0 });
        buffer.put(src);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 0.0, 0.0 }, arr, 0.0);
        assertEquals(3, buffer.position());
        assertEquals(3, src.position());

        assertThrows(BufferOverflowException.class, () -> buffer.put(
                DoubleBuffer.wrap(new double[] { 4.0, 5.0, 6.0 })));
        assertThrows(ReadOnlyBufferException.class, () -> buffer.rewind().asReadOnlyBuffer()
                .put(DoubleBuffer.wrap(new double[] { 4.0, 5.0, 6.0 })));
    }


    @Test
    public void errorIfAllocatingBufferOfNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> DoubleBuffer.allocate(-1));
    }

    @Test
    public void wrapsArray() {
        double[] array = new double[100];
        DoubleBuffer buffer = DoubleBuffer.wrap(array, 10, 70);
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
        assertEquals((double) 23, buffer.get(0));
        buffer.put(1, 24);
        assertEquals((double) 24, array[1]);
    }

    @Test
    public void errorWhenWrappingWithWrongParameters() {
        double[] array = new double[100];
        try {
            DoubleBuffer.wrap(array, -1, 10);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            DoubleBuffer.wrap(array, 101, 10);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            DoubleBuffer.wrap(array, 98, 3);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            DoubleBuffer.wrap(array, 98, -1);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
    }

    @Test
    public void wrapsArrayWithoutOffset() {
        double[] array = new double[100];
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
        assertEquals(0, buffer.position());
        assertEquals(100, buffer.limit());
    }

    @Test
    public void createsSlice() {
        DoubleBuffer buffer = DoubleBuffer.allocate(100);
        buffer.put(new double[60]);
        buffer.flip();
        buffer.put(new double[15]);
        DoubleBuffer slice = buffer.slice();
        assertArrayEquals(buffer.array(), slice.array());
        assertEquals(0, slice.position());
        assertEquals(45, slice.capacity());
        assertEquals(45, slice.limit());
        assertFalse(slice.isDirect());
        assertFalse(slice.isReadOnly());
        slice.put(3, 23);
        assertEquals((double) 23, buffer.get(18));
        slice.put(24);
        assertEquals((double) 24, buffer.get(15));
        buffer.put(16, 25);
        assertEquals((double) 25, slice.get(1));
    }

    @Test
    public void slicePropertiesSameWithOriginal() {
        DoubleBuffer buffer = DoubleBuffer.allocate(100).asReadOnlyBuffer().slice();
        assertTrue(buffer.isReadOnly());
    }

    @Test
    public void createsDuplicate() {
        DoubleBuffer buffer = DoubleBuffer.allocate(100);
        buffer.put(new double[60]);
        buffer.flip();
        buffer.put(new double[15]);
        DoubleBuffer duplicate = buffer.duplicate();
        assertArrayEquals(buffer.array(), duplicate.array());
        assertEquals(15, duplicate.position());
        assertEquals(100, duplicate.capacity());
        assertEquals(60, duplicate.limit());
        assertFalse(duplicate.isDirect());
        assertFalse(duplicate.isReadOnly());
        duplicate.put(3, 23);
        assertEquals((double) 23, buffer.get(3));
        duplicate.put(24);
        assertEquals((double) 24, buffer.get(15));
        buffer.put(1, 25);
        assertEquals((double) 25, duplicate.get(1));
        assertSame(buffer.array(), duplicate.array());
    }

    @Test
    public void getsDouble() {
        double[] array = { 2, 3, 5, 7 };
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
        assertEquals((double) 2, buffer.get());
        assertEquals((double) 3, buffer.get());
        buffer = buffer.slice();
        assertEquals((double) 5, buffer.get());
        assertEquals((double) 7, buffer.get());
    }

    @Test
    public void gettingDoubleFromEmptyBufferCausesError() {
        double[] array = { 2, 3, 5, 7 };
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
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
    public void putsDouble() {
        double[] array = new double[4];
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
        buffer.put(2).put(3).put(5).put(7);
        assertArrayEquals(new double[] { 2, 3, 5, 7 }, array);
    }

    @Test
    public void puttingDoubleToEmptyBufferCausesError() {
        double[] array = new double[4];
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
        buffer.limit(2);
        buffer.put(2).put(3);
        try {
            buffer.put(5);
            fail("Should have thrown error");
        } catch (BufferOverflowException e) {
            assertEquals((double) 0, array[2]);
        }
    }

    @Test
    public void puttingDoubleToReadOnlyBufferCausesError() {
        double[] array = new double[4];
        DoubleBuffer buffer = DoubleBuffer.wrap(array).asReadOnlyBuffer();
        assertThrows(ReadOnlyBufferException.class, () -> buffer.put(2));
    }

    @Test
    public void getsDoubleFromGivenLocation() {
        double[] array = { 2, 3, 5, 7 };
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
        assertEquals((double) 2, buffer.get(0));
        assertEquals((double) 3, buffer.get(1));
        buffer.get();
        buffer = buffer.slice();
        assertEquals((double) 5, buffer.get(1));
        assertEquals((double) 7, buffer.get(2));
    }

    @Test
    public void gettingDoubleFromWrongLocationCausesError() {
        double[] array = { 2, 3, 5, 7 };
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
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
    public void putsDoubleToGivenLocation() {
        double[] array = new double[4];
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
        buffer.put(0, 2);
        buffer.put(1, 3);
        buffer.get();
        buffer = buffer.slice();
        buffer.put(1, 5);
        buffer.put(2, 7);
        assertArrayEquals(new double[] { 2, 3, 5, 7 }, array);
    }

    @Test
    public void puttingDoubleToWrongLocationCausesError() {
        double[] array = new double[4];
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
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
    public void puttingDoubleToGivenLocationOfReadOnlyBufferCausesError() {
        double[] array = new double[4];
        DoubleBuffer buffer = DoubleBuffer.wrap(array).asReadOnlyBuffer();
        assertThrows(ReadOnlyBufferException.class, () -> buffer.put(0, 2));
    }

    @Test
    public void getsDoubles() {
        double[] array = { 2, 3, 5, 7 };
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
        buffer.get();
        double[] receiver = new double[2];
        buffer.get(receiver, 0, 2);
        assertEquals(3, buffer.position());
        assertArrayEquals(new double[] { 3, 5 }, receiver);
    }

    @Test
    public void gettingDoublesFromEmptyBufferCausesError() {
        double[] array = { 2, 3, 5, 7 };
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
        buffer.limit(3);
        double[] receiver = new double[4];
        try {
            buffer.get(receiver, 0, 4);
            fail("Error expected");
        } catch (BufferUnderflowException e) {
            assertArrayEquals(new double[4], receiver);
            assertEquals(0, buffer.position());
        }
    }

    @Test
    public void gettingDoublesWithIllegalArgumentsCausesError() {
        double[] array = { 2, 3, 5, 7 };
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
        double[] receiver = new double[4];
        try {
            buffer.get(receiver, 0, 5);
        } catch (IndexOutOfBoundsException e) {
            assertArrayEquals(new double[4], receiver);
            assertEquals(0, buffer.position());
        }
        try {
            buffer.get(receiver, -1, 3);
        } catch (IndexOutOfBoundsException e) {
            assertArrayEquals(new double[4], receiver);
            assertEquals(0, buffer.position());
        }
        try {
            buffer.get(receiver, 6, 3);
        } catch (IndexOutOfBoundsException e) {
            assertArrayEquals(new double[4], receiver);
            assertEquals(0, buffer.position());
        }
    }

    @Test
    public void putsDoubles() {
        double[] array = new double[4];
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
        buffer.get();
        double[] data = { 2, 3 };
        buffer.put(data, 0, 2);
        assertEquals(3, buffer.position());
        assertArrayEquals(new double[] { 0, 2, 3, 0 }, array);
    }

    @Test
    public void compacts() {
        double[] array = { 2, 3, 5, 7 };
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
        buffer.get();
        buffer.mark();
        buffer.compact();
        assertArrayEquals(new double[] { 3, 5, 7, 7 }, array);
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
        double[] array = { 2, 3, 5, 7 };
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
        buffer.position(1);
        buffer.mark();
        buffer.position(2);
        buffer.reset();
        assertEquals(1, buffer.position());
    }

    @Test
    public void putEmptyArray() {
        DoubleBuffer db = DoubleBuffer.allocate(0);
        db.put(new double[0]);
        db.get(new double[0]);
    }
    @Test
    public void bulkPut() {
        var buffer = DoubleBuffer.allocate(100);
        buffer.put(new double[] { 1, 2, 3 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(2, buffer.get(1), 0.1f);
        assertEquals(3, buffer.get(2), 0.1f);

        buffer.put(1, new double[] { 4, 5, 6 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(4, buffer.get(1), 0.1f);
        assertEquals(5, buffer.get(2), 0.1f);
        assertEquals(6, buffer.get(3), 0.1f);

        buffer.put(0, new double[] { 7, 8, 9, 10 }, 1, 2);
        assertEquals(8, buffer.get(0), 0.1f);
        assertEquals(9, buffer.get(1), 0.1f);
        assertEquals(5, buffer.get(2), 0.1f);
        assertEquals(6, buffer.get(3), 0.1f);
    }

    @Test
    public void bulkPutWrapper() {
        var byteBuffer = ByteBuffer.allocate(100);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        var buffer = byteBuffer.asDoubleBuffer();

        buffer.put(new double[] { 1, 2, 3 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(2, buffer.get(1), 0.1f);
        assertEquals(3, buffer.get(2), 0.1f);

        buffer.put(1, new double[] { 4, 5, 6 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(4, buffer.get(1), 0.1f);
        assertEquals(5, buffer.get(2), 0.1f);
        assertEquals(6, buffer.get(3), 0.1f);
        assertEquals((byte) 0x3f, byteBuffer.get(0));
        assertEquals((byte) 0xf0, byteBuffer.get(1));
        assertEquals(0, byteBuffer.get(7));

        buffer.put(0, new double[] { 7, 8, 9, 10 }, 1, 2);
        assertEquals(8, buffer.get(0), 0.1f);
        assertEquals(9, buffer.get(1), 0.1f);
        assertEquals(5, buffer.get(2), 0.1f);
        assertEquals(6, buffer.get(3), 0.1f);

        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer = byteBuffer.asDoubleBuffer();

        buffer.put(new double[] { 1, 2, 3 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(2, buffer.get(1), 0.1f);
        assertEquals(3, buffer.get(2), 0.1f);

        buffer.put(1, new double[] { 4, 5, 6 });
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(4, buffer.get(1), 0.1f);
        assertEquals(5, buffer.get(2), 0.1f);
        assertEquals(6, buffer.get(3), 0.1f);
        assertEquals(0, byteBuffer.get(0));
        assertEquals((byte) 0xf0, byteBuffer.get(6));
        assertEquals((byte) 0x3f, byteBuffer.get(7));

        buffer.put(0, new double[] { 7, 8, 9, 10 }, 1, 2);
        assertEquals(8, buffer.get(0), 0.1f);
        assertEquals(9, buffer.get(1), 0.1f);
        assertEquals(5, buffer.get(2), 0.1f);
        assertEquals(6, buffer.get(3), 0.1f);
    }

    @Test
    public void bulkPutBuffer() {
        var buffer = DoubleBuffer.allocate(100);
        buffer.put(DoubleBuffer.wrap(new double[] { 1, 2, 3 }));

        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(2, buffer.get(1), 0.1f);
        assertEquals(3, buffer.get(2), 0.1f);

        buffer.put(1, DoubleBuffer.wrap(new double[] { 4, 5, 6 }), 1, 2);
        assertEquals(3, buffer.position());
        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(5, buffer.get(1), 0.1f);
        assertEquals(6, buffer.get(2), 0.1f);
    }

    @Test
    public void bulkPutBufferWrapper() {
        var buffer = ByteBuffer.allocate(100).order(ByteOrder.BIG_ENDIAN).asDoubleBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] {
                        0x3f, (byte) 0xf0, 0, 0, 0, 0, 0, 0,
                        0x40, 0x00, 0, 0, 0, 0, 0, 0,
                        0x40, 0x08, 0, 0, 0, 0, 0, 0
                })
                .order(ByteOrder.BIG_ENDIAN)
                .asDoubleBuffer());

        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(2, buffer.get(1), 0.1f);
        assertEquals(3, buffer.get(2), 0.1f);

        buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] {
                        0x3f, (byte) 0xf0, 0, 0, 0, 0, 0, 0,
                        0x40, 0x00, 0, 0, 0, 0, 0, 0,
                        0x40, 0x08, 0, 0, 0, 0, 0, 0
                })
                .order(ByteOrder.BIG_ENDIAN)
                .asDoubleBuffer());

        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(2, buffer.get(1), 0.1f);
        assertEquals(3, buffer.get(2), 0.1f);

        buffer = ByteBuffer.allocate(100).order(ByteOrder.BIG_ENDIAN).asDoubleBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] {
                        0, 0, 0, 0, 0, 0, (byte) 0xf0, 0x3f,
                        0, 0, 0, 0, 0, 0, 0x00, 0x40,
                        0, 0, 0, 0, 0, 0, 0x08, 0x40,
                })
                .order(ByteOrder.LITTLE_ENDIAN)
                .asDoubleBuffer());

        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(2, buffer.get(1), 0.1f);
        assertEquals(3, buffer.get(2), 0.1f);

        buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] {
                        0, 0, 0, 0, 0, 0, (byte) 0xf0, 0x3f,
                        0, 0, 0, 0, 0, 0, 0x00, 0x40,
                        0, 0, 0, 0, 0, 0, 0x08, 0x40,
                })
                .order(ByteOrder.LITTLE_ENDIAN)
                .asDoubleBuffer());

        assertEquals(1, buffer.get(0), 0.1f);
        assertEquals(2, buffer.get(1), 0.1f);
        assertEquals(3, buffer.get(2), 0.1f);
    }

    @Test
    public void bulkGet() {
        var buffer = DoubleBuffer.wrap(new double[] { 1, 2, 3, 4, 5, 6 });
        var arr = new double[3];

        buffer.get(arr);
        assertArrayEquals(new double[] { 1, 2, 3 }, arr, 0.1f);
        assertEquals(3, buffer.position());

        buffer.get(1, arr);
        assertArrayEquals(new double[] { 2, 3, 4 }, arr, 0.1f);
        assertEquals(3, buffer.position());

        buffer.get(4, arr, 1, 2);
        assertArrayEquals(new double[] { 2, 5, 6 }, arr, 0.1f);
        assertEquals(3, buffer.position());
    }

    @Test
    public void bulkGetWrapper() {
        var buffer = ByteBuffer.wrap(new byte[] {
                        0, 0, 0, 0, 0, 0, (byte) 0xf0, 0x3f,
                        0, 0, 0, 0, 0, 0, 0x00, 0x40,
                        0, 0, 0, 0, 0, 0, 0x08, 0x40,
                        0, 0, 0, 0, 0, 0, 0x10, 0x40,
                        0, 0, 0, 0, 0, 0, 0x14, 0x40,
                        0, 0, 0, 0, 0, 0, 0x18, 0x40
                })
                .order(ByteOrder.LITTLE_ENDIAN)
                .asDoubleBuffer();
        var arr = new double[3];

        buffer.get(arr);
        assertArrayEquals(new double[] { 1, 2, 3 }, arr, 0.1f);
        assertEquals(3, buffer.position());

        buffer.get(1, arr);
        assertArrayEquals(new double[] { 2, 3, 4 }, arr, 0.1f);
        assertEquals(3, buffer.position());

        buffer.get(4, arr, 1, 2);
        assertArrayEquals(new double[] { 2, 5, 6 }, arr, 0.1f);
        assertEquals(3, buffer.position());

        buffer = ByteBuffer.wrap(new byte[] {
                        0x3f, (byte) 0xf0, 0, 0, 0, 0, 0, 0,
                        0x40, 0x00, 0, 0, 0, 0, 0, 0,
                        0x40, 0x08, 0, 0, 0, 0, 0, 0,
                        0x40, 0x10, 0, 0, 0, 0, 0, 0,
                        0x40, 0x14, 0, 0, 0, 0, 0, 0,
                        0x40, 0x18, 0, 0, 0, 0, 0, 0
                })
                .order(ByteOrder.BIG_ENDIAN)
                .asDoubleBuffer();

        buffer.get(arr);
        assertArrayEquals(new double[] { 1, 2, 3 }, arr, 0.1f);
        assertEquals(3, buffer.position());

        buffer.get(1, arr);
        assertArrayEquals(new double[] { 2, 3, 4 }, arr, 0.1f);
        assertEquals(3, buffer.position());

        buffer.get(4, arr, 1, 2);
        assertArrayEquals(new double[] { 2, 5, 6 }, arr, 0.1f);
        assertEquals(3, buffer.position());
    }
    
    @Test
    public void putsBufferViewToView() {
        var dst = ByteBuffer.allocate(32).asDoubleBuffer();
        dst.put(1);
        var src = ByteBuffer.allocate(16).asDoubleBuffer();
        src.put(new double[] { 2, 3 });
        src.flip();
        dst.put(src);
        dst.flip();

        var check = new double[3];
        dst.get(0, check);
        assertArrayEquals(new double[] { 1, 2, 3 }, check, 0.1);
    }
}
