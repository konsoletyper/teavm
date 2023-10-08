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
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.IntBuffer;
import java.nio.InvalidMarkException;
import java.nio.ReadOnlyBufferException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class IntBufferTest {
    @Test
    public void allocatesSimple() {
        IntBuffer buffer = IntBuffer.allocate(100);
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
        IntBuffer.allocate(-1);
    }

    @Test
    public void wrapsArray() {
        int[] array = new int[100];
        IntBuffer buffer = IntBuffer.wrap(array, 10, 70);
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
        assertThat(buffer.get(0), is(23));
        buffer.put(1, 24);
        assertThat(array[1], is(24));
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
        assertThat(buffer.position(), is(0));
        assertThat(buffer.limit(), is(100));
    }

    @Test
    public void createsSlice() {
        IntBuffer buffer = IntBuffer.allocate(100);
        buffer.put(new int[60]);
        buffer.flip();
        buffer.put(new int[15]);
        IntBuffer slice = buffer.slice();
        assertThat(slice.array(), is(buffer.array()));
        assertThat(slice.position(), is(0));
        assertThat(slice.capacity(), is(45));
        assertThat(slice.limit(), is(45));
        assertThat(slice.isDirect(), is(false));
        assertThat(slice.isReadOnly(), is(false));
        slice.put(3, 23);
        assertThat(buffer.get(18), is(23));
        slice.put(24);
        assertThat(buffer.get(15), is(24));
        buffer.put(16, 25);
        assertThat(slice.get(1), is(25));
    }


    @Test
    public void sliceOfSlice() {
        var buffer = IntBuffer.allocate(100);
        buffer.put(new int[10]);
        var slice1 = buffer.slice();
        slice1.put(new int[15]);
        var slice2 = slice1.slice();

        assertEquals(25, slice2.arrayOffset());
        assertEquals(75, slice2.capacity());
    }

    @Test
    public void slicePropertiesSameWithOriginal() {
        IntBuffer buffer = IntBuffer.allocate(100).asReadOnlyBuffer().slice();
        assertThat(buffer.isReadOnly(), is(true));
    }

    @Test
    public void createsDuplicate() {
        IntBuffer buffer = IntBuffer.allocate(100);
        buffer.put(new int[60]);
        buffer.flip();
        buffer.put(new int[15]);
        IntBuffer duplicate = buffer.duplicate();
        assertThat(duplicate.array(), is(buffer.array()));
        assertThat(duplicate.position(), is(15));
        assertThat(duplicate.capacity(), is(100));
        assertThat(duplicate.limit(), is(60));
        assertThat(duplicate.isDirect(), is(false));
        assertThat(duplicate.isReadOnly(), is(false));
        duplicate.put(3, 23);
        assertThat(buffer.get(3), is(23));
        duplicate.put(24);
        assertThat(buffer.get(15), is(24));
        buffer.put(1, 25);
        assertThat(duplicate.get(1), is(25));
        assertThat(duplicate.array(), is(sameInstance(buffer.array())));
    }

    @Test
    public void getsInt() {
        int[] array = { 2, 3, 5, 7 };
        IntBuffer buffer = IntBuffer.wrap(array);
        assertThat(buffer.get(), is(2));
        assertThat(buffer.get(), is(3));
        buffer = buffer.slice();
        assertThat(buffer.get(), is(5));
        assertThat(buffer.get(), is(7));
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
        assertThat(array, is(new int[] { 2, 3, 5, 7 }));
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
            assertThat(array[2], is(0));
        }
    }

    @Test(expected = ReadOnlyBufferException.class)
    public void puttingIntToReadOnlyBufferCausesError() {
        int[] array = new int[4];
        IntBuffer buffer = IntBuffer.wrap(array).asReadOnlyBuffer();
        buffer.put(2);
    }

    @Test
    public void getsIntFromGivenLocation() {
        int[] array = { 2, 3, 5, 7 };
        IntBuffer buffer = IntBuffer.wrap(array);
        assertThat(buffer.get(0), is(2));
        assertThat(buffer.get(1), is(3));
        buffer.get();
        buffer = buffer.slice();
        assertThat(buffer.get(1), is(5));
        assertThat(buffer.get(2), is(7));
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
        assertThat(array, is(new int[] { 2, 3, 5, 7 }));
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

    @Test(expected = ReadOnlyBufferException.class)
    public void puttingIntToGivenLocationOfReadOnlyBufferCausesError() {
        int[] array = new int[4];
        IntBuffer buffer = IntBuffer.wrap(array).asReadOnlyBuffer();
        buffer.put(0, 2);
    }

    @Test
    public void getsInts() {
        int[] array = { 2, 3, 5, 7 };
        IntBuffer buffer = IntBuffer.wrap(array);
        buffer.get();
        int[] receiver = new int[2];
        buffer.get(receiver, 0, 2);
        assertThat(buffer.position(), is(3));
        assertThat(receiver, is(new int[] { 3, 5 }));
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
            assertThat(receiver, is(new int[4]));
            assertThat(buffer.position(), is(0));
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
            assertThat(receiver, is(new int[4]));
            assertThat(buffer.position(), is(0));
        }
        try {
            buffer.get(receiver, -1, 3);
        } catch (IndexOutOfBoundsException e) {
            assertThat(receiver, is(new int[4]));
            assertThat(buffer.position(), is(0));
        }
        try {
            buffer.get(receiver, 6, 3);
        } catch (IndexOutOfBoundsException e) {
            assertThat(receiver, is(new int[4]));
            assertThat(buffer.position(), is(0));
        }
    }

    @Test
    public void putsInts() {
        int[] array = new int[4];
        IntBuffer buffer = IntBuffer.wrap(array);
        buffer.get();
        int[] data = { 2, 3 };
        buffer.put(data, 0, 2);
        assertThat(buffer.position(), is(3));
        assertThat(array, is(new int[] {0, 2, 3, 0 }));
    }

    @Test
    public void compacts() {
        int[] array = { 2, 3, 5, 7 };
        IntBuffer buffer = IntBuffer.wrap(array);
        buffer.get();
        buffer.mark();
        buffer.compact();
        assertThat(array, is(new int[] { 3, 5, 7, 7 }));
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
        int[] array = { 2, 3, 5, 7 };
        IntBuffer buffer = IntBuffer.wrap(array);
        buffer.position(1);
        buffer.mark();
        buffer.position(2);
        buffer.reset();
        assertThat(buffer.position(), is(1));
    }

    @Test
    public void putEmptyArray() {
        IntBuffer ib = IntBuffer.allocate(0);
        ib.put(new int[0]);
        ib.get(new int[0]);
    }
}
