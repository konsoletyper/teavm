package org.teavm.classlib.java.nio;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.nio.*;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
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
        assertThat(buffer.get(0), is((byte)23));
        buffer.put(1, (byte)24);
        assertThat(array[1], is((byte)24));
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
        slice.put(3, (byte)23);
        assertThat(buffer.get(18), is((byte)23));
        slice.put((byte)24);
        assertThat(buffer.get(15), is((byte)24));
        buffer.put(16, (byte)25);
        assertThat(slice.get(1), is((byte)25));
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
        duplicate.put(3, (byte)23);
        assertThat(buffer.get(3), is((byte)23));
        duplicate.put((byte)24);
        assertThat(buffer.get(15), is((byte)24));
        buffer.put(1, (byte)25);
        assertThat(duplicate.get(1), is((byte)25));
        assertThat(duplicate.array(), is(sameInstance(buffer.array())));
    }

    @Test
    public void getsByte() {
        byte[] array = { 2, 3, 5, 7 };
        ByteBuffer buffer = ByteBuffer.wrap(array);
        assertThat(buffer.get(), is((byte)2));
        assertThat(buffer.get(), is((byte)3));
        buffer = buffer.slice();
        assertThat(buffer.get(), is((byte)5));
        assertThat(buffer.get(), is((byte)7));
    }

    @Test
    public void gettingByteFromEmptyBufferCausesError() {
        byte[] array = { 2, 3, 5, 7 };
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
        buffer.put((byte)2).put((byte)3).put((byte)5).put((byte)7);
        assertThat(array, is(new byte[] { 2, 3, 5, 7 }));
    }

    @Test
    public void puttingByteToEmptyBufferCausesError() {
        byte[] array = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.limit(2);
        buffer.put((byte)2).put((byte)3);
        try {
            buffer.put((byte)5);
            fail("Should have thrown error");
        } catch (BufferOverflowException e) {
            System.out.println(e);
            assertThat(array[2], is((byte)0));
        }
    }

    @Test(expected = ReadOnlyBufferException.class)
    public void puttingByteToReadOnlyBufferCausesError() {
        byte[] array = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(array).asReadOnlyBuffer();
        buffer.put((byte)2);
    }

    @Test
    public void getsByteFromGivenLocation() {
        byte[] array = { 2, 3, 5, 7 };
        ByteBuffer buffer = ByteBuffer.wrap(array);
        assertThat(buffer.get(0), is((byte)2));
        assertThat(buffer.get(1), is((byte)3));
        buffer.get();
        buffer = buffer.slice();
        assertThat(buffer.get(1), is((byte)5));
        assertThat(buffer.get(2), is((byte)7));
    }

    @Test
    public void gettingByteFromWrongLocationCausesError() {
        byte[] array = { 2, 3, 5, 7 };
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
        buffer.put(0, (byte)2);
        buffer.put(1, (byte)3);
        buffer.get();
        buffer = buffer.slice();
        buffer.put(1, (byte)5);
        buffer.put(2, (byte)7);
        assertThat(array, is(new byte[] { 2, 3, 5, 7 }));
    }

    @Test
    public void puttingByteToWrongLocationCausesError() {
        byte[] array = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.limit(3);
        try {
            buffer.put(-1, (byte)2);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            buffer.put(3, (byte)2);
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
    }

    @Test(expected = ReadOnlyBufferException.class)
    public void puttingByteToGivenLocationOfReadOnlyBufferCausesError() {
        byte[] array = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(array).asReadOnlyBuffer();
        buffer.put(0, (byte)2);
    }
}
