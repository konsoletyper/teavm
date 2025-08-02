/*
 *  Copyright 2018 Alexey Andreev.
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

import com.carrotsearch.hppc.ObjectByteHashMap;
import com.carrotsearch.hppc.ObjectByteMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.teavm.common.OptionalPredicate;

public class ClassHierarchy {
    private final ClassReaderSource classSource;
    private final Map<String, OptionalPredicate<ValueType>> superclassPredicateCache = new HashMap<>();
    private final Map<String, Map<MethodDescriptor, Optional<MethodReader>>> resolveMethodCache = new HashMap<>();
    private final Map<String, Map<String, Optional<FieldReader>>> resolveFieldCache = new HashMap<>();

    public ClassHierarchy(ClassReaderSource classSource) {
        this.classSource = classSource;
        superclassPredicateCache.put("java.lang.Object", (c, d) -> true);
    }

    public ClassReaderSource getClassSource() {
        return classSource;
    }

    public boolean isSuperType(ValueType superType, ValueType subType, boolean defaultValue) {
        if (superType.equals(subType)) {
            return true;
        }
        if (superType instanceof ValueType.Primitive || subType instanceof ValueType.Primitive) {
            return false;
        }
        if (superType.isObject("java.lang.Object")) {
            return true;
        }
        if (superType instanceof ValueType.Object && subType instanceof ValueType.Object) {
            return isSuperType(((ValueType.Object) superType).getClassName(),
                    ((ValueType.Object) subType).getClassName(), defaultValue);
        } else if (superType instanceof ValueType.Array & subType instanceof ValueType.Array) {
            return isSuperType(((ValueType.Array) superType).getItemType(), ((ValueType.Array) subType).getItemType(),
                    defaultValue);
        } else {
            return false;
        }
    }

    public boolean isSuperType(String superType, String subType, boolean defaultValue) {
        if (subType.equals(superType)) {
            return true;
        }
        return getSuperclassPredicate(superType).test(ValueType.object(subType), defaultValue);
    }

    public MethodReader resolve(MethodReference method) {
        return resolve(method.getClassName(), method.getDescriptor());
    }

    public MethodReader resolve(String className, MethodDescriptor method) {
        Map<MethodDescriptor, Optional<MethodReader>> cache = resolveMethodCache.computeIfAbsent(className,
                k -> new HashMap<>());
        Optional<MethodReader> opt = cache.get(method);
        if (opt == null) {
            MethodReader reader = null;
            ClassReader cls = classSource.get(className);
            if (cls != null) {
                reader = cls.getMethod(method);
                if (reader == null && cls.getParent() != null) {
                    reader = resolve(cls.getParent(), method);
                }
                if (reader == null) {
                    for (String itf : cls.getInterfaces()) {
                        reader = resolve(itf, method);
                        if (reader != null) {
                            break;
                        }
                    }
                }
            }
            opt = Optional.ofNullable(reader);
            cache.put(method, opt);
        }
        return opt.orElse(null);
    }

    public FieldReader resolve(FieldReference field) {
        return resolve(field.getClassName(), field.getFieldName());
    }

    public FieldReader resolve(String className, String fieldName) {
        Map<String, Optional<FieldReader>> cache = resolveFieldCache.computeIfAbsent(className,
                k -> new HashMap<>());
        Optional<FieldReader> opt = cache.get(fieldName);
        if (opt == null) {
            FieldReader reader = null;
            ClassReader cls = classSource.get(className);
            if (cls != null) {
                if (cls.getParent() != null) {
                    reader = resolve(cls.getParent(), fieldName);
                }
                if (reader == null) {
                    for (String itf : cls.getInterfaces()) {
                        reader = resolve(itf, fieldName);
                        if (reader != null) {
                            break;
                        }
                    }
                }
            }
            opt = Optional.ofNullable(reader);
            cache.put(fieldName, opt);
        }
        return opt.orElse(null);
    }

    public OptionalPredicate<ValueType> getSuperclassPredicate(String superclass) {
        if (superclass.equals("java.lang.Object")) {
            return objectPredicate;
        }
        return superclassPredicateCache.computeIfAbsent(superclass, SuperclassPredicate::new);
    }

    public OptionalPredicate<ValueType> getSupertypePredicate(ValueType supertype) {
        if (supertype instanceof ValueType.Object) {
            return getSuperclassPredicate(((ValueType.Object) supertype).getClassName());
        } else if (supertype instanceof ValueType.Array) {
            return new SuperArrayPredicate(getSupertypePredicate(((ValueType.Array) supertype).getItemType()));
        } else {
            return (value, defaultResult) -> value == supertype;
        }
    }

    private static class SuperArrayPredicate implements OptionalPredicate<ValueType> {
        private OptionalPredicate<ValueType> itemPredicate;

        SuperArrayPredicate(OptionalPredicate<ValueType> itemPredicate) {
            this.itemPredicate = itemPredicate;
        }

        @Override
        public boolean test(ValueType value, boolean defaultResult) {
            if (value instanceof ValueType.Array) {
                return itemPredicate.test(((ValueType.Array) value).getItemType(), defaultResult);
            }
            return false;
        }
    }

    OptionalPredicate<ValueType> objectPredicate = (value, defaultResult) -> !(value instanceof ValueType.Primitive);

    private class SuperclassPredicate implements OptionalPredicate<ValueType> {
        private final String superclass;
        private final ObjectByteMap<String> cache = new ObjectByteHashMap<>(100, 0.5);

        SuperclassPredicate(String superclass) {
            this.superclass = superclass;
        }

        @Override
        public boolean test(ValueType value, boolean defaultResult) {
            if (!(value instanceof ValueType.Object)) {
                return false;
            }
            var className = ((ValueType.Object) value).getClassName();
            switch (test(className)) {
                case 1:
                    return true;
                case 2:
                    return false;
                default:
                    return defaultResult;
            }
        }

        byte test(String value) {
            if (value.equals(superclass)) {
                return 1;
            }
            byte result = cache.get(value);
            if (result == 0) {
                result = testCacheMiss(value);
                cache.put(value, result);
            }
            return result;
        }

        byte testCacheMiss(String value) {
            if (value.equals(superclass)) {
                return 1;
            }

            ClassReader cls = classSource.get(value);
            if (cls == null) {
                return 2;
            }

            if (cls.getParent() != null) {
                if (test(cls.getParent()) == 1) {
                    return 1;
                }
            }

            for (String itf : cls.getInterfaces()) {
                if (test(itf) == 1) {
                    return 1;
                }
            }

            return 2;
        }
    }
}
