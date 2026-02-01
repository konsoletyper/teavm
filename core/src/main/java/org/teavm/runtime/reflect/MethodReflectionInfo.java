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

public final class MethodReflectionInfo {
    public native GenericTypeInfo genericReturnType();

    public native int genericParameterTypeCount();

    public native GenericTypeInfo genericParameterType(int index);

    public native int annotationCount();

    public native AnnotationInfo annotation(int index);

    public native int typeParameterCount();

    public native TypeVariableInfo typeParameter(int index);
}
