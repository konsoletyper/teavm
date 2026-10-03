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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.lang.ref.WeakReference;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.InvalidMarkException;
import java.nio.ReadOnlyBufferException;
import org.junit.jupiter.api.Test;
import org.teavm.classlib.java.lang.DoubleTest;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.TeaVMTest;
import org.teavm.junit.TestPlatform;

@TeaVMTest
public class ByteBufferTest {
    @Test
    public void allocatesDirect() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(100);
        assertTrue(buffer.isDirect());
        assertFalse(buffer.isReadOnly());
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
        var buffer = ByteBuffer.allocateDirect(10);
        var bytes = new byte[] { 1, 2, 3 };
        buffer.put(0, bytes);
        var bytesCopy = new byte[bytes.length];
        buffer.get(0, bytesCopy);
        assertArrayEquals(bytes, bytesCopy);
    }
    
    @Test
    public void bulkTransferRelative() {
        var arr = new byte[5];
        var buffer = ByteBuffer.wrap(arr);
        var src = ByteBuffer.wrap(new byte[] { 1, 2, 3 });
        buffer.put(src);
        assertArrayEquals(new byte[] { 1, 2, 3, 0, 0 }, arr);
        assertEquals(3, buffer.position());
        assertEquals(3, src.position());
        
        assertThrows(BufferOverflowException.class, () -> buffer.put(ByteBuffer.wrap(new byte[] { 4, 5, 6 })));
        assertThrows(ReadOnlyBufferException.class, () -> buffer.rewind().asReadOnlyBuffer()
                .put(ByteBuffer.wrap(new byte[] { 4, 5, 6 })));
    }

    @Test
    public void errorIfAllocatingDirectOfNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> ByteBuffer.allocateDirect(-2));
    }

    @Test
    public void allocatesSimple() {
        ByteBuffer buffer = ByteBuffer.allocate(100);
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
    public void errorIfAllocatingBufferOfNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> ByteBuffer.allocate(-1));
    }

    @Test
    public void wrapsArray() {
        byte[] array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array, 10, 70);
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
        assertEquals((byte) 23, buffer.get(0));
        buffer.put(1, (byte) 24);
        assertEquals((byte) 24, array[1]);
    }

    @Test
    public void errorWhenWrappingWithWrongParameters() {
        byte[] array = new byte[100];
        try {
            ByteBuffer.wrap(array, -1, 10);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            ByteBuffer.wrap(array, 101, 10);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            ByteBuffer.wrap(array, 98, 3);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            ByteBuffer.wrap(array, 98, -1);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
    }

    @Test
    public void wrapsArrayWithoutOffset() {
        byte[] array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        assertEquals(0, buffer.position());
        assertEquals(100, buffer.limit());
    }

    @Test
    public void createsSlice() {
        ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.put(new byte[60]);
        buffer.flip();
        buffer.put(new byte[15]);
        ByteBuffer slice = buffer.slice();
        assertArrayEquals(buffer.array(), slice.array());
        assertEquals(0, slice.position());
        assertEquals(45, slice.capacity());
        assertEquals(45, slice.limit());
        assertFalse(slice.isDirect());
        assertFalse(slice.isReadOnly());
        slice.put(3, (byte) 23);
        assertEquals((byte) 23, buffer.get(18));
        slice.put((byte) 24);
        assertEquals((byte) 24, buffer.get(15));
        buffer.put(16, (byte) 25);
        assertEquals((byte) 25, slice.get(1));
    }

    @Test
    public void sliceOfSlice() {
        ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.put(new byte[10]);
        ByteBuffer slice1 = buffer.slice();
        slice1.put(new byte[15]);
        ByteBuffer slice2 = slice1.slice();

        assertEquals(25, slice2.arrayOffset());
        assertEquals(75, slice2.capacity());
    }

    @Test
    public void slicePropertiesSameWithOriginal() {
        ByteBuffer buffer = ByteBuffer.allocate(100).asReadOnlyBuffer().slice();
        assertTrue(buffer.isReadOnly());
        buffer = ByteBuffer.allocateDirect(100);
        assertTrue(buffer.isDirect());
    }

    @Test
    public void createsDuplicate() {
        ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.put(new byte[60]);
        buffer.flip();
        buffer.put(new byte[15]);
        ByteBuffer duplicate = buffer.duplicate();
        assertArrayEquals(buffer.array(), duplicate.array());
        assertEquals(15, duplicate.position());
        assertEquals(100, duplicate.capacity());
        assertEquals(60, duplicate.limit());
        assertFalse(duplicate.isDirect());
        assertFalse(duplicate.isReadOnly());
        duplicate.put(3, (byte) 23);
        assertEquals((byte) 23, buffer.get(3));
        duplicate.put((byte) 24);
        assertEquals((byte) 24, buffer.get(15));
        buffer.put(1, (byte) 25);
        assertEquals((byte) 25, duplicate.get(1));
        assertSame(buffer.array(), duplicate.array());
    }

    @Test
    public void getsByte() {
        byte[] array = {2, 3, 5, 7};
        ByteBuffer buffer = ByteBuffer.wrap(array);
        assertEquals((byte) 2, buffer.get());
        assertEquals((byte) 3, buffer.get());
        buffer = buffer.slice();
        assertEquals((byte) 5, buffer.get());
        assertEquals((byte) 7, buffer.get());
    }

    @Test
    public void gettingByteFromEmptyBufferCausesError() {
        byte[] array = {2, 3, 5, 7};
        ByteBuffer buffer = ByteBuffer.wrap(array);
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
    public void putsByte() {
        byte[] array = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.put((byte) 2).put((byte) 3).put((byte) 5).put((byte) 7);
        assertArrayEquals(new byte[]{2, 3, 5, 7}, array);
    }

    @Test
    public void puttingByteToEmptyBufferCausesError() {
        byte[] array = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.limit(2);
        buffer.put((byte) 2).put((byte) 3);
        try {
            buffer.put((byte) 5);
            fail("Should have thrown error");
        } catch (BufferOverflowException e) {
            assertEquals((byte) 0, array[2]);
        }
    }

    @Test
    public void puttingByteToReadOnlyBufferCausesError() {
        byte[] array = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(array).asReadOnlyBuffer();
        assertThrows(ReadOnlyBufferException.class, () -> buffer.put((byte) 2));
    }

    @Test
    public void getsByteFromGivenLocation() {
        byte[] array = {2, 3, 5, 7};
        ByteBuffer buffer = ByteBuffer.wrap(array);
        assertEquals((byte) 2, buffer.get(0));
        assertEquals((byte) 3, buffer.get(1));
        buffer.get();
        buffer = buffer.slice();
        assertEquals((byte) 5, buffer.get(1));
        assertEquals((byte) 7, buffer.get(2));
    }

    @Test
    public void gettingByteFromWrongLocationCausesError() {
        byte[] array = {2, 3, 5, 7};
        ByteBuffer buffer = ByteBuffer.wrap(array);
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
    public void putsByteToGivenLocation() {
        byte[] array = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.put(0, (byte) 2);
        buffer.put(1, (byte) 3);
        buffer.get();
        buffer = buffer.slice();
        buffer.put(1, (byte) 5);
        buffer.put(2, (byte) 7);
        assertArrayEquals(new byte[]{2, 3, 5, 7}, array);
    }

    @Test
    public void puttingByteToWrongLocationCausesError() {
        byte[] array = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.limit(3);
        try {
            buffer.put(-1, (byte) 2);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            buffer.put(3, (byte) 2);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
    }

    @Test
    public void puttingByteToGivenLocationOfReadOnlyBufferCausesError() {
        byte[] array = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(array).asReadOnlyBuffer();
        assertThrows(ReadOnlyBufferException.class, () -> buffer.put(0, (byte) 2));
    }

    @Test
    public void getsBytes() {
        byte[] array = {2, 3, 5, 7};
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.get();
        byte[] receiver = new byte[2];
        buffer.get(receiver, 0, 2);
        assertEquals(3, buffer.position());
        assertArrayEquals(new byte[]{3, 5}, receiver);
    }

    @Test
    public void gettingBytesFromEmptyBufferCausesError() {
        byte[] array = {2, 3, 5, 7};
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.limit(3);
        byte[] receiver = new byte[4];
        try {
            buffer.get(receiver, 0, 4);
            fail("Error expected");
        } catch (BufferUnderflowException e) {
            assertArrayEquals(new byte[4], receiver);
            assertEquals(0, buffer.position());
        }
    }

    @Test
    public void gettingBytesWithIllegalArgumentsCausesError() {
        byte[] array = {2, 3, 5, 7};
        ByteBuffer buffer = ByteBuffer.wrap(array);
        byte[] receiver = new byte[4];
        try {
            buffer.get(receiver, 0, 5);
            fail("Error expected");
        } catch (IndexOutOfBoundsException e) {
            assertArrayEquals(new byte[4], receiver);
            assertEquals(0, buffer.position());
        }
        try {
            buffer.get(receiver, -1, 3);
            fail("Error expected");
        } catch (IndexOutOfBoundsException e) {
            assertArrayEquals(new byte[4], receiver);
            assertEquals(0, buffer.position());
        }
        try {
            buffer.get(receiver, 6, 3);
            fail("Error expected");
        } catch (IndexOutOfBoundsException e) {
            assertArrayEquals(new byte[4], receiver);
            assertEquals(0, buffer.position());
        }
    }

    @Test
    public void putsBytes() {
        byte[] array = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.get();
        byte[] data = {2, 3};
        buffer.put(data, 0, 2);
        assertEquals(3, buffer.position());
        assertArrayEquals(new byte[]{0, 2, 3, 0}, array);
    }

    @Test
    public void putsBytesWithZeroLengthArray() {
        byte[] array = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.get();
        byte[] data = {};
        buffer.put(data, 0, 0);
        assertEquals(1, buffer.position());
        assertArrayEquals(new byte[]{0, 0, 0, 0}, array);
    }

    @Test
    public void putsOtherBuffer() {
        var dst = ByteBuffer.allocate(16);
        dst.putShort((short) 0x1234);
        var src = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4 });
        dst.put(src);
        dst.flip();

        var check = new byte[6];
        dst.get(0, check);
        assertArrayEquals(new byte[] { 0x12, 0x34, 1, 2, 3, 4 }, check);
    }

    @Test
    public void compacts() {
        byte[] array = {2, 3, 5, 7};
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.get();
        buffer.mark();
        buffer.compact();
        assertArrayEquals(new byte[]{3, 5, 7, 7}, array);
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
        byte[] array = {2, 3, 5, 7};
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.position(1);
        buffer.mark();
        buffer.position(2);
        buffer.reset();
        assertEquals(1, buffer.position());
    }

    @Test
    public void getsChar() {
        byte[] array = {0, 'A', 0, 'B'};
        ByteBuffer buffer = ByteBuffer.wrap(array);
        assertEquals('A', buffer.getChar());
        assertEquals('B', buffer.getChar());
        try {
            buffer.getChar();
            fail("Exception expected");
        } catch (BufferUnderflowException e) {
            // expected
        }
        buffer.position(3);
        try {
            buffer.getChar();
            fail("Exception expected");
        } catch (BufferUnderflowException e) {
            // expected
        }
        assertEquals('A', buffer.getChar(0));
        assertEquals('B', buffer.getChar(2));
        try {
            buffer.getChar(3);
            fail("Exception expected");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void putsChar() {
        byte[] array = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.putChar('A');
        buffer.putChar('B');
        try {
            buffer.putChar('C');
            fail("Exception expected");
        } catch (BufferOverflowException e) {
            // expected
        }
        buffer.position(3);
        try {
            buffer.putChar('D');
            fail("Exception expected");
        } catch (BufferOverflowException e) {
            // expected
        }
        assertEquals((byte) 0, buffer.get(0));
        assertEquals((byte) 'A', buffer.get(1));
        assertEquals((byte) 0, buffer.get(2));
        assertEquals((byte) 'B', buffer.get(3));
        buffer.putChar(0, 'E');
        assertEquals((byte) 'E', buffer.get(1));
        try {
            buffer.putChar(3, 'F');
            fail("Exception expected");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void getsShort() {
        byte[] array = {0x23, 0x24, 0x25, 0x26};
        ByteBuffer buffer = ByteBuffer.wrap(array);
        assertEquals((short) 0x2324, buffer.getShort());
        assertEquals((short) 0x2526, buffer.getShort());
        try {
            buffer.getShort();
            fail("Exception expected");
        } catch (BufferUnderflowException e) {
            // expected
        }
        buffer.position(3);
        try {
            buffer.getShort();
            fail("Exception expected");
        } catch (BufferUnderflowException e) {
            // expected
        }
        assertEquals((short) 0x2324, buffer.getShort(0));
        assertEquals((short) 0x2526, buffer.getShort(2));
        try {
            buffer.getShort(3);
            fail("Exception expected");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void putsShort() {
        byte[] array = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.putShort((short) 0x2324);
        buffer.putShort((short) 0x2526);
        try {
            buffer.putShort((short) 0x2728);
            fail("Exception expected");
        } catch (BufferOverflowException e) {
            // expected
        }
        buffer.position(3);
        try {
            buffer.putShort((short) 0x292A);
            fail("Exception expected");
        } catch (BufferOverflowException e) {
            // expected
        }
        assertEquals((byte) 0x23, buffer.get(0));
        assertEquals((byte) 0x24, buffer.get(1));
        assertEquals((byte) 0x25, buffer.get(2));
        assertEquals((byte) 0x26, buffer.get(3));
        buffer.putShort(0, (short) 0x2B2C);
        assertEquals((byte) 0x2C, buffer.get(1));
        try {
            buffer.putShort(3, (short) 0x2D2E);
            fail("Exception expected");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void getsInt() {
        byte[] array = {0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x30};
        ByteBuffer buffer = ByteBuffer.wrap(array);
        assertEquals(0x23242526, buffer.getInt());
        assertEquals(0x27282930, buffer.getInt());
        try {
            buffer.getInt();
            fail("Exception expected");
        } catch (BufferUnderflowException e) {
            // expected
        }
        buffer.position(7);
        try {
            buffer.getInt();
            fail("Exception expected");
        } catch (BufferUnderflowException e) {
            // expected
        }
        assertEquals(0x23242526, buffer.getInt(0));
        assertEquals(0x27282930, buffer.getInt(4));
        try {
            buffer.getInt(7);
            fail("Exception expected");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void putsFloat() {
        var array = new byte[8];
        var buffer = ByteBuffer.wrap(array);
        buffer.putFloat(1f);
        buffer.putFloat(23f);
        try {
            buffer.putFloat(42f);
            fail("Exception expected");
        } catch (BufferOverflowException e) {
            // expected
        }

        assertArrayEquals(new byte[] { 63, -128, 0, 0, 65, -72, 0, 0 }, array);

        buffer.putFloat(1, 2f);
        assertArrayEquals(new byte[] { 63, 64, 0, 0, 0, -72, 0, 0 }, array);
    }

    @Test
    public void getsFloat() {
        byte[] array = { 63, -128, 0, 0, 65, -72, 0, 0 };
        var buffer = ByteBuffer.wrap(array);
        assertEquals(1f, buffer.getFloat(), 0.0001f);
        assertEquals(23f, buffer.getFloat(), 0.0001f);
        try {
            buffer.getFloat();
            fail("Exception expected");
        } catch (BufferUnderflowException e) {
            // expected
        }

        array[1] = 64;
        array[4] = 0;
        assertEquals(2f, buffer.getFloat(1), 0.0001f);
    }

    @Test
    public void putsDouble() {
        var array = new byte[16];
        var buffer = ByteBuffer.wrap(array);
        buffer.putDouble(1.0);
        buffer.putDouble(23.0);
        try {
            buffer.putDouble(42.0);
            fail("Exception expected");
        } catch (BufferOverflowException e) {
            // expected
        }

        assertArrayEquals(new byte[] { 63, -16, 0, 0, 0, 0, 0, 0, 64, 55, 0, 0, 0, 0, 0, 0 }, array);

        buffer.putDouble(1, 2.0);
        assertArrayEquals(new byte[] { 63, 64, 0, 0, 0, 0, 0, 0, 0, 55, 0, 0, 0, 0, 0, 0 }, array);
    }

    @Test
    public void putsDoubleNaN() {
        var array = new byte[8];
        var buffer = ByteBuffer.wrap(array);

        buffer.putDouble(0, DoubleTest.OTHER_NAN);
        assertArrayEquals(new byte[] { 127, -8, 0, 0, 0, 0, 0, 1 }, array);
    }

    @Test
    public void getsDouble() {
        byte[] array = { 63, -16, 0, 0, 0, 0, 0, 0, 64, 55, 0, 0, 0, 0, 0, 0 };
        var buffer = ByteBuffer.wrap(array);
        assertEquals(1.0, buffer.getDouble(), 0.0001);
        assertEquals(23.0, buffer.getDouble(), 0.0001);
        try {
            buffer.getDouble();
            fail("Exception expected");
        } catch (BufferUnderflowException e) {
            // expected
        }

        array[1] = 64;
        array[8] = 0;
        assertEquals(2.0, buffer.getDouble(1), 0.0001);
    }

    @Test
    public void getsLong() {
        byte[] array = {0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38};
        ByteBuffer buffer = ByteBuffer.wrap(array);
        assertEquals(0x2324252627282930L, buffer.getLong());
        assertEquals(0x3132333435363738L, buffer.getLong());
        try {
            buffer.getLong();
            fail("Exception expected");
        } catch (BufferUnderflowException e) {
            // expected
        }
        buffer.position(15);
        try {
            buffer.getLong();
            fail("Exception expected");
        } catch (BufferUnderflowException e) {
            // expected
        }
        assertEquals(0x2324252627282930L, buffer.getLong(0));
        assertEquals(0x3132333435363738L, buffer.getLong(8));
        try {
            buffer.getLong(16);
            fail("Exception expected");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void putsLong() {
        byte[] array = new byte[16];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.putLong(0x2324252627282930L);
        buffer.putLong(0x3132333435363738L);
        try {
            buffer.putLong(0L);
            fail("Exception expected");
        } catch (BufferOverflowException e) {
            // expected
        }
        buffer.position(15);
        try {
            buffer.putLong(0L);
            fail("Exception expected");
        } catch (BufferOverflowException e) {
            // expected
        }
        assertEquals((byte) 0x23, buffer.get(0));
        assertEquals((byte) 0x24, buffer.get(1));
        assertEquals((byte) 0x25, buffer.get(2));
        assertEquals((byte) 0x26, buffer.get(3));
        assertEquals((byte) 0x27, buffer.get(4));
        assertEquals((byte) 0x28, buffer.get(5));
        assertEquals((byte) 0x29, buffer.get(6));
        assertEquals((byte) 0x30, buffer.get(7));
        buffer.putLong(0, 0xAABBCCDDEEFF0000L);
        assertEquals((byte) 0xBB, buffer.get(1));
        try {
            buffer.putLong(15, 0x0L);
            fail("Exception expected");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        buffer = ByteBuffer.wrap(array).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(1, 0x2324252627282930L);
        assertEquals((byte) 0x30, buffer.get(1));
        assertEquals((byte) 0x29, buffer.get(2));
        assertEquals((byte) 0x28, buffer.get(3));
        assertEquals((byte) 0x27, buffer.get(4));
        assertEquals((byte) 0x26, buffer.get(5));
        assertEquals((byte) 0x25, buffer.get(6));
        assertEquals((byte) 0x24, buffer.get(7));
        assertEquals((byte) 0x23, buffer.get(8));
    }

    @Test
    public void putGetEmptyArray() {
        ByteBuffer bb = ByteBuffer.allocate(0);
        bb.put(new byte[0]);
        bb.get(new byte[0]);
    }

    @Test
    @OnlyPlatform(TestPlatform.C)
    public void gcTest() {
        var buffers = new ByteBuffer[50];
        var n = 0;
        for (var i = 0; i < buffers.length; ++i) {
            var buffer = ByteBuffer.allocate(5);
            for (var j = 0; j < 5; ++j) {
                buffer.put((byte) n++);
            }
            buffers[i] = buffer;
            ByteBuffer.allocate(5000);
        }

        var ref = new WeakReference<>(ByteBuffer.allocate(5000));
        while (ref.get() != null) {
            ByteBuffer.allocate(5000);
        }

        n = 0;
        for (var buffer : buffers) {
            buffer.position(0);
            for (var j = 0; j < 5; ++j) {
                assertEquals((byte) n++, buffer.get());
            }
        }
    }

    @Test
    public void putToDirect() {
        var buffer = ByteBuffer.allocateDirect(2);
        var src = ByteBuffer.wrap(new byte[] { 1, 2 });
        src.get();
        buffer.put(src);
        buffer.flip();

        var check = new byte[1];
        buffer.get(check);
        assertArrayEquals(new byte[] { 2 }, check);
    }
}
