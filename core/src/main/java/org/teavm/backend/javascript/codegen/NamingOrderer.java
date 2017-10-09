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
package org.teavm.backend.javascript.codegen;

import java.util.*;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

public class NamingOrderer implements NameFrequencyConsumer {
    private Map<String, Entry> entries = new HashMap<>();

    @Override
    public void consume(final MethodReference method) {
        String key = "R:" + method;
        Entry entry = entries.get(key);
        if (entry == null) {
            entry = new Entry();
            entry.operation = naming -> naming.getFullNameFor(method);
            entries.put(key, entry);
        }
        entry.frequency++;
    }


    @Override
    public void consumeInit(final MethodReference method) {
        String key = "I:" + method;
        Entry entry = entries.get(key);
        if (entry == null) {
            entry = new Entry();
            entry.operation = naming -> naming.getNameForInit(method);
            entries.put(key, entry);
        }
        entry.frequency++;
    }

    @Override
    public void consume(final MethodDescriptor method) {
        String key = "r:" + method;
        Entry entry = entries.get(key);
        if (entry == null) {
            entry = new Entry();
            entry.operation = naming -> naming.getNameFor(method);
            entries.put(key, entry);
        }
        entry.frequency++;
    }

    @Override
    public void consume(final String className) {
        String key = "c:" + className;
        Entry entry = entries.get(key);
        if (entry == null) {
            entry = new Entry();
            entry.operation = naming -> naming.getNameFor(className);
            entries.put(key, entry);
        }
        entry.frequency++;
    }

    @Override
    public void consume(final FieldReference field) {
        String key = "f:" + field;
        Entry entry = entries.get(key);
        if (entry == null) {
            entry = new Entry();
            entry.operation = naming -> naming.getNameFor(field);
            entries.put(key, entry);
        }
        entry.frequency++;
    }

    @Override
    public void consumeFunction(final String name) {
        String key = "n:" + name;
        Entry entry = entries.get(key);
        if (entry == null) {
            entry = new Entry();
            entry.operation = naming -> naming.getNameForFunction(name);
            entries.put(key, entry);
        }
        entry.frequency++;
    }

    public void apply(NamingStrategy naming) {
        List<Entry> entryList = new ArrayList<>(entries.values());
        Collections.sort(entryList, (o1, o2) -> Integer.compare(o2.frequency, o1.frequency));
        for (Entry entry : entryList) {
            entry.operation.perform(naming);
        }
    }

    static class Entry {
        NamingOperation operation;
        int frequency;
    }

    interface NamingOperation {
        void perform(NamingStrategy naming);
    }
}
