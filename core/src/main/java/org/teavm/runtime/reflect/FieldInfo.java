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
import org.teavm.runtime.StringInfo;

public final class FieldInfo extends ReflectionInfo {
    @Unmanaged
    @Intrinsified
    public native StringInfo name();

    @Unmanaged
    @Intrinsified
    public native int modifiers();

    @Unmanaged
    @Intrinsified
    public native DerivedClassInfo type();

    @Unmanaged
    @Intrinsified
    public native Object read(Object instance);

    @Unmanaged
    @Intrinsified
    public native boolean readAsBoolean(Object instance);

    @Unmanaged
    @Intrinsified
    public native byte readAsByte(Object instance);

    @Unmanaged
    @Intrinsified
    public native short readAsShort(Object instance);

    @Unmanaged
    @Intrinsified
    public native char readAsChar(Object instance);

    @Unmanaged
    @Intrinsified
    public native int readAsInt(Object instance);

    @Unmanaged
    @Intrinsified
    public native long readAsLong(Object instance);

    @Unmanaged
    @Intrinsified
    public native float readAsFloat(Object instance);

    @Unmanaged
    @Intrinsified
    public native double readAsDouble(Object instance);

    @Unmanaged
    @Intrinsified
    public native void write(Object instance, Object value);

    @Unmanaged
    @Intrinsified
    public native void write(Object instance, boolean value);

    @Unmanaged
    @Intrinsified
    public native void write(Object instance, byte value);

    @Unmanaged
    @Intrinsified
    public native void write(Object instance, short value);

    @Unmanaged
    @Intrinsified
    public native void write(Object instance, char value);

    @Unmanaged
    @Intrinsified
    public native void write(Object instance, int value);

    @Unmanaged
    @Intrinsified
    public native void write(Object instance, long value);

    @Unmanaged
    @Intrinsified
    public native void write(Object instance, float value);

    @Unmanaged
    @Intrinsified
    public native void write(Object instance, double value);

    @Unmanaged
    @Intrinsified
    public native FieldReflectionInfo reflection();
}
