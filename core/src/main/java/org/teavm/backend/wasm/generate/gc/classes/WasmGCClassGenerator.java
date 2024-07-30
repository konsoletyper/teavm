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
import org.teavm.backend.lowlevel.generate.NameProvider;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.gc.WasmGCInitializerContributor;
import org.teavm.backend.wasm.generate.gc.strings.WasmGCStringPool;
import org.teavm.backend.wasm.model.WasmArray;
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
    private ObjectIntMap<MethodReference> methodIndexes = new ObjectIntHashMap<>();
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

    private int classTagOffset;
    private int classFlagsOffset;
    private int classNameOffset;
    private int classParentOffset;
    private int classArrayOffset;
    private int classArrayItemOffset;
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
        typeMapper = new WasmGCTypeMapper(this);
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
        for (var classInfo : classInfoMap.values()) {
            var newStruct = new WasmStructNewDefault(standardClasses.classClass().getStructure());
            function.getBody().add(new WasmSetGlobal(classInfo.pointer, newStruct));
        }
    }

    @Override
    public void contributeToInitializer(WasmFunction function) {
        var classClass = standardClasses.classClass();
        function.getBody().addAll(initializerFunctionStatements);
        initializerFunctionStatements.clear();
        for (var classInfo : classInfoMap.values()) {
            var supertypeFunction = supertypeGenerator.getIsSupertypeFunction(classInfo.getValueType());
            function.getBody().add(setClassField(classInfo, classSupertypeFunctionOffset,
                    new WasmFunctionReference(supertypeFunction)));
            function.getBody().add(setClassField(classInfo, CLASS_FIELD_OFFSET,
                    new WasmGetGlobal(classClass.pointer)));
            if (classInfo.initializerPointer != null) {
                var className = ((ValueType.Object) classInfo.getValueType()).getClassName();
                var initFunction = functionProvider.forStaticMethod(new MethodReference(className,
                        CLINIT_METHOD_DESC));
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
            if (!(type instanceof ValueType.Primitive)) {
                var name = type instanceof ValueType.Object
                        ? ((ValueType.Object) type).getClassName()
                        : null;
                classInfo.structure = new WasmStructure(name != null ? names.forClass(name) : null);
                module.types.add(classInfo.structure);
                fillFields(classInfo.structure.getFields(), type);
            }
            var pointerName = names.forClassInstance(type);
            var classStructure = type instanceof ValueType.Object
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
                initRegularClass(classInfo, classStructure, ((ValueType.Object) type).getClassName());
            }
        }
        return classInfo;
    }

    public int getClassTagOffset() {
        return classTagOffset;
    }

    public int getClassArrayItemOffset() {
        return classArrayItemOffset;
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
            target.add(fillPrimitiveClass(
                    classInfo.pointer,
                    ReflectionUtil.typeName(type.getKind()),
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

    private void initRegularClass(WasmGCClassInfo classInfo, WasmStructure classStructure, String name) {
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
            var metadataReg = metadataRequirements.getInfo(name);
            if (metadataReg.name()) {
                var namePtr = strings.getStringConstant(name).global;
                target.add(setClassField(classInfo, classNameOffset, new WasmGetGlobal(namePtr)));
            }
            if (cls != null) {
                if (metadataReg.simpleName() && cls.getSimpleName() != null) {
                    var namePtr = strings.getStringConstant(cls.getSimpleName()).global;
                    target.add(setClassField(classInfo, classNameOffset, new WasmGetGlobal(namePtr)));
                }
                if (cls.getParent() != null) {
                    var parent = getClassInfo(cls.getParent());
                    target.add(setClassField(classInfo, classParentOffset, new WasmGetGlobal(parent.pointer)));
                }
            }
            var virtualTable = virtualTables.lookup(name);
            fillVirtualTableMethods(target, classStructure, classInfo.pointer, virtualTable, virtualTableFieldOffset,
                    name);
        };
    }

    private int fillVirtualTableMethods(List<WasmExpression> target, WasmStructure structure, WasmGlobal global,
            VirtualTable virtualTable, int index, String origin) {
        if (virtualTable.getParent() != null) {
            index = fillVirtualTableMethods(target, structure, global, virtualTable.getParent(), index, origin);
        }
        for (var method : virtualTable.getMethods()) {
            var entry = virtualTable.getEntry(method);
            if (entry != null && entry.getImplementor() != null) {
                var function = functionProvider.forInstanceMethod(entry.getImplementor());
                if (!origin.equals(entry.getImplementor().getClassName())) {
                    var functionType = getFunctionType(virtualTable.getClassName(), method);
                    var wrapperFunction = new WasmFunction(functionType);
                    module.functions.add(wrapperFunction);
                    var call = new WasmCall(function);
                    var instanceParam = new WasmLocal(getClassInfo(virtualTable.getClassName()).getType());
                    wrapperFunction.getLocalVariables().add(instanceParam);
                    var castTarget = getClassInfo(entry.getImplementor().getClassName()).getType();
                    call.getArguments().add(new WasmCast(new WasmGetLocal(instanceParam), castTarget));
                    var params = new WasmLocal[method.parameterCount()];
                    for (var i = 0; i < method.parameterCount(); ++i) {
                        params[i] = new WasmLocal(typeMapper.mapType(method.parameterType(i)).asUnpackedType());
                        call.getArguments().add(new WasmGetLocal(params[i]));
                    }
                    wrapperFunction.getLocalVariables().addAll(List.of(params));
                }
                var ref = new WasmFunctionReference(function);
                target.add(new WasmStructSet(structure, new WasmGetGlobal(global), index, ref));
            }
        }
        return index;
    }

    private WasmStructure initRegularClassStructure(String className) {
        var virtualTable = virtualTables.lookup(className);
        var structure = new WasmStructure(names.forClassClass(className));
        module.types.add(structure);
        structure.getFields().add(standardClasses.classClass().getType().asStorage());
        structure.getFields().add(WasmType.Reference.ANY.asStorage());
        fillSimpleClassFields(structure.getFields(), "java.lang.Class");
        addVirtualTableFields(structure, virtualTable);
        return structure;
    }

    private void addVirtualTableFields(WasmStructure structure, VirtualTable virtualTable) {
        if (virtualTable.getParent() != null) {
            addVirtualTableFields(structure, virtualTable.getParent());
        }
        for (var methodDesc : virtualTable.getMethods()) {
            var functionType = getFunctionType(virtualTable.getClassName(), methodDesc);
            var methodRef = new MethodReference(virtualTable.getClassName(), methodDesc);
            methodIndexes.put(methodRef, structure.getFields().size());
            structure.getFields().add(functionType.getReference().asStorage());
        }
    }

    private WasmFunctionType getFunctionType(String className, MethodDescriptor methodDesc) {
        var returnType = typeMapper.mapType(methodDesc.getResultType()).asUnpackedType();
        var javaParamTypes = methodDesc.getParameterTypes();
        var paramTypes = new WasmType[javaParamTypes.length + 1];
        paramTypes[0] = getClassInfo(className).getType();
        for (var i = 0; i < javaParamTypes.length; ++i) {
            paramTypes[i + 1] = typeMapper.mapType(javaParamTypes[i]).asUnpackedType();
        }
        return functionTypes.of(returnType, paramTypes);
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
        return new WasmCall(
                getCreatePrimitiveClassFunction(),
                new WasmGetGlobal(global),
                new WasmGetGlobal(strings.getStringConstant(name).global),
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
        
        var type = typeMapper.mapType(javaType).asUnpackedType();
        var global = new WasmGlobal(names.forStaticField(fieldRef), type, WasmExpression.defaultValueOfType(type));
        module.globals.add(global);
        
        return global;
    }

    @Override
    public int getVirtualMethodIndex(MethodReference methodRef) {
        var result = methodIndexes.getOrDefault(methodRef, -1);
        if (result < 0) {
            throw new IllegalStateException("Can't get offset of method " + methodRef);
        }
        return result;
    }

    private void fillFields(List<WasmStorageType> fields, ValueType type) {
        fields.add(standardClasses.classClass().getType().asStorage());
        fields.add(WasmType.Reference.ANY.asStorage());
        if (type instanceof ValueType.Object) {
            fillClassFields(fields, ((ValueType.Object) type).getClassName());
        } else if (type instanceof ValueType.Array) {
            fillArrayField(fields, ((ValueType.Array) type).getItemType());
        }
    }

    private void fillClassFields(List<WasmStorageType> fields, String className) {
        var classReader = classSource.get(className);
        if (classReader.hasModifier(ElementModifier.INTERFACE)) {
            fillSimpleClassFields(fields, "java.lang.Object");
        } else {
            fillSimpleClassFields(fields, className);
        }
    }

    private void fillSimpleClassFields(List<WasmStorageType> fields, String className) {
        var classReader = classSource.get(className);
        if (classReader.getParent() != null) {
            fillClassFields(fields, classReader.getParent());
        }
        for (var field : classReader.getFields()) {
            if (className.equals("java.lang.Object") && field.getName().equals("monitor")) {
                continue;
            }
            if (field.hasModifier(ElementModifier.STATIC)) {
                continue;
            }
            fieldIndexes.putIfAbsent(field.getReference(), fields.size());
            fields.add(typeMapper.mapType(field.getType()));
        }
        if (className.equals("java.lang.Class")) {
            classFlagsOffset = fields.size();
            fields.add(WasmType.INT32.asStorage());
            classTagOffset = fields.size();
            fields.add(WasmType.INT32.asStorage());
            classParentOffset = fields.size();
            fields.add(standardClasses.classClass().getType().asStorage());
            classArrayItemOffset = fields.size();
            fields.add(standardClasses.classClass().getType().asStorage());
            classArrayOffset = fields.size();
            fields.add(standardClasses.classClass().getType().asStorage());
            classSupertypeFunctionOffset = fields.size();
            fields.add(supertypeGenerator.getFunctionType().getReference().asStorage());
            virtualTableFieldOffset = fields.size();
            classNameOffset = fieldIndexes.get(new FieldReference(className, "name"));
        }
    }

    private void fillArrayField(List<WasmStorageType> fields, ValueType elementType) {
        var wasmArray = new WasmArray(null, () -> typeMapper.mapType(elementType));
        module.types.add(wasmArray);
        fields.add(wasmArray.getReference().asStorage());
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
        function.setName("_teavm_fill_primitive_class_");
        module.functions.add(function);

        var targetVar = new WasmLocal(standardClasses.classClass().getType(), "target");
        var nameVar = new WasmLocal(standardClasses.objectClass().getType(), "name");
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
        return createCreateArrayClassFunction();
    }

    private WasmFunction createCreateArrayClassFunction() {
        var functionType = functionTypes.of(
                null,
                standardClasses.classClass().getType(),
                standardClasses.classClass().getType()
        );
        var function = new WasmFunction(functionType);
        module.functions.add(function);
        function.setName("_teavm_fill_array_class_");

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
