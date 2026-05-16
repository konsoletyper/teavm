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
import java.util.function.Predicate;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.TemporaryVariablePool;
import org.teavm.backend.wasm.generate.ValueCache;
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
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmSignedType;
import org.teavm.backend.wasm.transformation.CoroutineTransformation;
import org.teavm.backend.wasm.vtable.WasmGCVirtualTableProvider;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationContainerReader;
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
    private Predicate<MethodReference> asyncMethods;
    private boolean innerClassesRequired;
    private Set<String> innerClassesAccessed = new HashSet<>();

    private WasmFunction initFunction;

    public ReflectionMetadataGenerator(WasmGCNameProvider names, WasmModule module, WasmFunctionTypes functionTypes,
            DependencyInfo dependencies, ReflectionDependencyListener reflection, ListableClassReaderSource classes,
            WasmGCClassInfoProvider classInfoProvider, BaseWasmFunctionRepository functions,
            WasmGCTypeMapper typeMapper, WasmGCStringProvider strings, ClassInitializerInfo classInitInfo,
            WasmGCVirtualTableProvider virtualTables, Predicate<MethodReference> asyncMethods) {
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
        this.asyncMethods = asyncMethods;

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
                var builder = initFunction.getBody().builder();
                builder
                        .getGlobal(classInfoProvider.getClassInfo(className).getPointer())
                        .transferFrom(metadata.builder())
                        .structSet(classInfoStruct.structure(), classInfoStruct.reflectionInfoIndex());
            }
        }
    }

    private WasmInstructionList generateClassMetadata(String className, List<AnnotationReader> annotations,
            Collection<? extends String> reflectableFields,
            Collection<? extends MethodDescriptor> reflectableMethods,
            GenericTypeParameter[] typeParameters,
            List<? extends String> innerClasses) {
        if (annotations == null && reflectableFields.isEmpty() && reflectableMethods.isEmpty()
                && (typeParameters == null || typeParameters.length == 0)
                && innerClasses.isEmpty()) {
            return null;
        }

        var builder = new WasmInstructionList().builder();
        var classInfoStruct = classInfoProvider.reflectionTypes().classReflectionInfo();

        if (classInfoStruct.annotationsIndex() >= 0) {
            generateAnnotations(builder, annotations);
        }
        if (classInfoStruct.fieldsIndex() >= 0) {
            generateFields(builder, className, reflectableFields);
        }
        if (classInfoStruct.methodsIndex() >= 0) {
            generateMethods(builder, className, reflectableMethods);
        }
        if (classInfoStruct.typeParametersIndex() >= 0) {
            var cls = classes.get(className);
            generateTypeParameters(builder, typeParameters, cls, null);
        }
        if (classInfoStruct.innerClassesIndex() >= 0) {
            generateInnerClasses(builder, innerClasses);
        }
        builder.structNew(classInfoStruct.structure());

        return builder.list;
    }

    private void generateFields(WasmInstructionBuilder builder, String className,
            Collection<? extends String> fields) {
        var fieldInfoStruct = classInfoProvider.reflectionTypes().fieldInfo();
        if (fields.isEmpty()) {
            builder.nullConst(fieldInfoStruct.array().getReference());
            return;
        }

        var cls = classes.get(className);
        var skipPrivates = ReflectionDependencyListener.shouldSkipPrivates(cls);

        var count = 0;
        for (var field : cls.getFields()) {
            if (!fields.contains(field.getName())) {
                continue;
            }
            if (skipPrivates) {
                if (field.getLevel() == AccessLevel.PRIVATE || field.getLevel() == AccessLevel.PACKAGE_PRIVATE) {
                    continue;
                }
            }

            if (fieldInfoStruct.nameIndex() >= 0) {
                var nameStr = strings.getStringConstant(field.getName());
                builder.getGlobal(nameStr.global);
            }

            if (fieldInfoStruct.modifiersIndex() >= 0) {
                var modifiers = ElementModifier.asModifiersInfo(field.readModifiers(), field.getLevel());
                builder.i32Const(modifiers);
            }

            if (fieldInfoStruct.typeIndex() >= 0) {
                generateDerivedClass(builder, field.getType());
            }

            if (fieldInfoStruct.readerIndex() >= 0) {
                if (reflection.isRead(field.getReference())) {
                    var getter = generateGetter(field);
                    builder.funcRef(getter);
                } else {
                    var readerType = classInfoProvider.reflectionTypes().fieldInfo().readerType();
                    builder.nullConst(readerType.getReference());
                }
            }
            if (fieldInfoStruct.writerIndex() >= 0) {
                if (reflection.isWritten(field.getReference())) {
                    var setter = generateSetter(field);
                    builder.funcRef(setter);
                } else {
                    var writerType = classInfoProvider.reflectionTypes().fieldInfo().writerType();
                    builder.nullConst(writerType.getReference());
                }
            }
            if (fieldInfoStruct.reflectionIndex() >= 0) {
                generateFieldReflection(builder, field);
            }

            builder.structNew(fieldInfoStruct.structure());
            ++count;
        }

        builder.arrayNewFixed(fieldInfoStruct.array(), count);
    }

    private void generateFieldReflection(WasmInstructionBuilder builder, FieldReader field) {
        var fieldReflectionStruct = classInfoProvider.reflectionTypes().fieldReflectionInfo();
        var annotations = fieldReflectionStruct.annotationsIndex() >= 0
                ? AnnotationGenerationHelper.collectRuntimeAnnotations(classes, field.getAnnotations().all())
                : List.<AnnotationReader>of();
        var genericType = fieldReflectionStruct.genericTypeIndex() >= 0 ? field.getGenericType() : null;
        if (annotations.isEmpty() && genericType == null) {
            builder.nullConst(fieldReflectionStruct.structure().getReference());
            return;
        }

        if (fieldReflectionStruct.annotationsIndex() >= 0) {
            generateAnnotations(builder, annotations);
        }
        if (fieldReflectionStruct.genericTypeIndex() >= 0) {
            var cls = classes.get(field.getOwnerName());
            generateGenericType(builder, cls, null, genericType);
        }

        builder.structNew(fieldReflectionStruct.structure());
    }

    private void generateMethods(WasmInstructionBuilder builder, String className,
            Collection<? extends MethodDescriptor> methods) {
        var methodInfoStruct = classInfoProvider.reflectionTypes().methodInfo();
        if (methods.isEmpty()) {
            builder.nullConst(methodInfoStruct.array().getReference());
            return;
        }

        var cls = classes.get(className);
        if (cls == null || cls.getMethods().isEmpty()) {
            builder.nullConst(methodInfoStruct.array().getReference());
            return;
        }

        var callerType = methodInfoStruct.callerType();

        var count = 0;
        var skipPrivates = ReflectionDependencyListener.shouldSkipPrivates(cls);

        for (var method : cls.getMethods()) {
            if (skipPrivates) {
                if (method.getLevel() == AccessLevel.PRIVATE || method.getLevel() == AccessLevel.PACKAGE_PRIVATE) {
                    continue;
                }
            }
            if (method.getName().equals("<clinit>") || !methods.contains(method.getDescriptor())) {
                continue;
            }

            if (methodInfoStruct.nameIndex() >= 0) {
                var nameStr = strings.getStringConstant(method.getName());
                builder.getGlobal(nameStr.global);
            }

            if (methodInfoStruct.modifiersIndex() >= 0) {
                var modifiers = ElementModifier.asModifiersInfo(method.readModifiers(), method.getLevel());
                builder.i32Const(modifiers);
            }

            if (methodInfoStruct.returnTypeIndex() >= 0) {
                generateDerivedClass(builder, method.getResultType());
            }

            if (methodInfoStruct.parameterTypesIndex() >= 0) {
                var paramTypes = method.getParameterTypes();
                var derivedClassInfoStruct = classInfoProvider.reflectionTypes().derivedClassInfo();
                if (paramTypes.length == 0) {
                    builder.nullConst(derivedClassInfoStruct.array().getReference());
                } else {
                    for (var param : paramTypes) {
                        generateDerivedClass(builder, param);
                    }
                    builder.arrayNewFixed(derivedClassInfoStruct.array(), paramTypes.length);
                }
            }

            if (methodInfoStruct.checkedExceptionTypesIndex() >= 0) {
                var thrownTypes = method.getThrownTypes();
                var derivedClassInfoStruct = classInfoProvider.reflectionTypes().derivedClassInfo();
                if (thrownTypes == null || thrownTypes.isEmpty()) {
                    builder.nullConst(derivedClassInfoStruct.array().getReference());
                } else {
                    for (var thrownType : thrownTypes) {
                        generateDerivedClass(builder, ValueType.object(thrownType));
                    }
                    builder.arrayNewFixed(derivedClassInfoStruct.array(), thrownTypes.size());
                }
            }

            if (methodInfoStruct.callerIndex() >= 0) {
                if (reflection.isCalled(method.getReference())) {
                    var caller = generateCaller(method);
                    builder.funcRef(caller);
                } else {
                    builder.nullConst(callerType.getReference());
                }
            }

            if (methodInfoStruct.reflectionIndex() >= 0) {
                generateMethodReflection(builder, method);
            }

            builder.structNew(methodInfoStruct.structure());
            ++count;
        }

        builder.arrayNewFixed(methodInfoStruct.array(), count);
    }

    private void generateMethodReflection(WasmInstructionBuilder builder, MethodReader method) {
        var reflectionTypes = classInfoProvider.reflectionTypes();
        var methodReflectionStruct = reflectionTypes.methodReflectionInfo();
        var annotations = methodReflectionStruct.annotationsIndex() >= 0
                ? AnnotationGenerationHelper.collectRuntimeAnnotations(classes, method.getAnnotations().all())
                : List.<AnnotationReader>of();
        var genericReturnType = methodReflectionStruct.genericReturnTypeIndex() >= 0
                ? method.getGenericResultType()
                : null;

        GenericValueType[] genericParameterTypes = null;
        List<List<AnnotationReader>> paramAnnotations = null;
        if (methodReflectionStruct.parameterInfosIndex() >= 0) {
            var paramInfoStruct = reflectionTypes.parameterInfo();
            if (paramInfoStruct.genericTypeIndex() >= 0) {
                genericParameterTypes = method.getGenericParameterTypes();
            }
            if (paramInfoStruct.annotationsIndex() >= 0) {
                paramAnnotations = collectParamAnnotations(method);
            }
        }

        var typeParameters = methodReflectionStruct.typeParametersIndex() >= 0
                ? method.getTypeParameters()
                : null;
        var hasNonTrivialParams = computeHasNonTrivialParams(method, genericParameterTypes, paramAnnotations);

        if (annotations.isEmpty() && genericReturnType == null && !hasNonTrivialParams
                && (typeParameters == null || typeParameters.length == 0)) {
            builder.nullConst(methodReflectionStruct.structure().getReference());
            return;
        }

        var cls = classes.get(method.getOwnerName());

        if (methodReflectionStruct.genericReturnTypeIndex() >= 0) {
            if (genericReturnType != null) {
                generateGenericType(builder, cls, method, genericReturnType);
            } else {
                builder.nullConst(WasmType.STRUCT);
            }
        }

        if (methodReflectionStruct.parameterInfosIndex() >= 0) {
            if (hasNonTrivialParams) {
                generateParameterInfos(builder, cls, method, genericParameterTypes, paramAnnotations);
            } else {
                builder.nullConst(reflectionTypes.parameterInfo().array().getReference());
            }
        }

        if (methodReflectionStruct.annotationsIndex() >= 0) {
            generateAnnotations(builder, annotations);
        }

        if (methodReflectionStruct.typeParametersIndex() >= 0) {
            if (typeParameters == null || typeParameters.length == 0) {
                builder.nullConst(reflectionTypes.typeVariableInfo().array().getReference());
            } else {
                generateTypeParameters(builder, typeParameters, cls, method);
            }
        }

        builder.structNew(methodReflectionStruct.structure());
    }

    private boolean computeHasNonTrivialParams(MethodReader method, GenericValueType[] genericParameterTypes,
            List<List<AnnotationReader>> paramAnnotations) {
        if (genericParameterTypes != null) {
            for (var i = 0; i < genericParameterTypes.length; ++i) {
                var paramType = genericParameterTypes[i];
                if (!paramType.canBeRepresentedAsRaw() || !paramType.asValueType().equals(method.parameterType(i))) {
                    return true;
                }
            }
        }
        if (paramAnnotations != null) {
            for (var annots : paramAnnotations) {
                if (!annots.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<List<AnnotationReader>> collectParamAnnotations(MethodReader method) {
        var paramAnnots = method.getParameterAnnotations();
        if (paramAnnots == null) {
            return null;
        }
        var result = new ArrayList<List<AnnotationReader>>();
        var hasAny = false;
        for (AnnotationContainerReader container : paramAnnots) {
            var annots = AnnotationGenerationHelper.collectRuntimeAnnotations(classes, container.all());
            result.add(annots);
            if (!annots.isEmpty()) {
                hasAny = true;
            }
        }
        return hasAny ? result : null;
    }

    private void generateParameterInfos(WasmInstructionBuilder builder, ClassReader cls, MethodReader method,
            GenericValueType[] genericParameterTypes, List<List<AnnotationReader>> paramAnnotations) {
        var reflectionTypes = classInfoProvider.reflectionTypes();
        var paramInfoStruct = reflectionTypes.parameterInfo();
        var count = method.parameterCount();

        for (var i = 0; i < count; ++i) {
            if (paramInfoStruct.annotationsIndex() >= 0) {
                var annots = paramAnnotations != null && i < paramAnnotations.size()
                        ? paramAnnotations.get(i) : List.<AnnotationReader>of();
                generateAnnotations(builder, annots);
            }
            if (paramInfoStruct.genericTypeIndex() >= 0) {
                var hasNonTrivialType = genericParameterTypes != null && i < genericParameterTypes.length;
                if (hasNonTrivialType) {
                    var paramType = genericParameterTypes[i];
                    hasNonTrivialType = !paramType.canBeRepresentedAsRaw()
                            || !paramType.asValueType().equals(method.parameterType(i));
                    if (hasNonTrivialType) {
                        generateGenericType(builder, cls, method, paramType);
                    } else {
                        builder.nullConst(WasmType.STRUCT);
                    }
                } else {
                    builder.nullConst(WasmType.STRUCT);
                }
            }
            builder.structNew(paramInfoStruct.structure());
        }
        builder.arrayNewFixed(paramInfoStruct.array(), count);
    }

    private void generateAnnotations(WasmInstructionBuilder builder, List<AnnotationReader> annotations) {
        var annotationInfoStruct = classInfoProvider.reflectionTypes().annotationInfo();

        if (annotations == null || annotations.isEmpty()) {
            builder.nullConst(annotationInfoStruct.array().getReference());
            return;
        }

        for (var annotation : annotations) {
            generateAnnotation(builder, annotation);
            var dataStruct = classInfoProvider.reflectionTypes().annotationData(annotation.getType());
            dataStruct.constructor().setReferenced(true);
            builder.funcRef(dataStruct.constructor());
            builder.structNew(annotationInfoStruct.structure());
        }
        builder.arrayNewFixed(annotationInfoStruct.array(), annotations.size());
    }

    private void generateAnnotation(WasmInstructionBuilder builder, AnnotationReader annotation) {
        var struct = classInfoProvider.reflectionTypes().annotationData(annotation.getType());
        for (var field : struct.fields()) {
            var value = annotation.getValue(field.name);
            if (value == null) {
                value = field.defaultValue;
            }
            generateAnnotationValue(builder, value, field.type);
        }
        builder.structNew(struct.structure());
    }

    private void generateAnnotationValue(WasmInstructionBuilder builder, AnnotationValue value, ValueType type) {
        switch (value.getType()) {
            case AnnotationValue.BOOLEAN:
                builder.i32Const(value.getBoolean() ? 1 : 0);
                return;
            case AnnotationValue.BYTE:
                builder.i32Const(value.getByte());
                return;
            case AnnotationValue.SHORT:
                builder.i32Const(value.getShort());
                return;
            case AnnotationValue.CHAR:
                builder.i32Const(value.getChar());
                return;
            case AnnotationValue.INT:
                builder.i32Const(value.getInt());
                return;
            case AnnotationValue.LONG:
                builder.i64Const(value.getLong());
                return;
            case AnnotationValue.FLOAT:
                builder.f32Const(value.getFloat());
                return;
            case AnnotationValue.DOUBLE:
                builder.f64Const(value.getDouble());
                return;
            case AnnotationValue.STRING:
                builder.getGlobal(strings.getStringConstant(value.getString()).global);
                return;
            case AnnotationValue.CLASS:
                generateDerivedClass(builder, value.getJavaClass());
                return;
            case AnnotationValue.ANNOTATION:
                generateAnnotation(builder, value.getAnnotation());
                return;
            case AnnotationValue.ENUM: {
                var enumClass = classes.get(value.getEnumValue().getClassName());
                if (enumClass == null) {
                    builder.i32Const(-1);
                    return;
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
                builder.i32Const(index);
                return;
            }
            case AnnotationValue.LIST: {
                var itemType = ((ValueType.Array) type).getItemType();
                var wasmItemType = classInfoProvider.reflectionTypes().typeForAnnotation(itemType, true);
                for (var item : value.getList()) {
                    generateAnnotationValue(builder, item, itemType);
                }
                builder.arrayNewFixed(classInfoProvider.reflectionTypes().arrayTypeOf(wasmItemType),
                        value.getList().size());
                return;
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

    private void generateDerivedClass(WasmInstructionBuilder builder, ValueType type) {
        var degree = 0;
        while (type instanceof ValueType.Array) {
            type = ((ValueType.Array) type).getItemType();
            ++degree;
        }
        builder
                .getGlobal(classInfoProvider.getClassInfo(type).getPointer())
                .i32Const(degree)
                .structNew(classInfoProvider.reflectionTypes().derivedClassInfo().structure());
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

        var body = function.getBody().builder();

        var classInfo = classInfoProvider.getClassInfo(field.getOwnerName());
        if (field.hasModifier(ElementModifier.STATIC)) {
            initClass(classInfo, field.getOwnerName(), function);
            var global = classInfoProvider.getStaticFieldLocation(field.getReference());
            body.getGlobal(global);
        } else {
            body.getLocal(thisVar).cast(classInfo.getType());
            WasmSignedType signedType = null;
            if (field.getType() instanceof ValueType.Primitive) {
                switch (((ValueType.Primitive) field.getType()).getKind()) {
                    case BYTE:
                    case SHORT:
                        signedType = WasmSignedType.SIGNED;
                        break;
                    case BOOLEAN:
                    case CHARACTER:
                        signedType = WasmSignedType.UNSIGNED;
                        break;
                    default:
                        break;
                }
            }
            body.structGet(classInfo.getStructure(), classInfoProvider.getFieldIndex(field.getReference()),
                    signedType);
        }

        boxIfNecessary(body, field.getType());

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

        var body = function.getBody().builder();

        var value = new WasmInstructionList().builder();
        value.getLocal(valueVar);
        unboxIfNecessary(value, field.getType());
        var classInfo = classInfoProvider.getClassInfo(field.getOwnerName());
        if (field.hasModifier(ElementModifier.STATIC)) {
            initClass(classInfo, field.getOwnerName(), function);
            var global = classInfoProvider.getStaticFieldLocation(field.getReference());
            body.transferFrom(value).setGlobal(global);
        } else {
            body
                    .getLocal(thisVar).cast(classInfo.getType())
                    .transferFrom(value)
                    .structSet(classInfo.getStructure(), classInfoProvider.getFieldIndex(field.getReference()));
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
        var async = asyncMethods.test(method.getReference());

        var dataField = objectArrayClass.getStructure().getFields()
                .get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);

        var thisVar = new WasmLocal(objectClass.getType(), "this");
        function.add(thisVar);
        var argsVar = new WasmLocal(objectArrayClass.getType(), "args");
        function.add(argsVar);
        var argsDataVar = new WasmLocal(dataField.getUnpackedType(), "argsData");
        var body = function.getBody().builder();
        var args = new WasmInstructionList().builder();

        var classInfo = classInfoProvider.getClassInfo(method.getOwnerName());
        WasmFunction callee = null;
        var virtual = false;
        if (method.hasModifier(ElementModifier.STATIC)) {
            initClass(classInfo, method.getOwnerName(), function);
            callee = functions.forStaticMethod(method.getReference());
        } else {
            virtual = !method.hasModifier(ElementModifier.FINAL) && method.getLevel() != AccessLevel.PRIVATE
                    && !method.getName().equals("<init>");
            body.getLocal(thisVar).cast(classInfo.getType());
            if (!virtual) {
                callee = functions.forInstanceMethod(method.getReference());
            }
        }

        if (method.parameterCount() > 0) {
            args
                    .getLocal(argsVar)
                    .structGet(objectArrayClass.getStructure(), dataField.getIndex());
            if (method.parameterCount() > 1) {
                function.add(argsDataVar);
                args.teeLocal(argsDataVar);
            }
        }

        var dataType = (WasmType.CompositeReference) dataField.getUnpackedType();
        var dataArray = (WasmArray) dataType.composite;
        for (var i = 0; i < method.parameterCount(); ++i) {
            if (i > 0) {
                args.getLocal(argsDataVar);
            }
            args.i32Const(i).arrayGet(dataArray);
            unboxIfNecessary(args, method.parameterType(i));
        }

        if (virtual) {
            var callGen = new WasmGCVirtualCallGenerator(virtualTables, classInfoProvider);
            var tempVars = new TemporaryVariablePool(function);
            var valueCache = new ValueCache(tempVars);
            callGen.generate(body, method.getReference(), async, valueCache, classInfo.getStructure().getReference(),
                    b -> b.transferFrom(args));
        } else {
            body.transferFrom(args).call(callee, async);
        }
        if (method.getResultType() == ValueType.VOID) {
            body.nullConst(objectClass.getType());
        } else {
            boxIfNecessary(body, method.getResultType());
        }

        if (async) {
            var transformation = new CoroutineTransformation(functionTypes, functions, classInfoProvider);
            transformation.transform(function);
        }

        return function;
    }

    private void initClass(WasmGCClassInfo classInfo, String className, WasmFunction function) {
        if (classInitInfo.isDynamicInitializer(className)
                && classInfo.getInitializerPointer() != null) {
            var initType = functionTypes.of(null);
            function.getBody().builder()
                    .getGlobal(classInfo.getInitializerPointer())
                    .callReference(initType);
        }
    }

    private void boxIfNecessary(WasmInstructionBuilder builder, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    box(builder, Boolean.class, type);
                    break;
                case BYTE:
                    box(builder, Byte.class, type);
                    break;
                case SHORT:
                    box(builder, Short.class, type);
                    break;
                case CHARACTER:
                    box(builder, Character.class, type);
                    break;
                case INTEGER:
                    box(builder, Integer.class, type);
                    break;
                case LONG:
                    box(builder, Long.class, type);
                    break;
                case FLOAT:
                    box(builder, Float.class, type);
                    break;
                case DOUBLE:
                    box(builder, Double.class, type);
                    break;
            }
        }
    }

    private void box(WasmInstructionBuilder builder, Class<?> wrapperType, ValueType sourceType) {
        var method = new MethodReference(wrapperType.getName(), "valueOf", sourceType,
                ValueType.object(wrapperType.getName()));
        builder.call(functions.forStaticMethod(method));
    }

    private void unboxIfNecessary(WasmInstructionBuilder builder, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    unbox(builder, boolean.class, Boolean.class);
                    return;
                case BYTE:
                    unbox(builder, byte.class, Byte.class);
                    return;
                case SHORT:
                    unbox(builder, short.class, Short.class);
                    return;
                case CHARACTER:
                    unbox(builder, char.class, Character.class);
                    return;
                case INTEGER:
                    unbox(builder, int.class, Integer.class);
                    return;
                case LONG:
                    unbox(builder, long.class, Long.class);
                    return;
                case FLOAT:
                    unbox(builder, float.class, Float.class);
                    return;
                case DOUBLE:
                    unbox(builder, double.class, Double.class);
                    return;
            }
        } else if (type instanceof ValueType.Object) {
            if (((ValueType.Object) type).getClassName().equals("java.lang.Object")) {
                return;
            }
        }
        var targetType = typeMapper.mapType(type);
        if (targetType == typeMapper.mapType(ValueType.object("java.lang.Object"))) {
            return;
        }
        builder.cast((WasmType.Reference) targetType);
    }

    private void unbox(WasmInstructionBuilder builder, Class<?> primitiveType, Class<?> wrapperType) {
        var method = new MethodReference(wrapperType.getName(), primitiveType.getName() + "Value",
                ValueType.parse(primitiveType));
        var function = functions.forInstanceMethod(method);
        builder
                .cast(classInfoProvider.getClassInfo(wrapperType.getName()).getType())
                .call(function);
    }

    private void generateTypeParameters(WasmInstructionBuilder builder, GenericTypeParameter[] params, ClassReader cls,
            MethodReader method) {
        var struct = classInfoProvider.reflectionTypes().typeVariableInfo();
        for (var param : params) {
            if (struct.nameIndex() >= 0) {
                builder.getGlobal(strings.getStringConstant(param.getName()).global);
            }
            if (struct.boundsIndex() >= 0) {
                var bounds = param.extractAllBounds();
                if (bounds.isEmpty()) {
                    builder.nullConst(classInfoProvider.reflectionTypes().genericTypeArray().getReference());
                } else {
                    generateTypeParameterBounds(builder, cls, method, bounds);
                }
            }
            builder.structNew(struct.structure());
        }
        builder.arrayNewFixed(struct.array(), params.length);
    }

    private void generateTypeParameterBounds(WasmInstructionBuilder builder, ClassReader cls, MethodReader method,
            List<GenericValueType.Reference> bounds) {
        for (var bound : bounds) {
            generateGenericType(builder, cls, method, bound);
        }
        builder.arrayNewFixed(classInfoProvider.reflectionTypes().genericTypeArray(), bounds.size());
    }

    private void generateGenericType(WasmInstructionBuilder builder, ClassReader contextClass,
            MethodReader contextMethod, GenericValueType type) {
        if (type instanceof GenericValueType.Object) {
            var objectType = (GenericValueType.Object) type;
            var args = objectType.getArguments();
            if (args.length == 0) {
                generateDerivedClass(builder, ValueType.object(objectType.getClassName()));
            } else {
                var clsInfo = classInfoProvider.getClassInfo(objectType.getFullClassName());
                var resultStruct = classInfoProvider.reflectionTypes().parameterizedTypeInfo();
                if (resultStruct.rawTypeIndex() >= 0) {
                    builder.getGlobal(clsInfo.getPointer());
                }
                if (resultStruct.actualTypeArgumentsIndex() >= 0) {
                    var arrayType = classInfoProvider.reflectionTypes().genericTypeArray();
                    for (var arg : args) {
                        generateGenericType(builder, contextClass, contextMethod, arg);
                    }
                    builder.arrayNewFixed(arrayType, args.length);
                }
                if (resultStruct.ownerTypeIndex() >= 0) {
                    var ownerType = objectType.getParent();
                    if (ownerType != null) {
                        generateGenericType(builder, contextClass, contextMethod, ownerType);
                    } else {
                        builder.nullConst(WasmType.STRUCT);
                    }
                }
                builder.structNew(resultStruct.structure());
            }
        } else if (type instanceof GenericValueType.Variable) {
            var typeVar = (GenericValueType.Variable) type;
            var level = 0;
            if (contextMethod != null) {
                var genericParameters = contextMethod.getTypeParameters();
                for (var i = 0; i < genericParameters.length; i++) {
                    var param = genericParameters[i];
                    if (param.getName().equals(typeVar.getName())) {
                        typeVariableRef(builder, 0, i);
                        return;
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
                            typeVariableRef(builder, level, i);
                            return;
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
                generateGenericType(builder, contextClass, contextMethod, arrayType.getItemType());
                builder.structNew(struct.structure());
            } else {
                generateDerivedClass(builder, nonGenericType);
            }
        } else if (type instanceof GenericValueType.Primitive) {
            var primitiveType = (GenericValueType.Primitive) type;
            generateDerivedClass(builder, ValueType.primitive(primitiveType.getKind()));
        } else if (type instanceof GenericValueType.Void) {
            generateDerivedClass(builder, ValueType.VOID);
        } else {
            throw new IllegalArgumentException("Unsupported generic type: " + type);
        }
    }

    private void typeVariableRef(WasmInstructionBuilder builder, int level, int index) {
        builder
                .i32Const(level)
                .i32Const(index)
                .structNew(classInfoProvider.reflectionTypes().typeVariableReference().structure());
    }

    private void generateGenericType(WasmInstructionBuilder builder, ClassReader contextClass,
            MethodReader contextMethod, GenericValueType.Argument arg) {
        switch (arg.getKind()) {
            case INVARIANT:
                generateGenericType(builder, contextClass, contextMethod, arg.getValue());
                return;
            case ANY: {
                var struct = classInfoProvider.reflectionTypes().wildcardTypeInfo();
                builder
                        .i32Const(2)
                        .nullConst(WasmType.STRUCT)
                        .structNew(struct.structure());
                return;
            }
            case COVARIANT: {
                var struct = classInfoProvider.reflectionTypes().wildcardTypeInfo();
                builder.i32Const(0);
                generateGenericType(builder, contextClass, contextMethod, arg.getValue());
                builder.structNew(struct.structure());
                return;
            }
            case CONTRAVARIANT: {
                var struct = classInfoProvider.reflectionTypes().wildcardTypeInfo();
                builder.i32Const(1);
                generateGenericType(builder, contextClass, contextMethod, arg.getValue());
                builder.structNew(struct.structure());
                return;
            }
            default:
                throw new IllegalArgumentException("Unsupported generic type: " + arg.getKind());
        }
    }

    private void generateInnerClasses(WasmInstructionBuilder builder, List<? extends String> innerClasses) {
        if (innerClasses.isEmpty()) {
            builder.nullConst(classInfoProvider.reflectionTypes().classInfo().array().getReference());
            return;
        }
        for (var innerClass : innerClasses) {
            builder.getGlobal(classInfoProvider.getClassInfo(innerClass).getPointer());
        }
        builder.arrayNewFixed(classInfoProvider.reflectionTypes().classInfo().array(), innerClasses.size());
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
