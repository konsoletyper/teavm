/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.chromerdp;

import java.util.LinkedHashSet;
import java.util.Set;
import org.teavm.common.Promise;
import org.teavm.debugging.javascript.JavaScriptLocation;

class RDPNativeBreakpoint {
    volatile String chromeId;
    ChromeRDPDebugger debugger;
    private JavaScriptLocation location;
    Promise<Void> initPromise;
    Promise<Void> destroyPromise;
    Set<RDPBreakpoint> breakpoints = new LinkedHashSet<>();

    RDPNativeBreakpoint(ChromeRDPDebugger debugger, JavaScriptLocation location) {
        this.debugger = debugger;
        this.location = location;
    }

    public JavaScriptLocation getLocation() {
        return location;
    }

    public boolean isValid() {
        return chromeId != null && debugger != null && debugger.isAttached();
    }
}
