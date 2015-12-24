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

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.teavm.debugging.javascript.JavaScriptLocation;
import org.teavm.debugging.javascript.JavaScriptVariable;

/**
 *
 * @author Alexey Andreev
 */
class VariableMap extends AbstractMap<String, Variable> {
    private AtomicReference<Map<String, Variable>> backingMap = new AtomicReference<>();
    private Map<String, JavaScriptVariable> jsVariables;
    private Debugger debugger;
    private JavaScriptLocation location;

    public VariableMap(Map<String, JavaScriptVariable> jsVariables, Debugger debugger, JavaScriptLocation location) {
        this.jsVariables = jsVariables;
        this.debugger = debugger;
        this.location = location;
    }

    @Override
    public Set<Entry<String, Variable>> entrySet() {
        updateBackingMap();
        return backingMap.get().entrySet();
    }

    @Override
    public Variable get(Object key) {
        updateBackingMap();
        return backingMap.get().get(key);
    }

    @Override
    public int size() {
        updateBackingMap();
        return backingMap.get().size();
    }

    private void updateBackingMap() {
        if (backingMap.get() != null) {
            return;
        }
        Map<String, Variable> vars = new HashMap<>();
        for (Map.Entry<String, JavaScriptVariable> entry : jsVariables.entrySet()) {
            JavaScriptVariable jsVar = entry.getValue();
            String[] names = debugger.mapVariable(entry.getKey(), location);
            Value value = new Value(debugger, jsVar.getValue());
            for (String name : names) {
                vars.put(name, new Variable(name, value));
            }
        }
        backingMap.compareAndSet(null, vars);
    }
}
