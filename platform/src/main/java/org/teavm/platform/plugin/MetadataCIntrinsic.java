/*
 *  Copyright 2018 Alexey Andreev.
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
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.teavm.backend.c.generate.FileGenerator;
import org.teavm.backend.c.generators.Generator;
import org.teavm.backend.c.generators.GeneratorContext;
import org.teavm.common.ServiceRepository;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.ResourceArray;
import org.teavm.platform.metadata.ResourceMap;

class MetadataCIntrinsic implements Generator {
    private Set<String> writtenStructures = new HashSet<>();
    private Set<MethodReference> writtenInitializers = new HashSet<>();
    private DefaultMetadataGeneratorContext metadataContext;
    private Map<MethodReference, MethodGenerator> generatorMap = new HashMap<>();

    void init(ClassReaderSource classSource, ClassLoader classLoader,
            ServiceRepository services, Properties properties) {
        metadataContext = new DefaultMetadataGeneratorContext(classSource, classLoader, properties, services);
    }

    public void addGenerator(MethodReference constructor, MethodReference method, MetadataGenerator generator) {
        generatorMap.put(constructor, new MethodGenerator(method, generator));
    }

    @Override
    public boolean canHandle(MethodReference methodReference) {
        return generatorMap.containsKey(methodReference);
    }

    @Override
    public void generate(GeneratorContext context, MethodReference method) {
        MethodGenerator generator = generatorMap.get(method);
        context.writer().print("return ");
        generator.apply(context, method);
        context.writer().println(";");
    }

    void writeValue(GeneratorContext context, Object value) {
        if (value == null) {
            context.writerBefore().print("NULL");
        } else if (value instanceof String) {
            int stringIndex = context.stringPool().getStringIndex((String) value);
            context.includes().includePath("strings.h");
            context.writerBefore().print("(TeaVM_Object**) &TEAVM_GET_STRING(" + stringIndex + ")");
        } else if (value instanceof Boolean) {
            context.writerBefore().print((Boolean) value ? "1" : "0");
        } else if (value instanceof Integer) {
            int n = (Integer) value;
            if (n < 0) {
                context.writerBefore().print("-");
                n = -n;
            }
            context.writerBefore().print("INT32_C(" + n + ")");
        } else if (value instanceof Long) {
            long n = (Long) value;
            if (n < 0) {
                context.writerBefore().print("-");
                n = -n;
            }
            context.writerBefore().print("INT64_C(" + n + ")");
        } else if (value instanceof Byte || value instanceof Short || value instanceof Float
                || value instanceof Double) {
            context.writerBefore().print(value.toString());
        } else if (value instanceof ResourceTypeDescriptorProvider && value instanceof Resource) {
            writeResource(context, (ResourceTypeDescriptorProvider) value);
        } else if (value instanceof ResourceMap) {
            writeResourceMap(context, (ResourceMap<?>) value);
        } else if (value instanceof ResourceArray) {
            writeResourceArray(context, (ResourceArray<?>) value);
        } else {
            throw new IllegalArgumentException("Don't know how to write resource: " + value);
        }
    }

    private void writeResource(GeneratorContext context, ResourceTypeDescriptorProvider resourceType) {
        writeResourceStructure(context, resourceType.getDescriptor());

        String structureName = context.names().forClass(resourceType.getDescriptor().getRootInterface().getName());
        Object[] propertyValues = resourceType.getValues();
        context.writerBefore().print("&(" + structureName + ") {").indent();
        boolean first = true;
        for (String propertyName : resourceType.getDescriptor().getPropertyTypes().keySet()) {
            if (!first) {
                context.writerBefore().print(",");
            }
            first = false;
            context.writerBefore().println().print(".").print(propertyName).print(" = ");
            int index = resourceType.getPropertyIndex(propertyName);
            Object propertyValue = propertyValues[index];
            writeValue(context, propertyValue);
        }
        context.writerBefore().println().outdent().print("}");
    }

    private void writeResourceStructure(GeneratorContext context, ResourceTypeDescriptor structure) {
        String className = structure.getRootInterface().getName();
        String fileName = "resources/" + context.escapeFileName(className) + ".h";
        context.includes().includePath(fileName);

        if (!writtenStructures.add(className)) {
            return;
        }

        for (Class<?> propertyType : structure.getPropertyTypes().values()) {
            if (Resource.class.isAssignableFrom(propertyType) && !ResourceMap.class.isAssignableFrom(propertyType)
                    && !ResourceArray.class.isAssignableFrom(propertyType)) {
                ResourceTypeDescriptor propertyStructure = metadataContext.getTypeDescriptor(
                        propertyType.asSubclass(Resource.class));
                writeResourceStructure(context, propertyStructure);
            }
        }

        FileGenerator file = context.createHeaderFile(fileName);

        String structureName = context.names().forClass(className);
        file.writer().println("typedef struct " + structureName + " {").indent();
        file.includes().includePath("runtime.h");

        for (String propertyName : structure.getPropertyTypes().keySet()) {
            Class<?> propertyType = structure.getPropertyTypes().get(propertyName);
            file.writer().println(typeToString(propertyType) + " " + propertyName + ";");
        }

        file.writer().outdent().println("} " + structureName + ";");
    }

    private String typeToString(Class<?> cls) {
        if (cls == boolean.class || cls == byte.class) {
            return "int8_t";
        } else if (cls == short.class || cls == char.class) {
            return "int16_t";
        } else if (cls == int.class) {
            return "int32_t";
        } else if (cls == float.class) {
            return "float";
        } else if (cls == long.class) {
            return "int64_t";
        } else if (cls == double.class) {
            return "double";
        } else if (Resource.class.isAssignableFrom(cls)) {
            return "void*";
        } else if (cls == String.class) {
            return "TeaVM_Object**";
        } else {
            throw new IllegalArgumentException("Don't know how to write resource type " + cls);
        }
    }

    private void writeResourceArray(GeneratorContext context, ResourceArray<?> resourceArray) {
        context.writerBefore().println("&(struct { int32_t size; void* data[" + resourceArray.size() + "]; }) {")
                .indent();
        context.writerBefore().println(".size = " + resourceArray.size() + ",");
        context.writerBefore().print(".data = {").indent();

        boolean first = true;
        for (int i = 0; i < resourceArray.size(); ++i) {
            if (!first) {
                context.writerBefore().print(",");
            }
            context.writerBefore().println();
            first = false;
            writeValue(context, resourceArray.get(i));
        }

        context.writerBefore().println().outdent().println("}");
        context.writerBefore().outdent().print("}");
    }

    private void writeResourceMap(GeneratorContext context, ResourceMap<?> resourceMap) {
        String[] keys = resourceMap.keys();
        int tableSize = keys.length * 2;
        int maxTableSize = Math.min(keys.length * 5 / 2, tableSize + 10);

        String[] bestTable = null;
        int bestCollisionRatio = 0;
        while (tableSize <= maxTableSize) {
            String[] table = new String[tableSize];
            int maxCollisionRatio = 0;
            for (String key : keys) {
                int hashCode = key.hashCode();
                int collisionRatio = 0;
                while (true) {
                    int index = mod(hashCode++, table.length);
                    if (table[index] == null) {
                        table[index] = key;
                        break;
                    }
                    collisionRatio++;
                }
                maxCollisionRatio = Math.max(maxCollisionRatio, collisionRatio);
            }

            if (bestTable == null || bestCollisionRatio > maxCollisionRatio) {
                bestCollisionRatio = maxCollisionRatio;
                bestTable = table;
            }

            tableSize++;
        }

        context.includes().includePath("resource.h");
        context.writerBefore().println("&(struct { int32_t size; TeaVM_ResourceMapEntry entries["
                + bestTable.length + "]; }) {").indent();
        context.writerBefore().println(".size = " + bestTable.length + ",");
        context.writerBefore().print(".entries = {").indent();

        boolean first = true;
        for (String key : bestTable) {
            if (!first) {
                context.writerBefore().print(",");
            }
            context.writerBefore().println();
            first = false;
            if (key == null) {
                context.writerBefore().print("{ NULL, NULL }");
            } else {
                context.writerBefore().print("{ &TEAVM_GET_STRING("
                        + context.stringPool().getStringIndex(key) + "), ");
                writeValue(context, resourceMap.get(key));
                context.writerBefore().print("}");
            }
        }

        context.writerBefore().println().outdent().println("}");
        context.writerBefore().outdent().print("}");
    }

    private static int mod(int a, int b) {
        a %= b;
        if (a < 0) {
            a += b;
        }
        return a;
    }

    class MethodGenerator {
        private MethodReference targetMethod;
        private MetadataGenerator generator;

        MethodGenerator(MethodReference targetMethod, MetadataGenerator generator) {
            this.targetMethod = targetMethod;
            this.generator = generator;
        }

        public void apply(GeneratorContext context, MethodReference methodReference) {
            writeInitializer(context, methodReference);
            context.writer().print("resource_" + context.names().forMethod(methodReference));
        }

        private void writeInitializer(GeneratorContext context, MethodReference methodReference) {
            if (!writtenInitializers.add(methodReference)) {
                return;
            }

            String variableName = "resource_" + context.names().forMethod(methodReference);
            context.writerBefore().print("static ").printType(methodReference.getReturnType()).print(" ")
                    .print(variableName).print(" = ");
            if (generator == null) {
                context.writerBefore().print("NULL");
            } else {
                Resource resource = generator.generateMetadata(metadataContext, targetMethod);
                writeValue(context, resource);
            }
            context.writerBefore().println(";");
        }
    }
}
