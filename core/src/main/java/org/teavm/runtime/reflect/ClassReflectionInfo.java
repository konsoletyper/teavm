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

import org.teavm.interop.Intrinsified;
import org.teavm.interop.Unmanaged;

public final class ClassReflectionInfo extends ReflectionInfo {
    @Unmanaged
    @Intrinsified
    public native int annotationCount();

    @Unmanaged
    @Intrinsified
    public native AnnotationInfo annotation(int index);

    @Unmanaged
    @Intrinsified
    public native int fieldCount();

    @Unmanaged
    @Intrinsified
    public native FieldInfo field(int index);

    @Unmanaged
    @Intrinsified
    public native int methodCount();

    @Unmanaged
    @Intrinsified
    public native MethodInfo method(int index);

    @Unmanaged
    @Intrinsified
    public native int typeParameterCount();

    @Unmanaged
    @Intrinsified
    public native TypeVariableInfo typeParameter(int index);

    @Unmanaged
    @Intrinsified
    public native int innerClassCount();

    @Intrinsified
    public native ClassInfo innerClass(int index);
}
