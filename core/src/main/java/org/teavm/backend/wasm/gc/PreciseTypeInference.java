/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.gc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.analysis.BaseTypeInference;

public class PreciseTypeInference extends BaseTypeInference<ValueType> {
    public static final ValueType OBJECT_TYPE = ValueType.object("java.lang.Object");
    private ClassHierarchy hierarchy;

    public PreciseTypeInference(Program program, MethodReference reference, ClassHierarchy hierarchy) {
        super(program, reference);
        this.hierarchy = hierarchy;
    }

    @Override
    protected ValueType mapType(ValueType type) {
        return type;
    }

    @Override
    protected ValueType nullType() {
        return null;
    }

    @Override
    protected ValueType merge(ValueType a, ValueType b) {
        if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        } else {
            if (a instanceof ValueType.Primitive && b instanceof ValueType.Primitive) {
                if (a != b) {
                    return OBJECT_TYPE;
                } else {
                    return a;
                }
            } else if (a instanceof ValueType.Array) {
                if (b instanceof ValueType.Array) {
                    var p = ((ValueType.Array) a).getItemType();
                    var q = ((ValueType.Array) b).getItemType();
                    return ValueType.arrayOf(merge(p, q));
                } else {
                    return OBJECT_TYPE;
                }
            } else if (b instanceof ValueType.Array) {
                return OBJECT_TYPE;
            } else if (a instanceof ValueType.Object) {
                if (b instanceof ValueType.Object) {
                    var p = ((ValueType.Object) a).getClassName();
                    var q = ((ValueType.Object) b).getClassName();
                    if (p.equals(q)) {
                        return a;
                    }
                    var first = hierarchy.getClassSource().get(p);
                    if (first == null) {
                        p = "java.lang.Object";
                    }
                    var second = hierarchy.getClassSource().get(q);
                    if (second == null) {
                        q = "java.lang.Object";
                    }
                    if (hierarchy.isSuperType(p, q, false)) {
                        return a;
                    } else if (hierarchy.isSuperType(q, p, false)) {
                        return b;
                    }
                    return ValueType.object(findCommonSuperclass(first, second));
                } else {
                    return OBJECT_TYPE;
                }
            } else {
                return OBJECT_TYPE;
            }
        }
    }

    private String findCommonSuperclass(ClassReader a, ClassReader b) {
        var firstPath = findPathToRoot(a);
        Collections.reverse(firstPath);
        var secondPath = findPathToRoot(b);
        Collections.reverse(secondPath);
        if (firstPath.get(0) != secondPath.get(0)) {
            return "java.lang.Object";
        }
        var max = Math.max(firstPath.size(), secondPath.size());
        var index = 1;
        while (index < max && firstPath.get(index) == secondPath.get(index)) {
            ++index;
        }
        return firstPath.get(index).getName();
    }

    private List<ClassReader> findPathToRoot(ClassReader cls) {
        var path = new ArrayList<ClassReader>();
        while (cls != null) {
            path.add(cls);
            cls = cls.getParent() != null ? hierarchy.getClassSource().get(cls.getParent()) : null;
        }
        return path;
    }

    @Override
    protected ValueType elementType(ValueType valueType) {
        return ((ValueType.Array) valueType).getItemType();
    }
}
