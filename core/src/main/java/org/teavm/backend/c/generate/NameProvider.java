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

import java.util.Arrays;
import java.util.HashSet;
import org.teavm.backend.lowlevel.generate.LowLevelNameProvider;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.FieldReference;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeObject;

public class NameProvider extends LowLevelNameProvider {
    public NameProvider(ClassReaderSource classSource) {
        super(classSource);

        occupiedTopLevelNames.add("JavaObject");
        occupiedTopLevelNames.add("JavaArray");
        occupiedTopLevelNames.add("JavaString");
        occupiedTopLevelNames.add("JavaClass");

        classNames.put(RuntimeObject.class.getName(), "JavaObject");
        classNames.put(String.class.getName(), "JavaString");
        classNames.put(RuntimeClass.class.getName(), "JavaClass");
        classNames.put(RuntimeArray.class.getName(), "JavaArray");

        memberFieldNames.put(new FieldReference(RuntimeObject.class.getName(), "classReference"), "header");
        memberFieldNames.put(new FieldReference(RuntimeArray.class.getName(), "size"), "size");
        memberFieldNames.put(new FieldReference(String.class.getName(), "characters"), "characters");
        memberFieldNames.put(new FieldReference(String.class.getName(), "hashCode"), "hashCode");

        occupiedClassNames.put(RuntimeObject.class.getName(), new HashSet<>(Arrays.asList("header")));
        occupiedClassNames.put(RuntimeArray.class.getName(), new HashSet<>(Arrays.asList("length")));
    }
}
