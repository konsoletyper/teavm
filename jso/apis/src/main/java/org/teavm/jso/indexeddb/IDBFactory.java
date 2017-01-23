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

public abstract class IDBFactory implements JSObject {
    public static boolean isSupported() {
        return !getInstanceImpl().isUndefined();
    }

    @JSBody(script = "return typeof this === 'undefined';")
    private native boolean isUndefined();

    public static IDBFactory getInstance() {
        IDBFactory factory = getInstanceImpl();
        if (!factory.isUndefined()) {
            throw new IllegalStateException("IndexedDB is not supported in this browser");
        }
        return factory;
    }

    @JSBody(script = "return window.indexedDB || window.mozIndexedDB || window.webkitIndexedDB || "
            + "window.msIndexedDB;")
    static native IDBFactory getInstanceImpl();

    public abstract IDBOpenDBRequest open(String name, int version);

    public abstract IDBOpenDBRequest deleteDatabase(String name);

    public abstract int cmp(JSObject a, JSObject b);
}
