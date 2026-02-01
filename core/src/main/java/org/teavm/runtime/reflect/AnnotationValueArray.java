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

public final class AnnotationValueArray extends ReflectionInfo {
    public native int size();

    public native boolean getBoolean(int index);

    public native byte getByte(int index);

    public native short getShort(int index);

    public native char getChar(int index);

    public native int getInt(int index);

    public native long getLong(int index);

    public native float getFloat(int index);

    public native double getDouble(int index);

    public native DerivedClassInfo getClass(int index);

    public native short getEnum(int index);

    public native StringInfo getString(int index);

    public native AnnotationData getAnnotation(int index);
}
