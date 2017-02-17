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
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValueChildrenList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.debugging.CallFrame;
import org.teavm.debugging.Variable;
import org.teavm.debugging.information.SourceLocation;

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
            position = XDebuggerUtil.getInstance().createPosition(virtualFile, line);
        }
        return position;
    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        XValueChildrenList children = new XValueChildrenList();
        for (Variable variable : innerFrame.getVariables().values()) {
            children.add(new TeaVMValue(variable.getName(), true, variable.getValue()));
        }
        node.addChildren(children, true);
    }
}
