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

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.javascript.JavaScriptValue;

public class Value {
    private Debugger debugger;
    private DebugInformation debugInformation;
    private JavaScriptValue jsValue;
    private AtomicReference<PropertyMap> properties = new AtomicReference<>();

    Value(Debugger debugger, DebugInformation debugInformation, JavaScriptValue jsValue) {
        this.debugger = debugger;
        this.debugInformation = debugInformation;
        this.jsValue = jsValue;
    }

    public String getRepresentation() {
        return jsValue.getRepresentation();
    }

    public String getType() {
        String className = jsValue.getClassName();
        if (className.startsWith("a/")) {
            className = className.substring(2);
            String javaClassName = debugInformation.getClassNameByJsName(className);
            if (javaClassName != null) {
                className = javaClassName;
            }
        } else if (className.startsWith("@")) {
            className = className.substring(1);
        }
        return className;
    }

    public Map<String, Variable> getProperties() {
        if (properties.get() == null) {
            properties.compareAndSet(null, new PropertyMap(jsValue.getClassName(), jsValue.getProperties(), debugger,
                    debugInformation));
        }
        return properties.get();
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
