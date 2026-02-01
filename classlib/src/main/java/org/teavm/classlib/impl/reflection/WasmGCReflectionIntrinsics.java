/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.classlib.impl.reflection;

import static org.teavm.reflection.ReflectionMethods.EXECUTABLE_GET_GENERIC_PARAMETER_TYPES;
import static org.teavm.reflection.ReflectionMethods.FIELD_GET_GENERIC_TYPE;
import static org.teavm.reflection.ReflectionMethods.METHOD_GET_GENERIC_RETURN_TYPE;
import static org.teavm.reflection.ReflectionMethods.TYPE_VAR_GET_BOUNDS;
import java.util.ArrayList;
import java.util.Set;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.generate.gc.annotations.WasmGCAnnotationsHelper;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfo;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCReflectionProvider;
import org.teavm.backend.wasm.generate.gc.methods.WasmGCVirtualCallGenerator;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayGet;
import org.teavm.backend.wasm.model.expression.WasmArrayLength;
import org.teavm.backend.wasm.model.expression.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBreak;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmDrop;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmFunctionReference;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
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
import org.teavm.backend.wasm.transformation.gc.CoroutineTransformation;
import org.teavm.reflection.ReflectionDependencyListener;
import org.teavm.interop.Address;
import org.teavm.model.AccessLevel;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.runtime.Fiber;

public class WasmGCReflectionIntrinsics implements WasmGCIntrinsic {
    private ReflectionDependencyListener reflection;
    private final Set<ValueType> typesToSkip = Set.of(
            ValueType.parse(Address.class),
            ValueType.parse(Fiber.PlatformObject.class),
            ValueType.parse(Fiber.PlatformFunction.class)
    );

    private WasmFunction initReflectionFunction;
    private WasmFunction wrapAnnotationsFunction;

    public WasmGCReflectionIntrinsics(ReflectionDependencyListener reflection) {
        this.reflection = reflection;
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getClassName()) {
            case "org.teavm.classlib.impl.reflection.ObjectList": {
                var arrayInfo = context.classInfoProvider().getClassInfo(ValueType.parse(Object[].class));
                var objectInfo = context.classInfoProvider().getClassInfo("java.lang.Object");
                var arrayDataType = context.classInfoProvider().getObjectArrayType();
                var classClass = context.classInfoProvider().getClassInfo("java.lang.Class");
                var structNew = new WasmStructNew(arrayInfo.getStructure());
                var arrayCls = new WasmCall(context.classInfoProvider().getGetArrayClassFunction(),
                        new WasmGetGlobal(objectInfo.getPointer()));
                var arrayVt = new WasmStructGet(classClass.getStructure(), arrayCls,
                        context.classInfoProvider().getClassVtFieldOffset());
                structNew.getInitializers().add(arrayVt);
                structNew.getInitializers().add(new WasmNullConstant(WasmType.Reference.EQ));
                structNew.getInitializers().add(new WasmCast(context.generate(invocation.getArguments().get(0)),
                        arrayDataType.getNonNullReference()));
                return structNew;
            }
            case "org.teavm.runtime.reflect.FieldInfo":
                switch (invocation.getMethod().getName()) {
                    case "name":
                        return fieldInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_NAME);
                    case "modifiers":
                        return fieldInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_MODIFIERS);
                    case "accessLevel":
                        return fieldInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_ACCESS);
                    case "annotations":
                        return wrapAnnotations(
                                fieldInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_ANNOTATIONS),
                                context);
                    case "type":
                        return fieldInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_TYPE);
                    case "genericType":
                        return fieldInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_GENERIC_TYPE);
                    case "reader":
                        return fieldInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_READER);
                    case "writer":
                        return fieldInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_WRITER);
                    default:
                        break;
                }
                break;
            case "org.teavm.classlib.impl.reflection.FieldInfoList":
                switch (invocation.getMethod().getName()) {
                    case "count":
                        return new WasmArrayLength(context.generate(invocation.getArguments().get(0)));
                    case "get": {
                        var arg = context.generate(invocation.getArguments().get(0));
                        var index = context.generate(invocation.getArguments().get(1));
                        var arrayType = context.classInfoProvider().reflection().getReflectionFieldArrayType();
                        return new WasmArrayGet(arrayType, arg, index);
                    }
                    default:
                        break;
                }
                break;
            case "org.teavm.runtime.reflect.MethodInfo":
                switch (invocation.getMethod().getName()) {
                    case "name":
                        return methodInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_NAME);
                    case "modifiers":
                        return methodInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_MODIFIERS);
                    case "accessLevel":
                        return methodInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_ACCESS);
                    case "annotations":
                        return wrapAnnotations(
                                methodInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_ANNOTATIONS),
                                context);
                    case "returnType":
                        return methodInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_RETURN_TYPE);
                    case "genericReturnType":
                        return methodInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_GENERIC_RETURN_TYPE);
                    case "parameterTypes":
                        return methodInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_PARAMETER_TYPES);
                    case "genericParameterTypes":
                        return methodInfoCall(invocation, context,
                                WasmGCReflectionProvider.FIELD_GENERIC_PARAMETER_TYPES);
                    case "caller":
                        return methodInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_CALLER);
                    case "typeParameters":
                        return methodInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_TYPE_PARAMETERS);
                    default:
                        break;
                }
                break;
            case "org.teavm.classlib.impl.reflection.MethodInfoList":
                switch (invocation.getMethod().getName()) {
                    case "count":
                        return new WasmArrayLength(context.generate(invocation.getArguments().get(0)));
                    case "get": {
                        var arg = context.generate(invocation.getArguments().get(0));
                        var index = context.generate(invocation.getArguments().get(1));
                        var arrayType = context.classInfoProvider().reflection().getReflectionMethodArrayType();
                        return new WasmArrayGet(arrayType, arg, index);
                    }
                    default:
                        break;
                }
                break;
            case "org.teavm.classlib.impl.reflection.ClassList":
                switch (invocation.getMethod().getName()) {
                    case "count":
                        return new WasmArrayLength(context.generate(invocation.getArguments().get(0)));
                    case "get": {
                        var arg = context.generate(invocation.getArguments().get(0));
                        var index = context.generate(invocation.getArguments().get(1));
                        var arrayType = context.classInfoProvider().reflection().getClassArrayType();
                        return new WasmArrayGet(arrayType, arg, index);
                    }
                    default:
                        break;
                }
                break;
            case "org.teavm.runtime.reflect.FieldReader": {
                var fn = context.generate(invocation.getArguments().get(0));
                var arg = context.generate(invocation.getArguments().get(1));
                var objectType = context.classInfoProvider().getClassInfo("java.lang.Object").getType();
                var type = context.functionTypes().of(objectType, objectType);
                return new WasmCallReference(fn, type, arg);
            }
            case "org.teavm.runtime.reflect.FieldWriter": {
                var fn = context.generate(invocation.getArguments().get(0));
                var arg = context.generate(invocation.getArguments().get(1));
                var value = context.generate(invocation.getArguments().get(2));
                var objectType = context.classInfoProvider().getClassInfo("java.lang.Object").getType();
                var type = context.functionTypes().of(null, objectType, objectType);
                return new WasmCallReference(fn, type, arg, value);
            }
            case "org.teavm.runtime.reflect.MethodCaller": {
                var fn = context.generate(invocation.getArguments().get(0));
                var instanceArg = context.generate(invocation.getArguments().get(1));
                var paramsArg = context.generate(invocation.getArguments().get(2));
                var objectType = context.classInfoProvider().getClassInfo("java.lang.Object").getType();
                var objArrayType = context.classInfoProvider().getClassInfo(ValueType.arrayOf(
                        ValueType.object("java.lang.Object"))).getType();
                var type = context.functionTypes().of(objectType, objectType, objArrayType);
                return new WasmCallReference(fn, type, instanceArg, paramsArg);
            }
            case "java.lang.Class":
                switch (invocation.getMethod().getName()) {
                    case "createMetadata":
                        return new WasmCall(getInitReflectionFunction(context));
                    case "newInstanceImpl": {
                        var classClass = context.classInfoProvider().getClassInfo("java.lang.Class");
                        var objectClass = context.classInfoProvider().getClassInfo("java.lang.Object");
                        var functionType = context.functionTypes().of(objectClass.getType());
                        var arg = context.generate(invocation.getArguments().get(0));
                        var instantiator = new WasmStructGet(classClass.getStructure(), arg,
                                context.classInfoProvider().getClassInstantiatorOffset());
                        var outerBlock = new WasmBlock(false);
                        outerBlock.setType(objectClass.getType().asBlock());
                        var innerBlock = new WasmBlock(false);
                        innerBlock.setType(functionType.getReference().asBlock());
                        var nullBranch = new WasmNullBranch(WasmNullCondition.NOT_NULL, instantiator, innerBlock);
                        innerBlock.getBody().add(nullBranch);
                        var br = new WasmBreak(outerBlock);
                        br.setResult(new WasmNullConstant(objectClass.getType()));
                        innerBlock.getBody().add(br);
                        var call = new WasmCallReference(innerBlock, functionType);
                        outerBlock.getBody().add(call);
                        call.setSuspensionPoint(context.isAsync());
                        return outerBlock;
                    }
                }
                break;
        }
        throw new IllegalArgumentException();
    }

    private WasmExpression fieldInfoCall(InvocationExpr invocation, WasmGCIntrinsicContext context, int fieldIndex) {
        var arg = context.generate(invocation.getArguments().get(0));
        return new WasmStructGet(context.classInfoProvider().reflection().getReflectionFieldType(),
                arg, fieldIndex);
    }

    private WasmExpression methodInfoCall(InvocationExpr invocation, WasmGCIntrinsicContext context, int fieldIndex) {
        var arg = context.generate(invocation.getArguments().get(0));
        return new WasmStructGet(context.classInfoProvider().reflection().getReflectionMethodType(),
                arg, fieldIndex);
    }

    private WasmExpression wrapAnnotations(WasmExpression expression, WasmGCIntrinsicContext context) {
        return new WasmCall(getWrapAnnotationsFunction(context), expression);
    }

    private WasmFunction getWrapAnnotationsFunction(WasmGCIntrinsicContext context) {
        if (wrapAnnotationsFunction == null) {
            var annotArrayInfo = context.classInfoProvider().getClassInfo(ValueType.arrayOf(
                    ValueType.object("java.lang.annotation.Annotation")));
            var annotInfo = context.classInfoProvider().getClassInfo("java.lang.annotation.Annotation");
            var objectArray = annotArrayInfo.getType();
            var arrayDataType = context.classInfoProvider().getObjectArrayType().getReference();
            var classClass = context.classInfoProvider().getClassInfo("java.lang.Class");
            wrapAnnotationsFunction = new WasmFunction(context.functionTypes().of(objectArray, arrayDataType));
            wrapAnnotationsFunction.setName(context.names().topLevel("@teavm.wrapAnnotations"));
            context.module().functions.add(wrapAnnotationsFunction);
            var param = new WasmLocal(arrayDataType);
            wrapAnnotationsFunction.add(param);

            var nullCheck = new WasmBlock(false);
            nullCheck.setType(arrayDataType.composite.getNonNullReference().asBlock());
            nullCheck.getBody().add(new WasmNullBranch(WasmNullCondition.NOT_NULL,
                    new WasmGetLocal(param), nullCheck));
            nullCheck.getBody().add(new WasmReturn(new WasmNullConstant(objectArray)));
            wrapAnnotationsFunction.getBody().add(new WasmDrop(nullCheck));

            var structNew = new WasmStructNew(annotArrayInfo.getStructure());
            var arrayCls = new WasmCall(context.classInfoProvider().getGetArrayClassFunction(),
                    new WasmGetGlobal(annotInfo.getPointer()));
            var arrayVt = new WasmStructGet(classClass.getStructure(), arrayCls,
                    context.classInfoProvider().getClassVtFieldOffset());
            structNew.getInitializers().add(arrayVt);
            structNew.getInitializers().add(new WasmNullConstant(WasmType.Reference.EQ));
            structNew.getInitializers().add(new WasmCast(new WasmGetLocal(param),
                    arrayDataType.composite.getNonNullReference()));
            wrapAnnotationsFunction.getBody().add(structNew);
        }
        return wrapAnnotationsFunction;
    }

    private WasmFunction getInitReflectionFunction(WasmGCIntrinsicContext context) {
        if (initReflectionFunction == null) {
            initReflectionFunction = new WasmFunction(context.functionTypes().of(null));
            initReflectionFunction.setName(context.names().topLevel("@teavm.initReflection"));
            context.module().functions.add(initReflectionFunction);
            var helper = new WasmGCAnnotationsHelper(context.hierarchy().getClassSource(),
                    context.classInfoProvider(), context.strings());
            var genericsHelper = new WasmGCReflectionGenericsHelper(context, initReflectionFunction);
            /*if (context.dependency().getMethod(TYPE_VAR_CREATE) != null) {
                genericsHelper.initReflectionGenericsForClasses();
            }*/
            initReflectionFields(context, initReflectionFunction, helper, genericsHelper);
            initReflectionMethods(context, initReflectionFunction, helper, genericsHelper);
            initReflectionInstantiator(context, initReflectionFunction);
        }
        return initReflectionFunction;
    }

    private void initReflectionFields(WasmGCIntrinsicContext context, WasmFunction function,
            WasmGCAnnotationsHelper annotationsHelper, WasmGCReflectionGenericsHelper genericsHelper) {
        var wasmGcReflection = context.classInfoProvider().reflection();
        var classClass = context.classInfoProvider().getClassInfo("java.lang.Class");
        var objectClass = context.classInfoProvider().getClassInfo("java.lang.Object");
        var withGenericType = context.dependency().getMethod(FIELD_GET_GENERIC_TYPE) != null;

        for (var className : reflection.getClassesWithReflectableFields()) {
            var cls = context.hierarchy().getClassSource().get(className);
            if (cls == null || cls.getFields().isEmpty()) {
                continue;
            }
            var skipPrivates = ReflectionDependencyListener.shouldSkipPrivates(cls);

            var array = new WasmArrayNewFixed(wasmGcReflection.getReflectionFieldArrayType());
            var classInfo = context.classInfoProvider().getClassInfo(className);
            function.getBody().add(new WasmStructSet(
                    classClass.getStructure(),
                    new WasmGetGlobal(classInfo.getPointer()),
                    context.classInfoProvider().getClassFieldsOffset(),
                    array
            ));

            var accessibleFields = reflection.getAccessibleFields(className);
            for (var field : cls.getFields()) {
                if (skipPrivates) {
                    if (field.getLevel() == AccessLevel.PRIVATE || field.getLevel() == AccessLevel.PACKAGE_PRIVATE) {
                        continue;
                    }
                }
                var fieldInit = new WasmStructNew(wasmGcReflection.getReflectionFieldType());
                array.getElements().add(fieldInit);

                var nameStr = context.strings().getStringConstant(field.getName());
                fieldInit.getInitializers().add(new WasmGetGlobal(nameStr.global));

                fieldInit.getInitializers().add(new WasmInt32Constant(ElementModifier.pack(field.readModifiers())));

                fieldInit.getInitializers().add(new WasmInt32Constant(field.getLevel().ordinal()));

                var fieldAnnotations = annotationsHelper.generateAnnotations(field.getAnnotations().all());
                fieldInit.getInitializers().add(fieldAnnotations != null
                        ? fieldAnnotations
                        : new WasmNullConstant(context.classInfoProvider().getObjectArrayType().getReference()));

                fieldInit.getInitializers().add(renderType(context, field.getType()));
                if (withGenericType && field.getGenericType() != null) {
                    fieldInit.getInitializers().add(genericsHelper.writeGenericType(cls, null, field.getGenericType()));
                } else {
                    fieldInit.getInitializers().add(new WasmNullConstant(objectClass.getType()));
                }

                if (accessibleFields != null && accessibleFields.contains(field.getName())
                        && reflection.isGetReached() && reflection.isRead(field.getReference())) {
                    var getter = generateGetter(context, field);
                    fieldInit.getInitializers().add(new WasmFunctionReference(getter));
                } else {
                    var getterType = context.functionTypes().of(objectClass.getType(), objectClass.getType());
                    fieldInit.getInitializers().add(new WasmNullConstant(getterType.getReference()));
                }
                if (accessibleFields != null && accessibleFields.contains(field.getName())
                        && reflection.isSetReached() && reflection.isWritten(field.getReference())) {
                    var setter = generateSetter(context, field);
                    fieldInit.getInitializers().add(new WasmFunctionReference(setter));
                } else {
                    var setterType = context.functionTypes().of(null, objectClass.getType(), objectClass.getType());
                    fieldInit.getInitializers().add(new WasmNullConstant(setterType.getReference()));
                }

            }
        }
    }

    private void initReflectionMethods(WasmGCIntrinsicContext context, WasmFunction function,
            WasmGCAnnotationsHelper annotationsHelper, WasmGCReflectionGenericsHelper genericsHelper) {
        var wasmGcReflection = context.classInfoProvider().reflection();
        var classClass = context.classInfoProvider().getClassInfo("java.lang.Class");
        var objectClass = context.classInfoProvider().getClassInfo("java.lang.Object");
        var objectArrayClass = context.classInfoProvider().getClassInfo(ValueType.arrayOf(
                ValueType.object("java.lang.Object")));
        var objectArrayType = context.classInfoProvider().getObjectArrayType();
        var callerType = context.functionTypes().of(objectClass.getType(), objectClass.getType(),
                objectArrayClass.getType());
        var withBounds = context.dependency().getMethod(TYPE_VAR_GET_BOUNDS) != null;
        var withGenericReturn = context.dependency().getMethod(METHOD_GET_GENERIC_RETURN_TYPE) != null;
        var withGenericParams = context.dependency().getMethod(EXECUTABLE_GET_GENERIC_PARAMETER_TYPES) != null;

        for (var className : reflection.getClassesWithReflectableMethods()) {
            var cls = context.hierarchy().getClassSource().get(className);
            if (cls == null || cls.getMethods().isEmpty()) {
                continue;
            }
            var skipPrivates = ReflectionDependencyListener.shouldSkipPrivates(cls);

            var array = new WasmArrayNewFixed(wasmGcReflection.getReflectionMethodArrayType());
            var classInfo = context.classInfoProvider().getClassInfo(className);
            function.getBody().add(new WasmStructSet(
                    classClass.getStructure(),
                    new WasmGetGlobal(classInfo.getPointer()),
                    context.classInfoProvider().getClassMethodsOffset(),
                    array
            ));

            var accessibleMethods = reflection.getAccessibleMethods(className);
            for (var method : cls.getMethods()) {
                if (skipPrivates) {
                    if (method.getLevel() == AccessLevel.PRIVATE || method.getLevel() == AccessLevel.PACKAGE_PRIVATE) {
                        continue;
                    }
                }
                if (method.getName().equals("<clinit>")) {
                    continue;
                }
                var methodInit = new WasmStructNew(wasmGcReflection.getReflectionMethodType());
                array.getElements().add(methodInit);

                var nameStr = context.strings().getStringConstant(method.getName());
                methodInit.getInitializers().add(new WasmGetGlobal(nameStr.global));

                methodInit.getInitializers().add(new WasmInt32Constant(ElementModifier.pack(method.readModifiers())));

                methodInit.getInitializers().add(new WasmInt32Constant(method.getLevel().ordinal()));

                var methodAnnotations = annotationsHelper.generateAnnotations(method.getAnnotations().all());
                methodInit.getInitializers().add(methodAnnotations != null
                        ? methodAnnotations
                        : new WasmNullConstant(objectArrayType.getReference()));

                methodInit.getInitializers().add(renderType(context, method.getResultType()));

                if (withGenericReturn && method.getGenericResultType() != null) {
                    methodInit.getInitializers().add(genericsHelper.writeGenericType(cls, method,
                            method.getGenericResultType()));
                } else {
                    methodInit.getInitializers().add(new WasmNullConstant(objectClass.getType()));
                }

                var parametersArray = new WasmArrayNewFixed(wasmGcReflection.getClassArrayType());
                for (var param : method.getParameterTypes()) {
                    parametersArray.getElements().add(renderType(context, param));
                }
                methodInit.getInitializers().add(parametersArray);

                var hasGenericParameters = false;
                if (withGenericParams) {
                    for (var i = 0; i < method.parameterCount(); ++i) {
                        if (method.genericParameterType(i) != null) {
                            hasGenericParameters = true;
                            break;
                        }
                    }
                }
                if (hasGenericParameters) {
                    var genericParametersArray = new WasmArrayNewFixed(objectArrayType);
                    for (var i = 0; i < method.parameterCount(); ++i) {
                        var paramType = method.genericParameterType(i);
                        if (paramType != null) {
                            genericParametersArray.getElements().add(genericsHelper.writeGenericType(
                                    cls, method, paramType));
                        } else {
                            genericParametersArray.getElements().add(new WasmNullConstant(objectClass.getType()));
                        }
                    }
                    methodInit.getInitializers().add(genericParametersArray);
                } else {
                    methodInit.getInitializers().add(new WasmNullConstant(objectArrayType.getReference()));
                }

                if (accessibleMethods != null && accessibleMethods.contains(method.getDescriptor())
                        && reflection.isCallReached() && reflection.isCalled(method.getReference())) {
                    var caller = generateCaller(context, method);
                    methodInit.getInitializers().add(new WasmFunctionReference(caller));
                } else {
                    methodInit.getInitializers().add(new WasmNullConstant(callerType.getReference()));
                }

                var typeParameters = method.getTypeParameters();
                /*if (typeParameters != null && typeParameters.length > 0
                        && context.dependency().getMethod(TYPE_VAR_CREATE) != null) {
                    methodInit.getInitializers().add(genericsHelper.writeTypeParameters(typeParameters,
                            cls, method, withBounds));
                } else {
                    methodInit.getInitializers().add(new WasmNullConstant(objectArrayType.getReference()));
                }*/
            }
        }
    }

    private void initReflectionInstantiator(WasmGCIntrinsicContext context, WasmFunction function) {
        var dep = context.dependency().getMethod(new MethodReference(Class.class, "newInstance", Object.class));
        if (dep == null || !dep.isUsed()) {
            return;
        }
        var node = dep.getVariable(0).getClassValueNode();
        if (node == null) {
            return;
        }

        var classClass = context.classInfoProvider().getClassInfo("java.lang.Class");
        for (var type : node.getTypes()) {
            if (!(type instanceof ValueType.Object)) {
                continue;
            }
            if (typesToSkip.contains(type)) {
                continue;
            }
            var cls = context.hierarchy().getClassSource().get(((ValueType.Object) type).getClassName());
            if (cls == null) {
                continue;
            }
            if (cls.hasModifier(ElementModifier.ABSTRACT)) {
                continue;
            }
            var method = cls.getMethod(new MethodDescriptor("<init>", void.class));
            if (method == null || (method.getProgram() == null && !method.hasModifier(ElementModifier.NATIVE))) {
                continue;
            }

            var classInfo = context.classInfoProvider().getClassInfo(cls.getName());
            if (classInfo.getStructure() == null) {
                continue;
            }
            var instantiator = generateInstantiator(context, method);

            function.getBody().add(new WasmStructSet(
                    classClass.getStructure(),
                    new WasmGetGlobal(classInfo.getPointer()),
                            context.classInfoProvider().getClassInstantiatorOffset(),
                    new WasmFunctionReference(instantiator)
            ));
        }
    }


    private WasmFunction generateGetter(WasmGCIntrinsicContext context, FieldReader field) {
        var objectClass = context.classInfoProvider().getClassInfo("java.lang.Object");
        var getterType = context.functionTypes().of(objectClass.getType(), objectClass.getType());
        var function = new WasmFunction(getterType);
        function.setName(context.names().topLevel(context.names().suggestForStaticField(field.getReference())
                + "@getter"));
        context.module().functions.add(function);
        function.setReferenced(true);

        var thisVar = new WasmLocal(objectClass.getType(), "this");
        function.add(thisVar);

        WasmExpression result;
        var classInfo = context.classInfoProvider().getClassInfo(field.getOwnerName());
        if (field.hasModifier(ElementModifier.STATIC)) {
            if (context.classInitInfo().isDynamicInitializer(field.getOwnerName())
                    && classInfo.getInitializerPointer() != null) {
                var initRef = new WasmGetGlobal(classInfo.getInitializerPointer());
                var initType = context.functionTypes().of(null);
                function.getBody().add(new WasmCallReference(initRef, initType));
            }
            var global = context.classInfoProvider().getStaticFieldLocation(field.getReference());
            result = new WasmGetGlobal(global);
        } else {
            var castInstance = new WasmCast(new WasmGetLocal(thisVar), classInfo.getType());
            var structGet = new WasmStructGet(classInfo.getStructure(), castInstance,
                    context.classInfoProvider().getFieldIndex(field.getReference()));
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

        function.getBody().add(boxIfNecessary(context, result, field.getType()));

        return function;
    }

    private WasmFunction generateSetter(WasmGCIntrinsicContext context, FieldReader field) {
        var objectClass = context.classInfoProvider().getClassInfo("java.lang.Object");
        var setterType = context.functionTypes().of(null, objectClass.getType(), objectClass.getType());
        var function = new WasmFunction(setterType);
        function.setName(context.names().topLevel(context.names().suggestForStaticField(field.getReference())
                + "@setter"));
        context.module().functions.add(function);
        function.setReferenced(true);

        var thisVar = new WasmLocal(objectClass.getType(), "this");
        function.add(thisVar);
        var valueVar = new WasmLocal(objectClass.getType(), "value");
        function.add(valueVar);

        var value = unboxIfNecessary(context, new WasmGetLocal(valueVar), field.getType());
        var classInfo = context.classInfoProvider().getClassInfo(field.getOwnerName());
        if (field.hasModifier(ElementModifier.STATIC)) {
            initClass(context, classInfo, field.getOwnerName(), function);
            var global = context.classInfoProvider().getStaticFieldLocation(field.getReference());
            function.getBody().add(new WasmSetGlobal(global, value));
        } else {
            var castInstance = new WasmCast(new WasmGetLocal(thisVar), classInfo.getType());
            var structSet = new WasmStructSet(classInfo.getStructure(), castInstance,
                    context.classInfoProvider().getFieldIndex(field.getReference()), value);
            function.getBody().add(structSet);
        }

        return function;
    }

    private WasmFunction generateCaller(WasmGCIntrinsicContext context, MethodReader method) {
        var objectClass = context.classInfoProvider().getClassInfo("java.lang.Object");
        var objectArrayClass = context.classInfoProvider().getClassInfo(ValueType.arrayOf(
                ValueType.object("java.lang.Object")));
        var callerType = context.functionTypes().of(objectClass.getType(), objectClass.getType(),
                objectArrayClass.getType());
        var function = new WasmFunction(callerType);
        function.setName(context.names().topLevel(context.names().suggestForMethod(method.getReference())
                + "@caller"));
        context.module().functions.add(function);
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

        var classInfo = context.classInfoProvider().getClassInfo(method.getOwnerName());
        var args = new ArrayList<WasmExpression>();
        WasmFunction callee = null;
        var virtual = false;
        if (method.hasModifier(ElementModifier.STATIC)) {
            initClass(context, classInfo, method.getOwnerName(), function);
            callee = context.functions().forStaticMethod(method.getReference());
        } else {
            if (method.getName().equals("<init>")) {
                instanceVar = new WasmLocal(classInfo.getStructure().getNonNullReference(), "instance");
                function.add(instanceVar);
                function.getBody().add(new WasmSetLocal(instanceVar,
                        new WasmStructNewDefault(classInfo.getStructure())));
                function.getBody().add(new WasmStructSet(
                        classInfo.getStructure(),
                        new WasmGetLocal(instanceVar),
                        WasmGCClassInfoProvider.VT_FIELD_OFFSET,
                        new WasmGetGlobal(classInfo.getVirtualTablePointer())
                ));
                args.add(new WasmGetLocal(instanceVar));
            } else {
                virtual = !method.hasModifier(ElementModifier.FINAL) && method.getLevel() != AccessLevel.PRIVATE;
                if (!virtual) {
                    var castInstance = new WasmCast(new WasmGetLocal(thisVar), classInfo.getType());
                    args.add(castInstance);
                }
            }
            if (!virtual) {
                callee = context.functions().forInstanceMethod(method.getReference());
            }
        }

        var dataType = (WasmType.CompositeReference) dataField.getUnpackedType();
        var dataArray = (WasmArray) dataType.composite;
        for (var i = 0; i < method.parameterCount(); ++i) {
            var rawArg = new WasmArrayGet(dataArray, new WasmGetLocal(argsDataVar), new WasmInt32Constant(i));
            args.add(unboxIfNecessary(context, rawArg, method.parameterType(i)));
        }

        WasmExpression call;
        if (virtual) {
            var callGen = new WasmGCVirtualCallGenerator(context.virtualTables(), context.classInfoProvider());
            call = callGen.generate(method.getReference(), false, thisVar, args);
        } else {
            call = new WasmCall(callee, args.toArray(new WasmExpression[0]));
        }
        function.getBody().add(boxIfNecessary(context, call, method.getResultType()));
        if (method.getResultType() == ValueType.VOID) {
            if (method.getName().equals("<init>")) {
                function.getBody().add(new WasmGetLocal(instanceVar));
            } else {
                function.getBody().add(new WasmNullConstant(objectClass.getType()));
            }
        }

        return function;
    }

    private WasmFunction generateInstantiator(WasmGCIntrinsicContext context, MethodReader method) {
        var className = method.getOwnerName();
        var objectClass = context.classInfoProvider().getClassInfo("java.lang.Object");
        var instantiatorType = context.functionTypes().of(objectClass.getType());

        var instantiator = new WasmFunction(instantiatorType);
        instantiator.setName(context.names().topLevel(className + "@instantiate"));
        instantiator.setReferenced(true);
        context.module().functions.add(instantiator);

        var classInfo = context.classInfoProvider().getClassInfo(className);
        var localVar = new WasmLocal(classInfo.getType(), "instance");
        instantiator.add(localVar);

        initClass(context, classInfo, method.getOwnerName(), instantiator);

        instantiator.getBody().add(new WasmSetLocal(localVar, new WasmStructNewDefault(classInfo.getStructure())));
        instantiator.getBody().add(new WasmStructSet(
                objectClass.getStructure(),
                new WasmGetLocal(localVar),
                WasmGCClassInfoProvider.VT_FIELD_OFFSET,
                new WasmGetGlobal(classInfo.getVirtualTablePointer())
        ));
        var call = new WasmCall(context.functions().forInstanceMethod(method.getReference()),
                new WasmGetLocal(localVar));
        instantiator.getBody().add(call);
        instantiator.getBody().add(new WasmGetLocal(localVar));

        if (context.isAsyncMethod(method.getReference())) {
            call.setSuspensionPoint(true);
            var transformation = new CoroutineTransformation(
                    context.functionTypes(),
                    context.functions(),
                    context.classInfoProvider()
            );
            transformation.transform(instantiator);
        }

        return instantiator;
    }

    private void initClass(WasmGCIntrinsicContext context, WasmGCClassInfo classInfo, String className,
            WasmFunction function) {
        if (context.classInitInfo().isDynamicInitializer(className)
                && classInfo.getInitializerPointer() != null) {
            var initRef = new WasmGetGlobal(classInfo.getInitializerPointer());
            var initType = context.functionTypes().of(null);
            function.getBody().add(new WasmCallReference(initRef, initType));
        }
    }

    private WasmExpression boxIfNecessary(WasmGCIntrinsicContext context, WasmExpression expr, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return box(context, Boolean.class, type, expr);
                case BYTE:
                    return box(context, Byte.class, type, expr);
                case SHORT:
                    return box(context, Short.class, type, expr);
                case CHARACTER:
                    return box(context, Character.class, type, expr);
                case INTEGER:
                    return box(context, Integer.class, type, expr);
                case LONG:
                    return box(context, Long.class, type, expr);
                case FLOAT:
                    return box(context, Float.class, type, expr);
                case DOUBLE:
                    return box(context, Double.class, type, expr);
            }
        }
        return expr;
    }

    private WasmExpression box(WasmGCIntrinsicContext context, Class<?> wrapperType, ValueType sourceType,
            WasmExpression expr) {
        var method = new MethodReference(wrapperType.getName(), "valueOf", sourceType,
                ValueType.object(wrapperType.getName()));
        var function = context.functions().forStaticMethod(method);
        return new WasmCall(function, expr);
    }

    private WasmExpression unboxIfNecessary(WasmGCIntrinsicContext context, WasmExpression expr, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return unbox(context, boolean.class, Boolean.class, expr);
                case BYTE:
                    return unbox(context, byte.class, Byte.class, expr);
                case SHORT:
                    return unbox(context, short.class, Short.class, expr);
                case CHARACTER:
                    return unbox(context, char.class, Character.class, expr);
                case INTEGER:
                    return unbox(context, int.class, Integer.class, expr);
                case LONG:
                    return unbox(context, long.class, Long.class, expr);
                case FLOAT:
                    return unbox(context, float.class, Float.class, expr);
                case DOUBLE:
                    return unbox(context, double.class, Double.class, expr);
            }
        } else if (type instanceof ValueType.Object) {
            if (((ValueType.Object) type).getClassName().equals("java.lang.Object")) {
                return expr;
            }
        }
        var targetType = context.typeMapper().mapType(type);
        if (targetType == context.typeMapper().mapType(ValueType.object("java.lang.Object"))) {
            return expr;
        }
        return new WasmCast(expr, (WasmType.Reference) targetType);
    }

    private WasmExpression unbox(WasmGCIntrinsicContext context, Class<?> primitiveType, Class<?> wrapperType,
            WasmExpression expr) {
        var method = new MethodReference(wrapperType.getName(), primitiveType.getName() + "Value",
                ValueType.parse(primitiveType));
        var function = context.functions().forInstanceMethod(method);
        var cast = new WasmCast(expr, context.classInfoProvider().getClassInfo(wrapperType.getName()).getType());
        return new WasmCall(function, cast);
    }

    private WasmExpression renderType(WasmGCIntrinsicContext context, ValueType type) {
        if (type instanceof ValueType.Array) {
            var itemType = ((ValueType.Array) type).getItemType();
            if (!(itemType instanceof ValueType.Primitive)) {
                var degree = 0;
                while (type instanceof ValueType.Array) {
                    type = ((ValueType.Array) type).getItemType();
                    ++degree;
                }
                WasmExpression result = new WasmGetGlobal(context.classInfoProvider().getClassInfo(type).getPointer());
                while (degree-- > 0) {
                    result = new WasmCall(context.classInfoProvider().getGetArrayClassFunction(), result);
                }
                return result;
            }
        }
        var classConstant = context.classInfoProvider().getClassInfo(type);
        return new WasmGetGlobal(classConstant.getPointer());
    }
}
