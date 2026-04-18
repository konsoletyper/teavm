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

import java.util.Objects;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;

public class WasmGlobal extends WasmEntity {
    private String name;
    private WasmType type;
    private WasmInstructionList initialValue = new WasmInstructionList();
    private boolean immutable;
    private String exportName;
    private String importName;
    private String importModule;

    public WasmGlobal(String name, WasmType type) {
        this.name = name;
        this.type = Objects.requireNonNull(type);
    }

    public String getName() {
        return name;
    }

    public WasmType getType() {
        return type;
    }

    public void setType(WasmType type) {
        this.type = Objects.requireNonNull(type);
    }

    public WasmInstructionList getInitialValue() {
        return initialValue;
    }

    public boolean isImmutable() {
        return immutable;
    }

    public void setImmutable(boolean immutable) {
        this.immutable = immutable;
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

    @Override
    boolean isImported() {
        return importName != null;
    }
}
