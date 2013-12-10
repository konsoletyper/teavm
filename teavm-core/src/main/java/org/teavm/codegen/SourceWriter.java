package org.teavm.codegen;

import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class SourceWriter {
    private StringBuilder sb = new StringBuilder();
    private int indentSize = 0;
    private NamingStrategy naming;
    private boolean lineStart;
    private boolean minified;

    SourceWriter(NamingStrategy naming) {
        this.naming = naming;
    }

    void setMinified(boolean minified) {
        this.minified = minified;
    }

    public void clear() {
        sb.setLength(0);
    }

    public SourceWriter append(String value) {
        appendIndent();
        sb.append(value);
        return this;
    }

    public SourceWriter append(Object value) {
        return append(String.valueOf(value));
    }

    public SourceWriter append(int value) {
        return append(String.valueOf(value));
    }

    public SourceWriter append(char value) {
        return append(String.valueOf(value));
    }

    public SourceWriter appendClass(String cls) throws NamingException {
        return append(naming.getNameFor(cls));
    }

    public SourceWriter appendField(FieldReference field) throws NamingException {
        return append(naming.getNameFor(field));
    }

    public SourceWriter appendMethod(MethodReference method) throws NamingException {
        return append(naming.getNameFor(method));
    }

    public SourceWriter appendMethodBody(MethodReference method) throws NamingException {
        return append(naming.getFullNameFor(method));
    }

    private void appendIndent() {
        if (minified) {
            return;
        }
        if (lineStart) {
            for (int i = 0; i < indentSize; ++i) {
                sb.append("    ");
            }
            lineStart = false;
        }
    }

    public SourceWriter newLine() {
        sb.append('\n');
        lineStart = true;
        return this;
    }

    public SourceWriter ws() {
        if (!minified) {
            sb.append(' ');
        }
        return this;
    }

    public SourceWriter softNewLine() {
        if (!minified) {
            sb.append('\n');
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
    public String toString() {
        return sb.toString();
    }
}
