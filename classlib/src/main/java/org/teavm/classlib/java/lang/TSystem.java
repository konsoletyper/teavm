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

import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.classlib.java.io.TConsole;
import org.teavm.classlib.java.io.TInputStream;
import org.teavm.classlib.java.io.TPrintStream;
import org.teavm.classlib.java.lang.reflect.TArray;
import org.teavm.dependency.PluggableDependency;
import org.teavm.interop.Address;
import org.teavm.interop.DelegateTo;
import org.teavm.interop.Import;
import org.teavm.interop.Unmanaged;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.GC;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;

public final class TSystem extends TObject {
    public static final TPrintStream out = new TPrintStream(new TConsoleOutputStreamStdout(), false);
    public static final TPrintStream err = new TPrintStream(new TConsoleOutputStreamStderr(), false);
    public static final TInputStream in = new TConsoleInputStream();

    private TSystem() {
    }

    public static TConsole console() {
        return null;
    }

    public static void arraycopy(TObject src, int srcPos, TObject dest, int destPos, int length) {
        if (src == null || dest == null) {
            throw new TNullPointerException(TString.wrap("Either src or dest is null"));
        }
        if (srcPos < 0 || destPos < 0 || length < 0 || srcPos + length > TArray.getLength(src)
                || destPos + length > TArray.getLength(dest)) {
            throw new TIndexOutOfBoundsException();
        }
        if (src != dest) {
            Class<?> srcType = src.getClass().getComponentType();
            Class<?> targetType = dest.getClass().getComponentType();
            if (srcType == null || targetType == null) {
                throw new TArrayStoreException();
            }
            if (srcType != targetType) {
                if (!srcType.isPrimitive() && !targetType.isPrimitive()) {
                    Object[] srcArray = (Object[]) (Object) src;
                    int pos = srcPos;
                    for (int i = 0; i < length; ++i) {
                        Object elem = srcArray[pos++];
                        if (!targetType.isInstance(elem)) {
                            doArrayCopy(src, srcPos, dest, destPos, i);
                            throw new TArrayStoreException();
                        }
                    }
                    doArrayCopy(src, srcPos, dest, destPos, length);
                    return;
                } else if (!srcType.isPrimitive() || !targetType.isPrimitive()) {
                    throw new TArrayStoreException();
                }
            }
        }
        doArrayCopy(src, srcPos, dest, destPos, length);
    }

    @GeneratedBy(SystemNativeGenerator.class)
    @DelegateTo("doArrayCopyLowLevel")
    private static native void doArrayCopy(Object src, int srcPos, Object dest, int destPos, int length);

    @Unmanaged
    static void doArrayCopyLowLevel(RuntimeArray src, int srcPos, RuntimeArray dest, int destPos, int length) {
        RuntimeClass type = RuntimeClass.getClass(src);
        int itemSize = type.itemType.size;
        if ((type.itemType.flags & RuntimeClass.PRIMITIVE) == 0) {
            itemSize = Address.sizeOf();
        }

        Address srcAddress = Address.align(src.toAddress().add(RuntimeArray.class, 1), itemSize);
        srcAddress = srcAddress.add(itemSize * srcPos);

        Address destAddress = Address.align(dest.toAddress().add(RuntimeArray.class, 1), itemSize);
        destAddress = destAddress.add(itemSize * destPos);

        Allocator.moveMemoryBlock(srcAddress, destAddress, length * itemSize);
    }

    @GeneratedBy(SystemNativeGenerator.class)
    @DelegateTo("currentTimeMillisLowLevel")
    public static native long currentTimeMillis();

    private static long currentTimeMillisLowLevel() {
        return (long) currentTimeMillisImpl();
    }

    @Import(name = "currentTimeMillis", module = "runtime")
    private static native double currentTimeMillisImpl();

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

    @DelegateTo("gcLowLevel")
    public static void gc() {
        // Do nothing
    }

    private static void gcLowLevel() {
        GC.collectGarbage(0);
    }

    public static void runFinalization() {
        // Do nothing
    }

    public static long nanoTime() {
        return currentTimeMillis() * 1000000;
    }

    public static int identityHashCode(Object x) {
        return ((TObject) x).identity();
    }

    public static TString lineSeparator() {
        return TString.wrap("\n");
    }
}
