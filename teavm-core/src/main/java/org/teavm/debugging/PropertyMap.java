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
 */package org.teavm.debugging;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
class PropertyMap extends AbstractMap<String, Variable> {
    private String className;
    private AtomicReference<Map<String, Variable>> backingMap = new AtomicReference<>();
    private Map<String, JavaScriptVariable> jsVariables;
    private Debugger debugger;

    public PropertyMap(String className, Map<String, JavaScriptVariable> jsVariables, Debugger debugger) {
        this.className = className;
        this.jsVariables = jsVariables;
        this.debugger = debugger;
    }

    @Override
    public int size() {
        updateBackingMap();
        return backingMap.get().size();
    }

    @Override
    public Variable get(Object key) {
        updateBackingMap();
        return backingMap.get().get(key);
    }

    @Override
    public Set<Entry<String, Variable>> entrySet() {
        updateBackingMap();
        return backingMap.get().entrySet();
    }

    private void updateBackingMap() {
        if (backingMap.get() != null) {
            return;
        }
        Map<String, Variable> vars = new HashMap<>();
        for (Map.Entry<String, JavaScriptVariable> entry : jsVariables.entrySet()) {
            JavaScriptVariable jsVar = entry.getValue();
            String name = debugger.mapField(className, entry.getKey());
            if (name == null) {
                continue;
            }
            Value value = new Value(debugger, jsVar.getValue());
            vars.put(entry.getKey(), new Variable(name, value));
        }
        backingMap.compareAndSet(null, vars);
    }
}
