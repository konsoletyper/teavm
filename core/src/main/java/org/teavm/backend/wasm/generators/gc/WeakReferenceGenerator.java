/*
 *  Copyright 2024 konsoletyper.
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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmNullBranch;
import org.teavm.backend.wasm.model.expression.WasmNullCondition;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class WeakReferenceGenerator implements WasmGCCustomGenerator {
    private WasmFunction createWeakReferenceFunction;

    @Override
    public void apply(MethodReference method, WasmFunction function, WasmGCCustomGeneratorContext context) {
        switch (method.getName()) {
            case "<init>":
                generateConstructor(context, function);
                break;
            case "get":
                generateDeref(context, function);
                break;
            case "clear":
                generateClear(context, function);
                break;
        }
    }

    private void generateConstructor(WasmGCCustomGeneratorContext context, WasmFunction function) {
        var weakRefStruct = context.classInfoProvider().getClassInfo(WeakReference.class.getName()).getStructure();
        var thisLocal = new WasmLocal(weakRefStruct.getReference(), "this");
        var valueLocal = new WasmLocal(context.typeMapper().mapType(ValueType.parse(Object.class)), "value");
        var queueLocal = new WasmLocal(context.typeMapper().mapType(ValueType.parse(ReferenceQueue.class)), "queue");
        function.add(thisLocal);
        function.add(valueLocal);
        function.add(queueLocal);

        var weakRefConstructor = getCreateWeakReferenceFunction(context);
        var weakRef = new WasmCall(weakRefConstructor, new WasmGetLocal(valueLocal), new WasmGetLocal(thisLocal));
        function.getBody().add(new WasmStructSet(weakRefStruct, new WasmGetLocal(thisLocal),
                WasmGCClassInfoProvider.WEAK_REFERENCE_OFFSET, weakRef));
    }

    private void generateDeref(WasmGCCustomGeneratorContext context, WasmFunction function) {
        var weakRefStruct = context.classInfoProvider().getClassInfo(WeakReference.class.getName()).getStructure();
        var objectType = context.classInfoProvider().getClassInfo("java.lang.Object").getType();
        var thisLocal = new WasmLocal(weakRefStruct.getReference(), "this");
        function.add(thisLocal);

        var block = new WasmBlock(false);
        block.setType(WasmType.Reference.EXTERN);
        var weakRef = new WasmStructGet(weakRefStruct, new WasmGetLocal(thisLocal),
                WasmGCClassInfoProvider.WEAK_REFERENCE_OFFSET);
        var br = new WasmNullBranch(WasmNullCondition.NOT_NULL, weakRef, block);
        block.getBody().add(br);
        block.getBody().add(new WasmReturn(new WasmNullConstant(objectType)));

        function.getBody().add(new WasmCall(createDerefFunction(context), block));
    }

    private void generateClear(WasmGCCustomGeneratorContext context, WasmFunction function) {
        var weakRefStruct = context.classInfoProvider().getClassInfo(WeakReference.class.getName()).getStructure();
        var thisLocal = new WasmLocal(weakRefStruct.getReference(), "this");
        function.add(thisLocal);

        function.getBody().add(new WasmStructSet(weakRefStruct, new WasmGetLocal(thisLocal),
                WasmGCClassInfoProvider.WEAK_REFERENCE_OFFSET, new WasmNullConstant(WasmType.Reference.EXTERN)));
    }

    private WasmFunction getCreateWeakReferenceFunction(WasmGCCustomGeneratorContext context) {
        if (createWeakReferenceFunction == null) {
            var function = new WasmFunction(context.functionTypes().of(
                    WasmType.Reference.EXTERN,
                    context.typeMapper().mapType(ValueType.parse(Object.class)),
                    context.typeMapper().mapType(ValueType.parse(WeakReference.class))
            ));
            function.setName(context.names().topLevel("teavm@createWeakReference"));
            function.setImportName("createWeakRef");
            function.setImportModule("teavm");
            context.module().functions.add(function);
            createWeakReferenceFunction = function;
        }
        return createWeakReferenceFunction;
    }

    private WasmFunction createDerefFunction(WasmGCCustomGeneratorContext context) {
        var function = new WasmFunction(context.functionTypes().of(
                context.typeMapper().mapType(ValueType.parse(Object.class)),
                WasmType.Reference.EXTERN));
        function.setName(context.names().topLevel("teavm@deref"));
        function.setImportName("deref");
        function.setImportModule("teavm");
        context.module().functions.add(function);
        return function;
    }
}
