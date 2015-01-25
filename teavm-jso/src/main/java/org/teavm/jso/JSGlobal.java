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
 */
public interface JSGlobal extends JSObject {
    @JSConstructor
    JSObject newObject();

    @JSConstructor
    <T extends JSObject> JSArray<T> newArray();

    @JSConstructor
    <T extends JSObject> JSArray<T> newArray(int sz);

    @JSConstructor("Array")
    JSStringArray newStringArray();

    @JSConstructor("Array")
    JSStringArray newStringArray(int sz);

    @JSConstructor("Array")
    JSBooleanArray newBooleanArray();

    @JSConstructor("Array")
    JSBooleanArray newBooleanArray(int sz);

    @JSConstructor("Array")
    JSIntArray newIntArray();

    @JSConstructor("Array")
    JSIntArray newIntArray(int sz);

    @JSConstructor("Array")
    JSDoubleArray newDoubleArray();

    @JSConstructor("Array")
    JSDoubleArray newDoubleArray(int sz);
}
