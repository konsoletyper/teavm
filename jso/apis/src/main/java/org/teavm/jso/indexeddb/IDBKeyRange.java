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
package org.teavm.jso.indexeddb;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public abstract class IDBKeyRange implements JSObject {
    @JSProperty
    public abstract JSObject getLower();

    @JSProperty
    public abstract JSObject getUpper();

    @JSProperty
    public abstract boolean isLowerOpen();

    @JSProperty
    public abstract boolean isUpperOpen();

    @JSBody(params = "value", script = "return IDBKeyRange.only(value);")
    public static native IDBKeyRange only(JSObject value);

    @JSBody(params = { "lower", "open" }, script = "return IDBKeyRange.lowerBound(lower, open);")
    public static native IDBKeyRange lowerBound(JSObject lower, boolean open);

    public static IDBKeyRange lowerBound(JSObject lower) {
        return lowerBound(lower, false);
    }

    @JSBody(params = { "upper", "open" }, script = "return IDBKeyRange.upperBound(upper, open);")
    public static native IDBKeyRange upperBound(JSObject upper, boolean open);

    public static IDBKeyRange upperBound(JSObject upper) {
        return upperBound(upper, false);
    }

    @JSBody(params = { "lower", "upper", "lowerOpen", "upperOpen" },
            script = "return IDBKeyRange.bound(lower, upper, lowerOpen, upperOpen);")
    public static native IDBKeyRange bound(JSObject lower, JSObject upper, boolean lowerOpen, boolean upperOpen);

    public static IDBKeyRange bound(JSObject lower, JSObject upper) {
        return bound(lower, upper, false, false);
    }
}
