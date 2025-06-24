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
package org.teavm.backend.wasm.transformation.gc;

import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.transformation.SuspensionPointCollector;

public class CoroutineTransformation {
    private static final String FIBER = "org.teavm.runtime.Fiber";
    private WasmGCClassInfoProvider classInfoProvider;
    private CoroutineTransformationVisitor transformationVisitor;

    public CoroutineTransformation(WasmFunctionTypes functionTypes, BaseWasmFunctionRepository functions,
            WasmGCClassInfoProvider classInfoProvider) {
        this.classInfoProvider = classInfoProvider;
        transformationVisitor = new CoroutineTransformationVisitor(functionTypes, new CoroutineFunctions(functions,
                classInfoProvider));
    }

    public void transform(WasmFunction function) {
        var suspensionPoints = new SuspensionPointCollector();
        for (var part : function.getBody()) {
            part.acceptVisitor(suspensionPoints);
        }
        transformationVisitor.collector = suspensionPoints;

        var stateLocal = new WasmLocal(WasmType.INT32, "_teavm_fiberState");
        var fiberLocal = new WasmLocal(classInfoProvider.getClassInfo(FIBER).getType(), "_teavm_fiber");
        function.add(stateLocal);
        function.add(fiberLocal);



        transformationVisitor.collector = null;
    }
}
