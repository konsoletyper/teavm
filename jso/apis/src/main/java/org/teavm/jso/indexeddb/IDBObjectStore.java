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
import org.teavm.jso.core.JSString;

public abstract class IDBObjectStore implements JSObject, IDBCursorSource {
    @JSProperty
    public abstract String getName();

    @JSProperty("keyPath")
    abstract JSObject getKeyPathImpl();

    public final String[] getKeyPath() {
        JSObject result = getKeyPathImpl();
        if (JSString.isInstance(result)) {
            return new String[] { result.<JSString>cast().stringValue() };
        } else {
            return unwrapStringArray(result);
        }
    }

    @JSBody(script = "return this;")
    private native String[] unwrapStringArray(JSObject obj);

    @JSProperty
    public abstract String[] getIndexNames();

    @JSProperty
    public abstract boolean isAutoIncrement();

    public abstract IDBRequest put(JSObject value, JSObject key);

    public abstract IDBRequest put(JSObject value);

    public abstract IDBRequest add(JSObject value, JSObject key);

    public abstract IDBRequest add(JSObject value);

    public abstract IDBRequest delete(JSObject key);

    public abstract IDBGetRequest get(JSObject key);

    public abstract IDBRequest clear();

    public abstract IDBCursorRequest openCursor();

    public abstract IDBCursorRequest openCursor(IDBKeyRange range);

    public abstract IDBIndex createIndex(String name, String key);

    public abstract IDBIndex createIndex(String name, String[] keys);

    public abstract IDBIndex index(String name);

    public abstract void deleteIndex(String name);

    public abstract IDBCountRequest count();

    public abstract IDBCountRequest count(JSObject key);
}
