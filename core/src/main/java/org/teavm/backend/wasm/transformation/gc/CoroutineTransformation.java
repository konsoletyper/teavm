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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmConditional;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmFloat32Constant;
import org.teavm.backend.wasm.model.expression.WasmFloat64Constant;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt64Constant;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.transformation.SuspensionPointCollector;

public class CoroutineTransformation {
    private static final String FIBER = "org.teavm.runtime.Fiber";
    private WasmFunctionTypes functionTypes;
    private WasmGCClassInfoProvider classInfoProvider;
    private CoroutineFunctions coroutineFunctions;

    public CoroutineTransformation(WasmFunctionTypes functionTypes, BaseWasmFunctionRepository functions,
            WasmGCClassInfoProvider classInfoProvider) {
        this.functionTypes = functionTypes;
        this.classInfoProvider = classInfoProvider;
        coroutineFunctions = new CoroutineFunctions(functions);
    }

    public void transform(WasmFunction function) {
        var suspensionPoints = new SuspensionPointCollector();
        for (var part : function.getBody()) {
            part.acceptVisitor(suspensionPoints);
        }
        var transformationVisitor = new CoroutineTransformationVisitor(functionTypes, coroutineFunctions);
        transformationVisitor.collector = suspensionPoints;

        var locals = List.copyOf(function.getLocalVariables());

        var stateLocal = new WasmLocal(WasmType.INT32, "_teavm_fiberState");
        var fiberLocal = new WasmLocal(classInfoProvider.getClassInfo(FIBER).getType(), "_teavm_fiber");
        function.add(stateLocal);
        function.add(fiberLocal);

        transformationVisitor.stateLocal = stateLocal;
        transformationVisitor.fiberLocal = fiberLocal;
        transformationVisitor.tmpValueLocalSupplier = new Supplier<>() {
            private WasmLocal local;

            @Override
            public WasmLocal get() {
                if (local == null) {
                    local = new WasmLocal(WasmType.Reference.FUNC, "_teavm_fiberTmp");
                    function.add(local);
                }
                return local;
            }
        };
        transformationVisitor.init();
        for (var part : function.getBody()) {
            part.acceptVisitor(transformationVisitor);
        }
        if (!transformationVisitor.resultList.isEmpty()) {
            var lastIndex = transformationVisitor.resultList.size() - 1;
            var last = transformationVisitor.resultList.get(lastIndex);
            if (!last.isTerminating()) {
                transformationVisitor.resultList.set(lastIndex, new WasmReturn(last));
            }
        }

        function.getBody().clear();
        function.getBody().addAll(generatePrologue(fiberLocal, stateLocal, locals));
        transformationVisitor.mainBlock.getBody().addAll(transformationVisitor.resultList);
        function.getBody().add(transformationVisitor.mainBlock);
        function.getBody().addAll(generateEpilogue(fiberLocal, stateLocal, locals,
                function.getType().getSingleReturnType()));

        transformationVisitor.complete();
    }

    private List<WasmExpression> generatePrologue(WasmLocal fiberLocal, WasmLocal stateLocal,
            List<WasmLocal> locals) {
        var prologue = new ArrayList<WasmExpression>();
        prologue.add(new WasmSetLocal(fiberLocal, new WasmCall(coroutineFunctions.currentFiber())));
        var restoreCond = new WasmConditional(new WasmCall(coroutineFunctions.isResuming(),
                new WasmGetLocal(fiberLocal)));
        prologue.add(restoreCond);
        restoreCond.getElseBlock().getBody().add(new WasmSetLocal(stateLocal, new WasmInt32Constant(0)));

        var restoreBody = restoreCond.getThenBlock().getBody();
        restoreBody.add(new WasmSetLocal(stateLocal, new WasmCall(coroutineFunctions.popInt(),
                new WasmGetLocal(fiberLocal))));
        for (var i = locals.size() - 1; i >= 0; i--) {
            var local = locals.get(i);
            restoreBody.add(new WasmSetLocal(local, coroutineFunctions.restoreValue(local.getType(), fiberLocal)));
        }

        return prologue;
    }

    private List<WasmExpression> generateEpilogue(WasmLocal fiberLocal, WasmLocal stateLocal,
            List<WasmLocal> locals, WasmType returnType) {
        var epilogue = new ArrayList<WasmExpression>();
        for (var local : locals) {
            epilogue.add(coroutineFunctions.saveValue(local.getType(), fiberLocal, new WasmGetLocal(local)));
        }
        epilogue.add(new WasmCall(coroutineFunctions.pushInt(), new WasmGetLocal(stateLocal),
                new WasmGetLocal(fiberLocal)));

        if (returnType != null) {
            if (returnType instanceof WasmType.Number) {
                switch (((WasmType.Number) returnType).number) {
                    case INT32:
                        epilogue.add(new WasmInt32Constant(0));
                        break;
                    case INT64:
                        epilogue.add(new WasmInt64Constant(0));
                        break;
                    case FLOAT32:
                        epilogue.add(new WasmFloat32Constant(0));
                        break;
                    case FLOAT64:
                        epilogue.add(new WasmFloat64Constant(0));
                        break;
                }
            } else {
                epilogue.add(new WasmNullConstant((WasmType.Reference) returnType));
            }
        }

        return epilogue;
    }
}
