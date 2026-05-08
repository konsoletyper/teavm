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
package org.teavm.backend.wasm.generate.classes;

import com.carrotsearch.hppc.ObjectByteHashMap;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.WasmGCInitializerContributor;
import org.teavm.backend.wasm.generate.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.WasmGeneratorUtil;
import org.teavm.backend.wasm.generate.reflection.ReflectionTypes;
import org.teavm.backend.wasm.generate.strings.WasmGCStringPool;
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
import org.teavm.backend.wasm.model.instruction.WasmFunctionReference;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmNullCondition;
import org.teavm.backend.wasm.model.instruction.WasmNullConstant;
import org.teavm.backend.wasm.model.instruction.WasmSignedType;
import org.teavm.backend.wasm.model.instruction.WasmStructNewDefault;
import org.teavm.backend.wasm.runtime.StringInternPool;
import org.teavm.backend.wasm.runtime.WasmGCSupport;
import org.teavm.backend.wasm.transformation.CoroutineTransformation;
import org.teavm.backend.wasm.vtable.WasmGCVirtualTable;
import org.teavm.backend.wasm.vtable.WasmGCVirtualTableEntry;
import org.teavm.backend.wasm.vtable.WasmGCVirtualTableProvider;
import org.teavm.dependency.DependencyInfo;
import org.teavm.interop.Structure;
import org.teavm.model.AccessLevel;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.PrimitiveType;
import org.teavm.model.ValueType;
import org.teavm.model.analysis.ClassInitializerInfo;
import org.teavm.model.analysis.ClassMetadataRequirements;
import org.teavm.model.classes.TagRegistry;
import org.teavm.model.util.ReflectionUtil;
import org.teavm.runtime.reflect.ClassInfo;
import org.teavm.runtime.reflect.ModifiersInfo;

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
    private Set<MethodReference> asyncSplitMethods;

    public final WasmGCStringPool strings;
    public final WasmGCStandardClasses standardClasses;
    private final ReflectionTypes reflectionTypes;
    public final WasmGCTypeMapper typeMapper;
    private final WasmGCNameProvider names;
    private WasmInstructionBuilder initializerFunctionStatements = new WasmInstructionList().builder();
    private WasmFunction createPrimitiveClassFunction;
    private WasmFunction getArrayClassFunction;
    private WasmFunction fillArrayClassFunction;
    private WasmFunction fillRegularClassFunction;
    private final WasmGCSupertypeFunctionGenerator supertypeGenerator;
    private final WasmGCNewArrayFunctionGenerator newArrayGenerator;
    private String arrayDataFieldName;
    private Map<WasmStorageType, WasmArray> arrayTypes = new HashMap<>();

    private int throwableNativeOffset = -1;
    private WasmFunction arrayGetObjectFunction;
    private WasmFunction arraySetObjectFunction;
    private WasmFunction arrayLengthObjectFunction;
    private WasmFunction arrayCopyObjectFunction;
    private List<WasmFunction> multiArrayFunctions = new ArrayList<>();
    private List<WasmStructure> nonInitializedStructures = new ArrayList<>();
    private WasmArray objectArrayType;
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
        reflectionTypes = new ReflectionTypes(names, module, dependencyInfo, classSource, functionTypes, this,
                functionProvider, metadataRequirements);
        typeMapper = new WasmGCTypeMapper(classSource, this, functionTypes, reflectionTypes);
        var customTypeMapperFactoryContext = customTypeMapperFactoryContext();
        typeMapper.setCustomTypeMappers(customTypeMapperFactories.stream()
                .map(factory -> factory.createTypeMapper(customTypeMapperFactoryContext))
                .collect(Collectors.toList()));

        reflectionGenerator = new WasmGCReflectionGenerator(module, functionTypes, this, names);
    }

    public void setCompactMode(boolean compactMode) {
        this.compactMode = compactMode;
    }

    public void setAsyncSplitMethods(Set<MethodReference> asyncSplitMethods) {
        this.asyncSplitMethods = asyncSplitMethods;
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

    public boolean hasSomethingToGenerate() {
        return !queue.isEmpty();
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
        var builder = function.getBody().builder();
        builder.transferFrom(initializerFunctionStatements);
        var classInfoType = reflectionTypes.classInfo();
        for (var classInfo : classInfoMap.values()) {
            if (classInfo.supertypeFunction != null) {
                builder
                        .getGlobal(classInfo.pointer)
                        .funcRef(classInfo.supertypeFunction)
                        .structSet(classInfoType.structure(), classInfoType.supertypeFunctionIndex());
            }
            if (classInfo.initArrayFunction != null) {
                builder
                        .getGlobal(classInfo.pointer)
                        .funcRef(classInfo.initArrayFunction)
                        .structSet(classInfoType.structure(), classInfoType.newArrayFunctionIndex());
            }
        }
        for (var consumer : staticFieldInitializers) {
            consumer.accept(function);
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
                    classInfo.virtualTableStructure = standardClasses.objectClass().getVirtualTableStructure();
                }
            } else {
                classInfo.virtualTableStructure = standardClasses.objectClass().virtualTableStructure;
            }

            var vtStructure = classInfo.virtualTableStructure;
            if (vtStructure != null) {
                var classInfoType = reflectionTypes.classInfo();
                classInfo.pointer = new WasmGlobal(pointerName, classInfoType.structure().getNonNullReference());
                classInfo.pointer.getInitialValue().add(new WasmStructNewDefault(
                        classInfoType.structure()));
                classInfo.pointer.setImmutable(true);
                module.globals.add(classInfo.pointer);
                if (virtualTable != null && virtualTable.isConcrete()) {
                    classInfo.virtualTablePointer = new WasmGlobal(vtName, vtStructure.getNonNullReference());
                    classInfo.virtualTablePointer.getInitialValue().add(new WasmStructNewDefault(
                            vtStructure));
                    if (compactMode) {
                        var cls = classInfo;
                        var vt = virtualTable;
                        queue.add(() -> {
                            cls.virtualTablePointer.getInitialValue().clear();
                            fillVirtualTableInitializer(cls.virtualTablePointer.getInitialValue().builder(), vt, cls);
                        });
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
                    if (type instanceof ValueType.Primitive && metadataRequirements.hasArrayNewInstance()) {
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

    @Override
    public ReflectionTypes reflectionTypes() {
        return reflectionTypes;
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
                    kind = ClassInfo.PrimitiveKind.BOOLEAN;
                    break;
                case BYTE:
                    kind = ClassInfo.PrimitiveKind.BYTE;
                    break;
                case SHORT:
                    kind = ClassInfo.PrimitiveKind.SHORT;
                    break;
                case CHARACTER:
                    kind = ClassInfo.PrimitiveKind.CHAR;
                    break;
                case INTEGER:
                    kind = ClassInfo.PrimitiveKind.INT;
                    break;
                case LONG:
                    kind = ClassInfo.PrimitiveKind.LONG;
                    break;
                case FLOAT:
                    kind = ClassInfo.PrimitiveKind.FLOAT;
                    break;
                case DOUBLE:
                    kind = ClassInfo.PrimitiveKind.DOUBLE;
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            var req = metadataRequirements.getInfo(type);
            var name = req != null && req.name() ? ReflectionUtil.typeName(type.getKind()) : null;
            fillPrimitiveClass(
                    target,
                    classInfo.pointer,
                    name,
                    kind
            );
        };
    }

    private void initVoidClass(WasmGCClassInfo classInfo) {
        classInfo.initializer = target -> {
            fillPrimitiveClass(
                    target,
                    classInfo.pointer,
                    "void",
                    ClassInfo.PrimitiveKind.VOID
            );
        };
    }

    private void initRegularClass(WasmGCClassInfo classInfo, WasmGCVirtualTable virtualTable, String name) {
        var cls = classSource.get(name);

        if (!classInfo.isHeapStructure() && classInitializerInfo.isDynamicInitializer(name)) {
            if (cls != null && cls.getMethod(CLINIT_METHOD_DESC) != null) {
                var clinitType = functionTypes.of(null);
                var wasmName = names.topLevel(names.suggestForClass(name) + "@initializer");
                classInfo.initializerPointer = new WasmGlobal(wasmName, clinitType.getReference());
                classInfo.initializerPointer.getInitialValue().add(new WasmNullConstant(
                        clinitType.getReference()));
                module.globals.add(classInfo.initializerPointer);
            }
        }
        classInfo.initializer = target -> {
            int tag;
            if (!classInfo.isHeapStructure()) {
                var ranges = tagRegistry.getRanges(name);
                tag = ranges.stream().mapToInt(range -> range.lower).min().orElse(0);
            } else {
                tag = 0;
            }
            int flags = !classInfo.isHeapStructure() && cls != null ? ElementModifier.encodeModifiers(cls) : 0;
            target
                    .getGlobal(classInfo.pointer)
                    .i32Const(tag)
                    .i32Const(flags)
                    .call(getFillRegularClassFunction());
            var metadataReq = metadataRequirements.getInfo(name);
            var classInfoCls = reflectionTypes.classInfo();
            if (metadataReq.name()) {
                var namePtr = strings.getStringConstant(name).global;
                target
                        .getGlobal(classInfo.pointer)
                        .getGlobal(namePtr)
                        .structSet(classInfoCls.structure(), classInfoCls.nameIndex());
            }
            if (cls != null) {
                if (metadataReq.simpleName() && cls.getSimpleName() != null) {
                    var namePtr = strings.getStringConstant(cls.getSimpleName()).global;
                    target
                            .getGlobal(classInfo.pointer)
                            .getGlobal(namePtr)
                            .structSet(classInfoCls.structure(), classInfoCls.simpleNameIndex());
                }
                if (cls.getParent() != null && metadataReq.superclass()) {
                    var parent = getClassInfo(cls.getParent());
                    target
                            .getGlobal(classInfo.pointer)
                            .getGlobal(parent.pointer)
                            .structSet(classInfoCls.structure(), classInfoCls.parentIndex());
                }
                if (cls.getOwnerName() != null && metadataReq.enclosingClass()) {
                    var owner = getClassInfo(cls.getOwnerName());
                    target
                            .getGlobal(classInfo.pointer)
                            .getGlobal(owner.pointer)
                            .structSet(classInfoCls.structure(), classInfoCls.enclosingClassIndex());
                }
                if (cls.getDeclaringClassName() != null && metadataReq.declaringClass()) {
                    var owner = getClassInfo(cls.getDeclaringClassName());
                    target
                            .getGlobal(classInfo.pointer)
                            .getGlobal(owner.pointer)
                            .structSet(classInfoCls.structure(), classInfoCls.declaringClassIndex());
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
                    target
                            .getGlobal(classInfo.pointer)
                            .funcRef(cloneFunction)
                            .structSet(classInfoCls.structure(), classInfoCls.cloneFunctionIndex());
                }
                if (metadataReq.enumConstants() && cls.hasModifier(ElementModifier.ENUM)) {
                    target
                            .getGlobal(classInfo.pointer)
                            .funcRef(createEnumConstantsFunction(cls))
                            .structSet(classInfoCls.structure(), classInfoCls.initEnumConstantsIndex());
                }
                if (metadataReq.interfaces() && !cls.getInterfaces().isEmpty()) {
                    target.getGlobal(classInfo.pointer);
                    createInterfacesArray(target, cls);
                    target.structSet(classInfoCls.structure(), classInfoCls.interfacesIndex());
                }
                if (!cls.hasModifier(ElementModifier.INTERFACE) && !cls.hasModifier(ElementModifier.ABSTRACT)
                        && !classInfo.isHeapStructure()) {
                    if (classInfoCls.createInstanceIndex() >= 0) {
                        var fn = createNewInstanceFunction(cls.getName(), classInfo);
                        target
                                .getGlobal(classInfo.pointer)
                                .funcRef(fn)
                                .structSet(classInfoCls.structure(), classInfoCls.createInstanceIndex());
                    }
                    if (classInfoCls.initNewInstanceIndex() >= 0) {
                        var defaultConstructor = cls.getMethod(new MethodDescriptor("<init>", void.class));
                        if (defaultConstructor != null && defaultConstructor.getLevel() == AccessLevel.PUBLIC
                                && defaultConstructor.getProgram() != null) {
                            var fn = createInitNewInstanceFunction(defaultConstructor);
                            target
                                    .getGlobal(classInfo.pointer)
                                    .funcRef(fn)
                                    .structSet(classInfoCls.structure(), classInfoCls.initNewInstanceIndex());
                        }
                    }
                }
            }

            if (!classInfo.isHeapStructure() && classInfo.virtualTablePointer != null) {
                if (compactMode) {
                    assignVTToClass(classInfo, target);
                } else {
                    fillVirtualTableMethods(target, virtualTable, classInfo);
                }
            }
            if (!classInfo.isHeapStructure() && classInfo.initializerPointer != null) {
                var initFunction = functionProvider.forStaticMethod(new MethodReference(name, CLINIT_METHOD_DESC));
                initFunction.setReferenced(true);
                classInfo.initializerPointer.getInitialValue().clear();
                classInfo.initializerPointer.getInitialValue().add(new WasmFunctionReference(initFunction));
                if (metadataRequirements.hasClassInit() && metadataReq != null && metadataReq.classInit()) {
                    target
                            .getGlobal(classInfo.pointer)
                            .funcRef(initFunction)
                            .structSet(classInfoCls.structure(), classInfoCls.initializerIndex());
                }
            }
        };
    }

    private void createInterfacesArray(WasmInstructionBuilder builder, ClassReader cls) {
        var classInfoStruct = reflectionTypes.classInfo();
        for (var itf : cls.getInterfaces()) {
            builder.getGlobal(getClassInfo(itf).getPointer());
        }
        builder.arrayNewFixed(classInfoStruct.interfacesType(), cls.getInterfaces().size());
    }

    private void assignClassToVT(WasmGCVirtualTable virtualTable, WasmGCClassInfo classInfo,
            WasmInstructionBuilder target) {
        if (virtualTable != null) {
            while (virtualTable.getParent() != null) {
                virtualTable = virtualTable.getParent();
            }
            if (!virtualTable.getClassName().equals("java.lang.Object")) {
                return;
            }
        }
        var vtStruct = standardClasses.objectClass().getVirtualTableStructure();
        target
                .getGlobal(classInfo.virtualTablePointer)
                .getGlobal(classInfo.pointer)
                .structSet(vtStruct, WasmGCClassInfoProvider.CLASS_FIELD_OFFSET);
        assignVTToClass(classInfo, target);
    }

    private void assignVTToClass(WasmGCClassInfo classInfo, WasmInstructionBuilder target) {
        target
                .getGlobal(classInfo.pointer)
                .getGlobal(classInfo.virtualTablePointer)
                .structSet(reflectionTypes.classInfo().structure(), reflectionTypes.classInfo().vtableIndex());
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

        var body = function.getBody().builder();
        body
                .getLocal(objLocal)
                .cast(classInfo.getStructure().getReference())
                .setLocal(castObjLocal);

        for (var i = 0; i < classInfo.structure.getFields().size(); ++i) {
            if (i == MONITOR_FIELD_OFFSET) {
                body.nullConst(WasmType.EQ);
            } else {
                var fieldType = classInfo.structure.getFields().get(i).getType();
                WasmSignedType signedType = null;
                if (fieldType instanceof WasmStorageType.Packed) {
                    signedType = WasmSignedType.UNSIGNED;
                }
                body
                        .getLocal(castObjLocal)
                        .structGet(classInfo.structure, i, signedType);
            }
        }

        body.structNew(classInfo.structure);
        return function;
    }

    private void fillVirtualTableMethods(WasmInstructionBuilder target, WasmGCVirtualTable virtualTable,
            WasmGCClassInfo classInfo) {
        var usedVt = virtualTable.getFirstUsed();
        if (usedVt == null) {
            return;
        }

        var global = classInfo.virtualTablePointer;
        var structure = classInfo.virtualTableStructure;

        var isObject = isObject(usedVt);
        if (isObject) {
            target
                    .getGlobal(global)
                    .getGlobal(classInfo.pointer)
                    .call(systemFunctions.getFillObjectVirtualTableFunction());
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

    private void fillVirtualTableInitializer(WasmInstructionBuilder builder, WasmGCVirtualTable virtualTable,
            WasmGCClassInfo classInfo) {
        var usedVt = virtualTable.getFirstUsed();
        if (usedVt == null) {
            builder.structNewDefault(classInfo.structure);
        }

        var entries = new ArrayList<Consumer<WasmInstructionBuilder>>(Collections.nCopies(
                classInfo.virtualTableStructure.getFields().size(), null));
        for (var i = 0; i < usedVt.getEntries().size(); ++i) {
            var entry = virtualTable.getEntries().get(i);
            var implementor = virtualTable.implementor(entry);
            if (implementor != null) {
                var function = functionProvider.forInstanceMethod(implementor);
                function.setReferenced(true);
                entries.set(entry.getIndex() + VIRTUAL_METHOD_OFFSET, b -> b.funcRef(function));
            }
        }

        entries.set(0, b -> b.getGlobal(classInfo.pointer));
        for (var i = 1; i < classInfo.virtualTableStructure.getFields().size(); ++i) {
            if (entries.get(i) == null) {
                var functionType = classInfo.virtualTableStructure.getFields().get(i).getUnpackedType();
                entries.set(i, b -> b.nullConst((WasmType.Reference) functionType));
            }
        }

        for (var entry : entries) {
            entry.accept(builder);
        }
        builder.structNew(classInfo.virtualTableStructure);
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

    private void fillArrayVirtualTableMethods(WasmInstructionBuilder target, WasmGlobal global,
            WasmGlobal classGlobal) {
        target
                .getGlobal(global)
                .getGlobal(classGlobal)
                .call(systemFunctions.getFillObjectVirtualTableFunction());
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
        var classInfoStruct = reflectionTypes.classInfo();
        var function = new WasmFunction(functionTypes.of(WasmType.INT32, classInfoStruct.structure().getReference(),
                standardClasses.objectClass().getType()));
        function.setReferenced(true);
        function.setName(names.topLevel("Array<*>::length"));
        module.functions.add(function);

        var classLocal = new WasmLocal(classInfoStruct.structure().getReference(), "this");
        var objectLocal = new WasmLocal(standardClasses.objectClass().getType(), "object");
        function.add(classLocal);
        function.add(objectLocal);

        function.getBody().builder()
                .getLocal(objectLocal)
                .cast(objectStructure.getNonNullReference())
                .structGet(objectStructure, ARRAY_DATA_FIELD_OFFSET)
                .arrayLength();
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
            var classInfoStruct = reflectionTypes.classInfo();
            arrayGetObjectFunction = new WasmFunction(classInfoStruct.getItemFunctionType());
            arrayGetObjectFunction.setName(names.topLevel(names.suggestForArray(
                    names.suggestForClass("java.lang.Object") + "::get")));
            module.functions.add(arrayGetObjectFunction);
            arrayGetObjectFunction.setReferenced(true);

            var arrayStruct = getClassInfo(ValueType.arrayOf(OBJECT_TYPE)).structure;
            var arrayDataTypeRef = (WasmType.CompositeReference) arrayStruct.getFields()
                    .get(ARRAY_DATA_FIELD_OFFSET).getUnpackedType();
            var arrayDataType = (WasmArray) arrayDataTypeRef.composite;
            var selfLocal = new WasmLocal(classInfoStruct.structure().getReference(), "this");
            var objectLocal = new WasmLocal(standardClasses.objectClass().getType(), "object");
            var indexLocal = new WasmLocal(WasmType.INT32, "index");
            arrayGetObjectFunction.add(selfLocal);
            arrayGetObjectFunction.add(objectLocal);
            arrayGetObjectFunction.add(indexLocal);

            arrayGetObjectFunction.getBody().builder()
                    .getLocal(objectLocal)
                    .cast(arrayStruct.getNonNullReference())
                    .structGet(arrayStruct, ARRAY_DATA_FIELD_OFFSET)
                    .getLocal(indexLocal)
                    .arrayGet(arrayDataType);
        }
        return arrayGetObjectFunction;
    }

    private WasmFunction generateArrayGetPrimitiveFunction(PrimitiveType type) {
        var classInfoStruct = reflectionTypes.classInfo();
        var function = new WasmFunction(classInfoStruct.getItemFunctionType());
        function.setName(names.topLevel(names.suggestForArray(names.suggestForType(ValueType.primitive(type)))
                + "::get"));
        module.functions.add(function);
        function.setReferenced(true);

        var arrayStruct = getClassInfo(ValueType.arrayOf(ValueType.primitive(type))).structure;
        var arrayDataTypeRef = (WasmType.CompositeReference) arrayStruct.getFields()
                .get(ARRAY_DATA_FIELD_OFFSET).getUnpackedType();
        var arrayDataType = (WasmArray) arrayDataTypeRef.composite;
        var classLocal = new WasmLocal(classInfoStruct.structure().getReference(), "this");
        var objectLocal = new WasmLocal(standardClasses.objectClass().getType(), "object");
        var indexLocal = new WasmLocal(WasmType.INT32, "index");
        function.add(classLocal);
        function.add(objectLocal);
        function.add(indexLocal);

        Class<?> primitiveType;
        Class<?> wrapperType;
        WasmSignedType signedType = null;
        switch (type) {
            case BOOLEAN:
                primitiveType = boolean.class;
                wrapperType = Boolean.class;
                signedType = WasmSignedType.UNSIGNED;
                break;
            case BYTE:
                primitiveType = byte.class;
                wrapperType = Byte.class;
                signedType = WasmSignedType.SIGNED;
                break;
            case SHORT:
                primitiveType = short.class;
                wrapperType = Short.class;
                signedType = WasmSignedType.SIGNED;
                break;
            case CHARACTER:
                primitiveType = char.class;
                wrapperType = Character.class;
                signedType = WasmSignedType.UNSIGNED;
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
        var body = function.getBody().builder();
        body.getLocal(objectLocal)
                .cast(arrayStruct.getNonNullReference())
                .structGet(arrayStruct, ARRAY_DATA_FIELD_OFFSET)
                .getLocal(indexLocal);
        if (signedType != null) {
            body.arrayGet(arrayDataType, signedType);
        } else {
            body.arrayGet(arrayDataType);
        }
        body.call(wrapFunction);

        return function;
    }

    private WasmFunction getArraySetFunction(ValueType itemType) {
        if (itemType instanceof ValueType.Primitive) {
            return generateArraySetPrimitiveFunction(((ValueType.Primitive) itemType).getKind());
        }
        return getArraySetObjectFunction();
    }

    private WasmFunction getArrayCopyFunction(ValueType itemType) {
        if (itemType instanceof ValueType.Primitive) {
            return createArrayCopyFunction(itemType);
        }
        return getArrayCopyObjectFunction();
    }

    private WasmFunction getArraySetObjectFunction() {
        if (arraySetObjectFunction == null) {
            var classInfoStruct = reflectionTypes.classInfo();
            arraySetObjectFunction = new WasmFunction(classInfoStruct.putItemFunctionType());
            arraySetObjectFunction.setName(names.topLevel(names.suggestForArray(
                    names.suggestForClass("java.lang.Object")) + "::set"));
            module.functions.add(arraySetObjectFunction);
            arraySetObjectFunction.setReferenced(true);

            var arrayStruct = getClassInfo(ValueType.arrayOf(OBJECT_TYPE)).structure;
            var arrayDataTypeRef = (WasmType.CompositeReference) arrayStruct.getFields()
                    .get(ARRAY_DATA_FIELD_OFFSET).getUnpackedType();
            var arrayDataType = (WasmArray) arrayDataTypeRef.composite;
            var selfLocal = new WasmLocal(classInfoStruct.structure().getReference(), "this");
            var objectLocal = new WasmLocal(standardClasses.objectClass().getType(), "object");
            var indexLocal = new WasmLocal(WasmType.INT32, "index");
            var valueLocal = new WasmLocal(standardClasses.objectClass().getType(), "value");
            arraySetObjectFunction.add(selfLocal);
            arraySetObjectFunction.add(objectLocal);
            arraySetObjectFunction.add(indexLocal);
            arraySetObjectFunction.add(valueLocal);

            arraySetObjectFunction.getBody().builder()
                    .getLocal(objectLocal)
                    .cast(arrayStruct.getNonNullReference())
                    .structGet(arrayStruct, ARRAY_DATA_FIELD_OFFSET)
                    .getLocal(indexLocal)
                    .getLocal(valueLocal)
                    .arraySet(arrayDataType);
        }
        return arraySetObjectFunction;
    }

    private WasmFunction generateArraySetPrimitiveFunction(PrimitiveType type) {
        var classInfoStruct = reflectionTypes.classInfo();
        var function = new WasmFunction(classInfoStruct.putItemFunctionType());
        function.setName(names.topLevel(names.suggestForArray(names.suggestForType(ValueType.primitive(type)))
                + "::set"));
        module.functions.add(function);
        function.setReferenced(true);

        var arrayStruct = getClassInfo(ValueType.arrayOf(ValueType.primitive(type))).structure;
        var arrayDataTypeRef = (WasmType.CompositeReference) arrayStruct.getFields()
                .get(ARRAY_DATA_FIELD_OFFSET).getUnpackedType();
        var arrayDataType = (WasmArray) arrayDataTypeRef.composite;
        var classLocal = new WasmLocal(classInfoStruct.structure().getReference(), "this");
        var objectLocal = new WasmLocal(standardClasses.objectClass().getType(), "object");
        var indexLocal = new WasmLocal(WasmType.INT32, "index");
        var valueLocal = new WasmLocal(standardClasses.objectClass().getType(), "value");
        function.add(classLocal);
        function.add(objectLocal);
        function.add(indexLocal);
        function.add(valueLocal);

        Class<?> primitiveType;
        Class<?> wrapperType;
        switch (type) {
            case BOOLEAN:
                primitiveType = boolean.class;
                wrapperType = Boolean.class;
                break;
            case BYTE:
                primitiveType = byte.class;
                wrapperType = Byte.class;
                break;
            case SHORT:
                primitiveType = short.class;
                wrapperType = Short.class;
                break;
            case CHARACTER:
                primitiveType = char.class;
                wrapperType = Character.class;
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
        var method = new MethodReference(wrapperType, primitiveType.getName() + "Value", primitiveType);
        var unwrapFunction = functionProvider.forInstanceMethod(method);
        function.getBody().builder()
                .getLocal(objectLocal)
                .cast(arrayStruct.getNonNullReference())
                .structGet(arrayStruct, ARRAY_DATA_FIELD_OFFSET)
                .getLocal(indexLocal)
                .getLocal(valueLocal)
                .cast(getClassInfo(ValueType.parse(wrapperType)).getType())
                .call(unwrapFunction)
                .arraySet(arrayDataType);

        return function;
    }

    private WasmFunction getArrayCopyObjectFunction() {
        if (arrayCopyObjectFunction == null) {
            arrayCopyObjectFunction = createArrayCopyFunction(OBJECT_TYPE);
        }
        return arrayCopyObjectFunction;
    }

    private WasmFunction createArrayCopyFunction(ValueType type) {
        var classInfoStruct = reflectionTypes.classInfo();
        var function = new WasmFunction(classInfoStruct.copyArrayFunctionType());
        function.setName(names.topLevel(names.suggestForArray(names.suggestForType(type)) + "::copy"));
        module.functions.add(function);
        function.setReferenced(true);

        var arrayStruct = getClassInfo(ValueType.arrayOf(type)).structure;
        var arrayDataTypeRef = (WasmType.CompositeReference) arrayStruct.getFields()
                .get(ARRAY_DATA_FIELD_OFFSET).getUnpackedType();
        var arrayDataType = (WasmArray) arrayDataTypeRef.composite;
        var thisLocal = new WasmLocal(classInfoStruct.structure().getReference(), "this");
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

        function.getBody().builder()
                .getLocal(targetLocal)
                .cast(arrayStruct.getNonNullReference())
                .structGet(arrayStruct, ARRAY_DATA_FIELD_OFFSET)
                .getLocal(targetIndexLocal)
                .getLocal(sourceLocal)
                .cast(arrayStruct.getNonNullReference())
                .structGet(arrayStruct, ARRAY_DATA_FIELD_OFFSET)
                .getLocal(sourceIndexLocal)
                .getLocal(countLocal)
                .arrayCopy(arrayDataType, arrayDataType);
        return function;
    }


    private void fillVirtualTableEntry(WasmInstructionBuilder target, WasmGlobal global,
            WasmStructure structure, WasmGCVirtualTable virtualTable, WasmGCVirtualTableEntry entry) {
        fillVirtualTableEntry(target, b -> b.getGlobal(global), structure, virtualTable, entry);
    }

    void fillVirtualTableEntry(WasmInstructionBuilder target, Consumer<WasmInstructionBuilder> instance,
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
                var instanceParam = new WasmLocal(getClassInfo(virtualTable.getClassName()).getType());
                wrapperFunction.add(instanceParam);
                var castTarget = getClassInfo(implementor.getClassName()).getStructure().getReference();
                var params = new WasmLocal[entry.getMethod().parameterCount()];
                for (var i = 0; i < entry.getMethod().parameterCount(); ++i) {
                    params[i] = new WasmLocal(typeMapper.mapType(entry.getMethod().parameterType(i)));
                    wrapperFunction.add(params[i]);
                }
                var wrapBody = wrapperFunction.getBody().builder();
                wrapBody.getLocal(instanceParam).cast(castTarget);
                for (var param : params) {
                    wrapBody.getLocal(param);
                }
                wrapBody.call(function);
                function = wrapperFunction;
            }
            function.setReferenced(true);
            instance.accept(target);
            target
                    .funcRef(function)
                    .structSet(structure, fieldIndex);
        }
    }

    private WasmFunction generateArrayCloneMethod(WasmStructure objectStructure, ValueType itemType) {
        var arrayTypeRef = (WasmType.CompositeReference) objectStructure.getFields().get(
                WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET).getUnpackedType();
        var arrayType = (WasmArray) arrayTypeRef.composite;

        var type = typeMapper.getFunctionType(standardClasses.objectClass().getType(), CLONE_METHOD_DESC);
        var function = new WasmFunction(type);
        function.setName(names.topLevel(names.suggestForArray(names.suggestForType(itemType)) + "::clone"));
        module.functions.add(function);
        var instanceLocal = new WasmLocal(standardClasses.objectClass().getType(), "instance");
        var originalLocal = new WasmLocal(objectStructure.getReference(), "original");
        var originalDataLocal = new WasmLocal(arrayType.getNonNullReference(), "originalData");
        var dataCopyLocal = new WasmLocal(arrayType.getNonNullReference(), "resultData");
        function.add(instanceLocal);
        function.add(originalLocal);
        function.add(originalDataLocal);
        function.add(dataCopyLocal);

        var body = function.getBody().builder();
        body.getLocal(instanceLocal).cast(objectStructure.getNonNullReference()).setLocal(originalLocal);
        body.getLocal(originalLocal).structGet(objectStructure, ARRAY_DATA_FIELD_OFFSET).setLocal(originalDataLocal);
        body.getLocal(originalDataLocal).arrayLength().arrayNewDefault(arrayType).setLocal(dataCopyLocal);
        body.getLocal(dataCopyLocal).i32Const(0)
                .getLocal(originalDataLocal).i32Const(0)
                .getLocal(originalDataLocal).arrayLength()
                .arrayCopy(arrayType, arrayType);
        body.getLocal(originalLocal).structGet(objectStructure, VT_FIELD_OFFSET)
                .nullConst(WasmType.EQ)
                .getLocal(dataCopyLocal)
                .structNew(objectStructure);
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
        var classField = new WasmField(reflectionTypes.classInfo().structure().getReference().asStorage());
        classField.setName(names.forMemberField(FAKE_CLASS_FIELD));
        fields.add(classField);
    }

    private void addSystemFields(List<WasmField> fields) {
        var vtField = new WasmField(standardClasses.objectClass().getVirtualTableStructure().getReference());
        vtField.setName(names.forMemberField(FAKE_VT_FIELD));
        fields.add(vtField);
        var monitorField = new WasmField(WasmType.EQ);
        monitorField.setName(names.forMemberField(FAKE_MONITOR_FIELD));
        fields.add(monitorField);
    }

    private void addVirtualTableFields(List<WasmField> fields, WasmGCVirtualTable virtualTable) {
        for (var entry : virtualTable.getEntries()) {
            var receiverType = compactMode
                    ? WasmType.ANY
                    : getClassInfo(entry.getOrigin().getClassName()).getType();
            var functionType = typeMapper.getFunctionType(receiverType, entry.getMethod());
            var field = new WasmField(functionType.getReference().asStorage());
            field.setName(names.forVirtualMethod(entry.getMethod()));
            fields.add(field);
        }
    }

    private void initArrayClass(WasmGCClassInfo classInfo, ValueType.Array type) {
        if (!(type.getItemType() instanceof ValueType.Primitive)) {
            return;
        }
        classInfo.initializer = target -> {
            var itemTypeInfo = getClassInfo(type.getItemType());
            target
                    .getGlobal(classInfo.pointer)
                    .getGlobal(itemTypeInfo.pointer)
                    .call(getFillArrayClassFunction());
            if (!compactMode) {
                fillArrayVirtualTableMethods(target, classInfo.virtualTablePointer,
                        classInfo.pointer);
            } else {
                assignVTToClass(classInfo, target);
            }
            var metadataReq = metadataRequirements.getInfo(type);
            if (metadataReq.cloneMethod()) {
                var cloneFunction = generateArrayCloneMethod(classInfo.structure, type.getItemType());
                cloneFunction.setReferenced(true);
                var classInfoType = reflectionTypes.classInfo();
                target
                        .getGlobal(classInfo.pointer)
                        .funcRef(cloneFunction)
                        .structSet(classInfoType.structure(), classInfoType.cloneFunctionIndex());
            }
            if (metadataReq.name() && type.getItemType() instanceof ValueType.Primitive) {
                var name = strings.getStringConstant(type.toString());
                var classInfoType = reflectionTypes.classInfo();
                target
                        .getGlobal(classInfo.pointer)
                        .getGlobal(name.global)
                        .structSet(classInfoType.structure(), classInfoType.nameIndex());
            }

            var itemType = type.getItemType();

            if (metadataReq.arrayLength()) {
                var lengthFunction = getArrayLengthFunction(classInfo.structure);
                var classInfoType = reflectionTypes.classInfo();
                target
                        .getGlobal(classInfo.pointer)
                        .funcRef(lengthFunction)
                        .structSet(classInfoType.structure(), classInfoType.arrayLengthIndex());
            }
            if (metadataReq.arrayGet()) {
                var getFunction = getArrayGetFunction(itemType);
                var classInfoType = reflectionTypes.classInfo();
                target
                        .getGlobal(classInfo.pointer)
                        .funcRef(getFunction)
                        .structSet(classInfoType.structure(), classInfoType.getItemIndex());
            }
            if (metadataReq.arraySet()) {
                var setFunction = getArraySetFunction(itemType);
                var classInfoType = reflectionTypes.classInfo();
                target
                        .getGlobal(classInfo.pointer)
                        .funcRef(setFunction)
                        .structSet(classInfoType.structure(), classInfoType.putItemIndex());
            }
            if (metadataReq.arrayCopy()) {
                var copyFunction = getArrayCopyFunction(itemType);
                var classInfoType = reflectionTypes.classInfo();
                target
                        .getGlobal(classInfo.pointer)
                        .funcRef(copyFunction)
                        .structSet(classInfoType.structure(), classInfoType.copyArrayIndex());
            }
        };
    }

    private void fillPrimitiveClass(WasmInstructionBuilder builder, WasmGlobal global, String name, int kind) {
        builder.getGlobal(global);
        if (metadataRequirements.hasName()) {
            if (name != null) {
                builder.getGlobal(strings.getStringConstant(name).global);
            } else {
                builder.nullConst(standardClasses.stringClass().getType());
            }
        }
        builder
                .i32Const(kind)
                .call(getCreatePrimitiveClassFunction());
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
                var size = WasmGeneratorUtil.getNativeTypeSize(field.getType());
                if (offset == 0) {
                    alignment = size;
                }
                offset = WasmGeneratorUtil.align(offset, size);
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
        var wasmName = names.topLevel(names.suggestForStaticField(fieldRef));
        var global = new WasmGlobal(wasmName, type);
        var initBuilder = global.getInitialValue().builder();
        if (initValue != null) {
            staticInitialValue(initBuilder, initValue);
        } else {
            defaultValueOfType(initBuilder, type);
        }
        dynamicInitialValue(global, initValue);
        module.globals.add(global);

        return global;
    }

    private void staticInitialValue(WasmInstructionBuilder builder, Object value) {
        if (value instanceof Boolean) {
            builder.i32Const((Boolean) value ? 1 : 0);
        } else if (value instanceof Byte) {
            builder.i32Const((Byte) value);
        } else if (value instanceof Short) {
            builder.i32Const((Short) value);
        } else if (value instanceof Character) {
            builder.i32Const((Character) value);
        } else if (value instanceof Integer) {
            builder.i32Const((Integer) value);
        } else if (value instanceof Long) {
            builder.i64Const((Long) value);
        } else if (value instanceof Float) {
            builder.f32Const((Float) value);
        } else if (value instanceof Double) {
            builder.f64Const((Double) value);
        } else {
            builder.nullConst(standardClasses.stringClass().getType());
        }
    }

    private void defaultValueOfType(WasmInstructionBuilder builder, WasmType type) {
        if (type == WasmType.INT32) {
            builder.i32Const(0);
        } else if (type == WasmType.INT64) {
            builder.i64Const(0L);
        } else if (type == WasmType.FLOAT32) {
            builder.f32Const(0);
        } else if (type == WasmType.FLOAT64) {
            builder.f64Const(0);
        } else if (type instanceof WasmType.Reference) {
            builder.nullConst((WasmType.Reference) type);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void dynamicInitialValue(WasmGlobal global, Object value) {
        if (value instanceof String) {
            var constant = strings.getStringConstant((String) value).global;
            staticFieldInitializers.add(function -> {
                function.getBody().builder()
                        .getGlobal(constant)
                        .setGlobal(global);
            });
        } else if (value instanceof ValueType) {
            var constant = getClassInfo((ValueType) value).pointer;
            staticFieldInitializers.add(function -> {
                function.getBody().builder()
                        .getGlobal(constant)
                        .setGlobal(global);
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
            var field = new WasmField(WasmType.EXTERN.asStorage(), "nativeRef");
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
            var field = new WasmField(WasmType.EXTERN.asStorage(), "nativeRef");
            fields.add(field);
        }
        if (className.equals("java.lang.Throwable")) {
            throwableNativeOffset = fields.size();
            var field = new WasmField(WasmType.EXTERN.asStorage(), "nativeRef");
            fields.add(field);
        }
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
            wasmArray = getPrimitiveArrayType(wasmElementType);
        } else {
            wasmArray = getObjectArrayType();
        }

        classInfo.structure.getFields().add(new WasmField(wasmArray.getNonNullReference().asStorage(),
                arrayDataFieldName()));
    }

    @Override
    public WasmArray getPrimitiveArrayType(WasmStorageType type) {
        var array = arrayTypes.get(type);
        if (array == null) {
            array = new WasmArray(null, type);
            module.types.add(array);
            arrayTypes.put(type, array);
        }
        return array;
    }

    @Override
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
        var classInfoType = reflectionTypes.classInfo();
        params.add(classInfoType.structure().getReference());
        if (metadataRequirements.hasName()) {
            params.add(standardClasses.stringClass().getType());
        }
        params.add(WasmType.INT32);
        var functionType = functionTypes.of(null, params.toArray(new WasmType[0]));
        var function = new WasmFunction(functionType);
        function.setName(names.topLevel("teavm@fillPrimitiveClass"));
        module.functions.add(function);

        var targetVar = new WasmLocal(classInfoType.structure().getReference(), "target");
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

        var body = function.getBody().builder();
        body.getLocal(targetVar)
                .i32Const(ModifiersInfo.FINAL | ModifiersInfo.PUBLIC)
                .structSet(classInfoType.structure(), classInfoType.modifiersIndex());
        body.getLocal(targetVar)
                .getLocal(kindVar)
                .structSet(classInfoType.structure(), classInfoType.primitiveKindIndex());
        if (nameVar != null) {
            body.getLocal(targetVar)
                    .getLocal(nameVar)
                    .structSet(classInfoType.structure(), classInfoType.nameIndex());
        }
        body.getLocal(targetVar)
                .i32Const(Integer.MAX_VALUE)
                .structSet(classInfoType.structure(), classInfoType.tagIndex());
        return function;
    }

    private WasmFunction getFillRegularClassFunction() {
        if (fillRegularClassFunction == null) {
            fillRegularClassFunction = createFillRegularClassFunction();
        }
        return fillRegularClassFunction;
    }

    private WasmFunction createFillRegularClassFunction() {
        var classInfoType = reflectionTypes.classInfo();

        var functionType = functionTypes.of(
                null,
                classInfoType.structure().getReference(),
                WasmType.INT32,
                WasmType.INT32
        );
        var function = new WasmFunction(functionType);
        module.functions.add(function);
        function.setName(names.topLevel("teavm@fillRegularClass"));

        var targetVar = new WasmLocal(classInfoType.structure().getReference(), "target");
        var idVar = new WasmLocal(WasmType.INT32, "id");
        var flagsVar = new WasmLocal(WasmType.INT32, "flags");
        function.add(targetVar);
        function.add(idVar);
        function.add(flagsVar);

        var body = function.getBody().builder();
        body.getLocal(targetVar)
                .getGlobal(standardClasses.objectClass().virtualTablePointer)
                .structSet(classInfoType.structure(), classInfoType.vtableIndex());
        body.getLocal(targetVar)
                .getLocal(idVar)
                .structSet(classInfoType.structure(), classInfoType.tagIndex());
        body.getLocal(targetVar)
                .getLocal(flagsVar)
                .structSet(classInfoType.structure(), classInfoType.modifiersIndex());

        if (metadataRequirements.hasArrayNewInstance()) {
            var newArrayFunction = newArrayGenerator.getNewObjectArrayFunction();
            newArrayFunction.setReferenced(true);
            body.getLocal(targetVar)
                    .funcRef(newArrayFunction)
                    .structSet(classInfoType.structure(), classInfoType.newArrayFunctionIndex());
        }

        if (classInfoType.nextClassIndex() >= 0) {
            body.getLocal(targetVar)
                    .getGlobal(classInfoType.firstClassGlobal())
                    .structSet(classInfoType.structure(), classInfoType.nextClassIndex());
            body.getLocal(targetVar).setGlobal(classInfoType.firstClassGlobal());
        }

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
        var classInfoType = reflectionTypes.classInfo();
        var functionType = functionTypes.of(classInfoType.structure().getNonNullReference(),
                classInfoType.structure().getReference());
        var function = new WasmFunction(functionType);
        var arrayType = getClassInfo(ValueType.arrayOf(ValueType.object("java.lang.Object")));
        module.functions.add(function);
        function.setName(names.topLevel("teavm@getArrayClass"));
        queue.add(() -> {
            var itemTypeVar = new WasmLocal(classInfoType.structure().getReference(), "itemType");
            var resultVar = new WasmLocal(classInfoType.structure().getNonNullReference(), "result");
            var vtVar = new WasmLocal(standardClasses.objectClass().getVirtualTableStructure().getNonNullReference(),
                    "vt");
            function.add(itemTypeVar);
            function.add(resultVar);
            function.add(vtVar);

            var body = function.getBody().builder();
            var blockBody = body.block();
            blockBody
                    .getLocal(itemTypeVar)
                    .structGet(classInfoType.structure(), classInfoType.arrayTypeIndex())
                    .nullBranch(WasmNullCondition.NULL, blockBody);
            blockBody.return_();

            body.structNewDefault(classInfoType.structure()).setLocal(resultVar);
            body.getLocal(itemTypeVar).getLocal(resultVar)
                    .structSet(classInfoType.structure(), classInfoType.arrayTypeIndex());
            body.getLocal(resultVar).getLocal(itemTypeVar).call(getFillArrayClassFunction());

            var supertypeFunction = supertypeGenerator.getIsArraySupertypeFunction();
            supertypeFunction.setReferenced(true);
            body.getLocal(resultVar)
                    .funcRef(supertypeFunction)
                    .structSet(classInfoType.structure(), classInfoType.supertypeFunctionIndex());

            var cloneFunction = generateArrayCloneMethod(arrayType.structure, ValueType.object("java.lang.Object"));
            cloneFunction.setReferenced(true);
            body.getLocal(resultVar)
                    .funcRef(cloneFunction)
                    .structSet(classInfoType.structure(), classInfoType.cloneFunctionIndex());

            body.structNewDefault(standardClasses.objectClass().getVirtualTableStructure()).setLocal(vtVar);
            body.getLocal(vtVar).getLocal(resultVar).call(systemFunctions.getFillObjectVirtualTableFunction());
            body.getLocal(resultVar);
        });
        return function;
    }

    private WasmFunction getFillArrayClassFunction() {
        if (fillArrayClassFunction == null) {
            fillArrayClassFunction = createFillArrayClassFunction();
        }
        return fillArrayClassFunction;
    }

    private WasmFunction createFillArrayClassFunction() {
        var classInfoType = reflectionTypes.classInfo();

        var functionType = functionTypes.of(
                null,
                classInfoType.structure().getReference(),
                classInfoType.structure().getReference()
        );
        var function = new WasmFunction(functionType);
        module.functions.add(function);
        function.setName(names.topLevel("teavm@fillArrayClass"));

        var targetVar = new WasmLocal(classInfoType.structure().getReference(), "target");
        var itemVar = new WasmLocal(classInfoType.structure().getReference(), "item");
        function.add(targetVar);
        function.add(itemVar);

        var body = function.getBody().builder();
        body.getLocal(targetVar)
                .getGlobal(standardClasses.objectClass().getVirtualTablePointer())
                .structSet(classInfoType.structure(), classInfoType.vtableIndex());
        body.getLocal(targetVar)
                .i32Const(ModifiersInfo.FINAL | ModifiersInfo.PUBLIC)
                .structSet(classInfoType.structure(), classInfoType.modifiersIndex());
        body.getLocal(targetVar)
                .getLocal(itemVar)
                .structSet(classInfoType.structure(), classInfoType.itemTypeIndex());
        body.getLocal(itemVar)
                .getLocal(targetVar)
                .structSet(classInfoType.structure(), classInfoType.arrayTypeIndex());
        body.getLocal(targetVar)
                .i32Const(0)
                .structSet(classInfoType.structure(), classInfoType.tagIndex());
        if (classInfoType.parentIndex() >= 0) {
            body.getLocal(targetVar)
                    .getGlobal(standardClasses.objectClass().pointer)
                    .structSet(classInfoType.structure(), classInfoType.parentIndex());
        }
        if (classInfoType.arrayLengthIndex() >= 0) {
            var arrayClass = getClassInfo(ValueType.arrayOf(ValueType.object("java.lang.Object")));
            var lengthFunction = getArrayLengthFunction(arrayClass.structure);
            body.getLocal(targetVar)
                    .funcRef(lengthFunction)
                    .structSet(classInfoType.structure(), classInfoType.arrayLengthIndex());
        }
        if (classInfoType.getItemIndex() >= 0) {
            body.getLocal(targetVar)
                    .funcRef(getArrayGetObjectFunction())
                    .structSet(classInfoType.structure(), classInfoType.getItemIndex());
        }
        if (classInfoType.putItemIndex() >= 0) {
            body.getLocal(targetVar)
                    .funcRef(getArraySetObjectFunction())
                    .structSet(classInfoType.structure(), classInfoType.putItemIndex());
        }
        if (classInfoType.newArrayFunctionIndex() >= 0) {
            var newArrayFunction = newArrayGenerator.getNewObjectArrayFunction();
            newArrayFunction.setReferenced(true);
            body.getLocal(targetVar)
                    .funcRef(newArrayFunction)
                    .structSet(classInfoType.structure(), classInfoType.newArrayFunctionIndex());
        }
        if (classInfoType.copyArrayIndex() >= 0) {
            var copyFunction = getArrayCopyObjectFunction();
            body.getLocal(targetVar)
                    .funcRef(copyFunction)
                    .structSet(classInfoType.structure(), classInfoType.copyArrayIndex());
        }
        return function;
    }

    private WasmFunction createEnumConstantsFunction(ClassReader cls) {
        var classInfoStruct = reflectionTypes.classInfo();
        var function = new WasmFunction(functionTypes.of(classInfoStruct.enumConstantsType().getReference()));
        function.setName(names.topLevel(cls.getName() + "@constants"));
        module.functions.add(function);
        function.setReferenced(true);

        var fields = cls.getFields().stream()
                .filter(field -> field.hasModifier(ElementModifier.ENUM))
                .filter(field -> field.hasModifier(ElementModifier.STATIC))
                .collect(Collectors.toList());

        var body = function.getBody().builder();
        for (var field : fields) {
            body.getGlobal(getStaticFieldLocation(field.getReference()));
        }
        body.arrayNewFixed(classInfoStruct.enumConstantsType(), fields.size());

        return function;
    }

    private WasmFunction createNewInstanceFunction(String className, WasmGCClassInfo classInfo) {
        var objCls = standardClasses.objectClass();
        var function = new WasmFunction(functionTypes.of(objCls.getType()));
        function.setName(names.topLevel(className + "@newInstance"));
        var resultLocal = new WasmLocal(classInfo.getType(), "result");
        function.add(resultLocal);
        var body = function.getBody().builder();
        body.structNewDefault(classInfo.getStructure()).setLocal(resultLocal);
        body.getLocal(resultLocal)
                .getGlobal(classInfo.getVirtualTablePointer())
                .structSet(objCls.getStructure(), WasmGCClassInfoProvider.VT_FIELD_OFFSET);
        body.getLocal(resultLocal);
        function.setReferenced(true);
        module.functions.add(function);
        return function;
    }

    private WasmFunction createInitNewInstanceFunction(MethodReader method) {
        var className = method.getOwnerName();
        var objectClass = getClassInfo("java.lang.Object");
        var instantiatorType = functionTypes.of(null, objectClass.getType());

        var instantiator = new WasmFunction(instantiatorType);
        instantiator.setName(names.topLevel(className + "@initInstance"));
        instantiator.setReferenced(true);
        module.functions.add(instantiator);

        var classInfo = getClassInfo(className);
        var param = new WasmLocal(objectClass.getType(), "param");
        var localVar = new WasmLocal(classInfo.getType(), "instance");
        instantiator.add(param);
        instantiator.add(localVar);

        var body = instantiator.getBody().builder();
        body.getLocal(param).cast(classInfo.getType()).setLocal(localVar);
        var suspend = asyncSplitMethods.contains(method.getReference());
        body.getLocal(localVar).call(functionProvider.forInstanceMethod(method.getReference()), suspend);
        if (suspend) {
            var transformation = new CoroutineTransformation(
                    functionTypes,
                    functionProvider,
                    this
            );
            transformation.transform(instantiator);
        }

        return instantiator;
    }
}
