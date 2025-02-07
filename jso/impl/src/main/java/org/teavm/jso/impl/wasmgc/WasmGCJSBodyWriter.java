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
package org.teavm.jso.impl.wasmgc;

import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

class WasmGCJSBodyWriter extends SourceWriter {
    final StringBuilder sb = new StringBuilder();

    @Override
    public SourceWriter append(char value) {
        sb.append(value);
        return this;
    }

    @Override
    public SourceWriter append(CharSequence csq, int start, int end) {
        sb.append(csq, start, end);
        return this;
    }

    @Override
    public SourceWriter appendClass(String cls) {
        return this;
    }

    @Override
    public SourceWriter appendField(FieldReference field) {
        return this;
    }

    @Override
    public SourceWriter appendStaticField(FieldReference field) {
        return this;
    }

    @Override
    public SourceWriter appendVirtualMethod(MethodDescriptor method) {
        return this;
    }

    @Override
    public SourceWriter appendMethod(MethodReference method) {
        return this;
    }

    @Override
    public SourceWriter appendFunction(String name) {
        return this;
    }

    @Override
    public SourceWriter startFunctionDeclaration() {
        return this;
    }

    @Override
    public SourceWriter startVariableDeclaration() {
        return this;
    }

    @Override
    public SourceWriter endDeclaration() {
        return this;
    }

    @Override
    public SourceWriter declareVariable() {
        return this;
    }

    @Override
    public SourceWriter appendGlobal(String name) {
        sb.append(name);
        return this;
    }

    @Override
    public SourceWriter appendInit(MethodReference method) {
        return this;
    }

    @Override
    public SourceWriter appendClassInit(String className) {
        return this;
    }

    @Override
    public SourceWriter newLine() {
        sb.append('\n');
        return this;
    }

    @Override
    public SourceWriter ws() {
        return this;
    }

    @Override
    public SourceWriter sameLineWs() {
        return this;
    }

    @Override
    public SourceWriter tokenBoundary() {
        return this;
    }

    @Override
    public SourceWriter softNewLine() {
        return this;
    }

    @Override
    public SourceWriter indent() {
        return this;
    }

    @Override
    public SourceWriter outdent() {
        return this;
    }

    @Override
    public SourceWriter emitLocation(String fileName, int line) {
        return this;
    }

    @Override
    public SourceWriter enterLocation() {
        return this;
    }

    @Override
    public SourceWriter exitLocation() {
        return this;
    }

    @Override
    public SourceWriter emitStatementStart() {
        return this;
    }

    @Override
    public SourceWriter emitVariables(String[] names, String jsName) {
        return this;
    }

    @Override
    public void emitMethod(MethodDescriptor method) {
    }

    @Override
    public void emitClass(String className) {
    }

    @Override
    public void markClassStart(String className) {
    }

    @Override
    public void markClassEnd() {
    }

    @Override
    public void markSectionStart(int id) {
    }

    @Override
    public void markSectionEnd() {
    }
}
