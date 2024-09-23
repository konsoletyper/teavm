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
package org.teavm.backend.wasm.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.model.MethodReference;

public class WasmFunction extends WasmEntity {
    private String name;
    private String exportName;
    private String importName;
    private String importModule;
    private boolean referenced;
    private WasmFunctionType type;
    private List<WasmLocal> localVariables = new ArrayList<>();
    private List<WasmLocal> readonlyLocalVariables = Collections.unmodifiableList(localVariables);
    private List<WasmExpression> body = new ArrayList<>();
    private MethodReference javaMethod;

    public WasmFunction(WasmFunctionType type) {
        this.type = Objects.requireNonNull(type);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExportName() {
        return exportName;
    }

    public void setExportName(String exportName) {
        this.exportName = exportName;
    }

    public String getImportName() {
        return importName;
    }

    public void setImportName(String importName) {
        this.importName = importName;
        if (collection != null) {
            collection.invalidateIndexes();
        }
    }

    public String getImportModule() {
        return importModule;
    }

    public void setImportModule(String importModule) {
        this.importModule = importModule;
    }

    public boolean isReferenced() {
        return referenced;
    }

    public void setReferenced(boolean referenced) {
        this.referenced = referenced;
    }

    @Override
    boolean isImported() {
        return importName != null;
    }

    public WasmFunctionType getType() {
        return type;
    }

    public void setType(WasmFunctionType type) {
        this.type = Objects.requireNonNull(type);
    }

    public List<WasmLocal> getLocalVariables() {
        return readonlyLocalVariables;
    }

    public List<WasmExpression> getBody() {
        return body;
    }

    public void add(WasmLocal local) {
        if (local.function != null) {
            throw new IllegalArgumentException("This local is already registered in another function");
        }
        local.function = this;
        local.index = localVariables.size();
        localVariables.add(local);
    }

    public MethodReference getJavaMethod() {
        return javaMethod;
    }

    public void setJavaMethod(MethodReference javaMethod) {
        this.javaMethod = javaMethod;
    }
}
