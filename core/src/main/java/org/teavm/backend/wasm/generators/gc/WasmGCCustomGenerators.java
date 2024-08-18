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
package org.teavm.backend.wasm.generators.gc;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import org.teavm.backend.wasm.generate.gc.methods.WasmGCCustomGeneratorProvider;
import org.teavm.backend.wasm.runtime.WasmGCSupport;
import org.teavm.model.MethodReference;

public class WasmGCCustomGenerators implements WasmGCCustomGeneratorProvider {
    private Map<MethodReference, WasmGCCustomGenerator> generators = new HashMap<>();

    public WasmGCCustomGenerators() {
        fillClass();
        fillStringPool();
        fillSystem();
        fillArray();
    }

    private void fillClass() {
        var classGenerators = new ClassGenerators();
        generators.put(new MethodReference(Class.class, "isInstance", Object.class, boolean.class), classGenerators);
    }

    private void fillStringPool() {
        generators.put(
                new MethodReference(WasmGCSupport.class, "nextByte", byte.class),
                new WasmGCStringPoolGenerator()
        );
    }

    private void fillSystem() {
        generators.put(
                new MethodReference(System.class, "doArrayCopy", Object.class, int.class, Object.class,
                        int.class, int.class, void.class),
                new SystemDoArrayCopyGenerator()
        );
    }

    private void fillArray() {
        var arrayGenerator = new ArrayGenerator();
        generators.put(new MethodReference(Array.class, "newInstanceImpl", Class.class, int.class, Object.class),
                arrayGenerator);
    }

    @Override
    public WasmGCCustomGenerator get(MethodReference method) {
        return generators.get(method);
    }
}
