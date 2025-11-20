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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.expression.WasmArrayGet;
import org.teavm.backend.wasm.model.expression.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.model.ClassReader;
import org.teavm.model.FieldReference;
import org.teavm.model.GenericTypeParameter;
import org.teavm.model.GenericValueType;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

class WasmGCReflectionGenericsHelper {
    private WasmGCIntrinsicContext context;
    private WasmFunction function;
    private WasmFunction variableConstructor;

    static final String TYPE_VARIABLE_IMPL_CLASS = "java.lang.reflect.TypeVariableImpl";
    private static final MethodReference typeVarBounds = new MethodReference(TYPE_VARIABLE_IMPL_CLASS,
            "getBounds", ValueType.parse(Type[].class));
    static final MethodReference typeVarConstructor = new MethodReference(TYPE_VARIABLE_IMPL_CLASS,
            "create", ValueType.object("java.lang.String"), ValueType.object(TYPE_VARIABLE_IMPL_CLASS));
    static final MethodReference parameterizedTypeConstructor = new MethodReference(
            "java.lang.reflect.ParameterizedTypeImpl", "create", ValueType.parse(Class.class),
            ValueType.parse(ObjectList.class), ValueType.object("java.lang.reflect.ParameterizedTypeImpl"));
    private static final MethodReference wildcardTypeUpper = new MethodReference(
            "java.lang.reflect.WildcardTypeImpl", "upper", ValueType.parse(Type.class),
            ValueType.object("java.lang.reflect.WildcardTypeImpl"));
    private static final MethodReference wildcardTypeLower = new MethodReference(
            "java.lang.reflect.WildcardTypeImpl", "lower", ValueType.parse(Type.class),
            ValueType.object("java.lang.reflect.WildcardTypeImpl"));
    private static final MethodReference genericArrayTypeCreate = new MethodReference(
            "java.lang.reflect.GenericArrayTypeImpl", "create", ValueType.parse(Type.class),
            ValueType.object("java.lang.reflect.GenericArrayTypeImpl"));

    WasmGCReflectionGenericsHelper(WasmGCIntrinsicContext context, WasmFunction function) {
        this.context = context;
        this.function = function;
    }

    void initReflectionGenericsForClasses() {
        for (var className : context.dependency().getReachableClasses()) {
            var cls = context.hierarchy().getClassSource().get(className);
            if (cls != null) {
                initTypeParameters(cls);
            }
        }
        if (context.dependency().getMethod(typeVarBounds) != null) {
            for (var className : context.dependency().getReachableClasses()) {
                var cls = context.hierarchy().getClassSource().get(className);
                if (cls != null) {
                    initTypeParameterBounds(cls);
                }
            }
        }
    }

    private void initTypeParameters(ClassReader cls) {
        var params = cls.getGenericParameters();
        if (params == null || params.length == 0) {
            return;
        }
        var array = writeTypeParameters(params);
        var fieldOffset = context.classInfoProvider().getClassTypeParametersOffset();
        var clsInfo = context.classInfoProvider().getClassInfo(cls.getName());
        var clsCls = context.classInfoProvider().getClassInfo("java.lang.Class");
        function.getBody().add(new WasmStructSet(clsCls.getStructure(), new WasmGetGlobal(clsInfo.getPointer()),
                fieldOffset, array));
    }

    private void initTypeParameterBounds(ClassReader cls) {
        var params = cls.getGenericParameters();
        if (params == null) {
            return;
        }
        var clsCls = context.classInfoProvider().getClassInfo("java.lang.Class");
        var clsInfo = context.classInfoProvider().getClassInfo(cls.getName());
        var paramsOffset = context.classInfoProvider().getClassTypeParametersOffset();
        var arrayType = context.classInfoProvider().getObjectArrayType();
        var typeVariableImplCls = context.classInfoProvider().getClassInfo(TYPE_VARIABLE_IMPL_CLASS);
        var boundsFieldOffset = context.classInfoProvider().getFieldIndex(new FieldReference(TYPE_VARIABLE_IMPL_CLASS,
                "boundsList"));
        for (var i = 0; i < params.length; i++) {
            var param = params[i];
            var bounds = new ArrayList<GenericValueType.Reference>();
            if (param.getClassBound() != null) {
                bounds.add(param.getClassBound());
            }
            bounds.addAll(List.of(param.getInterfaceBounds()));
            if (bounds.isEmpty()) {
                continue;
            }

            var array = new WasmArrayNewFixed(arrayType);
            for (var bound : bounds) {
                array.getElements().add(writeGenericType(cls, bound));
            }
            var paramsGet = new WasmStructGet(clsCls.getStructure(), new WasmGetGlobal(clsInfo.getPointer()),
                    paramsOffset);
            var paramGet = new WasmCast(new WasmArrayGet(arrayType, paramsGet, new WasmInt32Constant(i)),
                    typeVariableImplCls.getType());
            var boundsSet = new WasmStructSet(typeVariableImplCls.getStructure(), paramGet,
                    boundsFieldOffset, array);
            function.getBody().add(boundsSet);
        }
    }

    WasmExpression writeTypeParameters(GenericTypeParameter[] params) {
        var arrayType = context.classInfoProvider().getObjectArrayType();
        var array = new WasmArrayNewFixed(arrayType);
        for (var param : params) {
            var nameRef = new WasmGetGlobal(context.strings().getStringConstant(param.getName()).global);
            array.getElements().add(new WasmCall(getVariableConstructor(), nameRef));
        }
        return array;
    }

    WasmExpression writeGenericType(ClassReader contextClass, GenericValueType type) {
        if (type instanceof GenericValueType.Object) {
            var objectType = (GenericValueType.Object) type;
            var args = objectType.getArguments();
            var clsInfo = context.classInfoProvider().getClassInfo(objectType.getClassName());
            var cls = new WasmGetGlobal(clsInfo.getPointer());
            if (args.length == 0) {
                return cls;
            } else {
                var arrayType = context.classInfoProvider().getObjectArrayType();
                var array = new WasmArrayNewFixed(arrayType);
                for (var arg : args) {
                    array.getElements().add(writeGenericType(contextClass, arg));
                }
                var constructor = context.functions().forStaticMethod(parameterizedTypeConstructor);
                return new WasmCall(constructor, cls, array);
            }
        } else if (type instanceof GenericValueType.Variable) {
            var typeVar = (GenericValueType.Variable) type;
            var genericParameters = contextClass.getGenericParameters();
            var typeVariableImplCls = context.classInfoProvider().getClassInfo(TYPE_VARIABLE_IMPL_CLASS);
            for (var i = 0; i < genericParameters.length; i++) {
                var param = genericParameters[i];
                if (param.getName().equals(typeVar.getName())) {
                    var contextClassInfo = context.classInfoProvider().getClassInfo(contextClass.getName());
                    var contextClassRef = new WasmGetGlobal(contextClassInfo.getPointer());
                    var clsCls = context.classInfoProvider().getClassInfo("java.lang.Class");
                    var paramsOffset = context.classInfoProvider().getClassTypeParametersOffset();
                    var paramsGet = new WasmStructGet(clsCls.getStructure(), contextClassRef,
                            paramsOffset);
                    var arrayType = context.classInfoProvider().getObjectArrayType();
                    return new WasmCast(new WasmArrayGet(arrayType, paramsGet, new WasmInt32Constant(i)),
                            typeVariableImplCls.getType());
                }
            }
            throw new IllegalArgumentException("Unknown type variable: " + typeVar.getName());
        } else if (type instanceof GenericValueType.Array) {
            var nonGenericType = type.asValueType();
            if (nonGenericType == null) {
                var arrayType = (GenericValueType.Array) type;
                var constructor = context.functions().forStaticMethod(genericArrayTypeCreate);
                return new WasmCall(constructor, writeGenericType(contextClass, arrayType.getItemType()));
            } else {
                var typeRef = context.classInfoProvider().getClassInfo(nonGenericType);
                return new WasmGetGlobal(typeRef.getPointer());
            }
        } else if (type instanceof GenericValueType.Primitive) {
            var primitiveType = (GenericValueType.Primitive) type;
            var typeRef = context.classInfoProvider().getClassInfo(ValueType.primitive(primitiveType.getKind()));
            return new WasmGetGlobal(typeRef.getPointer());
        } else if (type instanceof GenericValueType.Void) {
            var typeRef = context.classInfoProvider().getClassInfo(ValueType.VOID);
            return new WasmGetGlobal(typeRef.getPointer());
        } else {
            throw new IllegalArgumentException("Unsupported generic type: " + type);
        }
    }

    WasmExpression writeGenericType(ClassReader contextClass, GenericValueType.Argument arg) {
        switch (arg.getKind()) {
            case INVARIANT:
                return writeGenericType(contextClass, arg.getValue());
            case ANY: {
                var function = context.functions().forStaticMethod(wildcardTypeUpper);
                var typeType = context.classInfoProvider().getClassInfo("java.lang.reflect.Type");
                return new WasmCall(function, new WasmNullConstant(typeType.getType()));
            }
            case COVARIANT: {
                var function = context.functions().forStaticMethod(wildcardTypeUpper);
                return new WasmCall(function, writeGenericType(contextClass, arg.getValue()));
            }
            case CONTRAVARIANT: {
                var function = context.functions().forStaticMethod(wildcardTypeLower);
                return new WasmCall(function, writeGenericType(contextClass, arg.getValue()));
            }
            default: {
                throw new IllegalArgumentException("Unsupported generic type: " + arg.getKind());
            }
        }
    }

    private WasmFunction getVariableConstructor() {
        if (variableConstructor == null) {
            variableConstructor = context.functions().forStaticMethod(typeVarConstructor);
        }
        return variableConstructor;
    }
}
