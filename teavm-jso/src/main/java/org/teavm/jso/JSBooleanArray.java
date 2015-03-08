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
 * @author Alexey Andreev
 */
public interface JSBooleanArray extends JSBooleanArrayReader {
    @JSIndexer
    void set(int index, boolean value);

    int push(boolean a);

    int push(boolean a, boolean b);

    int push(boolean a, boolean b, boolean c);

    int push(boolean a, boolean b, boolean c, boolean d);

    boolean shift();

    String join(String separator);

    String join();

    JSBooleanArray concat(JSBooleanArrayReader a);

    JSBooleanArray concat(JSBooleanArray a, JSBooleanArray b);

    JSBooleanArray concat(JSBooleanArray a, JSBooleanArray b, JSBooleanArray c);

    JSBooleanArray concat(JSBooleanArray a, JSBooleanArray b, JSBooleanArray c, JSBooleanArray d);

    boolean pop();

    int unshift(boolean a);

    int unshift(boolean a, boolean b);

    int unshift(boolean a, boolean b, boolean c);

    int unshift(boolean a, boolean b, boolean c, boolean d);

    JSBooleanArray slice(int start);

    JSBooleanArray slice(int start, int end);

    JSBooleanArray reverse();

    JSBooleanArray splice(int start, int count);

    JSBooleanArray splice(int start, int count, boolean a);

    JSBooleanArray splice(int start, int count, boolean a, boolean b);

    JSBooleanArray splice(int start, int count, boolean a, boolean b, boolean c);

    JSBooleanArray splice(int start, int count, boolean a, boolean b, boolean c, boolean d);
}
