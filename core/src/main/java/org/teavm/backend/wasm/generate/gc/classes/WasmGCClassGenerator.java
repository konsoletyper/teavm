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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import org.teavm.backend.lowlevel.generate.NameProvider;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.gc.WasmGCInitializerContributor;
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
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmFunctionReference;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.backend.wasm.model.expression.WasmStructNewDefault;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.analysis.ClassInitializerInfo;
import org.teavm.model.analysis.ClassMetadataRequirements;
import org.teavm.model.classes.TagRegistry;
import org.teavm.model.classes.VirtualTable;
import org.teavm.model.classes.VirtualTableProvider;
import org.teavm.model.util.ReflectionUtil;

public class WasmGCClassGenerator implements WasmGCClassInfoProvider, WasmGCInitializerContributor {
    private static final MethodDescriptor CLINIT_METHOD_DESC = new MethodDescriptor("<clinit>", ValueType.VOID);
    private static final MethodDescriptor GET_CLASS_METHOD = new MethodDescriptor("getClass",
            ValueType.parse(Class.class));
    private static final FieldReference FAKE_CLASS_FIELD = new FieldReference(Object.class.getName(), "class");
    private static final FieldReference FAKE_MONITOR_FIELD = new FieldReference(Object.class.getName(), "monitor");

    private final WasmModule module;
    private ClassReaderSource classSource;
    private WasmFunctionTypes functionTypes;
    private TagRegistry tagRegistry;
    private ClassMetadataRequirements metadataRequirements;
    private VirtualTableProvider virtualTables;
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
    private final NameProvider names;
    private List<WasmExpression> initializerFunctionStatements = new ArrayList<>();
    private WasmFunction createPrimitiveClassFunction;
    private WasmFunction createArrayClassFunction;
    private final WasmGCSupertypeFunctionGenerator supertypeGenerator;
    private final WasmGCNewArrayFunctionGenerator newArrayGenerator;

    private int classTagOffset;
    private int classFlagsOffset;
    private int classNameOffset;
    private int classParentOffset;
    private int classArrayOffset;
    private int classArrayItemOffset;
    private int classNewArrayOffset;
    private int classSupertypeFunctionOffset;
    private int virtualTableFieldOffset;

    public WasmGCClassGenerator(WasmModule module, ClassReaderSource classSource,
            WasmFunctionTypes functionTypes, TagRegistry tagRegistry,
            ClassMetadataRequirements metadataRequirements, VirtualTableProvider virtualTables,
            BaseWasmFunctionRepository functionProvider, NameProvider names,
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
        strings = new WasmGCStringPool(standardClasses, module, functionProvider);
        supertypeGenerator = new WasmGCSupertypeFunctionGenerator(module, this, names, tagRegistry, functionTypes);
        newArrayGenerator = new WasmGCNewArrayFunctionGenerator(module, functionTypes, this);
        typeMapper = new WasmGCTypeMapper(this, functionTypes, module);
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
        }
        return true;
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
            if (classInfo.initializerPointer != null) {
                var className = ((ValueType.Object) classInfo.getValueType()).getClassName();
                var initFunction = functionProvider.forStaticMethod(new MethodReference(className,
                        CLINIT_METHOD_DESC));
                initFunction.setReferenced(true);
                function.getBody().add(new WasmSetGlobal(classInfo.initializerPointer,
                        new WasmFunctionReference(initFunction)));
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
            classInfoQueue.add(classInfo);
            classInfoMap.put(type, classInfo);
            VirtualTable virtualTable = null;
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
                    classInfo.structure = new WasmStructure(name != null ? names.forClass(name) : null);
                    module.types.add(classInfo.structure);
                }
                if (name != null) {
                    if (!isInterface) {
                        virtualTable = virtualTables.lookup(name);
                    }
                    if (classReader != null && classReader.getParent() != null && !isInterface) {
                        classInfo.structure.setSupertype(getClassInfo(classReader.getParent()).structure);
                    }
                } else {
                    classInfo.structure.setSupertype(standardClasses.objectClass().structure);
                }
                if (!isInterface) {
                    fillFields(classInfo, type);
                }
            }
            var pointerName = names.forClassInstance(type);
            classInfo.hasOwnVirtualTable = virtualTable != null && virtualTable.hasValidEntries();
            var classStructure = classInfo.hasOwnVirtualTable
                    ? initRegularClassStructure(((ValueType.Object) type).getClassName())
                    : standardClasses.classClass().getStructure();
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
        return classTagOffset;
    }

    @Override
    public int getClassArrayItemOffset() {
        return classArrayItemOffset;
    }

    @Override
    public int getClassSupertypeFunctionOffset() {
        return classSupertypeFunctionOffset;
    }

    @Override
    public int getClassNameOffset() {
        return classNameOffset;
    }

    @Override
    public int getNewArrayFunctionOffset() {
        return classNewArrayOffset;
    }

    @Override
    public int getVirtualMethodsOffset() {
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

    private void initRegularClass(WasmGCClassInfo classInfo, VirtualTable virtualTable, WasmStructure classStructure,
            String name) {
        var cls = classSource.get(name);
        if (classInitializerInfo.isDynamicInitializer(name)) {
            if (cls != null && cls.getMethod(CLINIT_METHOD_DESC) != null) {
                var clinitType = functionTypes.of(null);
                classInfo.initializerPointer = new WasmGlobal(null, clinitType.getReference(),
                        new WasmNullConstant(clinitType.getReference()));
                module.globals.add(classInfo.initializerPointer);
            }
        }
        classInfo.initializer = target -> {
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
            if (virtualTable != null && virtualTable.hasValidEntries()) {
                fillVirtualTableMethods(target, classStructure, classInfo.pointer, virtualTable, virtualTable,
                        new HashSet<>());
            }
        };
    }

    private void fillVirtualTableMethods(List<WasmExpression> target, WasmStructure structure, WasmGlobal global,
            VirtualTable virtualTable, VirtualTable original, Set<MethodDescriptor> filled) {
        if (virtualTable.getParent() != null) {
            fillVirtualTableMethods(target, structure, global, virtualTable.getParent(), original, filled);
        }
        for (var method : virtualTable.getMethods()) {
            var entry = original.getEntry(method);
            if (entry != null && entry.getImplementor() != null && filled.add(method)
                    && !method.equals(GET_CLASS_METHOD)) {
                var fieldIndex = virtualTableFieldOffset + entry.getIndex();
                var expectedType = (WasmType.CompositeReference) structure.getFields().get(fieldIndex)
                        .getUnpackedType();
                var expectedFunctionType = (WasmFunctionType) expectedType.composite;
                var function = functionProvider.forInstanceMethod(entry.getImplementor());
                if (!virtualTable.getClassName().equals(entry.getImplementor().getClassName())
                        || expectedFunctionType != function.getType()) {
                    var wrapperFunction = new WasmFunction(expectedFunctionType);
                    module.functions.add(wrapperFunction);
                    var call = new WasmCall(function);
                    var instanceParam = new WasmLocal(getClassInfo(virtualTable.getClassName()).getType());
                    wrapperFunction.add(instanceParam);
                    var castTarget = getClassInfo(entry.getImplementor().getClassName()).getType();
                    call.getArguments().add(new WasmCast(new WasmGetLocal(instanceParam), castTarget));
                    var params = new WasmLocal[method.parameterCount()];
                    for (var i = 0; i < method.parameterCount(); ++i) {
                        params[i] = new WasmLocal(typeMapper.mapType(method.parameterType(i)));
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
    }

    private WasmStructure initRegularClassStructure(String className) {
        var virtualTable = virtualTables.lookup(className);
        var structure = new WasmStructure(names.forClassClass(className));
        structure.setSupertype(standardClasses.classClass().getStructure());
        module.types.add(structure);
        addSystemFields(structure.getFields());
        fillSimpleClassFields(structure.getFields(), "java.lang.Class");
        addVirtualTableFields(structure, virtualTable);
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

    private void addVirtualTableFields(WasmStructure structure, VirtualTable virtualTable) {
        if (virtualTable.getParent() != null) {
            addVirtualTableFields(structure, virtualTable.getParent());
        }
        for (var methodDesc : virtualTable.getMethods()) {
            if (methodDesc == null) {
                structure.getFields().add(new WasmField(WasmType.Reference.FUNC.asStorage()));
            } else {
                var originalVirtualTable = virtualTable.findMethodContainer(methodDesc);
                var functionType = typeMapper.getFunctionType(originalVirtualTable.getClassName(), methodDesc, false);
                var field = new WasmField(functionType.getReference().asStorage());
                field.setName(names.forVirtualMethod(methodDesc));
                structure.getFields().add(field);
            }
        }
    }

    private void initArrayClass(WasmGCClassInfo classInfo, ValueType.Array type) {
        classInfo.initializer = target -> {
            var itemTypeInfo = getClassInfo(type.getItemType());
            target.add(new WasmCall(
                    getCreateArrayClassFunction(),
                    new WasmGetGlobal(classInfo.pointer),
                    new WasmGetGlobal(itemTypeInfo.pointer)
            ));
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
        getClassInfo(fieldRef.getClassName());
        var result = fieldIndexes.getOrDefault(fieldRef, -1);
        if (result < 0) {
            throw new IllegalStateException("Can't get offset of field " + fieldRef);
        }
        return result;
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
        var global = new WasmGlobal(names.forStaticField(fieldRef), type, WasmExpression.defaultValueOfType(type));
        module.globals.add(global);
        
        return global;
    }

    private void fillFields(WasmGCClassInfo classInfo, ValueType type) {
        var fields = classInfo.structure.getFields();
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
            wasmElementType = WasmType.Reference.STRUCT.asStorage();
        }
        var wasmArray = new WasmArray(null, wasmElementType);
        module.types.add(wasmArray);
        classInfo.structure.getFields().add(new WasmField(wasmArray.getReference().asStorage(), "data"));
        classInfo.array = wasmArray;
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
        function.setName("teavm_fill_primitive_class");
        module.functions.add(function);

        var targetVar = new WasmLocal(standardClasses.classClass().getType(), "target");
        var nameVar = new WasmLocal(standardClasses.stringClass().getType(), "name");
        var kindVar = new WasmLocal(WasmType.INT32, "kind");
        function.add(targetVar);
        function.add(nameVar);
        function.add(kindVar);

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
        function.setName("teavm_fill_array_class");

        var targetVar = new WasmLocal(standardClasses.classClass().getType(), "target");
        var itemVar = new WasmLocal(standardClasses.classClass().getType(), "item");
        function.add(targetVar);
        function.add(itemVar);

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
