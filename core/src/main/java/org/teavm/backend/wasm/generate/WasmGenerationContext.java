/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.backend.wasm.generate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.backend.lowlevel.generate.NameProvider;
import org.teavm.backend.wasm.WasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.common.methods.BaseWasmGenerationContext;
import org.teavm.backend.wasm.generators.WasmMethodGenerator;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsic;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmTag;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.interop.Import;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.classes.TagRegistry;
import org.teavm.model.classes.VirtualTableProvider;
import org.teavm.model.lowlevel.CallSiteDescriptor;
import org.teavm.model.lowlevel.Characteristics;
import org.teavm.parsing.resource.ResourceProvider;

public class WasmGenerationContext implements BaseWasmGenerationContext {
    private ClassReaderSource classSource;
    private ClassHierarchy classHierarchy;
    private ResourceProvider resources;
    public final WasmModule module;
    private final WasmFunctionTypes functionTypes;
    private final WasmFunctionRepository functions;
    private Diagnostics diagnostics;
    private VirtualTableProvider vtableProvider;
    private TagRegistry tagRegistry;
    private WasmStringPool stringPool;
    public final NameProvider names;
    public final Characteristics characteristics;
    private Map<MethodReference, ImportedMethod> importedMethods = new HashMap<>();
    private List<WasmIntrinsic> intrinsics = new ArrayList<>();
    private List<WasmMethodGenerator> generators = new ArrayList<>();
    private Map<MethodReference, IntrinsicHolder> intrinsicCache = new HashMap<>();
    private Map<MethodReference, GeneratorHolder> generatorCache = new HashMap<>();
    private WasmTag exceptionTag;
    private final List<CallSiteDescriptor> callSites = new ArrayList<>();

    public WasmGenerationContext(ClassReaderSource classSource, ClassHierarchy classHierarchy,
            ResourceProvider resources, WasmModule module, WasmFunctionTypes functionTypes,
            WasmFunctionRepository functions, Diagnostics diagnostics, VirtualTableProvider vtableProvider,
            TagRegistry tagRegistry, WasmStringPool stringPool, NameProvider names, Characteristics characteristics,
            WasmTag exceptionTag) {
        this.classSource = classSource;
        this.classHierarchy = classHierarchy;
        this.resources = resources;
        this.module = module;
        this.functionTypes = functionTypes;
        this.functions = functions;
        this.diagnostics = diagnostics;
        this.vtableProvider = vtableProvider;
        this.tagRegistry = tagRegistry;
        this.stringPool = stringPool;
        this.names = names;
        this.characteristics = characteristics;
        this.exceptionTag = exceptionTag;
    }

    @Override
    public WasmFunctionRepository functions() {
        return functions;
    }

    @Override
    public WasmFunctionTypes functionTypes() {
        return functionTypes;
    }

    @Override
    public ClassReaderSource classes() {
        return classSource;
    }

    @Override
    public ResourceProvider resources() {
        return resources;
    }

    public ClassHierarchy getClassHierarchy() {
        return classHierarchy;
    }

    public List<CallSiteDescriptor> callSites() {
        return callSites;
    }

    public void addIntrinsic(WasmIntrinsic intrinsic) {
        intrinsics.add(intrinsic);
    }

    public void addGenerator(WasmMethodGenerator generator) {
        generators.add(generator);
    }

    public WasmIntrinsic getIntrinsic(MethodReference method) {
        return intrinsicCache.computeIfAbsent(method, key -> new IntrinsicHolder(intrinsics.stream()
                .filter(intrinsic -> intrinsic.isApplicable(key))
                .findFirst().orElse(null)))
                .value;
    }

    public WasmMethodGenerator getGenerator(MethodReference method) {
        return generatorCache.computeIfAbsent(method, key -> new GeneratorHolder(generators.stream()
                .filter(generator -> generator.isApplicable(key))
                .findFirst().orElse(null)))
                .value;
    }

    static class IntrinsicHolder {
        WasmIntrinsic value;

        IntrinsicHolder(WasmIntrinsic value) {
            this.value = value;
        }
    }

    static class GeneratorHolder {
        WasmMethodGenerator value;

        GeneratorHolder(WasmMethodGenerator value) {
            this.value = value;
        }
    }

    public ImportedMethod getImportedMethod(MethodReference reference) {
        return importedMethods.computeIfAbsent(reference, ref -> {
            ClassReader cls = classSource.get(ref.getClassName());
            if (cls == null) {
                return null;
            }
            MethodReader method = cls.getMethod(ref.getDescriptor());
            if (method == null) {
                return null;
            }
            AnnotationReader annotation = method.getAnnotations().get(Import.class.getName());
            if (annotation == null) {
                return null;
            }

            String name = annotation.getValue("name").getString();
            AnnotationValue moduleValue = annotation.getValue("module");
            String module = moduleValue != null ? moduleValue.getString() : null;
            if (module != null && module.isEmpty()) {
                module = null;
            }
            return new ImportedMethod(name, module);
        });
    }

    public ClassReaderSource getClassSource() {
        return classSource;
    }

    public ValueType getFieldType(FieldReference fieldReference) {
        ClassReader cls = classSource.get(fieldReference.getClassName());
        FieldReader field = cls.getField(fieldReference.getFieldName());
        return field.getType();
    }

    public VirtualTableProvider getVirtualTableProvider() {
        return vtableProvider;
    }

    public TagRegistry getTagRegistry() {
        return tagRegistry;
    }

    public WasmStringPool getStringPool() {
        return stringPool;
    }

    public Diagnostics getDiagnostics() {
        return diagnostics;
    }

    @Override
    public WasmTag getExceptionTag() {
        return exceptionTag;
    }

    public static class ImportedMethod {
        public final String name;
        public final String module;

        ImportedMethod(String name, String module) {
            this.name = name;
            this.module = module;
        }
    }
}
