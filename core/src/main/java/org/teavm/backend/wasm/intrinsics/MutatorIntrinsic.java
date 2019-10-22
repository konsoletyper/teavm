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
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.model.MethodReference;
import org.teavm.runtime.Mutator;

public class MutatorIntrinsic implements WasmIntrinsic {
    private List<WasmInt32Constant> staticGcRootsExpressions = new ArrayList<>();
    private List<WasmInt32Constant> classesExpressions = new ArrayList<>();
    private List<WasmInt32Constant> classCountExpressions = new ArrayList<>();

    public void setStaticGcRootsAddress(int address) {
        for (WasmInt32Constant constant : staticGcRootsExpressions) {
            constant.setValue(address);
        }
    }

    public void setClassesAddress(int address) {
        for (WasmInt32Constant constant : classesExpressions) {
            constant.setValue(address);
        }
    }

    public void setClassCount(int count) {
        for (WasmInt32Constant constant : classCountExpressions) {
            constant.setValue(count);
        }
    }

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        if (!methodReference.getClassName().equals(Mutator.class.getName())) {
            return false;
        }
        switch (methodReference.getName()) {
            case "getStaticGCRoots":
            case "getClasses":
            case "getClassCount":
                return true;
            default:
                return false;
        }
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        switch (invocation.getMethod().getName()) {
            case "getStaticGCRoots": {
                WasmInt32Constant constant = new WasmInt32Constant(0);
                staticGcRootsExpressions.add(constant);
                return constant;
            }
            case "getClasses": {
                WasmInt32Constant constant = new WasmInt32Constant(0);
                classesExpressions.add(constant);
                return constant;
            }
            case "getClassCount": {
                WasmInt32Constant constant = new WasmInt32Constant(0);
                classCountExpressions.add(constant);
                return constant;
            }
        }
        throw new IllegalArgumentException(invocation.getMethod().toString());
    }
}
