package org.teavm.debugging;

import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class DebuggerVirtualCallSite extends DebuggerCallSite {
    private MethodReference method;
    private int variableId;
    private String variableName;

    DebuggerVirtualCallSite(MethodReference method, int variableId, String variableName) {
        this.method = method;
        this.variableId = variableId;
        this.variableName = variableName;
    }

    public MethodReference getMethod() {
        return method;
    }

    public void setMethod(MethodReference method) {
        this.method = method;
    }

    public int getVariableId() {
        return variableId;
    }

    public void setVariableId(int variableId) {
        this.variableId = variableId;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    @Override
    public void acceptVisitor(DebuggerCallSiteVisitor visitor) {
        visitor.visit(this);
    }
}
