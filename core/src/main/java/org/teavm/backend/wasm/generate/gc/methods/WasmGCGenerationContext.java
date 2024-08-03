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

import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.common.methods.BaseWasmGenerationContext;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCStandardClasses;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCSupertypeFunctionProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCTypeMapper;
import org.teavm.backend.wasm.generate.gc.strings.WasmGCStringProvider;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmTag;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.runtime.WasmGCSupport;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.classes.VirtualTableProvider;

public class WasmGCGenerationContext implements BaseWasmGenerationContext {
    private WasmModule module;
    private WasmGCClassInfoProvider classInfoProvider;
    private WasmGCStandardClasses standardClasses;
    private WasmGCStringProvider strings;
    private VirtualTableProvider virtualTables;
    private WasmGCTypeMapper typeMapper;
    private WasmFunctionTypes functionTypes;
    private ClassReaderSource classes;
    private ClassHierarchy hierarchy;
    private BaseWasmFunctionRepository functions;
    private WasmGCSupertypeFunctionProvider supertypeFunctions;
    private WasmGCCustomGeneratorProvider customGenerators;
    private WasmGCIntrinsicProvider intrinsics;
    private WasmFunction npeMethod;
    private WasmFunction aaiobeMethod;
    private WasmFunction cceMethod;
    private WasmGlobal exceptionGlobal;
    private WasmTag exceptionTag;

    public WasmGCGenerationContext(WasmModule module, VirtualTableProvider virtualTables,
            WasmGCTypeMapper typeMapper, WasmFunctionTypes functionTypes, ClassReaderSource classes,
            ClassHierarchy hierarchy, BaseWasmFunctionRepository functions,
            WasmGCSupertypeFunctionProvider supertypeFunctions, WasmGCClassInfoProvider classInfoProvider,
            WasmGCStandardClasses standardClasses, WasmGCStringProvider strings,
            WasmGCCustomGeneratorProvider customGenerators, WasmGCIntrinsicProvider intrinsics) {
        this.module = module;
        this.virtualTables = virtualTables;
        this.typeMapper = typeMapper;
        this.functionTypes = functionTypes;
        this.classes = classes;
        this.hierarchy = hierarchy;
        this.functions = functions;
        this.supertypeFunctions = supertypeFunctions;
        this.classInfoProvider = classInfoProvider;
        this.standardClasses = standardClasses;
        this.strings = strings;
        this.customGenerators = customGenerators;
        this.intrinsics = intrinsics;
    }

    public WasmGCClassInfoProvider classInfoProvider() {
        return classInfoProvider;
    }

    public WasmGCStandardClasses standardClasses() {
        return standardClasses;
    }

    public WasmGCStringProvider strings() {
        return strings;
    }

    public VirtualTableProvider virtualTables() {
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
            exceptionTag = new WasmTag(functionTypes.of(null));
            exceptionTag.setExportName("javaException");
            module.tags.add(exceptionTag);
        }
        return exceptionTag;
    }

    @Override
    public ClassReaderSource classes() {
        return classes;
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
            aaiobeMethod = functions().forStaticMethod(new MethodReference(WasmGCSupport.class, "aaiobe",
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

    public WasmGlobal exceptionGlobal() {
        if (exceptionGlobal == null) {
            var type = classInfoProvider.getClassInfo("java.lang.Throwable").getType();
            exceptionGlobal = new WasmGlobal("teavm_thrown_exception", type, new WasmNullConstant(type));
            module.globals.add(exceptionGlobal);
        }
        return exceptionGlobal;
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
}
