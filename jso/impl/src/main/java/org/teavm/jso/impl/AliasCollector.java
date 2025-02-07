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
package org.teavm.jso.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.teavm.model.ClassReader;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;

public class AliasCollector {
    private AliasCollector() {
    }

    public static boolean isStaticMember(MethodReader method) {
        return !isInstanceMember(method);
    }

    public static boolean isInstanceMember(MethodReader method) {
        return method.getAnnotations().get(JSInstanceExpose.class.getName()) != null;
    }

    public static Members collectMembers(ClassReader classReader, Predicate<MethodReader> filter) {
        var methods = new HashMap<String, MethodReference>();
        var properties = new HashMap<String, PropertyInfo>();
        MethodReference constructor = null;
        for (var method : classReader.getMethods()) {
            if (!filter.test(method)) {
                continue;
            }
            var methodAlias = getPublicAlias(method);
            if (methodAlias != null) {
                switch (methodAlias.kind) {
                    case METHOD:
                        methods.put(methodAlias.name, method.getReference());
                        break;
                    case GETTER: {
                        var propInfo = properties.computeIfAbsent(methodAlias.name, k -> new PropertyInfo());
                        propInfo.getter = method.getReference();
                        break;
                    }
                    case SETTER: {
                        var propInfo = properties.computeIfAbsent(methodAlias.name, k -> new PropertyInfo());
                        propInfo.setter = method.getReference();
                        break;
                    }
                    case CONSTRUCTOR:
                        constructor = method.getReference();
                        break;
                }
            }
        }
        return new Members(methods, properties, constructor);
    }

    public static Alias getPublicAlias(MethodReader method) {
        var annot = method.getAnnotations().get(JSMethodToExpose.class.getName());
        if (annot != null) {
            return new Alias(annot.getValue("name").getString(), AliasKind.METHOD);
        }

        annot = method.getAnnotations().get(JSGetterToExpose.class.getName());
        if (annot != null) {
            return new Alias(annot.getValue("name").getString(), AliasKind.GETTER);
        }

        annot = method.getAnnotations().get(JSSetterToExpose.class.getName());
        if (annot != null) {
            return new Alias(annot.getValue("name").getString(), AliasKind.SETTER);
        }

        annot = method.getAnnotations().get(JSConstructorToExpose.class.getName());
        if (annot != null) {
            return new Alias(null, AliasKind.CONSTRUCTOR);
        }

        return null;
    }

    public static class Members {
        public final Map<String, MethodReference> methods;
        public final Map<String, PropertyInfo> properties;
        public final MethodReference constructor;

        Members(Map<String, MethodReference> methods, Map<String, PropertyInfo> properties,
                MethodReference constructor) {
            this.methods = methods;
            this.properties = properties;
            this.constructor = constructor;
        }
    }


    public static class PropertyInfo {
        public MethodReference getter;
        public MethodReference setter;
    }

    public static class Alias {
        public final String name;
        public final AliasKind kind;

        Alias(String name, AliasKind kind) {
            this.name = name;
            this.kind = kind;
        }
    }

    public enum AliasKind {
        METHOD,
        GETTER,
        SETTER,
        CONSTRUCTOR
    }
}
