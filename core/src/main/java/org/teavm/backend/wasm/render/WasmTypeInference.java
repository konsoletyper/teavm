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

import java.util.Collections;
import java.util.List;
import org.teavm.backend.wasm.model.WasmBlockType;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayCopy;
import org.teavm.backend.wasm.model.expression.WasmArrayGet;
import org.teavm.backend.wasm.model.expression.WasmArrayLength;
import org.teavm.backend.wasm.model.expression.WasmArrayNewDefault;
import org.teavm.backend.wasm.model.expression.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.expression.WasmArraySet;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmBreak;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmCastBranch;
import org.teavm.backend.wasm.model.expression.WasmConditional;
import org.teavm.backend.wasm.model.expression.WasmConversion;
import org.teavm.backend.wasm.model.expression.WasmCopy;
import org.teavm.backend.wasm.model.expression.WasmDrop;
import org.teavm.backend.wasm.model.expression.WasmExpressionVisitor;
import org.teavm.backend.wasm.model.expression.WasmExternConversion;
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
import org.teavm.backend.wasm.model.expression.WasmInt31Get;
import org.teavm.backend.wasm.model.expression.WasmInt31Reference;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt64Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnary;
import org.teavm.backend.wasm.model.expression.WasmIsNull;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat32;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat64;
import org.teavm.backend.wasm.model.expression.WasmLoadInt32;
import org.teavm.backend.wasm.model.expression.WasmLoadInt64;
import org.teavm.backend.wasm.model.expression.WasmMemoryGrow;
import org.teavm.backend.wasm.model.expression.WasmNullBranch;
import org.teavm.backend.wasm.model.expression.WasmNullCondition;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmPop;
import org.teavm.backend.wasm.model.expression.WasmPush;
import org.teavm.backend.wasm.model.expression.WasmReferencesEqual;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmSequence;
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
import org.teavm.backend.wasm.model.expression.WasmTest;
import org.teavm.backend.wasm.model.expression.WasmThrow;
import org.teavm.backend.wasm.model.expression.WasmTry;
import org.teavm.backend.wasm.model.expression.WasmUnreachable;

public class WasmTypeInference implements WasmExpressionVisitor {
    private List<? extends WasmType> result;

    public List<? extends WasmType> getResult() {
        return result;
    }

    public WasmType getSingleResult() {
        if (result.isEmpty()) {
            return null;
        }
        if (result.size() != 1) {
            throw new IllegalStateException("Can't get single result from multi-value expression");
        }
        return result.get(0);
    }

    private void setSingleType(WasmType type) {
        result = type == null ? Collections.emptyList() : List.of(type);
    }

    private void setBlockType(WasmBlockType type) {
        if (type == null) {
            result = Collections.emptyList();
        } else if (type instanceof WasmBlockType.Function) {
            var function = ((WasmBlockType.Function) type).ref;
            result = function.getReturnTypes();
        } else {
            result = List.of(((WasmBlockType.Value) type).type);
        }
    }

    @Override
    public void visit(WasmBlock expression) {
        setBlockType(expression.getType());
    }

    @Override
    public void visit(WasmSequence expression) {
        if (expression.getBody().isEmpty()) {
            result = Collections.emptyList();
        } else {
            expression.getBody().getLast().acceptVisitor(this);
        }
    }

    @Override
    public void visit(WasmBranch expression) {
        if (expression.getResult() != null) {
            expression.getResult().acceptVisitor(this);
        } else {
            result = Collections.emptyList();
        }
    }

    @Override
    public void visit(WasmNullBranch expression) {
        if (expression.getCondition() == WasmNullCondition.NULL) {
            expression.getValue().acceptVisitor(this);
        } else {
            if (expression.getResult() != null) {
                expression.getResult().acceptVisitor(this);
            } else {
                result = Collections.emptyList();
            }
        }
    }

    @Override
    public void visit(WasmCastBranch expression) {
        switch (expression.getCondition()) {
            case SUCCESS:
                result = List.of(expression.getSourceType());
                break;
            case FAILURE:
                setSingleType(expression.getType());
                break;
        }
    }

    @Override
    public void visit(WasmBreak expression) {
        result = Collections.emptyList();
    }

    @Override
    public void visit(WasmSwitch expression) {
        result = Collections.emptyList();
    }

    @Override
    public void visit(WasmConditional expression) {
        setBlockType(expression.getType());
    }

    @Override
    public void visit(WasmReturn expression) {
        result = Collections.emptyList();
    }

    @Override
    public void visit(WasmUnreachable expression) {
        result = Collections.emptyList();
    }

    @Override
    public void visit(WasmInt32Constant expression) {
        result = List.of(WasmType.INT32);
    }

    @Override
    public void visit(WasmInt64Constant expression) {
        result = List.of(WasmType.INT64);
    }

    @Override
    public void visit(WasmFloat32Constant expression) {
        result = List.of(WasmType.FLOAT32);
    }

    @Override
    public void visit(WasmFloat64Constant expression) {
        result = List.of(WasmType.FLOAT64);
    }

    @Override
    public void visit(WasmGetLocal expression) {
        result = List.of(expression.getLocal().getType());
    }

    @Override
    public void visit(WasmSetLocal expression) {
        result = Collections.emptyList();
    }

    @Override
    public void visit(WasmGetGlobal expression) {
        result = List.of(expression.getGlobal().getType());
    }

    @Override
    public void visit(WasmSetGlobal expression) {
        result = Collections.emptyList();
    }

    @Override
    public void visit(WasmIntBinary expression) {
        result = List.of(map(expression.getType()));
    }

    @Override
    public void visit(WasmFloatBinary expression) {
        result = List.of(map(expression.getType()));
    }

    @Override
    public void visit(WasmIntUnary expression) {
        result = List.of(map(expression.getType()));
    }

    @Override
    public void visit(WasmFloatUnary expression) {
        result = List.of(map(expression.getType()));
    }

    @Override
    public void visit(WasmConversion expression) {
        result = List.of(WasmType.num(expression.getTargetType()));
    }

    @Override
    public void visit(WasmNullConstant expression) {
        result = List.of(expression.type);
    }


    @Override
    public void visit(WasmIsNull expression) {
        result = List.of(WasmType.INT32);
    }

    @Override
    public void visit(WasmCall expression) {
        var function = expression.getFunction();
        result = function == null ? null : function.getType().getReturnTypes();
    }

    @Override
    public void visit(WasmIndirectCall expression) {
        result = expression.getType().getReturnTypes();
    }

    @Override
    public void visit(WasmCallReference expression) {
        result = expression.getType().getReturnTypes();
    }

    @Override
    public void visit(WasmDrop expression) {
        result = Collections.emptyList();
    }

    @Override
    public void visit(WasmLoadInt32 expression) {
        result = List.of(WasmType.INT32);
    }

    @Override
    public void visit(WasmLoadInt64 expression) {
        result = List.of(WasmType.INT64);
    }

    @Override
    public void visit(WasmLoadFloat32 expression) {
        result = List.of(WasmType.FLOAT32);
    }

    @Override
    public void visit(WasmLoadFloat64 expression) {
        result = List.of(WasmType.FLOAT64);
    }

    @Override
    public void visit(WasmStoreInt32 expression) {
        result = Collections.emptyList();
    }

    @Override
    public void visit(WasmStoreInt64 expression) {
        result = Collections.emptyList();
    }

    @Override
    public void visit(WasmStoreFloat32 expression) {
        result = Collections.emptyList();
    }

    @Override
    public void visit(WasmStoreFloat64 expression) {
        result = Collections.emptyList();
    }

    @Override
    public void visit(WasmMemoryGrow expression) {
        result = List.of(WasmType.INT32);
    }

    @Override
    public void visit(WasmFill expression) {
        result = Collections.emptyList();
    }

    @Override
    public void visit(WasmCopy expression) {
        result = Collections.emptyList();
    }

    @Override
    public void visit(WasmTry expression) {
        result = List.of(expression.getType());
    }

    @Override
    public void visit(WasmThrow expression) {
        result = Collections.emptyList();
    }

    @Override
    public void visit(WasmReferencesEqual expression) {
        result = List.of(WasmType.INT32);
    }

    @Override
    public void visit(WasmCast expression) {
        result = List.of(expression.getTargetType());
    }

    @Override
    public void visit(WasmTest expression) {
        result = List.of(WasmType.INT32);
    }

    @Override
    public void visit(WasmExternConversion expression) {
        switch (expression.getType()) {
            case EXTERN_TO_ANY:
                result = List.of(WasmType.Reference.ANY);
                break;
            case ANY_TO_EXTERN:
                result = List.of(WasmType.Reference.EXTERN);
                break;
        }
    }

    @Override
    public void visit(WasmStructNew expression) {
        result = List.of(expression.getType().getReference());
    }

    @Override
    public void visit(WasmStructNewDefault expression) {
        result = List.of(expression.getType().getReference());
    }

    @Override
    public void visit(WasmStructGet expression) {
        result = List.of(expression.getType().getFields().get(expression.getFieldIndex()).getUnpackedType());
    }

    @Override
    public void visit(WasmStructSet expression) {
        result = Collections.emptyList();
    }

    @Override
    public void visit(WasmArrayNewDefault expression) {
        result = List.of(expression.getType().getReference());
    }

    @Override
    public void visit(WasmArrayNewFixed expression) {
        result = List.of(expression.getType().getReference());
    }

    @Override
    public void visit(WasmArrayGet expression) {
        result = List.of(expression.getType().getElementType().asUnpackedType());
    }

    @Override
    public void visit(WasmArraySet expression) {
        result = Collections.emptyList();
    }

    @Override
    public void visit(WasmArrayLength expression) {
        result = List.of(WasmType.INT32);
    }

    @Override
    public void visit(WasmArrayCopy expression) {
        result = Collections.emptyList();
    }

    @Override
    public void visit(WasmFunctionReference expression) {
        result = List.of(expression.getFunction().getType().getReference());
    }

    @Override
    public void visit(WasmInt31Get expression) {
        result = List.of(WasmType.INT32);
    }

    @Override
    public void visit(WasmInt31Reference expression) {
        result = List.of(WasmType.Reference.I31);
    }

    @Override
    public void visit(WasmPush expression) {
        result = Collections.emptyList();
    }

    @Override
    public void visit(WasmPop expression) {
        result = List.of(expression.getType());
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
