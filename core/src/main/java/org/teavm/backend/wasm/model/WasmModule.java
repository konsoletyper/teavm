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

public class WasmModule {
    private int minMemorySize;
    private int maxMemorySize;
    private List<WasmMemorySegment> segments = new ArrayList<>();
    private Map<String, WasmFunction> functions = new LinkedHashMap<>();
    private Map<String, WasmFunction> readonlyFunctions = Collections.unmodifiableMap(functions);
    private List<WasmFunction> functionTable = new ArrayList<>();
    private WasmFunction startFunction;

    public void add(WasmFunction function) {
        if (functions.containsKey(function.getName())) {
            throw new IllegalArgumentException("Function " + function.getName() + " already defined in this module");
        }
        if (function.module != null) {
            throw new IllegalArgumentException("Given function is already registered in another module");
        }
        functions.put(function.getName(), function);
        function.module = this;
    }

    public void remove(WasmFunction function) {
        if (function.getModule() != this) {
            return;
        }
        function.module = null;
        functions.remove(function.getName());
    }

    public Map<String, WasmFunction> getFunctions() {
        return readonlyFunctions;
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
}
