/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.eclipse.debugger.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.teavm.debugging.CallFrame;
import org.teavm.debugging.javascript.JavaScriptCallFrame;
import org.teavm.eclipse.debugger.TeaVMJSStackFrame;
import org.teavm.eclipse.debugger.TeaVMStackFrame;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMDebugModelPresentation extends LabelProvider implements IDebugModelPresentation {
    @Override
    public String getEditorId(IEditorInput input, Object element) {
        if (element instanceof IFile || element instanceof ILineBreakpoint) {
            return JavaUI.ID_CU_EDITOR;
        }
        return null;
    }

    @Override
    public IEditorInput getEditorInput(Object element) {
        if (element instanceof IFile) {
            return new FileEditorInput((IFile)element);
        }
        if (element instanceof ILineBreakpoint) {
            return new FileEditorInput((IFile)((ILineBreakpoint)element).getMarker().getResource());
        }
        return null;
    }

    @Override
    public void computeDetail(IValue arg0, IValueDetailListener arg1) {
    }

    @Override
    public void setAttribute(String arg0, Object arg1) {
    }

    @Override
    public String getText(Object element) {
        if (element instanceof TeaVMStackFrame) {
            TeaVMStackFrame stackFrame = (TeaVMStackFrame)element;
            return callFrameAsString(stackFrame.getCallFrame());
        } else if (element instanceof TeaVMJSStackFrame) {
            TeaVMJSStackFrame stackFrame = (TeaVMJSStackFrame)element;
            return callFrameAsString(stackFrame.getCallFrame());
        }
        return super.getText(element);
    }

    private String callFrameAsString(CallFrame callFrame) {
        MethodReference method = callFrame.getMethod();
        if (method == null) {
            return "<native JavaScript code>";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(classAsString(method.getClassName())).append('.').append(method.getName()).append('(');
        MethodDescriptor desc = method.getDescriptor();
        for (int i = 0; i < desc.parameterCount(); ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(typeAsString(desc.parameterType(i)));
        }
        sb.append(')');
        if (callFrame.getLocation() != null && callFrame.getLocation().getLine() >= 0) {
            sb.append(" line " + callFrame.getLocation().getLine());
        } else {
            sb.append(" unknown line");
        }
        return sb.toString();
    }

    private String typeAsString(ValueType type) {
        int arrayDegree = 0;
        StringBuilder sb = new StringBuilder();
        while (type instanceof ValueType.Array) {
            ++arrayDegree;
            type = ((ValueType.Array)type).getItemType();
        }
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive)type).getKind()) {
                case BOOLEAN:
                    sb.append("boolean");
                    break;
                case BYTE:
                    sb.append("byte");
                    break;
                case SHORT:
                    sb.append("short");
                    break;
                case CHARACTER:
                    sb.append("char");
                    break;
                case INTEGER:
                    sb.append("int");
                    break;
                case LONG:
                    sb.append("long");
                    break;
                case FLOAT:
                    sb.append("float");
                    break;
                case DOUBLE:
                    sb.append("double");
                    break;
            }
        } else if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object)type).getClassName();
            sb.append(classAsString(className));
        }
        while (arrayDegree-- > 0) {
            sb.append("[]");
        }
        return sb.toString();
    }

    private String classAsString(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    private String callFrameAsString(JavaScriptCallFrame callFrame) {
        StringBuilder sb = new StringBuilder();
        String script = callFrame.getLocation().getScript();
        sb.append(script.substring(script.lastIndexOf('/') + 1));
        sb.append(" at ").append(callFrame.getLocation().getLine() + 1).append(";")
                .append(callFrame.getLocation().getColumn() + 1);
        return sb.toString();
    }
}
