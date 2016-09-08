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
import org.teavm.backend.wasm.model.expression.WasmInt64Constant;
import org.teavm.model.MethodReference;
import org.teavm.runtime.GC;

public class GCIntrinsic implements WasmIntrinsic {
    private List<WasmInt32Constant> heapAddressExpressions = new ArrayList<>();
    private List<WasmInt64Constant> availableBytesExpressions = new ArrayList<>();
    private List<WasmInt32Constant> gcStorageAddressExpressions = new ArrayList<>();
    private List<WasmInt32Constant> gcStorageSizeExpressions = new ArrayList<>();
    private List<WasmInt32Constant> regionSizeExpressions = new ArrayList<>();
    private List<WasmInt32Constant> regionsAddressExpressions = new ArrayList<>();
    private List<WasmInt32Constant> regionMaxCountExpressions = new ArrayList<>();

    public void setHeapAddress(int address) {
        for (WasmInt32Constant constant : heapAddressExpressions) {
            constant.setValue(address);
        }
    }

    public void setAvailableBytes(long availableBytes) {
        for (WasmInt64Constant constant : availableBytesExpressions) {
            constant.setValue(availableBytes);
        }
    }

    public void setGCStorageAddress(int address) {
        for (WasmInt32Constant constant : gcStorageAddressExpressions) {
            constant.setValue(address);
        }
    }

    public void setGCStorageSize(int storageSize) {
        for (WasmInt32Constant constant : gcStorageSizeExpressions) {
            constant.setValue(storageSize);
        }
    }

    public void setRegionSize(int regionSize) {
        for (WasmInt32Constant constant : regionSizeExpressions) {
            constant.setValue(regionSize);
        }
    }

    public void setRegionsAddress(int address) {
        for (WasmInt32Constant constant : regionsAddressExpressions) {
            constant.setValue(address);
        }
    }

    public void setRegionMaxCount(int maxCount) {
        for (WasmInt32Constant constant : regionMaxCountExpressions) {
            constant.setValue(maxCount);
        }
    }

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        if (!methodReference.getClassName().endsWith(GC.class.getName())) {
            return false;
        }

        switch (methodReference.getName()) {
            case "gcStorageAddress":
            case "gcStorageSize":
            case "heapAddress":
            case "availableBytes":
            case "regionsAddress":
            case "regionMaxCount":
            case "regionSize":
                return true;
            default:
                return false;
        }
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        List<WasmInt32Constant> list;
        switch (invocation.getMethod().getName()) {
            case "gcStorageAddress":
                list = gcStorageAddressExpressions;
                break;
            case "gcStorageSize":
                list = gcStorageSizeExpressions;
                break;
            case "heapAddress":
                list = heapAddressExpressions;
                break;
            case "regionsAddress":
                list = regionsAddressExpressions;
                break;
            case "regionMaxCount":
                list = regionMaxCountExpressions;
                break;
            case "regionSize":
                list = regionSizeExpressions;
                break;
            case "availableBytes": {
                WasmInt64Constant constant = new WasmInt64Constant(0);
                availableBytesExpressions.add(constant);
                return constant;
            }
            default:
                throw new IllegalArgumentException(invocation.getMethod().toString());
        }
        WasmInt32Constant result = new WasmInt32Constant(0);
        list.add(result);
        return result;
    }
}
