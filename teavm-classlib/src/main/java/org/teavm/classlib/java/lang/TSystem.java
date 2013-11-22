package org.teavm.classlib.java.lang;

import org.teavm.javascript.ni.GeneratedBy;

/**
 *
 * @author Alexey Andreev
 */
public final class TSystem extends TObject {
    private TSystem() {
    }

    public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
        doArrayCopy(src, srcPos, dest, destPos, length);
    }

    @GeneratedBy(SystemNativeGenerator.class)
    private static native void doArrayCopy(Object src, int srcPos, Object dest, int destPos, int length);
}
