/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.backend.javascript.codegen;

import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

public interface SourceWriterSink {
    default SourceWriterSink append(CharSequence csq, int start, int end) {
        return this;
    }

    default SourceWriterSink appendClass(String cls) {
        return this;
    }

    default SourceWriterSink appendField(FieldReference field) {
        return this;
    }

    default SourceWriterSink appendStaticField(FieldReference field) {
        return this;
    }

    default SourceWriterSink appendMethod(MethodDescriptor method) {
        return this;
    }

    default SourceWriterSink appendMethodBody(MethodReference method) {
        return this;
    }

    default SourceWriterSink appendFunction(String name) {
        return this;
    }

    default SourceWriterSink appendGlobal(String name) {
        return this;
    }

    default SourceWriterSink appendInit(MethodReference method) {
        return this;
    }

    default SourceWriterSink appendClassInit(String className) {
        return this;
    }

    default SourceWriterSink newLine() {
        return this;
    }

    default SourceWriterSink ws() {
        return this;
    }

    default SourceWriterSink sameLineWs() {
        return this;
    }

    default SourceWriterSink tokenBoundary() {
        return this;
    }

    default SourceWriterSink softNewLine() {
        return this;
    }

    default SourceWriterSink indent() {
        return this;
    }

    default SourceWriterSink outdent() {
        return this;
    }

    default SourceWriterSink emitLocation(String fileName, int line) {
        return this;
    }

    default SourceWriterSink enterLocation() {
        return this;
    }

    default SourceWriterSink exitLocation() {
        return this;
    }

    default SourceWriterSink emitStatementStart() {
        return this;
    }

    default SourceWriterSink emitVariables(String[] names, String jsName) {
        return this;
    }

    default void emitMethod(MethodDescriptor method) {
    }

    default void emitClass(String className) {
    }

    default void markClassStart(String className) {
    }

    default void markClassEnd() {
    }

    default void markSectionStart(int id) {
    }

    default void markSectionEnd() {
    }
}
