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

import static org.teavm.jso.impl.JSMethods.JS_OBJECT_CLASS;
import java.util.HashMap;
import java.util.Map;
import org.teavm.jso.JSClass;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.ValueType;

public class JSTypeHelper {
    private ClassReaderSource classSource;
    private Map<String, Boolean> knownJavaScriptClasses = new HashMap<>();
    private Map<String, Boolean> knownJavaScriptImplementations = new HashMap<>();

    public JSTypeHelper(ClassReaderSource classSource) {
        this.classSource = classSource;
        knownJavaScriptClasses.put(JS_OBJECT_CLASS, true);
    }

    public JSType mapType(ValueType type) {
        if (type instanceof ValueType.Object) {
            var className = ((ValueType.Object) type).getClassName();
            if (isJavaScriptClass(className)) {
                return JSType.JS;
            }
        } else if (type instanceof ValueType.Array) {
            var elementType = mapType(((ValueType.Array) type).getItemType());
            return JSType.arrayOf(elementType);
        }
        return JSType.JAVA;
    }

    public boolean isJavaScriptClass(String className) {
        Boolean isJsClass = knownJavaScriptClasses.get(className);
        if (isJsClass == null) {
            isJsClass = examineIfJavaScriptClass(className);
            knownJavaScriptClasses.put(className, isJsClass);
        }
        return isJsClass;
    }

    public boolean isJavaScriptImplementation(String className) {
        return knownJavaScriptImplementations.computeIfAbsent(className, k ->
                examineIfJavaScriptImplementation(className));
    }

    private boolean examineIfJavaScriptClass(String className) {
        ClassReader cls = classSource.get(className);
        if (cls == null) {
            return false;
        }
        if (!(cls.hasModifier(ElementModifier.INTERFACE) || cls.hasModifier(ElementModifier.ABSTRACT))) {
            if (cls.getAnnotations().get(JSClass.class.getName()) == null) {
                return false;
            }
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
        if (cls == null || cls.getAnnotations().get(JSClass.class.getName()) != null
                || cls.hasModifier(ElementModifier.ABSTRACT)) {
            return false;
        }
        if (cls.getParent() != null) {
            if (isJavaScriptClass(cls.getParent())) {
                return true;
            }
        }
        return cls.getInterfaces().stream().anyMatch(this::isJavaScriptClass);
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
                case LONG:
                case FLOAT:
                case DOUBLE:
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }
}
