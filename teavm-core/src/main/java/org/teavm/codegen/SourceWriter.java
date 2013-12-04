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

    public SourceWriter(NamingStrategy naming) {
        this.naming = naming;
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
        appendIndent();
        sb.append(value);
        return this;
    }

    public SourceWriter append(int value) {
        appendIndent();
        sb.append(value);
        return this;
    }

    public SourceWriter append(char value) {
        appendIndent();
        sb.append(value);
        return this;
    }

    public SourceWriter appendClass(String cls) throws NamingException {
        appendIndent();
        sb.append(naming.getNameFor(cls));
        return this;
    }

    public SourceWriter appendField(FieldReference field) throws NamingException {
        appendIndent();
        sb.append(naming.getNameFor(field));
        return this;
    }

    public SourceWriter appendMethod(MethodReference method) throws NamingException {
        appendIndent();
        sb.append(naming.getNameFor(method));
        return this;
    }

    private void appendIndent() {
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
