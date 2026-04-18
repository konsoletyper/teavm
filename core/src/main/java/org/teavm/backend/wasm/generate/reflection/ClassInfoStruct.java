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

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmExpressionToInstructionConverter;
import org.teavm.backend.wasm.model.WasmField;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnary;
import org.teavm.backend.wasm.model.expression.WasmIntUnaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIsNull;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.backend.wasm.model.instruction.WasmNullConstantInstruction;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.analysis.ClassMetadataRequirements;
import org.teavm.runtime.reflect.ClassInfo;
import org.teavm.runtime.reflect.ClassReflectionInfo;

public class ClassInfoStruct {
    private final WasmGCNameProvider names;
    private final WasmModule module;
    private final WasmFunctionTypes functionTypes;
    private final WasmGCClassInfoProvider classInfoProvider;
    private final BaseWasmFunctionRepository functions;
    private final ClassMetadataRequirements metadataRequirements;
    private final DependencyInfo dependencies;
    private final ReflectionTypes reflectionTypes;

    private WasmStructure structure;
    private WasmArray array;
    private int vtableIndex;
    private int classObjectIndex;
    private int modifiersIndex;
    private int primitiveKindIndex;
    private int tagIndex;
    private int nameIndex = -1;
    private int simpleNameIndex = -1;
    private int itemTypeIndex;
    private int arrayTypeIndex;
    private int declaringClassIndex = -1;
    private int enclosingClassIndex = -1;
    private int supertypeFunctionIndex = -1;
    private int parentIndex = -1;
    private int interfacesIndex = -1;
    private int cloneFunctionIndex = -1;
    private int newArrayFunctionIndex = -1;
    private int getItemIndex = -1;
    private int putItemIndex = -1;
    private int arrayLengthIndex = -1;
    private int initializerIndex = -1;
    private int enumConstantsIndex = -1;
    private int initEnumConstantsIndex = -1;
    private int servicesIndex = -1;
    private int copyArrayIndex = -1;
    private int nextClassIndex = -1;
    private int createInstanceIndex = -1;
    private int initNewInstanceIndex = -1;
    private int reflectionInfoIndex = -1;

    private WasmFunctionType supertypeFunctionType;
    private WasmFunctionType cloneFunctionType;
    private WasmFunctionType newArrayFunctionType;
    private WasmFunctionType getItemFunctionType;
    private WasmFunctionType putItemFunctionType;
    private WasmFunctionType arrayLengthFunctionType;
    private WasmFunctionType copyArrayFunctionType;

    private WasmArray enumConstantsType;
    private WasmArray interfaceListType;

    private WasmGlobal firstClassGlobal;
    private WasmGlobal currentClassGlobal;

    private WasmFunction classFunction;

    ClassInfoStruct(WasmGCNameProvider names, WasmModule module, WasmFunctionTypes functionTypes,
            WasmGCClassInfoProvider classInfoProvider, BaseWasmFunctionRepository functions,
            ClassMetadataRequirements metadataRequirements, DependencyInfo dependencies,
            ReflectionTypes reflectionTypes) {
        this.names = names;
        this.module = module;
        this.functionTypes = functionTypes;
        this.classInfoProvider = classInfoProvider;
        this.functions = functions;
        this.metadataRequirements = metadataRequirements;
        this.dependencies = dependencies;
        this.reflectionTypes = reflectionTypes;

        var structName = names.topLevel(names.suggestForClass(ClassInfo.class.getName()));
        structure = new WasmStructure(structName, this::initFields);
        module.types.add(structure);
    }

    private void initFields(List<WasmField> fields) {
        vtableIndex = fields.size();
        var objectTypeInfo = classInfoProvider.getClassInfo("java.lang.Object");
        var vtableStruct = objectTypeInfo.getVirtualTableStructure();
        fields.add(new WasmField(vtableStruct, "vtable"));

        classObjectIndex = fields.size();
        var classObjectStruct = classInfoProvider.getClassInfo("java.lang.Class").getStructure();
        fields.add(new WasmField(classObjectStruct, "classObject"));

        tagIndex = fields.size();
        fields.add(new WasmField(WasmType.INT32, "tag"));

        modifiersIndex = fields.size();
        fields.add(new WasmField(WasmType.INT32, "modifiers"));

        primitiveKindIndex = fields.size();
        fields.add(new WasmField(WasmType.INT32, "primitiveKind"));

        if (metadataRequirements.hasName()) {
            nameIndex = fields.size();
            fields.add(new WasmField(stringType(), "name"));
        }
        if (metadataRequirements.hasSimpleName()) {
            simpleNameIndex = fields.size();
            fields.add(new WasmField(stringType(), "simpleName"));
        }

        itemTypeIndex = fields.size();
        fields.add(new WasmField(structure, "itemType"));

        arrayTypeIndex = fields.size();
        fields.add(new WasmField(structure, "arrayType"));

        if (metadataRequirements.hasDeclaringClass()) {
            declaringClassIndex = fields.size();
            fields.add(new WasmField(structure, "declaringClass"));
        }

        if (metadataRequirements.hasEnclosingClass()) {
            enclosingClassIndex = fields.size();
            fields.add(new WasmField(structure, "enclosingClass"));
        }

        if (metadataRequirements.hasIsAssignable()) {
            supertypeFunctionType = functionTypes.of(WasmType.INT32, structure.getReference(),
                    structure.getReference());
            supertypeFunctionIndex = fields.size();
            fields.add(new WasmField(supertypeFunctionType, "isSuperTypeOf"));
        }

        if (metadataRequirements.hasSuperclass()) {
            parentIndex = fields.size();
            fields.add(new WasmField(structure, "parent"));
        }

        if (metadataRequirements.hasGetInterfaces()) {
            interfacesIndex = fields.size();
            interfaceListType = array();
            fields.add(new WasmField(interfaceListType, "interfaces"));
        }

        cloneFunctionType = functionTypes.of(objectTypeInfo.getType(), objectTypeInfo.getType());
        cloneFunctionIndex = fields.size();
        fields.add(new WasmField(cloneFunctionType, "clone"));

        if (metadataRequirements.hasArrayNewInstance()) {
            newArrayFunctionType = functionTypes.of(objectTypeInfo.getType(), structure.getReference(), WasmType.INT32);
            newArrayFunctionType.setFinal(false);
            newArrayFunctionIndex = fields.size();
            fields.add(new WasmField(newArrayFunctionType, "newArray"));
        }

        if (metadataRequirements.hasArrayGet()) {
            getItemFunctionType = functionTypes.of(objectTypeInfo.getType(), structure.getReference(),
                    objectTypeInfo.getType(), WasmType.INT32);
            getItemIndex = fields.size();
            fields.add(new WasmField(getItemFunctionType, "getItem"));
        }

        if (metadataRequirements.hasArraySet()) {
            putItemFunctionType = functionTypes.of(null, structure.getReference(), objectTypeInfo.getType(),
                    WasmType.INT32, objectTypeInfo.getType());
            putItemIndex = fields.size();
            fields.add(new WasmField(putItemFunctionType, "putItem"));
        }

        if (metadataRequirements.hasArrayLength()) {
            arrayLengthFunctionType = functionTypes.of(WasmType.INT32, structure.getReference(),
                    objectTypeInfo.getType());
            arrayLengthIndex = fields.size();
            fields.add(new WasmField(arrayLengthFunctionType, "arrayLength"));
        }

        if (metadataRequirements.hasEnumConstants()) {
            enumConstantsType = new WasmArray(names.topLevel("TeaVM.EnumConstants)"),
                    objectTypeInfo.getType().asStorage());
            module.types.add(enumConstantsType);
            enumConstantsIndex = fields.size();
            fields.add(new WasmField(enumConstantsType, "enumConstants"));

            initEnumConstantsIndex = fields.size();
            fields.add(new WasmField(functionTypes.of(enumConstantsType.getReference()), "initEnumConstants"));
        }

        if (metadataRequirements.hasClassInit()) {
            initializerIndex = fields.size();
            fields.add(new WasmField(functionTypes.of(null), "initializer"));
        }

        var loadServicesMethod = dependencies.getMethod(new MethodReference(ServiceLoader.class, "loadServices",
                ClassInfo.class, Object[].class));
        if (loadServicesMethod != null && loadServicesMethod.isUsed()) {
            servicesIndex = fields.size();
            var serviceFunctionType = functionTypes.of(classInfoProvider.getClassInfo(ValueType.parse(Object[].class))
                    .getType());
            fields.add(new WasmField(serviceFunctionType, "services"));
        }

        var arrayCopyMethod = dependencies.getMethod(new MethodReference(System.class, "arrayCopyImpl",
                Object.class, int.class, Object.class, int.class, int.class, void.class));
        if (arrayCopyMethod != null && arrayCopyMethod.isUsed()) {
            var objectClass = classInfoProvider.getClassInfo("java.lang.Object");
            copyArrayFunctionType = functionTypes.of(null, structure.getReference(),
                    objectClass.getType(), WasmType.INT32, objectClass.getType(),
                    WasmType.INT32, WasmType.INT32);

            copyArrayIndex = fields.size();
            fields.add(new WasmField(copyArrayFunctionType, "copyArray"));
        }

        var fillNameMapMethod = dependencies.getMethod(new MethodReference(Class.class, "fillNameMap", void.class));
        if (fillNameMapMethod != null && fillNameMapMethod.isUsed()) {
            nextClassIndex = fields.size();
            fields.add(new WasmField(structure, "nextClass"));
        }

        var newInstanceMethod = dependencies.getMethod(new MethodReference(ClassInfo.class, "newInstance",
                Object.class));
        if (newInstanceMethod != null && newInstanceMethod.isUsed()) {
            var objectClass = classInfoProvider.getClassInfo("java.lang.Object");
            createInstanceIndex = fields.size();
            fields.add(new WasmField(functionTypes.of(objectClass.getType()), "createInstance"));
            initNewInstanceIndex = fields.size();
            fields.add(new WasmField(functionTypes.of(null, objectClass.getType()), "initNewInstance"));
        }

        var reflectionInfoMethod = dependencies.getMethod(new MethodReference(ClassInfo.class, "reflection",
                ClassReflectionInfo.class));
        if (reflectionInfoMethod != null && reflectionInfoMethod.isUsed()) {
            reflectionInfoIndex = fields.size();
            fields.add(new WasmField(reflectionTypes.classReflectionInfo().structure(), "reflection"));
        }
    }

    private WasmType stringType() {
        return classInfoProvider.getClassInfo("java.lang.String").getType();
    }

    public WasmStructure structure() {
        return structure;
    }

    public WasmArray array() {
        if (array == null) {
            var name = names.suggestForType(ValueType.arrayOf(ValueType.object(ClassInfo.class.getName())));
            array = new WasmArray(name, structure.getReference().asStorage());
            module.types.add(array);
        }
        return array;
    }

    public int vtableIndex() {
        init();
        return vtableIndex;
    }

    public int classObjectIndex() {
        init();
        return classObjectIndex;
    }

    public int tagIndex() {
        init();
        return tagIndex;
    }

    public int modifiersIndex() {
        init();
        return modifiersIndex;
    }

    public int primitiveKindIndex() {
        init();
        return primitiveKindIndex;
    }

    public int nameIndex() {
        init();
        return nameIndex;
    }

    public int simpleNameIndex() {
        init();
        return simpleNameIndex;
    }

    public int itemTypeIndex() {
        init();
        return itemTypeIndex;
    }

    public int arrayTypeIndex() {
        init();
        return arrayTypeIndex;
    }

    public int declaringClassIndex() {
        init();
        return declaringClassIndex;
    }

    public int enclosingClassIndex() {
        init();
        return enclosingClassIndex;
    }

    public int supertypeFunctionIndex() {
        init();
        return supertypeFunctionIndex;
    }

    public int parentIndex() {
        init();
        return parentIndex;
    }

    public int interfacesIndex() {
        init();
        return interfacesIndex;
    }

    public int cloneFunctionIndex() {
        init();
        return cloneFunctionIndex;
    }

    public int newArrayFunctionIndex() {
        init();
        return newArrayFunctionIndex;
    }

    public int getItemIndex() {
        init();
        return getItemIndex;
    }

    public int putItemIndex() {
        init();
        return putItemIndex;
    }

    public int arrayLengthIndex() {
        init();
        return arrayLengthIndex;
    }

    public int initializerIndex() {
        init();
        return initializerIndex;
    }

    public int enumConstantsIndex() {
        init();
        return enumConstantsIndex;
    }

    public int initEnumConstantsIndex() {
        init();
        return initEnumConstantsIndex;
    }

    public int servicesIndex() {
        init();
        return servicesIndex;
    }

    public int copyArrayIndex() {
        init();
        return copyArrayIndex;
    }

    public int nextClassIndex() {
        init();
        return nextClassIndex;
    }

    public int createInstanceIndex() {
        init();
        return createInstanceIndex;
    }

    public int initNewInstanceIndex() {
        init();
        return initNewInstanceIndex;
    }

    public int reflectionInfoIndex() {
        init();
        return reflectionInfoIndex;
    }

    public WasmFunctionType supertypeFunctionType() {
        init();
        return supertypeFunctionType;
    }

    public WasmFunctionType cloneFunctionType() {
        init();
        return cloneFunctionType;
    }

    public WasmFunctionType newArrayFunctionType() {
        init();
        return newArrayFunctionType;
    }

    public WasmFunctionType getItemFunctionType() {
        init();
        return getItemFunctionType;
    }

    public WasmFunctionType putItemFunctionType() {
        init();
        return putItemFunctionType;
    }

    public WasmFunctionType arrayLengthFunctionType() {
        init();
        return arrayLengthFunctionType;
    }

    public WasmFunctionType copyArrayFunctionType() {
        init();
        return copyArrayFunctionType;
    }

    public WasmArray enumConstantsType() {
        init();
        return enumConstantsType;
    }

    public WasmArray interfacesType() {
        init();
        return interfaceListType;
    }

    public WasmGlobal firstClassGlobal() {
        if (firstClassGlobal == null) {
            firstClassGlobal = new WasmGlobal(names.topLevel("teavm@firstClass"), structure.getReference());
            firstClassGlobal.getInitialValue().add(new WasmNullConstantInstruction(structure.getReference()));
            module.globals.add(firstClassGlobal);
        }
        return firstClassGlobal;
    }

    public WasmGlobal currentClassGlobal() {
        if (currentClassGlobal == null) {
            currentClassGlobal = new WasmGlobal(names.topLevel("teavm@currentClass"), structure.getReference());
            currentClassGlobal.getInitialValue().add(new WasmNullConstantInstruction(structure.getReference()));
            module.globals.add(currentClassGlobal);
        }
        return currentClassGlobal;
    }

    private void init() {
        structure.init();
    }

    public WasmFunction classObjectFunction() {
        if (classFunction == null) {
            var classType = classInfoProvider.getClassInfo("java.lang.Class").getType();
            var functionType = functionTypes.of(classType, structure.getReference());
            classFunction = new WasmFunction(functionType);
            module.functions.add(classFunction);
            classFunction.setName(names.topLevel("teavm@classObject"));

            var classInfoLocal = new WasmLocal(structure.getReference(), "classInfo");
            var resultLocal = new WasmLocal(classType, "result");
            classFunction.add(classInfoLocal);
            classFunction.add(resultLocal);

            structure.init();
            var body = new ArrayList<WasmExpression>();
            var candidateValue = new WasmStructGet(structure, new WasmGetLocal(classInfoLocal), classObjectIndex);
            body.add(new WasmSetLocal(resultLocal, candidateValue));

            var block = new WasmBlock(false);
            body.add(block);
            var notNullCond = new WasmIntUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ,
                    new WasmIsNull(new WasmGetLocal(resultLocal)));
            block.getBody().add(new WasmBranch(notNullCond, block));

            var constructor = functions.forStaticMethod(new MethodReference(Class.class, "createClass",
                    ClassInfo.class, Class.class));
            var createdClass = new WasmCall(constructor, new WasmGetLocal(classInfoLocal));
            block.getBody().add(new WasmSetLocal(resultLocal, createdClass));
            block.getBody().add(new WasmStructSet(structure, new WasmGetLocal(classInfoLocal), classObjectIndex,
                    new WasmGetLocal(resultLocal)));

            body.add(new WasmGetLocal(resultLocal));

            new WasmExpressionToInstructionConverter(classFunction.getBody()).convertAll(body);
        }
        return classFunction;
    }
}
