/*
 *  Copyright 2020 adam.
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

import java.io.Serializable;
import java.lang.invoke.MethodHandleInfo;
import java.util.Objects;

public final class TSerializedLambda implements Serializable {
    private static final long serialVersionUID = 8025925345765570181L;
    private final Class<?> capturingClass;
    private final String functionalInterfaceClass;
    private final String functionalInterfaceMethodName;
    private final String functionalInterfaceMethodSignature;
    private final String implClass;
    private final String implMethodName;
    private final String implMethodSignature;
    private final int implMethodKind;
    private final String instantiatedMethodType;
    private final Object[] capturedArgs;

    public TSerializedLambda(Class<?> capturingClass,
            String functionalInterfaceClass,
            String functionalInterfaceMethodName,
            String functionalInterfaceMethodSignature,
            int implMethodKind,
            String implClass,
            String implMethodName,
            String implMethodSignature,
            String instantiatedMethodType,
            Object[] capturedArgs) {
        this.capturingClass = capturingClass;
        this.functionalInterfaceClass = functionalInterfaceClass;
        this.functionalInterfaceMethodName = functionalInterfaceMethodName;
        this.functionalInterfaceMethodSignature = functionalInterfaceMethodSignature;
        this.implMethodKind = implMethodKind;
        this.implClass = implClass;
        this.implMethodName = implMethodName;
        this.implMethodSignature = implMethodSignature;
        this.instantiatedMethodType = instantiatedMethodType;
        this.capturedArgs = Objects.requireNonNull(capturedArgs).clone();
    }

    public String getCapturingClass() {
        return capturingClass.getName().replace('.', '/');
    }

    public String getFunctionalInterfaceClass() {
        return functionalInterfaceClass;
    }

    public String getFunctionalInterfaceMethodName() {
        return functionalInterfaceMethodName;
    }

    public String getFunctionalInterfaceMethodSignature() {
        return functionalInterfaceMethodSignature;
    }

    public String getImplClass() {
        return implClass;
    }

    public String getImplMethodName() {
        return implMethodName;
    }

    public String getImplMethodSignature() {
        return implMethodSignature;
    }

    public int getImplMethodKind() {
        return implMethodKind;
    }

    public String getInstantiatedMethodType() {
        return instantiatedMethodType;
    }

    public int getCapturedArgCount() {
        return capturedArgs.length;
    }

    public Object getCapturedArg(int i) {
        return capturedArgs[i];
    }

    private Object readResolve() throws ReflectiveOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        String implKind = MethodHandleInfo.referenceKindToString(implMethodKind);
        return "SerializedLambda[capturingClass=" + capturingClass + ", functionalInterfaceMethod="
                + functionalInterfaceClass + "." + functionalInterfaceMethodName + ":"
                + functionalInterfaceMethodSignature + ", "
                + "implementation=" + implKind + " " + implClass + "." + implMethodName + ":" + implMethodSignature
                + ", instantiatedMethodType=" + instantiatedMethodType + ", numCaptured=" + capturedArgs.length + "]";
    }
}
