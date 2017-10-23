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

import org.teavm.ast.ConstantExpr;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.generate.WasmClassGenerator;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.interop.Structure;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class StructureIntrinsic implements WasmIntrinsic {
    private ClassReaderSource classSource;
    private WasmClassGenerator classGenerator;

    public StructureIntrinsic(ClassReaderSource classSource, WasmClassGenerator classGenerator) {
        this.classSource = classSource;
        this.classGenerator = classGenerator;
    }

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        if (!classSource.isSuperType(Structure.class.getName(), methodReference.getClassName()).orElse(false)) {
            return false;
        }
        switch (methodReference.getName()) {
            case "toAddress":
            case "cast":
            case "sizeOf":
            case "add":
                return true;
        }
        return false;
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        switch (invocation.getMethod().getName()) {
            case "toAddress":
            case "cast":
                return manager.generate(invocation.getArguments().get(0));
            case "sizeOf": {
                ValueType.Object type = (ValueType.Object) ((ConstantExpr) invocation.getArguments().get(0)).getValue();
                return new WasmInt32Constant(classGenerator.getClassSize(type.getClassName()));
            }
            case "add": {
                WasmExpression base = manager.generate(invocation.getArguments().get(1));
                WasmExpression offset = manager.generate(invocation.getArguments().get(2));
                Object type = ((ConstantExpr) invocation.getArguments().get(0)).getValue();
                String className = ((ValueType.Object) type).getClassName();
                int size = classGenerator.getClassSize(className);
                int alignment = classGenerator.getClassAlignment(className);
                size = WasmClassGenerator.align(size, alignment);

                offset = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.MUL, offset,
                        new WasmInt32Constant(size));
                return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, base, offset);
            }
            default:
                throw new IllegalArgumentException(invocation.getMethod().toString());
        }
    }
}
