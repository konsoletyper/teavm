/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.model;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.instructions.BinaryBranchingCondition;
import org.teavm.model.instructions.BinaryOperation;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.CastIntegerDirection;
import org.teavm.model.instructions.InstructionReader;
import org.teavm.model.instructions.IntegerSubtype;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.NumericOperandType;
import org.teavm.model.instructions.SwitchTableEntryReader;

public class Interpreter {
    private ClassLoader classLoader;
    private BasicBlockReader currentBlock;
    private List<List<IncomingReader>> outgoings;
    private Object[] variables;
    private Object result;
    private State state;

    public Interpreter(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public Object interpret(ProgramReader program, Object[] parameters) throws InterpretException {
        variables = new Object[program.variableCount()];
        System.arraycopy(parameters, 0, variables, 0, parameters.length);
        currentBlock = program.basicBlockAt(0);
        state = State.EXECUTING;

        outgoings = new ArrayList<>();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            outgoings.add(new ArrayList<>());
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlockReader block = program.basicBlockAt(i);
            for (PhiReader phi : block.readPhis()) {
                for (IncomingReader incoming : phi.readIncomings()) {
                    outgoings.get(incoming.getSource().getIndex()).add(incoming);
                }
            }
        }

        try {
            while (true) {
                InstructionIterator iterator = currentBlock.iterateInstructions();
                try {
                    while (iterator.hasNext()) {
                        iterator.next();
                        iterator.read(reader);
                    }
                } catch (RuntimeException e) {
                    if (!pickExceptionHandler(e)) {
                        throw new InterpretException(currentBlock, e);
                    }
                }
                switch (state) {
                    case EXITED: {
                        return result;
                    }
                    case THROWN: {
                        Throwable ex = (Throwable) result;
                        throw new InterpretException(currentBlock, ex);
                    }
                    case EXECUTING:
                        break;
                }
            }

        } finally {
            currentBlock = null;
            variables = null;
            outgoings = null;
            result = null;
        }
    }

    private boolean pickExceptionHandler(Throwable e) {
        for (TryCatchBlockReader tryCatch : currentBlock.readTryCatchBlocks()) {
            Class<?> exceptionType;
            try {
                exceptionType = tryCatch.getExceptionType() != null
                        ? Class.forName(tryCatch.getExceptionType(), false, classLoader)
                        : null;
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException("Can't find exception class " + tryCatch.getExceptionType());
            }
            if (exceptionType == null || exceptionType.isInstance(e)) {
                currentBlock = tryCatch.getProtectedBlock();
                return true;
            }
        }
        return false;
    }

    private InstructionReader reader = new InstructionReader() {
        @Override
        public void location(TextLocation location) {
        }

        @Override
        public void nop() {
        }

        @Override
        public void classConstant(VariableReader receiver, ValueType cst) {
            variables[receiver.getIndex()] = asJvmClass(cst);
        }

        @Override
        public void nullConstant(VariableReader receiver) {
            variables[receiver.getIndex()] = null;
        }

        @Override
        public void integerConstant(VariableReader receiver, int cst) {
            variables[receiver.getIndex()] = cst;
        }

        @Override
        public void longConstant(VariableReader receiver, long cst) {
            variables[receiver.getIndex()] = cst;
        }

        @Override
        public void floatConstant(VariableReader receiver, float cst) {
            variables[receiver.getIndex()] = cst;
        }

        @Override
        public void doubleConstant(VariableReader receiver, double cst) {
            variables[receiver.getIndex()] = cst;
        }

        @Override
        public void stringConstant(VariableReader receiver, String cst) {
            variables[receiver.getIndex()] = cst;
        }

        @Override
        public void binary(BinaryOperation op, VariableReader receiver, VariableReader first, VariableReader second,
                NumericOperandType type) {
            switch (type) {
                case INT: {
                    int a = (Integer) variables[first.getIndex()];
                    int b = (Integer) variables[second.getIndex()];
                    int result;
                    switch (op) {
                        case ADD:
                            result = a + b;
                            break;
                        case SUBTRACT:
                            result = a - b;
                            break;
                        case MULTIPLY:
                            result = a * b;
                            break;
                        case DIVIDE:
                            result = a * b;
                            break;
                        case MODULO:
                            result = a % b;
                            break;
                        case COMPARE:
                            result = Integer.compare(a, b);
                            break;
                        case AND:
                            result = a & b;
                            break;
                        case OR:
                            result = a | b;
                            break;
                        case XOR:
                            result = a ^ b;
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown operation: " + op);
                    }
                    variables[receiver.getIndex()] = result;
                    break;
                }
                case LONG: {
                    long a = (Long) variables[first.getIndex()];
                    long b = (Long) variables[second.getIndex()];
                    long result;
                    switch (op) {
                        case ADD:
                            result = a + b;
                            break;
                        case SUBTRACT:
                            result = a - b;
                            break;
                        case MULTIPLY:
                            result = a * b;
                            break;
                        case DIVIDE:
                            result = a * b;
                            break;
                        case MODULO:
                            result = a % b;
                            break;
                        case COMPARE:
                            result = Long.compare(a, b);
                            break;
                        case AND:
                            result = a & b;
                            break;
                        case OR:
                            result = a | b;
                            break;
                        case XOR:
                            result = a ^ b;
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown operation: " + op);
                    }
                    variables[receiver.getIndex()] = result;
                    break;
                }
                case FLOAT: {
                    float a = (Float) variables[first.getIndex()];
                    float b = (Float) variables[second.getIndex()];
                    float result;
                    switch (op) {
                        case ADD:
                            result = a + b;
                            break;
                        case SUBTRACT:
                            result = a - b;
                            break;
                        case MULTIPLY:
                            result = a * b;
                            break;
                        case DIVIDE:
                            result = a * b;
                            break;
                        case MODULO:
                            result = a % b;
                            break;
                        case COMPARE:
                            result = Float.compare(a, b);
                            break;
                        case AND:
                        case OR:
                        case XOR:
                            throw new IllegalArgumentException("Unsupported operation " + op
                                    + " for operands of type" + type);
                        default:
                            throw new IllegalArgumentException("Unknown operation: " + op);
                    }
                    variables[receiver.getIndex()] = result;
                    break;
                }
                case DOUBLE: {
                    double a = (Double) variables[first.getIndex()];
                    double b = (Double) variables[second.getIndex()];
                    double result;
                    switch (op) {
                        case ADD:
                            result = a + b;
                            break;
                        case SUBTRACT:
                            result = a - b;
                            break;
                        case MULTIPLY:
                            result = a * b;
                            break;
                        case DIVIDE:
                            result = a * b;
                            break;
                        case MODULO:
                            result = a % b;
                            break;
                        case COMPARE:
                            result = Double.compare(a, b);
                            break;
                        case AND:
                        case OR:
                        case XOR:
                            throw new IllegalArgumentException("Unsupported operation " + op
                                    + " for operands of type" + type);
                        default:
                            throw new IllegalArgumentException("Unknown operation: " + op);
                    }
                    variables[receiver.getIndex()] = result;
                    break;
                }
            }
        }

        @Override
        public void negate(VariableReader receiver, VariableReader operand, NumericOperandType type) {
            Object result;
            Object a = variables[operand.getIndex()];
            switch (type) {
                case INT:
                    result = -(Integer) a;
                    break;
                case LONG:
                    result = -(Long) a;
                    break;
                case FLOAT:
                    result = -(Float) a;
                    break;
                case DOUBLE:
                    result = -(Double) a;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown type: " + type);
            }
            variables[receiver.getIndex()] = result;
        }

        @Override
        public void assign(VariableReader receiver, VariableReader assignee) {
            variables[receiver.getIndex()] = variables[assignee.getIndex()];
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
            variables[receiver.getIndex()] = asJvmClass(targetType).cast(variables[value.getIndex()]);
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, NumericOperandType sourceType,
                NumericOperandType targetType) {
            Object result;
            switch (sourceType) {
                case INT: {
                    int a = (Integer) variables[value.getIndex()];
                    switch (targetType) {
                        case INT:
                            result = a;
                            break;
                        case LONG:
                            result = (long) a;
                            break;
                        case FLOAT:
                            result = (float) a;
                            break;
                        case DOUBLE:
                            result = (double) a;
                            break;
                        default:
                            throw new IllegalArgumentException("Can't cast " + sourceType + " to " + targetType);
                    }
                    break;
                }
                case LONG: {
                    long a = (Long) variables[value.getIndex()];
                    switch (targetType) {
                        case INT:
                            result = (int) a;
                            break;
                        case LONG:
                            result = a;
                            break;
                        case FLOAT:
                            result = (float) a;
                            break;
                        case DOUBLE:
                            result = (double) a;
                            break;
                        default:
                            throw new IllegalArgumentException("Can't cast " + sourceType + " to " + targetType);
                    }
                    break;
                }
                case FLOAT: {
                    float a = (Float) variables[value.getIndex()];
                    switch (targetType) {
                        case INT:
                            result = (int) a;
                            break;
                        case LONG:
                            result = (long) a;
                            break;
                        case FLOAT:
                            result = a;
                            break;
                        case DOUBLE:
                            result = (double) a;
                            break;
                        default:
                            throw new IllegalArgumentException("Can't cast " + sourceType + " to " + targetType);
                    }
                    break;
                }
                case DOUBLE: {
                    double a = (Double) variables[value.getIndex()];
                    switch (targetType) {
                        case INT:
                            result = (int) a;
                            break;
                        case LONG:
                            result = (long) a;
                            break;
                        case FLOAT:
                            result = (float) a;
                            break;
                        case DOUBLE:
                            result = a;
                            break;
                        default:
                            throw new IllegalArgumentException("Can't cast " + sourceType + " to " + targetType);
                    }
                    break;
                }
                default:
                    throw new IllegalArgumentException("Can't cast " + sourceType + " to " + targetType);
            }
            variables[receiver.getIndex()] = result;
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, IntegerSubtype type,
                CastIntegerDirection direction) {
            switch (direction) {
                case FROM_INTEGER: {
                    int a = (Integer) variables[value.getIndex()];
                    Object result;
                    switch (type) {
                        case BYTE:
                            result = (byte) a;
                            break;
                        case SHORT:
                            result = (short) a;
                            break;
                        case CHAR:
                            result = (char) a;
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown type: " + type);
                    }
                    variables[receiver.getIndex()] = result;
                    break;
                }
                case TO_INTEGER: {
                    Object a = variables[value.getIndex()];
                    int result;
                    switch (type) {
                        case BYTE:
                            result = (Byte) a;
                            break;
                        case SHORT:
                            result = (Short) a;
                            break;
                        case CHAR:
                            result = (Character) a;
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown type: " + type);
                    }
                    variables[receiver.getIndex()] = result;
                    break;
                }
            }
        }

        @Override
        public void jumpIf(BranchingCondition cond, VariableReader operand, BasicBlockReader consequent,
                BasicBlockReader alternative) {
            Object a = variables[operand.getIndex()];
            boolean c;
            switch (cond) {
                case EQUAL:
                    c = (Integer) a == 0;
                    break;
                case NOT_EQUAL:
                    c = (Integer) a != 0;
                    break;
                case LESS:
                    c = (Integer) a < 0;
                    break;
                case LESS_OR_EQUAL:
                    c = (Integer) a <= 0;
                    break;
                case GREATER:
                    c = (Integer) a > 0;
                    break;
                case GREATER_OR_EQUAL:
                    c = (Integer) a >= 0;
                    break;
                case NULL:
                    c = a == null;
                    break;
                case NOT_NULL:
                    c = a != null;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown condition: " + cond);
            }
            jump(c ? consequent : alternative);
        }

        @Override
        public void jumpIf(BinaryBranchingCondition cond, VariableReader first, VariableReader second,
                BasicBlockReader consequent, BasicBlockReader alternative) {
            Object a = variables[first.getIndex()];
            Object b = variables[second.getIndex()];
            boolean c;
            switch (cond) {
                case EQUAL:
                    c = ((Integer) a).intValue() == (Integer) b;
                    break;
                case NOT_EQUAL:
                    c = ((Integer) a).intValue() != (Integer) b;
                    break;
                case REFERENCE_EQUAL:
                    c = a == b;
                    break;
                case REFERENCE_NOT_EQUAL:
                    c = a != b;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown condition: " + cond);
            }
            jump(c ? consequent : alternative);
        }

        @Override
        public void jump(BasicBlockReader target) {
            Object[] newVariables = variables.clone();

            for (IncomingReader outgoing : outgoings.get(currentBlock.getIndex())) {
                if (outgoing.getPhi().getBasicBlock() != target) {
                    continue;
                }
                newVariables[outgoing.getPhi().getReceiver().getIndex()] = variables[outgoing.getValue().getIndex()];
            }

            variables = newVariables;
            currentBlock = target;
        }

        @Override
        public void choose(VariableReader condition, List<? extends SwitchTableEntryReader> table,
                BasicBlockReader defaultTarget) {
            int value = (Integer) variables[condition.getIndex()];
            for (SwitchTableEntryReader entry : table) {
                if (value == entry.getCondition()) {
                    jump(entry.getTarget());
                    return;
                }
            }
            jump(defaultTarget);
        }

        @Override
        public void exit(VariableReader valueToReturn) {
            state = State.EXITED;
            result = variables[valueToReturn.getIndex()];
        }

        @Override
        public void raise(VariableReader exception) {
            Throwable e = (Throwable) variables[exception.getIndex()];
            if (!pickExceptionHandler(e)) {
                state = State.EXITED;
                result = e;
            }
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
            Class<?> itemJvmType = asJvmClass(itemType);
            int sizeValue = (int) variables[size.getIndex()];
            variables[receiver.getIndex()] = Array.newInstance(itemJvmType, sizeValue);
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType,
                List<? extends VariableReader> dimensions) {
            Class<?> itemJvmType = asJvmClass(itemType);
            for (int i = 1; i < dimensions.size(); ++i) {
                itemJvmType = Array.newInstance(itemJvmType, 0).getClass();
            }
            variables[receiver.getIndex()] = createArray(itemJvmType, dimensions, 0);
        }

        private Object createArray(Class<?> itemType, List<? extends VariableReader> dimensions, int dimensionIndex) {
            int dimensionValue = (int) variables[dimensions.get(dimensionIndex).getIndex()];
            Object result = Array.newInstance(itemType, dimensionValue);
            if (dimensionIndex < dimensions.size() - 1) {
                for (int i = 0; i < dimensionValue; ++i) {
                    Array.set(result, i, createArray(itemType.getComponentType(), dimensions, dimensionIndex + 1));
                }
            }
            return result;
        }

        @Override
        public void create(VariableReader receiver, String type) {
            Class<?> cls;
            try {
                cls = Class.forName(type, false, classLoader);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class not found: " + type);
            }
            variables[receiver.getIndex()] = null;
        }

        @Override
        public void getField(VariableReader receiver, VariableReader instance, FieldReference field,
                ValueType fieldType) {
            Field jvmField = getJvmField(field);

            Object jvmInstance = instance != null ? variables[instance.getIndex()] : null;
            Object result;
            try {
                result = jvmField.get(jvmInstance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Can't get field value: " + field);
            }

            variables[receiver.getIndex()] = result;
        }

        @Override
        public void putField(VariableReader instance, FieldReference field, VariableReader value, ValueType fieldType) {
            Field jvmField = getJvmField(field);

            Object jvmInstance = instance != null ? variables[instance.getIndex()] : null;
            try {
                jvmField.set(jvmInstance, variables[value.getIndex()]);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Can't get field value: " + field);
            }
        }

        private Field getJvmField(FieldReference field) {
            Class<?> cls;
            try {
                cls = Class.forName(field.getClassName(), false, classLoader);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class not found: " + field.getClassName());
            }

            Field jvmField;
            try {
                jvmField = cls.getDeclaredField(field.getFieldName());
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Field not found: " + field);
            }

            jvmField.setAccessible(true);
            return jvmField;
        }

        @Override
        public void arrayLength(VariableReader receiver, VariableReader array) {
            int value = Array.getLength(variables[array.getIndex()]);
            variables[receiver.getIndex()] = value;
        }

        @Override
        public void cloneArray(VariableReader receiver, VariableReader array) {
            Object jvmArray = variables[array.getIndex()];
            int length = Array.getLength(jvmArray);
            Object copy = Array.newInstance(jvmArray.getClass().getComponentType(), length);
            for (int i = 0; i < length; ++i) {
                Array.set(copy, i, Array.get(array, i));
            }
            variables[receiver.getIndex()] = copy;
        }

        @Override
        public void unwrapArray(VariableReader receiver, VariableReader array, ArrayElementType elementType) {
            variables[receiver.getIndex()] = variables[array.getIndex()];
        }

        @Override
        public void getElement(VariableReader receiver, VariableReader array, VariableReader index,
                ArrayElementType type) {
            Object jvmArray = variables[array.getIndex()];
            int indexValue = (Integer) variables[index.getIndex()];
            variables[receiver.getIndex()] = Array.get(jvmArray, indexValue);
        }

        @Override
        public void putElement(VariableReader array, VariableReader index, VariableReader value,
                ArrayElementType type) {
            Object jvmArray = variables[array.getIndex()];
            int indexValue = (Integer) variables[index.getIndex()];
            Array.set(jvmArray, indexValue, variables[value.getIndex()]);
        }

        @Override
        public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments, InvocationType type) {
            Method jvmMethod = asJvmMethod(method);
            Object[] jvmArgs = new Object[arguments.size()];
            for (int i = 0; i < jvmArgs.length; ++i) {
                jvmArgs[i] = variables[arguments.get(i).getIndex()];
            }
            Object jvmInstance = instance != null ? variables[instance.getIndex()] : null;
            Object result;
            try {
                result = jvmMethod.invoke(jvmInstance, jvmArgs);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Error calling method " + method, e);
            }
            if (receiver != null) {
                variables[receiver.getIndex()] = result;
            }
        }

        private Method asJvmMethod(MethodReference method) {
            Class<?> cls;
            try {
                cls = Class.forName(method.getClassName(), false, classLoader);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Can't find class " + method.getClassName());
            }

            Class<?>[] jvmParameters = new Class[method.parameterCount()];
            for (int i = 0; i < method.parameterCount(); ++i) {
                jvmParameters[i] = asJvmClass(method.parameterType(i));
            }
            Class<?> jvmReturnType = asJvmClass(method.getReturnType());
            for (Method jvmMethod : cls.getDeclaredMethods()) {
                if (Arrays.equals(jvmMethod.getParameterTypes(), jvmParameters)
                        && jvmReturnType.equals(jvmMethod.getReturnType())) {
                    return jvmMethod;
                }
            }

            throw new RuntimeException("Method not found: " + method);
        }

        @Override
        public void invokeDynamic(VariableReader receiver, VariableReader instance, MethodDescriptor method,
                List<? extends VariableReader> arguments, MethodHandle bootstrapMethod,
                List<RuntimeConstant> bootstrapArguments) {
            throw new RuntimeException("InvokeDynamic is not supported");
        }

        @Override
        public void isInstance(VariableReader receiver, VariableReader value, ValueType type) {
            Object jvmValue = variables[value.getIndex()];
            Class<?> jvmType = asJvmClass(type);
            variables[receiver.getIndex()] = jvmType.isInstance(jvmValue);
        }

        @Override
        public void initClass(String className) {
            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class not found: " + className);
            }
        }

        @Override
        public void nullCheck(VariableReader receiver, VariableReader value) {
            Object jvmValue = variables[value.getIndex()];
            if (jvmValue == null) {
                throw new NullPointerException();
            }
            variables[receiver.getIndex()] = jvmValue;
        }

        @Override
        public void monitorEnter(VariableReader objectRef) {
        }

        @Override
        public void monitorExit(VariableReader objectRef) {
        }

        @Override
        public void boundCheck(VariableReader receiver, VariableReader index, VariableReader array, boolean lower) {
            variables[receiver.getIndex()] = variables[index.getIndex()];
        }

        private Class<?> asJvmClass(ValueType type) {
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
                        break;
                }
            } else if (type instanceof ValueType.Void) {
                return void.class;
            } else if (type instanceof ValueType.Array) {
                Class<?> itemJvmClass = asJvmClass(((ValueType.Array) type).getItemType());
                return Array.newInstance(itemJvmClass, 0).getClass();
            } else if (type instanceof ValueType.Object) {
                try {
                    Class.forName(((ValueType.Object) type).getClassName(), false, classLoader);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Class not found: " + type);
                }
            }
            throw new IllegalArgumentException("Unknown type: " + type);
        }
    };

    private enum State {
        EXECUTING,
        EXITED,
        THROWN
    }
}
