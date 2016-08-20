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
package org.teavm.platform.plugin;

import java.lang.reflect.Method;
import java.util.*;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.ResourceArray;
import org.teavm.platform.metadata.ResourceMap;

/**
 *
 * @author Alexey Andreev
 */
class BuildTimeResourceProxyBuilder {
    private Map<Class<?>, BuildTimeResourceProxyFactory> factories = new HashMap<>();
    private static Set<Class<?>> allowedPropertyTypes = new HashSet<>(Arrays.asList(
            boolean.class, byte.class, short.class, int.class, float.class, double.class,
            String.class, ResourceArray.class, ResourceMap.class));
    private static Map<Class<?>, Object> defaultValues = new HashMap<>();

    static {
        defaultValues.put(boolean.class, false);
        defaultValues.put(byte.class, (byte) 0);
        defaultValues.put(short.class, (short) 0);
        defaultValues.put(int.class, 0);
        defaultValues.put(float.class, 0F);
        defaultValues.put(double.class, 0.0);
    }

    public BuildTimeResourceProxy buildProxy(Class<?> iface) {
        BuildTimeResourceProxyFactory factory = factories.get(iface);
        if (factory == null) {
            factory = createFactory(iface);
            factories.put(iface, factory);
        }
        return factory.create();
    }

    private BuildTimeResourceProxyFactory createFactory(Class<?> iface) {
        return new ProxyFactoryCreation(iface).create();
    }

    private static class ProxyFactoryCreation {
        private Class<?> rootIface;
        Map<String, Class<?>> getters = new HashMap<>();
        Map<String, Class<?>> setters = new HashMap<>();
        Map<Method, BuildTimeResourceMethod> methods = new HashMap<>();
        private Map<String, Integer> propertyIndexes = new HashMap<>();
        private Object[] initialData;
        private Map<String, Class<?>> propertyTypes = new HashMap<>();

        public ProxyFactoryCreation(Class<?> iface) {
            this.rootIface = iface;
        }

        BuildTimeResourceProxyFactory create() {
            if (!rootIface.isInterface()) {
                throw new IllegalArgumentException("Error creating a new resource of type " + rootIface.getName()
                        + " that is not an interface");
            }
            scanIface(rootIface);

            // Fill default values
            initialData = new Object[propertyIndexes.size()];
            for (Map.Entry<String, Class<?>> property : propertyTypes.entrySet()) {
                String propertyName = property.getKey();
                Class<?> propertyType = property.getValue();
                initialData[propertyIndexes.get(propertyName)] = defaultValues.get(propertyType);
            }

            // Generate write method
            Method writeMethod;
            try {
                writeMethod = ResourceWriter.class.getMethod("write", SourceWriter.class);
            } catch (NoSuchMethodException e) {
                throw new AssertionError("Method must exist", e);
            }

            // Create factory
            String[] properties = new String[propertyIndexes.size()];
            for (Map.Entry<String, Integer> entry : propertyIndexes.entrySet()) {
                properties[entry.getValue()] = entry.getKey();
            }
            methods.put(writeMethod, new BuildTimeResourceWriterMethod(properties));
            return new BuildTimeResourceProxyFactory(methods, initialData);
        }

        private void scanIface(Class<?> iface) {
            if (!Resource.class.isAssignableFrom(iface)) {
                throw new IllegalArgumentException("Error creating a new resource of type " + iface.getName() + "."
                        + " This type does not implement the " + Resource.class.getName() + " interface");
            }

            // Scan methods
            getters.clear();
            setters.clear();
            for (Method method : iface.getDeclaredMethods()) {
                if (method.getName().startsWith("get")) {
                    scanGetter(method);
                } else if (method.getName().startsWith("is")) {
                    scanBooleanGetter(method);
                } else if (method.getName().startsWith("set")) {
                    scanSetter(method);
                } else {
                    throwInvalidMethod(method);
                }
            }

            // Verify consistency of getters and setters
            for (Map.Entry<String, Class<?>> property : getters.entrySet()) {
                String propertyName = property.getKey();
                Class<?> getterType = property.getValue();
                Class<?> setterType = setters.get(propertyName);
                if (setterType == null) {
                    throw new IllegalArgumentException("Property " + iface.getName() + "." + propertyName
                            + " has a getter, but does not have a setter");
                }
                if (!setterType.equals(getterType)) {
                    throw new IllegalArgumentException("Property " + iface.getName() + "." + propertyName
                            + " has a getter and a setter of different types");
                }
            }
            for (String propertyName : setters.keySet()) {
                if (!getters.containsKey(propertyName)) {
                    throw new IllegalArgumentException("Property " + iface.getName() + "." + propertyName
                            + " has a setter, but does not have a getter");
                }
            }

            // Verify types of properties
            for (Map.Entry<String, Class<?>> property : getters.entrySet()) {
                String propertyName = property.getKey();
                Class<?> propertyType = property.getValue();
                if (!allowedPropertyTypes.contains(propertyType)) {
                    if (!propertyType.isInterface() || !Resource.class.isAssignableFrom(propertyType)) {
                        throw new IllegalArgumentException("Property " + rootIface.getName() + "." + propertyName
                                + " has an illegal type " + propertyType.getName());
                    }
                }
                if (!propertyTypes.containsKey(propertyName)) {
                    propertyTypes.put(propertyName, propertyType);
                }
            }

            // Scan superinterfaces
            for (Class<?> superIface : iface.getInterfaces()) {
                scanIface(superIface);
            }
        }

        private void throwInvalidMethod(Method method) {
            throw new IllegalArgumentException("Method " + method.getDeclaringClass().getName() + "."
                    + method.getName() + " is not likely to be either getter or setter");
        }

        private void scanGetter(Method method) {
            String propertyName = extractPropertyName(method.getName().substring(3));
            if (propertyName == null || method.getReturnType().equals(void.class)
                    || method.getParameterTypes().length > 0) {
                throwInvalidMethod(method);
            }
            if (getters.put(propertyName, method.getReturnType()) != null) {
                throw new IllegalArgumentException("Method " + method.getDeclaringClass().getName() + "."
                        + method.getName() + " is a duplicate getter for property " + propertyName);
            }
            methods.put(method, new BuildTimeResourceGetter(getPropertyIndex(propertyName)));
        }

        private void scanBooleanGetter(Method method) {
            String propertyName = extractPropertyName(method.getName().substring(2));
            if (propertyName == null || !method.getReturnType().equals(boolean.class)
                    || method.getParameterTypes().length > 0) {
                throwInvalidMethod(method);
            }
            if (getters.put(propertyName, method.getReturnType()) != null) {
                throw new IllegalArgumentException("Method " + method.getDeclaringClass().getName() + "."
                        + method.getName() + " is a duplicate getter for property " + propertyName);
            }
            methods.put(method, new BuildTimeResourceGetter(getPropertyIndex(propertyName)));
        }

        private void scanSetter(Method method) {
            String propertyName = extractPropertyName(method.getName().substring(3));
            if (propertyName == null || !method.getReturnType().equals(void.class)
                    || method.getParameterTypes().length != 1) {
                throwInvalidMethod(method);
            }
            if (setters.put(propertyName, method.getParameterTypes()[0]) != null) {
                throw new IllegalArgumentException("Method " + method.getDeclaringClass().getName() + "."
                        + method.getName() + " is a duplicate setter for property " + propertyName);
            }
            methods.put(method, new BuildTimeResourceSetter(getPropertyIndex(propertyName)));
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

        private int getPropertyIndex(String propertyName) {
            Integer index = propertyIndexes.get(propertyName);
            if (index == null) {
                index = propertyIndexes.size();
                propertyIndexes.put(propertyName, index);
            }
            return index;
        }
    }
}
