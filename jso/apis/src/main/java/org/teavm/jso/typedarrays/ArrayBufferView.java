/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.jso.typedarrays;

import org.teavm.jso.JSByRef;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArrayReader;

public abstract class ArrayBufferView implements JSObject {
    protected ArrayBufferView() {
    }

    @JSProperty
    public abstract int getLength();

    @JSProperty
    public abstract int getByteLength();

    @JSProperty
    public abstract int getByteOffset();

    @JSProperty
    public abstract ArrayBuffer getBuffer();

    public abstract void set(ArrayBufferView other, int offset);

    public abstract void set(ArrayBufferView other);

    public abstract void set(JSArrayReader<?> other, int offset);

    public abstract void set(JSArrayReader<?> other);

    public abstract void set(@JSByRef byte[] other, int offset);

    public abstract void set(@JSByRef byte[] other);

    public abstract void set(@JSByRef short[] other, int offset);

    public abstract void set(@JSByRef short[] other);

    public abstract void set(@JSByRef int[] other, int offset);

    public abstract void set(@JSByRef int[] other);

    public abstract void set(@JSByRef float[] other, int offset);

    public abstract void set(@JSByRef float[] other);

    public abstract void set(@JSByRef double[] other, int offset);

    public abstract void set(@JSByRef double[] other);
}
