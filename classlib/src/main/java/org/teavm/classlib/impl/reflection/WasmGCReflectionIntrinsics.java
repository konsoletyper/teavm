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

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCReflectionProvider;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayGet;
import org.teavm.backend.wasm.model.expression.WasmArrayLength;
import org.teavm.backend.wasm.model.expression.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmFunctionReference;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.backend.wasm.model.expression.WasmSignedType;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructNew;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.classlib.impl.ReflectionDependencyListener;
import org.teavm.model.AccessLevel;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class WasmGCReflectionIntrinsics implements WasmGCIntrinsic {
    private ReflectionDependencyListener reflection;

    private WasmFunction initReflectionFunction;

    public WasmGCReflectionIntrinsics(ReflectionDependencyListener reflection) {
        this.reflection = reflection;
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getClassName()) {
            case "org.teavm.classlib.impl.reflection.FieldInfo":
                switch (invocation.getMethod().getName()) {
                    case "name":
                        return fieldInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_NAME);
                    case "modifiers":
                        return fieldInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_MODIFIERS);
                    case "accessLevel":
                        return fieldInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_ACCESS);
                    case "type":
                        return fieldInfoCall(invocation, context, WasmGCReflectionProvider.FIELD_TYPE);
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
            case "org.teavm.classlib.impl.reflection.FieldReader": {
                var fn = context.generate(invocation.getArguments().get(0));
                var arg = context.generate(invocation.getArguments().get(1));
                var objectType = context.classInfoProvider().getClassInfo("java.lang.Object").getType();
                var type = context.functionTypes().of(objectType, objectType);
                return new WasmCallReference(fn, type, arg);
            }
            case "org.teavm.classlib.impl.reflection.FieldWriter": {
                var fn = context.generate(invocation.getArguments().get(0));
                var arg = context.generate(invocation.getArguments().get(1));
                var value = context.generate(invocation.getArguments().get(2));
                var objectType = context.classInfoProvider().getClassInfo("java.lang.Object").getType();
                var type = context.functionTypes().of(null, objectType, objectType);
                return new WasmCallReference(fn, type, arg, value);
            }
            case "java.lang.Class":
                return new WasmCall(getInitReflectionFunction(context));
        }
        throw new IllegalArgumentException();
    }

    private WasmExpression fieldInfoCall(InvocationExpr invocation, WasmGCIntrinsicContext context, int fieldIndex) {
        var arg = context.generate(invocation.getArguments().get(0));
        return new WasmStructGet(context.classInfoProvider().reflection().getReflectionFieldType(),
                arg, fieldIndex);
    }

    private WasmFunction getInitReflectionFunction(WasmGCIntrinsicContext context) {
        if (initReflectionFunction == null) {
            initReflectionFunction = new WasmFunction(context.functionTypes().of(null));
            initReflectionFunction.setName(context.names().topLevel("@teavm.initReflection"));
            context.module().functions.add(initReflectionFunction);
            initReflectionFields(context, initReflectionFunction);
        }
        return initReflectionFunction;
    }

    private void initReflectionFields(WasmGCIntrinsicContext context, WasmFunction function) {
        var wasmGcReflection = context.classInfoProvider().reflection();
        var classClass = context.classInfoProvider().getClassInfo("java.lang.Class");
        var objectClass = context.classInfoProvider().getClassInfo("java.lang.Object");

        for (var className : reflection.getClassesWithReflectableFields()) {
            var cls = context.hierarchy().getClassSource().get(className);
            if (cls == null || cls.getFields().isEmpty()) {
                return;
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

                fieldInit.getInitializers().add(renderType(context, field.getType()));

                if (accessibleFields != null && accessibleFields.contains(field.getName())
                        && reflection.isGetReached()) {
                    var getter = generateGetter(context, field);
                    fieldInit.getInitializers().add(new WasmFunctionReference(getter));
                } else {
                    var getterType = context.functionTypes().of(objectClass.getType(), objectClass.getType());
                    fieldInit.getInitializers().add(new WasmNullConstant(getterType.getReference()));
                }
                if (accessibleFields != null && accessibleFields.contains(field.getName())
                        && reflection.isSetReached()) {
                    var setter = generateSetter(context, field);
                    fieldInit.getInitializers().add(new WasmFunctionReference(setter));
                } else {
                    var setterType = context.functionTypes().of(null, objectClass.getType(), objectClass.getType());
                    fieldInit.getInitializers().add(new WasmNullConstant(setterType.getReference()));
                }
            }
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
            if (context.classInitInfo().isDynamicInitializer(field.getOwnerName())) {
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
            if (context.classInitInfo().isDynamicInitializer(field.getOwnerName())) {
                var initRef = new WasmGetGlobal(classInfo.getInitializerPointer());
                var initType = context.functionTypes().of(null);
                function.getBody().add(new WasmCallReference(initRef, initType));
            }
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
