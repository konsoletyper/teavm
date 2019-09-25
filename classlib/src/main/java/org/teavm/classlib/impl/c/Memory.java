/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.classlib.impl.c;

import org.teavm.interop.Address;
import org.teavm.interop.Import;
import org.teavm.interop.Unmanaged;
import org.teavm.interop.c.Include;

public final class Memory {
    private Memory() {
    }

    @Include("stdlib.h")
    @Import(name = "malloc")
    @Unmanaged
    public static native Address malloc(int size);

    @Include("stdlib.h")
    @Import(name = "free")
    @Unmanaged
    public static native void free(Address address);

    @Include("string.h")
    @Import(name = "memcpy")
    @Unmanaged
    public static native void memcpy(Address target, Address source, int size);
}
