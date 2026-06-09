/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.backend.c.generators;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.ServiceLoader;
import org.teavm.backend.c.generate.CodeGeneratorUtil;
import org.teavm.backend.c.runtime.CResources;
import org.teavm.classlib.ResourceSupplier;
import org.teavm.classlib.ResourceSupplierContext;
import org.teavm.extension.ExtensionEnvironment;
import org.teavm.extension.spi.resources.ResourcesPolicy;
import org.teavm.interop.Address;
import org.teavm.model.CallLocation;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.parsing.resource.ResourceProvider;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.RuntimeClass;

public class CResourcesGenerator implements Generator {
    private static final MethodReference ALLOC_ARRAY_METHOD = new MethodReference(
            Allocator.class, "allocateArray", RuntimeClass.class, int.class, Address.class);
    private static final MethodReference CREATE_METHOD = new MethodReference(
            CResources.class, "create", String.class, int.class, int.class, CResources.Resource.class);

    private Properties properties;
    private ResourceProvider resourceProvider;
    private ClassLoader classLoader;
    private ExtensionEnvironment extensionEnv;

    public CResourcesGenerator(Properties properties, ResourceProvider resourceProvider,
            ClassLoader classLoader, ExtensionEnvironment extensionEnv) {
        this.properties = properties;
        this.resourceProvider = resourceProvider;
        this.classLoader = classLoader;
        this.extensionEnv = extensionEnv;
    }

    @Override
    public boolean canHandle(MethodReference method) {
        return method.getClassName().equals(CResources.class.getName())
                && method.getName().equals("acquireResources");
    }

    @Override
    public void generate(GeneratorContext context, MethodReference method) {
        var classSource = (ListableClassReaderSource) context.classSource();
        var supplierContext = new SupplierContextImpl(classLoader, classSource, properties);
        var resourceSet = new LinkedHashSet<String>();
        for (var supplier : ServiceLoader.load(ResourceSupplier.class, classLoader)) {
            var resources = supplier.supplyResources(supplierContext);
            if (resources != null) {
                resourceSet.addAll(Arrays.asList(resources));
            }
        }
        for (var policy : ServiceLoader.load(ResourcesPolicy.class, classLoader)) {
            policy.initialize(extensionEnv);
            var resources = policy.supplyResources(classSource.getClassNames());
            if (resources != null) {
                resourceSet.addAll(Arrays.asList(resources));
            }
        }

        var descriptors = new ArrayList<ResourceDescriptor>();
        var data = new ByteArrayOutputStream();
        var location = new CallLocation(new MethodReference(ClassLoader.class,
                "getResourceAsStream", String.class, InputStream.class));
        for (var resourceName : resourceSet) {
            var res = resourceProvider.getResource(resourceName);
            if (res == null) {
                context.diagnotics().error(location, "Resource not found: " + resourceName);
                continue;
            }
            try (var input = res.open()) {
                var start = data.size();
                input.transferTo(data);
                var end = data.size();
                descriptors.add(new ResourceDescriptor(resourceName, start, end));
            } catch (IOException e) {
                context.diagnotics().error(location, "Error occurred reading resource '" + resourceName + "'");
            }
        }

        writeHeaderFile(context);
        writeDataArray(context, data.toByteArray());
        writeFunctionBody(context, descriptors);
    }

    private void writeHeaderFile(GeneratorContext context) {
        var header = context.createHeaderFile("resources_gen.h");
        header.writer().println("extern const char teavm_resourceData[];");
    }

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private void writeDataArray(GeneratorContext context, byte[] data) {
        var writer = context.writerBefore();
        if (data.length == 0) {
            writer.println("const char teavm_resourceData[] = \"\";");
            return;
        }
        writer.println("const char teavm_resourceData[] =");
        var line = new StringBuilder("    \"");
        var lastHex = false;
        for (var b : data) {
            var v = b & 0xFF;
            var useHex = v < 32 || v > 126 || v == '"' || (lastHex && isHexDigit(v));
            var len = useHex ? 4 : (v == '\\' ? 2 : 1);
            if (line.length() + len > 80) {
                line.append('"');
                writer.println(line.toString());
                line.setLength(0);
                line.append("    \"");
                lastHex = false;
                useHex = v < 32 || v > 126 || v == '"';
            }
            if (useHex) {
                line.append('\\').append('x').append(HEX_CHARS[v >> 4]).append(HEX_CHARS[v & 0xF]);
                lastHex = true;
            } else if (v == '\\') {
                line.append("\\\\");
                lastHex = false;
            } else {
                line.append((char) v);
                lastHex = false;
            }
        }
        line.append("\";");
        writer.println(line.toString());
    }

    private static boolean isHexDigit(int c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
    }

    private void writeFunctionBody(GeneratorContext context, ArrayList<ResourceDescriptor> descriptors) {
        var writer = context.writer();
        var resourceType = ValueType.object(CResources.Resource.class.getName());
        var resourceArrayType = ValueType.arrayOf(resourceType);

        context.importMethod(ALLOC_ARRAY_METHOD, true);
        context.importMethod(CREATE_METHOD, true);
        context.includes().includePath("strings.h");
        context.includes().includePath("arrayclass.h");
        context.includes().includeClass(CResources.Resource.class.getName());

        var allocArrayName = context.names().forMethod(ALLOC_ARRAY_METHOD);
        var createName = context.names().forMethod(CREATE_METHOD);

        writer.print("TeaVM_Array* result = (TeaVM_Array*) " + allocArrayName + "(");
        CodeGeneratorUtil.writeTypeReference(writer, context.mainContext(), context.includes(), resourceArrayType);
        writer.println(", " + descriptors.size() + ");");
        writer.println("void** arrayData = TEAVM_ARRAY_DATA(result, void*);");

        for (int i = 0; i < descriptors.size(); i++) {
            var descriptor = descriptors.get(i);
            int nameIndex = context.stringPool().getStringIndex(descriptor.name);
            writer.print("arrayData[" + i + "] = " + createName + "(TEAVM_GET_STRING(" + nameIndex + "), ");
            CodeGeneratorUtil.writeIntValue(writer, descriptor.start);
            writer.print(", ");
            CodeGeneratorUtil.writeIntValue(writer, descriptor.end - descriptor.start);
            writer.println(");");
        }

        writer.println("return result;");
    }

    private static class ResourceDescriptor {
        String name;
        int start;
        int end;

        ResourceDescriptor(String name, int start, int end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }
    }

    private static class SupplierContextImpl implements ResourceSupplierContext {
        private ClassLoader classLoader;
        private ListableClassReaderSource classSource;
        private Properties properties;

        SupplierContextImpl(ClassLoader classLoader, ListableClassReaderSource classSource, Properties properties) {
            this.classLoader = classLoader;
            this.classSource = classSource;
            this.properties = properties;
        }

        @Override
        public ClassLoader getClassLoader() {
            return classLoader;
        }

        @Override
        public ListableClassReaderSource getClassSource() {
            return classSource;
        }

        @Override
        public Properties getProperties() {
            return properties;
        }
    }
}
