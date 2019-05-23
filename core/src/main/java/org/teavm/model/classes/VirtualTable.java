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

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.model.MethodDescriptor;

public class VirtualTable {
    private String className;
    private VirtualTable parent;
    private List<? extends MethodDescriptor> methods;
    private Set<MethodDescriptor> methodSet;
    private Map<MethodDescriptor, VirtualTableEntry> entryMap;

    VirtualTable(String className, VirtualTable parent, List<? extends MethodDescriptor> methods,
            Set<MethodDescriptor> methodSet, Map<MethodDescriptor, VirtualTableEntry> entryMap) {
        this.className = className;
        this.parent = parent;
        this.methods = methods;
        this.methodSet = methodSet;
        this.entryMap = entryMap;
    }

    public String getClassName() {
        return className;
    }

    public VirtualTable getParent() {
        return parent;
    }

    public List<? extends MethodDescriptor> getMethods() {
        return methods;
    }

    public VirtualTableEntry getEntry(MethodDescriptor method) {
        return entryMap.get(method);
    }

    public boolean hasMethod(MethodDescriptor method) {
        return methodSet.contains(method);
    }

    public VirtualTable findMethodContainer(MethodDescriptor method) {
        VirtualTable vt = this;
        while (vt != null) {
            if (vt.hasMethod(method)) {
                return vt;
            }
            vt = vt.getParent();
        }
        return null;
    }

    public int size() {
        return methods.size() + (parent != null ? parent.size() : 0);
    }
}
