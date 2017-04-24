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
import java.util.HashMap;
import java.util.Map;
import org.teavm.backend.javascript.codegen.SourceWriter;

class BuildTimeResourceProxyBuilder {
    private Map<Class<?>, BuildTimeResourceProxyFactory> factories = new HashMap<>();
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
        return factories.computeIfAbsent(iface, k -> createFactory(iface)).create();
    }

    private BuildTimeResourceProxyFactory createFactory(Class<?> iface) {
        return new ProxyFactoryCreation(new ResourceTypeDescriptor(iface)).create();
    }

    private static class ProxyFactoryCreation {
        Map<Method, BuildTimeResourceMethod> methods = new HashMap<>();
        private Map<String, Integer> propertyIndexes = new HashMap<>();
        private Object[] initialData;
        private ResourceTypeDescriptor descriptor;

        public ProxyFactoryCreation(ResourceTypeDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        BuildTimeResourceProxyFactory create() {
            for (Map.Entry<Method, ResourceMethodDescriptor> entry : descriptor.getMethods().entrySet()) {
                Method method = entry.getKey();
                ResourceMethodDescriptor methodDescriptor = entry.getValue();
                int index = getPropertyIndex(methodDescriptor.getPropertyName());
                switch (methodDescriptor.getType()) {
                    case GETTER:
                        methods.put(method, new BuildTimeResourceGetter(index));
                        break;
                    case SETTER:
                        methods.put(method, new BuildTimeResourceSetter(index));
                        break;
                }
            }

            // Fill default values
            initialData = new Object[propertyIndexes.size()];
            for (Map.Entry<String, Class<?>> property : descriptor.getPropertyTypes().entrySet()) {
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
            String[] properties = new String[descriptor.getPropertyTypes().size()];
            for (Map.Entry<String, Integer> entry : propertyIndexes.entrySet()) {
                properties[entry.getValue()] = entry.getKey();
            }
            methods.put(writeMethod, new BuildTimeResourceWriterMethod(properties));

            for (Method method : ResourceTypeDescriptorProvider.class.getDeclaredMethods()) {
                switch (method.getName()) {
                    case "getDescriptor":
                        methods.put(method, (proxy, args) -> descriptor);
                        break;
                    case "getValues":
                        methods.put(method, (proxy, args) -> proxy.data.clone());
                        break;
                    case "getPropertyIndex":
                        methods.put(method, (proxy, args) -> propertyIndexes.getOrDefault(args[0], -1));
                        break;
                }
            }

            return new BuildTimeResourceProxyFactory(methods, initialData);
        }

        private int getPropertyIndex(String propertyName) {
            return propertyIndexes.computeIfAbsent(propertyName, k -> propertyIndexes.size());
        }
    }
}
