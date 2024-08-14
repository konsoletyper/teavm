/*
 *  Copyright 2020 Alexey Andreev.
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
package org.teavm.backend.wasm.generate;

import java.util.List;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.WasmHeap;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;
import org.teavm.backend.wasm.model.expression.WasmLoadInt32;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.model.FieldReference;

public class WasmSpecialFunctionGenerator {
    private WasmClassGenerator classGenerator;
    private WasmFunctionTypes functionTypes;
    private List<WasmInt32Constant> regionSizeExpressions;

    public WasmSpecialFunctionGenerator(WasmClassGenerator classGenerator, WasmFunctionTypes functionTypes,
            List<WasmInt32Constant> regionSizeExpressions) {
        this.classGenerator = classGenerator;
        this.functionTypes = functionTypes;
        this.regionSizeExpressions = regionSizeExpressions;
    }

    public void generateSpecialFunctions(WasmModule module) {
        module.functions.add(javaHeapAddress());
        module.functions.add(availableBytes());
        module.functions.add(regionsAddress());
        module.functions.add(regionSize());
    }

    private WasmFunction javaHeapAddress() {
        var function = new WasmFunction(functionTypes.of(WasmType.INT32));
        function.setName("teavm_javaHeapAddress");
        function.setExportName("teavm_javaHeapAddress");

        int address = classGenerator.getFieldOffset(new FieldReference(WasmHeap.class.getName(), "heapAddress"));
        function.getBody().add(new WasmReturn(
                new WasmLoadInt32(4, new WasmInt32Constant(address), WasmInt32Subtype.INT32)));
        return function;
    }

    private WasmFunction availableBytes() {
        var function = new WasmFunction(functionTypes.of(WasmType.INT32));
        function.setName("teavm_availableBytes");
        function.setExportName("teavm_availableBytes");

        int address = classGenerator.getFieldOffset(new FieldReference(WasmHeap.class.getName(), "heapSize"));
        function.getBody().add(new WasmReturn(
                new WasmLoadInt32(4, new WasmInt32Constant(address), WasmInt32Subtype.INT32)));
        return function;
    }

    private WasmFunction regionsAddress() {
        var function = new WasmFunction(functionTypes.of(WasmType.INT32));
        function.setExportName("teavm_regionsAddress");
        function.setName("teavm_regionsAddress");

        int address = classGenerator.getFieldOffset(new FieldReference(WasmHeap.class.getName(), "regionsAddress"));
        function.getBody().add(new WasmReturn(
                new WasmLoadInt32(4, new WasmInt32Constant(address), WasmInt32Subtype.INT32)));
        return function;
    }

    private WasmFunction regionSize() {
        var function = new WasmFunction(functionTypes.of(WasmType.INT32));
        function.setExportName("teavm_regionSize");
        function.setName("teavm_regionSize");

        WasmInt32Constant constant = new WasmInt32Constant(0);
        regionSizeExpressions.add(constant);
        function.getBody().add(new WasmReturn(constant));
        return function;
    }
}
