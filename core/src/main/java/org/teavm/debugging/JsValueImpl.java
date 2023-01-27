/*
 *  Copyright 2022 Alexey Andreev.
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

import java.util.HashMap;
import java.util.Map;
import org.teavm.common.Promise;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.javascript.JavaScriptValue;
import org.teavm.debugging.javascript.JavaScriptVariable;

class JsValueImpl extends Value {
    private DebugInformation debugInformation;
    private JavaScriptValue jsValue;

    JsValueImpl(Debugger debugger, DebugInformation debugInformation, JavaScriptValue jsValue) {
        super(debugger);
        this.debugInformation = debugInformation;
        this.jsValue = jsValue;
    }

    @Override
    public Promise<String> getRepresentation() {
        return jsValue.getRepresentation();
    }

    @Override
    Promise<String> prepareType() {
        return jsValue.getClassName().then(className -> {
            if (className.startsWith("a/")) {
                className = className.substring(2);
                String origClassName = className;
                int degree = 0;
                while (className.endsWith("[]")) {
                    className = className.substring(0, className.length() - 2);
                    ++degree;
                }
                String javaClassName = debugInformation.getClassNameByJsName(className);
                if (javaClassName != null) {
                    if (degree > 0) {
                        StringBuilder sb = new StringBuilder(javaClassName);
                        for (int i = 0; i < degree; ++i) {
                            sb.append("[]");
                        }
                        javaClassName = sb.toString();
                    }
                    className = javaClassName;
                } else {
                    className = origClassName;
                }
            }
            return className;
        });
    }

    @Override
    Promise<Map<String, Variable>> prepareProperties() {
        return jsValue.getProperties().thenAsync(jsVariables -> {
            return getType().thenAsync(className -> {
                if (!className.startsWith("@") && className.endsWith("[]") && jsVariables.containsKey("data")) {
                    return jsVariables.get("data").getValue().getProperties()
                            .then(arrayData -> fillArray(arrayData));
                }
                var vars = new HashMap<String, Variable>();
                for (var entry : jsVariables.entrySet()) {
                    var jsVar = entry.getValue();
                    String name;
                    name = debugger.mapField(className, entry.getKey());
                    if (name == null) {
                        continue;
                    }
                    var value = new JsValueImpl(debugger, debugInformation, jsVar.getValue());
                    vars.put(name, new Variable(name, value));
                }
                return Promise.of(vars);
            });
        });
    }

    private Map<String, Variable> fillArray(Map<String, ? extends JavaScriptVariable> jsVariables) {
        var vars = new HashMap<String, Variable>();
        for (var entry : jsVariables.entrySet()) {
            var jsVar = entry.getValue();
            if (!isNumeric(entry.getKey())) {
                continue;
            }
            Value value = new JsValueImpl(debugger, debugInformation, jsVar.getValue());
            vars.put(entry.getKey(), new Variable(entry.getKey(), value));
        }
        return vars;
    }

    @Override
    public Promise<Boolean> hasInnerStructure() {
        return getType().then(value -> !value.equals("long") && jsValue.hasInnerStructure());
    }

    @Override
    public Promise<String> getInstanceId() {
        return getType().then(value -> value.equals("long") ? null : jsValue.getInstanceId());
    }

    @Override
    public JavaScriptValue getOriginalValue() {
        return jsValue;
    }
}
