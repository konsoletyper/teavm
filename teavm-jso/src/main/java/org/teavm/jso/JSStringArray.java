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
public interface JSStringArray extends JSStringArrayReader {
    @JSIndexer
    void set(int index, String value);

    int push(String a);

    int push(String a, String b);

    int push(String a, String b, String c);

    int push(String a, String b, String c, String d);

    String shift();

    String join(String separator);

    String join();

    JSStringArray concat(JSStringArrayReader a);

    JSStringArray concat(JSStringArrayReader a, JSStringArrayReader b);

    JSStringArray concat(JSStringArrayReader a, JSStringArrayReader b, JSStringArrayReader c);

    JSStringArray concat(JSStringArrayReader a, JSStringArrayReader b, JSStringArrayReader c, JSStringArrayReader d);

    String pop();

    int unshift(String a);

    int unshift(String a, String b);

    int unshift(String a, String b, String c);

    int unshift(String a, String b, String c, String d);

    JSStringArray slice(int start);

    JSStringArray slice(int start, int end);

    JSStringArray reverse();

    JSStringArray sort(JSStringSortFunction function);

    JSStringArray sort();

    JSStringArray splice(int start, int count);

    JSStringArray splice(int start, int count, String a);

    JSStringArray splice(int start, int count, String a, String b);

    JSStringArray splice(int start, int count, String a, String b, String c);

    JSStringArray splice(int start, int count, String a, String b, String c, String d);
}
