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
package org.teavm.backend.c.generate;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.teavm.backend.lowlevel.generate.LowLevelNameProvider;
import org.teavm.model.FieldReference;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeObject;
import org.teavm.runtime.RuntimeReference;
import org.teavm.runtime.RuntimeReferenceQueue;

public class CNameProvider extends LowLevelNameProvider {
    private static final Set<? extends String> keywords = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "auto", "break", "case", "char", "const", "continue", "default", "do", "double", "else",
            "enum", "extern", "float", "for", "goto", "if", "inline", "int", "long", "register", "restrict",
            "return", "short", "signed", "sizeof", "static", "struct", "switch", "typedef", "union",
            "unsigned", "void", "volatile", "while"
    )));

    public CNameProvider() {

        occupiedTopLevelNames.add("TeaVM_Object");
        occupiedTopLevelNames.add("TeaVM_Array");
        occupiedTopLevelNames.add("TeaVM_String");
        occupiedTopLevelNames.add("TeaVM_Class");
        occupiedTopLevelNames.add("TeaVM_Reference");
        occupiedTopLevelNames.add("TeaVM_ReferenceQueue");

        classNames.put(RuntimeObject.class.getName(), "TeaVM_Object");
        classNames.put(Object.class.getName(), "TeaVM_Object");
        classNames.put(String.class.getName(), "TeaVM_String");
        classNames.put(RuntimeClass.class.getName(), "TeaVM_Class");
        classNames.put(RuntimeArray.class.getName(), "TeaVM_Array");
        classNames.put(WeakReference.class.getName(), "TeaVM_Reference");
        classNames.put(ReferenceQueue.class.getName(), "TeaVM_ReferenceQueue");
        classNames.put(RuntimeReference.class.getName(), "TeaVM_Reference");
        classNames.put(RuntimeReferenceQueue.class.getName(), "TeaVM_ReferenceQueue");

        memberFieldNames.put(new FieldReference(RuntimeObject.class.getName(), "classReference"), "header");
        memberFieldNames.put(new FieldReference(RuntimeObject.class.getName(), "hashCode"), "hash");
        memberFieldNames.put(new FieldReference(RuntimeArray.class.getName(), "size"), "size");
        memberFieldNames.put(new FieldReference(String.class.getName(), "characters"), "characters");
        memberFieldNames.put(new FieldReference(String.class.getName(), "hashCode"), "hashCode");

        preserveFieldNames(RuntimeClass.class.getName(), "size", "flags", "tag", "canary", "name", "itemType",
                "arrayType", "isSupertypeOf", "init", "enumValues", "layout", "simpleName", "superinterfaceCount",
                "superinterfaces", "simpleNameCache", "declaringClass", "enclosingClass", "canonicalName",
                "nameCache");
        memberFieldNames.put(new FieldReference(RuntimeClass.class.getName(), "parent"), "superclass");
        preserveFieldNames(RuntimeReference.class.getName(), "queue", "object", "next");
        preserveFieldNames(RuntimeReferenceQueue.class.getName(), "first", "last");

        occupiedClassNames.put(RuntimeObject.class.getName(), new HashSet<>(Arrays.asList("header")));
        occupiedClassNames.put(RuntimeArray.class.getName(), new HashSet<>(Arrays.asList("length")));
    }

    private void preserveFieldNames(String className, String... fieldNames) {
        for (String name : fieldNames) {
            memberFieldNames.put(new FieldReference(className, name), name);
        }
    }

    @Override
    protected Set<? extends String> getKeywords() {
        return keywords;
    }
}
