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

import java.util.Arrays;

public class MethodDescriptor {
    private String name;
    private ValueType[] signature;
    private volatile String reprCache;

    public MethodDescriptor(String name, ValueType... signature) {
        if (signature.length < 1) {
            throw new IllegalArgumentException("Signature must be at least 1 element length");
        }
        this.name = name;
        this.signature = Arrays.copyOf(signature, signature.length);
    }

    public MethodDescriptor(String name, Class<?>... signature) {
        if (signature.length < 1) {
            throw new IllegalArgumentException("Signature must be at least 1 element length");
        }
        this.name = name;
        this.signature = new ValueType[signature.length];
        for (int i = 0; i < signature.length; ++i) {
            this.signature[i] = ValueType.parse(signature[i]);
        }
    }

    public String getName() {
        return name;
    }

    public ValueType[] getSignature() {
        return Arrays.copyOf(signature, signature.length);
    }

    public ValueType[] getParameterTypes() {
        return Arrays.copyOf(signature, signature.length - 1);
    }

    public ValueType getResultType() {
        return signature[signature.length - 1];
    }

    public int parameterCount() {
        return signature.length - 1;
    }

    public ValueType parameterType(int index) {
        if (index >= signature.length - 1) {
            throw new IndexOutOfBoundsException(String.valueOf(index) + "/" + (signature.length - 1));
        }
        return signature[index];
    }

    @Override
    public String toString() {
        if (reprCache == null) {
            reprCache = name + signatureToString();
        }
        return reprCache;
    }

    public String signatureToString() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0; i < signature.length - 1; ++i) {
            sb.append(signature[i].toString());
        }
        sb.append(')');
        sb.append(signature[signature.length - 1]);
        return sb.toString();
    }

    public static MethodDescriptor get(MethodHolder method) {
        return new MethodDescriptor(method.getName(), method.getSignature());
    }

    public static MethodDescriptor parse(String text) {
        MethodDescriptor desc = parseIfPossible(text);
        if (desc == null) {
            throw new IllegalArgumentException("Wrong method descriptor: " + text);
        }
        return desc;
    }

    public static MethodDescriptor parseIfPossible(String text) {
        int parenIndex = text.indexOf('(');
        if (parenIndex < 1) {
            return null;
        }
        return new MethodDescriptor(text.substring(0, parenIndex),
                parseSignature(text.substring(parenIndex)));
    }

    public static ValueType[] parseSignature(String text) {
        ValueType[] signature = parseSignatureIfPossible(text);
        if (signature == null) {
            throw new IllegalArgumentException("Illegal method signature: " + text);
        }
        return signature;
    }

    public static ValueType[] parseSignatureIfPossible(String text) {
        if (text.charAt(0) != '(') {
            return null;
        }
        int index = text.indexOf(')', 1);
        if (index < 0) {
            return null;
        }
        ValueType[] params = ValueType.parseManyIfPossible(text.substring(1, index));
        if (params == null) {
            return null;
        }
        ValueType result = ValueType.parse(text.substring(index + 1));
        if (result == null) {
            return null;
        }
        ValueType[] signature = new ValueType[params.length + 1];
        System.arraycopy(params, 0, signature, 0, params.length);
        signature[params.length] = result;
        return signature;
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
         if (!(obj instanceof MethodDescriptor)) {
             return false;
         }
         return toString().equals(obj.toString());
    }
}
