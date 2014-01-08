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
 *
 * @author konsoletyper
 */
public class MethodReference {
    private String className;
    private MethodDescriptor descriptor;

    public MethodReference(String className, MethodDescriptor descriptor) {
        this.className = className;
        this.descriptor = descriptor;
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
