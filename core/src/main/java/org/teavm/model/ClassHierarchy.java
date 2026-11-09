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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

        SuperclassPredicate(String superclass) {
            this.superclass = superclass;
        }

        @Override
        public boolean test(ValueType value, boolean defaultResult) {
            if (!(value instanceof ValueType.Object)) {
                return false;
            }
            var className = ((ValueType.Object) value).getClassName();
            return className.equals(superclass) || ancestorsOf(className).contains(superclass);
        }
    }

    // Per class, the set of the class itself and all its superclasses and interfaces.
    // One shared index instead of a per-superclass cache keeps memory linear in the number of classes.
    private final Map<String, Set<String>> ancestors = new HashMap<>();
    private boolean ancestorsIncomplete;

    private Set<String> ancestorsOf(String className) {
        ancestorsIncomplete = false;
        return computeAncestors(className);
    }

    // Classes can appear in the class source later (e.g. generated during dependency analysis), so ancestor
    // sets that depended on a missing class are returned but not memoized.
    private Set<String> computeAncestors(String className) {
        var result = ancestors.get(className);
        if (result != null) {
            return result;
        }
        ClassReader cls = classSource.get(className);
        if (cls == null) {
            ancestorsIncomplete = true;
            return Set.of(className);
        }
        ancestors.put(className, Set.of(className));
        boolean outerIncomplete = ancestorsIncomplete;
        ancestorsIncomplete = false;
        result = new HashSet<>();
        result.add(className);
        if (cls.getParent() != null) {
            result.addAll(computeAncestors(cls.getParent()));
        }
        for (String itf : cls.getInterfaces()) {
            result.addAll(computeAncestors(itf));
        }
        if (ancestorsIncomplete) {
            ancestors.remove(className);
        } else {
            ancestors.put(className, result);
        }
        ancestorsIncomplete |= outerIncomplete;
        return result;
    }
}
