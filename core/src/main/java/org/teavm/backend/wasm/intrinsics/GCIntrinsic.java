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
import org.teavm.backend.wasm.WasmHeap;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmConversion;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmLoadInt32;
import org.teavm.backend.wasm.model.expression.WasmStoreInt32;
import org.teavm.backend.wasm.model.expression.WasmUnreachable;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.runtime.GC;

public class GCIntrinsic implements WasmIntrinsic {
    private static final MethodReference PRINT_OUT_OF_MEMORY = new MethodReference(
            WasmRuntime.class, "printOutOfMemory", void.class);
    private static final MethodReference RESIZE_HEAP = new MethodReference(
            WasmHeap.class, "resizeHeap", int.class, void.class);
    private static final FieldReference CARD_TABLE = new FieldReference(WasmHeap.class.getName(), "cardTable");
    private static final FieldReference HEAP_ADDRESS = new FieldReference(WasmHeap.class.getName(), "heapAddress");
    public final List<WasmInt32Constant> regionSizeExpressions = new ArrayList<>();

    public void setRegionSize(int regionSize) {
        for (WasmInt32Constant constant : regionSizeExpressions) {
            constant.setValue(regionSize);
        }
    }

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        if (!methodReference.getClassName().equals(GC.class.getName())) {
            return false;
        }

        switch (methodReference.getName()) {
            case "gcStorageAddress":
            case "gcStorageSize":
            case "heapAddress":
            case "availableBytes":
            case "regionsAddress":
            case "cardTable":
            case "regionMaxCount":
            case "regionSize":
            case "outOfMemory":
            case "minAvailableBytes":
            case "maxAvailableBytes":
            case "resizeHeap":
            case "writeBarrier":
                return true;
            default:
                return false;
        }
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        switch (invocation.getMethod().getName()) {
            case "gcStorageAddress":
                return getStaticField(manager, "storageAddress");
            case "gcStorageSize":
                return getStaticField(manager, "storageSize");
            case "heapAddress":
                return getStaticField(manager, "heapAddress");
            case "regionsAddress":
                return getStaticField(manager, "regionsAddress");
            case "cardTable":
                return getStaticField(manager, "cardTable");
            case "regionMaxCount":
                return getStaticField(manager, "regionsCount");
            case "minAvailableBytes":
                return intToLong(getStaticField(manager, "minHeapSize"));
            case "maxAvailableBytes":
                return intToLong(getStaticField(manager, "maxHeapSize"));
            case "resizeHeap": {
                WasmExpression amount = manager.generate(invocation.getArguments().get(0));
                amount = new WasmConversion(WasmType.INT64, WasmType.INT32, false, amount);
                return new WasmCall(manager.getNames().forMethod(RESIZE_HEAP), amount);
            }
            case "regionSize": {
                WasmInt32Constant result = new WasmInt32Constant(0);
                regionSizeExpressions.add(result);
                return result;
            }
            case "availableBytes":
                return intToLong(getStaticField(manager, "heapSize"));
            case "outOfMemory": {
                WasmBlock block = new WasmBlock(false);
                WasmCall call = new WasmCall(manager.getNames().forMethod(PRINT_OUT_OF_MEMORY), true);
                block.getBody().add(call);
                block.getBody().add(new WasmUnreachable());
                return block;
            }
            case "writeBarrier": {
                WasmExpression cardTableField = new WasmInt32Constant(manager.getStaticField(CARD_TABLE));
                WasmExpression cardTable = new WasmLoadInt32(4, cardTableField, WasmInt32Subtype.INT32);
                WasmExpression heapAddressField = new WasmInt32Constant(manager.getStaticField(HEAP_ADDRESS));
                WasmExpression heapAddress = new WasmLoadInt32(4, heapAddressField, WasmInt32Subtype.INT32);
                WasmInt32Constant regionSize = new WasmInt32Constant(0);
                regionSizeExpressions.add(regionSize);
                WasmExpression offsetInHeap = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB,
                        manager.generate(invocation.getArguments().get(0)), heapAddress);
                WasmExpression cardIndex = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.DIV_SIGNED,
                        offsetInHeap, regionSize);
                WasmExpression card = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, cardTable,
                        cardIndex);
                return new WasmStoreInt32(1, card, new WasmInt32Constant(0), WasmInt32Subtype.INT8);
            }
            default:
                throw new IllegalArgumentException(invocation.getMethod().toString());
        }
    }

    private static WasmExpression getStaticField(WasmIntrinsicManager manager, String fieldName) {
        int address = manager.getStaticField(new FieldReference(WasmHeap.class.getName(), fieldName));
        return new WasmLoadInt32(4, new WasmInt32Constant(address), WasmInt32Subtype.INT32);
    }

    private static WasmExpression intToLong(WasmExpression expression) {
        return new WasmConversion(WasmType.INT32, WasmType.INT64, false, expression);
    }
}
