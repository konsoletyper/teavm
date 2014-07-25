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
package org.teavm.debugging;

import java.util.List;

/**
 *
 * @author Alexey Andreev
 */
public class Breakpoint {
    private Debugger debugger;
    private List<JavaScriptBreakpoint> jsBreakpoints;
    private String fileName;
    private int line;
    private boolean enabled = true;

    Breakpoint(Debugger debugger, List<JavaScriptBreakpoint> jsBreakpoints, String fileName, int line) {
        this.debugger = debugger;
        this.jsBreakpoints = jsBreakpoints;
        this.fileName = fileName;
        this.line = line;
        for (JavaScriptBreakpoint jsBreakpoint : jsBreakpoints) {
            debugger.breakpointMap.put(jsBreakpoint.getId(), this);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        for (JavaScriptBreakpoint jsBreakpoint : jsBreakpoints) {
            jsBreakpoint.setEnabled(enabled);
        }
    }

    public String getFileName() {
        return fileName;
    }

    public int getLine() {
        return line;
    }

    public void destroy() {
        for (JavaScriptBreakpoint jsBreakpoint : jsBreakpoints) {
            jsBreakpoint.destroy();
            debugger.breakpointMap.remove(jsBreakpoint.getId());
        }
        jsBreakpoints.clear();
    }

    public boolean isDestroyed() {
        return jsBreakpoints.isEmpty();
    }

    public Debugger getDebugger() {
        return debugger;
    }
}
