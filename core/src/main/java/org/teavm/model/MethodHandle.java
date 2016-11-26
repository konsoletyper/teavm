/*
 *  Copyright 2015 Alexey Andreev.
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

import java.util.Arrays;

public class MethodHandle {
    private MethodHandleType kind;
    private String className;
    private String name;
    private ValueType valueType;
    private ValueType[] argumentTypes;

    MethodHandle(MethodHandleType kind, String className, String name, ValueType valueType,
            ValueType[] argumentTypes) {
        this.kind = kind;
        this.className = className;
        this.name = name;
        this.valueType = valueType;
        this.argumentTypes = argumentTypes;
    }

    public MethodHandleType getKind() {
        return kind;
    }

    public String getClassName() {
        return className;
    }

    public String getName() {
        return name;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public ValueType[] getArgumentTypes() {
        return argumentTypes != null ? argumentTypes.clone() : null;
    }

    public int getArgumentCount() {
        return argumentTypes != null ? argumentTypes.length : 0;
    }

    public ValueType getArgumentType(int index) {
        if (argumentTypes == null) {
            throw new IllegalArgumentException("Can't get argument of non-parameterized method handle");
        }
        return argumentTypes[index];
    }

    public ValueType[] signature() {
        ValueType[] result = Arrays.copyOf(argumentTypes, argumentTypes.length + 1);
        result[argumentTypes.length] = valueType;
        return result;
    }

    public static MethodHandle fieldGetter(String className, String name, ValueType valueType) {
        if (valueType == ValueType.VOID) {
            throw new IllegalArgumentException("Field can't be of void type");
        }
        return new MethodHandle(MethodHandleType.GET_FIELD, className, name, valueType, null);
    }

    public static MethodHandle staticFieldGetter(String className, String name, ValueType valueType) {
        if (valueType == ValueType.VOID) {
            throw new IllegalArgumentException("Field can't be of void type");
        }
        return new MethodHandle(MethodHandleType.GET_STATIC_FIELD, className, name, valueType, null);
    }

    public static MethodHandle fieldSetter(String className, String name, ValueType valueType) {
        if (valueType == ValueType.VOID) {
            throw new IllegalArgumentException("Field can't be of void type");
        }
        return new MethodHandle(MethodHandleType.PUT_FIELD, className, name, valueType, null);
    }

    public static MethodHandle staticFieldSetter(String className, String name, ValueType valueType) {
        if (valueType == ValueType.VOID) {
            throw new IllegalArgumentException("Field can't be of void type");
        }
        return new MethodHandle(MethodHandleType.PUT_STATIC_FIELD, className, name, valueType, null);
    }

    public static MethodHandle virtualCaller(String className, String name, ValueType... arguments) {
        ValueType valueType = arguments[arguments.length - 1];
        arguments = Arrays.copyOfRange(arguments, 0, arguments.length - 1);
        return new MethodHandle(MethodHandleType.INVOKE_VIRTUAL, className, name, valueType, arguments);
    }

    public static MethodHandle virtualCaller(String className, MethodDescriptor desc) {
        return virtualCaller(className, desc.getName(), desc.getSignature());
    }

    public static MethodHandle virtualCaller(MethodReference method) {
        return virtualCaller(method.getClassName(), method.getDescriptor());
    }

    public static MethodHandle staticCaller(String className, String name, ValueType... arguments) {
        ValueType valueType = arguments[arguments.length - 1];
        arguments = Arrays.copyOfRange(arguments, 0, arguments.length - 1);
        return new MethodHandle(MethodHandleType.INVOKE_STATIC, className, name, valueType, arguments);
    }

    public static MethodHandle staticCaller(String className, MethodDescriptor desc) {
        return staticCaller(className, desc.getName(), desc.getSignature());
    }

    public static MethodHandle staticCaller(MethodReference method) {
        return staticCaller(method.getClassName(), method.getDescriptor());
    }

    public static MethodHandle specialCaller(String className, String name, ValueType... arguments) {
        ValueType valueType = arguments[arguments.length - 1];
        arguments = Arrays.copyOfRange(arguments, 0, arguments.length - 1);
        return new MethodHandle(MethodHandleType.INVOKE_SPECIAL, className, name, valueType, arguments);
    }

    public static MethodHandle specialCaller(String className, MethodDescriptor desc) {
        return specialCaller(className, desc.getName(), desc.getSignature());
    }

    public static MethodHandle specialCaller(MethodReference method) {
        return specialCaller(method.getClassName(), method.getDescriptor());
    }

    public static MethodHandle constructorCaller(String className, String name, ValueType... arguments) {
        ValueType valueType = arguments[arguments.length - 1];
        arguments = Arrays.copyOfRange(arguments, 0, arguments.length - 1);
        return new MethodHandle(MethodHandleType.INVOKE_CONSTRUCTOR, className, name, valueType, arguments);
    }

    public static MethodHandle constructorCaller(String className, MethodDescriptor desc) {
        return constructorCaller(className, desc.getName(), desc.getSignature());
    }

    public static MethodHandle constructorCaller(MethodReference method) {
        return constructorCaller(method.getClassName(), method.getDescriptor());
    }

    public static MethodHandle interfaceCaller(String className, String name, ValueType... arguments) {
        ValueType valueType = arguments[arguments.length - 1];
        arguments = Arrays.copyOfRange(arguments, 0, arguments.length - 1);
        return new MethodHandle(MethodHandleType.INVOKE_INTERFACE, className, name, valueType, arguments);
    }

    public static MethodHandle interfaceCaller(String className, MethodDescriptor desc) {
        return interfaceCaller(className, desc.getName(), desc.getSignature());
    }

    public static MethodHandle interfaceCaller(MethodReference method) {
        return interfaceCaller(method.getClassName(), method.getDescriptor());
    }
}
