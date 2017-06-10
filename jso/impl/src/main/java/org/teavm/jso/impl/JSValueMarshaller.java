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

import java.util.List;
import java.util.function.Function;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSArrayReader;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.StringConstantInstruction;

class JSValueMarshaller {
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
            if (cls.getAnnotations().get(JSFunctor.class.getName()) != null) {
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
        insn.setMethod(new MethodReference(JS.class, "function", JSObject.class, JSObject.class, JSObject.class));
        insn.setReceiver(functor);
        insn.getArguments().add(var);
        insn.getArguments().add(nameVar);
        insn.setLocation(location.getSourceLocation());
        replacement.add(insn);
        return functor;
    }

    Variable wrap(Variable var, ValueType type, TextLocation location, boolean byRef) {
        if (byRef) {
            InvokeInstruction insn = new InvokeInstruction();
            insn.setMethod(new MethodReference(JS.class, "arrayData", Object.class, JSObject.class));
            insn.setReceiver(program.createVariable());
            insn.setType(InvocationType.SPECIAL);
            insn.getArguments().add(var);
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
            insn.setMethod(new MethodReference(JS.class.getName(), "wrap", getWrappedType(type),
                    getWrapperType(type)));
            insn.getArguments().add(var);
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
                insn.setMethod(new MethodReference(JS.class, "arrayMapper", Function.class, Function.class));
                insn.getArguments().add(function);
                function = program.createVariable();
                insn.setReceiver(function);
                insn.setType(InvocationType.SPECIAL);
                insn.setLocation(location);
                replacement.add(insn);
            }

            insn = new InvokeInstruction();
            insn.setMethod(new MethodReference(JS.class.getName(), "map", getWrappedType(type),
                    ValueType.parse(Function.class), getWrapperType(type)));
            insn.getArguments().add(var);
            insn.getArguments().add(function);
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
                return ValueType.parse(JSObject.class);
            }
        } else {
            return type;
        }
    }

    private ValueType getWrapperType(ValueType type) {
        if (type instanceof ValueType.Array) {
            return ValueType.parse(JSArray.class);
        } else {
            return ValueType.parse(JSObject.class);
        }
    }

    private MethodReference getWrapperFunction(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return new MethodReference(JS.class, "booleanArrayWrapper", Function.class);
                case BYTE:
                    return new MethodReference(JS.class, "byteArrayWrapper", Function.class);
                case SHORT:
                    return new MethodReference(JS.class, "shortArrayWrapper", Function.class);
                case CHARACTER:
                    return new MethodReference(JS.class, "charArrayWrapper", Function.class);
                case INTEGER:
                    return new MethodReference(JS.class, "intArrayWrapper", Function.class);
                case FLOAT:
                    return new MethodReference(JS.class, "floatArrayWrapper", Function.class);
                case DOUBLE:
                    return new MethodReference(JS.class, "doubleArrayWrapper", Function.class);
                default:
                    break;
            }
        } else if (type.isObject(String.class)) {
            return new MethodReference(JS.class, "stringArrayWrapper", Function.class);
        }
        return new MethodReference(JS.class, "arrayWrapper", Function.class);
    }

    Variable unwrapReturnValue(CallLocation location, Variable var, ValueType type) {
        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            ClassReader cls = classSource.get(className);
            if (cls.getAnnotations().get(JSFunctor.class.getName()) != null) {
                return unwrapFunctor(location, var, cls);
            }
        }
        return unwrap(location, var, type);
    }

    Variable unwrap(CallLocation location, Variable var, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return unwrap(var, "unwrapBoolean", ValueType.parse(JSObject.class), ValueType.BOOLEAN,
                            location.getSourceLocation());
                case BYTE:
                    return unwrap(var, "unwrapByte", ValueType.parse(JSObject.class), ValueType.BYTE,
                            location.getSourceLocation());
                case SHORT:
                    return unwrap(var, "unwrapShort", ValueType.parse(JSObject.class), ValueType.SHORT,
                            location.getSourceLocation());
                case INTEGER:
                    return unwrap(var, "unwrapInt", ValueType.parse(JSObject.class), ValueType.INTEGER,
                            location.getSourceLocation());
                case CHARACTER:
                    return unwrap(var, "unwrapCharacter", ValueType.parse(JSObject.class), ValueType.CHARACTER,
                            location.getSourceLocation());
                case DOUBLE:
                    return unwrap(var, "unwrapDouble", ValueType.parse(JSObject.class), ValueType.DOUBLE,
                            location.getSourceLocation());
                case FLOAT:
                    return unwrap(var, "unwrapFloat", ValueType.parse(JSObject.class), ValueType.FLOAT,
                            location.getSourceLocation());
                case LONG:
                    break;
            }
        } else if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            if (className.equals(JSObject.class.getName())) {
                return var;
            } else if (className.equals("java.lang.String")) {
                return unwrap(var, "unwrapString", ValueType.parse(JSObject.class), ValueType.parse(String.class),
                        location.getSourceLocation());
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

    private Variable unwrapSingleDimensionArray(CallLocation location, Variable var, ValueType type) {
        Variable result = program.createVariable();

        InvokeInstruction insn = new InvokeInstruction();
        insn.setMethod(singleDimensionArrayUnwrapper(type));
        insn.setType(InvocationType.SPECIAL);

        if (insn.getMethod().parameterCount() == 2) {
            Variable cls = program.createVariable();
            ClassConstantInstruction clsInsn = new ClassConstantInstruction();
            clsInsn.setConstant(type);
            clsInsn.setLocation(location.getSourceLocation());
            clsInsn.setReceiver(cls);
            replacement.add(clsInsn);
            insn.getArguments().add(cls);
        }

        insn.getArguments().add(var);
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
            insn.getArguments().add(cls);
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
            insn.setMethod(new MethodReference(JS.class, "arrayUnmapper", Class.class, Function.class,
                    Function.class));
            insn.setType(InvocationType.SPECIAL);
            insn.getArguments().add(cls);
            insn.getArguments().add(function);
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
        insn.setMethod(new MethodReference(JS.class, "unmapArray", Class.class, JSArrayReader.class, Function.class,
                Object[].class));
        insn.getArguments().add(cls);
        insn.getArguments().add(var);
        insn.getArguments().add(function);
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
                    return new MethodReference(JS.class, "unwrapBooleanArray", JSArrayReader.class, boolean[].class);
                case BYTE:
                    return new MethodReference(JS.class, "unwrapByteArray", JSArrayReader.class, byte[].class);
                case SHORT:
                    return new MethodReference(JS.class, "unwrapShortArray", JSArrayReader.class, short[].class);
                case CHARACTER:
                    return new MethodReference(JS.class, "unwrapCharArray", JSArrayReader.class, char[].class);
                case INTEGER:
                    return new MethodReference(JS.class, "unwrapIntArray", JSArrayReader.class, int[].class);
                case FLOAT:
                    return new MethodReference(JS.class, "unwrapFloatArray", JSArrayReader.class, float[].class);
                case DOUBLE:
                    return new MethodReference(JS.class, "unwrapDoubleArray", JSArrayReader.class, double[].class);
                default:
                    break;
            }
        } else if (itemType.isObject(String.class)) {
            return new MethodReference(JS.class, "unwrapStringArray", JSArrayReader.class, String[].class);
        }
        return new MethodReference(JS.class, "unwrapArray", Class.class, JSArrayReader.class, JSObject[].class);
    }

    private MethodReference multipleDimensionArrayUnwrapper(ValueType itemType) {
        if (itemType instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) itemType).getKind()) {
                case BOOLEAN:
                    return new MethodReference(JS.class, "booleanArrayUnwrapper", Function.class);
                case BYTE:
                    return new MethodReference(JS.class, "byteArrayUnwrapper", Function.class);
                case SHORT:
                    return new MethodReference(JS.class, "shortArrayUnwrapper", Function.class);
                case CHARACTER:
                    return new MethodReference(JS.class, "charArrayUnwrapper", Function.class);
                case INTEGER:
                    return new MethodReference(JS.class, "intArrayUnwrapper", Function.class);
                case FLOAT:
                    return new MethodReference(JS.class, "floatArrayUnwrapper", Function.class);
                case DOUBLE:
                    return new MethodReference(JS.class, "doubleArrayUnwrapper", Function.class);
                default:
                    break;
            }
        } else if (itemType.isObject(String.class)) {
            return new MethodReference(JS.class, "stringArrayUnwrapper", Function.class);
        }
        return new MethodReference(JS.class, "arrayUnwrapper", Class.class, Function.class);
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
        insn.setMethod(new MethodReference(JS.class.getName(), methodName, argType, resultType));
        insn.getArguments().add(var);
        insn.setReceiver(result);
        insn.setType(InvocationType.SPECIAL);
        insn.setLocation(location);
        replacement.add(insn);
        return result;
    }

    private Variable unwrapFunctor(CallLocation location, Variable var, ClassReader type) {
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
        insn.setMethod(new MethodReference(JS.class, "functionAsObject", JSObject.class, JSObject.class,
                JSObject.class));
        insn.setReceiver(functor);
        insn.getArguments().add(var);
        insn.getArguments().add(nameVar);
        insn.setLocation(location.getSourceLocation());
        replacement.add(insn);
        return functor;
    }

    Variable addStringWrap(Variable var, TextLocation location) {
        return wrap(var, ValueType.object("java.lang.String"), location, false);
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
