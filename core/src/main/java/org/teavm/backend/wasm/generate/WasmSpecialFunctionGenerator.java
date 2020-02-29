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
    private List<WasmInt32Constant> regionSizeExpressions;

    public WasmSpecialFunctionGenerator(WasmClassGenerator classGenerator,
            List<WasmInt32Constant> regionSizeExpressions) {
        this.classGenerator = classGenerator;
        this.regionSizeExpressions = regionSizeExpressions;
    }

    public void generateSpecialFunctions(WasmModule module) {
        module.add(javaHeapAddress());
        module.add(availableBytes());
        module.add(regionsAddress());
        module.add(regionSize());
    }

    private WasmFunction javaHeapAddress() {
        WasmFunction function = new WasmFunction("teavm_javaHeapAddress");
        function.setExportName("teavm_javaHeapAddress");
        function.setResult(WasmType.INT32);

        int address = classGenerator.getFieldOffset(new FieldReference(WasmHeap.class.getName(), "heapAddress"));
        function.getBody().add(new WasmReturn(
                new WasmLoadInt32(4, new WasmInt32Constant(address), WasmInt32Subtype.INT32)));
        return function;
    }

    private WasmFunction availableBytes() {
        WasmFunction function = new WasmFunction("teavm_availableBytes");
        function.setExportName("teavm_availableBytes");
        function.setResult(WasmType.INT32);

        int address = classGenerator.getFieldOffset(new FieldReference(WasmHeap.class.getName(), "heapSize"));
        function.getBody().add(new WasmReturn(
                new WasmLoadInt32(4, new WasmInt32Constant(address), WasmInt32Subtype.INT32)));
        return function;
    }

    private WasmFunction regionsAddress() {
        WasmFunction function = new WasmFunction("teavm_regionsAddress");
        function.setExportName("teavm_regionsAddress");
        function.setResult(WasmType.INT32);

        int address = classGenerator.getFieldOffset(new FieldReference(WasmHeap.class.getName(), "regionsAddress"));
        function.getBody().add(new WasmReturn(
                new WasmLoadInt32(4, new WasmInt32Constant(address), WasmInt32Subtype.INT32)));
        return function;
    }

    private WasmFunction regionSize() {
        WasmFunction function = new WasmFunction("teavm_regionSize");
        function.setExportName("teavm_regionSize");
        function.setResult(WasmType.INT32);

        WasmInt32Constant constant = new WasmInt32Constant(0);
        regionSizeExpressions.add(constant);
        function.getBody().add(new WasmReturn(constant));
        return function;
    }
}
