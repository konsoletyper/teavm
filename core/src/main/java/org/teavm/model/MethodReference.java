/*
 *  Copyright 2013 Alexey Andreev.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * <p>Specifies a fully qualified name of a method, including its name, class name, parameter types
 * and return value type. This class overloads <code>equals</code> and <code>hashCode</code>
 * so that any two references to one method are considered equal.</p>
 *
 * <p>Though in Java language it is enough to have only parameter types to uniquely identify
 * a method, JVM uses return value as well. Java generates <b>bridge</b> methods to make
 * adjust the JVM's behavior.</p>
 *
 * @author Alexey Andreev
 */
public class MethodReference implements Serializable {
    private String className;
    private String name;
    private ValueType[] signature;
    private transient MethodDescriptor descriptor;
    private transient String reprCache;

    public MethodReference(String className, MethodDescriptor descriptor) {
        this.className = className;
        this.descriptor = descriptor;
        this.name = descriptor.getName();
        this.signature = descriptor.getSignature();
    }

    /**
     * <p>Creates a new reference to a method.</p>
     *
     * <p>For example, here is how you should call this constructor to create a reference to
     * the <code>Integer.valueOf(int)</code> method:
     *
     * <pre>
     * new MethodReference("java.lang.Integer", "valueOf",
     *         ValueType.INT, ValueType.object("java.lang.Integer"))
     * </pre>
     *
     * @param className the name of the class that owns the method.
     * @param name the name of the method.
     * @param signature descriptor of a method, as described in VM spec. The last element is
     * a type of a returning value, and all the remaining elements are types of arguments.
     */
    public MethodReference(String className, String name, ValueType... signature) {
        this.className = className;
        this.name = name;
        this.signature = Arrays.copyOf(signature, signature.length);
    }

    public MethodReference(Class<?> cls, String name, Class<?>... signature) {
        this(cls.getName(), name, convertSignature(signature));
    }

    private static ValueType[] convertSignature(Class<?>... signature) {
        ValueType[] types = new ValueType[signature.length];
        for (int i = 0; i < types.length; ++i) {
            types[i] = ValueType.parse(signature[i]);
        }
        return types;
    }

    public String getClassName() {
        return className;
    }

    public MethodDescriptor getDescriptor() {
        if (descriptor == null) {
            descriptor = new MethodDescriptor(name, signature);
        }
        return descriptor;
    }

    public int parameterCount() {
        return signature.length - 1;
    }

    public ValueType parameterType(int index) {
        if (index >= signature.length + 1) {
            throw new IndexOutOfBoundsException("Index " + index + " is greater than size " + (signature.length - 1));
        }
        return signature[index];
    }

    public ValueType[] getParameterTypes() {
        return Arrays.copyOf(signature, signature.length - 1);
    }

    public ValueType[] getSignature() {
        return Arrays.copyOf(signature, signature.length);
    }

    public ValueType getReturnType() {
        return signature[signature.length - 1];
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MethodReference)) {
            return false;
        }
        MethodReference other = (MethodReference) obj;
        return toString().equals(other.toString());
    }

    @Override
    @JsonValue
    public String toString() {
        if (reprCache == null) {
            reprCache = className + "." + name + signatureToString();
        }
        return reprCache;
    }

    @JsonCreator
    public static MethodReference parse(String string) {
        MethodReference reference = parseIfPossible(string);
        if (reference == null) {
            throw new IllegalArgumentException("Illegal method reference: " + string);
        }
        return reference;
    }

    public static MethodReference parseIfPossible(String string) {
        int index = string.lastIndexOf('.');
        if (index < 1) {
            return null;
        }
        String className = string.substring(0, index);
        MethodDescriptor desc = MethodDescriptor.parseIfPossible(string.substring(index + 1));
        return desc != null ? new MethodReference(className, desc) : null;
    }

    public static MethodReference parse(Method method) {
        ValueType[] signature = Stream.concat(Arrays.stream(method.getParameterTypes()).map(ValueType::parse),
                Stream.of(ValueType.parse(method.getReturnType()))).toArray(ValueType[]::new);
        return new MethodReference(method.getDeclaringClass().getName(), method.getName(), signature);
    }

    public String signatureToString() {
        return getDescriptor().signatureToString();
    }
}
