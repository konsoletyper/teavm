/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.platform.plugin;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReader;
import org.teavm.model.MethodReader;
import org.teavm.model.ValueType;

public class ResourceTypeDescriptor {
    private static Set<ValueType> allowedPropertyTypes = Set.of(
            ValueType.BOOLEAN,
            ValueType.BYTE,
            ValueType.SHORT,
            ValueType.INTEGER,
            ValueType.FLOAT,
            ValueType.DOUBLE,
            ValueType.object("java.lang.String"),
            ValueType.object("org.teavm.platform.metadata.ResourceArray"),
            ValueType.object("org.teavm.platform.metadata.ResourceMap")
    );
    private ClassHierarchy hierarchy;
    private ClassReader rootIface;
    private Map<String, ValueType> getters = new HashMap<>();
    private Map<MethodReader, ResourceMethodDescriptor> methods = new LinkedHashMap<>();
    private Map<String, ValueType> propertyTypes = new LinkedHashMap<>();

    public ResourceTypeDescriptor(ClassHierarchy hierarchy, ClassReader iface) {
        this.hierarchy = hierarchy;
        this.rootIface = iface;
        scanIface(rootIface);
    }

    public ClassReader getRootInterface() {
        return rootIface;
    }

    public Map<MethodReader, ResourceMethodDescriptor> getMethods() {
        return methods;
    }

    public Map<String, ValueType> getPropertyTypes() {
        return propertyTypes;
    }

    private void scanIface(ClassReader iface) {
        // Scan methods
        getters.clear();
        for (var method : iface.getMethods()) {
            if (method.getName().startsWith("get")) {
                scanGetter(method);
            } else if (method.getName().startsWith("is")) {
                scanBooleanGetter(method);
            } else {
                throwInvalidMethod(method);
            }
        }

        // Verify types of properties
        for (var property : getters.entrySet()) {
            String propertyName = property.getKey();
            var propertyType = property.getValue();
            if (!allowedPropertyTypes.contains(propertyType)) {
                if (!isAllowedCustomType(propertyType)) {
                    throw new IllegalArgumentException("Property " + rootIface.getName() + "." + propertyName
                            + " has an illegal type " + propertyType);
                }
            }
            if (!propertyTypes.containsKey(propertyName)) {
                propertyTypes.put(propertyName, propertyType);
            }
        }

        // Scan superinterfaces
        for (var superIfaceName : iface.getInterfaces()) {
            var superIface = hierarchy.getClassSource().get(superIfaceName);
            if (superIface != null) {
                scanIface(superIface);
            }
        }
    }

    private boolean isAllowedCustomType(ValueType type) {
        if (!(type instanceof ValueType.Object)) {
            return false;
        }
        var className = ((ValueType.Object) type).getClassName();
        var cls = hierarchy.getClassSource().get(className);
        return cls != null;
    }

    private void throwInvalidMethod(MethodReader method) {
        throw new IllegalArgumentException("Method " + method.getOwnerName() + "."
                + method.getName() + " is not likely to be either getter or setter");
    }

    private void scanGetter(MethodReader method) {
        String propertyName = extractPropertyName(method.getName().substring(3));
        if (propertyName == null || method.getResultType() == ValueType.VOID
                || method.getParameterTypes().length > 0) {
            throwInvalidMethod(method);
        }
        if (getters.put(propertyName, method.getResultType()) != null) {
            throw new IllegalArgumentException("Method " + method.getOwnerName() + "."
                    + method.getName() + " is a duplicate getter for property " + propertyName);
        }
        methods.put(method, new ResourceMethodDescriptor(propertyName, ResourceAccessorType.GETTER));
    }

    private void scanBooleanGetter(MethodReader method) {
        String propertyName = extractPropertyName(method.getName().substring(2));
        if (propertyName == null || method.getResultType() != ValueType.BOOLEAN
                || method.getParameterTypes().length > 0) {
            throwInvalidMethod(method);
        }
        if (getters.put(propertyName, method.getResultType()) != null) {
            throw new IllegalArgumentException("Method " + method.getOwnerName() + "."
                    + method.getName() + " is a duplicate getter for property " + propertyName);
        }
        methods.put(method, new ResourceMethodDescriptor(propertyName, ResourceAccessorType.GETTER));
    }

    private String extractPropertyName(String propertyName) {
        if (propertyName.isEmpty()) {
            return null;
        }
        char c = propertyName.charAt(0);
        if (c != Character.toUpperCase(c)) {
            return null;
        }
        if (propertyName.length() == 1) {
            return propertyName.toLowerCase();
        }
        c = propertyName.charAt(1);
        if (c == Character.toUpperCase(c)) {
            return propertyName;
        } else {
            return Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
        }
    }
}
