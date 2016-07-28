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

import java.util.HashMap;
import java.util.Map;

public class ReferenceCache {
    private Map<MethodReference, MethodReference> referenceCache = new HashMap<>();
    private Map<FieldReference, FieldReference> fieldRefenceCache = new HashMap<>();
    private Map<MethodDescriptor, MethodDescriptor> descriptorCache = new HashMap<>();
    private Map<ValueType, ValueType> valueTypeCache = new HashMap<>();
    private Map<String, String> classCache = new HashMap<>();
    private Map<String, MethodReference> referenceParseCache = new HashMap<>();
    private Map<String, MethodDescriptor> descriptorParseCache = new HashMap<>();
    private Map<String, ValueType> valueTypeParseCache = new HashMap<>();

    public MethodReference getCached(MethodReference reference) {
        MethodReference result = referenceCache.get(reference);
        if (result == null) {
            MethodDescriptor descriptor = getCached(reference.getDescriptor());
            String className = getCached(reference.getClassName());
            if (descriptor != reference.getDescriptor() || className != reference.getClassName()) {
                result = new MethodReference(className, descriptor);
            } else {
                result = reference;
            }
            referenceCache.put(result, result);
        }
        return result;
    }

    public MethodDescriptor getCached(MethodDescriptor descriptor) {
        MethodDescriptor result = descriptorCache.get(descriptor);
        if (result == null) {
            result = descriptor;
            ValueType[] signature = descriptor.getSignature();
            boolean signatureChanged = false;
            for (int i = 0; i < signature.length; ++i) {
                ValueType type = signature[i];
                if (type == null) {
                    continue;
                }
                ValueType cachedType = getCached(type);
                if (type != cachedType) {
                    signatureChanged = true;
                    signature[i] = cachedType;
                }
            }
            if (signatureChanged) {
                result = new MethodDescriptor(descriptor.getName(), signature);
            }
            descriptorCache.put(result, result);
        }
        return result;
    }

    public FieldReference getCached(FieldReference reference) {
        FieldReference result = fieldRefenceCache.get(reference);
        if (result == null) {
            result = reference;
            String classNameCached = getCached(reference.getClassName());
            String fieldNameCached = getCached(reference.getFieldName());
            if (classNameCached != reference.getClassName() || fieldNameCached != reference.getFieldName()) {
                result = new FieldReference(classNameCached, fieldNameCached);
            }
            fieldRefenceCache.put(result, result);
        }
        return result;
    }

    public ValueType getCached(ValueType valueType) {
        if (valueType instanceof ValueType.Primitive) {
            return valueType;
        }

        ValueType result = valueTypeCache.get(valueType);
        if (result == null) {
            result = valueType;
            if (result instanceof ValueType.Object) {
                String className = ((ValueType.Object) result).getClassName();
                String cachedClassName = getCached(className);
                if (cachedClassName != className) {
                    result = ValueType.object(cachedClassName);
                }
            } else if (result instanceof ValueType.Array) {
                ValueType item = ((ValueType.Array) result).getItemType();
                ValueType cachedItem = getCached(item);
                if (item != cachedItem) {
                    result = ValueType.arrayOf(cachedItem);
                }
            }
            valueTypeCache.put(result, result);
        }
        return result;
    }

    public String getCached(String className) {
        String result = classCache.get(className);
        if (result == null) {
            result = className;
            classCache.put(result, result);
        }
        return result;
    }

    public MethodReference parseReferenceCached(String value) {
        MethodReference result = referenceParseCache.get(value);
        if (result == null) {
            result = getCached(MethodReference.parse(value));
            referenceParseCache.put(value, result);
        }
        return result;
    }

    public MethodDescriptor parseDescriptorCached(String value) {
        MethodDescriptor result = descriptorParseCache.get(value);
        if (result == null) {
            result = getCached(MethodDescriptor.parse(value));
            descriptorParseCache.put(value, result);
        }
        return result;
    }

    public ValueType parseValueTypeCached(String value) {
        ValueType result = valueTypeParseCache.get(value);
        if (result == null) {
            result = getCached(ValueType.parse(value));
            valueTypeParseCache.put(value, result);
        }
        return result;
    }
}
