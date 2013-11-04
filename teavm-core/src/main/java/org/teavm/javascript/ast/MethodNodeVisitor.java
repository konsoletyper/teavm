package org.teavm.javascript.ast;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface MethodNodeVisitor {
    void visit(RegularMethodNode methodNode);

    void visit(NativeMethodNode methodNode);
}
