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
package org.teavm.jso;

/**
 *
 * @author Alexey Andreev
 * @param <T>
 */
public interface JSArray<T extends JSObject> extends JSArrayReader<T> {
    @JSIndexer
    void set(int index, T value);

    int push(T a);

    int push(T a, T b);

    int push(T a, T b, T c);

    int push(T a, T b, T c, T d);

    T shift();

    String join(String separator);

    String join();

    JSArray<T> concat(JSArrayReader<T> a);

    JSArray<T> concat(JSArrayReader<T> a, JSArrayReader<T> b);

    JSArray<T> concat(JSArrayReader<T> a, JSArrayReader<T> b, JSArrayReader<T> c);

    JSArray<T> concat(JSArrayReader<T> a, JSArrayReader<T> b, JSArrayReader<T> c, JSArrayReader<T> d);

    T pop();

    int unshift(T a);

    int unshift(T a, T b);

    int unshift(T a, T b, T c);

    int unshift(T a, T b, T c, T d);

    JSArray<T> slice(int start);

    JSArray<T> slice(int start, int end);

    JSArray<T> reverse();

    JSArray<T> sort(JSSortFunction<T> function);

    JSArray<T> sort();

    JSArray<T> splice(int start, int count);

    JSArray<T> splice(int start, int count, T a);

    JSArray<T> splice(int start, int count, T a, T b);

    JSArray<T> splice(int start, int count, T a, T b, T c);

    JSArray<T> splice(int start, int count, T a, T b, T c, T d);
}
