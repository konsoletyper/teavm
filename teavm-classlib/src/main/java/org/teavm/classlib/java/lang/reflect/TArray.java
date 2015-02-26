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

import org.teavm.classlib.java.lang.*;
import org.teavm.dependency.PluggableDependency;
import org.teavm.javascript.spi.GeneratedBy;
import org.teavm.platform.PlatformClass;

/**
 *
 * @author Alexey Andreev
 */
public final class TArray extends TObject {
    @GeneratedBy(ArrayNativeGenerator.class)
    @PluggableDependency(ArrayNativeGenerator.class)
    public static native int getLength(TObject array) throws TIllegalArgumentException;

    public static TObject newInstance(TClass<?> componentType, int length) throws TNegativeArraySizeException {
        if (componentType == null) {
            throw new TNullPointerException();
        }
        if (componentType == TClass.wrap(void.class)) {
            throw new TIllegalArgumentException();
        }
        if (length < 0) {
            throw new TNegativeArraySizeException();
        }
        return newInstanceImpl(componentType.getPlatformClass(), length);
    }

    @GeneratedBy(ArrayNativeGenerator.class)
    @PluggableDependency(ArrayNativeGenerator.class)
    private static native TObject newInstanceImpl(PlatformClass componentType, int length);

    public static TObject get(TObject array, int index) throws TIllegalArgumentException,
            TArrayIndexOutOfBoundsException {
        if (index < 0 || index >= getLength(array)) {
            throw new TArrayIndexOutOfBoundsException();
        }
        return getImpl(array, index);
    }

    @GeneratedBy(ArrayNativeGenerator.class)
    @PluggableDependency(ArrayNativeGenerator.class)
    private static native TObject getImpl(TObject array, int index);
}
