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

    private JSDictonary() {
    }

    @JSIndexer
    public abstract JSObject get(String key);

    @JSIndexer
    public abstract <V extends JSObject> void put(String key, V value);

    public <V extends JSObject> JSDictonary with(String key, V value) {
        this.put(key, value);
        return this;
    }

    public JSDictonary with(String key, String value) {
        return this.with(key, JSString.valueOf(value));
    }

    public JSDictonary with(String key, JSDictonary value) {
        return this.with(key, (JSObject) value);
    }

    public JSDictonary with(String key, Integer value) {
        return this.with(key, JSNumber.valueOf(value));
    }

    public JSDictonary with(String key, Float value) {
        return this.with(key, JSNumber.valueOf(value));
    }

    public JSDictonary with(String key, Double value) {
        return this.with(key, JSNumber.valueOf(value));
    }

    public JSDictonary with(String key, Boolean value) {
        return this.with(key, JSBoolean.valueOf(value));
    }

    @JSBody(params = { "key" }, script = "delete this[key]; return this;")
    public final native JSDictonary del(String key);

    @JSBody(params = {}, script = "return Object.keys(this);")
    public final native String[] keys();

    @JSBody(params = { "key" }, script = "return this.hasOwnProperty(key)")
    public final native boolean has(String key);

    @JSBody(params = {}, script = "return {};")
    public static native JSDictonary create();

    @JSBody(params = { "obj" }, script = "return obj;")
    public static native JSDictonary of(JSObject obj);

}
