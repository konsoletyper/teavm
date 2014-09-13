package org.teavm.eclipse.debugger;

import org.eclipse.debug.core.DebugException;
import org.teavm.debugging.javascript.JavaScriptValue;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMJSScope extends TeaVMVariable {
    private String name;
    private JavaScriptValue value;

    public TeaVMJSScope(TeaVMDebugTarget debugTarget, String name, JavaScriptValue value) {
        super(debugTarget, new TeaVMJSValue(debugTarget, value));
        this.name = name;
    }

    @Override
    public String getName() throws DebugException {
        return name;
    }

    @Override
    public String getReferenceTypeName() throws DebugException {
        return value.getClassName();
    }
}
