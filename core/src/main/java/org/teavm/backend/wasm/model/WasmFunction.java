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

public class WasmFunction {
    WasmModule module;
    private String name;
    private String exportName;
    private String importName;
    private String importModule;
    private List<WasmType> parameters = new ArrayList<>();
    private WasmType result;
    private List<WasmLocal> localVariables = new ArrayList<>();
    private List<WasmLocal> readonlyLocalVariables = Collections.unmodifiableList(localVariables);
    private List<WasmExpression> body = new ArrayList<>();
    private MethodReference javaMethod;

    public WasmFunction(String name) {
        Objects.requireNonNull(name);
        this.name = name;
    }

    public WasmModule getModule() {
        return module;
    }

    public String getName() {
        return name;
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
    }

    public String getImportModule() {
        return importModule;
    }

    public void setImportModule(String importModule) {
        this.importModule = importModule;
    }

    public WasmType getResult() {
        return result;
    }

    public void setResult(WasmType result) {
        this.result = result;
    }

    public List<WasmType> getParameters() {
        return parameters;
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
