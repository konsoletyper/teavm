/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.runtime.heap;

import org.teavm.interop.Address;
import org.teavm.interop.StaticInit;
import org.teavm.interop.Structure;
import org.teavm.interop.Unmanaged;

@Unmanaged
@StaticInit
class HeapRecord extends Structure {
    static final int ALLOCATED = 1;

    int size;
    int previousSize;

    static int size(HeapRecord record) {
        return record.size & ~ALLOCATED;
    }

    static boolean isAllocated(HeapRecord record) {
        return (record.size & ALLOCATED) != 0;
    }

    static HeapRecord next(HeapRecord record) {
        return dataOf(record).add(HeapRecord.size(record)).toStructure();
    }

    static HeapRecord next(HeapRecord record, int size) {
        return dataOf(record).add(size).toStructure();
    }

    static HeapRecord previous(HeapRecord record) {
        return recordOf(record.toAddress().add(-record.previousSize));
    }

    static Address dataOf(HeapRecord record) {
        return record.toAddress().add(Structure.sizeOf(HeapRecord.class));
    }

    static HeapRecord recordOf(Address address) {
        return address.add(-Structure.sizeOf(HeapRecord.class)).toStructure();
    }
}
