/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.jso.impl;

import java.util.ArrayList;
import java.util.List;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.jso.JSBufferType;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSModule;
import org.teavm.jso.JSObject;
import org.teavm.model.AnnotationContainerReader;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ReferenceCache;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.StringConstantInstruction;

class JSValueMarshaller {
    private static final MethodReference JS_TO_JAVA = new MethodReference(JSWrapper.class, "jsToJava",
            JSObject.class, Object.class);
    private static final MethodReference LIGHTWEIGHT_JS_TO_JAVA = new MethodReference(JSWrapper.class,
            "dependencyJsToJava", JSObject.class, Object.class);
    private static final ValueType stringType = ValueType.parse(String.class);
    private ReferenceCache referenceCache = new ReferenceCache();
    private Diagnostics diagnostics;
    private JSTypeHelper typeHelper;
    private ClassReaderSource classSource;
    private ClassHierarchy hierarchy;
    private Program program;
    private List<Instruction> replacement;

    JSValueMarshaller(Diagnostics diagnostics, JSTypeHelper typeHelper, ClassReaderSource classSource,
            ClassHierarchy hierarchy, Program program, List<Instruction> replacement) {
        this.diagnostics = diagnostics;
        this.hierarchy = hierarchy;
        this.typeHelper = typeHelper;
        this.classSource = classSource;
        this.program = program;
        this.replacement = replacement;
    }

    Variable wrapArgument(CallLocation location, Variable var, ValueType type, JSType jsType, boolean byRef,
            JSBufferType bufferType) {
        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            ClassReader cls = classSource.get(className);
            if (cls != null && cls.getAnnotations().get(JSFunctor.class.getName()) != null) {
                return wrapFunctor(location, var, cls, jsType);
            }
        }
        return wrap(var, type, jsType, location.getSourceLocation(), byRef, bufferType);
    }

    boolean isProperFunctor(ClassReader type) {
        if (!type.hasModifier(ElementModifier.INTERFACE)) {
            return false;
        }
        return type.getMethods().stream()
                .filter(method -> method.hasModifier(ElementModifier.ABSTRACT))
                .count() == 1;
    }

    private Variable wrapFunctor(CallLocation location, Variable var, ClassReader type, JSType jsType) {
        if (!isProperFunctor(type)) {
            diagnostics.error(location, "Wrong functor: {{c0}}", type.getName());
            return var;
        }

        if (jsType == JSType.JAVA) {
            var unwrapNative = new InvokeInstruction();
            unwrapNative.setLocation(location.getSourceLocation());
            unwrapNative.setType(InvocationType.SPECIAL);
            unwrapNative.setMethod(JSMethods.UNWRAP);
            unwrapNative.setArguments(var);
            unwrapNative.setReceiver(program.createVariable());
            replacement.add(unwrapNative);
            var = unwrapNative.getReceiver();
        }

        String name = type.getMethods().stream()
                .filter(method -> method.hasModifier(ElementModifier.ABSTRACT))
                .findFirst().get().getName();
        Variable functor = program.createVariable();
        Variable nameVar = addStringWrap(addString(name, location.getSourceLocation()), location.getSourceLocation());
        InvokeInstruction insn = new InvokeInstruction();
        insn.setType(InvocationType.SPECIAL);
        insn.setMethod(JSMethods.FUNCTION);
        insn.setReceiver(functor);
        insn.setArguments(var, nameVar);
        insn.setLocation(location.getSourceLocation());
        replacement.add(insn);
        return functor;
    }

    Variable wrap(Variable var, ValueType type, JSType jsType, TextLocation location, boolean byRef,
            JSBufferType bufferType) {
        if (byRef) {
            InvokeInstruction insn = new InvokeInstruction();
            insn.setMethod(JSMethods.ARRAY_DATA);
            insn.setReceiver(program.createVariable());
            insn.setType(InvocationType.SPECIAL);
            insn.setArguments(var);
            replacement.add(insn);
            return insn.getReceiver();
        }

        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            if (className.equals("java.lang.Object")) {
                if (jsType != JSType.NULL && jsType != JSType.JS) {
                    var unwrapNative = new InvokeInstruction();
                    unwrapNative.setLocation(location);
                    unwrapNative.setType(InvocationType.SPECIAL);
                    unwrapNative.setMethod(new MethodReference(JSWrapper.class,
                            "javaToJs", Object.class, JSObject.class));
                    unwrapNative.setArguments(var);
                    unwrapNative.setReceiver(program.createVariable());
                    replacement.add(unwrapNative);
                    return unwrapNative.getReceiver();
                } else {
                    return var;
                }
            }
            if (!className.equals("java.lang.String")) {
                if (hierarchy.isSuperType("java.nio.Buffer", className, false)) {
                    return wrapBuffer(var, className, bufferType, location);
                }
                if (!typeHelper.isJavaScriptClass(className) && !typeHelper.isJavaScriptImplementation(className)) {
                    var unwrapNative = new InvokeInstruction();
                    unwrapNative.setLocation(location);
                    unwrapNative.setType(InvocationType.SPECIAL);
                    unwrapNative.setMethod(new MethodReference(JSWrapper.class,
                            "dependencyJavaToJs", Object.class, JSObject.class));
                    unwrapNative.setArguments(var);
                    unwrapNative.setReceiver(program.createVariable());
                    replacement.add(unwrapNative);
                    return unwrapNative.getReceiver();
                }
                if (typeHelper.isJavaScriptClass(className) && jsType == JSType.JAVA) {
                    var unwrapNative = new InvokeInstruction();
                    unwrapNative.setLocation(location);
                    unwrapNative.setType(InvocationType.SPECIAL);
                    unwrapNative.setMethod(JSMethods.UNWRAP);
                    unwrapNative.setArguments(var);
                    unwrapNative.setReceiver(program.createVariable());
                    replacement.add(unwrapNative);
                    return unwrapNative.getReceiver();
                }
                return var;
            }
        }
        Variable result = program.createVariable();

        ValueType itemType = type;
        int degree = 0;
        while (itemType instanceof ValueType.Array) {
            itemType = ((ValueType.Array) itemType).getItemType();
            ++degree;
        }

        if (degree <= 1) {
            InvokeInstruction insn = new InvokeInstruction();
            insn.setMethod(referenceCache.getCached(new MethodReference(JS.class.getName(), "wrap",
                    getWrappedType(type), getWrapperType())));
            insn.setArguments(var);
            insn.setReceiver(result);
            insn.setType(InvocationType.SPECIAL);
            insn.setLocation(location);
            replacement.add(insn);
        } else {
            Variable function = program.createVariable();

            InvokeInstruction insn = new InvokeInstruction();
            insn.setMethod(getWrapperFunction(itemType));
            insn.setReceiver(function);
            insn.setType(InvocationType.SPECIAL);
            insn.setLocation(location);
            replacement.add(insn);

            while (--degree > 1) {
                insn = new InvokeInstruction();
                insn.setMethod(JSMethods.ARRAY_MAPPER);
                insn.setArguments(function);
                function = program.createVariable();
                insn.setReceiver(function);
                insn.setType(InvocationType.SPECIAL);
                insn.setLocation(location);
                replacement.add(insn);
            }

            insn = new InvokeInstruction();
            insn.setMethod(referenceCache.getCached(new MethodReference(JS.class.getName(), "map",
                    getWrappedType(type), ValueType.parse(JS.WrapFunction.class), getWrapperType())));
            insn.setArguments(var, function);
            insn.setReceiver(result);
            insn.setType(InvocationType.SPECIAL);
            insn.setLocation(location);
            replacement.add(insn);
        }
        return result;
    }

    private Variable wrapBuffer(Variable value, String className, JSBufferType type, TextLocation location) {
        var extract = new InvokeInstruction();
        extract.setType(InvocationType.SPECIAL);
        extract.setMethod(new MethodReference("java.nio.JSBufferHelper", "getArrayBufferView",
                ValueType.object("java.nio.Buffer"), ValueType.object("org.teavm.jso.typedarrays.ArrayBufferView")));
        extract.setArguments(value);
        extract.setReceiver(program.createVariable());
        extract.setLocation(location);
        replacement.add(extract);

        type = resolveBufferType(className, type);
        String targetName;
        switch (type) {
            case INT8:
                targetName = "Int8Array";
                break;
            case UINT8:
                targetName = "Uint8Array";
                break;
            case INT16:
                targetName = "Int16Array";
                break;
            case UINT16:
                targetName = "Uint16Array";
                break;
            case INT32:
                targetName = "Int32Array";
                break;
            case UINT32:
                targetName = "Uint32Array";
                break;
            case INT64:
                targetName = "BigInt64Array";
                break;
            case UINT64:
                targetName = "BigUint64Array";
                break;
            case FLOAT32:
                targetName = "Float32Array";
                break;
            case FLOAT64:
                targetName = "Float64Array";
                break;
            case DATA_VIEW:
                targetName = "DataView";
                break;
            default:
                throw new IllegalStateException();
        }

        var convert = new InvokeInstruction();
        convert.setType(InvocationType.SPECIAL);
        convert.setMethod(new MethodReference("java.nio.JSBufferHelper", "to" + targetName,
                ValueType.object("org.teavm.jso.typedarrays.ArrayBufferView"),
                ValueType.object("org.teavm.jso.typedarrays." + targetName)));
        convert.setArguments(extract.getReceiver());
        convert.setReceiver(program.createVariable());
        convert.setLocation(location);
        replacement.add(convert);

        return convert.getReceiver();
    }

    private JSBufferType resolveBufferType(String className, JSBufferType type) {
        if (type == null) {
            switch (className) {
                case "java.nio.ByteBuffer":
                    type = JSBufferType.INT8;
                    break;
                case "java.nio.CharBuffer":
                    type = JSBufferType.UINT16;
                    break;
                case "java.nio.ShortBuffer":
                    type = JSBufferType.INT16;
                    break;
                case "java.nio.IntBuffer":
                    type = JSBufferType.INT32;
                    break;
                case "java.nio.LongBuffer":
                    type = JSBufferType.INT64;
                    break;
                case "java.nio.FloatBuffer":
                    type = JSBufferType.FLOAT32;
                    break;
                case "java.nio.DoubleBuffer":
                    type = JSBufferType.FLOAT64;
                    break;
            }
        }
        if (type == null) {
            type = JSBufferType.DATA_VIEW;
        }
        return type;
    }

    private ValueType getWrappedType(ValueType type) {
        if (type instanceof ValueType.Array) {
            ValueType itemType = ((ValueType.Array) type).getItemType();
            if (itemType instanceof ValueType.Array) {
                return ValueType.parse(Object[].class);
            } else {
                return ValueType.arrayOf(getWrappedType(itemType));
            }
        } else if (type instanceof ValueType.Object) {
            if (type.isObject(String.class)) {
                return type;
            } else if (typeHelper.isJavaScriptClass(((ValueType.Object) type).getClassName())) {
                return JSMethods.JS_OBJECT;
            } else {
                return JSMethods.OBJECT;
            }
        } else {
            return type;
        }
    }

    private ValueType getWrapperType() {
        return JSMethods.JS_OBJECT;
    }

    private MethodReference getWrapperFunction(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return JSMethods.BOOLEAN_ARRAY_WRAPPER;
                case BYTE:
                    return JSMethods.BYTE_ARRAY_WRAPPER;
                case SHORT:
                    return JSMethods.SHORT_ARRAY_WRAPPER;
                case CHARACTER:
                    return JSMethods.CHAR_ARRAY_WRAPPER;
                case INTEGER:
                    return JSMethods.INT_ARRAY_WRAPPER;
                case LONG:
                    return JSMethods.LONG_ARRAY_WRAPPER;
                case FLOAT:
                    return JSMethods.FLOAT_ARRAY_WRAPPER;
                case DOUBLE:
                    return JSMethods.DOUBLE_ARRAY_WRAPPER;
                default:
                    break;
            }
        } else if (type.isObject(String.class)) {
            return JSMethods.STRING_ARRAY_WRAPPER;
        }
        return JSMethods.ARRAY_WRAPPER;
    }

    Variable unwrapReturnValue(CallLocation location, Variable var, ValueType type, boolean byRef,
            boolean strictJava) {
        if (byRef) {
            return unwrapByRef(location, var, type);
        }

        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            ClassReader cls = classSource.get(className);
            if (cls != null && cls.getAnnotations().get(JSFunctor.class.getName()) != null) {
                return unwrapFunctor(location, var, cls);
            }
        }
        return unwrap(location, var, type, strictJava);
    }

    private Variable unwrapByRef(CallLocation location, Variable var, ValueType type) {
        type = ((ValueType.Array) type).getItemType();
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BYTE:
                    return invokeMethod(location, JSMethods.DATA_TO_BYTE_ARRAY, var);
                case SHORT:
                    return invokeMethod(location, JSMethods.DATA_TO_SHORT_ARRAY, var);
                case CHARACTER:
                    return invokeMethod(location, JSMethods.DATA_TO_CHAR_ARRAY, var);
                case INTEGER:
                    return invokeMethod(location, JSMethods.DATA_TO_INT_ARRAY, var);
                case LONG:
                    return invokeMethod(location, JSMethods.DATA_TO_LONG_ARRAY, var);
                case FLOAT:
                    return invokeMethod(location, JSMethods.DATA_TO_FLOAT_ARRAY, var);
                case DOUBLE:
                    return invokeMethod(location, JSMethods.DATA_TO_DOUBLE_ARRAY, var);
                default:
                    break;
            }
        }
        return invokeMethod(location, JSMethods.DATA_TO_ARRAY, var);
    }

    Variable unwrap(CallLocation location, Variable var, ValueType type, boolean strictJava) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return unwrap(var, "unwrapBoolean", JSMethods.JS_OBJECT, ValueType.BOOLEAN,
                            location.getSourceLocation());
                case BYTE:
                    return unwrap(var, "unwrapByte", JSMethods.JS_OBJECT, ValueType.BYTE,
                            location.getSourceLocation());
                case SHORT:
                    return unwrap(var, "unwrapShort", JSMethods.JS_OBJECT, ValueType.SHORT,
                            location.getSourceLocation());
                case INTEGER:
                    return unwrap(var, "unwrapInt", JSMethods.JS_OBJECT, ValueType.INTEGER,
                            location.getSourceLocation());
                case CHARACTER:
                    return unwrap(var, "unwrapCharacter", JSMethods.JS_OBJECT, ValueType.CHARACTER,
                            location.getSourceLocation());
                case DOUBLE:
                    return unwrap(var, "unwrapDouble", JSMethods.JS_OBJECT, ValueType.DOUBLE,
                            location.getSourceLocation());
                case FLOAT:
                    return unwrap(var, "unwrapFloat", JSMethods.JS_OBJECT, ValueType.FLOAT,
                            location.getSourceLocation());
                case LONG:
                    return unwrap(var, "unwrapLong", JSMethods.JS_OBJECT, ValueType.LONG,
                            location.getSourceLocation());
            }
        } else if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            if (className.equals(Object.class.getName())) {
                var wrapNative = new InvokeInstruction();
                wrapNative.setLocation(location.getSourceLocation());
                wrapNative.setType(InvocationType.SPECIAL);
                wrapNative.setMethod(strictJava ? JS_TO_JAVA : LIGHTWEIGHT_JS_TO_JAVA);
                wrapNative.setArguments(var);
                wrapNative.setReceiver(program.createVariable());
                replacement.add(wrapNative);
                return wrapNative.getReceiver();
            } else if (className.equals(JSObject.class.getName())) {
                return var;
            } else if (className.equals("java.lang.String")) {
                return unwrap(var, "unwrapString", JSMethods.JS_OBJECT, stringType, location.getSourceLocation());
            } else if (typeHelper.isJavaScriptClass(className)) {
                return var;
            } else {
                var wrapNative = new InvokeInstruction();
                wrapNative.setLocation(location.getSourceLocation());
                wrapNative.setType(InvocationType.SPECIAL);
                wrapNative.setMethod(LIGHTWEIGHT_JS_TO_JAVA);
                wrapNative.setArguments(var);
                wrapNative.setReceiver(program.createVariable());
                replacement.add(wrapNative);
                return wrapNative.getReceiver();
            }
        } else if (type instanceof ValueType.Array) {
            return unwrapArray(location, var, (ValueType.Array) type);
        }
        diagnostics.error(location, "Unsupported type: {{t0}}", type);
        return var;
    }

    private Variable unwrapArray(CallLocation location, Variable var, ValueType.Array type) {
        ValueType itemType = type;
        int degree = 0;
        while (itemType instanceof ValueType.Array) {
            ++degree;
            itemType = ((ValueType.Array) itemType).getItemType();
        }

        var = degree == 1
                ? unwrapSingleDimensionArray(location, var, itemType)
                : unwrapMultiDimensionArray(location, var, itemType, degree);

        return var;
    }

    private Variable invokeMethod(CallLocation location, MethodReference method, Variable var) {
        InvokeInstruction invoke = new InvokeInstruction();
        invoke.setArguments(var);
        invoke.setMethod(method);
        invoke.setReceiver(program.createVariable());
        invoke.setType(InvocationType.SPECIAL);
        invoke.setLocation(location.getSourceLocation());
        replacement.add(invoke);

        return invoke.getReceiver();
    }

    private Variable unwrapSingleDimensionArray(CallLocation location, Variable var, ValueType type) {
        Variable result = program.createVariable();

        InvokeInstruction insn = new InvokeInstruction();
        insn.setMethod(singleDimensionArrayUnwrapper(type));
        insn.setType(InvocationType.SPECIAL);
        List<Variable> args = new ArrayList<>();

        if (insn.getMethod().parameterCount() == 2) {
            Variable cls = program.createVariable();
            var clsInsn = new ClassConstantInstruction();
            clsInsn.setConstant(JSClassProcessor.processType(typeHelper, type));
            clsInsn.setLocation(location.getSourceLocation());
            clsInsn.setReceiver(cls);
            replacement.add(clsInsn);
            args.add(cls);
        }

        args.add(var);
        insn.setArguments(args.toArray(new Variable[0]));
        insn.setReceiver(result);
        replacement.add(insn);
        return result;
    }

    private Variable unwrapMultiDimensionArray(CallLocation location, Variable var, ValueType type, int degree) {
        Variable function = program.createVariable();

        InvokeInstruction insn = new InvokeInstruction();
        insn.setMethod(multipleDimensionArrayUnwrapper(type));
        insn.setType(InvocationType.SPECIAL);

        if (insn.getMethod().parameterCount() == 1) {
            Variable cls = program.createVariable();
            var clsInsn = new ClassConstantInstruction();
            clsInsn.setConstant(JSClassProcessor.processType(typeHelper, type));
            clsInsn.setLocation(location.getSourceLocation());
            clsInsn.setReceiver(cls);
            replacement.add(clsInsn);
            insn.setArguments(cls);
        }

        insn.setReceiver(function);
        replacement.add(insn);

        while (--degree > 1) {
            type = ValueType.arrayOf(type);
            Variable cls = program.createVariable();

            var clsInsn = new ClassConstantInstruction();
            clsInsn.setConstant(JSClassProcessor.processType(typeHelper, type));
            clsInsn.setLocation(location.getSourceLocation());
            clsInsn.setReceiver(cls);
            replacement.add(clsInsn);

            insn = new InvokeInstruction();
            insn.setMethod(JSMethods.ARRAY_UNMAPPER);
            insn.setType(InvocationType.SPECIAL);
            insn.setArguments(cls, function);
            function = program.createVariable();
            insn.setReceiver(function);
            replacement.add(insn);
        }

        Variable cls = program.createVariable();
        var clsInsn = new ClassConstantInstruction();
        clsInsn.setConstant(JSClassProcessor.processType(typeHelper, ValueType.arrayOf(type)));
        clsInsn.setLocation(location.getSourceLocation());
        clsInsn.setReceiver(cls);
        replacement.add(clsInsn);

        insn = new InvokeInstruction();
        insn.setMethod(JSMethods.UNMAP_ARRAY);
        insn.setArguments(cls, var, function);
        insn.setReceiver(program.createVariable());
        insn.setType(InvocationType.SPECIAL);
        insn.setLocation(location.getSourceLocation());
        replacement.add(insn);

        var cast = new CastInstruction();
        cast.setTargetType(ValueType.arrayOf(ValueType.arrayOf(type)));
        cast.setWeak(true);
        cast.setValue(insn.getReceiver());
        cast.setReceiver(var);
        cast.setLocation(location.getSourceLocation());
        replacement.add(cast);

        return var;
    }

    private MethodReference singleDimensionArrayUnwrapper(ValueType itemType) {
        if (itemType instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) itemType).getKind()) {
                case BOOLEAN:
                    return JSMethods.UNWRAP_BOOLEAN_ARRAY;
                case BYTE:
                    return JSMethods.UNWRAP_BYTE_ARRAY;
                case SHORT:
                    return JSMethods.UNWRAP_SHORT_ARRAY;
                case CHARACTER:
                    return JSMethods.UNWRAP_CHAR_ARRAY;
                case INTEGER:
                    return JSMethods.UNWRAP_INT_ARRAY;
                case LONG:
                    return JSMethods.UNWRAP_LONG_ARRAY;
                case FLOAT:
                    return JSMethods.UNWRAP_FLOAT_ARRAY;
                case DOUBLE:
                    return JSMethods.UNWRAP_DOUBLE_ARRAY;
                default:
                    break;
            }
        } else if (itemType.isObject(String.class)) {
            return JSMethods.UNWRAP_STRING_ARRAY;
        }
        return JSMethods.UNWRAP_ARRAY;
    }

    private MethodReference multipleDimensionArrayUnwrapper(ValueType itemType) {
        if (itemType instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) itemType).getKind()) {
                case BOOLEAN:
                    return JSMethods.BOOLEAN_ARRAY_UNWRAPPER;
                case BYTE:
                    return JSMethods.BYTE_ARRAY_UNWRAPPER;
                case SHORT:
                    return JSMethods.SHORT_ARRAY_UNWRAPPER;
                case CHARACTER:
                    return JSMethods.CHAR_ARRAY_UNWRAPPER;
                case INTEGER:
                    return JSMethods.INT_ARRAY_UNWRAPPER;
                case LONG:
                    return JSMethods.LONG_ARRAY_UNWRAPPER;
                case FLOAT:
                    return JSMethods.FLOAT_ARRAY_UNWRAPPER;
                case DOUBLE:
                    return JSMethods.DOUBLE_ARRAY_UNWRAPPER;
                default:
                    break;
            }
        } else if (itemType.isObject(String.class)) {
            return JSMethods.STRING_ARRAY_UNWRAPPER;
        }
        return JSMethods.ARRAY_UNWRAPPER;
    }

    private Variable unwrap(Variable var, String methodName, ValueType argType, ValueType resultType,
            TextLocation location) {
        Variable result = program.createVariable();
        InvokeInstruction insn = new InvokeInstruction();
        insn.setMethod(referenceCache.getCached(referenceCache.getCached(new MethodReference(
                JS.class.getName(), methodName, argType, resultType))));
        insn.setArguments(var);
        insn.setReceiver(result);
        insn.setType(InvocationType.SPECIAL);
        insn.setLocation(location);
        replacement.add(insn);
        return result;
    }

    Variable unwrapFunctor(CallLocation location, Variable var, ClassReader type) {
        if (!isProperFunctor(type)) {
            diagnostics.error(location, "Wrong functor: {{c0}}", type.getName());
            return var;
        }
        String name = type.getMethods().stream()
                .filter(method -> method.hasModifier(ElementModifier.ABSTRACT))
                .findFirst().get().getName();
        Variable functor = program.createVariable();
        Variable nameVar = addStringWrap(addString(name, location.getSourceLocation()), location.getSourceLocation());
        InvokeInstruction insn = new InvokeInstruction();
        insn.setType(InvocationType.SPECIAL);
        insn.setMethod(JSMethods.FUNCTION_AS_OBJECT);
        insn.setReceiver(functor);
        insn.setArguments(var, nameVar);
        insn.setLocation(location.getSourceLocation());
        replacement.add(insn);
        return functor;
    }

    Variable addStringWrap(Variable var, TextLocation location) {
        return wrap(var, stringType, JSType.MIXED, location, false, null);
    }

    Variable addString(String str, TextLocation location) {
        Variable var = program.createVariable();
        var nameInsn = new StringConstantInstruction();
        nameInsn.setReceiver(var);
        nameInsn.setConstant(str);
        nameInsn.setLocation(location);
        replacement.add(nameInsn);
        return var;
    }

    Variable addJsString(String str, TextLocation location) {
        return addStringWrap(addString(str, location), location);
    }

    Variable classRef(String className, TextLocation location) {
        return classRef(className, null, location);
    }

    Variable classRef(String className, AnnotationContainerReader annotations, TextLocation location) {
        String name = null;
        String module = null;
        var cls = classSource.get(className);
        if (cls != null) {
            name = cls.getSimpleName();
            var jsExport = cls.getAnnotations().get(JSClass.class.getName());
            if (jsExport != null) {
                var nameValue = jsExport.getValue("name");
                if (nameValue != null) {
                    var nameValueString = nameValue.getString();
                    if (!nameValueString.isEmpty()) {
                        name = nameValueString;
                    }
                }
            }
            module = moduleName(cls.getAnnotations());
        }
        if (name == null) {
            name = cls.getName().substring(cls.getName().lastIndexOf('.') + 1);
        }
        if (module == null && annotations != null) {
            module = moduleName(annotations);
        }
        return module != null ? moduleRef(module, name, location) : globalRef(name, location);
    }

    Variable moduleRef(String className, AnnotationContainerReader annotations, TextLocation location) {
        String module = null;
        var cls = classSource.get(className);
        if (cls != null) {
            module = moduleName(cls.getAnnotations());
        }
        if (module == null && annotations != null) {
            module = moduleName(annotations);
        }
        return module != null ? moduleRef(module, location) : nullInstance(location);
    }

    private String moduleName(AnnotationContainerReader annotations) {
        var jsModule = annotations.get(JSModule.class.getName());
        if (jsModule != null) {
            return jsModule.getValue("value").getString();
        }
        return null;
    }

    Variable globalRef(String name, TextLocation location) {
        var invoke = new InvokeInstruction();
        invoke.setType(InvocationType.SPECIAL);
        invoke.setMethod(JSMethods.GLOBAL);
        invoke.setArguments(addString(name, location));
        invoke.setReceiver(program.createVariable());
        invoke.setLocation(location);
        replacement.add(invoke);

        return invoke.getReceiver();
    }

    Variable moduleRef(String module, String name, TextLocation location) {
        return dot(moduleRef(module, location), name, location);
    }

    Variable moduleRef(String module, TextLocation location) {
        var moduleNameInsn = new StringConstantInstruction();
        moduleNameInsn.setReceiver(program.createVariable());
        moduleNameInsn.setConstant(module);
        moduleNameInsn.setLocation(location);
        replacement.add(moduleNameInsn);

        var invoke = new InvokeInstruction();
        invoke.setType(InvocationType.SPECIAL);
        invoke.setMethod(JSMethods.IMPORT_MODULE);
        invoke.setArguments(moduleNameInsn.getReceiver());
        invoke.setReceiver(program.createVariable());
        invoke.setLocation(location);
        replacement.add(invoke);

        return invoke.getReceiver();
    }

    Variable dot(Variable instance, String name, TextLocation location) {
        var get = new InvokeInstruction();
        get.setType(InvocationType.SPECIAL);
        get.setMethod(JSMethods.GET_PURE);
        get.setReceiver(program.createVariable());
        get.setArguments(instance, addJsString(name, location));
        get.setLocation(location);
        replacement.add(get);

        return get.getReceiver();
    }

    Variable nullInstance(TextLocation location) {
        var nullConstant = new NullConstantInstruction();
        nullConstant.setReceiver(program.createVariable());
        nullConstant.setLocation(location);
        replacement.add(nullConstant);
        return nullConstant.getReceiver();
    }
}
