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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;
import java.nio.ReadOnlyBufferException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class ByteBufferTest {
    @Test
    public void allocatesDirect() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(100);
        assertThat(buffer.isDirect(), is(true));
        assertThat(buffer.isReadOnly(), is(false));
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

    @Test(expected = IllegalArgumentException.class)
    public void errorIfAllocatingDirectOfNegativeSize() {
        ByteBuffer.allocateDirect(-2);
    }

    @Test
    public void allocatesSimple() {
        ByteBuffer buffer = ByteBuffer.allocate(100);
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

    @Test(expected = IllegalArgumentException.class)
    public void errorIfAllocatingBufferOfNegativeSize() {
        ByteBuffer.allocate(-1);
    }

    @Test
    public void wrapsArray() {
        byte[] array = new byte[100];
        ByteBuffer buffer = ByteBuffer.wrap(array, 10, 70);
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
        assertThat(buffer.get(0), is((byte) 23));
        buffer.put(1, (byte) 24);
        assertThat(array[1], is((byte) 24));
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
        assertThat(buffer.position(), is(0));
        assertThat(buffer.limit(), is(100));
    }

    @Test
    public void createsSlice() {
        ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.put(new byte[60]);
        buffer.flip();
        buffer.put(new byte[15]);
        ByteBuffer slice = buffer.slice();
        assertThat(slice.array(), is(buffer.array()));
        assertThat(slice.position(), is(0));
        assertThat(slice.capacity(), is(45));
        assertThat(slice.limit(), is(45));
        assertThat(slice.isDirect(), is(false));
        assertThat(slice.isReadOnly(), is(false));
        slice.put(3, (byte) 23);
        assertThat(buffer.get(18), is((byte) 23));
        slice.put((byte) 24);
        assertThat(buffer.get(15), is((byte) 24));
        buffer.put(16, (byte) 25);
        assertThat(slice.get(1), is((byte) 25));
    }

    @Test
    public void slicePropertiesSameWithOriginal() {
        ByteBuffer buffer = ByteBuffer.allocate(100).asReadOnlyBuffer().slice();
        assertThat(buffer.isReadOnly(), is(true));
        buffer = ByteBuffer.allocateDirect(100);
        assertThat(buffer.isDirect(), is(true));
    }

    @Test
    public void createsDuplicate() {
        ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.put(new byte[60]);
        buffer.flip();
        buffer.put(new byte[15]);
        ByteBuffer duplicate = buffer.duplicate();
        assertThat(duplicate.array(), is(buffer.array()));
        assertThat(duplicate.position(), is(15));
        assertThat(duplicate.capacity(), is(100));
        assertThat(duplicate.limit(), is(60));
        assertThat(duplicate.isDirect(), is(false));
        assertThat(duplicate.isReadOnly(), is(false));
        duplicate.put(3, (byte) 23);
        assertThat(buffer.get(3), is((byte) 23));
        duplicate.put((byte) 24);
        assertThat(buffer.get(15), is((byte) 24));
        buffer.put(1, (byte) 25);
        assertThat(duplicate.get(1), is((byte) 25));
        assertThat(duplicate.array(), is(sameInstance(buffer.array())));
    }

    @Test
    public void getsByte() {
        byte[] array = {2, 3, 5, 7};
        ByteBuffer buffer = ByteBuffer.wrap(array);
        assertThat(buffer.get(), is((byte) 2));
        assertThat(buffer.get(), is((byte) 3));
        buffer = buffer.slice();
        assertThat(buffer.get(), is((byte) 5));
        assertThat(buffer.get(), is((byte) 7));
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
        assertThat(array, is(new byte[]{2, 3, 5, 7}));
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
            assertThat(array[2], is((byte) 0));
        }
    }

    @Test(expected = ReadOnlyBufferException.class)
    public void puttingByteToReadOnlyBufferCausesError() {
        byte[] array = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(array).asReadOnlyBuffer();
        buffer.put((byte) 2);
    }

    @Test
    public void getsByteFromGivenLocation() {
        byte[] array = {2, 3, 5, 7};
        ByteBuffer buffer = ByteBuffer.wrap(array);
        assertThat(buffer.get(0), is((byte) 2));
        assertThat(buffer.get(1), is((byte) 3));
        buffer.get();
        buffer = buffer.slice();
        assertThat(buffer.get(1), is((byte) 5));
        assertThat(buffer.get(2), is((byte) 7));
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
        assertThat(array, is(new byte[]{2, 3, 5, 7}));
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

    @Test(expected = ReadOnlyBufferException.class)
    public void puttingByteToGivenLocationOfReadOnlyBufferCausesError() {
        byte[] array = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(array).asReadOnlyBuffer();
        buffer.put(0, (byte) 2);
    }

    @Test
    public void getsBytes() {
        byte[] array = {2, 3, 5, 7};
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.get();
        byte[] receiver = new byte[2];
        buffer.get(receiver, 0, 2);
        assertThat(buffer.position(), is(3));
        assertThat(receiver, is(new byte[]{3, 5}));
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
            assertThat(receiver, is(new byte[4]));
            assertThat(buffer.position(), is(0));
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
            assertThat(receiver, is(new byte[4]));
            assertThat(buffer.position(), is(0));
        }
        try {
            buffer.get(receiver, -1, 3);
            fail("Error expected");
        } catch (IndexOutOfBoundsException e) {
            assertThat(receiver, is(new byte[4]));
            assertThat(buffer.position(), is(0));
        }
        try {
            buffer.get(receiver, 6, 3);
            fail("Error expected");
        } catch (IndexOutOfBoundsException e) {
            assertThat(receiver, is(new byte[4]));
            assertThat(buffer.position(), is(0));
        }
    }

    @Test
    public void putsBytes() {
        byte[] array = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.get();
        byte[] data = {2, 3};
        buffer.put(data, 0, 2);
        assertThat(buffer.position(), is(3));
        assertThat(array, is(new byte[]{0, 2, 3, 0}));
    }

    @Test
    public void putsBytesWithZeroLengthArray() {
        byte[] array = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.get();
        byte[] data = {};
        buffer.put(data, 0, 0);
        assertThat(buffer.position(), is(1));
        assertThat(array, is(new byte[]{0, 0, 0, 0}));
    }

    @Test
    public void compacts() {
        byte[] array = {2, 3, 5, 7};
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.get();
        buffer.mark();
        buffer.compact();
        assertThat(array, is(new byte[]{3, 5, 7, 7}));
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
        byte[] array = {2, 3, 5, 7};
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.position(1);
        buffer.mark();
        buffer.position(2);
        buffer.reset();
        assertThat(buffer.position(), is(1));
    }

    @Test
    public void getsChar() {
        byte[] array = {0, 'A', 0, 'B'};
        ByteBuffer buffer = ByteBuffer.wrap(array);
        assertThat(buffer.getChar(), is('A'));
        assertThat(buffer.getChar(), is('B'));
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
        assertThat(buffer.getChar(0), is('A'));
        assertThat(buffer.getChar(2), is('B'));
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
        assertThat(buffer.get(0), is((byte) 0));
        assertThat(buffer.get(1), is((byte) 'A'));
        assertThat(buffer.get(2), is((byte) 0));
        assertThat(buffer.get(3), is((byte) 'B'));
        buffer.putChar(0, 'E');
        assertThat(buffer.get(1), is((byte) 'E'));
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
        assertThat(buffer.getShort(), is((short) 0x2324));
        assertThat(buffer.getShort(), is((short) 0x2526));
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
        assertThat(buffer.getShort(0), is((short) 0x2324));
        assertThat(buffer.getShort(2), is((short) 0x2526));
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
        assertThat(buffer.get(0), is((byte) 0x23));
        assertThat(buffer.get(1), is((byte) 0x24));
        assertThat(buffer.get(2), is((byte) 0x25));
        assertThat(buffer.get(3), is((byte) 0x26));
        buffer.putShort(0, (short) 0x2B2C);
        assertThat(buffer.get(1), is((byte) 0x2C));
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
        assertThat(buffer.getInt(), is(0x23242526));
        assertThat(buffer.getInt(), is(0x27282930));
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
        assertThat(buffer.getInt(0), is(0x23242526));
        assertThat(buffer.getInt(4), is(0x27282930));
        try {
            buffer.getInt(7);
            fail("Exception expected");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void getsLong() {
        byte[] array = {0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38};
        ByteBuffer buffer = ByteBuffer.wrap(array);
        assertThat(buffer.getLong(), is(0x2324252627282930L));
        assertThat(buffer.getLong(), is(0x3132333435363738L));
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
        assertThat(buffer.getLong(0), is(0x2324252627282930L));
        assertThat(buffer.getLong(8), is(0x3132333435363738L));
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
        assertThat(buffer.get(0), is((byte) 0x23));
        assertThat(buffer.get(1), is((byte) 0x24));
        assertThat(buffer.get(2), is((byte) 0x25));
        assertThat(buffer.get(3), is((byte) 0x26));
        assertThat(buffer.get(4), is((byte) 0x27));
        assertThat(buffer.get(5), is((byte) 0x28));
        assertThat(buffer.get(6), is((byte) 0x29));
        assertThat(buffer.get(7), is((byte) 0x30));
        buffer.putLong(0, 0xAABBCCDDEEFF0000L);
        assertThat(buffer.get(1), is((byte) 0xBB));
        try {
            buffer.putLong(15, 0x0L);
            fail("Exception expected");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }
}
