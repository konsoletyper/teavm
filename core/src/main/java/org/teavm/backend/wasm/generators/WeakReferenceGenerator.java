/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.backend.wasm.generators;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmNullCondition;
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
        var thisLocal = new WasmLocal(context.isCompactMode()
                ? WasmType.ANY
                : weakRefStruct.getReference(), "this");
        var valueLocal = new WasmLocal(context.typeMapper().mapType(ValueType.parse(Object.class)), "value");
        var queueLocal = new WasmLocal(context.typeMapper().mapType(ValueType.parse(ReferenceQueue.class)), "queue");
        function.add(thisLocal);
        function.add(valueLocal);
        function.add(queueLocal);

        var weakRefConstructor = getCreateWeakReferenceFunction(context);
        var body = function.getBody().builder();
        body.getLocal(thisLocal);
        if (context.isCompactMode()) {
            body.cast(weakRefStruct.getReference());
        }

        body.getLocal(valueLocal);
        body.getLocal(thisLocal);
        if (context.isCompactMode()) {
            body.cast(weakRefStruct.getReference());
        }
        body.getLocal(queueLocal);
        body.call(weakRefConstructor);
        body.structSet(weakRefStruct, WasmGCClassInfoProvider.WEAK_REFERENCE_OFFSET);
    }

    private void generateDeref(WasmGCCustomGeneratorContext context, WasmFunction function) {
        var weakRefStruct = context.classInfoProvider().getClassInfo(WeakReference.class.getName()).getStructure();
        var objectType = context.classInfoProvider().getClassInfo("java.lang.Object").getType();
        var thisType = context.isCompactMode() ? WasmType.ANY : weakRefStruct.getReference();
        var thisLocal = new WasmLocal(thisType, "this");
        function.add(thisLocal);

        var body = function.getBody().builder();
        var block = body.block(WasmType.EXTERN);
        block.getLocal(thisLocal);
        if (context.isCompactMode()) {
            block.cast(weakRefStruct.getReference());
        }
        block
                .structGet(weakRefStruct, WasmGCClassInfoProvider.WEAK_REFERENCE_OFFSET)
                .nullBranch(WasmNullCondition.NOT_NULL, block)
                .nullConst(objectType)
                .return_();
        body.call(createDerefFunction(context));
    }

    private void generateClear(WasmGCCustomGeneratorContext context, WasmFunction function) {
        var weakRefStruct = context.classInfoProvider().getClassInfo(WeakReference.class.getName()).getStructure();
        var thisType = context.isCompactMode() ? WasmType.ANY : weakRefStruct.getReference();
        var thisLocal = new WasmLocal(thisType, "this");
        function.add(thisLocal);

        var body = function.getBody().builder();
        body.getLocal(thisLocal);
        if (context.isCompactMode()) {
            body.cast(weakRefStruct.getReference());
        }
        body
                .nullConst(WasmType.EXTERN)
                .structSet(weakRefStruct, WasmGCClassInfoProvider.WEAK_REFERENCE_OFFSET);
    }

    private WasmFunction getCreateWeakReferenceFunction(WasmGCCustomGeneratorContext context) {
        if (createWeakReferenceFunction == null) {
            var function = new WasmFunction(context.functionTypes().of(
                    WasmType.EXTERN,
                    context.typeMapper().mapType(ValueType.parse(Object.class)),
                    context.typeMapper().mapType(ValueType.parse(WeakReference.class)),
                    context.typeMapper().mapType(ValueType.parse(ReferenceQueue.class))
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
                WasmType.EXTERN));
        function.setName(context.names().topLevel("teavm@deref"));
        function.setImportName("deref");
        function.setImportModule("teavm");
        context.module().functions.add(function);
        return function;
    }
}
