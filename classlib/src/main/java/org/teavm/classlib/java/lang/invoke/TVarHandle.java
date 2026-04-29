/*
 *  Copyright 2025 konsoletyper.
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
package org.teavm.classlib.java.lang.invoke;

import java.util.List;
import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.lang.TUnsupportedOperationException;

/**
 * VarHandle provides read and write access to fields, array elements,
 * and byte buffer elements under supported access modes.
 *
 * <p>In TeaVM's single-threaded JavaScript environment, VarHandle provides
 * the API surface for compatibility. Memory ordering modes (volatile, opaque,
 * acquire/release) are all treated as plain accesses since there is no
 * concurrent access in a single-threaded model.</p>
 */
public abstract class TVarHandle extends TObject {
    private final Class<?> receiverType;
    private final Class<?> varType;
    private final boolean isStatic;

    TVarHandle(Class<?> receiverType, Class<?> varType, boolean isStatic) {
        this.receiverType = receiverType;
        this.varType = varType;
        this.isStatic = isStatic;
    }

    public Class<?> varType() {
        return varType;
    }

    public List<Class<?>> coordinateTypes() {
        if (isStatic) {
            return List.of();
        }
        return List.of(receiverType);
    }

    public MethodType accessModeType(AccessMode accessMode) {
        List<Class<?>> coords = coordinateTypes();
        Class<?>[] paramTypes = new Class<?>[coords.size() + (accessMode == AccessMode.COMPARE_AND_SET ? 2 : 1)];
        int i = 0;
        for (Class<?> c : coords) {
            paramTypes[i++] = c;
        }
        switch (accessMode) {
            case GET:
            case GET_VOLATILE:
            case GET_OPAQUE:
            case GET_ACQUIRE:
                paramTypes[i] = varType;
                return MethodType.methodType(varType, paramTypes);
            case SET:
            case SET_VOLATILE:
            case SET_OPAQUE:
            case SET_RELEASE:
                paramTypes[i] = varType;
                return MethodType.methodType(void.class, paramTypes);
            case COMPARE_AND_SET:
                paramTypes[i++] = varType;
                paramTypes[i] = varType;
                return MethodType.methodType(boolean.class, paramTypes);
            case COMPARE_AND_EXCHANGE:
            case COMPARE_AND_EXCHANGE_ACQUIRE:
            case COMPARE_AND_EXCHANGE_RELEASE:
            case COMPARE_AND_EXCHANGE_VOLATILE:
                paramTypes[i] = varType;
                return MethodType.methodType(varType, paramTypes);
            case GET_AND_SET:
            case GET_AND_SET_ACQUIRE:
            case GET_AND_SET_RELEASE:
                paramTypes[i] = varType;
                return MethodType.methodType(varType, paramTypes);
            case GET_AND_ADD:
            case GET_AND_ADD_ACQUIRE:
            case GET_AND_ADD_RELEASE:
                paramTypes[i] = varType;
                return MethodType.methodType(varType, paramTypes);
            case GET_AND_BITWISE_AND:
            case GET_AND_BITWISE_AND_ACQUIRE:
            case GET_AND_BITWISE_AND_RELEASE:
                paramTypes[i] = varType;
                return MethodType.methodType(varType, paramTypes);
            case GET_AND_BITWISE_OR:
            case GET_AND_BITWISE_OR_ACQUIRE:
            case GET_AND_BITWISE_OR_RELEASE:
                paramTypes[i] = varType;
                return MethodType.methodType(varType, paramTypes);
            case GET_AND_BITWISE_XOR:
            case GET_AND_BITWISE_XOR_ACQUIRE:
            case GET_AND_BITWISE_XOR_RELEASE:
                paramTypes[i] = varType;
                return MethodType.methodType(varType, paramTypes);
            default:
                throw new TIllegalArgumentException("Unknown access mode: " + accessMode);
        }
    }

    public boolean isAccessModeSupported(AccessMode accessMode) {
        return true;
    }

    public Object get(Object... args) {
        throw new TUnsupportedOperationException("VarHandle.get not implemented in TeaVM");
    }

    public void set(Object... args) {
        throw new TUnsupportedOperationException("VarHandle.set not implemented in TeaVM");
    }

    public Object getVolatile(Object... args) {
        return get(args);
    }

    public void setVolatile(Object... args) {
        set(args);
    }

    public Object getOpaque(Object... args) {
        return get(args);
    }

    public void setOpaque(Object... args) {
        set(args);
    }

    public Object getAcquire(Object... args) {
        return get(args);
    }

    public void setRelease(Object... args) {
        set(args);
    }

    public boolean compareAndSet(Object... args) {
        throw new TUnsupportedOperationException("VarHandle.compareAndSet not implemented in TeaVM");
    }

    public Object compareAndExchange(Object... args) {
        throw new TUnsupportedOperationException("VarHandle.compareAndExchange not implemented in TeaVM");
    }

    public Object compareAndExchangeAcquire(Object... args) {
        return compareAndExchange(args);
    }

    public Object compareAndExchangeRelease(Object... args) {
        return compareAndExchange(args);
    }

    public Object getAndSet(Object... args) {
        throw new TUnsupportedOperationException("VarHandle.getAndSet not implemented in TeaVM");
    }

    public Object getAndSetAcquire(Object... args) {
        return getAndSet(args);
    }

    public Object getAndSetRelease(Object... args) {
        return getAndSet(args);
    }

    public Object getAndAdd(Object... args) {
        throw new TUnsupportedOperationException("VarHandle.getAndAdd not implemented in TeaVM");
    }

    public Object getAndAddAcquire(Object... args) {
        return getAndAdd(args);
    }

    public Object getAndAddRelease(Object... args) {
        return getAndAdd(args);
    }

    public Object getAndBitwiseOr(Object... args) {
        throw new TUnsupportedOperationException("VarHandle.getAndBitwiseOr not implemented in TeaVM");
    }

    public Object getAndBitwiseOrAcquire(Object... args) {
        return getAndBitwiseOr(args);
    }

    public Object getAndBitwiseOrRelease(Object... args) {
        return getAndBitwiseOr(args);
    }

    public Object getAndBitwiseAnd(Object... args) {
        throw new TUnsupportedOperationException("VarHandle.getAndBitwiseAnd not implemented in TeaVM");
    }

    public Object getAndBitwiseAndAcquire(Object... args) {
        return getAndBitwiseAnd(args);
    }

    public Object getAndBitwiseAndRelease(Object... args) {
        return getAndBitwiseAnd(args);
    }

    public Object getAndBitwiseXor(Object... args) {
        throw new TUnsupportedOperationException("VarHandle.getAndBitwiseXor not implemented in TeaVM");
    }

    public Object getAndBitwiseXorAcquire(Object... args) {
        return getAndBitwiseXor(args);
    }

    public Object getAndBitwiseXorRelease(Object... args) {
        return getAndBitwiseXor(args);
    }

    public enum AccessMode {
        GET("get"),
        SET("set"),
        GET_VOLATILE("getVolatile"),
        SET_VOLATILE("setVolatile"),
        GET_OPAQUE("getOpaque"),
        SET_OPAQUE("setOpaque"),
        GET_ACQUIRE("getAcquire"),
        SET_RELEASE("setRelease"),
        COMPARE_AND_SET("compareAndSet"),
        COMPARE_AND_EXCHANGE("compareAndExchange"),
        COMPARE_AND_EXCHANGE_ACQUIRE("compareAndExchangeAcquire"),
        COMPARE_AND_EXCHANGE_RELEASE("compareAndExchangeRelease"),
        GET_AND_SET("getAndSet"),
        GET_AND_SET_ACQUIRE("getAndSetAcquire"),
        GET_AND_SET_RELEASE("getAndSetRelease"),
        GET_AND_ADD("getAndAdd"),
        GET_AND_ADD_ACQUIRE("getAndAddAcquire"),
        GET_AND_ADD_RELEASE("getAndAddRelease"),
        GET_AND_BITWISE_OR("getAndBitwiseOr"),
        GET_AND_BITWISE_OR_ACQUIRE("getAndBitwiseOrAcquire"),
        GET_AND_BITWISE_OR_RELEASE("getAndBitwiseOrRelease"),
        GET_AND_BITWISE_AND("getAndBitwiseAnd"),
        GET_AND_BITWISE_AND_ACQUIRE("getAndBitwiseAndAcquire"),
        GET_AND_BITWISE_AND_RELEASE("getAndBitwiseAndRelease"),
        GET_AND_BITWISE_XOR("getAndBitwiseXor"),
        GET_AND_BITWISE_XOR_ACQUIRE("getAndBitwiseXorAcquire"),
        GET_AND_BITWISE_XOR_RELEASE("getAndBitwiseXorRelease");

        private final String methodName;

        AccessMode(String methodName) {
            this.methodName = methodName;
        }

        public String methodName() {
            return methodName;
        }

        public static AccessMode valueFromMethodName(String methodName) {
            for (AccessMode mode : values()) {
                if (mode.methodName.equals(methodName)) {
                    return mode;
                }
            }
            throw new TIllegalArgumentException("No AccessMode with methodName " + methodName);
        }
    }

    @Override
    public String toString() {
        return "VarHandle[varType=" + varType.getName() + "]";
    }
}
