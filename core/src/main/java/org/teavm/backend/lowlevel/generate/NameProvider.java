/*
 *  Copyright 2019 konsoletyper.
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

import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public interface NameProvider {
    String forMethod(MethodReference method);

    String forVirtualMethod(MethodDescriptor method);

    String forStaticField(FieldReference field);

    String forMemberField(FieldReference field);

    String forClass(String className);

    String forClassInitializer(String className);

    String forClassSystemInitializer(ValueType type);

    String forClassClass(String className);

    String forClassInstance(ValueType type);

    String forSupertypeFunction(ValueType type);
}
