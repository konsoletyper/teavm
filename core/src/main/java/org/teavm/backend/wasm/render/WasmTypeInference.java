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
package org.teavm.backend.wasm.render;

import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayCopy;
import org.teavm.backend.wasm.model.expression.WasmArrayGet;
import org.teavm.backend.wasm.model.expression.WasmArrayLength;
import org.teavm.backend.wasm.model.expression.WasmArrayNewDefault;
import org.teavm.backend.wasm.model.expression.WasmArraySet;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmBreak;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmConditional;
import org.teavm.backend.wasm.model.expression.WasmConversion;
import org.teavm.backend.wasm.model.expression.WasmCopy;
import org.teavm.backend.wasm.model.expression.WasmDrop;
import org.teavm.backend.wasm.model.expression.WasmExpressionVisitor;
import org.teavm.backend.wasm.model.expression.WasmFill;
import org.teavm.backend.wasm.model.expression.WasmFloat32Constant;
import org.teavm.backend.wasm.model.expression.WasmFloat64Constant;
import org.teavm.backend.wasm.model.expression.WasmFloatBinary;
import org.teavm.backend.wasm.model.expression.WasmFloatType;
import org.teavm.backend.wasm.model.expression.WasmFloatUnary;
import org.teavm.backend.wasm.model.expression.WasmFunctionReference;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmIndirectCall;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt64Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnary;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat32;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat64;
import org.teavm.backend.wasm.model.expression.WasmLoadInt32;
import org.teavm.backend.wasm.model.expression.WasmLoadInt64;
import org.teavm.backend.wasm.model.expression.WasmMemoryGrow;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmReferencesEqual;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmStoreFloat32;
import org.teavm.backend.wasm.model.expression.WasmStoreFloat64;
import org.teavm.backend.wasm.model.expression.WasmStoreInt32;
import org.teavm.backend.wasm.model.expression.WasmStoreInt64;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructNew;
import org.teavm.backend.wasm.model.expression.WasmStructNewDefault;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.backend.wasm.model.expression.WasmSwitch;
import org.teavm.backend.wasm.model.expression.WasmThrow;
import org.teavm.backend.wasm.model.expression.WasmTry;
import org.teavm.backend.wasm.model.expression.WasmUnreachable;

public class WasmTypeInference implements WasmExpressionVisitor {
    private WasmType result;

    public WasmType getResult() {
        return result;
    }

    @Override
    public void visit(WasmBlock expression) {
        result = expression.getType();
    }

    @Override
    public void visit(WasmBranch expression) {
        result = null;
    }

    @Override
    public void visit(WasmBreak expression) {
        result = null;
    }

    @Override
    public void visit(WasmSwitch expression) {
        result = null;
    }

    @Override
    public void visit(WasmConditional expression) {
        result = expression.getType();
    }

    @Override
    public void visit(WasmReturn expression) {
        result = null;
    }

    @Override
    public void visit(WasmUnreachable expression) {
        result = null;
    }

    @Override
    public void visit(WasmInt32Constant expression) {
        result = WasmType.INT32;
    }

    @Override
    public void visit(WasmInt64Constant expression) {
        result = WasmType.INT64;
    }

    @Override
    public void visit(WasmFloat32Constant expression) {
        result = WasmType.FLOAT32;
    }

    @Override
    public void visit(WasmFloat64Constant expression) {
        result = WasmType.FLOAT64;
    }

    @Override
    public void visit(WasmGetLocal expression) {
        result = expression.getLocal().getType();
    }

    @Override
    public void visit(WasmSetLocal expression) {
        result = null;
    }

    @Override
    public void visit(WasmGetGlobal expression) {
        result = expression.getGlobal().getType();
    }

    @Override
    public void visit(WasmSetGlobal expression) {
        result = null;
    }

    @Override
    public void visit(WasmIntBinary expression) {
        result = map(expression.getType());
    }

    @Override
    public void visit(WasmFloatBinary expression) {
        result = map(expression.getType());
    }

    @Override
    public void visit(WasmIntUnary expression) {
        result = map(expression.getType());
    }

    @Override
    public void visit(WasmFloatUnary expression) {
        result = map(expression.getType());
    }

    @Override
    public void visit(WasmConversion expression) {
        result = WasmType.num(expression.getTargetType());
    }

    @Override
    public void visit(WasmNullConstant expression) {
        result = expression.type;
    }

    @Override
    public void visit(WasmCall expression) {
        var function = expression.getFunction();
        result = function == null ? null : function.getType().getReturnType();
    }

    @Override
    public void visit(WasmIndirectCall expression) {
        result = expression.getType().getReturnType();
    }

    @Override
    public void visit(WasmCallReference expression) {
        result = expression.getType().getReturnType();
    }

    @Override
    public void visit(WasmDrop expression) {
        result = null;
    }

    @Override
    public void visit(WasmLoadInt32 expression) {
        result = WasmType.INT32;
    }

    @Override
    public void visit(WasmLoadInt64 expression) {
        result = WasmType.INT64;
    }

    @Override
    public void visit(WasmLoadFloat32 expression) {
        result = WasmType.FLOAT32;
    }

    @Override
    public void visit(WasmLoadFloat64 expression) {
        result = WasmType.FLOAT64;
    }

    @Override
    public void visit(WasmStoreInt32 expression) {
        result = null;
    }

    @Override
    public void visit(WasmStoreInt64 expression) {
        result = null;
    }

    @Override
    public void visit(WasmStoreFloat32 expression) {
        result = null;
    }

    @Override
    public void visit(WasmStoreFloat64 expression) {
        result = null;
    }

    @Override
    public void visit(WasmMemoryGrow expression) {
        result = WasmType.INT32;
    }

    @Override
    public void visit(WasmFill expression) {
        result = null;
    }

    @Override
    public void visit(WasmCopy expression) {
        result = null;
    }

    @Override
    public void visit(WasmTry expression) {
        result = expression.getType();
    }

    @Override
    public void visit(WasmThrow expression) {
        result = null;
    }

    @Override
    public void visit(WasmReferencesEqual expression) {
        result = WasmType.INT32;
    }

    @Override
    public void visit(WasmCast expression) {
        result = expression.getTargetType();
    }

    @Override
    public void visit(WasmStructNew expression) {
        result = expression.getType().getReference();
    }

    @Override
    public void visit(WasmStructNewDefault expression) {
        result = expression.getType().getReference();
    }

    @Override
    public void visit(WasmStructGet expression) {
        result = expression.getType().getFields().get(expression.getFieldIndex()).getUnpackedType();
    }

    @Override
    public void visit(WasmStructSet expression) {
        result = null;
    }

    @Override
    public void visit(WasmArrayNewDefault expression) {
        result = expression.getType().getReference();
    }

    @Override
    public void visit(WasmArrayGet expression) {
        result = expression.getType().getElementType().asUnpackedType();
    }

    @Override
    public void visit(WasmArraySet expression) {
        result = null;
    }

    @Override
    public void visit(WasmArrayLength expression) {
        result = WasmType.INT32;
    }

    @Override
    public void visit(WasmArrayCopy expression) {
        result = null;
    }

    @Override
    public void visit(WasmFunctionReference expression) {
        result = expression.getFunction().getType().getReference();
    }

    private static WasmType map(WasmIntType type) {
        switch (type) {
            case INT32:
                return WasmType.INT32;
            case INT64:
                return WasmType.INT64;
            default:
                throw new IllegalArgumentException(type.toString());
        }
    }

    private static WasmType map(WasmFloatType type) {
        switch (type) {
            case FLOAT32:
                return WasmType.FLOAT32;
            case FLOAT64:
                return WasmType.FLOAT64;
            default:
                throw new IllegalArgumentException(type.toString());
        }
    }
}
