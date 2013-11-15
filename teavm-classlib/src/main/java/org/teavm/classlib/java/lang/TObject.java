package org.teavm.classlib.java.lang;

import org.teavm.javascript.ni.GeneratedBy;
import org.teavm.javascript.ni.Rename;
import org.teavm.javascript.ni.Superclass;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
@Superclass("")
public class TObject {
    @Rename("fakeInit")
    public TObject() {
    }

    @GeneratedBy(ObjectNativeGenerator.class)
    @Rename("<init>")
    private native void init();

    @GeneratedBy(ObjectNativeGenerator.class)
    @Rename("getClass")
    public native final TClass<?> getClass0();

    @Override
    @GeneratedBy(ObjectNativeGenerator.class)
    public native int hashCode();

    @GeneratedBy(ObjectNativeGenerator.class)
    public native boolean equals(TObject other);

    @Override
    @GeneratedBy(ObjectNativeGenerator.class)
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

    @Rename("wait")
    public final void wait0() throws TInterruptedException {
    }

    @Override
    protected void finalize() throws TThrowable {
    }
}
