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
package org.teavm.backend.wasm.generate.gc.methods;

import java.util.List;
import org.teavm.backend.wasm.gc.vtable.WasmGCVirtualTableProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmUnreachable;
import org.teavm.model.MethodReference;

public class WasmGCVirtualCallGenerator {
    private WasmGCVirtualTableProvider virtualTables;
    private WasmGCClassInfoProvider classInfoProvider;

    public WasmGCVirtualCallGenerator(WasmGCVirtualTableProvider virtualTables,
            WasmGCClassInfoProvider classInfoProvider) {
        this.virtualTables = virtualTables;
        this.classInfoProvider = classInfoProvider;
    }

    public WasmExpression generate(MethodReference method, WasmLocal instance, List<WasmExpression> arguments) {
        var vtable = virtualTables.lookup(method.getClassName());
        if (vtable == null) {
            return new WasmUnreachable();
        }

        var entry = vtable.entry(method.getDescriptor());
        var nonInterfaceAncestor = vtable.closestNonInterfaceAncestor();
        if (entry == null || nonInterfaceAncestor == null) {
            return new WasmUnreachable();
        }

        var objectClass = classInfoProvider.getClassInfo("java.lang.Object");

        WasmExpression classRef = new WasmStructGet(objectClass.getStructure(),
                new WasmGetLocal(instance), WasmGCClassInfoProvider.VT_FIELD_OFFSET);
        var index = WasmGCClassInfoProvider.VIRTUAL_METHOD_OFFSET + entry.getIndex();
        var expectedInstanceClassInfo = classInfoProvider.getClassInfo(vtable.getClassName());
        var expectedInstanceClassStruct = classInfoProvider.getClassInfo(
                nonInterfaceAncestor.getClassName()).getStructure();
        var vtableStruct = expectedInstanceClassInfo.getVirtualTableStructure();
        classRef = new WasmCast(classRef, vtableStruct.getNonNullReference());

        var functionRef = new WasmStructGet(vtableStruct, classRef, index);
        var functionTypeRef = (WasmType.CompositeReference) vtableStruct.getFields().get(index).getUnpackedType();
        var invoke = new WasmCallReference(functionRef, (WasmFunctionType) functionTypeRef.composite);
        WasmExpression instanceRef = new WasmGetLocal(instance);
        var instanceType = (WasmType.CompositeReference) instance.getType();
        var instanceStruct = (WasmStructure) instanceType.composite;
        if (!expectedInstanceClassStruct.isSupertypeOf(instanceStruct)) {
            instanceRef = new WasmCast(instanceRef, expectedInstanceClassStruct.getNonNullReference());
        }

        invoke.getArguments().add(instanceRef);
        invoke.getArguments().addAll(arguments);
        return invoke;
    }
}
