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
public class MethodReference {
    private String className;
    private MethodDescriptor descriptor;

    public MethodReference(String className, MethodDescriptor descriptor) {
        this.className = className;
        this.descriptor = descriptor;
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
        this(className, new MethodDescriptor(name, signature));
    }

    public String getClassName() {
        return className;
    }

    public MethodDescriptor getDescriptor() {
        return descriptor;
    }

    public int parameterCount() {
        return descriptor.parameterCount();
    }

    public ValueType[] getParameterTypes() {
        return descriptor.getParameterTypes();
    }

    public ValueType[] getSignature() {
        return descriptor.getSignature();
    }

    public String getName() {
        return descriptor.getName();
    }

    @Override
    public int hashCode() {
        return className.hashCode() ^ descriptor.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return false;
        }
        if (!(obj instanceof MethodReference)) {
            return false;
        }
        MethodReference other = (MethodReference)obj;
        return className.equals(other.className) && descriptor.equals(other.descriptor);
    }

    @Override
    public String toString() {
        return className + "." + descriptor;
    }
}
