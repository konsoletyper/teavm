/*
 *  Copyright 2016 Alexey Andreev.
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

import java.util.ArrayList;
import java.util.List;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.binary.BinaryWriter;
import org.teavm.backend.wasm.generate.CallSiteBinaryGenerator;
import org.teavm.backend.wasm.generate.WasmClassGenerator;
import org.teavm.backend.wasm.generate.WasmStringPool;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.model.MethodReference;
import org.teavm.model.lowlevel.CallSiteDescriptor;
import org.teavm.runtime.CallSite;
import org.teavm.runtime.ExceptionHandling;

public class ExceptionHandlingIntrinsic implements WasmIntrinsic {
    private CallSiteBinaryGenerator callSiteBinaryGenerator;
    private WasmClassGenerator classGenerator;
    private List<WasmInt32Constant> constants = new ArrayList<>();

    public ExceptionHandlingIntrinsic(BinaryWriter binaryWriter, WasmClassGenerator classGenerator,
            WasmStringPool stringPool) {
        callSiteBinaryGenerator = new CallSiteBinaryGenerator(binaryWriter, classGenerator, stringPool);
        this.classGenerator = classGenerator;
    }

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        if (!methodReference.getClassName().equals(ExceptionHandling.class.getName())) {
            return false;
        }
        switch (methodReference.getName()) {
            case "findCallSiteById":
                return true;
        }
        return false;
    }

    public void postProcess(List<CallSiteDescriptor> callSites) {
        int address = callSiteBinaryGenerator.writeCallSites(callSites);
        for (WasmInt32Constant constant : constants) {
            constant.setValue(address);
        }
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        WasmInt32Constant constant = new WasmInt32Constant(0);
        constant.setLocation(invocation.getLocation());
        constants.add(constant);

        int callSiteSize = classGenerator.getClassSize(CallSite.class.getName());
        WasmExpression id = manager.generate(invocation.getArguments().get(0));
        WasmExpression offset = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.MUL,
                id, new WasmInt32Constant(callSiteSize));

        return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, constant, offset);
    }
}
