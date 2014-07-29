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
package org.teavm.chromerpd;

import org.teavm.debugging.JavaScriptBreakpoint;
import org.teavm.debugging.JavaScriptLocation;

/**
 *
 * @author Alexey Andreev
 */
public class RDPBreakpoint implements JavaScriptBreakpoint {
    private String chromeId;
    private ChromeRDPDebuggerEndpoint debugger;
    private JavaScriptLocation location;

    public RDPBreakpoint(String chromeId, ChromeRDPDebuggerEndpoint debugger, JavaScriptLocation location) {
        this.chromeId = chromeId;
        this.debugger = debugger;
        this.location = location;
    }

    public String getChromeId() {
        return chromeId;
    }

    @Override
    public JavaScriptLocation getLocation() {
        return location;
    }

    @Override
    public void destroy() {
        if (!debugger.closed) {
            debugger.destroyBreakpoint(this);
        }
    }
}
