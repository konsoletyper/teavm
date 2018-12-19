/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.idea.debug;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XNamedValue;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValueChildrenList;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.common.Promise;
import org.teavm.debugging.CallFrame;
import org.teavm.debugging.Value;
import org.teavm.debugging.Variable;
import org.teavm.debugging.information.SourceLocation;
import org.teavm.debugging.javascript.JavaScriptCallFrame;
import org.teavm.debugging.javascript.JavaScriptLocation;

class TeaVMStackFrame extends XStackFrame {
    private TeaVMExecutionStack stack;
    private CallFrame innerFrame;
    private XSourcePosition position;

    TeaVMStackFrame(@NotNull TeaVMExecutionStack stack, @NotNull CallFrame innerFrame) {
        this.stack = stack;
        this.innerFrame = innerFrame;
    }

    @Nullable
    @Override
    public XSourcePosition getSourcePosition() {
        if (position == null) {
            VirtualFile virtualFile = null;
            int line = -1;
            SourceLocation innerLocation = innerFrame.getLocation();
            if (innerLocation != null) {
                if (innerLocation.getFileName() != null) {
                    virtualFile = stack.findVirtualFile(innerLocation.getFileName());
                }
                line = innerLocation.getLine() - 1;
            }
            if (virtualFile == null) {
                JavaScriptCallFrame jsFrame = innerFrame.getOriginalCallFrame();
                if (jsFrame != null) {
                    JavaScriptLocation jsLocation = jsFrame.getLocation();
                    if (jsLocation != null) {
                        virtualFile = VirtualFileManager.getInstance().findFileByUrl(jsLocation.getScript());
                        if (virtualFile != null) {
                            line = jsLocation.getLine();
                        }
                    }
                }
            }
            position = XDebuggerUtil.getInstance().createPosition(virtualFile, line);
        }
        return position;
    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        computeChildrenImpl(node, innerFrame.getVariables(), true);
    }

    static void computeChildrenImpl(XCompositeNode node, Promise<Map<String, Variable>> variablesPromise,
            boolean root) {
        variablesPromise.then(variables -> variables.values()
                .stream()
                .map(var -> createValueNode(var.getName(), root, var.getValue()))
                .collect(Collectors.toList()))
                .thenAsync(Promise::all)
                .thenVoid(values -> {
                    XValueChildrenList children = new XValueChildrenList();
                    for (XNamedValue value : values) {
                        children.add(value);
                    }
                    node.addChildren(children, true);
                })
                .catchError(e -> {
                    node.setErrorMessage("Error occurred calculating scope: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                });
    }

    static Promise<XNamedValue> createValueNode(String name, boolean root, Value value) {
        return value.getType().then(type -> !type.startsWith("@")
                ? new TeaVMValue(name, root, value)
                : new TeaVMOriginalValue(name, root, value.getOriginalValue()));
    }
}
