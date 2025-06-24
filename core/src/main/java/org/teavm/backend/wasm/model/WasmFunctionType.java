/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class WasmFunctionType extends WasmCompositeType {
    private List<? extends WasmType> parameterTypes;
    private List<? extends WasmType> returnTypes;
    private Supplier<List<? extends WasmType>> parameterTypesSupplier;
    private Supplier<List<? extends WasmType>> returnTypeSupplier;
    private Set<WasmFunctionType> supertypes = new LinkedHashSet<>();
    private boolean isFinal = true;
    private WasmBlockType.Function blockType;

    public WasmFunctionType(String name, WasmType returnType, List<? extends WasmType> parameterTypes) {
        super(name);
        this.returnTypes = returnType != null ? List.of(returnType) : Collections.emptyList();
        this.parameterTypes = parameterTypes;
    }

    public WasmFunctionType(String name, List<? extends WasmType> returnTypes,
            List<? extends WasmType> parameterTypes) {
        super(name);
        this.returnTypes = returnTypes;
        this.parameterTypes = parameterTypes;
    }

    public WasmFunctionType(String name, Supplier<List<? extends WasmType>> returnTypeSupplier,
            Supplier<List<? extends WasmType>> parameterTypesSupplier) {
        super(name);
        this.returnTypeSupplier = returnTypeSupplier;
        this.parameterTypesSupplier = parameterTypesSupplier;
    }

    public List<? extends WasmType> getParameterTypes() {
        if (parameterTypes == null) {
            parameterTypes = List.copyOf(parameterTypesSupplier.get());
            parameterTypesSupplier = null;
        }
        return parameterTypes;
    }

    public List<? extends WasmType> getReturnTypes() {
        if (returnTypeSupplier != null) {
            returnTypes = returnTypeSupplier.get();
            returnTypeSupplier = null;
        }
        return returnTypes;
    }

    public WasmType getSingleReturnType() {
        if (returnTypes.isEmpty()) {
            return null;
        }
        if (returnTypes.size() != 1) {
            throw new IllegalStateException("Function produces more that one value");
        }
        return returnTypes.get(0);
    }

    public Set<WasmFunctionType> getSupertypes() {
        return supertypes;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }

    public WasmBlockType.Function asBlock() {
        if (blockType == null) {
            blockType = new WasmBlockType.Function(this);
        }
        return blockType;
    }

    @Override
    public void acceptVisitor(WasmCompositeTypeVisitor visitor) {
        visitor.visit(this);
    }
}
