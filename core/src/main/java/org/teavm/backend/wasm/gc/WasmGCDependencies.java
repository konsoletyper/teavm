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
import java.util.List;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.runtime.gc.WasmGCSupport;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyAnalyzer;
import org.teavm.interop.Address;
import org.teavm.model.MethodReference;
import org.teavm.runtime.heap.Heap;

public class WasmGCDependencies {
    private DependencyAnalyzer analyzer;

    public WasmGCDependencies(DependencyAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public void contribute() {
        analyzer.linkClass("java.lang.Class");
        contributeWasmRuntime();
        contributeMathUtils();
        contributeExceptionUtils();
        contributeInitializerUtils();
        contributeString();
        contributeBuffers();
        analyzer.addDependencyListener(new WasmGCReferenceQueueDependency());
        analyzer.addDependencyListener(new WasmGCResourceDependency());
        analyzer.addDependencyListener(new SystemArrayCopyDependencySupport());
    }

    public void contributeStandardExports() {
        analyzer.linkMethod(new MethodReference(WasmGCSupport.class, "createStringArray", int.class, String[].class))
                .use();
        analyzer.linkMethod(new MethodReference(WasmGCSupport.class, "createStringBuilder", StringBuilder.class))
                .use();
        analyzer.linkMethod(new MethodReference(WasmGCSupport.class, "setToStringArray", String[].class,
                        int.class, String.class, void.class))
                .propagate(1, analyzer.getType("[java/lang/String;"))
                .propagate(3, analyzer.getType("java.lang.String"))
                .use();
        analyzer.linkMethod(new MethodReference(StringBuilder.class, "append", char.class, StringBuilder.class))
                .propagate(0, analyzer.getType("java.lang.StringBuilder"))
                .use();
        analyzer.linkMethod(new MethodReference(StringBuilder.class, "toString", String.class))
                .propagate(0, analyzer.getType("java.lang.StringBuilder"))
                .use();
        analyzer.linkMethod(new MethodReference(String.class, "length", int.class))
                .propagate(0, analyzer.getType("java.lang.String"))
                .use();
        analyzer.linkMethod(new MethodReference(String.class, "charAt", int.class, char.class))
                .propagate(0, analyzer.getType("java.lang.String"))
                .use();
    }

    private void contributeWasmRuntime() {
        for (var cls : List.of(int.class, long.class, float.class, double.class)) {
            analyzer.linkMethod(new MethodReference(WasmRuntime.class, "lt", cls, cls, boolean.class)).use();
            analyzer.linkMethod(new MethodReference(WasmRuntime.class, "gt", cls, cls, boolean.class)).use();
        }
        for (var cls : List.of(int.class, long.class)) {
            analyzer.linkMethod(new MethodReference(WasmRuntime.class, "ltu", cls, cls, boolean.class)).use();
            analyzer.linkMethod(new MethodReference(WasmRuntime.class, "gtu", cls, cls, boolean.class)).use();
        }
        for (var cls : List.of(float.class, double.class)) {
            analyzer.linkMethod(new MethodReference(WasmRuntime.class, "min", cls, cls, cls)).use();
            analyzer.linkMethod(new MethodReference(WasmRuntime.class, "max", cls, cls, cls)).use();
        }
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
        analyzer.linkMethod(new MethodReference(WasmGCSupport.class, "npe", NullPointerException.class))
                .use();
        analyzer.linkMethod(new MethodReference(WasmGCSupport.class, "aiiobe", ArrayIndexOutOfBoundsException.class))
                .use();
        analyzer.linkMethod(new MethodReference(WasmGCSupport.class, "cce", ClassCastException.class)).use();
        analyzer.linkMethod(new MethodReference(WasmGCSupport.class, "throwCloneNotSupportedException",
                void.class)).use();
        analyzer.linkMethod(new MethodReference(NullPointerException.class, "<init>", void.class)).use();

        analyzer.addDependencyListener(new AbstractDependencyListener() {
            @Override
            public void classReached(DependencyAgent agent, String className) {
                if (className.equals(Throwable.class.getName())) {
                    agent.linkMethod(new MethodReference(Throwable.class, "getMessage", String.class))
                            .propagate(0, Throwable.class)
                            .use();
                }
            }
        });
    }

    private void contributeInitializerUtils() {
        analyzer.linkMethod(new MethodReference(WasmGCSupport.class, "nextCharArray", char[].class)).use();
    }

    private void contributeString() {
        analyzer.addDependencyListener(new StringInternDependencySupport());
    }

    private void contributeBuffers() {
        analyzer.linkMethod(new MethodReference(Heap.class, "init", Address.class, int.class, int.class,
                void.class)).use();
    }
}
