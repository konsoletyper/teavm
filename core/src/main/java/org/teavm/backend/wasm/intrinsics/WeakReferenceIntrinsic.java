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
package org.teavm.backend.wasm.intrinsics;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmNullCondition;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class WeakReferenceIntrinsic implements WasmGCBodyIntrinsic {
    private WasmGCCodeGenContext context;
    private WasmFunction createWeakReferenceFunction;

    public WeakReferenceIntrinsic(WasmGCCodeGenContext context) {
        this.context = context;
    }

    @Override
    public void apply(MethodReference method, WasmFunction function) {
        switch (method.getName()) {
            case "<init>":
                generateConstructor(function);
                break;
            case "get":
                generateDeref(function);
                break;
            case "clear":
                generateClear(function);
                break;
        }
    }

    private void generateConstructor(WasmFunction function) {
        var weakRefStruct = context.classInfoProvider().getClassInfo(WeakReference.class.getName()).getStructure();
        var thisLocal = new WasmLocal(weakRefStruct.getReference(), "this");
        var valueLocal = new WasmLocal(context.typeMapper().mapType(ValueType.parse(Object.class)), "value");
        var queueLocal = new WasmLocal(context.typeMapper().mapType(ValueType.parse(ReferenceQueue.class)), "queue");
        function.add(thisLocal);
        function.add(valueLocal);
        function.add(queueLocal);

        var weakRefConstructor = getCreateWeakReferenceFunction();
        var body = function.getBody().builder();
        body.getLocal(thisLocal);

        body.getLocal(valueLocal);
        body.getLocal(thisLocal);
        body.getLocal(queueLocal);
        body.call(weakRefConstructor);
        body.structSet(weakRefStruct, WasmGCClassInfoProvider.WEAK_REFERENCE_OFFSET);
    }

    private void generateDeref(WasmFunction function) {
        var weakRefStruct = context.classInfoProvider().getClassInfo(WeakReference.class.getName()).getStructure();
        var objectType = context.classInfoProvider().getClassInfo("java.lang.Object").getType();
        var thisType =  weakRefStruct.getReference();
        var thisLocal = new WasmLocal(thisType, "this");
        function.add(thisLocal);

        var body = function.getBody().builder();
        var block = body.block(WasmType.EXTERN);
        block.getLocal(thisLocal);
        block
                .structGet(weakRefStruct, WasmGCClassInfoProvider.WEAK_REFERENCE_OFFSET)
                .nullBranch(WasmNullCondition.NOT_NULL, block)
                .nullConst(objectType)
                .return_();
        body.call(createDerefFunction());
    }

    private void generateClear(WasmFunction function) {
        var weakRefStruct = context.classInfoProvider().getClassInfo(WeakReference.class.getName()).getStructure();
        var thisType = weakRefStruct.getReference();
        var thisLocal = new WasmLocal(thisType, "this");
        function.add(thisLocal);

        var body = function.getBody().builder();
        body.getLocal(thisLocal);
        body
                .nullConst(WasmType.EXTERN)
                .structSet(weakRefStruct, WasmGCClassInfoProvider.WEAK_REFERENCE_OFFSET);
    }

    private WasmFunction getCreateWeakReferenceFunction() {
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

    private WasmFunction createDerefFunction() {
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
