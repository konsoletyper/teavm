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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class WasmFunctionType extends WasmCompositeType {
    private List<? extends WasmType> parameterTypes;
    private WasmType returnType;
    private Supplier<List<? extends WasmType>> parameterTypesSupplier;
    private Supplier<WasmType> returnTypeSupplier;
    private Set<WasmFunctionType> supertypes = new LinkedHashSet<>();
    private boolean isFinal = true;

    public WasmFunctionType(String name, WasmType returnType, List<? extends WasmType> parameterTypes) {
        super(name);
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
    }

    public WasmFunctionType(String name, Supplier<WasmType> returnTypeSupplier,
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

    public WasmType getReturnType() {
        if (returnTypeSupplier != null) {
            returnType = returnTypeSupplier.get();
            returnTypeSupplier = null;
        }
        return returnType;
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

    @Override
    public void acceptVisitor(WasmCompositeTypeVisitor visitor) {
        visitor.visit(this);
    }
}
