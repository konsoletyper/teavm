/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.classlib.impl.reflection;

import java.lang.annotation.Annotation;
import org.teavm.backend.javascript.spi.InjectedBy;

public class FieldInfo {
    @InjectedBy(MemberInfoGenerator.class)
    public native final String name();

    @InjectedBy(MemberInfoGenerator.class)
    public native final int modifiers();

    @InjectedBy(MemberInfoGenerator.class)
    public native final int accessLevel();

    @InjectedBy(MemberInfoGenerator.class)
    public native final Annotation[] annotations();

    @InjectedBy(MemberInfoGenerator.class)
    public native final Class<?> type();

    @InjectedBy(MemberInfoGenerator.class)
    public native final Object genericType();

    @InjectedBy(MemberInfoGenerator.class)
    public native final FieldReader reader();

    @InjectedBy(MemberInfoGenerator.class)
    public native final FieldWriter writer();
}
