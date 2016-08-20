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

import java.io.IOException;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class SourceWriter implements Appendable, LocationProvider {
    private final Appendable innerWriter;
    private int indentSize;
    private final NamingStrategy naming;
    private boolean lineStart;
    private boolean minified;
    private final int lineWidth;
    private int column;
    private int line;

    SourceWriter(NamingStrategy naming, Appendable innerWriter, int lineWidth) {
        this.naming = naming;
        this.innerWriter = innerWriter;
        this.lineWidth = lineWidth;
    }

    void setMinified(boolean minified) {
        this.minified = minified;
    }

    public SourceWriter append(String value) throws IOException {
        append((CharSequence) value);
        return this;
    }

    public SourceWriter append(Object value) throws IOException {
        return append(String.valueOf(value));
    }

    public SourceWriter append(int value) throws IOException {
        return append(String.valueOf(value));
    }

    @Override
    public SourceWriter append(char value) throws IOException {
        appendIndent();
        innerWriter.append(value);
        if (value == '\n') {
            newLine();
        } else {
            column++;
        }
        return this;
    }

    @Override
    public SourceWriter append(CharSequence csq) throws IOException {
        append(csq, 0, csq.length());
        return this;
    }

    @Override
    public SourceWriter append(CharSequence csq, int start, int end) throws IOException {
        int last = start;
        for (int i = start; i < end; ++i) {
            if (csq.charAt(i) == '\n') {
                appendSingleLine(csq, last, i);
                newLine();
                last = i + 1;
            }
        }
        appendSingleLine(csq, last, end);
        return this;
    }

    private void appendSingleLine(CharSequence csq, int start, int end) throws IOException {
        if (start == end) {
            return;
        }
        appendIndent();
        column += end - start;
        innerWriter.append(csq, start, end);
    }

    public SourceWriter appendClass(String cls) throws NamingException, IOException {
        return append(naming.getNameFor(cls));
    }

    public SourceWriter appendClass(Class<?> cls) throws NamingException, IOException {
        return append(naming.getNameFor(cls.getName()));
    }

    public SourceWriter appendField(FieldReference field) throws NamingException, IOException {
        return append(naming.getNameFor(field));
    }

    public SourceWriter appendStaticField(FieldReference field) throws NamingException, IOException {
        return append(naming.getFullNameFor(field));
    }

    public SourceWriter appendMethod(MethodDescriptor method) throws NamingException, IOException {
        return append(naming.getNameFor(method));
    }

    public SourceWriter appendMethod(String name, ValueType... params) throws NamingException, IOException {
        return append(naming.getNameFor(new MethodDescriptor(name, params)));
    }

    public SourceWriter appendMethod(String name, Class<?>... params) throws NamingException, IOException {
        return append(naming.getNameFor(new MethodDescriptor(name, params)));
    }

    public SourceWriter appendMethodBody(MethodReference method) throws NamingException, IOException {
        return append(naming.getFullNameFor(method));
    }

    public SourceWriter appendMethodBody(String className, String name, ValueType... params)
            throws NamingException, IOException {
        return append(naming.getFullNameFor(new MethodReference(className, new MethodDescriptor(name, params))));
    }

    public SourceWriter appendMethodBody(Class<?> cls, String name, Class<?>... params)
            throws NamingException, IOException {
        return append(naming.getFullNameFor(new MethodReference(cls, name, params)));
    }

    public SourceWriter appendFunction(String name) throws NamingException, IOException {
        return append(naming.getNameForFunction(name));
    }

    private void appendIndent() throws IOException {
        if (minified) {
            return;
        }
        if (lineStart) {
            for (int i = 0; i < indentSize; ++i) {
                innerWriter.append("    ");
                column += 4;
            }
            lineStart = false;
        }
    }

    public SourceWriter newLine() throws IOException {
        innerWriter.append('\n');
        column = 0;
        ++line;
        lineStart = true;
        return this;
    }

    public SourceWriter ws() throws IOException {
        if (column >= lineWidth) {
            newLine();
        } else {
            if (!minified) {
                innerWriter.append(' ');
                column++;
            }
        }
        return this;
    }

    public SourceWriter tokenBoundary() throws IOException {
        if (column >= lineWidth) {
            newLine();
        }
        return this;
    }

    public SourceWriter softNewLine() throws IOException {
        if (!minified) {
            innerWriter.append('\n');
            column = 0;
            ++line;
            lineStart = true;
        }
        return this;
    }

    public SourceWriter indent() {
        ++indentSize;
        return this;
    }

    public SourceWriter outdent() {
        --indentSize;
        return this;
    }

    public NamingStrategy getNaming() {
        return naming;
    }

    @Override
    public int getColumn() {
        return column;
    }

    @Override
    public int getLine() {
        return line;
    }
}
