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
        TSystem.arraycopy(src, 0, dest, 0, 3);
        assertSame(a, dest[0]);
        assertSame(b, dest[1]);
        assertSame(a, dest[2]);
    }
}
