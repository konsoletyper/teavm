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

import java.util.HashMap;
import java.util.Map;
import org.teavm.common.Promise;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.javascript.JavaScriptValue;
import org.teavm.debugging.javascript.JavaScriptVariable;

public class Value {
    private Debugger debugger;
    private DebugInformation debugInformation;
    private JavaScriptValue jsValue;
    private Promise<Map<String, Variable>> properties;
    private Promise<String> type;

    Value(Debugger debugger, DebugInformation debugInformation, JavaScriptValue jsValue) {
        this.debugger = debugger;
        this.debugInformation = debugInformation;
        this.jsValue = jsValue;
    }

    private static boolean isNumeric(String str) {
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    public Promise<String> getRepresentation() {
        return jsValue.getRepresentation();
    }

    public Promise<String> getType() {
        if (type == null) {
            type = jsValue.getClassName().then(className -> {
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
        return type;
    }

    public Promise<Map<String, Variable>> getProperties() {
        if (properties == null) {
            properties = jsValue.getProperties().thenAsync(jsVariables -> {
                return getType().thenAsync(className -> {
                    if (!className.startsWith("@") && className.endsWith("[]") && jsVariables.containsKey("data")) {
                        return jsVariables.get("data").getValue().getProperties()
                                .then(arrayData -> fillArray(arrayData));
                    }
                    Map<String, Variable> vars = new HashMap<>();
                    for (Map.Entry<String, ? extends JavaScriptVariable> entry : jsVariables.entrySet()) {
                        JavaScriptVariable jsVar = entry.getValue();
                        String name;
                        name = debugger.mapField(className, entry.getKey());
                        if (name == null) {
                            continue;
                        }
                        Value value = new Value(debugger, debugInformation, jsVar.getValue());
                        vars.put(name, new Variable(name, value));
                    }
                    return Promise.of(vars);
                });
            });
        }
        return properties;
    }

    private Map<String, Variable> fillArray(Map<String, ? extends JavaScriptVariable> jsVariables) {
        Map<String, Variable> vars = new HashMap<>();
        for (Map.Entry<String, ? extends JavaScriptVariable> entry : jsVariables.entrySet()) {
            JavaScriptVariable jsVar = entry.getValue();
            if (!isNumeric(entry.getKey())) {
                continue;
            }
            Value value = new Value(debugger, debugInformation, jsVar.getValue());
            vars.put(entry.getKey(), new Variable(entry.getKey(), value));
        }
        return vars;
    }

    public boolean hasInnerStructure() {
        if (getType().equals("long")) {
            return false;
        }
        return jsValue.hasInnerStructure();
    }

    public String getInstanceId() {
        if (getType().equals("long")) {
            return null;
        }
        return jsValue.getInstanceId();
    }

    public JavaScriptValue getOriginalValue() {
        return jsValue;
    }
}
