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
package org.teavm.backend.wasm.gc;

import java.util.Arrays;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.generate.gc.strings.WasmGCStringPool;
import org.teavm.backend.wasm.runtime.WasmGCSupport;
import org.teavm.dependency.DependencyAnalyzer;
import org.teavm.model.MethodReference;

public class WasmGCDependencies {
    private DependencyAnalyzer analyzer;

    public WasmGCDependencies(DependencyAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public void contribute() {
        contributeMathUtils();
        contributeExceptionUtils();
        contributeInitializerUtils();
    }

    private void contributeMathUtils() {
        for (var type : Arrays.asList(int.class, long.class, float.class, double.class)) {
            var method = new MethodReference(WasmRuntime.class, "compare", type, type, int.class);
            analyzer.linkMethod(method).use();
        }
        for (var type : Arrays.asList(int.class, long.class)) {
            var method = new MethodReference(WasmRuntime.class, "compareUnsigned", type, type, int.class);
            analyzer.linkMethod(method).use();
        }

        for (var type : Arrays.asList(float.class, double.class)) {
            var method = new MethodReference(WasmRuntime.class, "remainder", type, type, type);
            analyzer.linkMethod(method).use();
        }
    }

    private void contributeExceptionUtils() {
        analyzer.linkMethod(new MethodReference(WasmGCSupport.class, "npe", NullPointerException.class));
        analyzer.linkMethod(new MethodReference(WasmGCSupport.class, "aiiobe", ArrayIndexOutOfBoundsException.class));
        analyzer.linkMethod(new MethodReference(WasmGCSupport.class, "cce", ClassCastException.class));
    }

    private void contributeInitializerUtils() {
        analyzer.linkMethod(new MethodReference(WasmGCStringPool.class, "nextCharArray", char[].class));
    }
}
