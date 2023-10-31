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

public abstract class SourceWriter implements Appendable {
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

    public abstract SourceWriter appendClass(String cls);

    public SourceWriter appendClass(Class<?> cls) {
        return appendClass(cls.getName());
    }

    public abstract SourceWriter appendField(FieldReference field);

    public abstract SourceWriter appendStaticField(FieldReference field);

    public abstract SourceWriter appendMethod(MethodDescriptor method);

    public SourceWriter appendMethod(String name, Class<?>... params) {
        return appendMethod(new MethodDescriptor(name, params));
    }

    public abstract SourceWriter appendMethodBody(MethodReference method);

    public SourceWriter appendMethodBody(String className, String name, ValueType... params) {
        return appendMethodBody(new MethodReference(className, new MethodDescriptor(name, params)));
    }

    public SourceWriter appendMethodBody(Class<?> cls, String name, Class<?>... params) {
        return appendMethodBody(new MethodReference(cls, name, params));
    }

    public abstract SourceWriter appendFunction(String name);

    public abstract SourceWriter appendInit(MethodReference method);

    public abstract SourceWriter appendClassInit(String className);

    public abstract SourceWriter newLine();

    public abstract SourceWriter ws();

    public abstract SourceWriter tokenBoundary();

    public abstract SourceWriter softNewLine();

    public abstract SourceWriter indent();

    public abstract SourceWriter outdent();

    public abstract SourceWriter emitLocation(String fileName, int line);

    public abstract SourceWriter enterLocation();

    public abstract SourceWriter exitLocation();

    public abstract SourceWriter emitStatementStart();

    public abstract void emitMethod(MethodDescriptor method);

    public abstract void emitClass(String className);
}
