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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.DoubleBuffer;
import java.nio.InvalidMarkException;
import java.nio.ReadOnlyBufferException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class DoubleBufferTest {
    @Test
    public void allocatesSimple() {
        DoubleBuffer buffer = DoubleBuffer.allocate(100);
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
        DoubleBuffer.allocate(-1);
    }

    @Test
    public void wrapsArray() {
        double[] array = new double[100];
        DoubleBuffer buffer = DoubleBuffer.wrap(array, 10, 70);
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
        assertThat(buffer.get(0), is((double) 23));
        buffer.put(1, 24);
        assertThat(array[1], is((double) 24));
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
        assertThat(buffer.position(), is(0));
        assertThat(buffer.limit(), is(100));
    }

    @Test
    public void createsSlice() {
        DoubleBuffer buffer = DoubleBuffer.allocate(100);
        buffer.put(new double[60]);
        buffer.flip();
        buffer.put(new double[15]);
        DoubleBuffer slice = buffer.slice();
        assertThat(slice.array(), is(buffer.array()));
        assertThat(slice.position(), is(0));
        assertThat(slice.capacity(), is(45));
        assertThat(slice.limit(), is(45));
        assertThat(slice.isDirect(), is(false));
        assertThat(slice.isReadOnly(), is(false));
        slice.put(3, 23);
        assertThat(buffer.get(18), is((double) 23));
        slice.put(24);
        assertThat(buffer.get(15), is((double) 24));
        buffer.put(16, 25);
        assertThat(slice.get(1), is((double) 25));
    }

    @Test
    public void slicePropertiesSameWithOriginal() {
        DoubleBuffer buffer = DoubleBuffer.allocate(100).asReadOnlyBuffer().slice();
        assertThat(buffer.isReadOnly(), is(true));
    }

    @Test
    public void createsDuplicate() {
        DoubleBuffer buffer = DoubleBuffer.allocate(100);
        buffer.put(new double[60]);
        buffer.flip();
        buffer.put(new double[15]);
        DoubleBuffer duplicate = buffer.duplicate();
        assertThat(duplicate.array(), is(buffer.array()));
        assertThat(duplicate.position(), is(15));
        assertThat(duplicate.capacity(), is(100));
        assertThat(duplicate.limit(), is(60));
        assertThat(duplicate.isDirect(), is(false));
        assertThat(duplicate.isReadOnly(), is(false));
        duplicate.put(3, 23);
        assertThat(buffer.get(3), is((double) 23));
        duplicate.put(24);
        assertThat(buffer.get(15), is((double) 24));
        buffer.put(1, 25);
        assertThat(duplicate.get(1), is((double) 25));
        assertThat(duplicate.array(), is(sameInstance(buffer.array())));
    }

    @Test
    public void getsDouble() {
        double[] array = { 2, 3, 5, 7 };
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
        assertThat(buffer.get(), is((double) 2));
        assertThat(buffer.get(), is((double) 3));
        buffer = buffer.slice();
        assertThat(buffer.get(), is((double) 5));
        assertThat(buffer.get(), is((double) 7));
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
        assertThat(array, is(new double[] { 2, 3, 5, 7 }));
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
            assertThat(array[2], is((double) 0));
        }
    }

    @Test(expected = ReadOnlyBufferException.class)
    public void puttingDoubleToReadOnlyBufferCausesError() {
        double[] array = new double[4];
        DoubleBuffer buffer = DoubleBuffer.wrap(array).asReadOnlyBuffer();
        buffer.put(2);
    }

    @Test
    public void getsDoubleFromGivenLocation() {
        double[] array = { 2, 3, 5, 7 };
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
        assertThat(buffer.get(0), is((double) 2));
        assertThat(buffer.get(1), is((double) 3));
        buffer.get();
        buffer = buffer.slice();
        assertThat(buffer.get(1), is((double) 5));
        assertThat(buffer.get(2), is((double) 7));
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
        assertThat(array, is(new double[] { 2, 3, 5, 7 }));
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

    @Test(expected = ReadOnlyBufferException.class)
    public void puttingDoubleToGivenLocationOfReadOnlyBufferCausesError() {
        double[] array = new double[4];
        DoubleBuffer buffer = DoubleBuffer.wrap(array).asReadOnlyBuffer();
        buffer.put(0, 2);
    }

    @Test
    public void getsDoubles() {
        double[] array = { 2, 3, 5, 7 };
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
        buffer.get();
        double[] receiver = new double[2];
        buffer.get(receiver, 0, 2);
        assertThat(buffer.position(), is(3));
        assertThat(receiver, is(new double[] { 3, 5 }));
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
            assertThat(receiver, is(new double[4]));
            assertThat(buffer.position(), is(0));
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
            assertThat(receiver, is(new double[4]));
            assertThat(buffer.position(), is(0));
        }
        try {
            buffer.get(receiver, -1, 3);
        } catch (IndexOutOfBoundsException e) {
            assertThat(receiver, is(new double[4]));
            assertThat(buffer.position(), is(0));
        }
        try {
            buffer.get(receiver, 6, 3);
        } catch (IndexOutOfBoundsException e) {
            assertThat(receiver, is(new double[4]));
            assertThat(buffer.position(), is(0));
        }
    }

    @Test
    public void putsDoubles() {
        double[] array = new double[4];
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
        buffer.get();
        double[] data = { 2, 3 };
        buffer.put(data, 0, 2);
        assertThat(buffer.position(), is(3));
        assertThat(array, is(new double[] { 0, 2, 3, 0 }));
    }

    @Test
    public void compacts() {
        double[] array = { 2, 3, 5, 7 };
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
        buffer.get();
        buffer.mark();
        buffer.compact();
        assertThat(array, is(new double[] { 3, 5, 7, 7 }));
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
        double[] array = { 2, 3, 5, 7 };
        DoubleBuffer buffer = DoubleBuffer.wrap(array);
        buffer.position(1);
        buffer.mark();
        buffer.position(2);
        buffer.reset();
        assertThat(buffer.position(), is(1));
    }
}
