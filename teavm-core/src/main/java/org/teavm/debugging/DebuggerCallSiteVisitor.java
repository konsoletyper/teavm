package org.teavm.debugging;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface DebuggerCallSiteVisitor {
    void visit(DebuggerVirtualCallSite callSite);

    void visit(DebuggerStaticCallSite callSite);
}
