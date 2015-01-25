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
public interface JSDoubleArray extends JSDoubleArrayReader {
    @JSIndexer
    void set(int index, double value);

    int push(double a);

    int push(double a, double b);

    int push(double a, double b, double c);

    int push(double a, double b, double c, double d);

    double shift();

    String join(String separator);

    String join();

    JSDoubleArray concat(JSDoubleArrayReader a);

    JSDoubleArray concat(JSDoubleArray a, JSDoubleArray b);

    JSDoubleArray concat(JSDoubleArray a, JSDoubleArray b, JSDoubleArray c);

    JSDoubleArray concat(JSDoubleArray a, JSDoubleArray b, JSDoubleArray c, JSDoubleArray d);

    double pop();

    int unshift(double a);

    int unshift(double a, double b);

    int unshift(double a, double b, double c);

    int unshift(double a, double b, double c, double d);

    JSDoubleArray slice(int start);

    JSDoubleArray slice(int start, int end);

    JSDoubleArray reverse();

    JSDoubleArray sort(JSDoubleSortFunction function);

    JSDoubleArray sort();

    JSDoubleArray splice(int start, int count);

    JSDoubleArray splice(int start, int count, double a);

    JSDoubleArray splice(int start, int count, double a, double b);

    JSDoubleArray splice(int start, int count, double a, double b, double c);

    JSDoubleArray splice(int start, int count, double a, double b, double c, double d);
}
