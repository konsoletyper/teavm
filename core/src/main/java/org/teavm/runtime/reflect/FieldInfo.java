/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.runtime.reflect;

import org.teavm.runtime.StringInfo;

public class FieldInfo {
    public native final StringInfo name();

    public native final int modifiers();

    public native final int annotationCount();

    public native final AnnotationInfo annotation(int index);

    public native final ClassInfo type();

    public native final GenericTypeInfo genericType();

    public native final FieldReader reader();

    public native final FieldWriter writer();
}
