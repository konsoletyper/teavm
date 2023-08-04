/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.jso.impl;

import org.teavm.jso.JSBody;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.analysis.BaseTypeInference;

class JSTypeInference extends BaseTypeInference<JSType> {
    private JSTypeHelper typeHelper;
    private ClassReaderSource classes;

    JSTypeInference(JSTypeHelper typeHelper, ClassReaderSource classes, Program program, MethodReference reference) {
        super(program, reference);
        this.typeHelper = typeHelper;
        this.classes = classes;
    }

    @Override
    protected JSType mapType(ValueType type) {
        if (type instanceof ValueType.Object) {
            var className = ((ValueType.Object) type).getClassName();
            if (typeHelper.isJavaScriptClass(className)) {
                return JSType.JS;
            }
        } else if (type instanceof ValueType.Array) {
            var elementType = mapType(((ValueType.Array) type).getItemType());
            return JSType.arrayOf(elementType);
        }
        return JSType.JAVA;
    }

    @Override
    protected JSType nullType() {
        return JSType.NULL;
    }

    @Override
    protected JSType merge(JSType a, JSType b) {
        if (a == JSType.NULL) {
            return b;
        } else if (b == JSType.NULL) {
            return a;
        } else if (a == b) {
            return a;
        } else if (a instanceof JSType.ArrayType) {
            if (b instanceof JSType.ArrayType) {
                var elementType = merge(((JSType.ArrayType) a).elementType, ((JSType.ArrayType) b).elementType);
                return JSType.arrayOf(elementType);
            } else if (b == JSType.JAVA) {
                return JSType.JAVA;
            } else {
                return JSType.MIXED;
            }
        } else if (b instanceof JSType.ArrayType) {
            if (a == JSType.JAVA) {
                return JSType.JAVA;
            } else {
                return JSType.MIXED;
            }
        } else {
            return JSType.MIXED;
        }
    }

    @Override
    protected JSType elementType(JSType jsType) {
        return jsType instanceof JSType.ArrayType ? ((JSType.ArrayType) jsType).elementType : JSType.MIXED;
    }

    @Override
    protected JSType methodReturnType(MethodReference methodRef) {
        if (!methodRef.getReturnType().isObject(Object.class)) {
            return mapType(methodRef.getReturnType());
        }
        return isJsMethod(methodRef) ? JSType.MIXED : JSType.JAVA;
    }

    private boolean isJsMethod(MethodReference methodRef) {
        if (typeHelper.isJavaScriptClass(methodRef.getClassName())) {
            return true;
        }
        var method = classes.resolveImplementation(methodRef);
        return method != null && method.getAnnotations().get(JSBody.class.getName()) != null;
    }
}
