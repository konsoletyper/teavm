package org.teavm.javascript.ast;

import org.teavm.javascript.ni.Generator;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class NativeMethodNode extends MethodNode {
    private Generator generator;

    public NativeMethodNode(MethodReference reference) {
        super(reference);
    }

    public Generator getGenerator() {
        return generator;
    }

    public void setGenerator(Generator generator) {
        this.generator = generator;
    }

    @Override
    public void acceptVisitor(MethodNodeVisitor visitor) {
        visitor.visit(this);
    }
}
