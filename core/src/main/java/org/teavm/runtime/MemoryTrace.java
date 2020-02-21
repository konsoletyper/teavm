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
import org.teavm.interop.Import;

public class MemoryTrace {
    private MemoryTrace() {
    }

    @Import(name = "allocate", module = "teavmHeapTrace")
    public static native void allocate(Address address, int size);

    @Import(name = "free", module = "teavmHeapTrace")
    public static native void free(Address address, int size);

    @Import(name = "assertFree", module = "teavmHeapTrace")
    public static native void assertFree(Address address, int size);

    @Import(name = "checkIsFree", module = "teavmHeapTrace")
    public static native void checkIsFree(Address address, int size);

    @Import(name = "markStarted", module = "teavmHeapTrace")
    public static native void markStarted();

    @Import(name = "mark", module = "teavmHeapTrace")
    public static native void mark(Address address);

    @Import(name = "reportDirtyRegion", module = "teavmHeapTrace")
    public static native void reportDirtyRegion(Address address);

    @Import(name = "markCompleted", module = "teavmHeapTrace")
    public static native void markCompleted();

    @Import(name = "move", module = "teavmHeapTrace")
    public static native void move(Address from, Address to, int size);

    @Import(name = "gcStarted", module = "teavmHeapTrace")
    public static native void gcStarted(boolean full);

    @Import(name = "sweepStarted", module = "teavmHeapTrace")
    public static native void sweepStarted();

    @Import(name = "sweepCompleted", module = "teavmHeapTrace")
    public static native void sweepCompleted();

    @Import(name = "defragStarted", module = "teavmHeapTrace")
    public static native void defragStarted();

    @Import(name = "defragCompleted", module = "teavmHeapTrace")
    public static native void defragCompleted();

    @Import(name = "writeHeapDump", module = "teavmHeapTrace")
    public static native void writeHeapDump();

    @Import(name = "gcCompleted", module = "teavmHeapTrace")
    public static native void gcCompleted();
}
