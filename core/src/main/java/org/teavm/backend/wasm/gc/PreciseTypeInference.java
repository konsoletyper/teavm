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

import org.teavm.model.ClassHierarchy;
import org.teavm.model.MethodReference;
import org.teavm.model.PrimitiveType;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.analysis.BaseTypeInference;

public class PreciseTypeInference extends BaseTypeInference<PreciseValueType> {
    public static final PreciseValueType OBJECT_TYPE = new PreciseValueType(ValueType.object("java.lang.Object"),
            false);
    private ClassHierarchy hierarchy;

    public PreciseTypeInference(Program program, MethodReference reference, ClassHierarchy hierarchy) {
        super(program, reference);
        this.hierarchy = hierarchy;
    }

    @Override
    protected PreciseValueType mapType(ValueType type) {
        return new PreciseValueType(type, false);
    }

    @Override
    protected PreciseValueType nullType() {
        return null;
    }

    @Override
    protected PreciseValueType merge(PreciseValueType a, PreciseValueType b) {
        if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        } else {
            if (a.valueType instanceof ValueType.Primitive && b.valueType instanceof ValueType.Primitive) {
                if (a.valueType != b.valueType) {
                    var firstKind = ((ValueType.Primitive) a.valueType).getKind();
                    var secondKind = ((ValueType.Primitive) b.valueType).getKind();
                    if (isIntegerType(firstKind) && isIntegerType(secondKind)) {
                        return new PreciseValueType(ValueType.INTEGER, false);
                    }
                    return OBJECT_TYPE;
                } else {
                    return a;
                }
            } else if (a.valueType instanceof ValueType.Array) {
                if (b.valueType instanceof ValueType.Array) {
                    var p = new PreciseValueType(((ValueType.Array) a.valueType).getItemType(), false);
                    var q = new PreciseValueType(((ValueType.Array) b.valueType).getItemType(), false);
                    return new PreciseValueType(ValueType.arrayOf(merge(p, q).valueType), a.isArrayUnwrap);
                } else {
                    return OBJECT_TYPE;
                }
            } else if (b.valueType instanceof ValueType.Array) {
                return OBJECT_TYPE;
            } else if (a.valueType instanceof ValueType.Object) {
                if (b.valueType instanceof ValueType.Object) {
                    var p = ((ValueType.Object) a.valueType).getClassName();
                    var q = ((ValueType.Object) b.valueType).getClassName();
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
                    var result = ValueType.object(WasmGCUtil.findCommonSuperclass(hierarchy, first, second));
                    return new PreciseValueType(result, false);
                } else {
                    return OBJECT_TYPE;
                }
            } else {
                return OBJECT_TYPE;
            }
        }
    }

    private boolean isIntegerType(PrimitiveType type) {
        switch (type) {
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case CHARACTER:
            case INTEGER:
                return true;
            default:
                return false;
        }
    }

    @Override
    protected PreciseValueType elementType(PreciseValueType valueType) {
        return new PreciseValueType(((ValueType.Array) valueType.valueType).getItemType(), false);
    }

    @Override
    protected PreciseValueType arrayType(PreciseValueType preciseValueType) {
        return new PreciseValueType(ValueType.arrayOf(preciseValueType.valueType), false);
    }

    @Override
    protected PreciseValueType arrayUnwrapType(PreciseValueType type) {
        return new PreciseValueType(type.valueType, true);
    }

    @Override
    protected PreciseValueType arrayWrapType(PreciseValueType type) {
        return new PreciseValueType(type.valueType, false);
    }
}
