/*
 *  Copyright 2021 Alexey Andreev.
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
package org.teavm.newir.interpreter.instructions;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.newir.interpreter.Instruction;
import org.teavm.newir.interpreter.InstructionPrinter;
import org.teavm.newir.interpreter.InterpreterContext;

public final class JavaInstructions {
    private JavaInstructions() {
    }

    public static Instruction call(int result, MethodReference method, int instance, int[] arguments) {
        ValueType[] argumentTypes = new ValueType[arguments.length];
        for (int i = 0; i < arguments.length; ++i) {
            argumentTypes[i] = method.parameterType(i);
        }

        ValueType type = method.getReturnType();
        Method javaMethod = getMethod(method);
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                Object resultObj;
                Object instanceObj = instance >= 0 ? context.ov[instance] : null;
                try {
                    resultObj = javaMethod.invoke(instanceObj, mapArguments(context, arguments, argumentTypes));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
                if (result >= 0) {
                    setValue(context, result, type, resultObj);
                }
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                if (result >= 0) {
                    printType(p, result, type);
                }
                p.opcode(instance >= 0 ? "call" : "callstatic");
                p.arg().text(method.toString());
                for (int i = 0, argumentsLength = arguments.length; i < argumentsLength; i++) {
                    p.arg();
                    printType(p, arguments[i], argumentTypes[i]);
                }
            }
        };
    }

    public static Instruction getField(int result, FieldReference field, ValueType type, int instance) {
        Field javaField = getField(field);
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                try {
                    setValue(context, result, type, javaField.get(instance >= 0 ? context.ov[instance] : null));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                printType(p, result, type);
                p.opcode(instance >= 0 ? "getfield" : "getstatic");
                p.arg().text(field.toString());
                if (instance >= 0) {
                    p.arg().objVar(instance);
                }
            }
        };
    }

    public static Instruction setField(FieldReference field, ValueType type, int instance, int value) {
        Field javaField = getField(field);
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                try {
                    javaField.set(instance >= 0 ? context.ov[instance] : null, getValue(context, value, type));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.opcode(instance >= 0 ? "getfield" : "getstatic");
                p.arg().text(field.toString());
                if (instance >= 0) {
                    p.arg().objVar(instance);
                }
                printType(p, value, type);
            }
        };
    }

    static Object getValue(InterpreterContext context, int slot, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return context.iv[slot] != 0;
                case BYTE:
                    return (byte) context.iv[slot];
                case SHORT:
                    return (short) context.iv[slot];
                case CHARACTER:
                    return (char) context.iv[slot];
                case INTEGER:
                    return context.iv[slot];
                case LONG:
                    return context.lv[slot];
                case FLOAT:
                    return context.fv[slot];
                case DOUBLE:
                    return context.dv[slot];
            }
        }
        return context.ov[slot];
    }

    static void setValue(InterpreterContext context, int slot, ValueType type, Object value) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    context.iv[slot] = (Boolean) value ? 1 : 0;
                    break;
                case BYTE:
                case SHORT:
                case INTEGER:
                    context.iv[slot] = ((Number) value).intValue();
                    break;
                case CHARACTER:
                    context.iv[slot] = (char) value;
                    break;
                case LONG:
                    context.lv[slot] = ((Number) value).longValue();
                    break;
                case FLOAT:
                    context.fv[slot] = ((Number) value).floatValue();
                    break;
                case DOUBLE:
                    context.dv[slot] = ((Number) value).doubleValue();
                    break;
            }
        } else {
            context.ov[slot] = value;
        }
    }

    static void printType(InstructionPrinter printer, int slot, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                case SHORT:
                case INTEGER:
                case CHARACTER:
                    printer.intVar(slot);
                    break;
                case LONG:
                    printer.longVar(slot);
                    break;
                case FLOAT:
                    printer.floatVar(slot);
                    break;
                case DOUBLE:
                    printer.doubleVar(slot);
                    break;
            }
        } else {
            printer.objVar(slot);
        }
    }

    static Field getField(FieldReference ref) {
        Class<?> cls;
        try {
            cls = Class.forName(ref.getClassName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Class not found " + ref.getClassName(), e);
        }
        Field field;
        try {
            field = cls.getField(ref.getFieldName());
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Field not found: " + ref.getFieldName(), e);
        }
        return field;
    }

    static Method getMethod(MethodReference ref) {
        Class<?> cls;
        try {
            cls = Class.forName(ref.getClassName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Class not found " + ref.getClassName(), e);
        }
        Class<?>[] parameterTypes = new Class[ref.parameterCount()];
        for (int i = 0; i < parameterTypes.length; ++i) {
            parameterTypes[i] = valueTypeToClass(ref.parameterType(i));
        }
        try {
            return cls.getMethod(ref.getName(), parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static Class<?> valueTypeToClass(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return boolean.class;
                case BYTE:
                    return byte.class;
                case SHORT:
                    return short.class;
                case CHARACTER:
                    return char.class;
                case INTEGER:
                    return int.class;
                case LONG:
                    return long.class;
                case FLOAT:
                    return float.class;
                case DOUBLE:
                    return double.class;
                default:
                    throw new IllegalArgumentException();
            }
        } else if (type instanceof ValueType.Void) {
            return void.class;
        } else if (type instanceof ValueType.Array) {
            Class<?> itemClass = valueTypeToClass(((ValueType.Array) type).getItemType());
            return Array.newInstance(itemClass, 0).getClass();
        } else if (type instanceof ValueType.Object) {
            try {
                return Class.forName(((ValueType.Object) type).getClassName());
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    static Object[] mapArguments(InterpreterContext context, int[] arguments, ValueType[] types) {
        Object[] result = new Object[arguments.length];
        for (int i = 0; i < arguments.length; ++i) {
            result[i] = getValue(context, arguments[i], types[i]);
        }
        return result;
    }
}
