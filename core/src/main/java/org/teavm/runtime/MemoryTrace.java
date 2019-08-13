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
package org.teavm.runtime;

import org.teavm.interop.Address;

public class MemoryTrace {
    private MemoryTrace() {
    }

    public static native void allocate(Address address, int size);

    public static native void free(Address address, int size);

    public static native void assertFree(Address address, int size);

    public static native void checkIsFree(Address address, int size);

    public static native void initMark();

    public static native void mark(Address address);

    public static native void move(Address from, Address to, int size);

    public static native void gcStarted();

    public static native void sweepCompleted();

    public static native void defragCompleted();

    public static native void writeHeapDump();
}
