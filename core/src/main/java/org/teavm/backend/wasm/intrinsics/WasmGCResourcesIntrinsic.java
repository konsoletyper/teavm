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
package org.teavm.backend.wasm.intrinsics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.ServiceLoader;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.generate.methods.WasmGCGenerationUtil;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmMemorySegment;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmInt32Constant;
import org.teavm.backend.wasm.model.instruction.WasmInt32Subtype;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.backend.wasm.runtime.WasmGCResources;
import org.teavm.classlib.ResourceSupplier;
import org.teavm.classlib.ResourceSupplierContext;
import org.teavm.extension.ExtensionEnvironment;
import org.teavm.extension.spi.resources.ResourcesPolicy;
import org.teavm.model.CallLocation;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class WasmGCResourcesIntrinsic implements WasmGCInlineIntrinsic {
    private Properties properties;
    private ByteArrayOutputStream resources = new ByteArrayOutputStream();
    private WasmGlobal baseGlobal;
    private WasmGCCodeGenContext genContext;
    private ExtensionEnvironment extensionEnv;

    public WasmGCResourcesIntrinsic(Properties properties, WasmGCCodeGenContext genContext,
            ExtensionEnvironment extensionEnv) {
        this.properties = properties;
        this.genContext = genContext;
        this.extensionEnv = extensionEnv;
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
        baseGlobal.getInitialValue().clear();
        baseGlobal.getInitialValue().add(new WasmInt32Constant(segment.getOffset()));
    }

    @Override
    public void apply(InvocationExpr invocation, WasmGCInlineIntrinsicContext context, WasmInstructionBuilder builder) {
        switch (invocation.getMethod().getName()) {
            case "acquireResources":
                acquireResources(builder);
                break;
            case "readSingleByte":
                context.generate(builder, invocation.getArguments().get(0));
                builder.loadI32(1, 0, WasmInt32Subtype.UINT8);
                break;
        }
    }

    private void acquireResources(WasmInstructionBuilder builder) {
        var supplierContext = new SupplierContextImpl(genContext.classLoader(), genContext.classes(), properties);
        var resourceSet = new LinkedHashSet<String>();
        for (var supplier : ServiceLoader.load(ResourceSupplier.class, genContext.classLoader())) {
            var resources = supplier.supplyResources(supplierContext);
            if (resources != null) {
                resourceSet.addAll(Arrays.asList(resources));
            }
        }
        for (var policy : ServiceLoader.load(ResourcesPolicy.class, genContext.classLoader())) {
            policy.initialize(extensionEnv);
            var resources = policy.supplyResources(genContext.classes().getClassNames());
            if (resources != null) {
                resourceSet.addAll(Arrays.asList(resources));
            }
        }

        var descriptors = new ArrayList<ResourceDescriptor>();
        var location = new CallLocation(new MethodReference(ClassLoader.class,
                "getResourceAsStream", String.class, InputStream.class));
        for (var resource : resourceSet) {
            try (var input = genContext.classLoader().getResourceAsStream(resource)) {
                if (input == null) {
                    genContext.diagnostics().error(location, "Resource not found: " + resource);
                } else {
                    var start = resources.size();
                    input.transferTo(resources);
                    var end = resources.size();
                    descriptors.add(new ResourceDescriptor(resource, start, end));
                }
            } catch (IOException e) {
                genContext.diagnostics().error(location, "Error occurred reading resource '" + resource + "'");
            }
        }

        baseGlobal = new WasmGlobal(genContext.names().topLevel("teavm@resourcesBaseAddress"),
                WasmType.INT32);
        genContext.module().globals.add(baseGlobal);

        var constructor = genContext.functions().forStaticMethod(new MethodReference(WasmGCResources.class,
                "create", String.class, int.class, int.class, WasmGCResources.Resource.class));

        WasmGCGenerationUtil.allocateArray(
                genContext.classInfoProvider(),
                ValueType.parse(WasmGCResources.Resource.class),
                builder,
                (array, b) -> {
                    for (var descriptor : descriptors) {
                        var name = genContext.strings().getStringConstant(descriptor.name);
                        b.getGlobal(name.global);
                        b.getGlobal(baseGlobal).i32Const(descriptor.address)
                                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD);
                        b.i32Const(descriptor.end - descriptor.address);
                        b.call(constructor);
                    }
                    b.arrayNewFixed(array, descriptors.size());
                }
        );
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
