package org.teavm.classlib.java.lang;

import org.teavm.javascript.ni.GeneratedBy;

/**
 *
 * @author Alexey Andreev
 */
public class TFloat {
    @GeneratedBy(FloatNativeGenerator.class)
    public static native boolean isNaN(float v);

    @GeneratedBy(FloatNativeGenerator.class)
    public static native boolean isInfinite(float v);
}
