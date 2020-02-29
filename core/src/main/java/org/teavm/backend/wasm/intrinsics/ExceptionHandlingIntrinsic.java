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
import org.teavm.backend.wasm.model.expression.WasmUnreachable;
import org.teavm.model.MethodReference;
import org.teavm.model.lowlevel.CallSiteDescriptor;
import org.teavm.runtime.CallSite;
import org.teavm.runtime.ExceptionHandling;

public class ExceptionHandlingIntrinsic implements WasmIntrinsic {
    private CallSiteBinaryGenerator callSiteBinaryGenerator;
    private WasmClassGenerator classGenerator;
    private List<WasmInt32Constant> constants = new ArrayList<>();

    public ExceptionHandlingIntrinsic(BinaryWriter binaryWriter, WasmClassGenerator classGenerator,
            WasmStringPool stringPool, boolean obfuscated) {
        callSiteBinaryGenerator = new CallSiteBinaryGenerator(binaryWriter, classGenerator, stringPool, obfuscated);
        this.classGenerator = classGenerator;
    }

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        if (!methodReference.getClassName().equals(ExceptionHandling.class.getName())) {
            return false;
        }
        switch (methodReference.getName()) {
            case "findCallSiteById":
            case "isJumpSupported":
            case "jumpToFrame":
            case "abort":
            case "isObfuscated":
                return true;
        }
        return false;
    }

    public void postProcess(List<? extends CallSiteDescriptor> callSites) {
        int address = callSiteBinaryGenerator.writeCallSites(callSites);
        for (WasmInt32Constant constant : constants) {
            constant.setValue(address);
        }
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        switch (invocation.getMethod().getName()) {
            case "findCallSiteById": {
                WasmInt32Constant constant = new WasmInt32Constant(0);
                constant.setLocation(invocation.getLocation());
                constants.add(constant);

                int callSiteSize = classGenerator.getClassSize(CallSite.class.getName());
                WasmExpression id = manager.generate(invocation.getArguments().get(0));
                WasmExpression offset = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.MUL,
                        id, new WasmInt32Constant(callSiteSize));

                return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, constant, offset);
            }

            case "isJumpSupported":
            case "isObfuscated":
                return new WasmInt32Constant(0);

            case "jumpToFrame":
            case "abort":
                return new WasmUnreachable();

            default:
                throw new IllegalArgumentException("Unknown method: " + invocation.getMethod());
        }
    }
}
