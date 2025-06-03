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
import org.teavm.common.HashUtils;
import org.teavm.common.ServiceRepository;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.parsing.resource.ResourceProvider;
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.ResourceArray;
import org.teavm.platform.metadata.ResourceMap;
import org.teavm.platform.metadata.builders.ObjectResourceBuilder;
import org.teavm.platform.metadata.builders.ResourceArrayBuilder;
import org.teavm.platform.metadata.builders.ResourceMapBuilder;

class MetadataCIntrinsic implements Generator {
    private Set<String> writtenStructures = new HashSet<>();
    private Set<MethodReference> writtenInitializers = new HashSet<>();
    private DefaultMetadataGeneratorContext metadataContext;
    private Map<MethodReference, MethodGenerator> generatorMap = new HashMap<>();

    void init(ClassReaderSource classSource, ResourceProvider resourceProvider, ClassLoader classLoader,
            ServiceRepository services, Properties properties) {
        metadataContext = new DefaultMetadataGeneratorContext(classSource, resourceProvider,
                classLoader, properties, services);
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
            context.writerBefore().print("(TeaVM_Object**) TEAVM_GET_STRING_ADDRESS(" + stringIndex + ")");
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
        } else if (value instanceof ObjectResourceBuilder) {
            writeResource(context, (ObjectResourceBuilder) value);
        } else if (value instanceof ResourceMapBuilder) {
            writeResourceMap(context, (ResourceMapBuilder<?>) value);
        } else if (value instanceof ResourceArrayBuilder) {
            writeResourceArray(context, (ResourceArrayBuilder<?>) value);
        } else {
            throw new IllegalArgumentException("Don't know how to write resource: " + value);
        }
    }

    private void writeResource(GeneratorContext context, ObjectResourceBuilder value) {
        writeResourceStructure(context, value.getOutputClass().getName());
        var resourceType = value.getOutputClass().getName();

        String structureName = context.names().forClass(resourceType);
        context.writerBefore().print("&(" + structureName + ") {").indent();
        boolean first = true;
        var names = value.fieldNames();
        for (var i = 0; i < names.length; ++i) {
            if (!first) {
                context.writerBefore().print(",");
            }
            first = false;
            context.writerBefore().println().print(".").print(names[i]).print(" = ");
            Object propertyValue = value.getValue(i);
            writeValue(context, propertyValue);
        }
        context.writerBefore().println().outdent().print("}");
    }

    private void writeResourceStructure(GeneratorContext context, String className) {
        String fileName = "resources/" + context.escapeFileName(className) + ".h";
        context.includes().includePath(fileName);

        if (!writtenStructures.add(className)) {
            return;
        }

        var cls = context.classSource().get(className);
        var hierarchy = context.hierarchy();
        var descriptor = new ResourceTypeDescriptor(context.hierarchy(), cls);
        for (var propertyType : descriptor.getPropertyTypes().values()) {
            if (isResourceStructureType(hierarchy, propertyType)) {
                writeResourceStructure(context, ((ValueType.Object) propertyType).getClassName());
            }
        }

        FileGenerator file = context.createHeaderFile(fileName);

        String structureName = context.names().forClass(className);
        file.writer().println("typedef struct " + structureName + " {").indent();
        file.includes().includePath("runtime.h");

        for (var entry : descriptor.getPropertyTypes().entrySet()) {
            var propertyName = entry.getKey();
            var propertyType = entry.getValue();
            file.writer().printType(propertyType).print(" " + propertyName + ";").println();
        }

        file.writer().outdent().println("} " + structureName + ";");
    }

    private boolean isResourceStructureType(ClassHierarchy hierarchy, ValueType type) {
        if (!(type instanceof ValueType.Object)) {
            return false;
        }
        var className = ((ValueType.Object) type).getClassName();
        if (className.equals(ResourceArray.class.getName()) || className.equals(ResourceMap.class.getName())) {
            return false;
        }
        return hierarchy.isSuperType(Resource.class.getName(), className, false);
    }

    private void writeResourceArray(GeneratorContext context, ResourceArrayBuilder<?> resourceArray) {
        context.writerBefore().println("&(struct { int32_t size; void* data[" + resourceArray.values.size()
                        + "]; }) {")
                .indent();
        context.writerBefore().println(".size = " + resourceArray.values.size() + ",");
        context.writerBefore().print(".data = {").indent();

        boolean first = true;
        for (int i = 0; i < resourceArray.values.size(); ++i) {
            if (!first) {
                context.writerBefore().print(",");
            }
            context.writerBefore().println();
            first = false;
            writeValue(context, resourceArray.values.get(i));
        }

        context.writerBefore().println().outdent().println("}");
        context.writerBefore().outdent().print("}");
    }

    private void writeResourceMap(GeneratorContext context, ResourceMapBuilder<?> resourceMap) {
        String[] bestTable = HashUtils.createHashTable(resourceMap.values.keySet().toArray(new String[0]));
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
                context.writerBefore().print("{ TEAVM_GET_STRING_ADDRESS("
                        + context.stringPool().getStringIndex(key) + "), ");
                writeValue(context, resourceMap.values.get(key));
                context.writerBefore().print("}");
            }
        }

        context.writerBefore().println().outdent().println("}");
        context.writerBefore().outdent().print("}");
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
                var resource = generator.generateMetadata(metadataContext, targetMethod);
                writeValue(context, resource);
            }
            context.writerBefore().println(";");
        }
    }
}
