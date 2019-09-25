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

public interface AliasProvider {
    String getFieldAlias(FieldReference field);

    ScopedName getStaticFieldAlias(FieldReference field);

    ScopedName getStaticMethodAlias(MethodReference method);

    String getMethodAlias(MethodDescriptor method);

    ScopedName getClassAlias(String className);

    String getFunctionAlias(String name);

    ScopedName getClassInitAlias(String className);

    String getScopeAlias();
}
