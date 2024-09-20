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
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.gc.vtable.WasmGCVirtualTable;
import org.teavm.backend.wasm.gc.vtable.WasmGCVirtualTableEntry;
import org.teavm.backend.wasm.gc.vtable.WasmGCVirtualTableProvider;
import org.teavm.backend.wasm.generate.TemporaryVariablePool;
import org.teavm.backend.wasm.generate.gc.WasmGCInitializerContributor;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
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
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmSignedType;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructNew;
import org.teavm.backend.wasm.model.expression.WasmStructNewDefault;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.backend.wasm.runtime.WasmGCSupport;
import org.teavm.dependency.DependencyInfo;
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
    private static final ValueType OBJECT_TYPE = ValueType.parse(Object.class);

    private final WasmModule module;
    private ClassReaderSource classSource;
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

    public final WasmGCStringPool strings;
    public final WasmGCStandardClasses standardClasses;
    public final WasmGCTypeMapper typeMapper;
    private final WasmGCNameProvider names;
    private List<WasmExpression> initializerFunctionStatements = new ArrayList<>();
    private WasmFunction createPrimitiveClassFunction;
    private WasmFunction createArrayClassFunction;
    private WasmFunction fillRegularClassFunction;
    private final WasmGCSupertypeFunctionGenerator supertypeGenerator;
    private final WasmGCNewArrayFunctionGenerator newArrayGenerator;
    private String arrayDataFieldName;

    private int classTagOffset;
    private int classFlagsOffset = -1;
    private int classNameOffset;
    private int classSimpleNameOffset;
    private int classCanonicalNameOffset;
    private int classParentOffset;
    private int classArrayOffset;
    private int classArrayItemOffset;
    private int classNewArrayOffset;
    private int classSupertypeFunctionOffset;
    private int classEnclosingClassOffset;
    private int classDeclaringClassOffset;
    private int virtualTableFieldOffset;
    private int enumConstantsFunctionOffset;
    private int arrayLengthOffset = -1;
    private int arrayGetOffset = -1;
    private int cloneOffset = -1;
    private int servicesOffset = -1;
    private WasmStructure arrayVirtualTableStruct;
    private WasmFunction arrayGetObjectFunction;
    private WasmFunction arrayLengthObjectFunction;
    private WasmFunctionType arrayGetType;
    private WasmFunctionType arrayLengthType;
    private List<WasmStructure> nonInitializedStructures = new ArrayList<>();
    private WasmArray objectArrayType;
    private boolean hasLoadServices;

    public WasmGCClassGenerator(WasmModule module, ClassReaderSource classSource,
            ClassHierarchy hierarchy, DependencyInfo dependencyInfo,
            WasmFunctionTypes functionTypes, TagRegistry tagRegistry,
            ClassMetadataRequirements metadataRequirements, WasmGCVirtualTableProvider virtualTables,
            BaseWasmFunctionRepository functionProvider, WasmGCNameProvider names,
            ClassInitializerInfo classInitializerInfo,
            List<WasmGCCustomTypeMapperFactory> customTypeMapperFactories) {
        this.module = module;
        this.classSource = classSource;
        this.hierarchy = hierarchy;
        this.functionTypes = functionTypes;
        this.tagRegistry = tagRegistry;
        this.metadataRequirements = metadataRequirements;
        this.virtualTables = virtualTables;
        this.functionProvider = functionProvider;
        this.names = names;
        this.classInitializerInfo = classInitializerInfo;
        standardClasses = new WasmGCStandardClasses(this);
        strings = new WasmGCStringPool(standardClasses, module, functionProvider, names, functionTypes);
        supertypeGenerator = new WasmGCSupertypeFunctionGenerator(module, this, names, tagRegistry, functionTypes);
        newArrayGenerator = new WasmGCNewArrayFunctionGenerator(module, functionTypes, this, names, queue);
        typeMapper = new WasmGCTypeMapper(classSource, this, functionTypes, module);
        var customTypeMapperFactoryContext = customTypeMapperFactoryContext();
        typeMapper.setCustomTypeMappers(customTypeMapperFactories.stream()
                .map(factory -> factory.createTypeMapper(customTypeMapperFactoryContext))
                .collect(Collectors.toList()));

        var loadServicesMethod = dependencyInfo.getMethod(new MethodReference(ServiceLoader.class, "loadServices",
                Class.class, Object[].class));
        if (loadServicesMethod != null && loadServicesMethod.isUsed()) {
            hasLoadServices = true;
        }
    }

    private WasmGCCustomTypeMapperFactoryContext customTypeMapperFactoryContext() {
        return new WasmGCCustomTypeMapperFactoryContext() {
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
    }

    @Override
    public WasmGCClassInfo getClassInfo(ValueType type) {
        var classInfo = classInfoMap.get(type);
        if (classInfo == null) {
            classInfo = new WasmGCClassInfo(type);
            var finalClassInfo = classInfo;
            queue.add(() -> {
                finalClassInfo.initializer.accept(initializerFunctionStatements);
                finalClassInfo.initializer = null;
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
                        var structName = names.topLevel(names.suggestForType(type));
                        classInfo.structure = new WasmStructure(structName,
                                fields -> fillFields(finalClassInfo, fields, type));
                        classInfo.structure.setNominal(true);
                        module.types.add(classInfo.structure);
                        nonInitializedStructures.add(classInfo.structure);
                    }
                }
                if (name != null) {
                    if (!isInterface) {
                        virtualTable = virtualTables.lookup(name);
                    }
                    if (classReader != null && classReader.getParent() != null && !isInterface) {
                        classInfo.structure.setSupertype(getClassInfo(classReader.getParent()).structure);
                    }
                } else {
                    virtualTable = virtualTables.lookup("java.lang.Object");
                    classInfo.structure.setSupertype(standardClasses.objectClass().structure);
                }
            }
            var pointerName = names.topLevel(names.suggestForType(type) + "@class");
            if (virtualTable != null) {
                if (type instanceof ValueType.Object) {
                    if (virtualTable.isUsed()) {
                        initRegularClassStructure(classInfo, ((ValueType.Object) type).getClassName());
                    } else {
                        var usedVt = virtualTable.getFirstUsed();
                        if (usedVt != null) {
                            classInfo.virtualTableStructure = getClassInfo(usedVt.getClassName())
                                    .virtualTableStructure;
                        } else {
                            classInfo.virtualTableStructure = standardClasses.classClass().getStructure();
                        }
                    }
                } else {
                    classInfo.virtualTableStructure = getArrayVirtualTableStructure();
                }
            } else {
                classInfo.virtualTableStructure = standardClasses.classClass().getStructure();
            }

            var classStructure = classInfo.virtualTableStructure;
            classInfo.pointer = new WasmGlobal(pointerName, classStructure.getNonNullReference(),
                    new WasmStructNewDefault(classStructure));
            classInfo.pointer.setImmutable(true);
            module.globals.add(classInfo.pointer);
            if (type instanceof ValueType.Primitive) {
                initPrimitiveClass(classInfo, (ValueType.Primitive) type);
            } else if (type instanceof ValueType.Void) {
                initVoidClass(classInfo);
            } else if (type instanceof ValueType.Array) {
                initArrayClass(classInfo, (ValueType.Array) type);
            } else if (type instanceof ValueType.Object) {
                initRegularClass(classInfo, virtualTable, classStructure, ((ValueType.Object) type).getClassName());
            }
            var req = metadataRequirements.getInfo(type);
            if (req != null) {
                if (req.newArray()) {
                    classInfo.initArrayFunction = getArrayConstructor(classInfo.getValueType(), 1);
                    classInfo.initArrayFunction.setReferenced(true);
                }
                if (req.isAssignable()) {
                    var supertypeFunction = supertypeGenerator.getIsSupertypeFunction(classInfo.getValueType());
                    supertypeFunction.setReferenced(true);
                    classInfo.supertypeFunction = supertypeFunction;
                }
            }
        }
        return classInfo;
    }

    @Override
    public WasmFunction getArrayConstructor(ValueType type, int depth) {
        var arrayInfo = getClassInfo(type);
        if (arrayInfo.newArrayFunctions == null) {
            arrayInfo.newArrayFunctions = new ArrayList<>();
        }
        if (depth >= arrayInfo.newArrayFunctions.size()) {
            arrayInfo.newArrayFunctions.addAll(Collections.nCopies(
                    depth - arrayInfo.newArrayFunctions.size() + 1, null));
        }
        var function = arrayInfo.newArrayFunctions.get(depth);
        if (function == null) {
            function = depth == 1
                    ? newArrayGenerator.generateNewArrayFunction(type)
                    : newArrayGenerator.generateNewMultiArrayFunction(type, depth);
            arrayInfo.newArrayFunctions.set(depth, function);
        }
        return function;
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
    public int getNewArrayFunctionOffset() {
        standardClasses.classClass().getStructure().init();
        return classNewArrayOffset;
    }

    @Override
    public int getVirtualMethodsOffset() {
        standardClasses.classClass().getStructure().init();
        return virtualTableFieldOffset;
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

    private void initRegularClass(WasmGCClassInfo classInfo, WasmGCVirtualTable virtualTable,
            WasmStructure classStructure, String name) {
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
            }
            if (virtualTable != null && virtualTable.isConcrete()) {
                fillVirtualTableMethods(target, classStructure, classInfo.pointer, virtualTable);
            }
            if (classInfo.initializerPointer != null) {
                var initFunction = functionProvider.forStaticMethod(new MethodReference(name, CLINIT_METHOD_DESC));
                initFunction.setReferenced(true);
                classInfo.initializerPointer.setInitialValue(new WasmFunctionReference(initFunction));
            }
        };
    }

    private int getFlags(ClassReader cls) {
        var flags = 0;
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

    private void fillVirtualTableMethods(List<WasmExpression> target, WasmStructure structure, WasmGlobal global,
            WasmGCVirtualTable virtualTable) {
        var usedVt = virtualTable.getFirstUsed();
        if (usedVt == null) {
            return;
        }
        for (var i = 0; i < usedVt.getEntries().size(); ++i) {
            var entry = virtualTable.getEntries().get(i);
            fillVirtualTableEntry(target, global, structure, virtualTable, entry);
        }
    }

    private void fillArrayVirtualTableMethods(ValueType type, List<WasmExpression> target, WasmGlobal global,
            WasmStructure objectStructure) {
        var virtualTable = virtualTables.lookup("java.lang.Object");
        var structure = getArrayVirtualTableStructure();
        var itemType = ((ValueType.Array) type).getItemType();

        for (var entry : virtualTable.getEntries()) {
            fillVirtualTableEntry(target, global, structure, virtualTable, entry);
        }

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
        var function = new WasmFunction(functionTypes.of(WasmType.INT32, standardClasses.objectClass().getType()));
        function.setReferenced(true);
        function.setName(names.topLevel("Array<*>::length"));
        module.functions.add(function);

        var objectLocal = new WasmLocal(standardClasses.objectClass().getType(), "object");
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
            var objectLocal = new WasmLocal(standardClasses.objectClass().getType(), "object");
            var indexLocal = new WasmLocal(WasmType.INT32, "index");
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
        var objectLocal = new WasmLocal(standardClasses.objectClass().getType(), "object");
        var indexLocal = new WasmLocal(WasmType.INT32, "index");
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

    private WasmFunctionType getArrayGetType() {
        if (arrayGetType == null) {
            arrayGetType = functionTypes.of(standardClasses.objectClass().getType(),
                    standardClasses.objectClass().getType(), WasmType.INT32);
        }
        return arrayGetType;
    }

    private WasmFunctionType getArrayLengthType() {
        if (arrayLengthType == null) {
            arrayLengthType = functionTypes.of(WasmType.INT32, standardClasses.objectClass().getType());
        }
        return arrayLengthType;
    }

    private void fillVirtualTableEntry(List<WasmExpression> target, WasmGlobal global,
            WasmStructure structure, WasmGCVirtualTable virtualTable, WasmGCVirtualTableEntry entry) {
        var implementor = virtualTable.implementor(entry);
        if (implementor != null && !entry.getMethod().equals(GET_CLASS_METHOD)) {
            var fieldIndex = virtualTableFieldOffset + entry.getIndex();
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
            target.add(new WasmStructSet(structure, new WasmGetGlobal(global), fieldIndex, ref));
        }
    }

    private WasmFunction generateArrayCloneMethod(WasmStructure objectStructure, ValueType itemType) {
        var arrayTypeRef = (WasmType.CompositeReference) objectStructure.getFields().get(
                WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET).getUnpackedType();
        var arrayType = (WasmArray) arrayTypeRef.composite;

        var type = typeMapper.getFunctionType(standardClasses.objectClass().getType(), CLONE_METHOD_DESC, false);
        var function = new WasmFunction(type);
        function.setName(names.topLevel("Array<" + names.suggestForType(itemType) + ">::clone"));
        module.functions.add(function);
        var instanceLocal = new WasmLocal(standardClasses.objectClass().getType(), "instance");
        var originalLocal = new WasmLocal(objectStructure.getReference(), "original");
        var resultLocal = new WasmLocal(objectStructure.getReference(), "result");
        var originalDataLocal = new WasmLocal(arrayType.getReference(), "originalData");
        var dataCopyLocal = new WasmLocal(arrayType.getReference(), "resultData");
        function.add(instanceLocal);
        function.add(originalLocal);
        function.add(resultLocal);
        function.add(originalDataLocal);
        function.add(dataCopyLocal);

        function.getBody().add(new WasmSetLocal(originalLocal,
                new WasmCast(new WasmGetLocal(instanceLocal), objectStructure.getNonNullReference())));
        function.getBody().add(new WasmSetLocal(resultLocal, new WasmStructNewDefault(objectStructure)));

        var classValue = new WasmStructGet(objectStructure, new WasmGetLocal(originalLocal),
                WasmGCClassInfoProvider.CLASS_FIELD_OFFSET);
        function.getBody().add(new WasmStructSet(objectStructure, new WasmGetLocal(resultLocal),
                WasmGCClassInfoProvider.CLASS_FIELD_OFFSET, classValue));

        var originalDataValue = new WasmStructGet(objectStructure, new WasmGetLocal(originalLocal),
                WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
        function.getBody().add(new WasmSetLocal(originalDataLocal, originalDataValue));
        var originalLength = new WasmArrayLength(new WasmGetLocal(originalDataLocal));
        function.getBody().add(new WasmSetLocal(dataCopyLocal, new WasmArrayNewDefault(arrayType, originalLength)));
        function.getBody().add(new WasmStructSet(objectStructure, new WasmGetLocal(resultLocal),
                WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET, new WasmGetLocal(dataCopyLocal)));

        function.getBody().add(new WasmArrayCopy(arrayType, new WasmGetLocal(dataCopyLocal),
                new WasmInt32Constant(0), arrayType, new WasmGetLocal(originalDataLocal),
                new WasmInt32Constant(0), new WasmArrayLength(new WasmGetLocal(originalDataLocal))));

        function.getBody().add(new WasmGetLocal(resultLocal));

        return function;
    }

    private void initRegularClassStructure(WasmGCClassInfo classInfo, String className) {
        var virtualTable = virtualTables.lookup(className);
        var wasmName = names.topLevel("Class<" + names.suggestForClass(className) + ">");
        var structure = new WasmStructure(wasmName, fields -> {
            addSystemFields(fields);
            fillSimpleClassFields(fields, "java.lang.Class");
            addVirtualTableFields(fields, virtualTable);
        });
        classInfo.virtualTableStructure = structure;
        nonInitializedStructures.add(structure);
        var usedParent = virtualTable.getUsedParent();
        var supertype = usedParent != null
                ? getClassInfo(usedParent.getClassName()).getVirtualTableStructure()
                : standardClasses.classClass().getStructure();
        structure.setSupertype(supertype);
        module.types.add(structure);
    }

    private void addSystemFields(List<WasmField> fields) {
        var classField = new WasmField(standardClasses.classClass().getType().asStorage());
        classField.setName(names.forMemberField(FAKE_CLASS_FIELD));
        fields.add(classField);
        var monitorField = new WasmField(WasmType.Reference.EQ.asStorage());
        monitorField.setName(names.forMemberField(FAKE_MONITOR_FIELD));
        fields.add(monitorField);
    }

    private void addVirtualTableFields(List<WasmField> fields, WasmGCVirtualTable virtualTable) {
        for (var entry : virtualTable.getEntries()) {
            var functionType = typeMapper.getFunctionType(entry.getOrigin().getClassName(), entry.getMethod(), false);
            var field = new WasmField(functionType.getReference().asStorage());
            field.setName(names.forVirtualMethod(entry.getMethod()));
            fields.add(field);
        }
    }

    @Override
    public WasmStructure getArrayVirtualTableStructure() {
        if (arrayVirtualTableStruct == null) {
            var wasmName = names.topLevel("Class<Array<*>>");
            arrayVirtualTableStruct = new WasmStructure(wasmName, fields -> {
                addSystemFields(fields);
                fillSimpleClassFields(fields, "java.lang.Class");
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
    public int getEnumConstantsFunctionOffset() {
        return enumConstantsFunctionOffset;
    }

    private void initArrayClass(WasmGCClassInfo classInfo, ValueType.Array type) {
        classInfo.initializer = target -> {
            var itemTypeInfo = getClassInfo(type.getItemType());
            target.add(new WasmCall(
                    getCreateArrayClassFunction(),
                    new WasmGetGlobal(classInfo.pointer),
                    new WasmGetGlobal(itemTypeInfo.pointer)
            ));
            fillArrayVirtualTableMethods(classInfo.getValueType(), target, classInfo.pointer, classInfo.structure);
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
        getClassInfo(fieldRef.getClassName()).structure.init();
        return fieldIndexes.getOrDefault(fieldRef, -1);
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
        if (className.equals("java.lang.Class")) {
            var cls = classSource.get("java.lang.Class");
            classFlagsOffset = fields.size();
            fields.add(createClassField(WasmType.INT32.asStorage(), "flags"));
            classTagOffset = fields.size();
            fields.add(createClassField(WasmType.INT32.asStorage(), "id"));
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
            if (metadataRequirements.hasArrayNewInstance()) {
                classNewArrayOffset = fields.size();
                fields.add(createClassField(newArrayGenerator.getNewArrayFunctionType().getReference().asStorage(),
                        "createArrayInstance"));
            }
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
            virtualTableFieldOffset = fields.size();
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
            wasmElementType = standardClasses.objectClass().getType().asStorage();
            wasmArray = objectArrayType;
            if (wasmArray == null) {
                var wasmArrayName = names.topLevel(names.suggestForType(ValueType.arrayOf(
                        ValueType.object("java.lang.Object"))) + "$Data");
                wasmArray = new WasmArray(wasmArrayName, wasmElementType);
                module.types.add(wasmArray);
                objectArrayType = wasmArray;
            }
        }

        classInfo.structure.getFields().add(new WasmField(wasmArray.getReference().asStorage(),
                arrayDataFieldName()));
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
                CLASS_FIELD_OFFSET,
                new WasmGetGlobal(standardClasses.classClass().pointer)
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
                CLASS_FIELD_OFFSET,
                new WasmGetGlobal(standardClasses.classClass().pointer)
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

        return function;
    }

    private WasmFunction getCreateArrayClassFunction() {
        if (createArrayClassFunction == null) {
            createArrayClassFunction = createCreateArrayClassFunction();
        }
        return createArrayClassFunction;
    }

    private WasmFunction createCreateArrayClassFunction() {
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
                CLASS_FIELD_OFFSET,
                new WasmGetGlobal(standardClasses.classClass().pointer)
        ));
        function.getBody().add(new WasmStructSet(
                standardClasses.classClass().getStructure(),
                new WasmGetLocal(targetVar),
                classFlagsOffset,
                new WasmInt32Constant(WasmGCClassFlags.FINAL)
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
        function.getBody().add(new WasmStructSet(
                standardClasses.classClass().getStructure(),
                new WasmGetLocal(targetVar),
                classParentOffset,
                new WasmGetGlobal(standardClasses.objectClass().pointer)
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

        var tempVars = new TemporaryVariablePool(function);
        var util = new WasmGCGenerationUtil(this, tempVars);
        var local = tempVars.acquire(enumArrayStruct.getReference());
        var block = new WasmBlock(false);
        block.setType(enumArrayStruct.getReference());
        util.allocateArrayWithElements(ValueType.parse(Enum.class), () -> fields, null, null, block.getBody());
        function.getBody().add(block);
        tempVars.release(local);

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
