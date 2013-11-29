package org.teavm.classlib.java.lang.reflect;

import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.dependency.PluggableDependency;
import org.teavm.javascript.ni.GeneratedBy;

/**
 *
 * @author Alexey Andreev
 */
public final class TArray extends TObject {
    @GeneratedBy(TArrayNativeGenerator.class)
    @PluggableDependency(TArrayNativeGenerator.class)
    public static native int getLength(TObject array) throws TIllegalArgumentException;
}
