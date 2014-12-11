/*
 *  Copyright 2013 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.java.lang;

import org.teavm.dependency.PluggableDependency;
import org.teavm.javascript.ni.GeneratedBy;
import org.teavm.javascript.ni.InjectedBy;
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

    @InjectedBy(ObjectNativeGenerator.class)
    @Rename("getClass")
    @PluggableDependency(ObjectNativeGenerator.class)
    public native final TClass<?> getClass0();

    @Override
    @GeneratedBy(ObjectNativeGenerator.class)
    public native int hashCode();

    @Rename("equals")
    public boolean equals0(TObject other) {
        return this == other;
    }

    @Rename("toString")
    public TString toString0() {
        return TString.wrap(getClass().getName() + "@" + identity());
    }

    @GeneratedBy(ObjectNativeGenerator.class)
    native int identity();

    @GeneratedBy(ObjectNativeGenerator.class)
    @PluggableDependency(ObjectNativeGenerator.class)
    @Override
    protected native Object clone() throws TCloneNotSupportedException;

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

    @InjectedBy(ObjectNativeGenerator.class)
    @PluggableDependency(ObjectNativeGenerator.class)
    public static native TObject wrap(Object obj);
}
