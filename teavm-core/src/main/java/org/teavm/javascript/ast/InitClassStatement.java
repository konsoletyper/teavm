package org.teavm.javascript.ast;

/**
 *
 * @author Alexey Andreev
 */
public class InitClassStatement extends Statement {
    private String className;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public void acceptVisitor(StatementVisitor visitor) {
        visitor.visit(this);
    }
}
