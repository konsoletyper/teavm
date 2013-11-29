package org.teavm.classlib.java.lang;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
class TSystemTests {
    @Test
    public void copiesArray() {
        TObject a = new TObject();
        TObject b = new TObject();
        TObject[] src = { a, b, a };
        TObject[] dest = new TObject[3];
        TSystem.arraycopy(TObject.wrap(src), 0, TObject.wrap(dest), 0, 3);
        assertSame(a, dest[0]);
        assertSame(b, dest[1]);
        assertSame(a, dest[2]);
    }

    @Test
    public void failsToCopyArraysWithInvalidIndexes() {
        TSystem.arraycopy(TObject.wrap(new TObject[0]), 0, TObject.wrap(new TObject[0]), 0, 1);
    }

    @Test
    public void failsToCopyArraysWithIncompatibleElements() {
        TSystem.arraycopy(TObject.wrap(new TObject[1]), 0, TObject.wrap(new int[1]), 0, 1);
    }
}
