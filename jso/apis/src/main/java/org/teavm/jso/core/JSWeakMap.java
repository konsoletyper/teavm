/*
 *  Copyright 2023 konsoletyper.
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
package org.teavm.jso.core;

import org.teavm.interop.NoSideEffects;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;

@JSClass(name = "WeakMap")
public class JSWeakMap<K, V> implements JSObject {
    public JSWeakMap() {
    }

    public native V get(K key);

    public native boolean has(K key);

    public native JSWeakMap<K, V> set(K key, V value);

    public native boolean remove(K key);

    @JSBody(script = "return new WeakMap();")
    @NoSideEffects
    @Deprecated
    public static native <K, V> JSWeakMap<K, V> create();

    @JSBody(script = "return typeof WeakMap !== 'undefined';")
    @NoSideEffects
    public static native boolean isSupported();
}
