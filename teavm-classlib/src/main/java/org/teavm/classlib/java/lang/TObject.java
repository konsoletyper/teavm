package org.teavm.classlib.java.lang;

import org.teavm.javascript.ni.GeneratedBy;
import org.teavm.javascript.ni.Rename;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TObject {
    public TObject() {
        init();
    }

    @GeneratedBy(TObjectNativeGenerator.class)
    private native void init();

    @GeneratedBy(TObjectNativeGenerator.class)
    @Rename("getClass")
    public native final TClass<?> getClass0();

    @Override
    @GeneratedBy(TObjectNativeGenerator.class)
    public native int hashCode();

    @GeneratedBy(TObjectNativeGenerator.class)
    public native boolean equals(TObject other);

    @Override
    @GeneratedBy(TObjectNativeGenerator.class)
    protected native TObject clone();

    @Rename("notify")
    public final void notify0() {
    }

    @Rename("notifyAll")
    public final void notifyAll0() {
    }

    @SuppressWarnings("unused")
    @Rename("wait")
    public final void wait0(long timeout) throws TInterruptedException {
    }

    @SuppressWarnings("unused")
    @Rename("wait")
    public final void wait0(long timeout, int nanos) throws TInterruptedException {
    }

    @SuppressWarnings("unused")
    @Rename("wait")
    public final void wait0() throws TInterruptedException {
    }

    @Override
    protected void finalize() throws TThrowable {
    }
}
