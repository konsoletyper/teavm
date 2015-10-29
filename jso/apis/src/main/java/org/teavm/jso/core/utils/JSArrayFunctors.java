/*
 *  Copyright 2015 Jan-Felix Wittmann.
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
package org.teavm.jso.core.utils;

import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSString;

/**
*
* @author Jan-Felix Wittmann
*/
public final class JSArrayFunctors {
    // ForEach Interfaces

    @JSFunctor
    @FunctionalInterface
    public interface JSForeachFunction<T extends JSObject> extends JSObject {

        void apply(T element, int index, JSArray<T> arr);

    }

    // Map Interfaces

    @JSFunctor
    @FunctionalInterface
    public interface JSMapFunction<T extends JSObject, R extends JSObject> extends JSObject {

        R apply(T element, int index, JSArray<T> arr);

    }

    @JSFunctor
    @FunctionalInterface
    public interface JSMapIntFunction extends JSObject {

        int apply(int element, int index, JSArray<JSNumber> arr);

    }

    @JSFunctor
    @FunctionalInterface
    public interface JSMapDoubleFunction extends JSObject {

        double apply(double element, int index, JSArray<JSNumber> arr);

    }

    @JSFunctor
    @FunctionalInterface
    public interface JSMapStringFunction extends JSObject {

        String apply(String element, int index, JSArray<JSString> arr);

    }

    // Filter Interfaces

    @JSFunctor
    @FunctionalInterface
    public interface JSFilterFunction<T extends JSObject> extends JSObject {

        boolean apply(T element, int index, JSArray<T> arr);

    }

    // Reduce Interfaces

    @JSFunctor
    @FunctionalInterface
    public interface JSReduceFunction<T extends JSObject, R extends JSObject> extends JSObject {

        R apply(R previousValue, T element, int index, JSArray<T> arr);

    }
}
