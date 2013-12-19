package org.teavm.classlib.java.lang;

import org.teavm.javascript.ni.GeneratedBy;

/**
 *
 * @author Alexey Andreev
 */
public class TDouble {
    @GeneratedBy(DoubleNativeGenerator.class)
    public static native boolean isNaN(double v);

    @GeneratedBy(DoubleNativeGenerator.class)
    public static native boolean isInfinite(double v);
}
