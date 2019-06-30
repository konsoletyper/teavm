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
package org.teavm.classlib.java.lang.reflect;

import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.classlib.java.lang.TArrayIndexOutOfBoundsException;
import org.teavm.classlib.java.lang.TClass;
import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.TNegativeArraySizeException;
import org.teavm.classlib.java.lang.TNullPointerException;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.dependency.PluggableDependency;
import org.teavm.interop.DelegateTo;
import org.teavm.interop.NoSideEffects;
import org.teavm.platform.PlatformClass;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeObject;

public final class TArray extends TObject {
    @GeneratedBy(ArrayNativeGenerator.class)
    @PluggableDependency(ArrayNativeGenerator.class)
    @DelegateTo("getLengthLowLevel")
    @NoSideEffects
    public static native int getLength(TObject array) throws TIllegalArgumentException;

    @SuppressWarnings("unused")
    private static int getLengthLowLevel(RuntimeObject obj) {
        RuntimeClass cls = RuntimeClass.getClass(obj);
        if (cls.itemType == null) {
            throw new TIllegalArgumentException();
        }
        RuntimeArray array = (RuntimeArray) obj;
        return array.size;
    }

    @PluggableDependency(ArrayNativeGenerator.class)
    public static TObject newInstance(Class<?> componentType, int length) throws TNegativeArraySizeException {
        if (componentType == null) {
            throw new TNullPointerException();
        }
        if (componentType == void.class) {
            throw new TIllegalArgumentException();
        }
        if (length < 0) {
            throw new TNegativeArraySizeException();
        }
        return newInstanceImpl(((TClass<?>) (Object) componentType).getPlatformClass(), length);
    }

    @GeneratedBy(ArrayNativeGenerator.class)
    @DelegateTo("newInstanceLowLevel")
    @NoSideEffects
    private static native TObject newInstanceImpl(PlatformClass componentType, int length);

    @SuppressWarnings("unused")
    private static RuntimeObject newInstanceLowLevel(RuntimeClass cls, int length) {
        return Allocator.allocateArray(cls.arrayType, length).toStructure();
    }

    public static TObject get(TObject array, int index) throws TIllegalArgumentException,
            TArrayIndexOutOfBoundsException {
        if (index < 0 || index >= getLength(array)) {
            throw new TArrayIndexOutOfBoundsException();
        }
        return getImpl(array, index);
    }

    public static void set(TObject array, int index, TObject value) throws TIllegalArgumentException,
            TArrayIndexOutOfBoundsException {
        if (index < 0 || index >= getLength(array)) {
            throw new TArrayIndexOutOfBoundsException();
        }
        setImpl(array, index, value);
    }

    @GeneratedBy(ArrayNativeGenerator.class)
    @PluggableDependency(ArrayNativeGenerator.class)
    @NoSideEffects
    private static native TObject getImpl(TObject array, int index);

    @GeneratedBy(ArrayNativeGenerator.class)
    @PluggableDependency(ArrayNativeGenerator.class)
    @NoSideEffects
    private static native void setImpl(TObject array, int index, TObject value);
}
