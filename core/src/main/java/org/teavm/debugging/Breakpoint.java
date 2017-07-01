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

import java.util.ArrayList;
import java.util.List;
import org.teavm.debugging.information.SourceLocation;
import org.teavm.debugging.javascript.JavaScriptBreakpoint;

public class Breakpoint {
    private Debugger debugger;
    volatile List<JavaScriptBreakpoint> jsBreakpoints = new ArrayList<>();
    private SourceLocation location;
    boolean valid;

    Breakpoint(Debugger debugger, SourceLocation location) {
        this.debugger = debugger;
        this.location = location;
    }

    public SourceLocation getLocation() {
        return location;
    }

    public void destroy() {
        debugger.destroyBreakpoint(this);
        debugger = null;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isDestroyed() {
        return debugger == null;
    }

    public Debugger getDebugger() {
        return debugger;
    }
}
