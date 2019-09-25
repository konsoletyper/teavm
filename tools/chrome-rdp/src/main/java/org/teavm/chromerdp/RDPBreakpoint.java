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
package org.teavm.chromerdp;

import org.teavm.common.Promise;
import org.teavm.debugging.javascript.JavaScriptBreakpoint;
import org.teavm.debugging.javascript.JavaScriptLocation;

class RDPBreakpoint implements JavaScriptBreakpoint {
    ChromeRDPDebugger debugger;
    RDPNativeBreakpoint nativeBreakpoint;
    Promise<Void> destroyPromise;

    RDPBreakpoint(ChromeRDPDebugger debugger) {
        this.debugger = debugger;
    }

    @Override
    public JavaScriptLocation getLocation() {
        return nativeBreakpoint.getLocation();
    }

    @Override
    public Promise<Void> destroy() {
        if (destroyPromise == null) {
            destroyPromise = debugger.destroyBreakpoint(this);
            debugger = null;
        }
        return destroyPromise;
    }

    @Override
    public boolean isValid() {
        return nativeBreakpoint != null && nativeBreakpoint.isValid();
    }
}
