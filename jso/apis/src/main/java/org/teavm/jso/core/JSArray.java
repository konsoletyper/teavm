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
package org.teavm.jso.core;

import org.teavm.interop.NoSideEffects;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public abstract class JSArray<T extends JSObject> implements JSArrayReader<T> {
    private JSArray() {
    }

    @JSIndexer
    public abstract void set(int index, T value);

    public abstract int push(T a);

    public abstract int push(T a, T b);

    public abstract int push(T a, T b, T c);

    public abstract int push(T a, T b, T c, T d);

    public abstract T shift();

    public abstract String join(String separator);

    public abstract String join();

    public abstract JSArray<T> concat(JSArrayReader<T> a);

    public abstract JSArray<T> concat(JSArrayReader<T> a, JSArrayReader<T> b);

    public abstract JSArray<T> concat(JSArrayReader<T> a, JSArrayReader<T> b, JSArrayReader<T> c);

    public abstract JSArray<T> concat(JSArrayReader<T> a, JSArrayReader<T> b, JSArrayReader<T> c, JSArrayReader<T> d);

    public abstract T pop();

    public abstract int unshift(T a);

    public abstract int unshift(T a, T b);

    public abstract int unshift(T a, T b, T c);

    public abstract int unshift(T a, T b, T c, T d);

    public abstract JSArray<T> slice(int start);

    public abstract JSArray<T> slice(int start, int end);

    public abstract JSArray<T> reverse();

    public abstract JSArray<T> sort(JSSortFunction<T> function);

    public abstract JSArray<T> sort();

    public abstract JSArray<T> splice(int start, int count);

    public abstract JSArray<T> splice(int start, int count, T a);

    public abstract JSArray<T> splice(int start, int count, T a, T b);

    public abstract JSArray<T> splice(int start, int count, T a, T b, T c);

    public abstract JSArray<T> splice(int start, int count, T a, T b, T c, T d);
    
    @JSProperty
    public abstract void setLength(int len);

    @JSBody(script = "return new Array();")
    @NoSideEffects
    public static native <T extends JSObject> JSArray<T> create();

    @JSBody(params = "size", script = "return new Array(size);")
    @NoSideEffects
    public static native <T extends JSObject> JSArray<T> create(int size);

    @SafeVarargs
    public static <S extends JSObject> JSArray<S> of(S... items) {
        JSArray<S> array = create(items.length);
        for (int i = 0; i < items.length; ++i) {
            array.set(i, items[i]);
        }
        return array;
    }
}
