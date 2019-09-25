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
import java.util.function.Function;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArrayReader;
import org.teavm.model.CallLocation;
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
import org.teavm.model.instructions.StringConstantInstruction;

class JSValueMarshaller {
    private static final ValueType stringType = ValueType.parse(String.class);
    private ReferenceCache referenceCache = new ReferenceCache();
    private Diagnostics diagnostics;
    private JSTypeHelper typeHelper;
    private ClassReaderSource classSource;
    private Program program;
    private List<Instruction> replacement;

    JSValueMarshaller(Diagnostics diagnostics, JSTypeHelper typeHelper, ClassReaderSource classSource,
            Program program, List<Instruction> replacement) {
        this.diagnostics = diagnostics;
        this.typeHelper = typeHelper;
        this.classSource = classSource;
        this.program = program;
        this.replacement = replacement;
    }

    Variable wrapArgument(CallLocation location, Variable var, ValueType type, boolean byRef) {
        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            ClassReader cls = classSource.get(className);
            if (cls != null && cls.getAnnotations().get(JSFunctor.class.getName()) != null) {
                return wrapFunctor(location, var, cls);
            }
        }
        return wrap(var, type, location.getSourceLocation(), byRef);
    }

    boolean isProperFunctor(ClassReader type) {
        if (!type.hasModifier(ElementModifier.INTERFACE)) {
            return false;
        }
        return type.getMethods().stream()
                .filter(method -> method.hasModifier(ElementModifier.ABSTRACT))
                .count() == 1;
    }

    private Variable wrapFunctor(CallLocation location, Variable var, ClassReader type) {
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
        insn.setMethod(JSMethods.FUNCTION);
        insn.setReceiver(functor);
        insn.setArguments(var, nameVar);
        insn.setLocation(location.getSourceLocation());
        replacement.add(insn);
        return functor;
    }

    Variable wrap(Variable var, ValueType type, TextLocation location, boolean byRef) {
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
            if (!className.equals("java.lang.String")) {
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
                    getWrappedType(type), getWrapperType(type))));
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
                    getWrappedType(type), ValueType.parse(Function.class), getWrapperType(type))));
            insn.setArguments(var, function);
            insn.setReceiver(result);
            insn.setType(InvocationType.SPECIAL);
            insn.setLocation(location);
            replacement.add(insn);
        }
        return result;
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
            } else {
                return JSMethods.JS_OBJECT;
            }
        } else {
            return type;
        }
    }

    private ValueType getWrapperType(ValueType type) {
        if (type instanceof ValueType.Array) {
            return JSMethods.JS_ARRAY;
        } else {
            return JSMethods.JS_OBJECT;
        }
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

    Variable unwrapReturnValue(CallLocation location, Variable var, ValueType type, boolean byRef) {
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
        return unwrap(location, var, type);
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

    Variable unwrap(CallLocation location, Variable var, ValueType type) {
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
                    break;
            }
        } else if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            if (className.equals(JSObject.class.getName())) {
                return var;
            } else if (className.equals("java.lang.String")) {
                return unwrap(var, "unwrapString", JSMethods.JS_OBJECT, stringType, location.getSourceLocation());
            } else if (typeHelper.isJavaScriptClass(className)) {
                Variable result = program.createVariable();
                CastInstruction castInsn = new CastInstruction();
                castInsn.setReceiver(result);
                castInsn.setValue(var);
                castInsn.setTargetType(type);
                castInsn.setLocation(location.getSourceLocation());
                replacement.add(castInsn);
                return result;
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

        CastInstruction castInsn = new CastInstruction();
        castInsn.setValue(var);
        castInsn.setTargetType(ValueType.parse(JSArrayReader.class));
        var = program.createVariable();
        castInsn.setReceiver(var);
        castInsn.setLocation(location.getSourceLocation());
        replacement.add(castInsn);

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
            ClassConstantInstruction clsInsn = new ClassConstantInstruction();
            clsInsn.setConstant(type);
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
            ClassConstantInstruction clsInsn = new ClassConstantInstruction();
            clsInsn.setConstant(type);
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

            ClassConstantInstruction clsInsn = new ClassConstantInstruction();
            clsInsn.setConstant(type);
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
        ClassConstantInstruction clsInsn = new ClassConstantInstruction();
        clsInsn.setConstant(ValueType.arrayOf(type));
        clsInsn.setLocation(location.getSourceLocation());
        clsInsn.setReceiver(cls);
        replacement.add(clsInsn);

        insn = new InvokeInstruction();
        insn.setMethod(JSMethods.UNMAP_ARRAY);
        insn.setArguments(cls, var, function);
        insn.setReceiver(var);
        insn.setType(InvocationType.SPECIAL);
        insn.setLocation(location.getSourceLocation());
        replacement.add(insn);

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
        if (!argType.isObject(JSObject.class.getName())) {
            Variable castValue = program.createVariable();
            CastInstruction castInsn = new CastInstruction();
            castInsn.setValue(var);
            castInsn.setReceiver(castValue);
            castInsn.setLocation(location);
            castInsn.setTargetType(argType);
            replacement.add(castInsn);
            var = castValue;
        }
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
        return wrap(var, stringType, location, false);
    }

    Variable addString(String str, TextLocation location) {
        Variable var = program.createVariable();
        StringConstantInstruction nameInsn = new StringConstantInstruction();
        nameInsn.setReceiver(var);
        nameInsn.setConstant(str);
        nameInsn.setLocation(location);
        replacement.add(nameInsn);
        return var;
    }
}
