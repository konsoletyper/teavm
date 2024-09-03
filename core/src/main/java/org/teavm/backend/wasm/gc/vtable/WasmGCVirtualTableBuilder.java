/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.gc.vtable;

import com.carrotsearch.hppc.ObjectIntHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.teavm.common.LCATree;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

class WasmGCVirtualTableBuilder {
    ListableClassReaderSource classes;
    Collection<MethodReference> methodsAtCallSites;
    Predicate<MethodReference> isVirtual;
    private Map<String, Set<MethodDescriptor>> groupedMethodsAtCallSites = new HashMap<>();
    private List<Table> tables = new ArrayList<>();
    private Map<String, Table> tableMap = new HashMap<>();
    private LCATree lcaTree;
    private Map<String, Table> interfaceImplementors = new HashMap<>();
    Map<String, WasmGCVirtualTable> result = new HashMap<>();

    void build() {
        initTables();
        buildLCA();
        fillInterfaceImplementors();
        groupMethodsFromCallSites();
        fillTables();
        buildResult();
    }

    private void initTables() {
        for (var className : classes.getClassNames()) {
            initTable(className);
        }
    }

    private void initTable(String className) {
        var cls = classes.get(className);
        if (!cls.hasModifier(ElementModifier.INTERFACE) && !tableMap.containsKey(className)) {
            if (cls.getParent() != null) {
                initTable(cls.getParent());
            }
            var table = new Table(cls, tables.size());
            tables.add(table);
            tableMap.put(className, table);
        }
    }

    private void buildLCA() {
        lcaTree = new LCATree(tables.size() + 1);
        for (var i = 0; i < tables.size(); i++) {
            var table = tables.get(i);
            var parentTable = table.cls.getParent() != null ? tableMap.get(table.cls.getParent()) : null;
            lcaTree.addNode(parentTable != null ? parentTable.index + 1 : 0);
        }
    }

    private void fillInterfaceImplementors() {
        for (var className : classes.getClassNames()) {
            var cls = classes.get(className);
            if (!cls.hasModifier(ElementModifier.INTERFACE)) {
                var visited = new HashSet<String>();
                do {
                    var table = tableMap.get(cls.getName());
                    for (var itfName : cls.getInterfaces()) {
                        addImplementorToInterface(itfName, table, visited);
                    }
                    cls = cls.getParent() != null ? classes.get(cls.getParent()) : null;
                } while (cls != null);
            }
        }
    }

    private void addImplementorToInterface(String interfaceName, Table newImplementor, Set<String> visited) {
        if (!visited.add(interfaceName)) {
            return;
        }
        var knownImplementor = interfaceImplementors.get(interfaceName);
        if (knownImplementor == null) {
            interfaceImplementors.put(interfaceName, newImplementor);
        } else {
            var lcaIndex = lcaTree.lcaOf(newImplementor.index + 1, knownImplementor.index + 1);
            if (lcaIndex > 0) {
                interfaceImplementors.put(interfaceName, tables.get(lcaIndex - 1));
            }
        }
        var cls = classes.get(interfaceName);
        if (cls != null) {
            for (var superInterface : cls.getInterfaces()) {
                addImplementorToInterface(superInterface, newImplementor, visited);
            }
        }
    }

    private void groupMethodsFromCallSites() {
        for (var methodRef : methodsAtCallSites) {
            var className = mapInterface(methodRef.getClassName());
            var group = groupedMethodsAtCallSites.computeIfAbsent(className, k -> new LinkedHashSet<>());
            group.add(methodRef.getDescriptor());
        }
    }

    private String mapInterface(String name) {
        var cls = classes.get(name);
        if (cls == null || !cls.hasModifier(ElementModifier.INTERFACE)) {
            return name;
        }
        var implementor = interfaceImplementors.get(cls.getName());
        if (implementor == null) {
            return name;
        }
        return implementor.cls.getName();
    }

    private void fillTables() {
        for (var className : classes.getClassNames()) {
            var table = tableMap.get(className);
            if (table != null) {
                fillTable(table);
            }
        }
    }

    private void fillTable(Table table) {
        if (table.filled) {
            return;
        }
        table.filled = true;
        var parent = table.cls.getParent() != null ? tableMap.get(table.cls.getParent()) : null;
        table.parent = parent;
        var indexes = new ObjectIntHashMap<MethodDescriptor>();
        if (parent != null) {
            fillTable(parent);
            table.entries.addAll(parent.entries);
            table.implementors.addAll(parent.implementors);
            for (var entry : table.entries) {
                indexes.put(entry.method, entry.index);
            }
            table.currentImplementors.putAll(parent.currentImplementors);
            table.interfaces.addAll(parent.interfaces);
        }

        for (var method : table.cls.getMethods()) {
            if (!method.hasModifier(ElementModifier.STATIC) && !method.hasModifier(ElementModifier.ABSTRACT)) {
                if (method.getProgram() == null && !method.hasModifier(ElementModifier.NATIVE)) {
                    continue;
                }
                if (!isVirtual.test(method.getReference())) {
                    continue;
                }
                table.currentImplementors.put(method.getDescriptor(), method.getReference());
            }
        }

        for (var itfName : table.cls.getInterfaces()) {
            fillFromInterfaces(itfName, table);
        }

        var group = groupedMethodsAtCallSites.get(table.cls.getName());
        if (group != null) {
            table.used = true;
            for (var method : group) {
                if (indexes.getOrDefault(method, -1) < 0) {
                    var entry = new Entry(method, table, table.entries.size());
                    table.entries.add(entry);
                    indexes.put(method, entry.index);
                    table.implementors.add(null);
                }
            }
        }
        for (var entry : indexes) {
            var implementor = table.currentImplementors.get(entry.key);
            table.implementors.set(entry.value, implementor);
        }
    }

    private void fillFromInterfaces(String itfName, Table table) {
        if (!table.interfaces.add(itfName)) {
            return;
        }
        var cls = classes.get(itfName);
        if (cls == null) {
            return;
        }
        for (var method : cls.getMethods()) {
            if (!method.hasModifier(ElementModifier.STATIC) && !method.hasModifier(ElementModifier.ABSTRACT)) {
                if (method.getProgram() == null && !method.hasModifier(ElementModifier.NATIVE)) {
                    continue;
                }
                if (!isVirtual.test(method.getReference())) {
                    continue;
                }
                if (table.currentImplementors.get(method.getDescriptor()) == null) {
                    table.currentImplementors.put(method.getDescriptor(), method.getReference());
                }
            }
        }
        for (var superItf : cls.getInterfaces()) {
            fillFromInterfaces(superItf, table);
        }
    }

    private void buildResult() {
        for (var className : classes.getClassNames()) {
            var cls = classes.get(className);
            var table = !cls.hasModifier(ElementModifier.INTERFACE)
                    ? tableMap.get(className)
                    : interfaceImplementors.get(className);
            if (table != null) {
                result.put(className, table.getBuildResult());
            }
        }
    }

    private static class Table {
        final ClassReader cls;
        int index;
        boolean filled;
        boolean used;
        Table parent;
        List<Entry> entries = new ArrayList<>();
        List<MethodReference> implementors = new ArrayList<>();
        Map<MethodDescriptor, MethodReference> currentImplementors = new HashMap<>();
        Set<String> interfaces = new HashSet<>();
        private WasmGCVirtualTable buildResult;

        Table(ClassReader cls, int index) {
            this.cls = cls;
            this.index = index;
        }

        WasmGCVirtualTable getBuildResult() {
            if (buildResult == null) {
                buildResult = new WasmGCVirtualTable(parent != null ? parent.getBuildResult() : null, cls.getName(),
                        used, !cls.hasModifier(ElementModifier.ABSTRACT));
                buildResult.entries = entries.stream()
                        .map(Entry::getBuildResult)
                        .collect(Collectors.toList());
                buildResult.implementors = implementors.toArray(new MethodReference[0]);
            }
            return buildResult;
        }
    }

    private static class Entry {
        MethodDescriptor method;
        Table origin;
        int index;
        private WasmGCVirtualTableEntry buildResult;

        Entry(MethodDescriptor method, Table origin, int index) {
            this.method = method;
            this.origin = origin;
            this.index = index;
        }

        WasmGCVirtualTableEntry getBuildResult() {
            if (buildResult == null) {
                buildResult = new WasmGCVirtualTableEntry(origin.getBuildResult(), method, index);
            }
            return buildResult;
        }
    }
}
