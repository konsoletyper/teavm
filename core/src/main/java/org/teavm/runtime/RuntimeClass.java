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
import org.teavm.interop.Unmanaged;

public class RuntimeClass extends RuntimeJavaObject {
    public static final int INITIALIZED = 1;
    public static final int PRIMITIVE = 2;

    public int size;
    public int flags;
    public int tag;
    public int canary;
    public RuntimeClass itemType;
    public RuntimeClass arrayType;
    public IsSupertypeFunction isSupertypeOf;
    public RuntimeClass parent;
    public Address layout;

    @Unmanaged
    public static int computeCanary(int size, int tag) {
        return size ^ (tag << 8) ^ (tag >>> 24) ^ 0xAAAAAAAA;
    }

    @Unmanaged
    public int computeCanary() {
        return computeCanary(size, tag);
    }

    @Unmanaged
    public static RuntimeClass getClass(RuntimeObject object) {
        return Address.fromInt(object.classReference << 3).toStructure();
    }
}
