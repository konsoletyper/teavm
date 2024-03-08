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
import org.teavm.jso.JSClass;
import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSProperty;

@JSClass(name = "Array")
public class JSArray<T> implements JSArrayReader<T> {
    @NoSideEffects
    public JSArray(int size) {
    }

    @NoSideEffects
    public JSArray() {
    }

    @JSIndexer
    public native void set(int index, T value);

    public native int push(T a);

    public native int push(T a, T b);

    public native int push(T a, T b, T c);

    public native int push(T a, T b, T c, T d);

    public native T shift();

    public native String join(String separator);

    public native String join();

    public native JSArray<T> concat(JSArrayReader<T> a);

    public native JSArray<T> concat(JSArrayReader<T> a, JSArrayReader<T> b);

    public native JSArray<T> concat(JSArrayReader<T> a, JSArrayReader<T> b, JSArrayReader<T> c);

    public native JSArray<T> concat(JSArrayReader<T> a, JSArrayReader<T> b, JSArrayReader<T> c, JSArrayReader<T> d);

    public native T pop();

    public native int unshift(T a);

    public native int unshift(T a, T b);

    public native int unshift(T a, T b, T c);

    public native int unshift(T a, T b, T c, T d);

    public native JSArray<T> slice(int start);

    public native JSArray<T> slice(int start, int end);

    public native JSArray<T> reverse();

    public native JSArray<T> sort(JSSortFunction<T> function);

    public native JSArray<T> sort();

    public native JSArray<T> splice(int start, int count);

    public native JSArray<T> splice(int start, int count, T a);

    public native JSArray<T> splice(int start, int count, T a, T b);

    public native JSArray<T> splice(int start, int count, T a, T b, T c);

    public native JSArray<T> splice(int start, int count, T a, T b, T c, T d);
    
    @JSProperty
    public native void setLength(int len);

    @Override
    public native int getLength();

    @Override
    public native T get(int index);

    @JSBody(script = "return new Array();")
    @NoSideEffects
    @Deprecated
    public static native <T> JSArray<T> create();

    @JSBody(params = "size", script = "return new Array(size);")
    @NoSideEffects
    @Deprecated
    public static native <T> JSArray<T> create(int size);

    @NoSideEffects
    public static native boolean isArray(Object object);

    @SafeVarargs
    public static <S> JSArray<S> of(S... items) {
        JSArray<S> array = create(items.length);
        for (int i = 0; i < items.length; ++i) {
            array.set(i, items[i]);
        }
        return array;
    }
}
