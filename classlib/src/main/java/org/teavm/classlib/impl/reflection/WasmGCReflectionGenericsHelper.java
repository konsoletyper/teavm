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

import java.util.List;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.expression.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.model.ClassReader;
import org.teavm.model.GenericTypeParameter;
import org.teavm.model.GenericValueType;
import org.teavm.model.MethodReader;
import org.teavm.model.ValueType;

class WasmGCReflectionGenericsHelper {
    private WasmGCIntrinsicContext context;
    private WasmFunction function;
    private WasmFunction variableConstructor;
    private WasmFunction variableConstructorWithBounds;
    private WasmFunction stubCreate;
    private WasmFunction stubCreateWithLevel;

    WasmGCReflectionGenericsHelper(WasmGCIntrinsicContext context, WasmFunction function) {
        this.context = context;
        this.function = function;
    }

    void initReflectionGenericsForClasses() {
        var withBounds = context.dependency().getMethod(org.teavm.reflection.ReflectionMethods.TYPE_VAR_GET_BOUNDS) != null;
        for (var className : context.dependency().getReachableClasses()) {
            var cls = context.hierarchy().getClassSource().get(className);
            if (cls != null) {
                initTypeParameters(cls, withBounds);
            }
        }
    }

    private void initTypeParameters(ClassReader cls, boolean withBounds) {
        var params = cls.getGenericParameters();
        if (params == null || params.length == 0) {
            return;
        }
        var array = writeTypeParameters(params, cls, null, withBounds);
        var fieldOffset = context.classInfoProvider().getClassTypeParametersOffset();
        var clsInfo = context.classInfoProvider().getClassInfo(cls.getName());
        var clsCls = context.classInfoProvider().getClassInfo("java.lang.Class");
        function.getBody().add(new WasmStructSet(clsCls.getStructure(), new WasmGetGlobal(clsInfo.getPointer()),
                fieldOffset, array));
    }

    private WasmExpression generateTypeParameterBounds(ClassReader cls, MethodReader method,
            List<GenericValueType.Reference> bounds) {
        var arrayType = context.classInfoProvider().getObjectArrayType();
        var array = new WasmArrayNewFixed(arrayType);
        for (var bound : bounds) {
            array.getElements().add(writeGenericType(cls, method, bound));
        }
        return array;
    }

    WasmExpression writeTypeParameters(GenericTypeParameter[] params, ClassReader cls, MethodReader method,
            boolean withBounds) {
        var arrayType = context.classInfoProvider().getObjectArrayType();
        var array = new WasmArrayNewFixed(arrayType);
        for (var param : params) {
            var nameRef = new WasmGetGlobal(context.strings().getStringConstant(param.getName()).global);
            var bounds = withBounds ? param.extractAllBounds() : List.<GenericValueType.Reference>of();
            if (bounds.isEmpty()) {
                array.getElements().add(new WasmCall(getVariableConstructor(), nameRef));
            } else {
                array.getElements().add(new WasmCall(getVariableConstructorWithBounds(), nameRef,
                        generateTypeParameterBounds(cls, method, bounds)));
            }
        }
        return array;
    }

    WasmExpression writeGenericType(ClassReader contextClass, MethodReader contextMethod, GenericValueType type) {
        if (type instanceof GenericValueType.Object) {
            var objectType = (GenericValueType.Object) type;
            var args = objectType.getArguments();
            var clsInfo = context.classInfoProvider().getClassInfo(objectType.getFullClassName());
            var cls = new WasmGetGlobal(clsInfo.getPointer());
            if (args.length == 0) {
                return cls;
            } else {
                var arrayType = context.classInfoProvider().getObjectArrayType();
                var array = new WasmArrayNewFixed(arrayType);
                for (var arg : args) {
                    array.getElements().add(writeGenericType(contextClass, contextMethod, arg));
                }
                if (objectType.getParent() == null) {
                    //var constructor = context.functions().forStaticMethod(
                    // org.teavm.reflection.ReflectionMethods.PARAM_TYPE_CREATE);
                    //return new WasmCall(constructor, cls, array);
                    return null;
                } else {
                    /*var owner = writeGenericType(contextClass, contextMethod, objectType.getParent());
                    var constructor = context.functions().forStaticMethod(
                    org.teavm.reflection.ReflectionMethods.PARAM_TYPE_CREATE_OWNER);
                    return new WasmCall(constructor, cls, array, owner);*/
                    return null;
                }
            }
        } else if (type instanceof GenericValueType.Variable) {
            var typeVar = (GenericValueType.Variable) type;
            var level = 0;
            if (contextMethod != null) {
                var genericParameters = contextMethod.getTypeParameters();
                for (var i = 0; i < genericParameters.length; i++) {
                    var param = genericParameters[i];
                    if (param.getName().equals(typeVar.getName())) {
                        return new WasmCall(getStubCreate(), new WasmInt32Constant(i));
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
                            if (level == 0) {
                                return new WasmCall(getStubCreate(), new WasmInt32Constant(i));
                            } else {
                                return new WasmCall(getStubCreateWithLevel(), new WasmInt32Constant(i),
                                        new WasmInt32Constant(level));
                            }
                        }
                    }
                }
                ++level;
                if (contextClass.getOwnerName() == null) {
                    break;
                }
                contextClass = context.hierarchy().getClassSource().get(contextClass.getOwnerName());
            }
            throw new IllegalArgumentException("Unknown type variable: " + typeVar.getName());
        } else if (type instanceof GenericValueType.Array) {
            var nonGenericType = type.asValueType();
            if (nonGenericType == null) {
                var arrayType = (GenericValueType.Array) type;
                //var constructor = context.functions().forStaticMethod(GENERIC_ARRAY_TYPE_CREATE);
                return new WasmCall(null, writeGenericType(contextClass, contextMethod,
                        arrayType.getItemType()));
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

    private WasmExpression writeGenericType(ClassReader contextClass, MethodReader contextMethod,
            GenericValueType.Argument arg) {
        switch (arg.getKind()) {
            case INVARIANT:
                return writeGenericType(contextClass, contextMethod, arg.getValue());
            case ANY: {
                //var function = context.functions().forStaticMethod(WILDCARD_TYPE_UPPER);
                var typeType = context.classInfoProvider().getClassInfo("java.lang.reflect.Type");
                return new WasmCall(null, new WasmNullConstant(typeType.getType()));
            }
            case COVARIANT: {
                //var function = context.functions().forStaticMethod(WILDCARD_TYPE_UPPER);
                return new WasmCall(null, writeGenericType(contextClass, contextMethod, arg.getValue()));
            }
            case CONTRAVARIANT: {
                //var function = context.functions().forStaticMethod(WILDCARD_TYPE_LOWER);
                return new WasmCall(null, writeGenericType(contextClass, contextMethod, arg.getValue()));
            }
            default: {
                throw new IllegalArgumentException("Unsupported generic type: " + arg.getKind());
            }
        }
    }

    private WasmFunction getVariableConstructor() {
        if (variableConstructor == null) {
            //variableConstructor = context.functions().forStaticMethod(TYPE_VAR_CREATE);
        }
        return variableConstructor;
    }

    private WasmFunction getVariableConstructorWithBounds() {
        if (variableConstructorWithBounds == null) {
            //variableConstructorWithBounds = context.functions().forStaticMethod(TYPE_VAR_CREATE_BOUNDS);
        }
        return variableConstructorWithBounds;
    }

    private WasmFunction getStubCreate() {
        if (stubCreate == null) {
            //stubCreate = context.functions().forStaticMethod(TYPE_VAR_STUB_CREATE);
        }
        return stubCreate;
    }

    private WasmFunction getStubCreateWithLevel() {
        if (stubCreateWithLevel == null) {
            //stubCreateWithLevel = context.functions().forStaticMethod(TYPE_VAR_STUB_CREATE_LEVEL);
        }
        return stubCreateWithLevel;
    }
}
