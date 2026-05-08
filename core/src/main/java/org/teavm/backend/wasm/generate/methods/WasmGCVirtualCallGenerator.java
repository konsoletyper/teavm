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
package org.teavm.backend.wasm.generate.methods;

import java.util.function.Consumer;
import org.teavm.backend.wasm.generate.ValueCache;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.vtable.WasmGCVirtualTableProvider;
import org.teavm.model.MethodReference;

public class WasmGCVirtualCallGenerator {
    private WasmGCVirtualTableProvider virtualTables;
    private WasmGCClassInfoProvider classInfoProvider;

    public WasmGCVirtualCallGenerator(WasmGCVirtualTableProvider virtualTables,
            WasmGCClassInfoProvider classInfoProvider) {
        this.virtualTables = virtualTables;
        this.classInfoProvider = classInfoProvider;
    }

    public void generate(WasmInstructionBuilder builder, MethodReference method, boolean suspending,
            ValueCache valueCache, WasmType.CompositeReference instanceType,
            Consumer<WasmInstructionBuilder> argsBuilder) {
        var vtable = virtualTables.lookup(method.getClassName());
        if (vtable == null) {
            builder.unreachable();
            return;
        }

        var entry = vtable.entry(method.getDescriptor());
        var nonInterfaceAncestor = vtable.closestNonInterfaceAncestor();
        if (entry == null || nonInterfaceAncestor == null) {
            builder.unreachable();
            return;
        }

        var instance = valueCache.create(instanceType, builder);

        var objectClass = classInfoProvider.getClassInfo("java.lang.Object");

        var index = WasmGCClassInfoProvider.VIRTUAL_METHOD_OFFSET + entry.getIndex();
        var expectedInstanceClassInfo = classInfoProvider.getClassInfo(vtable.getClassName());
        var expectedInstanceClassStruct = classInfoProvider.getClassInfo(
                nonInterfaceAncestor.getClassName()).getStructure();
        var vtableStruct = expectedInstanceClassInfo.getVirtualTableStructure();
        var instanceStruct = (WasmStructure) instanceType.composite;

        if (!expectedInstanceClassStruct.isSupertypeOf(instanceStruct)) {
            builder.cast(expectedInstanceClassStruct.getNonNullReference());
        }

        argsBuilder.accept(builder);

        builder
                .append(instance)
                .structGet(objectClass.getStructure(), WasmGCClassInfoProvider.VT_FIELD_OFFSET)
                .cast(vtableStruct.getNonNullReference())
                .structGet(vtableStruct, index);

        var functionTypeRef = (WasmType.CompositeReference) vtableStruct.getFields().get(index).getUnpackedType();
        builder.callReference((WasmFunctionType) functionTypeRef.composite, suspending);

        instance.release();
    }
}
