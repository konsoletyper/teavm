/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.backend.javascript.rendering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.backend.javascript.codegen.NamingStrategy;
import org.teavm.backend.javascript.codegen.SourceWriterSink;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

public class NameFrequencyEstimator implements SourceWriterSink {
    static final MethodReference MONITOR_ENTER_METHOD = new MethodReference(Object.class,
            "monitorEnter", Object.class, void.class);
    static final MethodReference MONITOR_ENTER_SYNC_METHOD = new MethodReference(Object.class,
            "monitorEnterSync", Object.class, void.class);
    static final MethodReference MONITOR_EXIT_METHOD = new MethodReference(Object.class,
            "monitorExit", Object.class, void.class);
    static final MethodReference MONITOR_EXIT_SYNC_METHOD = new MethodReference(Object.class,
            "monitorExitSync", Object.class, void.class);

    private Map<String, Entry> entries = new HashMap<>();
    private Set<String> reservedNames = new HashSet<>();
    private boolean hasAdditionalScope;

    public boolean hasAdditionalScope() {
        return hasAdditionalScope;
    }

    @Override
    public SourceWriterSink appendClass(String cls) {
        var key = "c:" + cls;
        var entry = entries.get(key);
        if (entry == null) {
            entry = new Entry();
            entry.operation = naming -> naming.className(cls).scoped;
            entries.put(key, entry);
        }
        entry.frequency++;
        return this;
    }

    @Override
    public SourceWriterSink appendField(FieldReference field) {
        var key = "f:" + field;
        var entry = entries.get(key);
        if (entry == null) {
            entry = new Entry();
            entry.operation = naming -> {
                naming.instanceFieldName(field);
                return false;
            };
            entries.put(key, entry);
        }
        entry.frequency++;
        return this;
    }

    @Override
    public SourceWriterSink appendStaticField(FieldReference field) {
        var key = "sf:" + field;
        var entry = entries.get(key);
        if (entry == null) {
            entry = new Entry();
            entry.operation = naming -> naming.fieldName(field).scoped;
            entries.put(key, entry);
        }
        entry.frequency++;
        return this;
    }

    @Override
    public SourceWriterSink appendVirtualMethod(MethodDescriptor method) {
        var key = "r:" + method;
        var entry = entries.get(key);
        if (entry == null) {
            entry = new Entry();
            entry.operation = naming -> {
                naming.instanceMethodName(method);
                return false;
            };
            entries.put(key, entry);
        }
        entry.frequency++;
        return this;
    }

    @Override
    public SourceWriterSink appendMethod(MethodReference method) {
        var key = "R:" + method;
        var entry = entries.get(key);
        if (entry == null) {
            entry = new Entry();
            entry.operation = naming -> naming.methodName(method).scoped;
            entries.put(key, entry);
        }
        entry.frequency++;
        return this;
    }

    @Override
    public SourceWriterSink appendFunction(String name) {
        var key = "n:" + name;
        var entry = entries.get(key);
        if (entry == null) {
            entry = new Entry();
            entry.operation = naming -> naming.functionName(name).scoped;
            entries.put(key, entry);
        }
        entry.frequency++;
        return this;
    }

    @Override
    public SourceWriterSink appendGlobal(String name) {
        reservedNames.add(name);
        return this;
    }

    @Override
    public SourceWriterSink appendInit(MethodReference method) {
        var key = "I:" + method;
        var entry = entries.get(key);
        if (entry == null) {
            entry = new Entry();
            entry.operation = naming -> naming.initializerName(method).scoped;
            entries.put(key, entry);
        }
        entry.frequency++;
        return this;
    }

    @Override
    public SourceWriterSink appendClassInit(String className) {
        var key = "C:" + className;
        var entry = entries.get(key);
        if (entry == null) {
            entry = new Entry();
            entry.operation = naming -> naming.classInitializerName(className).scoped;
            entries.put(key, entry);
        }
        entry.frequency++;
        return this;
    }

    public void apply(NamingStrategy naming) {
        for (var name : reservedNames) {
            naming.reserveName(name);
        }
        var entryList = new ArrayList<>(entries.values());
        entryList.sort((o1, o2) -> Integer.compare(o2.frequency, o1.frequency));
        for (var entry : entryList) {
            hasAdditionalScope |= entry.operation.perform(naming);
        }
    }

    private static class Entry {
        NamingOperation operation;
        int frequency;
    }

    private interface NamingOperation {
        boolean perform(NamingStrategy naming);
    }
}
