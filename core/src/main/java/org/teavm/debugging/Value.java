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
import org.teavm.common.Promise;
import org.teavm.debugging.javascript.JavaScriptValue;

public abstract class Value {
    Debugger debugger;
    private Promise<Map<String, Variable>> properties;
    private Promise<String> type;

    Value(Debugger debugger) {
        this.debugger = debugger;
    }

    static boolean isNumeric(String str) {
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    public abstract Promise<String> getRepresentation();

    public Promise<String> getType() {
        if (type == null) {
            type = prepareType();
        }
        return type;
    }

    abstract Promise<String> prepareType();

    public Promise<Map<String, Variable>> getProperties() {
        if (properties == null) {
            properties = prepareProperties();
        }
        return properties;
    }

    abstract Promise<Map<String, Variable>> prepareProperties();

    public abstract Promise<Boolean> hasInnerStructure();

    public abstract Promise<String> getInstanceId();

    public abstract JavaScriptValue getOriginalValue();
}
