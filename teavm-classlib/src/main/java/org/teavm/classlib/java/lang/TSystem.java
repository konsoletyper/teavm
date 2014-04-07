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

import org.teavm.classlib.java.io.TPrintStream;
import org.teavm.classlib.java.lang.reflect.TArray;
import org.teavm.dependency.PluggableDependency;
import org.teavm.javascript.ni.GeneratedBy;

/**
 *
 * @author Alexey Andreev
 */
public final class TSystem extends TObject {
    public static final TPrintStream out = new TPrintStream(new TConsoleOutputStream_stdout(), false);
    public static final TPrintStream err = new TPrintStream(new TConsoleOutputStream_stderr(), false);

    private TSystem() {
    }

    public static void arraycopy(TObject src, int srcPos, TObject dest, int destPos, int length) {
        if (src == null || dest == null) {
            throw new TNullPointerException(TString.wrap("Either src or dest is null"));
        }
        if (src.getClass() != dest.getClass()) {
            throw new TArrayStoreException();
        }
        if (srcPos < 0 || destPos < 0 || length < 0 || srcPos + length > TArray.getLength(src) ||
                destPos + length > TArray.getLength(dest)) {
            throw new TIndexOutOfBoundsException();
        }
        doArrayCopy(src, srcPos, dest, destPos, length);
    }

    @GeneratedBy(SystemNativeGenerator.class)
    private static native void doArrayCopy(Object src, int srcPos, Object dest, int destPos, int length);

    @GeneratedBy(SystemNativeGenerator.class)
    public static native long currentTimeMillis();

    public static TString getProperty(@SuppressWarnings("unused") TString key) {
        // TODO: make implementation
        return null;
    }

    public static TString getProperty(TString key, TString def) {
        TString value = getProperty(key);
        return value != null ? value : def;
    }

    @GeneratedBy(SystemNativeGenerator.class)
    @PluggableDependency(SystemNativeGenerator.class)
    public static native void setErr(TPrintStream err);

    @GeneratedBy(SystemNativeGenerator.class)
    @PluggableDependency(SystemNativeGenerator.class)
    public static native void setOut(TPrintStream err);

    public static void gc() {
        // Do nothing
    }

    public static void runFinalization() {
        // Do nothing
    }

    public static long nanoTime() {
        return currentTimeMillis() * 10000000;
    }

    @GeneratedBy(SystemNativeGenerator.class)
    @PluggableDependency(SystemNativeGenerator.class)
    public static native int identityHashCode(Object x);

    public static TString lineSeparator() {
        return TString.wrap("\n");
    }
}
