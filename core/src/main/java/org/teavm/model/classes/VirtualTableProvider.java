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
package org.teavm.model.classes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;

public class VirtualTableProvider {
    private ClassReaderSource classSource;
    private Map<String, Set<MethodDescriptor>> virtualMethodMap = new HashMap<>();
    private Map<String, VirtualTable> virtualTables = new LinkedHashMap<>();
    private InterfaceToClassMapping interfaceMapping;

    public VirtualTableProvider(ListableClassReaderSource classSource, Set<MethodReference> virtualMethods) {
        this.classSource = classSource;
        interfaceMapping = new InterfaceToClassMapping(classSource);

        Set<String> classNames = new HashSet<>(classSource.getClassNames());
        for (MethodReference virtualMethod : virtualMethods) {
            String cls = interfaceMapping.mapClass(virtualMethod.getClassName());
            if (cls == null) {
                cls = virtualMethod.getClassName();
            }
            classNames.add(cls);
            virtualMethodMap.computeIfAbsent(cls, c -> new LinkedHashSet<>()).add(virtualMethod.getDescriptor());
        }

        for (String className : classNames) {
            fillClass(className);
        }
    }

    private void fillClass(String className) {
        if (virtualTables.containsKey(className)) {
            return;
        }

        VirtualTable table = new VirtualTable(className);
        virtualTables.put(className, table);
        ClassReader cls = classSource.get(className);
        if (cls == null) {
            return;
        }
        if (cls.getParent() != null) {
            fillClass(cls.getParent());
            VirtualTable parentTable = virtualTables.get(cls.getParent());
            for (VirtualTableEntry parentEntry : parentTable.entries.values()) {
                VirtualTableEntry entry = new VirtualTableEntry(table, parentEntry.getMethod(),
                        parentEntry.getImplementor(), parentEntry.getIndex());
                table.entries.put(entry.getMethod(), entry);
            }
        }

        Set<MethodDescriptor> newDescriptors = virtualMethodMap.get(className);
        if (newDescriptors != null) {
            for (MethodDescriptor method : newDescriptors) {
                if (!table.entries.containsKey(method)) {
                    MethodReader implementation = classSource.resolveImplementation(
                            className, method);
                    MethodReference implementationRef = implementation != null
                            ? implementation.getReference()
                            : null;
                    table.entries.put(method, new VirtualTableEntry(table, method, implementationRef,
                            table.entries.size()));
                }
            }
        }

        for (MethodReader method : cls.getMethods()) {
            if (method.hasModifier(ElementModifier.ABSTRACT)) {
                continue;
            }
            VirtualTableEntry entry = table.entries.get(method.getDescriptor());
            if (entry != null) {
                entry.implementor = method.getReference();
            }
        }
    }

    public VirtualTableEntry lookup(MethodReference method) {
        VirtualTable vtable = virtualTables.get(interfaceMapping.mapClass(method.getClassName()));
        if (vtable == null) {
            return null;
        }
        return vtable.getEntries().get(method.getDescriptor());
    }

    public VirtualTable lookup(String className) {
        return virtualTables.get(interfaceMapping.mapClass(className));
    }
}
