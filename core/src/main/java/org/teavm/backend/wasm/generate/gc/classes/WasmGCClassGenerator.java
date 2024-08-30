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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.gc.vtable.WasmGCVirtualTable;
import org.teavm.backend.wasm.gc.vtable.WasmGCVirtualTableEntry;
import org.teavm.backend.wasm.gc.vtable.WasmGCVirtualTableProvider;
import org.teavm.backend.wasm.generate.gc.WasmGCInitializerContributor;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
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
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructNewDefault;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
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
    private WasmFunctionTypes functionTypes;
    private TagRegistry tagRegistry;
    private ClassMetadataRequirements metadataRequirements;
    private WasmGCVirtualTableProvider virtualTables;
    private BaseWasmFunctionRepository functionProvider;
    private Map<ValueType, WasmGCClassInfo> classInfoMap = new LinkedHashMap<>();
    private Queue<WasmGCClassInfo> classInfoQueue = new ArrayDeque<>();
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
    private final WasmGCSupertypeFunctionGenerator supertypeGenerator;
    private final WasmGCNewArrayFunctionGenerator newArrayGenerator;
    private String arrayDataFieldName;

    private int classTagOffset;
    private int classFlagsOffset;
    private int classNameOffset;
    private int classParentOffset;
    private int classArrayOffset;
    private int classArrayItemOffset;
    private int classNewArrayOffset;
    private int classSupertypeFunctionOffset;
    private int virtualTableFieldOffset;
    private int arrayLengthOffset = -1;
    private int arrayGetOffset = -1;
    private WasmStructure arrayVirtualTableStruct;
    private WasmFunction arrayGetObjectFunction;
    private WasmFunction arrayLengthObjectFunction;
    private WasmFunctionType arrayGetType;
    private WasmFunctionType arrayLengthType;
    private List<WasmStructure> nonInitializedStructures = new ArrayList<>();

    public WasmGCClassGenerator(WasmModule module, ClassReaderSource classSource,
            WasmFunctionTypes functionTypes, TagRegistry tagRegistry,
            ClassMetadataRequirements metadataRequirements, WasmGCVirtualTableProvider virtualTables,
            BaseWasmFunctionRepository functionProvider, WasmGCNameProvider names,
            ClassInitializerInfo classInitializerInfo) {
        this.module = module;
        this.classSource = classSource;
        this.functionTypes = functionTypes;
        this.tagRegistry = tagRegistry;
        this.metadataRequirements = metadataRequirements;
        this.virtualTables = virtualTables;
        this.functionProvider = functionProvider;
        this.names = names;
        this.classInitializerInfo = classInitializerInfo;
        standardClasses = new WasmGCStandardClasses(this);
        strings = new WasmGCStringPool(standardClasses, module, functionProvider, names);
        supertypeGenerator = new WasmGCSupertypeFunctionGenerator(module, this, names, tagRegistry, functionTypes);
        newArrayGenerator = new WasmGCNewArrayFunctionGenerator(module, functionTypes, this, names);
        typeMapper = new WasmGCTypeMapper(classSource, this, functionTypes, module);
    }

    public WasmGCSupertypeFunctionProvider getSupertypeProvider() {
        return supertypeGenerator;
    }

    public boolean process() {
        if (classInfoQueue.isEmpty()) {
            return false;
        }
        while (!classInfoQueue.isEmpty()) {
            var classInfo = classInfoQueue.remove();
            classInfo.initializer.accept(initializerFunctionStatements);
            classInfo.initializer = null;
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
        fillVirtualTableSupertypes();
        for (var classInfo : classInfoMap.values()) {
            var classInstanceType = classInfo.virtualTableStructure != null
                    ? classInfo.virtualTableStructure
                    : standardClasses.classClass().getStructure();
            var newStruct = new WasmStructNewDefault(classInstanceType);
            function.getBody().add(new WasmSetGlobal(classInfo.pointer, newStruct));
        }
    }

    private void fillVirtualTableSupertypes() {
        for (var classInfo : classInfoMap.values()) {
            if (classInfo.virtualTableStructure != null && classInfo.getValueType() instanceof ValueType.Object
                    && classInfo.hasOwnVirtualTable) {
                var className = ((ValueType.Object) classInfo.getValueType()).getClassName();
                var candidate = findVirtualTableSupertype(className);
                if (candidate != null) {
                    classInfo.virtualTableStructure.setSupertype(candidate);
                }
            }
        }
    }

    private WasmStructure findVirtualTableSupertype(String className) {
        while (className != null) {
            var cls = classSource.get(className);
            if (cls == null) {
                break;
            }
            className = cls.getParent();
            if (className == null) {
                break;
            }
            var parentInfo = classInfoMap.get(ValueType.object(className));
            if (parentInfo != null && parentInfo.virtualTableStructure != null) {
                return parentInfo.virtualTableStructure;
            }
        }
        var classClass = classInfoMap.get(ValueType.object("java.lang.Class"));
        return classClass != null ? classClass.structure : null;
    }

    @Override
    public void contributeToInitializer(WasmFunction function) {
        var classClass = standardClasses.classClass();
        function.getBody().addAll(initializerFunctionStatements);
        initializerFunctionStatements.clear();
        for (var classInfo : classInfoMap.values()) {
            var req = metadataRequirements.getInfo(classInfo.getValueType());
            if (req != null) {
                if (req.isAssignable()) {
                    var supertypeFunction = supertypeGenerator.getIsSupertypeFunction(classInfo.getValueType());
                    supertypeFunction.setReferenced(true);
                    function.getBody().add(setClassField(classInfo, classSupertypeFunctionOffset,
                            new WasmFunctionReference(supertypeFunction)));
                }
                if (req.newArray()) {
                    var newArrayFunction = newArrayGenerator.generateNewArrayFunction(classInfo.getValueType());
                    newArrayFunction.setReferenced(true);
                    function.getBody().add(setClassField(classInfo, classNewArrayOffset,
                            new WasmFunctionReference(newArrayFunction)));
                }
            }
            function.getBody().add(setClassField(classInfo, CLASS_FIELD_OFFSET,
                    new WasmGetGlobal(classClass.pointer)));
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
            classInfoQueue.add(classInfo);
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
                    var finalClassInfo = classInfo;
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
            classInfo.hasOwnVirtualTable = virtualTable != null && !virtualTable.getEntries().isEmpty();
            WasmStructure classStructure;
            if (classInfo.hasOwnVirtualTable) {
                if (type instanceof ValueType.Object) {
                    classStructure = initRegularClassStructure(((ValueType.Object) type).getClassName());
                } else {
                    classStructure = getArrayVirtualTableStructure();
                }
            } else {
                classStructure = standardClasses.classClass().getStructure();
            }

            classInfo.virtualTableStructure = classStructure;
            classInfo.pointer = new WasmGlobal(pointerName, classStructure.getReference(),
                    new WasmNullConstant(classStructure.getReference()));
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
        }
        return classInfo;
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
    public int getClassSupertypeFunctionOffset() {
        standardClasses.classClass().getStructure().init();
        return classSupertypeFunctionOffset;
    }

    @Override
    public int getClassNameOffset() {
        standardClasses.classClass().getStructure().init();
        return classNameOffset;
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
            target.add(setClassField(classInfo, classTagOffset, new WasmInt32Constant(tag)));
            var metadataReq = metadataRequirements.getInfo(name);
            if (metadataReq.name()) {
                var namePtr = strings.getStringConstant(name).global;
                target.add(setClassField(classInfo, classNameOffset, new WasmGetGlobal(namePtr)));
            }
            if (cls != null) {
                if (metadataReq.simpleName() && cls.getSimpleName() != null) {
                    var namePtr = strings.getStringConstant(cls.getSimpleName()).global;
                    target.add(setClassField(classInfo, classNameOffset, new WasmGetGlobal(namePtr)));
                }
                if (cls.getParent() != null && metadataReq.superclass()) {
                    var parent = getClassInfo(cls.getParent());
                    target.add(setClassField(classInfo, classParentOffset, new WasmGetGlobal(parent.pointer)));
                }
            }
            if (virtualTable != null) {
                fillVirtualTableMethods(target, classStructure, classInfo.pointer, virtualTable);
            }
            if (classInfo.initializerPointer != null) {
                var initFunction = functionProvider.forStaticMethod(new MethodReference(name,
                        CLINIT_METHOD_DESC));
                initFunction.setReferenced(true);
                target.add(new WasmSetGlobal(classInfo.initializerPointer, new WasmFunctionReference(initFunction)));
            }
        };
    }

    private void fillVirtualTableMethods(List<WasmExpression> target, WasmStructure structure, WasmGlobal global,
            WasmGCVirtualTable virtualTable) {
        for (var entry : virtualTable.getEntries()) {
            fillVirtualTableEntry(target, global, structure, virtualTable, entry);
        }
    }

    private void fillArrayVirtualTableMethods(ValueType type, List<WasmExpression> target, WasmGlobal global,
            WasmStructure objectStructure) {
        var virtualTable = virtualTables.lookup("java.lang.Object");
        var structure = getArrayVirtualTableStructure();
        var itemType = ((ValueType.Array) type).getItemType();

        for (var entry : virtualTable.getEntries()) {
            if (entry.getMethod().getName().equals("clone")) {
                var function = generateArrayCloneMethod(objectStructure, itemType);
                function.setReferenced(true);
                var ref = new WasmFunctionReference(function);
                var fieldIndex = virtualTableFieldOffset + entry.getIndex();
                target.add(new WasmStructSet(structure, new WasmGetGlobal(global), fieldIndex, ref));
            } else {
                fillVirtualTableEntry(target, global, structure, virtualTable, entry);
            }
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

        var castObject = new WasmCast(new WasmGetLocal(objectLocal), objectStructure.getReference());
        var arrayField = new WasmStructGet(objectStructure, castObject, ARRAY_DATA_FIELD_OFFSET);
        var result = new WasmArrayLength(arrayField);
        function.getBody().add(new WasmReturn(result));
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

            var array = new WasmCast(new WasmGetLocal(objectLocal), arrayStruct.getReference());
            var arrayData = new WasmStructGet(arrayStruct, array, ARRAY_DATA_FIELD_OFFSET);
            var result = new WasmArrayGet(arrayDataType, arrayData, new WasmGetLocal(indexLocal));
            arrayGetObjectFunction.getBody().add(new WasmReturn(result));
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

        var array = new WasmCast(new WasmGetLocal(objectLocal), arrayStruct.getReference());
        var arrayData = new WasmStructGet(arrayStruct, array, ARRAY_DATA_FIELD_OFFSET);
        var result = new WasmArrayGet(arrayDataType, arrayData, new WasmGetLocal(indexLocal));
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
        var method = new MethodReference(wrapperType, "valueOf", primitiveType, wrapperType);
        var wrapFunction = functionProvider.forStaticMethod(method);
        var castResult = new WasmCall(wrapFunction, result);
        function.getBody().add(new WasmReturn(castResult));

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
                var castTarget = getClassInfo(implementor.getClassName()).getType();
                call.getArguments().add(new WasmCast(new WasmGetLocal(instanceParam), castTarget));
                var params = new WasmLocal[entry.getMethod().parameterCount()];
                for (var i = 0; i < entry.getMethod().parameterCount(); ++i) {
                    params[i] = new WasmLocal(typeMapper.mapType(entry.getMethod().parameterType(i)));
                    call.getArguments().add(new WasmGetLocal(params[i]));
                    wrapperFunction.add(params[i]);
                }
                wrapperFunction.getBody().add(new WasmReturn(call));
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
                new WasmCast(new WasmGetLocal(instanceLocal), objectStructure.getReference())));
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

    private WasmStructure initRegularClassStructure(String className) {
        var virtualTable = virtualTables.lookup(className);
        var wasmName = names.topLevel("Class<" + names.suggestForClass(className) + ">");
        var structure = new WasmStructure(wasmName, fields -> {
            addSystemFields(fields);
            fillSimpleClassFields(fields, "java.lang.Class");
            addVirtualTableFields(fields, virtualTable);
        });
        nonInitializedStructures.add(structure);
        structure.setSupertype(standardClasses.classClass().getStructure());
        module.types.add(structure);
        return structure;
    }

    private void addSystemFields(List<WasmField> fields) {
        var classField = new WasmField(standardClasses.classClass().getType().asStorage());
        classField.setName(names.forMemberField(FAKE_CLASS_FIELD));
        fields.add(classField);
        var monitorField = new WasmField(WasmType.Reference.ANY.asStorage());
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

    private void initArrayClass(WasmGCClassInfo classInfo, ValueType.Array type) {
        classInfo.initializer = target -> {
            var itemTypeInfo = getClassInfo(type.getItemType());
            target.add(new WasmCall(
                    getCreateArrayClassFunction(),
                    new WasmGetGlobal(classInfo.pointer),
                    new WasmGetGlobal(itemTypeInfo.pointer)
            ));
            fillArrayVirtualTableMethods(classInfo.getValueType(), target, classInfo.pointer, classInfo.structure);
        };
    }

    private WasmExpression fillPrimitiveClass(WasmGlobal global, String name, int kind) {
        var str = name != null
                ? new WasmGetGlobal(strings.getStringConstant(name).global)
                : new WasmNullConstant(standardClasses.stringClass().getType());
        return new WasmCall(
                getCreatePrimitiveClassFunction(),
                new WasmGetGlobal(global),
                str,
                new WasmInt32Constant(kind)
        );
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
        if (className.equals("java.lang.Class")) {
            classFlagsOffset = fields.size();
            fields.add(createClassField(WasmType.INT32.asStorage(), "lowerIndex"));
            classTagOffset = fields.size();
            fields.add(createClassField(WasmType.INT32.asStorage(), "upperIndex"));
            classParentOffset = fields.size();
            fields.add(createClassField(standardClasses.classClass().getType().asStorage(), "parent"));
            classArrayItemOffset = fields.size();
            fields.add(createClassField(standardClasses.classClass().getType().asStorage(), "arrayItem"));
            classArrayOffset = fields.size();
            fields.add(createClassField(standardClasses.classClass().getType().asStorage(), "array"));
            classSupertypeFunctionOffset = fields.size();
            fields.add(createClassField(supertypeGenerator.getFunctionType().getReference().asStorage(),
                    "isSupertype"));
            classNewArrayOffset = fields.size();
            fields.add(createClassField(newArrayGenerator.getNewArrayFunctionType().getReference().asStorage(),
                    "createArrayInstance"));
            classNameOffset = fields.size();
            fields.add(createClassField(standardClasses.stringClass().getType().asStorage(), "name"));
            virtualTableFieldOffset = fields.size();
        }
    }

    private WasmField createClassField(WasmStorageType type, String name) {
        return new WasmField(type, names.forMemberField(new FieldReference("java.lang.Class", name)));
    }

    private void fillArrayFields(WasmGCClassInfo classInfo, ValueType elementType) {
        WasmStorageType wasmElementType;
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
        } else {
            wasmElementType = standardClasses.objectClass().getType().asStorage();
        }
        var wasmArrayName = names.topLevel(names.suggestForType(classInfo.getValueType()) + "$Data");
        var wasmArray = new WasmArray(wasmArrayName, wasmElementType);
        module.types.add(wasmArray);
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
        var functionType = functionTypes.of(
                null,
                standardClasses.classClass().getType(),
                standardClasses.stringClass().getType(),
                WasmType.INT32
        );
        var function = new WasmFunction(functionType);
        function.setName(names.topLevel("teavm@fill_primitive_class"));
        module.functions.add(function);

        var targetVar = new WasmLocal(standardClasses.classClass().getType(), "target");
        var nameVar = new WasmLocal(standardClasses.stringClass().getType(), "name");
        var kindVar = new WasmLocal(WasmType.INT32, "kind");
        function.add(targetVar);
        function.add(nameVar);
        function.add(kindVar);

        standardClasses.classClass().getStructure().getFields().size();
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
        function.getBody().add(new WasmStructSet(
                standardClasses.classClass().getStructure(),
                new WasmGetLocal(targetVar),
                classNameOffset,
                new WasmGetLocal(nameVar)
        ));
        function.getBody().add(new WasmStructSet(
                standardClasses.classClass().getStructure(),
                new WasmGetLocal(targetVar),
                classTagOffset,
                new WasmInt32Constant(Integer.MAX_VALUE)
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
                new WasmGetGlobal(standardClasses.classClass().pointer)
        ));
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
