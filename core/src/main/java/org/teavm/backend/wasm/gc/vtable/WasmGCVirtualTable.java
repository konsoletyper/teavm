/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.gc.vtable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

public class WasmGCVirtualTable {
    private WasmGCVirtualTable parent;
    private String className;
    List<WasmGCVirtualTableEntry> entries;
    MethodReference[] implementors;
    private Map<MethodDescriptor, WasmGCVirtualTableEntry> entryMap;

    WasmGCVirtualTable(WasmGCVirtualTable parent, String className) {
        this.parent = parent;
        this.className = className;
    }

    public WasmGCVirtualTable getParent() {
        return parent;
    }

    public String getClassName() {
        return className;
    }

    public List<? extends WasmGCVirtualTableEntry> getEntries() {
        return entries;
    }

    public MethodReference implementor(WasmGCVirtualTableEntry entry) {
        return implementors[entry.getIndex()];
    }

    public WasmGCVirtualTableEntry entry(MethodDescriptor method) {
        if (entryMap == null) {
            entryMap = new HashMap<>();
            for (var entry : entries) {
                entryMap.put(entry.getMethod(), entry);
            }
        }
        return entryMap.get(method);
    }
}
