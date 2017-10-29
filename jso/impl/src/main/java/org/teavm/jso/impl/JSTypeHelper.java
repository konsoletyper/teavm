/*
 *  Copyright 2014 Alexey Andreev.
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

import java.util.HashMap;
import java.util.Map;
import org.teavm.jso.JSObject;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.ValueType;

class JSTypeHelper {
    private ClassReaderSource classSource;
    private Map<String, Boolean> knownJavaScriptClasses = new HashMap<>();
    private Map<String, Boolean> knownJavaScriptImplementations = new HashMap<>();

    public JSTypeHelper(ClassReaderSource classSource) {
        this.classSource = classSource;
        knownJavaScriptClasses.put(JSObject.class.getName(), true);
    }

    public boolean isJavaScriptClass(String className) {
        return knownJavaScriptClasses.computeIfAbsent(className, k -> examineIfJavaScriptClass(className));
    }

    public boolean isJavaScriptImplementation(String className) {
        return knownJavaScriptImplementations
                .computeIfAbsent(className, k -> examineIfJavaScriptImplementation(className));
    }

    private boolean examineIfJavaScriptClass(String className) {
        ClassReader cls = classSource.get(className);
        if (cls == null || !(cls.hasModifier(ElementModifier.INTERFACE) || cls.hasModifier(ElementModifier.ABSTRACT))) {
            return false;
        }
        if (cls.getParent() != null) {
            if (isJavaScriptClass(cls.getParent())) {
                return true;
            }
        }
        return cls.getInterfaces().stream().anyMatch(this::isJavaScriptClass);
    }

    private boolean examineIfJavaScriptImplementation(String className) {
        if (isJavaScriptClass(className)) {
            return false;
        }
        ClassReader cls = classSource.get(className);
        if (cls == null) {
            return false;
        }
        if (cls.getParent() != null) {
            if (isJavaScriptClass(cls.getParent())) {
                return true;
            }
        }
        return cls.getInterfaces().stream().anyMatch(this::isJavaScriptClass);
    }

    public boolean isSupportedType(ValueType type) {
        if (type == ValueType.VOID) {
            return false;
        }
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case LONG:
                    return false;
                default:
                    return true;
            }
        } else if (type instanceof ValueType.Array) {
            return isSupportedType(((ValueType.Array) type).getItemType());
        } else if (type instanceof ValueType.Object) {
            String typeName = ((ValueType.Object) type).getClassName();
            return typeName.equals("java.lang.String") || isJavaScriptClass(typeName);
        } else {
            return false;
        }
    }

    public boolean isSupportedByRefType(ValueType type) {
        if (!(type instanceof ValueType.Array)) {
            return false;
        }
        ValueType itemType = ((ValueType.Array) type).getItemType();
        if (itemType instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) itemType).getKind()) {
                case BYTE:
                case SHORT:
                case CHARACTER:
                case INTEGER:
                case FLOAT:
                case DOUBLE:
                    return true;
                default:
                    return false;
            }
        } else if (itemType instanceof ValueType.Object) {
            return isJavaScriptClass(((ValueType.Object) itemType).getClassName());
        } else {
            return false;
        }
    }
}
