package org.teavm.classlib.java.lang;

import org.teavm.javascript.ni.GeneratedBy;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TClass<T> extends TObject {
    static TClass<?> createNew() {
        return new TClass<>();
    }

    @GeneratedBy(ClassNativeGenerator.class)
    public native boolean isInstance(TObject obj);
}
