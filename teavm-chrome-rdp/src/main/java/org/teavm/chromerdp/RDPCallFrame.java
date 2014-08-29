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

import java.util.Collections;
import java.util.Map;
import org.teavm.debugging.javascript.JavaScriptCallFrame;
import org.teavm.debugging.javascript.JavaScriptLocation;
import org.teavm.debugging.javascript.JavaScriptVariable;

/**
 *
 * @author Alexey Andreev
 */
public class RDPCallFrame implements JavaScriptCallFrame {
    private String chromeId;
    private JavaScriptLocation location;
    private Map<String, JavaScriptVariable> variables;

    public RDPCallFrame(String chromeId, JavaScriptLocation location,
            Map<String, ? extends JavaScriptVariable> variables) {
        this.chromeId = chromeId;
        this.location = location;
        this.variables = Collections.unmodifiableMap(variables);
    }

    public String getChromeId() {
        return chromeId;
    }

    @Override
    public JavaScriptLocation getLocation() {
        return location;
    }

    @Override
    public Map<String, JavaScriptVariable> getVariables() {
        return variables;
    }
}
