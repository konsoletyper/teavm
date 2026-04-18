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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.WasmGCInitializerContributor;
import org.teavm.backend.wasm.generate.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.WasmGeneratorUtil;
import org.teavm.backend.wasm.generate.reflection.ReflectionTypes;
import org.teavm.backend.wasm.generate.strings.WasmGCStringPool;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmExpressionToInstructionConverter;
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
import org.teavm.backend.wasm.model.expression.WasmArraySet;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmFloat32Constant;
import org.teavm.backend.wasm.model.expression.WasmFloat64Constant;
import org.teavm.backend.wasm.model.expression.WasmFunctionReference;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt64Constant;
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
import org.teavm.backend.wasm.model.instruction.WasmFunctionReferenceInstruction;
import org.teavm.backend.wasm.model.instruction.WasmNullConstantInstruction;
import org.teavm.backend.wasm.model.instruction.WasmStructNewDefaultInstruction;
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
    private List<WasmExpression> initializerFunctionStatements = new ArrayList<>();
    private List<Consumer<WasmFunction>> annotationInitStatements = new ArrayList<>();
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
        var converter = new WasmExpressionToInstructionConverter(function.getBody());
        converter.convertAll(initializerFunctionStatements);
        initializerFunctionStatements.clear();
        for (var classInfo : classInfoMap.values()) {
            if (classInfo.supertypeFunction != null) {
                setClassField(classInfo, reflectionTypes.classInfo().supertypeFunctionIndex(),
                        new WasmFunctionReference(classInfo.supertypeFunction)).acceptVisitor(converter);
            }
            if (classInfo.initArrayFunction != null) {
                setClassField(classInfo, reflectionTypes.classInfo().newArrayFunctionIndex(),
                        new WasmFunctionReference(classInfo.initArrayFunction)).acceptVisitor(converter);
            }
        }
        for (var consumer : staticFieldInitializers) {
            consumer.accept(function);
        }
        if (!annotationInitStatements.isEmpty()) {
            var annotationsFunction = new WasmFunction(functionTypes.of(null));
            annotationsFunction.setName(names.topLevel("teavm@initClassAnnotations"));
            module.functions.add(annotationsFunction);
            new WasmCall(annotationsFunction).acceptVisitor(converter);
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
                    classInfo.virtualTableStructure = standardClasses.objectClass().getVirtualTableStructure();
                }
            } else {
                classInfo.virtualTableStructure = standardClasses.objectClass().virtualTableStructure;
            }

            var vtStructure = classInfo.virtualTableStructure;
            if (vtStructure != null) {
                var classInfoType = reflectionTypes.classInfo();
                classInfo.pointer = new WasmGlobal(pointerName, classInfoType.structure().getNonNullReference());
                classInfo.pointer.getInitialValue().add(new WasmStructNewDefaultInstruction(
                        classInfoType.structure()));
                classInfo.pointer.setImmutable(true);
                module.globals.add(classInfo.pointer);
                if (virtualTable != null && virtualTable.isConcrete()) {
                    classInfo.virtualTablePointer = new WasmGlobal(vtName, vtStructure.getNonNullReference());
                    classInfo.virtualTablePointer.getInitialValue().add(new WasmStructNewDefaultInstruction(
                            vtStructure));
                    if (compactMode) {
                        var cls = classInfo;
                        var vt = virtualTable;
                        queue.add(() -> {
                            cls.virtualTablePointer.getInitialValue().clear();
                            new WasmExpressionToInstructionConverter(cls.virtualTablePointer.getInitialValue())
                                    .convert(fillVirtualTableInitializer(vt, cls));
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
                    ClassInfo.PrimitiveKind.VOID
            ));
        };
    }

    private void initRegularClass(WasmGCClassInfo classInfo, WasmGCVirtualTable virtualTable, String name) {
        var cls = classSource.get(name);

        if (!classInfo.isHeapStructure() && classInitializerInfo.isDynamicInitializer(name)) {
            if (cls != null && cls.getMethod(CLINIT_METHOD_DESC) != null) {
                var clinitType = functionTypes.of(null);
                var wasmName = names.topLevel(names.suggestForClass(name) + "@initializer");
                classInfo.initializerPointer = new WasmGlobal(wasmName, clinitType.getReference());
                classInfo.initializerPointer.getInitialValue().add(new WasmNullConstantInstruction(
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
            target.add(new WasmCall(getFillRegularClassFunction(), new WasmGetGlobal(classInfo.pointer),
                    new WasmInt32Constant(tag), new WasmInt32Constant(flags)));
            var metadataReq = metadataRequirements.getInfo(name);
            var classInfoCls = reflectionTypes.classInfo();
            if (metadataReq.name()) {
                var namePtr = strings.getStringConstant(name).global;
                target.add(setClassField(classInfo, classInfoCls.nameIndex(), new WasmGetGlobal(namePtr)));
            }
            if (cls != null) {
                if (metadataReq.simpleName() && cls.getSimpleName() != null) {
                    var namePtr = strings.getStringConstant(cls.getSimpleName()).global;
                    target.add(setClassField(classInfo, classInfoCls.simpleNameIndex(), new WasmGetGlobal(namePtr)));
                }
                if (cls.getParent() != null && metadataReq.superclass()) {
                    var parent = getClassInfo(cls.getParent());
                    target.add(setClassField(classInfo, classInfoCls.parentIndex(), new WasmGetGlobal(parent.pointer)));
                }
                if (cls.getOwnerName() != null && metadataReq.enclosingClass()) {
                    var owner = getClassInfo(cls.getOwnerName());
                    target.add(setClassField(classInfo, classInfoCls.enclosingClassIndex(),
                            new WasmGetGlobal(owner.pointer)));
                }
                if (cls.getDeclaringClassName() != null && metadataReq.declaringClass()) {
                    var owner = getClassInfo(cls.getDeclaringClassName());
                    target.add(setClassField(classInfo, classInfoCls.declaringClassIndex(),
                            new WasmGetGlobal(owner.pointer)));
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
                    target.add(setClassField(classInfo, classInfoCls.cloneFunctionIndex(),
                            new WasmFunctionReference(cloneFunction)));
                }
                if (metadataReq.enumConstants() && cls.hasModifier(ElementModifier.ENUM)) {
                    target.add(setClassField(classInfo, classInfoCls.initEnumConstantsIndex(),
                            new WasmFunctionReference(createEnumConstantsFunction(cls))));
                }
                if (metadataReq.interfaces() && !cls.getInterfaces().isEmpty()) {
                    target.add(setClassField(classInfo, classInfoCls.interfacesIndex(), createInterfacesArray(cls)));
                }
                if (!cls.hasModifier(ElementModifier.INTERFACE) && !cls.hasModifier(ElementModifier.ABSTRACT)
                        && !classInfo.isHeapStructure()) {
                    if (classInfoCls.createInstanceIndex() >= 0) {
                        var fn = createNewInstanceFunction(cls.getName(), classInfo);
                        target.add(setClassField(classInfo, classInfoCls.createInstanceIndex(),
                                new WasmFunctionReference(fn)));
                    }
                    if (classInfoCls.initNewInstanceIndex() >= 0) {
                        var defaultConstructor = cls.getMethod(new MethodDescriptor("<init>", void.class));
                        if (defaultConstructor != null && defaultConstructor.getLevel() == AccessLevel.PUBLIC
                                && defaultConstructor.getProgram() != null) {
                            var fn = createInitNewInstanceFunction(defaultConstructor);
                            target.add(setClassField(classInfo, classInfoCls.initNewInstanceIndex(),
                                    new WasmFunctionReference(fn)));
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
                classInfo.initializerPointer.getInitialValue().add(new WasmFunctionReferenceInstruction(initFunction));
                if (metadataRequirements.hasClassInit() && metadataReq != null && metadataReq.classInit()) {
                    target.add(setClassField(classInfo, classInfoCls.initializerIndex(),
                            new WasmFunctionReference(initFunction)));
                }
            }
        };
    }

    private WasmExpression createInterfacesArray(ClassReader cls) {
        var classInfoStruct = reflectionTypes.classInfo();
        var result = new WasmArrayNewFixed(classInfoStruct.interfacesType());
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
        target.add(new WasmStructSet(vtStruct,
                new WasmGetGlobal(classInfo.virtualTablePointer), WasmGCClassInfoProvider.CLASS_FIELD_OFFSET,
                new WasmGetGlobal(classInfo.pointer)));
        assignVTToClass(classInfo, target);
    }

    private void assignVTToClass(WasmGCClassInfo classInfo, List<WasmExpression> target) {
        target.add(new WasmStructSet(reflectionTypes.classInfo().structure(),
                new WasmGetGlobal(classInfo.pointer), reflectionTypes.classInfo().vtableIndex(),
                new WasmGetGlobal(classInfo.virtualTablePointer)));
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

        var converter = new WasmExpressionToInstructionConverter(function.getBody());
        new WasmSetLocal(castObjLocal, cast).acceptVisitor(converter);

        var copy = new WasmStructNew(classInfo.structure);
        for (var i = 0; i < classInfo.structure.getFields().size(); ++i) {
            if (i == MONITOR_FIELD_OFFSET) {
                copy.getInitializers().add(new WasmNullConstant(WasmType.EQ));
            } else {
                var fieldType = classInfo.structure.getFields().get(i).getType();
                var getExpr = new WasmStructGet(classInfo.structure, new WasmGetLocal(castObjLocal), i);
                if (fieldType instanceof WasmStorageType.Packed) {
                    getExpr.setSignedType(WasmSignedType.UNSIGNED);
                }
                copy.getInitializers().add(getExpr);
            }
        }

        copy.acceptVisitor(converter);
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

    private boolean isObject(WasmGCVirtualTable vt) {
        while (vt != null) {
            if (vt.getClassName().equals("java.lang.Object")) {
                return true;
            }
            vt = vt.getParent();
        }
        return false;
    }

    private void fillArrayVirtualTableMethods(List<WasmExpression> target, WasmGlobal global, WasmGlobal classGlobal) {
        target.add(new WasmCall(systemFunctions.getFillObjectVirtualTableFunction(), new WasmGetGlobal(global),
                new WasmGetGlobal(classGlobal)));
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

        var castObject = new WasmCast(new WasmGetLocal(objectLocal), objectStructure.getNonNullReference());
        var arrayField = new WasmStructGet(objectStructure, castObject, ARRAY_DATA_FIELD_OFFSET);
        var result = new WasmArrayLength(arrayField);
        var converter = new WasmExpressionToInstructionConverter(function.getBody());
        result.acceptVisitor(converter);
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

            var array = new WasmCast(new WasmGetLocal(objectLocal), arrayStruct.getNonNullReference());
            var arrayData = new WasmStructGet(arrayStruct, array, ARRAY_DATA_FIELD_OFFSET);
            var result = new WasmArrayGet(arrayDataType, arrayData, new WasmGetLocal(indexLocal));
            var converter = new WasmExpressionToInstructionConverter(arrayGetObjectFunction.getBody());
            result.acceptVisitor(converter);
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
        var converter = new WasmExpressionToInstructionConverter(function.getBody());
        castResult.acceptVisitor(converter);

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

            var array = new WasmCast(new WasmGetLocal(objectLocal), arrayStruct.getNonNullReference());
            var arrayData = new WasmStructGet(arrayStruct, array, ARRAY_DATA_FIELD_OFFSET);
            var set = new WasmArraySet(arrayDataType, arrayData, new WasmGetLocal(indexLocal),
                    new WasmGetLocal(valueLocal));
            var converter = new WasmExpressionToInstructionConverter(arraySetObjectFunction.getBody());
            set.acceptVisitor(converter);
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

        var array = new WasmCast(new WasmGetLocal(objectLocal), arrayStruct.getNonNullReference());
        var arrayData = new WasmStructGet(arrayStruct, array, ARRAY_DATA_FIELD_OFFSET);
        var set = new WasmArraySet(arrayDataType, arrayData, new WasmGetLocal(indexLocal),
                new WasmGetLocal(valueLocal));
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
        set.setValue(new WasmCast(set.getValue(), getClassInfo(ValueType.parse(wrapperType)).getType()));
        set.setValue(new WasmCall(unwrapFunction, set.getValue()));
        var converter = new WasmExpressionToInstructionConverter(function.getBody());
        set.acceptVisitor(converter);

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

        var sourceArray = new WasmCast(new WasmGetLocal(sourceLocal), arrayStruct.getNonNullReference());
        var sourceArrayData = new WasmStructGet(arrayStruct, sourceArray, ARRAY_DATA_FIELD_OFFSET);
        var targetArray = new WasmCast(new WasmGetLocal(targetLocal), arrayStruct.getNonNullReference());
        var targetArrayData = new WasmStructGet(arrayStruct, targetArray, ARRAY_DATA_FIELD_OFFSET);

        var converter = new WasmExpressionToInstructionConverter(function.getBody());
        new WasmArrayCopy(
                arrayDataType, targetArrayData, new WasmGetLocal(targetIndexLocal),
                arrayDataType, sourceArrayData, new WasmGetLocal(sourceIndexLocal),
                new WasmGetLocal(countLocal)).acceptVisitor(converter);
        return function;
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
                var castTarget = getClassInfo(implementor.getClassName()).getStructure().getReference();
                call.getArguments().add(new WasmCast(new WasmGetLocal(instanceParam), castTarget));
                var params = new WasmLocal[entry.getMethod().parameterCount()];
                for (var i = 0; i < entry.getMethod().parameterCount(); ++i) {
                    params[i] = new WasmLocal(typeMapper.mapType(entry.getMethod().parameterType(i)));
                    call.getArguments().add(new WasmGetLocal(params[i]));
                    wrapperFunction.add(params[i]);
                }
                var converter = new WasmExpressionToInstructionConverter(wrapperFunction.getBody());
                call.acceptVisitor(converter);
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

        var newExpr = new WasmStructNew(objectStructure);
        var converter = new WasmExpressionToInstructionConverter(function.getBody());
        new WasmSetLocal(originalLocal,
                new WasmCast(new WasmGetLocal(instanceLocal), objectStructure.getNonNullReference()))
                .acceptVisitor(converter);

        var classValue = new WasmStructGet(objectStructure, new WasmGetLocal(originalLocal),
                WasmGCClassInfoProvider.VT_FIELD_OFFSET);
        newExpr.getInitializers().add(classValue);
        newExpr.getInitializers().add(new WasmNullConstant(WasmType.EQ));

        var originalDataValue = new WasmStructGet(objectStructure, new WasmGetLocal(originalLocal),
                WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
        new WasmSetLocal(originalDataLocal, originalDataValue)
                .acceptVisitor(converter);
        var originalLength = new WasmArrayLength(new WasmGetLocal(originalDataLocal));
        new WasmSetLocal(dataCopyLocal, new WasmArrayNewDefault(arrayType, originalLength))
                .acceptVisitor(converter);
        newExpr.getInitializers().add(new WasmGetLocal(dataCopyLocal));

        new WasmArrayCopy(arrayType, new WasmGetLocal(dataCopyLocal),
                new WasmInt32Constant(0), arrayType, new WasmGetLocal(originalDataLocal),
                new WasmInt32Constant(0), new WasmArrayLength(new WasmGetLocal(originalDataLocal)))
                .acceptVisitor(converter);

        newExpr.acceptVisitor(converter);
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
            target.add(new WasmCall(
                    getFillArrayClassFunction(),
                    new WasmGetGlobal(classInfo.pointer),
                    new WasmGetGlobal(itemTypeInfo.pointer)
            ));
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
                target.add(setClassField(classInfo, classInfoType.cloneFunctionIndex(),
                        new WasmFunctionReference(cloneFunction)));
            }
            if (metadataReq.name() && type.getItemType() instanceof ValueType.Primitive) {
                var name = strings.getStringConstant(type.toString());
                target.add(setClassField(classInfo, reflectionTypes.classInfo().nameIndex(),
                        new WasmGetGlobal(name.global)));
            }

            var itemType = type.getItemType();

            if (metadataReq.arrayLength()) {
                var lengthFunction = getArrayLengthFunction(classInfo.structure);
                target.add(setClassField(classInfo, reflectionTypes.classInfo().arrayLengthIndex(),
                        new WasmFunctionReference(lengthFunction)));
            }
            if (metadataReq.arrayGet()) {
                var getFunction = getArrayGetFunction(itemType);
                target.add(setClassField(classInfo, reflectionTypes.classInfo().getItemIndex(),
                        new WasmFunctionReference(getFunction)));
            }
            if (metadataReq.arraySet()) {
                var setFunction = getArraySetFunction(itemType);
                target.add(setClassField(classInfo, reflectionTypes.classInfo().putItemIndex(),
                        new WasmFunctionReference(setFunction)));
            }
            if (metadataReq.arrayCopy()) {
                var copyFunction = getArrayCopyFunction(itemType);
                target.add(setClassField(classInfo, reflectionTypes.classInfo().copyArrayIndex(),
                        new WasmFunctionReference(copyFunction)));
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
        var wasmInitialValue = initValue != null ? initialValue(initValue) : WasmExpression.defaultValueOfType(type);
        var wasmName = names.topLevel(names.suggestForStaticField(fieldRef));
        var global = new WasmGlobal(wasmName, type);
        new WasmExpressionToInstructionConverter(global.getInitialValue()).convert(wasmInitialValue);
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
                new WasmSetGlobal(global, new WasmGetGlobal(constant))
                        .acceptVisitor(new WasmExpressionToInstructionConverter(function.getBody()));

            });
        } else if (value instanceof ValueType) {
            var constant = getClassInfo((ValueType) value).pointer;
            staticFieldInitializers.add(function -> {
                new WasmSetGlobal(global, new WasmGetGlobal(constant))
                        .acceptVisitor(new WasmExpressionToInstructionConverter(function.getBody()));
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

        var converter = new WasmExpressionToInstructionConverter(function.getBody());
        converter.convert(new WasmStructSet(
                classInfoType.structure(),
                new WasmGetLocal(targetVar),
                classInfoType.modifiersIndex(),
                new WasmInt32Constant(ModifiersInfo.FINAL | ModifiersInfo.PUBLIC)
        ));
        converter.convert(new WasmStructSet(
                classInfoType.structure(),
                new WasmGetLocal(targetVar),
                classInfoType.primitiveKindIndex(),
                new WasmGetLocal(kindVar)
        ));
        if (nameVar != null) {
            converter.convert(new WasmStructSet(
                    classInfoType.structure(),
                    new WasmGetLocal(targetVar),
                    classInfoType.nameIndex(),
                    new WasmGetLocal(nameVar)
            ));
        }
        converter.convert(new WasmStructSet(
                classInfoType.structure(),
                new WasmGetLocal(targetVar),
                classInfoType.tagIndex(),
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

        var converter = new WasmExpressionToInstructionConverter(function.getBody());
        converter.convert(new WasmStructSet(
                classInfoType.structure(),
                new WasmGetLocal(targetVar),
                classInfoType.vtableIndex(),
                new WasmGetGlobal(standardClasses.objectClass().virtualTablePointer)
        ));
        converter.convert(new WasmStructSet(
                classInfoType.structure(),
                new WasmGetLocal(targetVar),
                classInfoType.tagIndex(),
                new WasmGetLocal(idVar)
        ));
        converter.convert(new WasmStructSet(
                classInfoType.structure(),
                new WasmGetLocal(targetVar),
                classInfoType.modifiersIndex(),
                new WasmGetLocal(flagsVar)
        ));

        if (metadataRequirements.hasArrayNewInstance()) {
            var newArrayFunction = newArrayGenerator.getNewObjectArrayFunction();
            newArrayFunction.setReferenced(true);
            converter.convert(new WasmStructSet(
                    classInfoType.structure(),
                    new WasmGetLocal(targetVar),
                    classInfoType.newArrayFunctionIndex(),
                    new WasmFunctionReference(newArrayFunction)
            ));
        }

        if (classInfoType.nextClassIndex() >= 0) {
            converter.convert(new WasmStructSet(
                classInfoType.structure(),
                new WasmGetLocal(targetVar),
                classInfoType.nextClassIndex(),
                new WasmGetGlobal(classInfoType.firstClassGlobal())
            ));
            converter.convert(new WasmSetGlobal(classInfoType.firstClassGlobal(), new WasmGetLocal(targetVar)));
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

            var converter = new WasmExpressionToInstructionConverter(function.getBody());
            var existing = new WasmStructGet(classInfoType.structure(), new WasmGetLocal(itemTypeVar),
                    classInfoType.arrayTypeIndex());
            var block = new WasmBlock(false);
            block.getBody().add(new WasmReturn(new WasmNullBranch(WasmNullCondition.NULL, existing, block)));
            converter.convert(block);

            converter.convert(new WasmSetLocal(resultVar, new WasmStructNewDefault(classInfoType.structure())));
            converter.convert(new WasmStructSet(classInfoType.structure(), new WasmGetLocal(itemTypeVar),
                    classInfoType.arrayTypeIndex(), new WasmGetLocal(resultVar)));
            converter.convert(new WasmCall(getFillArrayClassFunction(), new WasmGetLocal(resultVar),
                    new WasmGetLocal(itemTypeVar)));

            var supertypeFunction = supertypeGenerator.getIsArraySupertypeFunction();
            supertypeFunction.setReferenced(true);
            converter.convert(new WasmStructSet(classInfoType.structure(), new WasmGetLocal(resultVar),
                    classInfoType.supertypeFunctionIndex(), new WasmFunctionReference(supertypeFunction)));

            var cloneFunction = generateArrayCloneMethod(arrayType.structure, ValueType.object("java.lang.Object"));
            cloneFunction.setReferenced(true);
            converter.convert(new WasmStructSet(classInfoType.structure(), new WasmGetLocal(resultVar),
                    classInfoType.cloneFunctionIndex(), new WasmFunctionReference(cloneFunction)));

            converter.convert(new WasmSetLocal(vtVar, new WasmStructNewDefault(
                    standardClasses.objectClass().getVirtualTableStructure())));
            fillArrayVirtualTableMethods(converter, vtVar, resultVar);
            converter.convert(new WasmGetLocal(resultVar));
        });
        return function;
    }

    private void fillArrayVirtualTableMethods(WasmExpressionToInstructionConverter converter, WasmLocal vt,
            WasmLocal cls) {
        converter.convert(new WasmCall(systemFunctions.getFillObjectVirtualTableFunction(), new WasmGetLocal(vt),
                new WasmGetLocal(cls)));
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

        var converter = new WasmExpressionToInstructionConverter(function.getBody());
        converter.convert(new WasmStructSet(
                classInfoType.structure(),
                new WasmGetLocal(targetVar),
                classInfoType.vtableIndex(),
                new WasmGetGlobal(standardClasses.objectClass().getVirtualTablePointer())
        ));
        converter.convert(new WasmStructSet(
                classInfoType.structure(),
                new WasmGetLocal(targetVar),
                classInfoType.modifiersIndex(),
                new WasmInt32Constant(ModifiersInfo.FINAL | ModifiersInfo.PUBLIC)
        ));
        converter.convert(new WasmStructSet(
                classInfoType.structure(),
                new WasmGetLocal(targetVar),
                classInfoType.itemTypeIndex(),
                new WasmGetLocal(itemVar)
        ));
        converter.convert(new WasmStructSet(
                classInfoType.structure(),
                new WasmGetLocal(itemVar),
                classInfoType.arrayTypeIndex(),
                new WasmGetLocal(targetVar)
        ));
        converter.convert(new WasmStructSet(
                classInfoType.structure(),
                new WasmGetLocal(targetVar),
                classInfoType.tagIndex(),
                new WasmInt32Constant(0)
        ));
        if (classInfoType.parentIndex() >= 0) {
            converter.convert(new WasmStructSet(
                    classInfoType.structure(),
                    new WasmGetLocal(targetVar),
                    classInfoType.parentIndex(),
                    new WasmGetGlobal(standardClasses.objectClass().pointer)
            ));
        }
        if (classInfoType.arrayLengthIndex() >= 0) {
            var arrayClass = getClassInfo(ValueType.arrayOf(ValueType.object("java.lang.Object")));
            var lengthFunction = getArrayLengthFunction(arrayClass.structure);
            converter.convert(new WasmStructSet(
                    classInfoType.structure(),
                    new WasmGetLocal(targetVar),
                    classInfoType.arrayLengthIndex(),
                    new WasmFunctionReference(lengthFunction)
            ));
        }
        if (classInfoType.getItemIndex() >= 0) {
            converter.convert(new WasmStructSet(
                    classInfoType.structure(),
                    new WasmGetLocal(targetVar),
                    classInfoType.getItemIndex(),
                    new WasmFunctionReference(getArrayGetObjectFunction())
            ));
        }
        if (classInfoType.putItemIndex() >= 0) {
            converter.convert(new WasmStructSet(
                    classInfoType.structure(),
                    new WasmGetLocal(targetVar),
                    classInfoType.putItemIndex(),
                    new WasmFunctionReference(getArraySetObjectFunction())
            ));
        }
        if (classInfoType.newArrayFunctionIndex() >= 0) {
            var newArrayFunction = newArrayGenerator.getNewObjectArrayFunction();
            newArrayFunction.setReferenced(true);
            converter.convert(new WasmStructSet(
                    classInfoType.structure(),
                    new WasmGetLocal(targetVar),
                    classInfoType.newArrayFunctionIndex(),
                    new WasmFunctionReference(newArrayFunction)
            ));
        }
        if (classInfoType.copyArrayIndex() >= 0) {
            var copyFunction = getArrayCopyObjectFunction();
            converter.convert(new WasmStructSet(
                    classInfoType.structure(),
                    new WasmGetLocal(targetVar),
                    classInfoType.copyArrayIndex(),
                    new WasmFunctionReference(copyFunction)
            ));
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
                .map(field -> new WasmGetGlobal(getStaticFieldLocation(field.getReference())))
                .collect(Collectors.toList());

        var alloc = new WasmArrayNewFixed(classInfoStruct.enumConstantsType());
        alloc.getElements().addAll(fields);
        var converter = new WasmExpressionToInstructionConverter(function.getBody());
        converter.convert(alloc);

        return function;
    }

    private WasmFunction createNewInstanceFunction(String className, WasmGCClassInfo classInfo) {
        var objCls = standardClasses.objectClass();
        var function = new WasmFunction(functionTypes.of(objCls.getType()));
        function.setName(names.topLevel(className + "@newInstance"));
        var resultLocal = new WasmLocal(classInfo.getType(), "result");
        function.add(resultLocal);
        var converter = new WasmExpressionToInstructionConverter(function.getBody());
        converter.convert(new WasmSetLocal(resultLocal, new WasmStructNewDefault(classInfo.getStructure())));
        converter.convert(new WasmStructSet(objCls.getStructure(), new WasmGetLocal(resultLocal),
                WasmGCClassInfoProvider.VT_FIELD_OFFSET, new WasmGetGlobal(classInfo.getVirtualTablePointer())));
        converter.convert(new WasmGetLocal(resultLocal));
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

        var converter = new WasmExpressionToInstructionConverter(instantiator.getBody());

        converter.convert(new WasmSetLocal(localVar, new WasmCast(new WasmGetLocal(param),
                classInfo.getType())));
        var call = new WasmCall(functionProvider.forInstanceMethod(method.getReference()),
                new WasmGetLocal(localVar));
        converter.convert(call);

        if (asyncSplitMethods.contains(method.getReference())) {
            call.setSuspensionPoint(true);
            var transformation = new CoroutineTransformation(
                    functionTypes,
                    functionProvider,
                    this
            );
            transformation.transform(instantiator);
        }

        return instantiator;
    }

    private WasmExpression setClassField(WasmGCClassInfo classInfo, int fieldIndex, WasmExpression value) {
        return new WasmStructSet(
                reflectionTypes.classInfo().structure(),
                new WasmGetGlobal(classInfo.pointer),
                fieldIndex,
                value
        );
    }

}
