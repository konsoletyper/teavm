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
    public native int modifiers();

    @Unmanaged
    public native int primitiveKind();

    @Unmanaged
    public native StringInfo name();

    @Unmanaged
    public native StringInfo simpleName();

    @Unmanaged
    public native ClassInfo itemType();

    @Unmanaged
    public native ClassInfo arrayType();

    @Unmanaged
    public native ClassInfo declaringClass();

    @Unmanaged
    public native ClassInfo enclosingClass();

    @Unmanaged
    public native boolean isSuperTypeOf(ClassInfo subtype);

    @Unmanaged
    public native ClassInfo parent();

    @Unmanaged
    public native int superinterfaceCount();

    @Unmanaged
    public native ClassInfo superinterface(int index);

    @Unmanaged
    public native int enumConstantCount();

    @Unmanaged
    public native Object enumConstant(int index);
    
    public native Object newArrayInstance(int index);

    @Unmanaged
    public native void putItem(Object array, int index, Object value);

    public native Object getItem(Object array, int index);

    @Unmanaged
    public native int arrayLength(Object array);

    public native Object newInstance();

    public native boolean initializeNewInstance(Object instance);

    public native void initialize();

    @Unmanaged
    public native ClassReflectionInfo reflection();

    @Unmanaged
    public static native void rewind();

    @Unmanaged
    public static native ClassInfo next();

    @Unmanaged
    public static native boolean hasNext();

    public native Class<?> classObject();
}
