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
package org.teavm.backend.wasm.generate.gc.methods;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.gc.vtable.WasmGCVirtualTableProvider;
import org.teavm.backend.wasm.generate.common.methods.BaseWasmGenerationContext;
import org.teavm.backend.wasm.generate.gc.WasmGCInitializerContributor;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCStandardClasses;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCSupertypeFunctionProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCTypeMapper;
import org.teavm.backend.wasm.generate.gc.strings.WasmGCStringProvider;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmTag;
import org.teavm.backend.wasm.runtime.gc.WasmGCSupport;
import org.teavm.dependency.DependencyInfo;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.analysis.ClassInitializerInfo;
import org.teavm.parsing.resource.ResourceProvider;

public class WasmGCGenerationContext implements BaseWasmGenerationContext {
    private WasmModule module;
    private WasmGCClassInfoProvider classInfoProvider;
    private WasmGCStandardClasses standardClasses;
    private WasmGCStringProvider strings;
    private WasmGCVirtualTableProvider virtualTables;
    private WasmGCTypeMapper typeMapper;
    private WasmFunctionTypes functionTypes;
    private ListableClassReaderSource classes;
    private ResourceProvider resources;
    private ClassLoader classLoader;
    private ClassHierarchy hierarchy;
    private BaseWasmFunctionRepository functions;
    private WasmGCSupertypeFunctionProvider supertypeFunctions;
    private WasmGCCustomGeneratorProvider customGenerators;
    private WasmGCIntrinsicProvider intrinsics;
    private WasmFunction npeMethod;
    private WasmFunction aaiobeMethod;
    private WasmFunction cceMethod;
    private WasmTag exceptionTag;
    private Map<String, Set<String>> interfaceImplementors;
    private WasmGCNameProvider names;
    private boolean strict;
    private String entryPoint;
    private Consumer<WasmGCInitializerContributor> initializerContributors;
    private Diagnostics diagnostics;
    private ClassInitializerInfo classInitInfo;
    private DependencyInfo dependency;

    public WasmGCGenerationContext(WasmModule module, WasmGCVirtualTableProvider virtualTables,
            WasmGCTypeMapper typeMapper, WasmFunctionTypes functionTypes, ListableClassReaderSource classes,
            ResourceProvider resources, ClassLoader classLoader, ClassHierarchy hierarchy,
            BaseWasmFunctionRepository functions, WasmGCSupertypeFunctionProvider supertypeFunctions,
            WasmGCClassInfoProvider classInfoProvider, WasmGCStandardClasses standardClasses,
            WasmGCStringProvider strings, WasmGCCustomGeneratorProvider customGenerators,
            WasmGCIntrinsicProvider intrinsics, WasmGCNameProvider names, boolean strict, String entryPoint,
            Consumer<WasmGCInitializerContributor> initializerContributors,
            Diagnostics diagnostics, ClassInitializerInfo classInitInfo, DependencyInfo dependency) {
        this.module = module;
        this.virtualTables = virtualTables;
        this.typeMapper = typeMapper;
        this.functionTypes = functionTypes;
        this.classes = classes;
        this.resources = resources;
        this.classLoader = classLoader;
        this.hierarchy = hierarchy;
        this.functions = functions;
        this.supertypeFunctions = supertypeFunctions;
        this.classInfoProvider = classInfoProvider;
        this.standardClasses = standardClasses;
        this.strings = strings;
        this.customGenerators = customGenerators;
        this.intrinsics = intrinsics;
        this.names = names;
        this.strict = strict;
        this.entryPoint = entryPoint;
        this.initializerContributors = initializerContributors;
        this.diagnostics = diagnostics;
        this.classInitInfo = classInitInfo;
        this.dependency = dependency;
    }

    public WasmGCClassInfoProvider classInfoProvider() {
        return classInfoProvider;
    }

    public WasmGCNameProvider names() {
        return names;
    }

    public WasmGCStandardClasses standardClasses() {
        return standardClasses;
    }

    public WasmGCStringProvider strings() {
        return strings;
    }

    public String entryPoint() {
        return entryPoint;
    }

    public WasmGCVirtualTableProvider virtualTables() {
        return virtualTables;
    }

    public WasmGCTypeMapper typeMapper() {
        return typeMapper;
    }

    @Override
    public BaseWasmFunctionRepository functions() {
        return functions;
    }

    public WasmGCSupertypeFunctionProvider supertypeFunctions() {
        return supertypeFunctions;
    }

    @Override
    public WasmFunctionTypes functionTypes() {
        return functionTypes;
    }

    @Override
    public WasmTag getExceptionTag() {
        if (exceptionTag == null) {
            exceptionTag = new WasmTag(functionTypes.of(null,
                    classInfoProvider.getClassInfo("java.lang.Throwable").getStructure().getReference()));
            exceptionTag.setExportName("teavm.javaException");
            module.tags.add(exceptionTag);
        }
        return exceptionTag;
    }

    @Override
    public ClassReaderSource classes() {
        return classes;
    }

    public boolean isStrict() {
        return strict;
    }

    public ClassLoader classLoader() {
        return classLoader;
    }

    @Override
    public ResourceProvider resources() {
        return resources;
    }

    public ClassHierarchy hierarchy() {
        return hierarchy;
    }

    public WasmFunction npeMethod() {
        if (npeMethod == null) {
            npeMethod = functions().forStaticMethod(new MethodReference(WasmGCSupport.class, "npe",
                    NullPointerException.class));
        }
        return npeMethod;
    }

    public WasmFunction aaiobeMethod() {
        if (aaiobeMethod == null) {
            aaiobeMethod = functions().forStaticMethod(new MethodReference(WasmGCSupport.class, "aiiobe",
                    ArrayIndexOutOfBoundsException.class));
        }
        return aaiobeMethod;
    }

    public WasmFunction cceMethod() {
        if (cceMethod == null) {
            cceMethod = functions().forStaticMethod(new MethodReference(WasmGCSupport.class, "cce",
                    ClassCastException.class));
        }
        return cceMethod;
    }

    public WasmModule module() {
        return module;
    }

    public WasmGCCustomGeneratorProvider customGenerators() {
        return customGenerators;
    }

    public WasmGCIntrinsicProvider intrinsics() {
        return intrinsics;
    }

    public Diagnostics diagnostics() {
        return diagnostics;
    }

    public ClassInitializerInfo classInitInfo() {
        return classInitInfo;
    }

    public DependencyInfo dependency() {
        return dependency;
    }

    public Collection<String> getInterfaceImplementors(String className) {
        if (interfaceImplementors == null) {
            fillInterfaceImplementors();
        }
        var result = interfaceImplementors.get(className);
        return result != null ? result : List.of();
    }

    private void fillInterfaceImplementors() {
        interfaceImplementors = new HashMap<>();
        for (var className : classes.getClassNames()) {
            var cls = classes.get(className);
            if (!cls.hasModifier(ElementModifier.INTERFACE)) {
                for (var itf : cls.getInterfaces()) {
                    addInterfaceImplementor(className, itf);
                }
            }
        }
    }

    private void addInterfaceImplementor(String implementorName, String interfaceName) {
        var implementorsByKey = interfaceImplementors.computeIfAbsent(interfaceName, k -> new LinkedHashSet<>());
        if (implementorsByKey.add(implementorName)) {
            var itf = classes.get(implementorName);
            if (itf != null) {
                for (var parentItf : itf.getInterfaces()) {
                    addInterfaceImplementor(implementorName, parentItf);
                }
            }
        }
    }

    public void addToInitializer(Consumer<WasmFunction> initializer) {
        initializerContributors.accept(new WasmGCInitializerContributor() {
            @Override
            public void contributeToInitializerDefinitions(WasmFunction function) {
            }

            @Override
            public void contributeToInitializer(WasmFunction function) {
                initializer.accept(function);
            }
        });
    }
}
