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
package org.teavm.jso;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface JSIntArray extends JSIntArrayReader {
    @JSIndexer
    void set(int index, int value);

    int push(int a);

    int push(int a, int b);

    int push(int a, int b, int c);

    int push(int a, int b, int c, int d);

    int shift();

    int join(int separator);

    int join();

    JSIntArray concat(JSIntArrayReader a);

    JSIntArray concat(JSIntArrayReader a, JSIntArrayReader b);

    JSIntArray concat(JSIntArrayReader a, JSIntArrayReader b, JSIntArrayReader c);

    JSIntArray concat(JSIntArrayReader a, JSIntArrayReader b, JSIntArrayReader c, JSIntArrayReader d);

    int pop();

    int unshift(int a);

    int unshift(int a, int b);

    int unshift(int a, int b, int c);

    int unshift(int a, int b, int c, int d);

    JSIntArray slice(int start);

    JSIntArray slice(int start, int end);

    JSIntArray reverse();

    JSIntArray sort(JSIntSortFunction function);

    JSIntArray sort();

    JSIntArray splice(int start, int count);

    JSIntArray splice(int start, int count, int a);

    JSIntArray splice(int start, int count, int a, int b);

    JSIntArray splice(int start, int count, int a, int b, int c);

    JSIntArray splice(int start, int count, int a, int b, int c, int d);
}
