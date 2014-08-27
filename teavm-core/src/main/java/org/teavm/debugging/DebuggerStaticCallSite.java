package org.teavm.debugging;

import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class DebuggerStaticCallSite extends DebuggerCallSite {
    private MethodReference method;

    DebuggerStaticCallSite(MethodReference method) {
        this.method = method;
    }

    public MethodReference getMethod() {
        return method;
    }

    @Override
    public void acceptVisitor(DebuggerCallSiteVisitor visitor) {
        visitor.visit(this);
    }
}
