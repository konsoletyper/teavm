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
package org.teavm.backend.wasm.generate.reflection;

import java.util.List;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmField;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmNumType;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.MethodReference;
import org.teavm.runtime.StringInfo;
import org.teavm.runtime.reflect.DerivedClassInfo;
import org.teavm.runtime.reflect.FieldInfo;
import org.teavm.runtime.reflect.FieldReflectionInfo;

public class FieldInfoStruct {
    private final WasmGCNameProvider names;
    private final WasmModule module;
    private final WasmFunctionTypes functionTypes;
    private final DependencyInfo dependencies;
    private final WasmGCClassInfoProvider classInfoProvider;

    private WasmStructure structure;

    private int nameIndex = -1;
    private int modifiersIndex = -1;
    private int typeIndex = -1;
    private int readerIndex = -1;
    private int readerConverterIndex = -1;
    private int writerIndex = -1;
    private int writerConverterIndex = -1;
    private int reflectionIndex = -1;

    // Converter types — shared across all fields of the same kind
    private WasmFunctionType readerConverterType;
    private WasmFunctionType writerConverterType;

    // Per-Wasm-type function types for the per-field getter/setter and inline cast at call sites
    private WasmFunctionType[] numericReaderTypes = new WasmFunctionType[WasmNumType.values().length];
    private WasmFunctionType objectReaderType;
    private WasmFunctionType[] numericWriterTypes = new WasmFunctionType[WasmNumType.values().length];
    private WasmFunctionType objectWriterType;

    private WasmArray array;

    FieldInfoStruct(WasmGCNameProvider names, WasmModule module, WasmFunctionTypes functionTypes,
            DependencyInfo dependencies, WasmGCClassInfoProvider classInfoProvider) {
        this.names = names;
        this.module = module;
        this.functionTypes = functionTypes;
        this.dependencies = dependencies;
        this.classInfoProvider = classInfoProvider;

        var name = names.topLevel(names.suggestForClass(FieldInfo.class.getName()));
        structure = new WasmStructure(name, this::initFields);
        module.types.add(structure);
    }

    private void initFields(List<WasmField> fields) {
        if (dependencies.getMethod(new MethodReference(FieldInfo.class, "name", StringInfo.class)) != null) {
            nameIndex = fields.size();
            var strType = classInfoProvider.getClassInfo("java.lang.String").getType();
            fields.add(new WasmField(strType, "name"));
        }
        if (dependencies.getMethod(new MethodReference(FieldInfo.class, "modifiers", int.class)) != null) {
            modifiersIndex = fields.size();
            fields.add(new WasmField(WasmType.INT32, "modifiers"));
        }
        if (dependencies.getMethod(new MethodReference(FieldInfo.class, "type", DerivedClassInfo.class)) != null) {
            typeIndex = fields.size();
            var infoType = classInfoProvider.reflectionTypes().derivedClassInfo().structure().getReference();
            fields.add(new WasmField(infoType, "type"));
        }
        // reader: untyped funcref covering both generic read() and primitive readAsXxx()
        if (needsAnyReader()) {
            readerIndex = fields.size();
            fields.add(new WasmField(WasmType.FUNC, "reader"));
        }
        // readerConverter: boxes the raw value for the generic read() → Object path
        if (dependencies.getMethod(new MethodReference(FieldInfo.class, "read", Object.class, Object.class)) != null) {
            var objType = classInfoProvider.getClassInfo("java.lang.Object").getType();
            readerConverterType = functionTypes.of(objType, WasmType.FUNC, objType);
            readerConverterIndex = fields.size();
            fields.add(new WasmField(readerConverterType, "readerConverter"));
        }
        // writer: untyped funcref covering both generic write(Object,Object) and primitive write(Object,xxx)
        if (needsAnyWriter()) {
            writerIndex = fields.size();
            fields.add(new WasmField(WasmType.FUNC, "writer"));
        }
        // writerConverter: unboxes the Object value for the generic write(Object,Object) path
        if (dependencies.getMethod(new MethodReference(FieldInfo.class, "write", Object.class, Object.class,
                void.class)) != null) {
            var objType = classInfoProvider.getClassInfo("java.lang.Object").getType();
            writerConverterType = functionTypes.of(null, WasmType.FUNC, objType, objType);
            writerConverterIndex = fields.size();
            fields.add(new WasmField(writerConverterType, "writerConverter"));
        }
        if (dependencies.getMethod(new MethodReference(FieldInfo.class, "reflection",
                FieldReflectionInfo.class)) != null) {
            reflectionIndex = fields.size();
            var reflectionStruct = classInfoProvider.reflectionTypes().fieldReflectionInfo().structure();
            fields.add(new WasmField(reflectionStruct, "reflection"));
        }
    }

    private boolean needsAnyReader() {
        return dependencies.getMethod(new MethodReference(FieldInfo.class, "read", Object.class, Object.class)) != null
                || dependencies.getMethod(new MethodReference(FieldInfo.class,
                        "readAsBoolean", Object.class, boolean.class)) != null
                || dependencies.getMethod(new MethodReference(FieldInfo.class,
                        "readAsByte", Object.class, byte.class)) != null
                || dependencies.getMethod(new MethodReference(FieldInfo.class,
                        "readAsShort", Object.class, short.class)) != null
                || dependencies.getMethod(new MethodReference(FieldInfo.class,
                        "readAsChar", Object.class, char.class)) != null
                || dependencies.getMethod(new MethodReference(FieldInfo.class,
                        "readAsInt", Object.class, int.class)) != null
                || dependencies.getMethod(new MethodReference(FieldInfo.class,
                        "readAsLong", Object.class, long.class)) != null
                || dependencies.getMethod(new MethodReference(FieldInfo.class,
                        "readAsFloat", Object.class, float.class)) != null
                || dependencies.getMethod(new MethodReference(FieldInfo.class,
                        "readAsDouble", Object.class, double.class)) != null;
    }

    private boolean needsAnyWriter() {
        return dependencies.getMethod(new MethodReference(FieldInfo.class, "write", Object.class, Object.class,
                        void.class)) != null
                || dependencies.getMethod(new MethodReference(FieldInfo.class,
                        "write", Object.class, boolean.class, void.class)) != null
                || dependencies.getMethod(new MethodReference(FieldInfo.class,
                        "write", Object.class, byte.class, void.class)) != null
                || dependencies.getMethod(new MethodReference(FieldInfo.class,
                        "write", Object.class, short.class, void.class)) != null
                || dependencies.getMethod(new MethodReference(FieldInfo.class,
                        "write", Object.class, char.class, void.class)) != null
                || dependencies.getMethod(new MethodReference(FieldInfo.class,
                        "write", Object.class, int.class, void.class)) != null
                || dependencies.getMethod(new MethodReference(FieldInfo.class,
                        "write", Object.class, long.class, void.class)) != null
                || dependencies.getMethod(new MethodReference(FieldInfo.class,
                        "write", Object.class, float.class, void.class)) != null
                || dependencies.getMethod(new MethodReference(FieldInfo.class,
                        "write", Object.class, double.class, void.class)) != null;
    }

    public WasmStructure structure() {
        return structure;
    }

    public WasmArray array() {
        if (array == null) {
            var name = names.topLevel(names.suggestForArray(names.suggestForClass(FieldInfo.class.getName())));
            array = new WasmArray(name, structure.getReference().asStorage());
            module.types.add(array);
        }
        return array;
    }

    public int nameIndex() {
        init();
        return nameIndex;
    }

    public int modifiersIndex() {
        init();
        return modifiersIndex;
    }

    public int typeIndex() {
        init();
        return typeIndex;
    }

    public int readerIndex() {
        init();
        return readerIndex;
    }

    public int readerConverterIndex() {
        init();
        return readerConverterIndex;
    }

    public int writerIndex() {
        init();
        return writerIndex;
    }

    public int writerConverterIndex() {
        init();
        return writerConverterIndex;
    }

    public int reflectionIndex() {
        init();
        return reflectionIndex;
    }

    public WasmFunctionType readerConverterType() {
        init();
        return readerConverterType;
    }

    public WasmFunctionType writerConverterType() {
        init();
        return writerConverterType;
    }

    /** Returns the typed function type for per-field raw reader: (Object) -> rawWasmType */
    public WasmFunctionType rawReaderFunctionType(WasmType rawWasmType) {
        var objType = classInfoProvider.getClassInfo("java.lang.Object").getType();
        if (rawWasmType instanceof WasmType.Number numType) {
            var idx = numType.number.ordinal();
            if (numericReaderTypes[idx] == null) {
                numericReaderTypes[idx] = functionTypes.of(rawWasmType, objType);
            }
            return numericReaderTypes[idx];
        }
        if (objectReaderType == null) {
            objectReaderType = functionTypes.of(objType, objType);
        }
        return objectReaderType;
    }

    /** Returns the typed function type for per-field raw writer: (Object, rawWasmType) -> void */
    public WasmFunctionType rawWriterFunctionType(WasmType rawWasmType) {
        var objType = classInfoProvider.getClassInfo("java.lang.Object").getType();
        if (rawWasmType instanceof WasmType.Number numType) {
            var idx = numType.number.ordinal();
            if (numericWriterTypes[idx] == null) {
                numericWriterTypes[idx] = functionTypes.of(null, objType, rawWasmType);
            }
            return numericWriterTypes[idx];
        }
        if (objectWriterType == null) {
            objectWriterType = functionTypes.of(null, objType, objType);
        }
        return objectWriterType;
    }

    private void init() {
        structure.init();
    }
}
