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

import java.net.URL;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.teavm.debugging.CallFrame;
import org.teavm.debugging.javascript.JavaScriptCallFrame;
import org.teavm.debugging.javascript.JavaScriptLocation;
import org.teavm.eclipse.debugger.*;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class TeaVMDebugModelPresentation extends LabelProvider implements IDebugModelPresentation {
    @Override
    public String getEditorId(IEditorInput input, Object element) {
        IEditorRegistry registry = PlatformUI.getWorkbench().getEditorRegistry();
        if (element instanceof IFile) {
            IFile file = (IFile)element;
            return registry.getDefaultEditor(file.getName()).getId();
        } else if (element instanceof ILineBreakpoint) {
            String fileName = ((ILineBreakpoint)element).getMarker().getResource().getName();
            return registry.getDefaultEditor(fileName).getId();
        } else if (element instanceof IStorage) {
            IStorage storage = (IStorage)element;
            return registry.getDefaultEditor(storage.getName()).getId();
        } else if (element instanceof URL) {
            URL url = (URL)element;
            return registry.getDefaultEditor(url.getFile()).getId();
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
        if (element instanceof URL) {
            return new URLEditorInput((URL)element);
        }
        if (element instanceof IStorage) {
            return new StorageEditorInput((IStorage)element);
        }
        return null;
    }

    @Override
    public void computeDetail(IValue value, IValueDetailListener listener) {
        if (value instanceof TeaVMValue) {
            String description = ((TeaVMValue)value).getDescription();
            listener.detailComputed(value, description);
        } else {
            listener.detailComputed(value, "");
        }
    }

    @Override
    public void setAttribute(String attr, Object value) {
    }

    @Override
    public String getText(Object element) {
        if (element instanceof TeaVMJavaStackFrame) {
            TeaVMJavaStackFrame stackFrame = (TeaVMJavaStackFrame)element;
            return callFrameAsString(stackFrame.getCallFrame());
        } else if (element instanceof TeaVMJSStackFrame) {
            TeaVMJSStackFrame stackFrame = (TeaVMJSStackFrame)element;
            return callFrameAsString(stackFrame.getCallFrame());
        } else if (element instanceof TeaVMDebugTarget) {
            return ((TeaVMDebugTarget)element).getName();
        } else if (element instanceof TeaVMThread) {
            return ((TeaVMThread)element).getName();
        }
        return super.getText(element);
    }


    private String callFrameAsString(CallFrame callFrame) {
        MethodReference method = callFrame.getMethod();
        if (method == null) {
            return locationAsString(callFrame.getOriginalLocation());
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
        return locationAsString(callFrame.getLocation());
    }

    private String locationAsString(JavaScriptLocation location) {
        StringBuilder sb = new StringBuilder();
        String script = location.getScript();
        sb.append(script.substring(script.lastIndexOf('/') + 1));
        sb.append(" at ").append(location.getLine() + 1).append(";").append(location.getColumn() + 1);
        return sb.toString();
    }
}
