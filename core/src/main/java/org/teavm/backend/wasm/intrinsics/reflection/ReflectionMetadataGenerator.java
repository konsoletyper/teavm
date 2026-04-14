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
package org.teavm.backend.wasm.intrinsics.reflection;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfo;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.generate.classes.WasmGCTypeMapper;
import org.teavm.backend.wasm.generate.methods.WasmGCVirtualCallGenerator;
import org.teavm.backend.wasm.generate.strings.WasmGCStringProvider;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayGet;
import org.teavm.backend.wasm.model.expression.WasmArrayNewFixed;
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
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmSignedType;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructNew;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.backend.wasm.vtable.WasmGCVirtualTableProvider;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;
import org.teavm.model.GenericTypeParameter;
import org.teavm.model.GenericValueType;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.analysis.ClassInitializerInfo;
import org.teavm.reflection.AnnotationGenerationHelper;
import org.teavm.reflection.ReflectionDependencyListener;
import org.teavm.runtime.reflect.ClassReflectionInfo;

public class ReflectionMetadataGenerator {
    private WasmGCNameProvider names;
    private WasmModule module;
    private WasmFunctionTypes functionTypes;
    private DependencyInfo dependencies;
    private ReflectionDependencyListener reflection;
    private ListableClassReaderSource classes;
    private WasmGCClassInfoProvider classInfoProvider;
    private BaseWasmFunctionRepository functions;
    private WasmGCTypeMapper typeMapper;
    private WasmGCStringProvider strings;
    private ClassInitializerInfo classInitInfo;
    private WasmGCVirtualTableProvider virtualTables;
    private boolean innerClassesRequired;
    private Set<String> innerClassesAccessed = new HashSet<>();

    private WasmFunction initFunction;

    public ReflectionMetadataGenerator(WasmGCNameProvider names, WasmModule module, WasmFunctionTypes functionTypes,
            DependencyInfo dependencies, ReflectionDependencyListener reflection, ListableClassReaderSource classes,
            WasmGCClassInfoProvider classInfoProvider, BaseWasmFunctionRepository functions,
            WasmGCTypeMapper typeMapper, WasmGCStringProvider strings, ClassInitializerInfo classInitInfo,
            WasmGCVirtualTableProvider virtualTables) {
        this.names = names;
        this.module = module;
        this.functionTypes = functionTypes;
        this.dependencies = dependencies;
        this.reflection = reflection;
        this.classes = classes;
        this.functions = functions;
        this.classInfoProvider = classInfoProvider;
        this.typeMapper = typeMapper;
        this.strings = strings;
        this.classInitInfo = classInitInfo;
        this.virtualTables = virtualTables;

        var fn = new WasmFunction(functionTypes.of(null));
        fn.setName(names.topLevel("teavm@initReflection"));
        module.functions.add(fn);
        initFunction = fn;
    }

    public WasmFunction initFunction() {
        return initFunction;
    }

    public void generate() {
        var annotationsByClass = collectAnnotations();
        var classInfoStruct = classInfoProvider.reflectionTypes().classInfo();
        var needFieldsMetadata = dependencies.getMethod(new MethodReference(ClassReflectionInfo.class,
                "fieldCount", int.class)) != null;
        var needMethodsMetadata = dependencies.getMethod(new MethodReference(ClassReflectionInfo.class,
                "methodCount", int.class)) != null;
        var needTypeVars = dependencies.getMethod(new MethodReference(ClassReflectionInfo.class,
                "typeParameterCount", int.class)) != null;

        var classesMethod = dependencies.getMethod(new MethodReference(Class.class, "getDeclaredClasses",
                Class[].class));
        if (classesMethod != null) {
            innerClassesRequired = true;
            for (var type : classesMethod.getVariable(0).getClassValueNode().getTypes()) {
                if (type instanceof ValueType.Object) {
                    innerClassesAccessed.add(((ValueType.Object) type).getClassName());
                }
            }
        }

        for (var className : classes.getClassNames()) {
            var annotations = annotationsByClass.get(className);
            var cls = classes.get(className);
            var fields = needFieldsMetadata && reflection.getClassesWithReflectableFields().contains(className)
                    ? reflection.getAccessibleFields(className)
                    : List.<String>of();
            var methods = needMethodsMetadata && reflection.getClassesWithReflectableMethods().contains(className)
                    ? reflection.getAccessibleMethods(className)
                    : List.<MethodDescriptor>of();
            var innerClasses = extractInnerClasses(cls);
            var typeParameters = needTypeVars  ? cls.getGenericParameters() : new GenericTypeParameter[0];
            var metadata = generateClassMetadata(className, annotations, fields, methods, typeParameters,
                    innerClasses);
            if (metadata != null) {
                initFunction.getBody().add(new WasmStructSet(
                        classInfoStruct.structure(),
                        new WasmGetGlobal(classInfoProvider.getClassInfo(className).getPointer()),
                        classInfoStruct.reflectionInfoIndex(),
                        metadata
                ));
            }
        }
    }

    private WasmExpression generateClassMetadata(String className, List<AnnotationReader> annotations,
            Collection<? extends String> reflectableFields,
            Collection<? extends MethodDescriptor> reflectableMethods,
            GenericTypeParameter[] typeParameters,
            List<? extends String> innerClasses) {
        if (annotations == null && reflectableFields.isEmpty() && reflectableMethods.isEmpty()
                && (typeParameters == null || typeParameters.length == 0)
                && innerClasses.isEmpty()) {
            return null;
        }

        var classInfoStruct = classInfoProvider.reflectionTypes().classReflectionInfo();
        var metadata = new WasmStructNew(classInfoStruct.structure());

        if (classInfoStruct.annotationsIndex() >= 0) {
            metadata.getInitializers().add(generateAnnotations(annotations));
        }
        if (classInfoStruct.fieldsIndex() >= 0) {
            metadata.getInitializers().add(generateFields(className, reflectableFields));
        }
        if (classInfoStruct.methodsIndex() >= 0) {
            metadata.getInitializers().add(generateMethods(className, reflectableMethods));
        }
        if (classInfoStruct.typeParametersIndex() >= 0) {
            var cls = classes.get(className);
            metadata.getInitializers().add(generateTypeParameters(typeParameters, cls, null));
        }
        if (classInfoStruct.innerClassesIndex() >= 0) {
            metadata.getInitializers().add(generateInnerClasses(innerClasses));
        }

        return metadata;
    }

    private WasmExpression generateFields(String className, Collection<? extends String> fields) {
        var fieldInfoStruct = classInfoProvider.reflectionTypes().fieldInfo();
        if (fields.isEmpty()) {
            return new WasmNullConstant(fieldInfoStruct.array().getReference());
        }

        var cls = classes.get(className);
        var skipPrivates = ReflectionDependencyListener.shouldSkipPrivates(cls);

        var array = new WasmArrayNewFixed(fieldInfoStruct.array());

        for (var field : cls.getFields()) {
            if (!fields.contains(field.getName())) {
                continue;
            }
            if (skipPrivates) {
                if (field.getLevel() == AccessLevel.PRIVATE || field.getLevel() == AccessLevel.PACKAGE_PRIVATE) {
                    continue;
                }
            }

            var fieldInit = new WasmStructNew(fieldInfoStruct.structure());
            array.getElements().add(fieldInit);

            if (fieldInfoStruct.nameIndex() >= 0) {
                var nameStr = strings.getStringConstant(field.getName());
                fieldInit.getInitializers().add(new WasmGetGlobal(nameStr.global));
            }

            if (fieldInfoStruct.modifiersIndex() >= 0) {
                var modifiers = ElementModifier.asModifiersInfo(field.readModifiers(), field.getLevel());
                fieldInit.getInitializers().add(new WasmInt32Constant(modifiers));
            }

            if (fieldInfoStruct.typeIndex() >= 0) {
                fieldInit.getInitializers().add(generateDerivedClass(field.getType()));
            }

            if (fieldInfoStruct.readerIndex() >= 0) {
                if (reflection.isRead(field.getReference())) {
                    var getter = generateGetter(field);
                    fieldInit.getInitializers().add(new WasmFunctionReference(getter));
                } else {
                    var readerType = classInfoProvider.reflectionTypes().fieldInfo().readerType();
                    fieldInit.getInitializers().add(new WasmNullConstant(readerType.getReference()));
                }
            }
            if (fieldInfoStruct.writerIndex() >= 0) {
                if (reflection.isWritten(field.getReference())) {
                    var setter = generateSetter(field);
                    fieldInit.getInitializers().add(new WasmFunctionReference(setter));
                } else {
                    var writerType = classInfoProvider.reflectionTypes().fieldInfo().writerType();
                    fieldInit.getInitializers().add(new WasmNullConstant(writerType.getReference()));
                }
            }
            if (fieldInfoStruct.reflectionIndex() >= 0) {
                fieldInit.getInitializers().add(generateFieldReflection(field));
            }
        }

        return array;
    }

    private WasmExpression generateFieldReflection(FieldReader field) {
        var fieldReflectionStruct = classInfoProvider.reflectionTypes().fieldReflectionInfo();
        var annotations = fieldReflectionStruct.annotationsIndex() >= 0
                ? AnnotationGenerationHelper.collectRuntimeAnnotations(classes, field.getAnnotations().all())
                : List.<AnnotationReader>of();
        var genericType = fieldReflectionStruct.genericTypeIndex() >= 0 ? field.getGenericType() : null;
        if (annotations.isEmpty() && genericType == null) {
            return new WasmNullConstant(fieldReflectionStruct.structure().getReference());
        }

        var result = new WasmStructNew(fieldReflectionStruct.structure());
        if (fieldReflectionStruct.annotationsIndex() >= 0) {
            result.getInitializers().add(generateAnnotations(annotations));
        }
        if (fieldReflectionStruct.genericTypeIndex() >= 0) {
            var cls = classes.get(field.getOwnerName());
            result.getInitializers().add(generateGenericType(cls, null, genericType));
        }

        return result;
    }

    private WasmExpression generateMethods(String className, Collection<? extends MethodDescriptor> methods) {
        var methodInfoStruct = classInfoProvider.reflectionTypes().methodInfo();
        if (methods.isEmpty()) {
            return new WasmNullConstant(methodInfoStruct.array().getReference());
        }

        var cls = classes.get(className);
        if (cls == null || cls.getMethods().isEmpty()) {
            return new WasmNullConstant(methodInfoStruct.array().getReference());
        }

        var callerType = methodInfoStruct.callerType();
        /*
        var objectClass = classInfoProvider.getClassInfo("java.lang.Object");
        var objectArrayClass = classInfoProvider.getClassInfo(ValueType.parse(Object[].class));
        var objectArrayType = classInfoProvider.getObjectArrayType();
        var withBounds = context.dependency().getMethod(TYPE_VAR_GET_BOUNDS) != null;
        var withGenericReturn = context.dependency().getMethod(METHOD_GET_GENERIC_RETURN_TYPE) != null;
        var withGenericParams = context.dependency().getMethod(EXECUTABLE_GET_GENERIC_PARAMETER_TYPES) != null;
         */

        var result = new WasmArrayNewFixed(methodInfoStruct.array());
        var skipPrivates = ReflectionDependencyListener.shouldSkipPrivates(cls);

        for (var method : cls.getMethods()) {
            if (skipPrivates) {
                if (method.getLevel() == AccessLevel.PRIVATE || method.getLevel() == AccessLevel.PACKAGE_PRIVATE) {
                    continue;
                }
            }
            if (method.getName().equals("<clinit>")) {
                continue;
            }
            var methodInit = new WasmStructNew(methodInfoStruct.structure());
            result.getElements().add(methodInit);

            if (methodInfoStruct.nameIndex() >= 0) {
                var nameStr = strings.getStringConstant(method.getName());
                methodInit.getInitializers().add(new WasmGetGlobal(nameStr.global));
            }

            if (methodInfoStruct.modifiersIndex() >= 0) {
                var modifiers = ElementModifier.asModifiersInfo(method.readModifiers(), method.getLevel());
                methodInit.getInitializers().add(new WasmInt32Constant(modifiers));
            }

            if (methodInfoStruct.returnTypeIndex() >= 0) {
                methodInit.getInitializers().add(generateDerivedClass(method.getResultType()));
            }

            if (methodInfoStruct.parameterTypesIndex() >= 0) {
                var paramTypes = method.getParameterTypes();
                var derivedClassInfoStruct = classInfoProvider.reflectionTypes().derivedClassInfo();
                if (paramTypes.length == 0) {
                    methodInit.getInitializers().add(new WasmNullConstant(
                            derivedClassInfoStruct.array().getReference()));
                } else {
                    var parametersArray = new WasmArrayNewFixed(derivedClassInfoStruct.array());
                    for (var param : paramTypes) {
                        parametersArray.getElements().add(generateDerivedClass(param));
                    }
                    methodInit.getInitializers().add(parametersArray);
                }
            }

            if (methodInfoStruct.callerIndex() >= 0) {
                if (reflection.isCalled(method.getReference())) {
                    var caller = generateCaller(method);
                    methodInit.getInitializers().add(new WasmFunctionReference(caller));
                } else {
                    methodInit.getInitializers().add(new WasmNullConstant(callerType.getReference()));
                }
            }

            if (methodInfoStruct.reflectionIndex() >= 0) {
                methodInit.getInitializers().add(generateMethodReflection(method));
            }
        }

        return result;
    }

    private WasmExpression generateMethodReflection(MethodReader method) {
        var reflectionTypes = classInfoProvider.reflectionTypes();
        var methodReflectionStruct = reflectionTypes.methodReflectionInfo();
        var annotations = methodReflectionStruct.annotationsIndex() >= 0
                ? AnnotationGenerationHelper.collectRuntimeAnnotations(classes, method.getAnnotations().all())
                : List.<AnnotationReader>of();
        var genericReturnType = methodReflectionStruct.genericReturnTypeIndex() >= 0
                ? method.getGenericResultType()
                : null;
        var genericParameterTypes = methodReflectionStruct.genericParameterTypesIndex() >= 0
                ? method.getGenericParameterTypes()
                : null;
        var typeParameters = methodReflectionStruct.typeParametersIndex() >= 0
                ? method.getTypeParameters()
                : null;
        if (annotations.isEmpty() && genericReturnType == null
                && (genericParameterTypes == null || genericParameterTypes.length == 0)
                && (typeParameters == null || typeParameters.length == 0)) {
            return new WasmNullConstant(methodReflectionStruct.structure().getReference());
        }

        var result = new WasmStructNew(methodReflectionStruct.structure());
        var cls = classes.get(method.getOwnerName());

        if (methodReflectionStruct.genericReturnTypeIndex() >= 0) {
            if (genericReturnType != null) {
                result.getInitializers().add(generateGenericType(cls, method, genericReturnType));
            } else {
                result.getInitializers().add(new WasmNullConstant(WasmType.STRUCT));
            }
        }

        if (methodReflectionStruct.genericParameterTypesIndex() >= 0) {
            if (genericParameterTypes != null && genericParameterTypes.length > 0) {
                var array = new WasmArrayNewFixed(reflectionTypes.genericTypeArray());
                for (var i = 0; i < genericParameterTypes.length; ++i) {
                    var paramType = genericParameterTypes[i];
                    if (paramType.canBeRepresentedAsRaw() && paramType.asValueType()
                            .equals(method.parameterType(i))) {
                        array.getElements().add(new WasmNullConstant(WasmType.STRUCT));
                    } else {
                        array.getElements().add(generateGenericType(cls, method, paramType));
                    }
                }
                result.getInitializers().add(array);
            } else {
                result.getInitializers().add(new WasmNullConstant(reflectionTypes.genericTypeArray().getReference()));
            }
        }

        if (methodReflectionStruct.annotationsIndex() >= 0) {
            result.getInitializers().add(generateAnnotations(annotations));
        }

        if (methodReflectionStruct.typeParametersIndex() >= 0) {
            if (typeParameters == null || typeParameters.length == 0) {
                result.getInitializers().add(new WasmNullConstant(reflectionTypes.typeVariableInfo()
                        .array().getReference()));
            } else {
                result.getInitializers().add(generateTypeParameters(typeParameters, cls, method));
            }
        }

        return result;
    }

    private WasmExpression generateAnnotations(List<AnnotationReader> annotations) {
        var annotationInfoStruct = classInfoProvider.reflectionTypes().annotationInfo();

        if (annotations == null || annotations.isEmpty()) {
            return new WasmNullConstant(annotationInfoStruct.array().getReference());
        }

        var annotationsExpr = new WasmArrayNewFixed(annotationInfoStruct.array());

        for (var annotation : annotations) {
            var elem = new WasmStructNew(annotationInfoStruct.structure());
            elem.getInitializers().add(generateAnnotation(annotation));
            var dataStruct = classInfoProvider.reflectionTypes().annotationData(annotation.getType());
            dataStruct.constructor().setReferenced(true);
            elem.getInitializers().add(new WasmFunctionReference(dataStruct.constructor()));
            annotationsExpr.getElements().add(elem);
        }

        return annotationsExpr;
    }

    private WasmExpression generateAnnotation(AnnotationReader annotation) {
        var struct = classInfoProvider.reflectionTypes().annotationData(annotation.getType());
        var result = new WasmStructNew(struct.structure());
        for (var field : struct.fields()) {
            var value = annotation.getValue(field.name);
            if (value == null) {
                value = field.defaultValue;
            }
            result.getInitializers().add(generateAnnotationValue(value, field.type));
        }
        return result;
    }

    private WasmExpression generateAnnotationValue(AnnotationValue value, ValueType type) {
        switch (value.getType()) {
            case AnnotationValue.BOOLEAN:
                return new WasmInt32Constant(value.getBoolean() ? 1 : 0);
            case AnnotationValue.BYTE:
                return new WasmInt32Constant(value.getByte());
            case AnnotationValue.SHORT:
                return new WasmInt32Constant(value.getShort());
            case AnnotationValue.CHAR:
                return new WasmInt32Constant(value.getChar());
            case AnnotationValue.INT:
                return new WasmInt32Constant(value.getInt());
            case AnnotationValue.LONG:
                return new WasmInt64Constant(value.getLong());
            case AnnotationValue.FLOAT:
                return new WasmFloat32Constant(value.getFloat());
            case AnnotationValue.DOUBLE:
                return new WasmFloat64Constant(value.getDouble());
            case AnnotationValue.STRING:
                return new WasmGetGlobal(strings.getStringConstant(value.getString()).global);
            case AnnotationValue.CLASS:
                return generateDerivedClass(value.getJavaClass());
            case AnnotationValue.ANNOTATION:
                return generateAnnotation(value.getAnnotation());
            case AnnotationValue.ENUM: {
                var enumClass = classes.get(value.getEnumValue().getClassName());
                if (enumClass == null) {
                    return new WasmInt32Constant(-1);
                }
                var index = 0;
                for (var field : enumClass.getFields()) {
                    if (field.hasModifier(ElementModifier.ENUM) || field.hasModifier(ElementModifier.STATIC)) {
                        if (field.getName().equals(value.getEnumValue().getFieldName())) {
                            break;
                        }
                        ++index;
                    }
                }
                return new WasmInt32Constant(index);
            }
            case AnnotationValue.LIST: {
                var itemType = ((ValueType.Array) type).getItemType();
                var wasmItemType = classInfoProvider.reflectionTypes().typeForAnnotation(itemType, true);
                var result = new WasmArrayNewFixed(classInfoProvider.reflectionTypes().arrayTypeOf(wasmItemType));
                for (var item : value.getList()) {
                    result.getElements().add(generateAnnotationValue(item, itemType));
                }
                return result;
            }
            default:
                throw new IllegalStateException();
        }
    }

    private Map<String, List<AnnotationReader>> collectAnnotations() {
        var methodDep = dependencies.getMethod(new MethodReference(Class.class, "getDeclaredAnnotations",
                Annotation[].class));
        if (methodDep == null) {
            return Collections.emptyMap();
        }
        var result = new LinkedHashMap<String, List<AnnotationReader>>();
        for (var type : methodDep.getVariable(0).getClassValueNode().getTypes()) {
            if (!(type instanceof ValueType.Object)) {
                continue;
            }
            var className = ((ValueType.Object) type).getClassName();
            var cls = classes.get(className);
            if (cls == null) {
                return null;
            }
            var annotations = AnnotationGenerationHelper.collectRuntimeAnnotations(classes,
                    cls.getAnnotations().all());
            if (!annotations.isEmpty()) {
                result.put(className, annotations);
            }
        }
        return result;
    }

    private WasmExpression generateDerivedClass(ValueType type) {
        var degree = 0;
        while (type instanceof ValueType.Array) {
            type = ((ValueType.Array) type).getItemType();
            ++degree;
        }
        var result = new WasmStructNew(classInfoProvider.reflectionTypes().derivedClassInfo().structure());
        result.getInitializers().add(new WasmGetGlobal(classInfoProvider.getClassInfo(type).getPointer()));
        result.getInitializers().add(new WasmInt32Constant(degree));
        return result;
    }

    private WasmFunction generateGetter(FieldReader field) {
        var objectClass = classInfoProvider.getClassInfo("java.lang.Object");
        var getterType = classInfoProvider.reflectionTypes().fieldInfo().readerType();
        var function = new WasmFunction(getterType);
        function.setName(names.topLevel(names.suggestForStaticField(field.getReference()) + "@getter"));
        module.functions.add(function);
        function.setReferenced(true);

        var thisVar = new WasmLocal(objectClass.getType(), "this");
        function.add(thisVar);

        WasmExpression result;
        var classInfo = classInfoProvider.getClassInfo(field.getOwnerName());
        if (field.hasModifier(ElementModifier.STATIC)) {
            initClass(classInfo, field.getOwnerName(), function);
            var global = classInfoProvider.getStaticFieldLocation(field.getReference());
            result = new WasmGetGlobal(global);
        } else {
            var castInstance = new WasmCast(new WasmGetLocal(thisVar), classInfo.getType());
            var structGet = new WasmStructGet(classInfo.getStructure(), castInstance,
                    classInfoProvider.getFieldIndex(field.getReference()));
            if (field.getType() instanceof ValueType.Primitive) {
                switch (((ValueType.Primitive) field.getType()).getKind()) {
                    case BYTE:
                    case SHORT:
                        structGet.setSignedType(WasmSignedType.SIGNED);
                        break;
                    case BOOLEAN:
                    case CHARACTER:
                        structGet.setSignedType(WasmSignedType.UNSIGNED);
                        break;
                    default:
                        break;
                }
            }
            result = structGet;
        }

        function.getBody().add(boxIfNecessary(result, field.getType()));

        return function;
    }

    private WasmFunction generateSetter(FieldReader field) {
        var objectClass = classInfoProvider.getClassInfo("java.lang.Object");
        var setterType = classInfoProvider.reflectionTypes().fieldInfo().writerType();
        var function = new WasmFunction(setterType);
        function.setName(names.topLevel(names.suggestForStaticField(field.getReference()) + "@setter"));
        module.functions.add(function);
        function.setReferenced(true);

        var thisVar = new WasmLocal(objectClass.getType(), "this");
        function.add(thisVar);
        var valueVar = new WasmLocal(objectClass.getType(), "value");
        function.add(valueVar);

        var value = unboxIfNecessary(new WasmGetLocal(valueVar), field.getType());
        var classInfo = classInfoProvider.getClassInfo(field.getOwnerName());
        if (field.hasModifier(ElementModifier.STATIC)) {
            initClass(classInfo, field.getOwnerName(), function);
            var global = classInfoProvider.getStaticFieldLocation(field.getReference());
            function.getBody().add(new WasmSetGlobal(global, value));
        } else {
            var castInstance = new WasmCast(new WasmGetLocal(thisVar), classInfo.getType());
            var structSet = new WasmStructSet(classInfo.getStructure(), castInstance,
                    classInfoProvider.getFieldIndex(field.getReference()), value);
            function.getBody().add(structSet);
        }

        return function;
    }

    private WasmFunction generateCaller(MethodReader method) {
        var objectClass = classInfoProvider.getClassInfo("java.lang.Object");
        var objectArrayClass = classInfoProvider.getClassInfo(ValueType.parse(Object[].class));
        var methodInfoStruct = classInfoProvider.reflectionTypes().methodInfo();
        var callerType = methodInfoStruct.callerType();
        var function = new WasmFunction(callerType);
        function.setName(names.topLevel(names.suggestForMethod(method.getReference()) + "@caller"));
        module.functions.add(function);
        function.setReferenced(true);

        var dataField = objectArrayClass.getStructure().getFields()
                .get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);

        var thisVar = new WasmLocal(objectClass.getType(), "this");
        function.add(thisVar);
        var argsVar = new WasmLocal(objectArrayClass.getType(), "args");
        function.add(argsVar);
        var argsDataVar = new WasmLocal(dataField.getUnpackedType(), "argsData");
        function.add(argsDataVar);
        WasmLocal instanceVar = null;

        function.getBody().add(new WasmSetLocal(argsDataVar, new WasmStructGet(objectArrayClass.getStructure(),
                new WasmGetLocal(argsVar), dataField.getIndex())));

        var classInfo = classInfoProvider.getClassInfo(method.getOwnerName());
        var args = new ArrayList<WasmExpression>();
        WasmFunction callee = null;
        var virtual = false;
        if (method.hasModifier(ElementModifier.STATIC)) {
            initClass(classInfo, method.getOwnerName(), function);
            callee = functions.forStaticMethod(method.getReference());
        } else {
            virtual = !method.hasModifier(ElementModifier.FINAL) && method.getLevel() != AccessLevel.PRIVATE
                    && !method.getName().equals("<init>");

            if (!virtual) {
                var castInstance = new WasmCast(new WasmGetLocal(thisVar), classInfo.getType());
                args.add(castInstance);
                callee = functions.forInstanceMethod(method.getReference());
            }
        }

        var dataType = (WasmType.CompositeReference) dataField.getUnpackedType();
        var dataArray = (WasmArray) dataType.composite;
        for (var i = 0; i < method.parameterCount(); ++i) {
            var rawArg = new WasmArrayGet(dataArray, new WasmGetLocal(argsDataVar), new WasmInt32Constant(i));
            args.add(unboxIfNecessary(rawArg, method.parameterType(i)));
        }

        WasmExpression call;
        if (virtual) {
            var callGen = new WasmGCVirtualCallGenerator(virtualTables, classInfoProvider);
            call = callGen.generate(method.getReference(), false, thisVar, args);
        } else {
            call = new WasmCall(callee, args.toArray(new WasmExpression[0]));
        }
        function.getBody().add(boxIfNecessary(call, method.getResultType()));
        if (method.getResultType() == ValueType.VOID) {
            function.getBody().add(new WasmNullConstant(objectClass.getType()));
        }

        return function;
    }

    private void initClass(WasmGCClassInfo classInfo, String className, WasmFunction function) {
        if (classInitInfo.isDynamicInitializer(className)
                && classInfo.getInitializerPointer() != null) {
            var initRef = new WasmGetGlobal(classInfo.getInitializerPointer());
            var initType = functionTypes.of(null);
            function.getBody().add(new WasmCallReference(initRef, initType));
        }
    }

    private WasmExpression boxIfNecessary(WasmExpression expr, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return box(Boolean.class, type, expr);
                case BYTE:
                    return box(Byte.class, type, expr);
                case SHORT:
                    return box(Short.class, type, expr);
                case CHARACTER:
                    return box(Character.class, type, expr);
                case INTEGER:
                    return box(Integer.class, type, expr);
                case LONG:
                    return box(Long.class, type, expr);
                case FLOAT:
                    return box(Float.class, type, expr);
                case DOUBLE:
                    return box(Double.class, type, expr);
            }
        }
        return expr;
    }

    private WasmExpression box(Class<?> wrapperType, ValueType sourceType, WasmExpression expr) {
        var method = new MethodReference(wrapperType.getName(), "valueOf", sourceType,
                ValueType.object(wrapperType.getName()));
        var function = functions.forStaticMethod(method);
        return new WasmCall(function, expr);
    }

    private WasmExpression unboxIfNecessary(WasmExpression expr, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return unbox(boolean.class, Boolean.class, expr);
                case BYTE:
                    return unbox(byte.class, Byte.class, expr);
                case SHORT:
                    return unbox(short.class, Short.class, expr);
                case CHARACTER:
                    return unbox(char.class, Character.class, expr);
                case INTEGER:
                    return unbox(int.class, Integer.class, expr);
                case LONG:
                    return unbox(long.class, Long.class, expr);
                case FLOAT:
                    return unbox(float.class, Float.class, expr);
                case DOUBLE:
                    return unbox(double.class, Double.class, expr);
            }
        } else if (type instanceof ValueType.Object) {
            if (((ValueType.Object) type).getClassName().equals("java.lang.Object")) {
                return expr;
            }
        }
        var targetType = typeMapper.mapType(type);
        if (targetType == typeMapper.mapType(ValueType.object("java.lang.Object"))) {
            return expr;
        }
        return new WasmCast(expr, (WasmType.Reference) targetType);
    }

    private WasmExpression unbox(Class<?> primitiveType, Class<?> wrapperType, WasmExpression expr) {
        var method = new MethodReference(wrapperType.getName(), primitiveType.getName() + "Value",
                ValueType.parse(primitiveType));
        var function = functions.forInstanceMethod(method);
        var cast = new WasmCast(expr, classInfoProvider.getClassInfo(wrapperType.getName()).getType());
        return new WasmCall(function, cast);
    }

    private WasmExpression generateTypeParameters(GenericTypeParameter[] params, ClassReader cls,
            MethodReader method) {
        var struct = classInfoProvider.reflectionTypes().typeVariableInfo();
        var array = new WasmArrayNewFixed(struct.array());
        for (var param : params) {
            var arrayItem = new WasmStructNew(struct.structure());
            if (struct.nameIndex() >= 0) {
                arrayItem.getInitializers().add(new WasmGetGlobal(strings.getStringConstant(param.getName()).global));
            }
            if (struct.boundsIndex() >= 0) {
                var bounds = param.extractAllBounds();
                if (bounds.isEmpty()) {
                    arrayItem.getInitializers().add(new WasmNullConstant(classInfoProvider.reflectionTypes()
                            .genericTypeArray().getReference()));
                } else {
                    arrayItem.getInitializers().add(generateTypeParameterBounds(cls, method, bounds));
                }
            }
            array.getElements().add(arrayItem);
        }
        return array;
    }

    private WasmExpression generateTypeParameterBounds(ClassReader cls, MethodReader method,
            List<GenericValueType.Reference> bounds) {
        var array = new WasmArrayNewFixed(classInfoProvider.reflectionTypes().genericTypeArray());
        for (var bound : bounds) {
            array.getElements().add(generateGenericType(cls, method, bound));
        }
        return array;
    }

    private WasmExpression generateGenericType(ClassReader contextClass, MethodReader contextMethod,
            GenericValueType type) {
        if (type instanceof GenericValueType.Object) {
            var objectType = (GenericValueType.Object) type;
            var args = objectType.getArguments();
            if (args.length == 0) {
                return generateDerivedClass(ValueType.object(objectType.getClassName()));
            } else {
                var clsInfo = classInfoProvider.getClassInfo(objectType.getFullClassName());
                var cls = new WasmGetGlobal(clsInfo.getPointer());
                var resultStruct = classInfoProvider.reflectionTypes().parameterizedTypeInfo();
                var result = new WasmStructNew(resultStruct.structure());
                if (resultStruct.rawTypeIndex() >= 0) {
                    result.getInitializers().add(cls);
                }
                if (resultStruct.actualTypeArgumentsIndex() >= 0) {
                    var arrayType = classInfoProvider.reflectionTypes().genericTypeArray();
                    var array = new WasmArrayNewFixed(arrayType);
                    for (var arg : args) {
                        array.getElements().add(generateGenericType(contextClass, contextMethod, arg));
                    }
                    result.getInitializers().add(array);
                }
                if (resultStruct.ownerTypeIndex() >= 0) {
                    var ownerType = objectType.getParent();
                    if (ownerType != null) {
                        result.getInitializers().add(generateGenericType(contextClass, contextMethod, ownerType));
                    } else {
                        result.getInitializers().add(new WasmNullConstant(WasmType.STRUCT));
                    }
                }
                return result;
            }
        } else if (type instanceof GenericValueType.Variable) {
            var typeVar = (GenericValueType.Variable) type;
            var level = 0;
            if (contextMethod != null) {
                var genericParameters = contextMethod.getTypeParameters();
                for (var i = 0; i < genericParameters.length; i++) {
                    var param = genericParameters[i];
                    if (param.getName().equals(typeVar.getName())) {
                        return typeVariableRef(0, i);
                    }
                }
                ++level;
            }
            while (contextClass != null) {
                var genericParameters = contextClass.getGenericParameters();
                if (genericParameters != null) {
                    for (var i = 0; i < genericParameters.length; i++) {
                        var param = genericParameters[i];
                        if (param.getName().equals(typeVar.getName())) {
                            return typeVariableRef(level, i);
                        }
                    }
                }
                ++level;
                if (contextClass.getOwnerName() == null) {
                    break;
                }
                contextClass = classes.get(contextClass.getOwnerName());
            }
            throw new IllegalArgumentException("Unknown type variable: " + typeVar.getName());
        } else if (type instanceof GenericValueType.Array) {
            var nonGenericType = type.asValueType();
            if (nonGenericType == null) {
                var arrayType = (GenericValueType.Array) type;
                var struct = classInfoProvider.reflectionTypes().genericArrayInfo();
                var result = new WasmStructNew(struct.structure());
                result.getInitializers().add(generateGenericType(contextClass, contextMethod, arrayType.getItemType()));
                return result;
            } else {
                return generateDerivedClass(nonGenericType);
            }
        } else if (type instanceof GenericValueType.Primitive) {
            var primitiveType = (GenericValueType.Primitive) type;
            return generateDerivedClass(ValueType.primitive(primitiveType.getKind()));
        } else if (type instanceof GenericValueType.Void) {
            return generateDerivedClass(ValueType.VOID);
        } else {
            throw new IllegalArgumentException("Unsupported generic type: " + type);
        }
    }

    private WasmExpression typeVariableRef(int level, int index) {
        var result = new WasmStructNew(classInfoProvider.reflectionTypes().typeVariableReference().structure());
        result.getInitializers().add(new WasmInt32Constant(level));
        result.getInitializers().add(new WasmInt32Constant(index));
        return result;
    }

    private WasmExpression generateGenericType(ClassReader contextClass, MethodReader contextMethod,
            GenericValueType.Argument arg) {
        switch (arg.getKind()) {
            case INVARIANT:
                return generateGenericType(contextClass, contextMethod, arg.getValue());
            case ANY: {
                var struct = classInfoProvider.reflectionTypes().wildcardTypeInfo();
                var result = new WasmStructNew(struct.structure());
                result.getInitializers().add(new WasmInt32Constant(2));
                result.getInitializers().add(new WasmNullConstant(WasmType.STRUCT));
                return result;
            }
            case COVARIANT: {
                var struct = classInfoProvider.reflectionTypes().wildcardTypeInfo();
                var result = new WasmStructNew(struct.structure());
                result.getInitializers().add(new WasmInt32Constant(0));
                result.getInitializers().add(generateGenericType(contextClass, contextMethod, arg.getValue()));
                return result;
            }
            case CONTRAVARIANT: {
                var struct = classInfoProvider.reflectionTypes().wildcardTypeInfo();
                var result = new WasmStructNew(struct.structure());
                result.getInitializers().add(new WasmInt32Constant(1));
                result.getInitializers().add(generateGenericType(contextClass, contextMethod, arg.getValue()));
                return result;
            }
            default: {
                throw new IllegalArgumentException("Unsupported generic type: " + arg.getKind());
            }
        }
    }

    private WasmExpression generateInnerClasses(List<? extends String> innerClasses) {
        if (innerClasses.isEmpty()) {
            return new WasmNullConstant(classInfoProvider.reflectionTypes().classInfo().array().getReference());
        }
        var array = new WasmArrayNewFixed(classInfoProvider.reflectionTypes().classInfo().array());
        for (var innerClass : innerClasses) {
            array.getElements().add(new WasmGetGlobal(classInfoProvider.getClassInfo(innerClass).getPointer()));
        }
        return array;
    }

    private List<? extends String> extractInnerClasses(ClassReader cls) {
        if (!innerClassesRequired || !innerClassesAccessed.contains(cls.getName())) {
            return Collections.emptyList();
        }
        var filtered = new ArrayList<String>();
        for (var innerCls : cls.getInnerClasses()) {
            if (dependencies.getClass(innerCls) != null) {
                filtered.add(innerCls);
            }
        }
        return filtered;
    }
}
