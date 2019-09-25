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
    private Map<String, Map<MethodDescriptor, MethodReference>> referenceCache = new HashMap<>();
    private Map<FieldReference, FieldReference> fieldRefenceCache = new HashMap<>();
    private Map<MethodDescriptor, MethodDescriptor> descriptorCache = new HashMap<>();
    private Map<ValueType, ValueType> valueTypeCache = new HashMap<>();
    private Map<GenericValueType, GenericValueType> genericValueTypeCache = new HashMap<>();
    private Map<String, String> stringCache = new HashMap<>();
    private Map<String, MethodDescriptor> descriptorParseCache = new HashMap<>();
    private Map<String, ValueType> valueTypeParseCache = new HashMap<>();

    public MethodReference getCached(MethodReference reference) {
        return getCached(reference.getClassName(), reference.getDescriptor());
    }

    public MethodReference getCached(String className, MethodDescriptor descriptor) {
        return referenceCache
                .computeIfAbsent(className, key -> new HashMap<>())
                .computeIfAbsent(getCached(descriptor), key -> new MethodReference(className, key));
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

    public GenericValueType getCached(GenericValueType valueType) {
        if (valueType instanceof GenericValueType.Primitive
                || valueType instanceof GenericValueType.Variable
                || valueType instanceof GenericValueType.Void) {
            return valueType;
        }

        GenericValueType result = genericValueTypeCache.get(valueType);
        if (result == null) {
            result = valueType;
            if (result instanceof GenericValueType.Object) {
                GenericValueType.Object objectType = (GenericValueType.Object) result;
                String className = objectType.getClassName();
                String cachedClassName = getCached(className);

                boolean changed = false;
                GenericValueType.Argument[] arguments = objectType.getArguments();
                for (int i = 0; i < arguments.length; ++i) {
                    GenericValueType.Argument argument = arguments[i];
                    if (argument.getValue() != null) {
                        GenericValueType.Reference cachedValue = (GenericValueType.Reference) getCached(
                                argument.getValue());
                        if (cachedValue != argument.getValue()) {
                            changed = true;
                            switch (argument.getKind()) {
                                case COVARIANT:
                                    argument = GenericValueType.Argument.covariant(cachedValue);
                                    break;
                                case CONTRAVARIANT:
                                    argument = GenericValueType.Argument.contravariant(cachedValue);
                                    break;
                                case INVARIANT:
                                    argument = GenericValueType.Argument.invariant(cachedValue);
                                    break;
                            }
                            arguments[i] = argument;
                        }
                    }
                }

                GenericValueType.Object parent = objectType.getParent();
                GenericValueType.Object cachedParent = parent != null
                        ? (GenericValueType.Object) getCached(parent)
                        : null;

                if (changed || className != cachedClassName || parent != cachedParent) {
                    result = new GenericValueType.Object(parent, className, arguments);
                }
            } else if (result instanceof GenericValueType.Array) {
                GenericValueType item = ((GenericValueType.Array) result).getItemType();
                GenericValueType cachedItem = getCached(item);
                if (item != cachedItem) {
                    result = new GenericValueType.Array(cachedItem);
                }
            }
            genericValueTypeCache.put(result, result);
        }

        return result;
    }

    public String getCached(String s) {
        String result = stringCache.get(s);
        if (result == null) {
            result = s;
            stringCache.put(result, result);
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
