/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.runtime.reflect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import org.teavm.interop.Intrinsified;
import org.teavm.interop.Unmanaged;
import org.teavm.runtime.StringInfo;

public final class ClassInfo extends ReflectionInfo {
    public static final class PrimitiveKind {
        public static final int NOT = 0;
        public static final int BOOLEAN = 1;
        public static final int BYTE = 2;
        public static final int SHORT = 3;
        public static final int CHAR = 4;
        public static final int INT = 5;
        public static final int LONG = 6;
        public static final int FLOAT = 7;
        public static final int DOUBLE = 8;
        public static final int VOID = 9;
    }

    @Unmanaged
    @Intrinsified
    public native int modifiers();

    @Unmanaged
    @Intrinsified
    public native int primitiveKind();

    @Unmanaged
    @Intrinsified
    public native StringInfo name();

    @Unmanaged
    @Intrinsified
    public native StringInfo simpleName();

    @Unmanaged
    @Intrinsified
    public native ClassInfo itemType();

    @Unmanaged
    @Intrinsified
    public native ClassInfo arrayType();

    @Unmanaged
    @Intrinsified
    public native ClassInfo declaringClass();

    @Unmanaged
    @Intrinsified
    public native ClassInfo enclosingClass();

    @Unmanaged
    @Intrinsified
    public native boolean isSuperTypeOf(ClassInfo subtype);

    @Unmanaged
    @Intrinsified
    public native ClassInfo parent();

    @Unmanaged
    @Intrinsified
    public native int superinterfaceCount();

    @Unmanaged
    @Intrinsified
    public native ClassInfo superinterface(int index);

    @Unmanaged
    @Intrinsified
    public native int enumConstantCount();

    @Unmanaged
    @Intrinsified
    public native Object enumConstant(int index);

    @Intrinsified
    public native Object newArrayInstance(int index);

    @Unmanaged
    @Intrinsified
    public native void putItem(Object array, int index, Object value);

    @Intrinsified
    public native Object getItem(Object array, int index);

    @Unmanaged
    @Intrinsified
    public native int arrayLength(Object array);

    @Intrinsified
    public native Object newInstance();

    @Intrinsified
    public native Proxy newProxyInstance(InvocationHandler handler);

    @Intrinsified
    public native boolean initializeNewInstance(Object instance);

    @Intrinsified
    public native void initialize();

    @Unmanaged
    @Intrinsified
    public native ClassReflectionInfo reflection();

    @Unmanaged
    @Intrinsified
    public static native void rewind();

    @Unmanaged
    @Intrinsified
    public static native ClassInfo next();

    @Unmanaged
    @Intrinsified
    public static native boolean hasNext();

    @Intrinsified
    public native Class<?> classObject();
}
