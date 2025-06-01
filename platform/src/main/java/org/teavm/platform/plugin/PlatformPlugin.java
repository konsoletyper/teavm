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

import java.util.ArrayList;
import java.util.List;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.c.TeaVMCHost;
import org.teavm.backend.c.intrinsic.Intrinsic;
import org.teavm.backend.c.intrinsic.IntrinsicContext;
import org.teavm.backend.javascript.TeaVMJavaScriptHost;
import org.teavm.backend.wasm.TeaVMWasmHost;
import org.teavm.backend.wasm.gc.TeaVMWasmGCHost;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsicManager;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsic;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.interop.Async;
import org.teavm.interop.PlatformMarker;
import org.teavm.model.ClassReader;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformQueue;
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.ResourceArray;
import org.teavm.platform.metadata.ResourceMap;
import org.teavm.platform.plugin.wasmgc.ResourceCustomTypeMapper;
import org.teavm.platform.plugin.wasmgc.ResourceDependencySupport;
import org.teavm.platform.plugin.wasmgc.ResourceInterfaceToClassTransformer;
import org.teavm.platform.plugin.wasmgc.ResourceMapEntry;
import org.teavm.platform.plugin.wasmgc.ResourceMapHelper;
import org.teavm.platform.plugin.wasmgc.WasmGCResourceArrayIntrinsic;
import org.teavm.platform.plugin.wasmgc.WasmGCResourceMapHelperIntrinsic;
import org.teavm.platform.plugin.wasmgc.WasmGCResourceMetadataIntrinsicFactory;
import org.teavm.platform.plugin.wasmgc.WasmGCResourceMethodIntrinsicFactory;
import org.teavm.vm.TeaVMPluginUtil;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

public class PlatformPlugin implements TeaVMPlugin, MetadataRegistration {
    private MetadataProviderTransformer metadataTransformer = new MetadataProviderTransformer();
    private List<MetadataGeneratorConsumer> metadataGeneratorConsumers = new ArrayList<>();

    @Override
    public void install(TeaVMHost host) {
        if (host.getExtension(TeaVMJavaScriptHost.class) != null) {
            host.add(metadataTransformer);
            host.add(new ResourceTransformer());
            host.add(new ResourceAccessorTransformer(host));
            host.add(new ResourceAccessorDependencyListener());
            TeaVMJavaScriptHost jsHost = host.getExtension(TeaVMJavaScriptHost.class);
            jsHost.addGeneratorProvider(context -> {
                ClassReader cls = context.getClassSource().get(context.getMethod().getClassName());
                if (cls == null) {
                    return null;
                }
                MethodReader method = cls.getMethod(context.getMethod().getDescriptor());
                if (method == null) {
                    return null;
                }
                return method.getAnnotations().get(Async.class.getName()) != null
                        ? new AsyncMethodGenerator() : null;
            });
            host.add(new AsyncDependencyListener());
            jsHost.addVirtualMethods(new AsyncMethodGenerator());

            metadataGeneratorConsumers.add((method, constructor, generator) -> jsHost.add(method,
                    new MetadataProviderNativeGenerator(generator, constructor)));
            host.add(new StringAmplifierTransformer());
        }

        if (!isBootstrap()) {
            var wasmHost = host.getExtension(TeaVMWasmHost.class);
            if (wasmHost != null) {
                installWasm(host, wasmHost);
            }

            var cHost = host.getExtension(TeaVMCHost.class);
            if (cHost != null) {
                installC(host, cHost);
            }

            var wasmGCHost = host.getExtension(TeaVMWasmGCHost.class);
            if (wasmGCHost != null) {
                installWasmGC(host, wasmGCHost);
            }
        }

        var isJs = host.getExtension(TeaVMJavaScriptHost.class) != null;
        if (!isBootstrap()) {
            host.add(new AsyncMethodProcessor(!isJs));
            if (isJs) {
                host.add(new NewInstanceDependencySupport());
            }
            host.add(new ClassLookupDependencySupport());
            host.add(new EnumDependencySupport());
            host.add(new PlatformDependencyListener());

            TeaVMPluginUtil.handleNatives(host, Platform.class);
            TeaVMPluginUtil.handleNatives(host, PlatformQueue.class);
        }

        host.registerService(MetadataRegistration.class, this);
    }

    private void installWasm(TeaVMHost host, TeaVMWasmHost wasmHost) {
        host.add(metadataTransformer);
        host.add(new StringAmplifierTransformer());
        host.add(new ResourceLowLevelTransformer());
        metadataGeneratorConsumers.add((constructor, method, generator) -> {
            wasmHost.add(ctx -> new MetadataIntrinsic(ctx.getClassSource(), ctx.getClassLoader(),
                    ctx.getServices(), ctx.getProperties(), constructor, method, generator));
        });
        wasmHost.add(ctx -> new ResourceReadIntrinsic(ctx.getClassSource(), ctx.getClassLoader()));

        wasmHost.add(ctx -> new WasmIntrinsic() {
            @Override
            public boolean isApplicable(MethodReference methodReference) {
                return methodReference.getClassName().equals(StringAmplifier.class.getName());
            }

            @Override
            public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
                return manager.generate(invocation.getArguments().get(0));
            }
        });
    }

    private void installC(TeaVMHost host, TeaVMCHost cHost) {
        host.add(metadataTransformer);
        host.add(new StringAmplifierTransformer());
        host.add(new ResourceLowLevelTransformer());
        MetadataCIntrinsic metadataCIntrinsic = new MetadataCIntrinsic();
        cHost.addGenerator(ctx -> {
            metadataCIntrinsic.init(ctx.getClassSource(), ctx.getClassLoader(),
                    ctx.getServices(), ctx.getProperties());
            return metadataCIntrinsic;
        });
        metadataGeneratorConsumers.add(metadataCIntrinsic::addGenerator);
        cHost.addIntrinsic(ctx -> new ResourceReadCIntrinsic(ctx.getClassSource()));
        cHost.addIntrinsic(ctx -> new Intrinsic() {
            @Override
            public boolean canHandle(MethodReference method) {
                return method.getClassName().equals(StringAmplifier.class.getName());
            }

            @Override
            public void apply(IntrinsicContext context, InvocationExpr invocation) {
                context.emit(invocation.getArguments().get(0));
            }
        });
    }

    private void installWasmGC(TeaVMHost host, TeaVMWasmGCHost wasmGCHost) {
        WasmGCIntrinsic amplifierIntrinsic =
                (invocation, context) -> context.generate(invocation.getArguments().get(0));
        wasmGCHost.addIntrinsic(new MethodReference(StringAmplifier.class, "amplify", String.class, String.class),
                amplifierIntrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(StringAmplifier.class, "amplifyArray",
                        String[].class, String[].class), amplifierIntrinsic);

        host.add(new ResourceInterfaceToClassTransformer());
        var dependencySupport = new ResourceDependencySupport();
        host.add(dependencySupport);
        wasmGCHost.addCustomTypeMapperFactory(context -> new ResourceCustomTypeMapper(
                context.module(),
                context.typeMapper(),
                context.classes(),
                context.names()
        ));
        wasmGCHost.addIntrinsicFactory(new WasmGCResourceMethodIntrinsicFactory());
        var metadataIntrinsicFactory = new WasmGCResourceMetadataIntrinsicFactory(host.getProperties(), host);
        wasmGCHost.addIntrinsicFactory(metadataIntrinsicFactory);

        var arrayIntrinsic = new WasmGCResourceArrayIntrinsic();
        wasmGCHost.addIntrinsic(new MethodReference(ResourceArray.class, "size", int.class), arrayIntrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(ResourceArray.class, "get", int.class, Resource.class),
                arrayIntrinsic);
        var mapHelperIntrinsic = new WasmGCResourceMapHelperIntrinsic();
        wasmGCHost.addIntrinsic(new MethodReference(ResourceMapHelper.class, "entryCount", ResourceMap.class,
                int.class), mapHelperIntrinsic);
        wasmGCHost.addIntrinsic(new MethodReference(ResourceMapHelper.class, "entry", ResourceMap.class, int.class,
                ResourceMapEntry.class), mapHelperIntrinsic);

        metadataGeneratorConsumers.add((constructor, target, generator) -> {
            dependencySupport.addMetadataMethod(target);
            metadataIntrinsicFactory.addGenerator(target, generator);
        });
    }

    @Override
    public void register(MethodReference method, MetadataGenerator generator) {
        MethodReference constructor = new MethodReference(method.getClassName(), method.getName() + "$$create",
                method.getSignature());
        for (MetadataGeneratorConsumer consumer : metadataGeneratorConsumers) {
            consumer.consume(constructor, method, generator);
        }
        metadataTransformer.addMetadataMethod(method);
    }

    interface MetadataGeneratorConsumer {
        void consume(MethodReference constructor, MethodReference target, MetadataGenerator generator);
    }

    @PlatformMarker
    private static boolean isBootstrap() {
        return false;
    }
}
