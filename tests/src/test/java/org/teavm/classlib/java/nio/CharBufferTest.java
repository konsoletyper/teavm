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
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.InvalidMarkException;
import java.nio.ReadOnlyBufferException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
public class CharBufferTest {
    @Test
    public void allocates() {
        CharBuffer buffer = CharBuffer.allocate(100);
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
        var buffer = ByteBuffer.allocateDirect(10).asCharBuffer();
        var chars = new char[] { 1, 2, 3 };
        buffer.put(0, chars);
        var charsCopy = new char[chars.length];
        buffer.get(0, charsCopy);
        assertArrayEquals(chars, charsCopy);
    }


    @Test
    public void bulkTransferRelative() {
        var arr = new char[5];
        var buffer = CharBuffer.wrap(arr);
        var src = CharBuffer.wrap(new char[] { 1, 2, 3 });
        buffer.put(src);
        assertArrayEquals(new char[] { 1, 2, 3, 0, 0 }, arr);
        assertEquals(3, buffer.position());
        assertEquals(3, src.position());

        assertThrows(BufferOverflowException.class, () -> buffer.put(CharBuffer.wrap(new char[] { 4, 5, 6 })));
        assertThrows(ReadOnlyBufferException.class, () -> buffer.rewind().asReadOnlyBuffer()
                .put(CharBuffer.wrap(new char[] { 4, 5, 6 })));
    }

    @Test(expected = IllegalArgumentException.class)
    public void errorIfAllocatingBufferOfNegativeSize() {
        CharBuffer.allocate(-1);
    }

    @Test
    public void wrapsArray() {
        char[] array = new char[100];
        CharBuffer buffer = CharBuffer.wrap(array, 10, 70);
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
        array[0] = 'A';
        assertThat(buffer.get(0), is('A'));
        buffer.put(1, 'B');
        assertThat(array[1], is('B'));
    }

    @Test
    public void errorWhenWrappingWithWrongParameters() {
        char[] array = new char[100];
        try {
            CharBuffer.wrap(array, -1, 10);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            CharBuffer.wrap(array, 101, 10);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            CharBuffer.wrap(array, 98, 3);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            CharBuffer.wrap(array, 98, -1);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
    }

    @Test
    public void wrapsArrayWithoutOffset() {
        char[] array = new char[100];
        CharBuffer buffer = CharBuffer.wrap(array);
        assertThat(buffer.position(), is(0));
        assertThat(buffer.limit(), is(100));
    }

    @Test
    public void createsSlice() {
        CharBuffer buffer = CharBuffer.allocate(100);
        buffer.put(new char[60]);
        buffer.flip();
        buffer.put(new char[15]);
        CharBuffer slice = buffer.slice();
        assertThat(slice.array(), is(buffer.array()));
        assertThat(slice.position(), is(0));
        assertThat(slice.capacity(), is(45));
        assertThat(slice.limit(), is(45));
        assertThat(slice.isDirect(), is(false));
        assertThat(slice.isReadOnly(), is(false));
        slice.put(3, 'A');
        assertThat(buffer.get(18), is('A'));
        slice.put('B');
        assertThat(buffer.get(15), is('B'));
        buffer.put(16, 'C');
        assertThat(slice.get(1), is('C'));
    }

    @Test
    public void slicePropertiesSameWithOriginal() {
        CharBuffer buffer = CharBuffer.allocate(100).asReadOnlyBuffer().slice();
        assertThat(buffer.isReadOnly(), is(true));
    }

    @Test
    public void createsDuplicate() {
        CharBuffer buffer = CharBuffer.allocate(100);
        buffer.put(new char[60]);
        buffer.flip();
        buffer.put(new char[15]);
        CharBuffer duplicate = buffer.duplicate();
        assertThat(duplicate.array(), is(buffer.array()));
        assertThat(duplicate.position(), is(15));
        assertThat(duplicate.capacity(), is(100));
        assertThat(duplicate.limit(), is(60));
        assertThat(duplicate.isDirect(), is(false));
        assertThat(duplicate.isReadOnly(), is(false));
        duplicate.put(3, 'A');
        assertThat(buffer.get(3), is('A'));
        duplicate.put('B');
        assertThat(buffer.get(15), is('B'));
        buffer.put(1, 'C');
        assertThat(duplicate.get(1), is('C'));
        assertThat(duplicate.array(), is(sameInstance(buffer.array())));
    }

    @Test
    public void getsChar() {
        char[] array = { 'T', 'e', 'a', 'V', 'M' };
        CharBuffer buffer = CharBuffer.wrap(array);
        assertThat(buffer.get(), is('T'));
        assertThat(buffer.get(), is('e'));
        buffer = buffer.slice();
        assertThat(buffer.get(), is('a'));
        assertThat(buffer.get(), is('V'));
    }

    @Test
    public void gettingCharFromEmptyBufferCausesError() {
        char[] array = { 'T', 'e', 'a', 'V', 'M' };
        CharBuffer buffer = CharBuffer.wrap(array);
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
    public void putsChar() {
        char[] array = new char[5];
        CharBuffer buffer = CharBuffer.wrap(array);
        buffer.put('T').put('e').put('a').put('V').put('M');
        assertThat(array, is(new char[] { 'T', 'e', 'a', 'V', 'M' }));
    }

    @Test
    public void puttingCharToEmptyBufferCausesError() {
        char[] array = new char[4];
        CharBuffer buffer = CharBuffer.wrap(array);
        buffer.limit(2);
        buffer.put('A').put('B');
        try {
            buffer.put('C');
            fail("Should have thrown error");
        } catch (BufferOverflowException e) {
            assertThat(array[2], is('\0'));
        }
    }

    @Test(expected = ReadOnlyBufferException.class)
    public void puttingCharToReadOnlyBufferCausesError() {
        char[] array = new char[4];
        CharBuffer buffer = CharBuffer.wrap(array).asReadOnlyBuffer();
        buffer.put('A');
    }

    @Test
    public void getsCharFromGivenLocation() {
        char[] array = { 'T', 'e', 'a', 'V', 'M' };
        CharBuffer buffer = CharBuffer.wrap(array);
        assertThat(buffer.get(0), is('T'));
        assertThat(buffer.get(1), is('e'));
        buffer.get();
        buffer = buffer.slice();
        assertThat(buffer.get(1), is('a'));
        assertThat(buffer.get(2), is('V'));
    }

    @Test
    public void gettingCharFromWrongLocationCausesError() {
        char[] array = { 'T', 'e', 'a', 'V', 'M' };
        CharBuffer buffer = CharBuffer.wrap(array);
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
    public void putsCharToGivenLocation() {
        char[] array = new char[5];
        CharBuffer buffer = CharBuffer.wrap(array);
        buffer.put(0, 'T');
        buffer.put(1, 'e');
        buffer.get();
        buffer = buffer.slice();
        buffer.put(1, 'a');
        buffer.put(2, 'V');
        buffer.put(3, 'M');
        assertThat(array, is(new char[] { 'T', 'e', 'a', 'V', 'M' }));
    }

    @Test
    public void puttingCharToWrongLocationCausesError() {
        char[] array = new char[4];
        CharBuffer buffer = CharBuffer.wrap(array);
        buffer.limit(3);
        try {
            buffer.put(-1, 'A');
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            buffer.put(3, 'A');
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
    }

    @Test(expected = ReadOnlyBufferException.class)
    public void puttingCharToGivenLocationOfReadOnlyBufferCausesError() {
        char[] array = new char[4];
        CharBuffer buffer = CharBuffer.wrap(array).asReadOnlyBuffer();
        buffer.put(0, 'A');
    }

    @Test
    public void getsChars() {
        char[] array = { 'T', 'e', 'a', 'V', 'M' };
        CharBuffer buffer = CharBuffer.wrap(array);
        buffer.get();
        char[] receiver = new char[2];
        buffer.get(receiver, 0, 2);
        assertThat(buffer.position(), is(3));
        assertThat(receiver, is(new char[] { 'e', 'a' }));
    }

    @Test
    public void gettingCharsFromEmptyBufferCausesError() {
        char[] array = { 'T', 'e', 'a', 'V', 'M' };
        CharBuffer buffer = CharBuffer.wrap(array);
        buffer.limit(3);
        char[] receiver = new char[5];
        try {
            buffer.get(receiver, 0, 4);
            fail("Error expected");
        } catch (BufferUnderflowException e) {
            assertThat(receiver, is(new char[5]));
            assertThat(buffer.position(), is(0));
        }
    }

    @Test
    public void gettingCharsWithIllegalArgumentsCausesError() {
        char[] array = { 'T', 'e', 'a', 'V', 'M' };
        CharBuffer buffer = CharBuffer.wrap(array);
        char[] receiver = new char[5];
        try {
            buffer.get(receiver, 0, 6);
        } catch (IndexOutOfBoundsException e) {
            assertThat(receiver, is(new char[5]));
            assertThat(buffer.position(), is(0));
        }
        try {
            buffer.get(receiver, -1, 3);
        } catch (IndexOutOfBoundsException e) {
            assertThat(receiver, is(new char[5]));
            assertThat(buffer.position(), is(0));
        }
        try {
            buffer.get(receiver, 6, 3);
        } catch (IndexOutOfBoundsException e) {
            assertThat(receiver, is(new char[5]));
            assertThat(buffer.position(), is(0));
        }
    }

    @Test
    public void putsChars() {
        char[] array = new char[4];
        CharBuffer buffer = CharBuffer.wrap(array);
        buffer.get();
        char[] data = { 'A', 'B' };
        buffer.put(data, 0, 2);
        assertThat(buffer.position(), is(3));
        assertThat(array, is(new char[] { '\0', 'A', 'B', '\0' }));
    }

    @Test
    public void compacts() {
        char[] array = { 'T', 'e', 'a', 'V', 'M' };
        CharBuffer buffer = CharBuffer.wrap(array);
        buffer.get();
        buffer.mark();
        buffer.compact();
        assertThat(array, is(new char[] { 'e', 'a', 'V', 'M', 'M' }));
        assertThat(buffer.position(), is(4));
        assertThat(buffer.limit(), is(5));
        assertThat(buffer.capacity(), is(5));
        try {
            buffer.reset();
            fail("Exception expected");
        } catch (InvalidMarkException e) {
            // ok
        }
    }

    @Test
    public void marksPosition() {
        char[] array = { 'T', 'e', 'a', 'V', 'M' };
        CharBuffer buffer = CharBuffer.wrap(array);
        buffer.position(1);
        buffer.mark();
        buffer.position(2);
        buffer.reset();
        assertThat(buffer.position(), is(1));
    }

    @Test
    public void readsChars() throws IOException {
        char[] array = { 'T', 'e', 'a', 'V', 'M' };
        CharBuffer buffer = CharBuffer.wrap(array);
        CharBuffer target = CharBuffer.allocate(10);
        int charsRead = buffer.read(target);
        assertThat(charsRead, is(5));
        assertThat(target.get(0), is('T'));
        assertThat(target.get(4), is('M'));
        assertThat(target.get(5), is('\0'));
    }

    @Test
    public void readsCharsToSmallBuffer() throws IOException {
        char[] array = { 'T', 'e', 'a', 'V', 'M' };
        CharBuffer buffer = CharBuffer.wrap(array);
        CharBuffer target = CharBuffer.allocate(2);
        int charsRead = buffer.read(target);
        assertThat(charsRead, is(2));
        assertThat(target.get(0), is('T'));
        assertThat(target.get(1), is('e'));
    }

    @Test
    public void readsCharsToEmptyBuffer() throws IOException {
        char[] array = { 'T', 'e', 'a', 'V', 'M' };
        CharBuffer buffer = CharBuffer.wrap(array);
        CharBuffer target = CharBuffer.allocate(2);
        target.position(2);
        int charsRead = buffer.read(target);
        assertThat(charsRead, is(0));
    }

    @Test
    public void readsCharsFromEmptyBuffer() throws IOException {
        char[] array = { 'T', 'e', 'a', 'V', 'M' };
        CharBuffer buffer = CharBuffer.wrap(array);
        CharBuffer target = CharBuffer.allocate(2);
        buffer.position(5);
        int charsRead = buffer.read(target);
        assertThat(charsRead, is(-1));
    }

    @Test
    public void wrapsCharSequence() {
        CharBuffer buffer = CharBuffer.wrap("TeaVM", 2, 4);
        assertThat(buffer.capacity(), is(5));
        assertThat(buffer.limit(), is(4));
        assertThat(buffer.position(), is(2));
    }

    @Test
    public void putsString() {
        CharBuffer buffer = CharBuffer.allocate(100);
        buffer.put("TeaVM");
        assertThat(buffer.flip().toString(), is("TeaVM"));
    }

    @Test
    public void putGetEmptyArray() {
        CharBuffer cb = CharBuffer.allocate(0);
        cb.put("");
        cb.get(new char[0]);
    }

    @Test
    public void bulkPut() {
        var buffer = CharBuffer.allocate(100);
        buffer.put(new char[] { 'A', 'B', 'C' });
        assertEquals(3, buffer.position());
        assertEquals('A', buffer.get(0));
        assertEquals('B', buffer.get(1));
        assertEquals('C', buffer.get(2));

        buffer.put(1, new char[] { 'D', 'E', 'F' });
        assertEquals(3, buffer.position());
        assertEquals('A', buffer.get(0));
        assertEquals('D', buffer.get(1));
        assertEquals('E', buffer.get(2));
        assertEquals('F', buffer.get(3));

        buffer.put(0, new char[] { 'G', 'H', 'I', 'J' }, 1, 2);
        assertEquals('H', buffer.get(0));
        assertEquals('I', buffer.get(1));
        assertEquals('E', buffer.get(2));
        assertEquals('F', buffer.get(3));
    }

    @Test
    public void bulkPutWrapper() {
        var byteBuffer = ByteBuffer.allocate(100);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        var buffer = byteBuffer.asCharBuffer();

        buffer.put(new char[] { 'A', 'B', 'C' });
        assertEquals(3, buffer.position());
        assertEquals('A', buffer.get(0));
        assertEquals('B', buffer.get(1));
        assertEquals('C', buffer.get(2));

        buffer.put(1, new char[] { 'D', 'E', 'F' });
        assertEquals(3, buffer.position());
        assertEquals('A', buffer.get(0));
        assertEquals('D', buffer.get(1));
        assertEquals('E', buffer.get(2));
        assertEquals('F', buffer.get(3));
        assertEquals(0, byteBuffer.get(0));
        assertEquals('A', byteBuffer.get(1));

        buffer.put(0, new char[] { 'G', 'H', 'I', 'J' }, 1, 2);
        assertEquals('H', buffer.get(0));
        assertEquals('I', buffer.get(1));
        assertEquals('E', buffer.get(2));
        assertEquals('F', buffer.get(3));

        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer = byteBuffer.asCharBuffer();

        buffer.put(new char[] { 'A', 'B', 'C' });
        assertEquals(3, buffer.position());
        assertEquals('A', buffer.get(0));
        assertEquals('B', buffer.get(1));
        assertEquals('C', buffer.get(2));

        buffer.put(1, new char[] { 'D', 'E', 'F' });
        assertEquals(3, buffer.position());
        assertEquals('A', buffer.get(0));
        assertEquals('D', buffer.get(1));
        assertEquals('E', buffer.get(2));
        assertEquals('F', buffer.get(3));
        assertEquals('A', byteBuffer.get(0));
        assertEquals(0, byteBuffer.get(1));

        buffer.put(0, new char[] { 'G', 'H', 'I', 'J' }, 1, 2);
        assertEquals('H', buffer.get(0));
        assertEquals('I', buffer.get(1));
        assertEquals('E', buffer.get(2));
        assertEquals('F', buffer.get(3));
    }

    @Test
    public void bulkPutBuffer() {
        var buffer = CharBuffer.allocate(100);
        buffer.put(CharBuffer.wrap(new char[] { 'A', 'B', 'C' }));

        assertEquals(3, buffer.position());
        assertEquals('A', buffer.get(0));
        assertEquals('B', buffer.get(1));
        assertEquals('C', buffer.get(2));

        buffer.put(1, CharBuffer.wrap(new char[] { 'D', 'E', 'F' }), 1, 2);
        assertEquals(3, buffer.position());
        assertEquals('A', buffer.get(0));
        assertEquals('E', buffer.get(1));
        assertEquals('F', buffer.get(2));
    }

    @Test
    public void bulkPutBufferWrapper() {
        var buffer = ByteBuffer.allocate(100).order(ByteOrder.BIG_ENDIAN).asCharBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] { 0, 'A', 0, 'B', 0, 'C' })
                .order(ByteOrder.BIG_ENDIAN)
                .asCharBuffer());

        assertEquals('A', buffer.get(0));
        assertEquals('B', buffer.get(1));
        assertEquals('C', buffer.get(2));

        buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN).asCharBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] { 0, 'A', 0, 'B', 0, 'C' })
                .order(ByteOrder.BIG_ENDIAN)
                .asCharBuffer());

        assertEquals('A', buffer.get(0));
        assertEquals('B', buffer.get(1));
        assertEquals('C', buffer.get(2));

        buffer = ByteBuffer.allocate(100).order(ByteOrder.BIG_ENDIAN).asCharBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] { 'A', 0, 'B', 0, 'C', 0 })
                .order(ByteOrder.LITTLE_ENDIAN)
                .asCharBuffer());

        assertEquals('A', buffer.get(0));
        assertEquals('B', buffer.get(1));
        assertEquals('C', buffer.get(2));

        buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN).asCharBuffer();
        buffer.put(ByteBuffer.wrap(new byte[] { 'A', 0, 'B', 0, 'C', 0 })
                .order(ByteOrder.LITTLE_ENDIAN)
                .asCharBuffer());

        assertEquals('A', buffer.get(0));
        assertEquals('B', buffer.get(1));
        assertEquals('C', buffer.get(2));
    }

    @Test
    public void bulkGet() {
        var buffer = CharBuffer.wrap(new char[] { 'A', 'B', 'C', 'D', 'E', 'F' });
        var chars = new char[3];

        buffer.get(chars);
        assertArrayEquals(new char[] { 'A', 'B', 'C' }, chars);
        assertEquals(3, buffer.position());

        buffer.get(1, chars);
        assertArrayEquals(new char[] { 'B', 'C', 'D' }, chars);
        assertEquals(3, buffer.position());

        buffer.get(4, chars, 1, 2);
        assertArrayEquals(new char[] { 'B', 'E', 'F' }, chars);
        assertEquals(3, buffer.position());
    }

    @Test
    public void bulkGetWrapper() {
        var buffer = ByteBuffer.wrap(new byte[] { 'A', 0, 'B', 0, 'C', 0, 'D', 0, 'E', 0, 'F', 0 })
                .order(ByteOrder.LITTLE_ENDIAN)
                .asCharBuffer();
        var chars = new char[3];

        buffer.get(chars);
        assertArrayEquals(new char[] { 'A', 'B', 'C' }, chars);
        assertEquals(3, buffer.position());

        buffer.get(1, chars);
        assertArrayEquals(new char[] { 'B', 'C', 'D' }, chars);
        assertEquals(3, buffer.position());

        buffer.get(4, chars, 1, 2);
        assertArrayEquals(new char[] { 'B', 'E', 'F' }, chars);
        assertEquals(3, buffer.position());

        buffer = ByteBuffer.wrap(new byte[] { 0, 'A', 0, 'B', 0, 'C', 0, 'D', 0, 'E', 0, 'F' })
                .order(ByteOrder.BIG_ENDIAN)
                .asCharBuffer();

        buffer.get(chars);
        assertArrayEquals(new char[] { 'A', 'B', 'C' }, chars);
        assertEquals(3, buffer.position());

        buffer.get(1, chars);
        assertArrayEquals(new char[] { 'B', 'C', 'D' }, chars);
        assertEquals(3, buffer.position());

        buffer.get(4, chars, 1, 2);
        assertArrayEquals(new char[] { 'B', 'E', 'F' }, chars);
        assertEquals(3, buffer.position());
    }
}
