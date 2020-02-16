/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.platform;

import org.teavm.interop.NoSideEffects;
import org.teavm.interop.Unmanaged;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface PlatformClassMetadata extends JSObject {
    @JSProperty("item")
    @Unmanaged
    @NoSideEffects
    PlatformClass getArrayItem();

    @JSProperty
    @NoSideEffects
    PlatformSequence<PlatformClass> getSupertypes();

    @JSProperty
    @Unmanaged
    @NoSideEffects
    PlatformClass getSuperclass();

    @JSProperty
    @Unmanaged
    @NoSideEffects
    String getName();

    @JSProperty
    @NoSideEffects
    boolean isPrimitive();

    @JSProperty
    @NoSideEffects
    boolean isEnum();

    @JSProperty
    @NoSideEffects
    int getFlags();

    @JSProperty
    @NoSideEffects
    int getAccessLevel();

    @JSProperty
    @Unmanaged
    @NoSideEffects
    String getSimpleName();

    @JSProperty
    @Unmanaged
    @NoSideEffects
    PlatformClass getEnclosingClass();

    @JSProperty
    @Unmanaged
    @NoSideEffects
    PlatformClass getDeclaringClass();
}
