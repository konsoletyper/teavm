/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.codegen;

import java.io.IOException;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

/**
 *
 * @author Alexey Andreev
 */
public class SourceWriter implements Appendable {
    private Appendable innerWriter;
    private int indentSize = 0;
    private NamingStrategy naming;
    private boolean lineStart;
    private boolean minified;

    SourceWriter(NamingStrategy naming, Appendable innerWriter) {
        this.naming = naming;
        this.innerWriter = innerWriter;
    }

    void setMinified(boolean minified) {
        this.minified = minified;
    }

    public SourceWriter append(String value) throws IOException {
        appendIndent();
        innerWriter.append(value);
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
        return this;
    }

    @Override
    public SourceWriter append(CharSequence csq) throws IOException {
        appendIndent();
        innerWriter.append(csq);
        return this;
    }

    @Override
    public SourceWriter append(CharSequence csq, int start, int end) throws IOException {
        appendIndent();
        innerWriter.append(csq, start, end);
        return this;
    }

    public SourceWriter appendClass(String cls) throws NamingException, IOException {
        return append(naming.getNameFor(cls));
    }

    public SourceWriter appendField(FieldReference field) throws NamingException, IOException {
        return append(naming.getNameFor(field));
    }

    public SourceWriter appendMethod(MethodReference method) throws NamingException, IOException {
        return append(naming.getNameFor(method));
    }

    public SourceWriter appendMethod(String className, String name, ValueType... params)
            throws NamingException, IOException {
        return append(naming.getNameFor(new MethodReference(className, new MethodDescriptor(name, params))));
    }

    public SourceWriter appendMethodBody(MethodReference method) throws NamingException, IOException {
        return append(naming.getFullNameFor(method));
    }

    public SourceWriter appendMethodBody(String className, String name, ValueType... params)
            throws NamingException, IOException {
        return append(naming.getFullNameFor(new MethodReference(className, new MethodDescriptor(name, params))));
    }

    private void appendIndent() throws IOException {
        if (minified) {
            return;
        }
        if (lineStart) {
            for (int i = 0; i < indentSize; ++i) {
                innerWriter.append("    ");
            }
            lineStart = false;
        }
    }

    public SourceWriter newLine() throws IOException{
        innerWriter.append('\n');
        lineStart = true;
        return this;
    }

    public SourceWriter ws() throws IOException{
        if (!minified) {
            innerWriter.append(' ');
        }
        return this;
    }

    public SourceWriter softNewLine() throws IOException{
        if (!minified) {
            innerWriter.append('\n');
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
}
