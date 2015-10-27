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
public abstract class JSDictonary<K extends JSObject, T extends JSObject> implements JSObject {

    @JSIndexer
    public abstract T get(K key);

    @JSIndexer
    public abstract T get(String key);

    @JSIndexer
    public abstract void set(K key, T value);

    @JSIndexer
    public abstract void set(String key, T value);
    
    @JSBody(params = { "key" }, script = "delete this[key]; return this;")
    public final native JSDictonary<K, T> del(K key);

    @JSBody(params = {}, script = "return Object.keys(this);")
    public final native K[] keys();

    @JSBody(params = {}, script = "return {};")
    public static native <K extends JSObject, T extends JSObject> JSDictonary<K, T> create();

    @JSBody(params = { "obj" }, script = "return obj;")
    public static native <K extends JSObject, T extends JSObject> JSDictonary<K, T> create(JSObject obj);
}
