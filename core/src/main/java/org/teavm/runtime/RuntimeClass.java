/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.runtime;

import org.teavm.interop.Address;
import org.teavm.interop.Structure;
import org.teavm.interop.Unmanaged;
import org.teavm.interop.c.Name;
import org.teavm.interop.c.Native;

@Native
@Name("TeaVM_Class")
public class RuntimeClass extends Structure {
    public static final int INITIALIZED = 1;
    public static final int VM_TYPE_SHIFT = 1;
    public static final int VM_TYPE_MASK = 7;
    public static final int PRIMITIVE_TYPE_SHIFT = 4;
    public static final int PRIMITIVE_TYPE_MASK = 15;

    public static final int VM_TYPE_REGULAR = 0;
    public static final int VM_TYPE_WEAKREFERENCE = 1;
    public static final int VM_TYPE_REFERENCEQUEUE = 2;
    public static final int VM_TYPE_BUFFER = 3;

    public RuntimeObject classObject;
    public RuntimeClass next;
    public int size;
    public int flags;
    public int tag;
    public int modifiers;
    public RuntimeObjectPtr name;
    public RuntimeObjectPtr simpleName;

    public RuntimeClass itemType;
    public RuntimeClass arrayType;
    public RuntimeClass declaringClass;
    public RuntimeClass enclosingClass;
    public IsSupertypeFunction isSupertypeOf;
    public InitFunction init;
    public RuntimeClass superclass;
    public int superinterfaceCount;
    public RuntimeClassPointer superinterfaces;
    public Address enumValues;
    public Address layout;

    @Unmanaged
    public static RuntimeClass getClass(RuntimeObject object) {
        return unpack(object.classReference);
    }

    @Unmanaged
    public static native RuntimeClass unpack(int n);

    @Unmanaged
    public final native int pack();

    @Unmanaged
    public static boolean isPrimitive(RuntimeClass cls) {
        return ((cls.flags >> RuntimeClass.PRIMITIVE_TYPE_SHIFT) & RuntimeClass.PRIMITIVE_TYPE_MASK) != 0;
    }

    @Unmanaged
    public static native RuntimeClass first();
}
