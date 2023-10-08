/*
 *  Copyright 2023 konsoletyper.
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
package org.teavm.classlib.impl;

import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.interop.Address;
import org.teavm.interop.Structure;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.GC;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeObject;

final class ServiceLoadWasmRuntime {
    private ServiceLoadWasmRuntime() {
    }

    @SuppressWarnings("unused")
    static RuntimeObject createServices(Address table, RuntimeClass cls) {
        var entry = WasmRuntime.lookupResource(table, cls.toAddress());
        if (entry == null) {
            return null;
        }
        entry = entry.add(Address.sizeOf()).getAddress();
        var size = entry.getInt();
        entry = entry.add(4);
        RuntimeArray result = Allocator.allocateArray(cls, size).toStructure();
        var resultData = WasmRuntime.align(result.toAddress().add(Structure.sizeOf(RuntimeArray.class)),
                Address.sizeOf());
        for (var i = 0; i < size; ++i) {
            RuntimeObject obj = Allocator.allocate(entry.getAddress().toStructure()).toStructure();
            entry = entry.add(Address.sizeOf());
            WasmRuntime.callFunctionFromTable(entry.getInt(), obj);
            entry = entry.add(4);
            resultData.putAddress(obj.toAddress());
            resultData = resultData.add(Address.sizeOf());
            GC.writeBarrier(result);
        }
        return result;
    }
}
