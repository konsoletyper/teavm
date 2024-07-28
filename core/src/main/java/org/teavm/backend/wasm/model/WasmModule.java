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
package org.teavm.backend.wasm.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.teavm.common.GraphUtils;

public class WasmModule {
    private int minMemorySize;
    private int maxMemorySize;
    private List<WasmMemorySegment> segments = new ArrayList<>();
    private List<WasmFunction> functionTable = new ArrayList<>();
    private WasmFunction startFunction;
    private Map<String, WasmCustomSection> customSections = new LinkedHashMap<>();
    private Map<String, WasmCustomSection> readonlyCustomSections = Collections.unmodifiableMap(customSections);

    public final WasmCollection<WasmFunction> functions = new WasmCollection<>();
    public final WasmCollection<WasmGlobal> globals = new WasmCollection<>();
    public final WasmCollection<WasmCompositeType> types = new WasmCollection<>();
    public final WasmCollection<WasmTag> tags = new WasmCollection<>();

    public void add(WasmCustomSection customSection) {
        if (customSections.containsKey(customSection.getName())) {
            throw new IllegalArgumentException("Custom section " + customSection.getName()
                    + " already defined in this module");
        }
        if (customSection.module != null) {
            throw new IllegalArgumentException("Given custom section is already registered in another module");
        }
        customSections.put(customSection.getName(), customSection);
        customSection.module = this;
    }

    public void remove(WasmCustomSection customSection) {
        if (customSection.module != this) {
            return;
        }
        customSection.module = null;
        customSections.remove(customSection.getName());
    }

    public Map<? extends String, ? extends WasmCustomSection> getCustomSections() {
        return readonlyCustomSections;
    }

    public List<WasmFunction> getFunctionTable() {
        return functionTable;
    }

    public List<WasmMemorySegment> getSegments() {
        return segments;
    }

    public int getMinMemorySize() {
        return minMemorySize;
    }

    public void setMinMemorySize(int minMemorySize) {
        this.minMemorySize = minMemorySize;
    }

    public int getMaxMemorySize() {
        return maxMemorySize;
    }

    public void setMaxMemorySize(int maxMemorySize) {
        this.maxMemorySize = maxMemorySize;
    }

    public WasmFunction getStartFunction() {
        return startFunction;
    }

    public void setStartFunction(WasmFunction startFunction) {
        this.startFunction = startFunction;
    }

    public void prepareForRendering() {
        prepareRecursiveTypes();
    }

    private void prepareRecursiveTypes() {
        var typeGraph = WasmTypeGraphBuilder.buildTypeGraph(types, types.size());
        var newList = new ArrayList<WasmCompositeType>();
        var typesInScc = new boolean[types.size()];
        for (var scc : GraphUtils.findStronglyConnectedComponents(typeGraph)) {
            var firstType = types.get(scc[0]);
            firstType.recursiveTypeCount = scc.length;
            for (var i = 0; i < scc.length; i++) {
                var index = scc[i];
                var type = types.get(index);
                newList.add(type);
                type.indexInRecursiveType = i;
                typesInScc[index] = true;
            }
        }
        for (var type : types) {
            if (!typesInScc[type.index]) {
                newList.add(type);
            }
        }
        types.clear();
        for (var type : newList) {
            types.add(type);
        }
    }
}
