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
package org.teavm.backend.wasm.generate.gc.classes;

import com.carrotsearch.hppc.ObjectByteHashMap;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.gc.vtable.WasmGCVirtualTable;
import org.teavm.backend.wasm.gc.vtable.WasmGCVirtualTableEntry;
import org.teavm.backend.wasm.gc.vtable.WasmGCVirtualTableProvider;
import org.teavm.backend.wasm.generate.WasmClassGenerator;
import org.teavm.backend.wasm.generate.gc.WasmGCInitializerContributor;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.gc.annotations.WasmGCAnnotationsGenerator;
import org.teavm.backend.wasm.generate.gc.methods.WasmGCGenerationUtil;
import org.teavm.backend.wasm.generate.gc.strings.WasmGCStringPool;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmField;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStorageType;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayCopy;
import org.teavm.backend.wasm.model.expression.WasmArrayGet;
import org.teavm.backend.wasm.model.expression.WasmArrayLength;
import org.teavm.backend.wasm.model.expression.WasmArrayNewDefault;
import org.teavm.backend.wasm.model.expression.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmFloat32Constant;
import org.teavm.backend.wasm.model.expression.WasmFloat64Constant;
import org.teavm.backend.wasm.model.expression.WasmFunctionReference;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt64Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmNullBranch;
import org.teavm.backend.wasm.model.expression.WasmNullCondition;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmSignedType;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructNew;
import org.teavm.backend.wasm.model.expression.WasmStructNewDefault;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.backend.wasm.runtime.StringInternPool;
import org.teavm.backend.wasm.runtime.gc.WasmGCSupport;
import org.teavm.dependency.DependencyInfo;
import org.teavm.interop.Structure;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.PrimitiveType;
import org.teavm.model.ValueType;
import org.teavm.model.analysis.ClassInitializerInfo;
import org.teavm.model.analysis.ClassMetadataRequirements;
import org.teavm.model.classes.TagRegistry;
import org.teavm.model.util.ReflectionUtil;

public class WasmGCClassGenerator implements WasmGCClassInfoProvider, WasmGCInitializerContributor {
    private static final MethodDescriptor CLINIT_METHOD_DESC = new MethodDescriptor("<clinit>", ValueType.VOID);
    private static final MethodDescriptor CLONE_METHOD_DESC = new MethodDescriptor("clone",
            ValueType.object("java.lang.Object"));
    private static final MethodDescriptor GET_CLASS_METHOD = new MethodDescriptor("getClass",
            ValueType.parse(Class.class));
    private static final FieldReference FAKE_CLASS_FIELD = new FieldReference(Object.class.getName(), "class");
    private static final FieldReference FAKE_MONITOR_FIELD = new FieldReference(Object.class.getName(), "monitor");
    private static final FieldReference FAKE_VT_FIELD = new FieldReference(Object.class.getName(), "vt");
    private static final ValueType OBJECT_TYPE = ValueType.parse(Object.class);

    private final WasmModule module;
    private ClassReaderSource classSource;
    private ClassReaderSource originalClassSource;
    private ClassHierarchy hierarchy;
    private WasmFunctionTypes functionTypes;
    private TagRegistry tagRegistry;
    private ClassMetadataRequirements metadataRequirements;
    private WasmGCVirtualTableProvider virtualTables;
    private BaseWasmFunctionRepository functionProvider;
    private Map<ValueType, WasmGCClassInfo> classInfoMap = new LinkedHashMap<>();
    private Queue<Runnable> queue = new ArrayDeque<>();
    private ObjectIntMap<FieldReference> fieldIndexes = new ObjectIntHashMap<>();
    private Map<FieldReference, WasmGlobal> staticFieldLocations = new HashMap<>();
    private List<Consumer<WasmFunction>> staticFieldInitializers = new ArrayList<>();
    private ClassInitializerInfo classInitializerInfo;
    private ObjectByteHashMap<String> heapStructures = new ObjectByteHashMap<>();

    public final WasmGCStringPool strings;
    public final WasmGCStandardClasses standardClasses;
    public final WasmGCTypeMapper typeMapper;
    private final WasmGCNameProvider names;
    private List<WasmExpression> initializerFunctionStatements = new ArrayList<>();
    private List<Consumer<WasmFunction>> annotationInitStatements = new ArrayList<>();
    private WasmGCAnnotationsGenerator annotationsGenerator;
    private WasmFunction createPrimitiveClassFunction;
    private WasmFunction getArrayClassFunction;
    private WasmFunction fillArrayClassFunction;
    private WasmFunction fillRegularClassFunction;
    private final WasmGCSupertypeFunctionGenerator supertypeGenerator;
    private final WasmGCNewArrayFunctionGenerator newArrayGenerator;
    private String arrayDataFieldName;
    private WasmGlobal lastRegularClassGlobal;

    private int classTagOffset;
    private int classFlagsOffset = -1;
    private int classVtOffset = -1;
    private int classNameOffset;
    private int classSimpleNameOffset;
    private int classCanonicalNameOffset;
    private int classParentOffset = -1;
    private int classArrayOffset;
    private int classArrayItemOffset;
    private int classNewArrayOffset;
    private int classSupertypeFunctionOffset;
    private int classEnclosingClassOffset;
    private int classDeclaringClassOffset;
    private int classAnnotationsOffset = -1;
    private int classInterfacesOffset = -1;
    private int classFieldsOffset = -1;
    private int classMethodsOffset = -1;
    private int classInstantiatorOffset = -1;
    private int classInitializerOffset = -1;
    private int previousRegularClassOffset = -1;
    private int enumConstantsFunctionOffset = -1;
    private int arrayLengthOffset = -1;
    private int arrayGetOffset = -1;
    private int arrayCopyOffset = -1;
    private int cloneOffset = -1;
    private int servicesOffset = -1;
    private int throwableNativeOffset = -1;
    private WasmStructure arrayVirtualTableStruct;
    private WasmFunction arrayGetObjectFunction;
    private WasmFunction arrayLengthObjectFunction;
    private WasmFunction arrayCopyObjectFunction;
    private WasmFunctionType arrayGetType;
    private WasmFunctionType arrayLengthType;
    private WasmFunctionType arrayCopyType;
    private List<WasmFunction> multiArrayFunctions = new ArrayList<>();
    private List<WasmStructure> nonInitializedStructures = new ArrayList<>();
    private WasmArray objectArrayType;
    private boolean hasLoadServices;
    private WasmGCSystemFunctionGenerator systemFunctions;
    private boolean compactMode;
    private WasmGCReflectionGenerator reflectionGenerator;

    public WasmGCClassGenerator(WasmModule module, ClassReaderSource classSource,
            ClassReaderSource originalClassSource, ClassHierarchy hierarchy, DependencyInfo dependencyInfo,
            WasmFunctionTypes functionTypes, TagRegistry tagRegistry,
            ClassMetadataRequirements metadataRequirements, WasmGCVirtualTableProvider virtualTables,
            BaseWasmFunctionRepository functionProvider, WasmGCNameProvider names,
            ClassInitializerInfo classInitializerInfo,
            List<WasmGCCustomTypeMapperFactory> customTypeMapperFactories) {
        this.module = module;
        this.classSource = classSource;
        this.originalClassSource = originalClassSource;
        this.hierarchy = hierarchy;
        this.functionTypes = functionTypes;
        this.tagRegistry = tagRegistry;
        this.metadataRequirements = metadataRequirements;
        this.virtualTables = virtualTables;
        this.functionProvider = functionProvider;
        this.names = names;
        this.classInitializerInfo = classInitializerInfo;
        standardClasses = new WasmGCStandardClasses(this);
        systemFunctions = new WasmGCSystemFunctionGenerator(this, module, functionTypes, names, virtualTables,
                standardClasses);
        strings = new WasmGCStringPool(standardClasses, module, functionProvider, names, functionTypes,
                dependencyInfo);
        supertypeGenerator = new WasmGCSupertypeFunctionGenerator(module, this, names, tagRegistry, functionTypes,
                queue);
        newArrayGenerator = new WasmGCNewArrayFunctionGenerator(module, functionTypes, this, names, queue);
        typeMapper = new WasmGCTypeMapper(classSource, this, functionTypes);
        var customTypeMapperFactoryContext = customTypeMapperFactoryContext();
        typeMapper.setCustomTypeMappers(customTypeMapperFactories.stream()
                .map(factory -> factory.createTypeMapper(customTypeMapperFactoryContext))
                .collect(Collectors.toList()));

        var loadServicesMethod = dependencyInfo.getMethod(new MethodReference(ServiceLoader.class, "loadServices",
                Class.class, Object[].class));
        if (loadServicesMethod != null && loadServicesMethod.isUsed()) {
            hasLoadServices = true;
        }

        annotationsGenerator = new WasmGCAnnotationsGenerator(classSource, metadataRequirements,
                this, strings, annotationInitStatements);
        reflectionGenerator = new WasmGCReflectionGenerator(module, functionTypes, this, names);
    }

    public void setCompactMode(boolean compactMode) {
        this.compactMode = compactMode;
    }

    private WasmGCCustomTypeMapperFactoryContext customTypeMapperFactoryContext() {
        return new WasmGCCustomTypeMapperFactoryContext() {
            @Override
            public ClassReaderSource originalClasses() {
                return originalClassSource;
            }

            @Override
            public ClassReaderSource classes() {
                return classSource;
            }

            @Override
            public WasmModule module() {
                return module;
            }

            @Override
            public WasmGCClassInfoProvider classInfoProvider() {
                return WasmGCClassGenerator.this;
            }

            @Override
            public WasmGCNameProvider names() {
                return names;
            }

            @Override
            public WasmGCTypeMapper typeMapper() {
                return typeMapper;
            }

            @Override
            public WasmFunctionTypes functionTypes() {
                return functionTypes;
            }
        };
    }

    public WasmGCSupertypeFunctionProvider getSupertypeProvider() {
        return supertypeGenerator;
    }

    public boolean process() {
        if (queue.isEmpty()) {
            return false;
        }
        while (!queue.isEmpty()) {
            var action = queue.remove();
            action.run();
            initStructures();
        }
        return true;
    }

    private void initStructures() {
        if (nonInitializedStructures.isEmpty()) {
            return;
        }
        var copy = List.copyOf(nonInitializedStructures);
        nonInitializedStructures.clear();
        for (var structure : copy) {
            structure.init();
        }
    }

    @Override
    public void contributeToInitializerDefinitions(WasmFunction function) {
    }

    @Override
    public void contributeToInitializer(WasmFunction function) {
        function.getBody().addAll(initializerFunctionStatements);
        initializerFunctionStatements.clear();
        for (var classInfo : classInfoMap.values()) {
            if (classInfo.supertypeFunction != null) {
                function.getBody().add(setClassField(classInfo, classSupertypeFunctionOffset,
                        new WasmFunctionReference(classInfo.supertypeFunction)));
            }
            if (classInfo.initArrayFunction != null) {
                function.getBody().add(setClassField(classInfo, classNewArrayOffset,
                        new WasmFunctionReference(classInfo.initArrayFunction)));
            }
        }
        for (var consumer : staticFieldInitializers) {
            consumer.accept(function);
        }
        if (!annotationInitStatements.isEmpty()) {
            var annotationsFunction = new WasmFunction(functionTypes.of(null));
            annotationsFunction.setName(names.topLevel("teavm@initClassAnnotations"));
            module.functions.add(annotationsFunction);
            function.getBody().add(new WasmCall(annotationsFunction));
            for (var contributor : annotationInitStatements) {
                contributor.accept(annotationsFunction);
            }
        }
    }

    private boolean isHeapStructure(String className) {
        var result = heapStructures.getOrDefault(className, (byte) -1);
        if (result < 0) {
            if (className.equals(Structure.class.getName())) {
                result = 1;
            } else {
                var cls = classSource.get(className);
                result = cls != null && cls.getParent() != null && isHeapStructure(cls.getParent()) ? (byte) 1 : 0;
            }
            heapStructures.put(className, result);
        }
        return result != 0;
    }

    @Override
    public WasmGCClassInfo getClassInfo(ValueType type) {
        var classInfo = classInfoMap.get(type);
        if (classInfo == null) {
            classInfo = new WasmGCClassInfo(type);
            var finalClassInfo = classInfo;
            queue.add(() -> {
                if (finalClassInfo.initializer != null) {
                    finalClassInfo.initializer.accept(initializerFunctionStatements);
                    finalClassInfo.initializer = null;
                }
            });
            classInfoMap.put(type, classInfo);
            WasmGCVirtualTable virtualTable = null;
            if (!(type instanceof ValueType.Primitive)) {
                var name = type instanceof ValueType.Object
                        ? ((ValueType.Object) type).getClassName()
                        : null;
                var isInterface = false;
                var classReader = name != null ? classSource.get(name) : null;
                if (classReader != null && classReader.hasModifier(ElementModifier.INTERFACE)) {
                    isInterface = true;
                    classInfo.structure = standardClasses.objectClass().structure;
                } else {
                    if (type instanceof ValueType.Array) {
                        var itemType = ((ValueType.Array) type).getItemType();
                        if (!(itemType instanceof ValueType.Primitive) && !itemType.equals(OBJECT_TYPE)) {
                            classInfo.structure = getClassInfo(ValueType.arrayOf(OBJECT_TYPE)).structure;
                        }
                    }
                    if (classInfo.structure == null) {
                        if (name != null && isHeapStructure(name)) {
                            classInfo.heapStructure = true;
                        } else {
                            var structName = names.topLevel(names.suggestForType(type));
                            classInfo.structure = new WasmStructure(structName,
                                    fields -> fillFields(finalClassInfo, fields, type));
                            classInfo.structure.setNominal(true);
                            module.types.add(classInfo.structure);
                            nonInitializedStructures.add(classInfo.structure);
                        }
                    }
                }
                if (!classInfo.isHeapStructure()) {
                    if (name != null) {
                        virtualTable = virtualTables.lookup(name);
                        if (isInterface && virtualTable != null && !virtualTable.isFakeInterfaceRepresentative()) {
                            virtualTable = null;
                        }
                        if (classReader != null && classReader.getParent() != null && !isInterface) {
                            classInfo.structure.setSupertype(getClassInfo(classReader.getParent()).structure);
                        }
                    } else {
                        virtualTable = virtualTables.lookup("java.lang.Object");
                        classInfo.structure.setSupertype(standardClasses.objectClass().structure);
                    }
                }
            }
            var pointerName = names.topLevel(names.suggestForType(type) + "@class");
            var vtName = names.topLevel(names.suggestForType(type) + "@vt");
            if (virtualTable != null) {
                if (type instanceof ValueType.Object) {
                    if (virtualTable.isUsed()) {
                        initRegularVirtualTableStructure(classInfo, ((ValueType.Object) type).getClassName());
                    } else {
                        var usedVt = virtualTable.getFirstUsed();
                        if (usedVt != null) {
                            classInfo.virtualTableStructure = getClassInfo(usedVt.getClassName())
                                    .virtualTableStructure;
                        } else {
                            classInfo.virtualTableStructure = standardClasses.objectClass().virtualTableStructure;
                        }
                    }
                } else {
                    classInfo.virtualTableStructure = getArrayVirtualTableStructure();
                }
            } else {
                classInfo.virtualTableStructure = standardClasses.objectClass().virtualTableStructure;
            }

            var vtStructure = classInfo.virtualTableStructure;
            if (vtStructure != null) {
                var classStructure = standardClasses.classClass().getStructure();
                classInfo.pointer = new WasmGlobal(pointerName, classStructure.getNonNullReference(),
                        new WasmStructNewDefault(classStructure));
                classInfo.pointer.setImmutable(true);
                module.globals.add(classInfo.pointer);
                if (virtualTable != null && virtualTable.isConcrete()) {
                    classInfo.virtualTablePointer = new WasmGlobal(vtName, vtStructure.getNonNullReference(),
                            new WasmStructNewDefault(vtStructure));
                    if (compactMode) {
                        var cls = classInfo;
                        var vt = virtualTable;
                        queue.add(() -> cls.virtualTablePointer.setInitialValue(
                                fillVirtualTableInitializer(vt, cls)));
                    }
                    classInfo.virtualTablePointer.setImmutable(true);
                    module.globals.add(classInfo.virtualTablePointer);
                }

                if (type instanceof ValueType.Primitive) {
                    initPrimitiveClass(classInfo, (ValueType.Primitive) type);
                } else if (type instanceof ValueType.Void) {
                    initVoidClass(classInfo);
                } else if (type instanceof ValueType.Array) {
                    initArrayClass(classInfo, (ValueType.Array) type);
                } else if (type instanceof ValueType.Object) {
                    initRegularClass(classInfo, virtualTable, ((ValueType.Object) type).getClassName());
                }
                var req = metadataRequirements.getInfo(type);
                if (req != null) {
                    if (type instanceof ValueType.Primitive) {
                        classInfo.initArrayFunction = getArrayConstructor(classInfo.getValueType());
                        classInfo.initArrayFunction.setReferenced(true);
                    }
                    if (req.isAssignable() && !(type instanceof ValueType.Array)) {
                        var supertypeFunction = supertypeGenerator.getIsSupertypeFunction(classInfo.getValueType());
                        supertypeFunction.setReferenced(true);
                        classInfo.supertypeFunction = supertypeFunction;
                    }
                }
            }
        }
        return classInfo;
    }

    @Override
    public WasmFunction getArrayConstructor(ValueType type) {
        var arrayInfo = getClassInfo(type);
        var function = arrayInfo.newArrayFunction;
        if (function == null) {
            function = newArrayGenerator.generateNewArrayFunction(type);
            arrayInfo.newArrayFunction = function;
        }
        return function;
    }

    @Override
    public WasmFunction getMultiArrayConstructor(int depth) {
        if (depth >= multiArrayFunctions.size()) {
            multiArrayFunctions.addAll(Collections.nCopies(depth + 1, null));
        }
        var result = newArrayGenerator.generateNewMultiArrayFunction(depth);
        multiArrayFunctions.set(depth, result);
        return result;
    }

    public int getClassTagOffset() {
        standardClasses.classClass().getStructure().init();
        return classTagOffset;
    }

    @Override
    public int getClassArrayItemOffset() {
        standardClasses.classClass().getStructure().init();
        return classArrayItemOffset;
    }

    @Override
    public int getClassFlagsOffset() {
        standardClasses.classClass().getStructure().init();
        return classFlagsOffset;
    }

    @Override
    public int getClassVtFieldOffset() {
        standardClasses.classClass().getStructure().init();
        return classVtOffset;
    }

    @Override
    public int getClassSupertypeFunctionOffset() {
        standardClasses.classClass().getStructure().init();
        return classSupertypeFunctionOffset;
    }

    @Override
    public int getClassEnclosingClassOffset() {
        standardClasses.classClass().getStructure().init();
        return classEnclosingClassOffset;
    }

    @Override
    public int getClassDeclaringClassOffset() {
        standardClasses.classClass().getStructure().init();
        return classDeclaringClassOffset;
    }

    @Override
    public int getClassParentOffset() {
        standardClasses.classClass().getStructure().init();
        return classParentOffset;
    }

    @Override
    public int getClassNameOffset() {
        standardClasses.classClass().getStructure().init();
        return classNameOffset;
    }

    @Override
    public int getClassSimpleNameOffset() {
        standardClasses.classClass().getStructure().init();
        return classSimpleNameOffset;
    }

    @Override
    public int getClassCanonicalNameOffset() {
        standardClasses.classClass().getStructure().init();
        return classCanonicalNameOffset;
    }

    @Override
    public int getClassAnnotationsOffset() {
        return classAnnotationsOffset;
    }

    @Override
    public int getClassInterfacesOffset() {
        return classInterfacesOffset;
    }

    @Override
    public int getClassFieldsOffset() {
        return classFieldsOffset;
    }

    @Override
    public int getClassMethodsOffset() {
        return classMethodsOffset;
    }

    @Override
    public int getClassInstantiatorOffset() {
        return classInstantiatorOffset;
    }

    @Override
    public int getClassInitializerOffset() {
        return classInitializerOffset;
    }

    @Override
    public int getPreviousRegularClassOffset() {
        return previousRegularClassOffset;
    }

    @Override
    public WasmGlobal getLastRegularClassGlobal() {
        return lastRegularClassGlobal;
    }

    @Override
    public int getNewArrayFunctionOffset() {
        standardClasses.classClass().getStructure().init();
        return classNewArrayOffset;
    }

    @Override
    public int getCloneOffset() {
        standardClasses.classClass().getStructure().init();
        return cloneOffset;
    }

    @Override
    public int getServicesOffset() {
        standardClasses.classClass().getStructure().init();
        return servicesOffset;
    }

    @Override
    public int getThrowableNativeOffset() {
        return throwableNativeOffset;
    }

    private void initPrimitiveClass(WasmGCClassInfo classInfo, ValueType.Primitive type) {
        classInfo.initializer = target -> {
            int kind;
            switch (type.getKind()) {
                case BOOLEAN:
                    kind = WasmGCClassFlags.PRIMITIVE_BOOLEAN;
                    break;
                case BYTE:
                    kind = WasmGCClassFlags.PRIMITIVE_BYTE;
                    break;
                case SHORT:
                    kind = WasmGCClassFlags.PRIMITIVE_SHORT;
                    break;
                case CHARACTER:
                    kind = WasmGCClassFlags.PRIMITIVE_CHAR;
                    break;
                case INTEGER:
                    kind = WasmGCClassFlags.PRIMITIVE_INT;
                    break;
                case LONG:
                    kind = WasmGCClassFlags.PRIMITIVE_LONG;
                    break;
                case FLOAT:
                    kind = WasmGCClassFlags.PRIMITIVE_FLOAT;
                    break;
                case DOUBLE:
                    kind = WasmGCClassFlags.PRIMITIVE_DOUBLE;
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            var req = metadataRequirements.getInfo(type);
            var name = req != null && req.name() ? ReflectionUtil.typeName(type.getKind()) : null;
            target.add(fillPrimitiveClass(
                    classInfo.pointer,
                    name,
                    kind
            ));
        };
    }

    private void initVoidClass(WasmGCClassInfo classInfo) {
        classInfo.initializer = target -> {
            target.add(fillPrimitiveClass(
                    classInfo.pointer,
                    "void",
                    WasmGCClassFlags.PRIMITIVE_VOID
            ));
        };
    }

    private void initRegularClass(WasmGCClassInfo classInfo, WasmGCVirtualTable virtualTable, String name) {
        var cls = classSource.get(name);

        if (classInitializerInfo.isDynamicInitializer(name)) {
            if (cls != null && cls.getMethod(CLINIT_METHOD_DESC) != null) {
                var clinitType = functionTypes.of(null);
                var wasmName = names.topLevel(names.suggestForClass(name) + "@initializer");
                classInfo.initializerPointer = new WasmGlobal(wasmName, clinitType.getReference(),
                        new WasmNullConstant(clinitType.getReference()));
                module.globals.add(classInfo.initializerPointer);
            }
        }
        classInfo.initializer = target -> {
            standardClasses.classClass().getStructure().init();
            var ranges = tagRegistry.getRanges(name);
            int tag = ranges.stream().mapToInt(range -> range.lower).min().orElse(0);
            int flags = cls != null ? getFlags(cls) : 0;
            target.add(new WasmCall(getFillRegularClassFunction(), new WasmGetGlobal(classInfo.pointer),
                    new WasmInt32Constant(tag), new WasmInt32Constant(flags)));
            var metadataReq = metadataRequirements.getInfo(name);
            if (metadataReq.name()) {
                var namePtr = strings.getStringConstant(name).global;
                target.add(setClassField(classInfo, classNameOffset, new WasmGetGlobal(namePtr)));
            }
            if (cls != null) {
                if (metadataReq.simpleName() && cls.getSimpleName() != null) {
                    var namePtr = strings.getStringConstant(cls.getSimpleName()).global;
                    target.add(setClassField(classInfo, classSimpleNameOffset, new WasmGetGlobal(namePtr)));
                }
                if (cls.getParent() != null && metadataReq.superclass()) {
                    var parent = getClassInfo(cls.getParent());
                    target.add(setClassField(classInfo, classParentOffset, new WasmGetGlobal(parent.pointer)));
                }
                if (cls.getOwnerName() != null && metadataReq.enclosingClass()) {
                    var owner = getClassInfo(cls.getOwnerName());
                    target.add(setClassField(classInfo, classEnclosingClassOffset, new WasmGetGlobal(owner.pointer)));
                }
                if (cls.getDeclaringClassName() != null && metadataReq.declaringClass()) {
                    var owner = getClassInfo(cls.getDeclaringClassName());
                    target.add(setClassField(classInfo, classDeclaringClassOffset, new WasmGetGlobal(owner.pointer)));
                }
                if (metadataReq.cloneMethod()) {
                    WasmFunction cloneFunction;
                    if (hierarchy.isSuperType("java.lang.Cloneable", name, false)) {
                        cloneFunction = generateCloneFunction(classInfo, name);
                    } else {
                        cloneFunction = functionProvider.forStaticMethod(new MethodReference(
                                WasmGCSupport.class, "defaultClone", Object.class, Object.class));
                    }
                    cloneFunction.setReferenced(true);
                    target.add(setClassField(classInfo, cloneOffset, new WasmFunctionReference(cloneFunction)));
                }
                if (metadataReq.enumConstants() && cls.hasModifier(ElementModifier.ENUM)) {
                    target.add(setClassField(classInfo, enumConstantsFunctionOffset,
                            new WasmFunctionReference(createEnumConstantsFunction(classInfo, cls))));
                }
                if (metadataReq.interfaces() && !cls.getInterfaces().isEmpty()) {
                    target.add(setClassField(classInfo, classInterfacesOffset, createInterfacesArray(cls)));
                }
            }

            if (classInfo.virtualTablePointer != null) {
                if (compactMode) {
                    assignVTToClass(classInfo, target);
                } else {
                    fillVirtualTableMethods(target, virtualTable, classInfo);
                }
            }
            if (classInfo.initializerPointer != null) {
                var initFunction = functionProvider.forStaticMethod(new MethodReference(name, CLINIT_METHOD_DESC));
                initFunction.setReferenced(true);
                classInfo.initializerPointer.setInitialValue(new WasmFunctionReference(initFunction));
                if (classInitializerOffset >= 0 && metadataReq != null && metadataReq.classInit()) {
                    target.add(setClassField(classInfo, classInitializerOffset,
                            new WasmFunctionReference(initFunction)));
                }
            }
        };
        annotationsGenerator.addClassAnnotations(name, classInfo);
    }

    private WasmExpression createInterfacesArray(ClassReader cls) {
        var result = new WasmArrayNewFixed(getObjectArrayType());
        for (var itf : cls.getInterfaces()) {
            result.getElements().add(new WasmGetGlobal(getClassInfo(itf).getPointer()));
        }
        return result;
    }

    private void assignClassToVT(WasmGCVirtualTable virtualTable, WasmGCClassInfo classInfo,
            List<WasmExpression> target) {
        if (virtualTable != null) {
            while (virtualTable.getParent() != null) {
                virtualTable = virtualTable.getParent();
            }
            if (!virtualTable.getClassName().equals("java.lang.Object")) {
                return;
            }
        }
        var vtStruct = standardClasses.objectClass().getVirtualTableStructure();
        standardClasses.classClass().getStructure().init();
        target.add(new WasmStructSet(vtStruct,
                new WasmGetGlobal(classInfo.virtualTablePointer), WasmGCClassInfoProvider.CLASS_FIELD_OFFSET,
                new WasmGetGlobal(classInfo.pointer)));
        assignVTToClass(classInfo, target);
    }

    private void assignVTToClass(WasmGCClassInfo classInfo, List<WasmExpression> target) {
        target.add(new WasmStructSet(standardClasses.classClass().getStructure(),
                new WasmGetGlobal(classInfo.pointer), getClassVtFieldOffset(),
                new WasmGetGlobal(classInfo.virtualTablePointer)));
    }

    private int getFlags(ClassReader cls) {
        var flags = 0;
        switch (cls.getLevel()) {
            case PUBLIC:
                flags |= WasmGCClassFlags.PUBLIC;
                break;
            case PRIVATE:
                flags |= WasmGCClassFlags.PRIVATE;
                break;
            case PROTECTED:
                flags |= WasmGCClassFlags.PROTECTED;
                break;
            case PACKAGE_PRIVATE:
                break;
        }
        if (cls.hasModifier(ElementModifier.ABSTRACT)) {
            flags |= WasmGCClassFlags.ABSTRACT;
        }
        if (cls.hasModifier(ElementModifier.INTERFACE)) {
            flags |= WasmGCClassFlags.INTERFACE;
        }
        if (cls.hasModifier(ElementModifier.FINAL)) {
            flags |= WasmGCClassFlags.FINAL;
        }
        if (cls.hasModifier(ElementModifier.ANNOTATION)) {
            flags |= WasmGCClassFlags.ANNOTATION;
        }
        if (cls.hasModifier(ElementModifier.SYNTHETIC)) {
            flags |= WasmGCClassFlags.SYNTHETIC;
        }
        if (cls.hasModifier(ElementModifier.ENUM)) {
            flags |= WasmGCClassFlags.ENUM;
        }
        if (cls.hasModifier(ElementModifier.ANNOTATION)) {
            var retention = cls.getAnnotations().get(Retention.class.getName());
            if (retention != null && retention.getValue("value").getEnumValue().getFieldName().equals("RUNTIME")) {
                if (cls.getAnnotations().get(Inherited.class.getName()) != null) {
                    flags |= WasmGCClassFlags.INHERITED_ANNOTATIONS;
                }
            }
        }
        return flags;
    }

    private WasmFunction generateCloneFunction(WasmGCClassInfo classInfo, String className) {
        var function = new WasmFunction(functionTypes.of(standardClasses.objectClass().getType(),
                standardClasses.objectClass().getType()));
        function.setName(names.topLevel(className + "@clone"));
        module.functions.add(function);

        var objLocal = new WasmLocal(standardClasses.objectClass().getType(), "obj");
        var castObjLocal = new WasmLocal(classInfo.getType(), "castObj");
        function.add(objLocal);
        function.add(castObjLocal);

        var cast = new WasmCast(new WasmGetLocal(objLocal), classInfo.getStructure().getReference());
        function.getBody().add(new WasmSetLocal(castObjLocal, cast));

        var copy = new WasmStructNew(classInfo.structure);
        for (var i = 0; i < classInfo.structure.getFields().size(); ++i) {
            if (i == MONITOR_FIELD_OFFSET) {
                copy.getInitializers().add(new WasmNullConstant(WasmType.Reference.EQ));
            } else {
                var fieldType = classInfo.structure.getFields().get(i).getType();
                var getExpr = new WasmStructGet(classInfo.structure, new WasmGetLocal(castObjLocal), i);
                if (fieldType instanceof WasmStorageType.Packed) {
                    getExpr.setSignedType(WasmSignedType.UNSIGNED);
                }
                copy.getInitializers().add(getExpr);
            }
        }

        function.getBody().add(copy);
        return function;
    }

    private void fillVirtualTableMethods(List<WasmExpression> target, WasmGCVirtualTable virtualTable,
            WasmGCClassInfo classInfo) {
        var usedVt = virtualTable.getFirstUsed();
        if (usedVt == null) {
            return;
        }

        var global = classInfo.virtualTablePointer;
        var structure = classInfo.virtualTableStructure;

        var isObject = isObject(usedVt);
        if (isObject) {
            target.add(new WasmCall(systemFunctions.getFillObjectVirtualTableFunction(), new WasmGetGlobal(global),
                    new WasmGetGlobal(classInfo.pointer)));
        }

        for (var i = 0; i < usedVt.getEntries().size(); ++i) {
            var entry = virtualTable.getEntries().get(i);
            var implementor = virtualTable.implementor(entry);
            if (implementor != null) {
                if (isObject && implementor.getClassName().equals("java.lang.Object")) {
                    continue;
                }
                fillVirtualTableEntry(target, global, structure, virtualTable, entry);
            }
        }

        if (!isObject) {
            assignClassToVT(virtualTable, classInfo, target);
        }
    }

    private WasmExpression fillVirtualTableInitializer(WasmGCVirtualTable virtualTable, WasmGCClassInfo classInfo) {
        var usedVt = virtualTable.getFirstUsed();
        if (usedVt == null) {
            return new WasmStructNewDefault(classInfo.structure);
        }

        var entries = new WasmExpression[classInfo.virtualTableStructure.getFields().size()];
        for (var i = 0; i < usedVt.getEntries().size(); ++i) {
            var entry = virtualTable.getEntries().get(i);
            var implementor = virtualTable.implementor(entry);
            if (implementor != null) {
                var function = functionProvider.forInstanceMethod(implementor);
                function.setReferenced(true);
                entries[entry.getIndex() + VIRTUAL_METHOD_OFFSET] = new WasmFunctionReference(function);
            }
        }

        if (classInfo.getValueType() instanceof ValueType.Array) {
            applyArrayVirtualTable(entries, (ValueType.Array) classInfo.getValueType(), classInfo.structure);
        }

        entries[0] = new WasmGetGlobal(classInfo.pointer);
        for (var i = 1; i < classInfo.virtualTableStructure.getFields().size(); ++i) {
            if (entries[i] == null) {
                var functionType = classInfo.virtualTableStructure.getFields().get(i).getUnpackedType();
                entries[i] = new WasmNullConstant((WasmType.Reference) functionType);
            }
        }

        var result = new WasmStructNew(classInfo.virtualTableStructure);
        result.getInitializers().addAll(List.of(entries));
        return result;
    }

    private void applyArrayVirtualTable(WasmExpression[] entries, ValueType.Array type,
            WasmStructure objectStructure) {
        var structure = getArrayVirtualTableStructure();
        structure.init();
        var itemType = type.getItemType();

        var info = metadataRequirements.getInfo(type);
        if (info.arrayLength()) {
            var lengthFunction = getArrayLengthFunction(objectStructure);
            entries[arrayLengthOffset] = new WasmFunctionReference(lengthFunction);
        }
        if (info.arrayGet()) {
            var getFunction = getArrayGetFunction(itemType);
            entries[arrayGetOffset] = new WasmFunctionReference(getFunction);
        }
        if (info.arrayCopy()) {
            var copyFunction = getArrayCopyFunction(itemType);
            entries[arrayCopyOffset] = new WasmFunctionReference(copyFunction);
        }
    }

    private boolean isObject(WasmGCVirtualTable vt) {
        while (vt != null) {
            if (vt.getClassName().equals("java.lang.Object")) {
                return true;
            }
            vt = vt.getParent();
        }
        return false;
    }

    private void fillArrayVirtualTableMethods(ValueType type, List<WasmExpression> target, WasmGlobal global,
            WasmGlobal classGlobal, WasmStructure objectStructure) {
        var structure = getArrayVirtualTableStructure();
        structure.init();
        var itemType = ((ValueType.Array) type).getItemType();

        target.add(new WasmCall(systemFunctions.getFillObjectVirtualTableFunction(), new WasmGetGlobal(global),
                new WasmGetGlobal(classGlobal)));

        var info = metadataRequirements.getInfo(type);
        if (info.arrayLength()) {
            var lengthFunction = getArrayLengthFunction(objectStructure);
            target.add(new WasmStructSet(structure, new WasmGetGlobal(global), arrayLengthOffset,
                    new WasmFunctionReference(lengthFunction)));
        }
        if (info.arrayGet()) {
            var getFunction = getArrayGetFunction(itemType);
            target.add(new WasmStructSet(structure, new WasmGetGlobal(global), arrayGetOffset,
                    new WasmFunctionReference(getFunction)));
        }
        if (info.arrayCopy()) {
            var copyFunction = getArrayCopyFunction(itemType);
            target.add(new WasmStructSet(structure, new WasmGetGlobal(global), arrayCopyOffset,
                    new WasmFunctionReference(copyFunction)));
        }
    }
    private WasmFunction getArrayLengthFunction(WasmStructure objectStructure) {
        var arrayTypeRef = (WasmType.CompositeReference) objectStructure.getFields().get(ARRAY_DATA_FIELD_OFFSET)
                .getUnpackedType();
        var arrayType = (WasmArray) arrayTypeRef.composite;
        var elementType = arrayType.getElementType().asUnpackedType();
        if (elementType instanceof WasmType.Reference) {
            if (arrayLengthObjectFunction == null) {
                arrayLengthObjectFunction = createArrayLengthFunction(objectStructure);
            }
            return arrayLengthObjectFunction;
        }
        return createArrayLengthFunction(objectStructure);
    }

    private WasmFunction createArrayLengthFunction(WasmStructure objectStructure) {
        var function = new WasmFunction(functionTypes.of(WasmType.INT32, standardClasses.classClass().getType(),
                standardClasses.objectClass().getType()));
        function.setReferenced(true);
        function.setName(names.topLevel("Array<*>::length"));
        module.functions.add(function);

        var classLocal = new WasmLocal(standardClasses.classClass().getType(), "this");
        var objectLocal = new WasmLocal(standardClasses.objectClass().getType(), "object");
        function.add(classLocal);
        function.add(objectLocal);

        var castObject = new WasmCast(new WasmGetLocal(objectLocal), objectStructure.getNonNullReference());
        var arrayField = new WasmStructGet(objectStructure, castObject, ARRAY_DATA_FIELD_OFFSET);
        var result = new WasmArrayLength(arrayField);
        function.getBody().add(result);
        return function;
    }

    private WasmFunction getArrayGetFunction(ValueType itemType) {
        if (itemType instanceof ValueType.Primitive) {
            return generateArrayGetPrimitiveFunction(((ValueType.Primitive) itemType).getKind());
        }
        return getArrayGetObjectFunction();
    }

    private WasmFunction getArrayGetObjectFunction() {
        if (arrayGetObjectFunction == null) {
            arrayGetObjectFunction = new WasmFunction(getArrayGetType());
            arrayGetObjectFunction.setName(names.topLevel("Array<" + names.suggestForClass("java.lang.Object")
                    + "::get"));
            module.functions.add(arrayGetObjectFunction);
            arrayGetObjectFunction.setReferenced(true);

            var arrayStruct = getClassInfo(ValueType.arrayOf(OBJECT_TYPE)).structure;
            var arrayDataTypeRef = (WasmType.CompositeReference) arrayStruct.getFields()
                    .get(ARRAY_DATA_FIELD_OFFSET).getUnpackedType();
            var arrayDataType = (WasmArray) arrayDataTypeRef.composite;
            var selfLocal = new WasmLocal(standardClasses.classClass().getType(), "this");
            var objectLocal = new WasmLocal(standardClasses.objectClass().getType(), "object");
            var indexLocal = new WasmLocal(WasmType.INT32, "index");
            arrayGetObjectFunction.add(selfLocal);
            arrayGetObjectFunction.add(objectLocal);
            arrayGetObjectFunction.add(indexLocal);

            var array = new WasmCast(new WasmGetLocal(objectLocal), arrayStruct.getNonNullReference());
            var arrayData = new WasmStructGet(arrayStruct, array, ARRAY_DATA_FIELD_OFFSET);
            var result = new WasmArrayGet(arrayDataType, arrayData, new WasmGetLocal(indexLocal));
            arrayGetObjectFunction.getBody().add(result);
        }
        return arrayGetObjectFunction;
    }

    private WasmFunction generateArrayGetPrimitiveFunction(PrimitiveType type) {
        var function = new WasmFunction(getArrayGetType());
        function.setName(names.topLevel("Array<" + names.suggestForType(ValueType.primitive(type))
                + ">::get"));
        module.functions.add(function);
        function.setReferenced(true);

        var arrayStruct = getClassInfo(ValueType.arrayOf(ValueType.primitive(type))).structure;
        var arrayDataTypeRef = (WasmType.CompositeReference) arrayStruct.getFields()
                .get(ARRAY_DATA_FIELD_OFFSET).getUnpackedType();
        var arrayDataType = (WasmArray) arrayDataTypeRef.composite;
        var classLocal = new WasmLocal(standardClasses.classClass().getType(), "this");
        var objectLocal = new WasmLocal(standardClasses.objectClass().getType(), "object");
        var indexLocal = new WasmLocal(WasmType.INT32, "index");
        function.add(classLocal);
        function.add(objectLocal);
        function.add(indexLocal);

        var array = new WasmCast(new WasmGetLocal(objectLocal), arrayStruct.getNonNullReference());
        var arrayData = new WasmStructGet(arrayStruct, array, ARRAY_DATA_FIELD_OFFSET);
        var result = new WasmArrayGet(arrayDataType, arrayData, new WasmGetLocal(indexLocal));
        Class<?> primitiveType;
        Class<?> wrapperType;
        switch (type) {
            case BOOLEAN:
                primitiveType = boolean.class;
                wrapperType = Boolean.class;
                result.setSignedType(WasmSignedType.UNSIGNED);
                break;
            case BYTE:
                primitiveType = byte.class;
                wrapperType = Byte.class;
                result.setSignedType(WasmSignedType.SIGNED);
                break;
            case SHORT:
                primitiveType = short.class;
                wrapperType = Short.class;
                result.setSignedType(WasmSignedType.SIGNED);
                break;
            case CHARACTER:
                primitiveType = char.class;
                wrapperType = Character.class;
                result.setSignedType(WasmSignedType.UNSIGNED);
                break;
            case INTEGER:
                primitiveType = int.class;
                wrapperType = Integer.class;
                break;
            case LONG:
                primitiveType = long.class;
                wrapperType = Long.class;
                break;
            case FLOAT:
                primitiveType = float.class;
                wrapperType = Float.class;
                break;
            case DOUBLE:
                primitiveType = double.class;
                wrapperType = Double.class;
                break;
            default:
                throw new IllegalArgumentException();
        }
        var method = new MethodReference(wrapperType, "valueOf", primitiveType, wrapperType);
        var wrapFunction = functionProvider.forStaticMethod(method);
        var castResult = new WasmCall(wrapFunction, result);
        function.getBody().add(castResult);

        return function;
    }

    private WasmFunction getArrayCopyFunction(ValueType itemType) {
        if (itemType instanceof ValueType.Primitive) {
            return createArrayCopyFunction(itemType);
        }
        return getArrayCopyObjectFunction();
    }

    private WasmFunction getArrayCopyObjectFunction() {
        if (arrayCopyObjectFunction == null) {
            arrayCopyObjectFunction = createArrayCopyFunction(OBJECT_TYPE);
        }
        return arrayCopyObjectFunction;
    }

    private WasmFunction createArrayCopyFunction(ValueType type) {
        var function = new WasmFunction(getArrayCopyType());
        function.setName(names.topLevel("Array<" + names.suggestForType(type) + ">::copy"));
        module.functions.add(function);
        function.setReferenced(true);

        var arrayStruct = getClassInfo(ValueType.arrayOf(type)).structure;
        var arrayDataTypeRef = (WasmType.CompositeReference) arrayStruct.getFields()
                .get(ARRAY_DATA_FIELD_OFFSET).getUnpackedType();
        var arrayDataType = (WasmArray) arrayDataTypeRef.composite;
        var thisLocal = new WasmLocal(standardClasses.classClass().getType(), "this");
        var sourceLocal = new WasmLocal(standardClasses.objectClass().getType(), "source");
        var sourceIndexLocal = new WasmLocal(WasmType.INT32, "sourceIndex");
        var targetLocal = new WasmLocal(standardClasses.objectClass().getType(), "target");
        var targetIndexLocal = new WasmLocal(WasmType.INT32, "targetIndex");
        var countLocal = new WasmLocal(WasmType.INT32, "count");
        function.add(thisLocal);
        function.add(sourceLocal);
        function.add(sourceIndexLocal);
        function.add(targetLocal);
        function.add(targetIndexLocal);
        function.add(countLocal);

        var sourceArray = new WasmCast(new WasmGetLocal(sourceLocal), arrayStruct.getNonNullReference());
        var sourceArrayData = new WasmStructGet(arrayStruct, sourceArray, ARRAY_DATA_FIELD_OFFSET);
        var targetArray = new WasmCast(new WasmGetLocal(targetLocal), arrayStruct.getNonNullReference());
        var targetArrayData = new WasmStructGet(arrayStruct, targetArray, ARRAY_DATA_FIELD_OFFSET);

        function.getBody().add(new WasmArrayCopy(
                arrayDataType, targetArrayData, new WasmGetLocal(targetIndexLocal),
                arrayDataType, sourceArrayData, new WasmGetLocal(sourceIndexLocal),
                new WasmGetLocal(countLocal)));
        return function;
    }

    private WasmFunctionType getArrayGetType() {
        if (arrayGetType == null) {
            arrayGetType = functionTypes.of(standardClasses.objectClass().getType(),
                    standardClasses.classClass().getType(), standardClasses.objectClass().getType(), WasmType.INT32);
        }
        return arrayGetType;
    }

    private WasmFunctionType getArrayLengthType() {
        if (arrayLengthType == null) {
            arrayLengthType = functionTypes.of(WasmType.INT32, standardClasses.classClass().getType(),
                    standardClasses.objectClass().getType());
        }
        return arrayLengthType;
    }

    private WasmFunctionType getArrayCopyType() {
        if (arrayCopyType == null) {
            arrayCopyType = functionTypes.of(null, standardClasses.classClass().getType(),
                    standardClasses.objectClass().getType(), WasmType.INT32, standardClasses.objectClass().getType(),
                    WasmType.INT32, WasmType.INT32);
        }
        return arrayCopyType;
    }

    private void fillVirtualTableEntry(List<WasmExpression> target, WasmGlobal global,
            WasmStructure structure, WasmGCVirtualTable virtualTable, WasmGCVirtualTableEntry entry) {
        fillVirtualTableEntry(target, () -> new WasmGetGlobal(global), structure, virtualTable, entry);
    }

    void fillVirtualTableEntry(List<WasmExpression> target, Supplier<WasmExpression> vtInstance,
            WasmStructure structure, WasmGCVirtualTable virtualTable, WasmGCVirtualTableEntry entry) {
        var implementor = virtualTable.implementor(entry);
        if (entry.getOrigin() != virtualTable) {
            structure = getClassInfo(entry.getOrigin().getClassName()).virtualTableStructure;
        }
        if (implementor != null && !entry.getMethod().equals(GET_CLASS_METHOD)) {
            var fieldIndex = VIRTUAL_METHOD_OFFSET + entry.getIndex();
            var expectedType = (WasmType.CompositeReference) structure.getFields().get(fieldIndex)
                    .getUnpackedType();
            var expectedFunctionType = (WasmFunctionType) expectedType.composite;
            var function = functionProvider.forInstanceMethod(implementor);
            if (!entry.getOrigin().getClassName().equals(implementor.getClassName())
                    || expectedFunctionType != function.getType()) {
                var wrapperFunction = new WasmFunction(expectedFunctionType);
                wrapperFunction.setName(names.topLevel(names.suggestForMethod(implementor) + "@caller"));
                module.functions.add(wrapperFunction);
                var call = new WasmCall(function);
                var instanceParam = new WasmLocal(getClassInfo(virtualTable.getClassName()).getType());
                wrapperFunction.add(instanceParam);
                var castTarget = getClassInfo(implementor.getClassName()).getStructure().getNonNullReference();
                call.getArguments().add(new WasmCast(new WasmGetLocal(instanceParam), castTarget));
                var params = new WasmLocal[entry.getMethod().parameterCount()];
                for (var i = 0; i < entry.getMethod().parameterCount(); ++i) {
                    params[i] = new WasmLocal(typeMapper.mapType(entry.getMethod().parameterType(i)));
                    call.getArguments().add(new WasmGetLocal(params[i]));
                    wrapperFunction.add(params[i]);
                }
                wrapperFunction.getBody().add(call);
                function = wrapperFunction;
            }
            function.setReferenced(true);
            var ref = new WasmFunctionReference(function);
            target.add(new WasmStructSet(structure, vtInstance.get(), fieldIndex, ref));
        }
    }

    private WasmFunction generateArrayCloneMethod(WasmStructure objectStructure, ValueType itemType) {
        var arrayTypeRef = (WasmType.CompositeReference) objectStructure.getFields().get(
                WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET).getUnpackedType();
        var arrayType = (WasmArray) arrayTypeRef.composite;

        var type = typeMapper.getFunctionType(standardClasses.objectClass().getType(), CLONE_METHOD_DESC);
        var function = new WasmFunction(type);
        function.setName(names.topLevel("Array<" + names.suggestForType(itemType) + ">::clone"));
        module.functions.add(function);
        var instanceLocal = new WasmLocal(standardClasses.objectClass().getType(), "instance");
        var originalLocal = new WasmLocal(objectStructure.getReference(), "original");
        var originalDataLocal = new WasmLocal(arrayType.getNonNullReference(), "originalData");
        var dataCopyLocal = new WasmLocal(arrayType.getNonNullReference(), "resultData");
        function.add(instanceLocal);
        function.add(originalLocal);
        function.add(originalDataLocal);
        function.add(dataCopyLocal);

        var newExpr = new WasmStructNew(objectStructure);
        function.getBody().add(new WasmSetLocal(originalLocal,
                new WasmCast(new WasmGetLocal(instanceLocal), objectStructure.getNonNullReference())));

        var classValue = new WasmStructGet(objectStructure, new WasmGetLocal(originalLocal),
                WasmGCClassInfoProvider.VT_FIELD_OFFSET);
        newExpr.getInitializers().add(classValue);
        newExpr.getInitializers().add(new WasmNullConstant(WasmType.Reference.EQ));

        var originalDataValue = new WasmStructGet(objectStructure, new WasmGetLocal(originalLocal),
                WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
        function.getBody().add(new WasmSetLocal(originalDataLocal, originalDataValue));
        var originalLength = new WasmArrayLength(new WasmGetLocal(originalDataLocal));
        function.getBody().add(new WasmSetLocal(dataCopyLocal, new WasmArrayNewDefault(arrayType, originalLength)));
        newExpr.getInitializers().add(new WasmGetLocal(dataCopyLocal));

        function.getBody().add(new WasmArrayCopy(arrayType, new WasmGetLocal(dataCopyLocal),
                new WasmInt32Constant(0), arrayType, new WasmGetLocal(originalDataLocal),
                new WasmInt32Constant(0), new WasmArrayLength(new WasmGetLocal(originalDataLocal))));

        function.getBody().add(newExpr);
        return function;
    }

    private void initRegularVirtualTableStructure(WasmGCClassInfo classInfo, String className) {
        var virtualTable = virtualTables.lookup(className);
        var wasmName = names.topLevel("VT<" + names.suggestForClass(className) + ">");
        var structure = new WasmStructure(wasmName, fields -> {
            addVirtualTableBaseFields(fields);
            addVirtualTableFields(fields, virtualTable);
        });
        classInfo.virtualTableStructure = structure;
        nonInitializedStructures.add(structure);
        var usedParent = virtualTable.getUsedParent();
        var supertype = usedParent != null
                ? getClassInfo(usedParent.getClassName()).getVirtualTableStructure()
                : null;
        structure.setSupertype(supertype);
        module.types.add(structure);
    }

    private void addVirtualTableBaseFields(List<WasmField> fields) {
        var classField = new WasmField(standardClasses.classClass().getType().asStorage());
        classField.setName(names.forMemberField(FAKE_CLASS_FIELD));
        fields.add(classField);
    }

    private void addSystemFields(List<WasmField> fields) {
        var vtField = new WasmField(standardClasses.objectClass().getVirtualTableStructure().getReference()
                .asStorage());
        vtField.setName(names.forMemberField(FAKE_VT_FIELD));
        fields.add(vtField);
        var monitorField = new WasmField(WasmType.Reference.EQ.asStorage());
        monitorField.setName(names.forMemberField(FAKE_MONITOR_FIELD));
        fields.add(monitorField);
    }

    private void addVirtualTableFields(List<WasmField> fields, WasmGCVirtualTable virtualTable) {
        for (var entry : virtualTable.getEntries()) {
            var receiverType = compactMode
                    ? WasmType.Reference.ANY
                    : getClassInfo(entry.getOrigin().getClassName()).getType();
            var functionType = typeMapper.getFunctionType(receiverType, entry.getMethod());
            var field = new WasmField(functionType.getReference().asStorage());
            field.setName(names.forVirtualMethod(entry.getMethod()));
            fields.add(field);
        }
    }

    @Override
    public WasmStructure getArrayVirtualTableStructure() {
        if (arrayVirtualTableStruct == null) {
            var wasmName = names.topLevel("VT<Array<*>>");
            arrayVirtualTableStruct = new WasmStructure(wasmName, fields -> {
                addVirtualTableBaseFields(fields);
                addVirtualTableFields(fields, virtualTables.lookup("java.lang.Object"));
                if (metadataRequirements.hasArrayLength()) {
                    arrayLengthOffset = fields.size();
                    var arrayLengthType = getArrayLengthType();
                    fields.add(new WasmField(arrayLengthType.getReference().asStorage(),
                            names.structureField("@arrayLength")));
                }
                if (metadataRequirements.hasArrayGet()) {
                    arrayGetOffset = fields.size();
                    var arrayGetType = getArrayGetType();
                    fields.add(new WasmField(arrayGetType.getReference().asStorage(),
                            names.structureField("@arrayGet")));
                }
                if (metadataRequirements.hasArrayCopy()) {
                    arrayCopyOffset = fields.size();
                    var arrayCopyType = getArrayCopyType();
                    fields.add(new WasmField(arrayCopyType.getReference().asStorage(),
                            names.structureField("@arrayCopy")));
                }
            });
            arrayVirtualTableStruct.setSupertype(standardClasses.objectClass().getVirtualTableStructure());
            module.types.add(arrayVirtualTableStruct);
            nonInitializedStructures.add(arrayVirtualTableStruct);
        }
        return arrayVirtualTableStruct;
    }

    @Override
    public int getArrayLengthOffset() {
        initStructures();
        return arrayLengthOffset;
    }

    @Override
    public int getArrayGetOffset() {
        initStructures();
        return arrayGetOffset;
    }

    @Override
    public int getArrayCopyOffset() {
        initStructures();
        return arrayCopyOffset;
    }

    @Override
    public int getEnumConstantsFunctionOffset() {
        return enumConstantsFunctionOffset;
    }

    private void initArrayClass(WasmGCClassInfo classInfo, ValueType.Array type) {
        if (!(type.getItemType() instanceof ValueType.Primitive)) {
            return;
        }
        classInfo.initializer = target -> {
            var itemTypeInfo = getClassInfo(type.getItemType());
            target.add(new WasmCall(
                    getFillArrayClassFunction(),
                    new WasmGetGlobal(classInfo.pointer),
                    new WasmGetGlobal(itemTypeInfo.pointer)
            ));
            if (!compactMode) {
                fillArrayVirtualTableMethods(classInfo.getValueType(), target, classInfo.virtualTablePointer,
                        classInfo.pointer, classInfo.structure);
            } else {
                assignVTToClass(classInfo, target);
            }
            var metadataReq = metadataRequirements.getInfo(type);
            if (metadataReq.cloneMethod()) {
                var cloneFunction = generateArrayCloneMethod(classInfo.structure, type.getItemType());
                cloneFunction.setReferenced(true);
                target.add(setClassField(classInfo, cloneOffset, new WasmFunctionReference(cloneFunction)));
            }
            if (metadataReq.name() && type.getItemType() instanceof ValueType.Primitive) {
                var name = strings.getStringConstant(type.toString());
                target.add(setClassField(classInfo, classNameOffset, new WasmGetGlobal(name.global)));
            }
        };
    }

    private WasmExpression fillPrimitiveClass(WasmGlobal global, String name, int kind) {
        var call = new WasmCall(getCreatePrimitiveClassFunction());
        call.getArguments().add(new WasmGetGlobal(global));
        if (metadataRequirements.hasName()) {
            call.getArguments().add(name != null
                    ? new WasmGetGlobal(strings.getStringConstant(name).global)
                    : new WasmNullConstant(standardClasses.stringClass().getType()));
        }
        call.getArguments().add(new WasmInt32Constant(kind));
        return call;
    }

    @Override
    public int getFieldIndex(FieldReference fieldRef) {
        var classInfo = getClassInfo(fieldRef.getClassName());
        if (classInfo.isHeapStructure()) {
            throw new IllegalArgumentException("Given field belongs to heap structure");
        }
        classInfo.structure.init();
        return fieldIndexes.getOrDefault(fieldRef, -1);
    }

    @Override
    public int getHeapFieldOffset(FieldReference fieldRef) {
        var classInfo = getClassInfo(fieldRef.getClassName());
        if (!classInfo.isHeapStructure()) {
            throw new IllegalArgumentException("Given field does not belong to heap structure");
        }
        if (classInfo.heapFieldOffsets == null) {
            fillHeapFieldOffsets(classInfo);
        }
        return classInfo.heapFieldOffsets.get(fieldRef.getFieldName());
    }

    @Override
    public int getHeapSize(String className) {
        var classInfo = getClassInfo(className);
        if (!classInfo.isHeapStructure()) {
            throw new IllegalArgumentException("Given field does not belong to heap structure");
        }
        if (classInfo.heapFieldOffsets == null) {
            fillHeapFieldOffsets(classInfo);
        }
        return classInfo.heapSize;
    }

    @Override
    public int getHeapAlignment(String className) {
        var classInfo = getClassInfo(className);
        if (!classInfo.isHeapStructure()) {
            throw new IllegalArgumentException("Given field does not belong to heap structure");
        }
        if (classInfo.heapFieldOffsets == null) {
            fillHeapFieldOffsets(classInfo);
        }
        return classInfo.heapAlignment;
    }

    @Override
    public WasmGCReflectionProvider reflection() {
        return reflectionGenerator;
    }

    private void fillHeapFieldOffsets(WasmGCClassInfo classInfo) {
        var offsets = new ObjectIntHashMap<String>();
        classInfo.heapFieldOffsets = offsets;
        var className = ((ValueType.Object) classInfo.getValueType()).getClassName();
        var offset = 0;
        var alignment = 1;
        if (!className.equals(Structure.class.getName())) {
            var cls = classSource.get(className);
            var parentInfo = getClassInfo(cls.getParent());
            if (parentInfo.heapFieldOffsets == null) {
                fillHeapFieldOffsets(parentInfo);
            }
            offset = parentInfo.heapSize;
            for (var field : cls.getFields()) {
                if (field.hasModifier(ElementModifier.STATIC)) {
                    continue;
                }
                var size = WasmClassGenerator.getTypeSize(field.getType());
                if (offset == 0) {
                    alignment = size;
                }
                offset = WasmClassGenerator.align(offset, size);
                offsets.put(field.getName(), offset);
                offset += size;
            }
        }
        classInfo.heapSize = offset;
        classInfo.heapAlignment = alignment;
    }

    @Override
    public WasmGlobal getStaticFieldLocation(FieldReference fieldRef) {
        return staticFieldLocations.computeIfAbsent(fieldRef, this::generateStaticFieldLocation); 
    }
    
    private WasmGlobal generateStaticFieldLocation(FieldReference fieldRef) {
        ValueType javaType = null;
        Object initValue = null;
        var cls = classSource.get(fieldRef.getClassName());
        if (cls != null) {
            var field = cls.getField(fieldRef.getFieldName());
            if (field != null) {
                javaType = field.getType();
                initValue = field.getInitialValue();
            }
        }
        if (javaType == null) {
            javaType = ValueType.object("java.lang.Object");
        }
        
        var type = typeMapper.mapType(javaType);
        var wasmInitialValue = initValue != null ? initialValue(initValue) : WasmExpression.defaultValueOfType(type);
        var wasmName = names.topLevel(names.suggestForStaticField(fieldRef));
        var global = new WasmGlobal(wasmName, type, wasmInitialValue);
        dynamicInitialValue(global, initValue);
        module.globals.add(global);
        
        return global;
    }

    private WasmExpression initialValue(Object value) {
        if (value instanceof Boolean) {
            return new WasmInt32Constant((Boolean) value ? 1 : 0);
        } else if (value instanceof Byte) {
            return new WasmInt32Constant((Byte) value);
        } else if (value instanceof Short) {
            return new WasmInt32Constant((Short) value);
        } else if (value instanceof Character) {
            return new WasmInt32Constant((Character) value);
        } else if (value instanceof Integer) {
            return new WasmInt32Constant((Integer) value);
        } else if (value instanceof Long) {
            return new WasmInt64Constant((Long) value);
        } else if (value instanceof Float) {
            return new WasmFloat32Constant((Float) value);
        } else if (value instanceof Double) {
            return new WasmFloat64Constant((Double) value);
        } else {
            return new WasmNullConstant(standardClasses.stringClass().getType());
        }
    }

    private void dynamicInitialValue(WasmGlobal global, Object value) {
        if (value instanceof String) {
            var constant = strings.getStringConstant((String) value).global;
            staticFieldInitializers.add(function -> {
                function.getBody().add(new WasmSetGlobal(global, new WasmGetGlobal(constant)));
            });
        } else if (value instanceof ValueType) {
            var constant = getClassInfo((ValueType) value).pointer;
            staticFieldInitializers.add(function -> {
                function.getBody().add(new WasmSetGlobal(global, new WasmGetGlobal(constant)));
            });
        }
    }

    private void fillFields(WasmGCClassInfo classInfo, List<WasmField> fields, ValueType type) {
        addSystemFields(fields);
        if (type instanceof ValueType.Object) {
            fillClassFields(fields, ((ValueType.Object) type).getClassName());
        } else if (type instanceof ValueType.Array) {
            fillArrayFields(classInfo, ((ValueType.Array) type).getItemType());
        }
    }

    private void fillClassFields(List<WasmField> fields, String className) {
        var classReader = classSource.get(className);
        if (classReader == null || classReader.hasModifier(ElementModifier.INTERFACE)) {
            fillSimpleClassFields(fields, "java.lang.Object");
        } else {
            fillSimpleClassFields(fields, className);
        }
    }

    private void fillSimpleClassFields(List<WasmField> fields, String className) {
        var classReader = classSource.get(className);
        if (classReader.getParent() != null) {
            fillClassFields(fields, classReader.getParent());
        }
        if (className.equals("java.lang.ref.WeakReference")) {
            var field = new WasmField(WasmType.Reference.EXTERN.asStorage(), "nativeRef");
            fields.add(field);
        } else {
            for (var field : classReader.getFields()) {
                if (className.equals("java.lang.Object") && field.getName().equals("monitor")) {
                    continue;
                }
                if (className.equals("java.lang.Class") && field.getName().equals("platformClass")) {
                    continue;
                }
                if (field.hasModifier(ElementModifier.STATIC)) {
                    continue;
                }
                fieldIndexes.putIfAbsent(field.getReference(), fields.size());
                var wasmField = new WasmField(typeMapper.mapStorageType(field.getType()),
                        names.forMemberField(field.getReference()));
                fields.add(wasmField);
            }
        }
        if (className.equals(StringInternPool.class.getName() + "$Entry")) {
            var field = new WasmField(WasmType.Reference.EXTERN.asStorage(), "nativeRef");
            fields.add(field);
        }
        if (className.equals("java.lang.Throwable")) {
            throwableNativeOffset = fields.size();
            var field = new WasmField(WasmType.Reference.EXTERN.asStorage(), "nativeRef");
            fields.add(field);
        }
        if (className.equals("java.lang.Class")) {
            var cls = classSource.get("java.lang.Class");
            classFlagsOffset = fields.size();
            fields.add(createClassField(WasmType.INT32.asStorage(), "flags"));
            classTagOffset = fields.size();
            fields.add(createClassField(WasmType.INT32.asStorage(), "id"));
            classVtOffset = fields.size();
            fields.add(createClassField(standardClasses.objectClass().getVirtualTableStructure().getReference()
                    .asStorage(), "classVt"));
            if (metadataRequirements.hasSuperclass()) {
                classParentOffset = fields.size();
                fields.add(createClassField(standardClasses.classClass().getType().asStorage(), "parent"));
            }
            classArrayItemOffset = fields.size();
            fields.add(createClassField(standardClasses.classClass().getType().asStorage(), "arrayItem"));
            classArrayOffset = fields.size();
            fields.add(createClassField(standardClasses.classClass().getType().asStorage(), "array"));
            if (metadataRequirements.hasIsAssignable()) {
                classSupertypeFunctionOffset = fields.size();
                fields.add(createClassField(supertypeGenerator.getFunctionType().getReference().asStorage(),
                        "isSupertype"));
            }
            classNewArrayOffset = fields.size();
            fields.add(createClassField(newArrayGenerator.getNewArrayFunctionType().getReference().asStorage(),
                    "createArrayInstance"));
            if (metadataRequirements.hasEnclosingClass()) {
                classEnclosingClassOffset = fields.size();
                fields.add(createClassField(standardClasses.classClass().getType().asStorage(), "enclosingClass"));
            }
            if (metadataRequirements.hasDeclaringClass()) {
                classDeclaringClassOffset = fields.size();
                fields.add(createClassField(standardClasses.classClass().getType().asStorage(), "declaringClass"));
            }
            if (metadataRequirements.hasName()) {
                classNameOffset = fields.size();
                fields.add(createClassField(standardClasses.stringClass().getType().asStorage(), "name"));
            }
            if (metadataRequirements.hasSimpleName()) {
                classSimpleNameOffset = fields.size();
                fields.add(createClassField(standardClasses.stringClass().getType().asStorage(), "simpleName"));
            }
            if (cls != null && cls.getMethod(new MethodDescriptor("getCanonicalName", String.class)) != null) {
                classCanonicalNameOffset = fields.size();
                fields.add(createClassField(standardClasses.stringClass().getType().asStorage(), "canonicalName"));
            }
            cloneOffset = fields.size();
            fields.add(createClassField(functionTypes.of(standardClasses.objectClass().getType(),
                    standardClasses.objectClass().getType()).getReference().asStorage(), "clone"));
            if (hasLoadServices) {
                servicesOffset = fields.size();
                var serviceFunctionType = functionTypes.of(getClassInfo(ValueType.parse(Object[].class)).getType());
                fields.add(createClassField(serviceFunctionType.getReference().asStorage(), "services"));
            }
            if (metadataRequirements.hasEnumConstants()) {
                enumConstantsFunctionOffset = fields.size();
                var enumArrayType = getClassInfo(ValueType.arrayOf(ValueType.object("java.lang.Enum"))).getType();
                var enumConstantsType = functionTypes.of(enumArrayType);
                fields.add(createClassField(enumConstantsType.getReference().asStorage(), "getEnumConstants"));
            }
            if (metadataRequirements.hasGetAnnotations()) {
                classAnnotationsOffset = fields.size();
                var annotationArrayType = getObjectArrayType();
                fields.add(createClassField(annotationArrayType.getReference().asStorage(), "annotations"));
            }
            if (metadataRequirements.hasGetInterfaces()) {
                classInterfacesOffset = fields.size();
                var classArrayType = getObjectArrayType();
                fields.add(createClassField(classArrayType.getReference().asStorage(), "interfaces"));
            }
            if (metadataRequirements.hasGetFields()) {
                classFieldsOffset = fields.size();
                var fieldsType = reflection().getReflectionFieldArrayType();
                fields.add(createClassField(fieldsType.getReference().asStorage(), "fields"));
            }
            if (metadataRequirements.hasGetMethods()) {
                classMethodsOffset = fields.size();
                var methodsType = reflection().getReflectionMethodArrayType();
                fields.add(createClassField(methodsType.getReference().asStorage(), "methods"));
            }
            if (metadataRequirements.hasNewInstance()) {
                classInstantiatorOffset = fields.size();
                var instantiatorType = functionTypes.of(standardClasses.objectClass().getType()).getReference();
                fields.add(createClassField(instantiatorType.asStorage(), "instantiator"));
            }
            if (metadataRequirements.hasClassInit()) {
                classInitializerOffset = fields.size();
                var instantiatorType = functionTypes.of(null).getReference();
                fields.add(createClassField(instantiatorType.asStorage(), "initializer"));
            }
            previousRegularClassOffset = fields.size();
            fields.add(createClassField(standardClasses.classClass().getType().asStorage(), "previous"));

            lastRegularClassGlobal = new WasmGlobal(names.topLevel("teavm.lastRegularClass"),
                    standardClasses.classClass().getType(),
                    new WasmNullConstant(standardClasses.classClass().getType()));
            module.globals.add(lastRegularClassGlobal);
        }
    }

    private WasmField createClassField(WasmStorageType type, String name) {
        return new WasmField(type, names.forMemberField(new FieldReference("java.lang.Class", name)));
    }

    private void fillArrayFields(WasmGCClassInfo classInfo, ValueType elementType) {
        WasmStorageType wasmElementType;
        WasmArray wasmArray;
        if (elementType instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) elementType).getKind()) {
                case BOOLEAN:
                case BYTE:
                    wasmElementType = WasmStorageType.INT8;
                    break;
                case SHORT:
                case CHARACTER:
                    wasmElementType = WasmStorageType.INT16;
                    break;
                case INTEGER:
                    wasmElementType = WasmType.INT32.asStorage();
                    break;
                case LONG:
                    wasmElementType = WasmType.INT64.asStorage();
                    break;
                case FLOAT:
                    wasmElementType = WasmType.FLOAT32.asStorage();
                    break;
                case DOUBLE:
                    wasmElementType = WasmType.FLOAT64.asStorage();
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            var wasmArrayName = names.topLevel(names.suggestForType(classInfo.getValueType()) + "$Data");
            wasmArray = new WasmArray(wasmArrayName, wasmElementType);
            module.types.add(wasmArray);
        } else {
            wasmArray = getObjectArrayType();
        }

        classInfo.structure.getFields().add(new WasmField(wasmArray.getNonNullReference().asStorage(),
                arrayDataFieldName()));
    }

    public WasmArray getObjectArrayType() {
        var wasmArray = objectArrayType;
        if (wasmArray == null) {
            var wasmElementType = standardClasses.objectClass().getType().asStorage();
            var wasmArrayName = names.topLevel(names.suggestForType(ValueType.arrayOf(
                    ValueType.object("java.lang.Object"))) + "$Data");
            wasmArray = new WasmArray(wasmArrayName, wasmElementType);
            module.types.add(wasmArray);
            objectArrayType = wasmArray;
        }
        return wasmArray;
    }

    private String arrayDataFieldName() {
        if (arrayDataFieldName == null) {
            arrayDataFieldName = names.structureField("@data");
        }
        return arrayDataFieldName;
    }

    private WasmFunction getCreatePrimitiveClassFunction() {
        if (createPrimitiveClassFunction == null) {
            createPrimitiveClassFunction = createCreatePrimitiveClassFunction();
        }
        return createPrimitiveClassFunction;
    }

    private WasmFunction createCreatePrimitiveClassFunction() {
        var params = new ArrayList<WasmType>();
        params.add(standardClasses.classClass().getType());
        if (metadataRequirements.hasName()) {
            params.add(standardClasses.stringClass().getType());
        }
        params.add(WasmType.INT32);
        var functionType = functionTypes.of(null, params.toArray(new WasmType[0]));
        var function = new WasmFunction(functionType);
        function.setName(names.topLevel("teavm@fillPrimitiveClass"));
        module.functions.add(function);

        var targetVar = new WasmLocal(standardClasses.classClass().getType(), "target");
        function.add(targetVar);
        WasmLocal nameVar;
        if (metadataRequirements.hasName()) {
            nameVar = new WasmLocal(standardClasses.stringClass().getType(), "name");
            function.add(nameVar);
        } else {
            nameVar = null;
        }
        var kindVar = new WasmLocal(WasmType.INT32, "kind");
        function.add(kindVar);

        standardClasses.classClass().getStructure().init();
        function.getBody().add(new WasmStructSet(
                standardClasses.classClass().getStructure(),
                new WasmGetLocal(targetVar),
                VT_FIELD_OFFSET,
                new WasmGetGlobal(standardClasses.classClass().virtualTablePointer)
        ));
        var flagsExpr = new WasmIntBinary(
                WasmIntType.INT32,
                WasmIntBinaryOperation.SHL,
                new WasmGetLocal(kindVar),
                new WasmInt32Constant(WasmGCClassFlags.PRIMITIVE_KIND_SHIFT)
        );
        flagsExpr = new WasmIntBinary(
                WasmIntType.INT32,
                WasmIntBinaryOperation.OR,
                flagsExpr,
                new WasmInt32Constant(WasmGCClassFlags.FINAL | WasmGCClassFlags.PRIMITIVE)
        );
        function.getBody().add(new WasmStructSet(
                standardClasses.classClass().getStructure(),
                new WasmGetLocal(targetVar),
                classFlagsOffset,
                flagsExpr
        ));
        if (nameVar != null) {
            function.getBody().add(new WasmStructSet(
                    standardClasses.classClass().getStructure(),
                    new WasmGetLocal(targetVar),
                    classNameOffset,
                    new WasmGetLocal(nameVar)
            ));
        }
        function.getBody().add(new WasmStructSet(
                standardClasses.classClass().getStructure(),
                new WasmGetLocal(targetVar),
                classTagOffset,
                new WasmInt32Constant(Integer.MAX_VALUE)
        ));
        return function;
    }

    private WasmFunction getFillRegularClassFunction() {
        if (fillRegularClassFunction == null) {
            fillRegularClassFunction = createFillRegularClassFunction();
        }
        return fillRegularClassFunction;
    }

    private WasmFunction createFillRegularClassFunction() {
        var functionType = functionTypes.of(
                null,
                standardClasses.classClass().getType(),
                WasmType.INT32,
                WasmType.INT32
        );
        var function = new WasmFunction(functionType);
        module.functions.add(function);
        function.setName(names.topLevel("teavm@fillRegularClass"));

        var targetVar = new WasmLocal(standardClasses.classClass().getType(), "target");
        var idVar = new WasmLocal(standardClasses.classClass().getType(), "id");
        var flagsVar = new WasmLocal(standardClasses.classClass().getType(), "flags");
        function.add(targetVar);
        function.add(idVar);
        function.add(flagsVar);
        standardClasses.classClass().getStructure().init();

        function.getBody().add(new WasmStructSet(
                standardClasses.classClass().getStructure(),
                new WasmGetLocal(targetVar),
                VT_FIELD_OFFSET,
                new WasmGetGlobal(standardClasses.classClass().virtualTablePointer)
        ));
        function.getBody().add(new WasmStructSet(
                standardClasses.classClass().getStructure(),
                new WasmGetLocal(targetVar),
                classTagOffset,
                new WasmGetLocal(idVar)
        ));
        function.getBody().add(new WasmStructSet(
                standardClasses.classClass().getStructure(),
                new WasmGetLocal(targetVar),
                classFlagsOffset,
                new WasmGetLocal(flagsVar)
        ));

        var newArrayFunction = newArrayGenerator.getNewObjectArrayFunction();
        newArrayFunction.setReferenced(true);
        function.getBody().add(new WasmStructSet(
                standardClasses.classClass().getStructure(),
                new WasmGetLocal(targetVar),
                classNewArrayOffset,
                new WasmFunctionReference(newArrayFunction)
        ));

        function.getBody().add(new WasmStructSet(
                standardClasses.classClass().getStructure(),
                new WasmGetLocal(targetVar),
                previousRegularClassOffset,
                new WasmGetGlobal(lastRegularClassGlobal)
        ));
        function.getBody().add(new WasmSetGlobal(lastRegularClassGlobal, new WasmGetLocal(targetVar)));

        return function;
    }

    @Override
    public WasmFunction getGetArrayClassFunction() {
        if (getArrayClassFunction == null) {
            getArrayClassFunction = createGetArrayClassFunction();
        }
        return getArrayClassFunction;
    }

    private WasmFunction createGetArrayClassFunction() {
        var classClass = standardClasses.classClass();
        var functionType = functionTypes.of(classClass.getStructure().getNonNullReference(), classClass.getType());
        var function = new WasmFunction(functionType);
        var arrayType = getClassInfo(ValueType.arrayOf(ValueType.object("java.lang.Object")));
        module.functions.add(function);
        function.setName(names.topLevel("teavm@getArrayClass"));
        queue.add(() -> {
            var itemTypeVar = new WasmLocal(classClass.getType(), "itemType");
            var resultVar = new WasmLocal(classClass.getStructure().getNonNullReference(), "result");
            var vtVar = new WasmLocal(getArrayVirtualTableStructure().getNonNullReference(), "vt");
            function.add(itemTypeVar);
            function.add(resultVar);
            function.add(vtVar);

            var body = function.getBody();
            var existing = new WasmStructGet(classClass.structure, new WasmGetLocal(itemTypeVar), classArrayOffset);
            var block = new WasmBlock(false);
            block.getBody().add(new WasmReturn(new WasmNullBranch(WasmNullCondition.NULL, existing, block)));
            body.add(block);

            body.add(new WasmSetLocal(resultVar, new WasmStructNewDefault(classClass.structure)));
            body.add(new WasmStructSet(classClass.structure, new WasmGetLocal(itemTypeVar), classArrayOffset,
                    new WasmGetLocal(resultVar)));
            body.add(new WasmCall(getFillArrayClassFunction(), new WasmGetLocal(resultVar),
                    new WasmGetLocal(itemTypeVar)));

            var supertypeFunction = supertypeGenerator.getIsArraySupertypeFunction();
            supertypeFunction.setReferenced(true);
            body.add(new WasmStructSet(classClass.structure, new WasmGetLocal(resultVar),
                    classSupertypeFunctionOffset, new WasmFunctionReference(supertypeFunction)));

            var cloneFunction = generateArrayCloneMethod(arrayType.structure, ValueType.object("java.lang.Object"));
            cloneFunction.setReferenced(true);
            body.add(new WasmStructSet(classClass.structure, new WasmGetLocal(resultVar), cloneOffset,
                    new WasmFunctionReference(cloneFunction)));

            body.add(new WasmSetLocal(vtVar, new WasmStructNewDefault(getArrayVirtualTableStructure())));
            fillArrayVirtualTableMethods(body, vtVar, resultVar);
            body.add(new WasmGetLocal(resultVar));
        });
        return function;
    }

    private void fillArrayVirtualTableMethods(List<WasmExpression> target, WasmLocal vt, WasmLocal cls) {
        var structure = getArrayVirtualTableStructure();
        structure.init();

        target.add(new WasmCall(systemFunctions.getFillObjectVirtualTableFunction(), new WasmGetLocal(vt),
                new WasmGetLocal(cls)));

        if (arrayLengthOffset >= 0) {
            var arrayClass = getClassInfo(ValueType.arrayOf(ValueType.object("java.lang.Object")));
            var lengthFunction = getArrayLengthFunction(arrayClass.structure);
            target.add(new WasmStructSet(structure, new WasmGetLocal(vt), arrayLengthOffset,
                    new WasmFunctionReference(lengthFunction)));
        }
        if (arrayGetOffset >= 0) {
            var getFunction = getArrayGetObjectFunction();
            target.add(new WasmStructSet(structure, new WasmGetLocal(vt), arrayGetOffset,
                    new WasmFunctionReference(getFunction)));
        }
        if (arrayCopyOffset >= 0) {
            var copyFunction = getArrayCopyObjectFunction();
            target.add(new WasmStructSet(structure, new WasmGetLocal(vt), arrayCopyOffset,
                    new WasmFunctionReference(copyFunction)));
        }
    }

    private WasmFunction getFillArrayClassFunction() {
        if (fillArrayClassFunction == null) {
            fillArrayClassFunction = createFillArrayClassFunction();
        }
        return fillArrayClassFunction;
    }

    private WasmFunction createFillArrayClassFunction() {
        var functionType = functionTypes.of(
                null,
                standardClasses.classClass().getType(),
                standardClasses.classClass().getType()
        );
        var function = new WasmFunction(functionType);
        module.functions.add(function);
        function.setName(names.topLevel("teavm@fillArrayClass"));

        var targetVar = new WasmLocal(standardClasses.classClass().getType(), "target");
        var itemVar = new WasmLocal(standardClasses.classClass().getType(), "item");
        function.add(targetVar);
        function.add(itemVar);
        standardClasses.classClass().getStructure().init();

        function.getBody().add(new WasmStructSet(
                standardClasses.classClass().getStructure(),
                new WasmGetLocal(targetVar),
                VT_FIELD_OFFSET,
                new WasmGetGlobal(standardClasses.classClass().getVirtualTablePointer())
        ));
        function.getBody().add(new WasmStructSet(
                standardClasses.classClass().getStructure(),
                new WasmGetLocal(targetVar),
                classFlagsOffset,
                new WasmInt32Constant(WasmGCClassFlags.FINAL | WasmGCClassFlags.PUBLIC)
        ));
        function.getBody().add(new WasmStructSet(
                standardClasses.classClass().getStructure(),
                new WasmGetLocal(targetVar),
                classArrayItemOffset,
                new WasmGetLocal(itemVar)
        ));
        function.getBody().add(new WasmStructSet(
                standardClasses.classClass().getStructure(),
                new WasmGetLocal(itemVar),
                classArrayOffset,
                new WasmGetLocal(targetVar)
        ));
        function.getBody().add(new WasmStructSet(
                standardClasses.classClass().getStructure(),
                new WasmGetLocal(targetVar),
                classTagOffset,
                new WasmInt32Constant(0)
        ));
        if (classParentOffset >= 0) {
            function.getBody().add(new WasmStructSet(
                    standardClasses.classClass().getStructure(),
                    new WasmGetLocal(targetVar),
                    classParentOffset,
                    new WasmGetGlobal(standardClasses.objectClass().pointer)
            ));
        }
        var newArrayFunction = newArrayGenerator.getNewObjectArrayFunction();
        newArrayFunction.setReferenced(true);
        function.getBody().add(new WasmStructSet(
                standardClasses.classClass().getStructure(),
                new WasmGetLocal(targetVar),
                classNewArrayOffset,
                new WasmFunctionReference(newArrayFunction)
        ));
        return function;
    }

    private WasmFunction createEnumConstantsFunction(WasmGCClassInfo classInfo, ClassReader cls) {
        var enumArrayStruct = getClassInfo(ValueType.parse(Enum[].class)).structure;
        var function = new WasmFunction(functionTypes.of(enumArrayStruct.getReference()));
        function.setName(names.topLevel(cls.getName() + "@constants"));
        module.functions.add(function);
        function.setReferenced(true);

        var fields = cls.getFields().stream()
                .filter(field -> field.hasModifier(ElementModifier.ENUM))
                .filter(field -> field.hasModifier(ElementModifier.STATIC))
                .map(field -> new WasmGetGlobal(getStaticFieldLocation(field.getReference())))
                .collect(Collectors.toList());

        if (classInfo.getInitializerPointer() != null) {
            function.getBody().add(new WasmCallReference(new WasmGetGlobal(classInfo.getInitializerPointer()),
                    functionTypes.of(null)));
        }

        var util = new WasmGCGenerationUtil(this);
        function.getBody().add(util.allocateArrayWithElements(ValueType.parse(Enum.class), () -> fields));

        return function;
    }

    private WasmExpression setClassField(WasmGCClassInfo classInfo, int fieldIndex, WasmExpression value) {
        return new WasmStructSet(
                standardClasses.classClass().getStructure(),
                new WasmGetGlobal(classInfo.pointer),
                fieldIndex,
                value
        );
    }
}
