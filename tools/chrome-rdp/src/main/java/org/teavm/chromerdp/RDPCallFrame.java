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

import java.util.Map;
import org.teavm.common.Promise;
import org.teavm.debugging.javascript.JavaScriptCallFrame;
import org.teavm.debugging.javascript.JavaScriptDebugger;
import org.teavm.debugging.javascript.JavaScriptLocation;
import org.teavm.debugging.javascript.JavaScriptValue;
import org.teavm.debugging.javascript.JavaScriptVariable;

class RDPCallFrame implements JavaScriptCallFrame {
    private ChromeRDPDebugger debugger;
    private String chromeId;
    private JavaScriptLocation location;
    private Promise<Map<String, ? extends JavaScriptVariable>> variables;
    private JavaScriptValue thisObject;
    private JavaScriptValue closure;
    private String scopeId;

    RDPCallFrame(ChromeRDPDebugger debugger, String chromeId, JavaScriptLocation location, String scopeId,
            JavaScriptValue thisObject, JavaScriptValue closure) {
        this.debugger = debugger;
        this.chromeId = chromeId;
        this.location = location;
        this.scopeId = scopeId;
        this.thisObject = thisObject;
        this.closure = closure;
    }

    public String getChromeId() {
        return chromeId;
    }

    @Override
    public JavaScriptLocation getLocation() {
        return location;
    }

    @Override
    public Promise<Map<String, ? extends JavaScriptVariable>> getVariables() {
        if (variables == null) {
            variables = debugger.createScope(scopeId);
        }
        return variables;
    }

    @Override
    public JavaScriptDebugger getDebugger() {
        return debugger;
    }

    @Override
    public JavaScriptValue getThisVariable() {
        return thisObject;
    }

    @Override
    public JavaScriptValue getClosureVariable() {
        return closure;
    }
}
