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
package org.teavm.backend.wasm.generators.gc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.ServiceLoader;
import org.teavm.backend.wasm.generate.gc.methods.WasmGCGenerationUtil;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmMemorySegment;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.runtime.gc.WasmGCResources;
import org.teavm.classlib.ResourceSupplier;
import org.teavm.classlib.ResourceSupplierContext;
import org.teavm.model.CallLocation;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class WasmGCResourcesGenerator implements WasmGCCustomGenerator {
    private Properties properties;
    private ByteArrayOutputStream resources = new ByteArrayOutputStream();
    private WasmGlobal baseGlobal;

    public WasmGCResourcesGenerator(Properties properties) {
        this.properties = properties;
    }

    public void writeModule(WasmModule module) {
        if (resources.size() == 0) {
            return;
        }

        var segment = new WasmMemorySegment();
        if (!module.getSegments().isEmpty()) {
            var lastSegment = module.getSegments().get(module.getSegments().size() - 1);
            segment.setOffset(lastSegment.getOffset() + lastSegment.getLength());
        }
        segment.setData(resources.toByteArray());
        module.getSegments().add(segment);
        baseGlobal.setInitialValue(new WasmInt32Constant(segment.getOffset()));
    }

    @Override
    public void apply(MethodReference method, WasmFunction function, WasmGCCustomGeneratorContext context) {
        var supplierContext = new SupplierContextImpl(context.classLoader(), context.classes(), properties);
        var resourceSet = new LinkedHashSet<String>();
        for (var supplier : ServiceLoader.load(ResourceSupplier.class, context.classLoader())) {
            var resources = supplier.supplyResources(supplierContext);
            if (resources != null) {
                resourceSet.addAll(Arrays.asList(resources));
            }
        }

        var descriptors = new ArrayList<ResourceDescriptor>();
        var location = new CallLocation(new MethodReference(ClassLoader.class,
                "getResourceAsStream", String.class, InputStream.class));
        for (var resource : resourceSet) {
            try (var input = context.classLoader().getResourceAsStream(resource)) {
                if (input == null) {
                    context.diagnostics().error(location, "Resource not found: " + resource);
                } else {
                    var start = resources.size();
                    input.transferTo(resources);
                    var end = resources.size();
                    descriptors.add(new ResourceDescriptor(resource, start, end));
                }
            } catch (IOException e) {
                context.diagnostics().error(location, "Error occurred reading resource '" + resource + "'");
            }
        }

        if (!descriptors.isEmpty()) {
            baseGlobal = new WasmGlobal(context.names().topLevel("teavm@resourcesBaseAddress"),
                    WasmType.INT32, new WasmInt32Constant(0));
            context.module().globals.add(baseGlobal);

            var genUtil = new WasmGCGenerationUtil(context.classInfoProvider());
            var constructor = context.functions().forStaticMethod(new MethodReference(WasmGCResources.class,
                    "create", String.class, int.class, int.class, WasmGCResources.Resource.class));

            function.getBody().add(genUtil.allocateArrayWithElements(
                    ValueType.parse(WasmGCResources.Resource.class),
                    () -> {
                        var items = new ArrayList<WasmExpression>();
                        for (var descriptor : descriptors) {
                            var name = context.strings().getStringConstant(descriptor.name);
                            var offset = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD,
                                    new WasmGetGlobal(baseGlobal), new WasmInt32Constant(descriptor.address));
                            var end = new WasmInt32Constant(descriptor.end - descriptor.address);
                            items.add(new WasmCall(constructor, new WasmGetGlobal(name.global), offset, end));
                        }
                        return items;
                    }
            ));
        }
    }

    private static class ResourceDescriptor {
        String name;
        int address;
        int end;

        ResourceDescriptor(String name, int address, int end) {
            this.name = name;
            this.address = address;
            this.end = end;
        }
    }

    static class SupplierContextImpl implements ResourceSupplierContext {
        private ClassLoader classLoader;
        private ListableClassReaderSource classSource;
        private Properties properties;

        SupplierContextImpl(ClassLoader classLoader, ListableClassReaderSource classSource,
                Properties properties) {
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
