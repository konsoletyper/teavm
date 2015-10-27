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
package org.teavm.jso.core;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSObject;

/**
*
* @author Jan-Felix Wittmann
*/
public abstract class JSDictonary implements JSObject {

    @JSIndexer
    public abstract JSObject get(String key);

    @JSIndexer
    public abstract JSObject get(int key);

    @JSIndexer
    public abstract JSObject get(float key);

    @JSIndexer
    public abstract <V extends JSObject> void set(String key, V value);

    @JSIndexer
    public abstract <V extends JSObject> void set(int key, V value);

    @JSIndexer
    public abstract <V extends JSObject> void set(float key, V value);

    public <V extends JSObject> JSDictonary with(String key, V value) {
        this.set(key, value);
        return this;
    }

    public JSDictonary with(String key, String value) {
        return this.with(key, JSString.valueOf(value));
    }

    public JSDictonary with(String key, JSDictonary value) {
        return this.with(key, (JSObject) value);
    }

    public JSDictonary with(String key, int value) {
        return this.with(key, JSNumber.valueOf(value));
    }

    public JSDictonary with(String key, float value) {
        return this.with(key, JSNumber.valueOf(value));
    }

    public JSDictonary with(String key, double value) {
        return this.with(key, JSNumber.valueOf(value));
    }

    public JSDictonary with(String key, boolean value) {
        return this.with(key, JSBoolean.valueOf(value));
    }

    @JSBody(params = { "key" }, script = "delete this[key]; return this;")
    public final native JSDictonary del(String key);

    @JSBody(params = { "key" }, script = "delete this[key]; return this;")
    public final native JSDictonary del(int key);

    @JSBody(params = { "key" }, script = "delete this[key]; return this;")
    public final native JSDictonary del(float key);

    @JSBody(params = {}, script = "return Object.keys(this);")
    public final native String[] keys();

    @JSBody(params = {}, script = "return {};")
    public static native JSDictonary create();

    @JSBody(params = { "obj" }, script = "return obj;")
    public static native JSDictonary of(JSObject obj);

}
