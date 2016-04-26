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

import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;
import org.teavm.debugging.Breakpoint;
import org.teavm.debugging.Debugger;

public class TeaVMLineBreakpointHandler extends XBreakpointHandler<XLineBreakpoint<JavaLineBreakpointProperties>> {
    private Debugger innerDebugger;

    @SuppressWarnings("unchecked")
    public TeaVMLineBreakpointHandler(Debugger innerDebugger) {
        super(JavaLineBreakpointType.class);
        this.innerDebugger = innerDebugger;
    }

    @Override
    public void registerBreakpoint(@NotNull XLineBreakpoint<JavaLineBreakpointProperties> breakpoint) {
        Breakpoint innerBreakpoint = innerDebugger.createBreakpoint(breakpoint.getShortFilePath(),
                breakpoint.getLine());
        breakpoint.putUserData(TeaVMDebugProcess.INNER_BREAKPOINT_KEY, innerBreakpoint);
    }

    @Override
    public void unregisterBreakpoint(@NotNull XLineBreakpoint<JavaLineBreakpointProperties> breakpoint,
            boolean temporary) {
        Breakpoint innerBreakpoint = breakpoint.getUserData(TeaVMDebugProcess.INNER_BREAKPOINT_KEY);
        if (innerBreakpoint != null) {
            breakpoint.putUserData(TeaVMDebugProcess.INNER_BREAKPOINT_KEY, null);
            innerBreakpoint.destroy();
        }
    }
}
