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
package org.teavm.backend.wasm.generate.gc.classes;

import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.gc.vtable.WasmGCVirtualTableProvider;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmStructSet;

class WasmGCSystemFunctionGenerator {
    private WasmGCClassGenerator classGenerator;
    private WasmModule module;
    private WasmFunctionTypes functionTypes;
    private WasmGCNameProvider names;
    private WasmGCVirtualTableProvider virtualTables;
    private WasmGCStandardClasses standardClasses;
    private WasmFunction objectVirtualTableFunction;

    WasmGCSystemFunctionGenerator(WasmGCClassGenerator classGenerator, WasmModule module,
            WasmFunctionTypes functionTypes, WasmGCNameProvider names, WasmGCVirtualTableProvider virtualTables,
            WasmGCStandardClasses standardClasses) {
        this.classGenerator = classGenerator;
        this.module = module;
        this.functionTypes = functionTypes;
        this.names = names;
        this.virtualTables = virtualTables;
        this.standardClasses = standardClasses;
    }

    WasmFunction getFillObjectVirtualTableFunction() {
        if (objectVirtualTableFunction == null) {
            objectVirtualTableFunction = createFillObjectVirtualTableFunction();
        }
        return objectVirtualTableFunction;
    }

    private WasmFunction createFillObjectVirtualTableFunction() {
        var objectCls = standardClasses.objectClass();
        var struct = objectCls.getVirtualTableStructure();
        var function = new WasmFunction(functionTypes.of(null, struct.getNonNullReference(),
                standardClasses.classClass().getStructure().getNonNullReference()));
        function.setName(names.topLevel("teavm@objectVirtualTable"));
        var vtParam = new WasmLocal(struct.getNonNullReference(), "vt");
        var clsParam = new WasmLocal(standardClasses.classClass().getStructure().getNonNullReference());
        function.add(vtParam);
        function.add(clsParam);
        var virtualTable = virtualTables.lookup("java.lang.Object");
        for (var i = 0; i < virtualTable.getEntries().size(); ++i) {
            var entry = virtualTable.getEntries().get(i);
            classGenerator.fillVirtualTableEntry(function.getBody(), () -> new WasmGetLocal(vtParam), struct,
                    virtualTable, entry);
        }
        standardClasses.classClass().getStructure().init();
        function.getBody().add(new WasmStructSet(struct,
                new WasmGetLocal(vtParam), WasmGCClassInfoProvider.CLASS_FIELD_OFFSET,
                new WasmGetLocal(clsParam)));
        function.getBody().add(new WasmStructSet(standardClasses.classClass().getStructure(),
                new WasmGetLocal(clsParam), classGenerator.getClassVtFieldOffset(),
                new WasmGetLocal(vtParam)));
        module.functions.add(function);
        return function;
    }
}
