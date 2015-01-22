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
public interface JSFloatArrayReader extends JSObject {
    @JSProperty
    int getLength();

    @JSIndexer
    float get(int index);

    @JSIndexer
    void set(int index, float value);

    int push(float a);

    int push(float a, float b);

    int push(float a, float b, float c);

    int push(float a, float b, float c, float d);

    float shift();

    String join(String separator);

    String join();

    JSFloatArray concat(JSFloatArrayReader a);

    JSFloatArray concat(JSFloatArray a, JSFloatArray b);

    JSFloatArray concat(JSFloatArray a, JSFloatArray b, JSFloatArray c);

    JSFloatArray concat(JSFloatArray a, JSFloatArray b, JSFloatArray c, JSFloatArray d);

    float pop();

    int unshift(float a);

    int unshift(float a, float b);

    int unshift(float a, float b, float c);

    int unshift(float a, float b, float c, float d);

    JSFloatArray slice(int start);

    JSFloatArray slice(int start, int end);

    JSFloatArray reverse();

    JSFloatArray sort(JSFloatSortFunction function);

    JSFloatArray sort();

    JSFloatArray splice(int start, int count);

    JSFloatArray splice(int start, int count, float a);

    JSFloatArray splice(int start, int count, float a, float b);

    JSFloatArray splice(int start, int count, float a, float b, float c);

    JSFloatArray splice(int start, int count, float a, float b, float c, float d);
}
