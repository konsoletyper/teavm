/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.backend.javascript.codegen;

import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

public interface NamingStrategy {
    ScopedName className(String cls);

    String instanceMethodName(MethodDescriptor method);

    ScopedName initializerName(MethodReference method);

    ScopedName methodName(MethodReference method);

    String instanceFieldName(FieldReference field);

    ScopedName fieldName(FieldReference method);

    ScopedName functionName(String name);

    ScopedName classInitializerName(String className);

    String additionalScopeName();

    void reserveName(String name);
}
