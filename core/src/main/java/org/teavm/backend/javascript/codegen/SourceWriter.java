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
    private int offset;

    SourceWriter(NamingStrategy naming, Appendable innerWriter, int lineWidth) {
        this.naming = naming;
        this.innerWriter = innerWriter;
        this.lineWidth = lineWidth;
    }

    void setMinified(boolean minified) {
        this.minified = minified;
    }

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
    public SourceWriter append(char value) {
        appendIndent();
        try {
            innerWriter.append(value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (value == '\n') {
            newLine();
        } else {
            column++;
            offset++;
        }
        return this;
    }

    @Override
    public SourceWriter append(CharSequence csq) {
        append(csq, 0, csq.length());
        return this;
    }

    @Override
    public SourceWriter append(CharSequence csq, int start, int end) {
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

    private void appendSingleLine(CharSequence csq, int start, int end) {
        if (start == end) {
            return;
        }
        appendIndent();
        column += end - start;
        offset += end - start;
        try {
            innerWriter.append(csq, start, end);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SourceWriter appendClass(String cls) {
        return appendName(naming.getNameFor(cls));
    }

    public SourceWriter appendClass(Class<?> cls) {
        return appendClass(cls.getName());
    }

    public SourceWriter appendField(FieldReference field) {
        return append(naming.getNameFor(field));
    }

    public SourceWriter appendStaticField(FieldReference field) {
        return appendName(naming.getFullNameFor(field));
    }

    public SourceWriter appendMethod(MethodDescriptor method) {
        return append(naming.getNameFor(method));
    }

    public SourceWriter appendMethod(String name, Class<?>... params) {
        return append(naming.getNameFor(new MethodDescriptor(name, params)));
    }

    public SourceWriter appendMethodBody(MethodReference method) {
        return appendName(naming.getFullNameFor(method));
    }

    public SourceWriter appendMethodBody(String className, String name, ValueType... params) {
        return appendMethodBody(new MethodReference(className, new MethodDescriptor(name, params)));
    }

    public SourceWriter appendMethodBody(Class<?> cls, String name, Class<?>... params) {
        return appendMethodBody(new MethodReference(cls, name, params));
    }

    public SourceWriter appendFunction(String name) {
        return append(naming.getNameForFunction(name));
    }

    public SourceWriter appendInit(MethodReference method) {
        return appendName(naming.getNameForInit(method));
    }

    public SourceWriter appendClassInit(String className) {
        return appendName(naming.getNameForClassInit(className));
    }

    private SourceWriter appendName(ScopedName name) {
        if (name.scoped) {
            append(naming.getScopeName()).append(".");
        }
        append(name.value);
        return this;
    }

    private void appendIndent() {
        if (minified) {
            return;
        }
        if (lineStart) {
            try {
                for (int i = 0; i < indentSize; ++i) {
                    innerWriter.append("    ");
                    column += 4;
                    offset += 4;
                }
                lineStart = false;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public SourceWriter newLine() {
        try {
            innerWriter.append('\n');
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        column = 0;
        ++line;
        ++offset;
        lineStart = true;
        return this;
    }

    public SourceWriter ws() {
        if (column >= lineWidth) {
            newLine();
        } else {
            if (!minified) {
                try {
                    innerWriter.append(' ');
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                column++;
                offset++;
            }
        }
        return this;
    }

    public SourceWriter tokenBoundary() {
        if (column >= lineWidth) {
            newLine();
        }
        return this;
    }

    public SourceWriter softNewLine() {
        if (!minified) {
            try {
                innerWriter.append('\n');
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            column = 0;
            ++offset;
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

    @Override
    public int getOffset() {
        return offset;
    }
}
