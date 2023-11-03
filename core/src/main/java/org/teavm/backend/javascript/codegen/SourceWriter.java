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
package org.teavm.backend.javascript.codegen;

import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public abstract class SourceWriter implements Appendable, SourceWriterSink {
    public SourceWriter append(String value) {
        append((CharSequence) value);
        return this;
    }

    public SourceWriter appendBlockStart() {
        return ws().append("{").indent().softNewLine();
    }

    public SourceWriter appendBlockEnd() {
        return outdent().append("}").softNewLine();
    }

    public SourceWriter appendIf() {
        return append("if").ws().append("(");
    }

    public SourceWriter appendElseIf() {
        return outdent().append("}").ws().append("else ").appendIf();
    }

    public SourceWriter appendElse() {
        return outdent().append("}").ws().append("else").appendBlockStart();
    }

    public SourceWriter append(int value) {
        return append(String.valueOf(value));
    }

    @Override
    public abstract SourceWriter append(char value);

    @Override
    public SourceWriter append(CharSequence csq) {
        append(csq, 0, csq.length());
        return this;
    }

    @Override
    public abstract SourceWriter append(CharSequence csq, int start, int end);

    @Override
    public abstract SourceWriter appendClass(String cls);

    public SourceWriter appendClass(Class<?> cls) {
        return appendClass(cls.getName());
    }

    @Override
    public abstract SourceWriter appendField(FieldReference field);

    @Override
    public abstract SourceWriter appendStaticField(FieldReference field);

    @Override
    public abstract SourceWriter appendMethod(MethodDescriptor method);

    public SourceWriter appendMethod(String name, Class<?>... params) {
        return appendMethod(new MethodDescriptor(name, params));
    }

    @Override
    public abstract SourceWriter appendMethodBody(MethodReference method);

    public SourceWriter appendMethodBody(String className, String name, ValueType... params) {
        return appendMethodBody(new MethodReference(className, new MethodDescriptor(name, params)));
    }

    public SourceWriter appendMethodBody(Class<?> cls, String name, Class<?>... params) {
        return appendMethodBody(new MethodReference(cls, name, params));
    }

    @Override
    public abstract SourceWriter appendFunction(String name);

    @Override
    public abstract SourceWriter appendGlobal(String name);

    @Override
    public abstract SourceWriter appendInit(MethodReference method);

    @Override
    public abstract SourceWriter appendClassInit(String className);

    @Override
    public abstract SourceWriter newLine();

    @Override
    public abstract SourceWriter ws();

    @Override
    public abstract SourceWriter sameLineWs();

    @Override
    public abstract SourceWriter tokenBoundary();

    @Override
    public abstract SourceWriter softNewLine();

    @Override
    public abstract SourceWriter indent();

    @Override
    public abstract SourceWriter outdent();

    @Override
    public abstract SourceWriter emitLocation(String fileName, int line);

    @Override
    public abstract SourceWriter enterLocation();

    @Override
    public abstract SourceWriter exitLocation();

    @Override
    public abstract SourceWriter emitStatementStart();

    @Override
    public abstract SourceWriter emitVariables(String[] names, String jsName);

    @Override
    public abstract void emitMethod(MethodDescriptor method);

    @Override
    public abstract void emitClass(String className);

    @Override
    public abstract void markClassStart(String className);

    @Override
    public abstract void markClassEnd();

    @Override
    public abstract void markSectionStart(int id);

    @Override
    public abstract void markSectionEnd();
}
