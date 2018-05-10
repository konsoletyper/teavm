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
package org.teavm.backend.lowlevel.generate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.interop.Export;
import org.teavm.interop.Import;
import org.teavm.model.AnnotationReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public abstract class LowLevelNameProvider {
    private ClassReaderSource classSource;

    protected Set<String> occupiedTopLevelNames = new HashSet<>();
    protected Map<String, Set<String>> occupiedVtableNames = new HashMap<>();
    protected Map<String, Set<String>> occupiedClassNames = new HashMap<>();

    protected Map<MethodReference, String> methodNames = new HashMap<>();
    protected Map<MethodReference, String> virtualMethodNames = new HashMap<>();

    protected Map<FieldReference, String> staticFieldNames = new HashMap<>();
    protected Map<FieldReference, String> memberFieldNames = new HashMap<>();

    protected Map<String, String> classNames = new HashMap<>();
    protected Map<String, String> classInitializerNames = new HashMap<>();
    protected Map<String, String> classClassNames = new HashMap<>();
    protected Map<ValueType, String> classInstanceNames = new HashMap<>();
    protected Map<ValueType, String> supertypeNames = new HashMap<>();

    private Set<ValueType> types = new LinkedHashSet<>();

    public LowLevelNameProvider(ClassReaderSource classSource) {
        this.classSource = classSource;
    }

    public String forMethod(MethodReference method) {
        return methodNames.computeIfAbsent(method, k -> {
            String specialName = getSpecialName(k);
            return specialName == null ? pickUnoccupied(suggestForMethod(k)) : specialName;
        });
    }

    public String forVirtualMethod(MethodReference method) {
        return virtualMethodNames.computeIfAbsent(method, k -> {
            Set<String> occupied = occupiedVtableNames.computeIfAbsent(k.getClassName(),
                    c -> new HashSet<>(Arrays.asList("parent")));
            return pickUnoccupied(k.getName(), occupied);
        });
    }

    private String getSpecialName(MethodReference methodReference) {
        MethodReader method = classSource.resolve(methodReference);
        if (method == null) {
            return null;
        }

        AnnotationReader exportAnnot = method.getAnnotations().get(Export.class.getName());
        if (exportAnnot != null) {
            return exportAnnot.getValue("name").getString();
        }

        AnnotationReader importAnnot = method.getAnnotations().get(Import.class.getName());
        if (importAnnot != null) {
            return importAnnot.getValue("name").getString();
        }

        return null;
    }

    public String forStaticField(FieldReference field) {
        return staticFieldNames.computeIfAbsent(field, k -> pickUnoccupied(suggestForStaticField(k)));
    }

    public String forMemberField(FieldReference field) {
        return memberFieldNames.computeIfAbsent(field, k -> {
            Set<String> occupied = occupiedClassNames.computeIfAbsent(k.getClassName(),
                    c -> new HashSet<>(Arrays.asList("parent")));
            return pickUnoccupied(sanitize(field.getFieldName()), occupied);
        });
    }

    public String forClass(String className) {
        return classNames.computeIfAbsent(className, k -> pickUnoccupied(suggestForClass(k)));
    }

    public String forClassInitializer(String className) {
        return classInitializerNames.computeIfAbsent(className, k -> pickUnoccupied("initclass_" + suggestForClass(k)));
    }

    public String forClassClass(String className) {
        types.add(ValueType.object(className));
        return classClassNames.computeIfAbsent(className, k -> pickUnoccupied(suggestForClass(k) + "_VT"));
    }

    public String forClassInstance(ValueType type) {
        types.add(type);
        return classInstanceNames.computeIfAbsent(type, k -> pickUnoccupied(suggestForType(k) + "_Cls"));
    }

    public String forSupertypeFunction(ValueType type) {
        return supertypeNames.computeIfAbsent(type, k -> pickUnoccupied("supertypeof_" + suggestForType(k)));
    }

    private String suggestForMethod(MethodReference method) {
        StringBuilder sb = new StringBuilder();
        suggestForClass(method.getClassName(), sb);
        sb.append('_');
        sb.append(sanitize(method.getName()));
        return sb.toString();
    }

    private String suggestForStaticField(FieldReference field) {
        StringBuilder sb = new StringBuilder();
        suggestForClass(field.getClassName(), sb);
        sb.append('_');
        sb.append(sanitize(field.getFieldName()));
        return sb.toString();
    }

    private String suggestForClass(String className) {
        StringBuilder sb = new StringBuilder();
        suggestForClass(className, sb);
        return sb.toString();
    }

    private void suggestForClass(String className, StringBuilder sb) {
        int index = 0;
        while (true) {
            int next = className.indexOf('.', index);
            if (next < 0) {
                if (index > 0) {
                    sb.append('_');
                    sb.append(sanitize(className.substring(index)));
                } else {
                    sb.append(sanitize(className));
                }
                return;
            }

            sb.append(sanitize(String.valueOf(className.charAt(index))));
            index = next + 1;
        }
    }

    private String suggestForType(ValueType type) {
        StringBuilder sb = new StringBuilder();
        suggestForType(type, sb);
        return sb.toString();
    }

    private void suggestForType(ValueType type, StringBuilder sb) {
        if (type instanceof ValueType.Object) {
            suggestForClass(((ValueType.Object) type).getClassName(), sb);
        } else if (type instanceof ValueType.Array) {
            sb.append("Arr_");
            suggestForType(((ValueType.Array) type).getItemType(), sb);
        } else {
            sb.append(type.toString());
        }
    }

    private String sanitize(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); ++i) {
            char c = name.charAt(i);
            switch (c) {
                case '>':
                case '<':
                case '$':
                    sb.append('_');
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    private String pickUnoccupied(String name) {
        return pickUnoccupied(name, occupiedTopLevelNames);
    }

    private String pickUnoccupied(String name, Set<String> occupied) {
        String result = name;
        int index = 0;
        while (!occupied.add(result)) {
            result = name + "_" + index++;
        }

        return result;
    }

    public Set<? extends ValueType> getTypes() {
        return types;
    }
}
