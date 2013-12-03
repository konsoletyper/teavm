package org.teavm.classlib.java.lang;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
public class SystemTests {
    @Test
    public void copiesArray() {
        Object a = new Object();
        Object b = new Object();
        Object[] src = { a, b, a };
        Object[] dest = new Object[3];
        System.arraycopy(src, 0, dest, 0, 3);
        assertSame(a, dest[0]);
        assertSame(b, dest[1]);
        assertSame(a, dest[2]);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void failsToCopyArraysWithInvalidIndexes() {
        System.arraycopy(new Object[0], 0, new TObject[0], 0, 1);
    }

    @Test(expected = ArrayStoreException.class)
    public void failsToCopyArraysWithIncompatibleElements() {
        System.arraycopy(new Object[1], 0, new int[1], 0, 1);
    }

    @Test(expected = NullPointerException.class)
    public void failsToCopyFromNullSource() {
        System.arraycopy(null, 0, new int[1], 0, 1);
    }

    @Test(expected = NullPointerException.class)
    public void failsToCopyToNullTarget() {
        System.arraycopy(new TObject[1], 0, null, 0, 1);
    }
}
