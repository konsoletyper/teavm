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

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/**
*
* @author Jan-Felix Wittmann
*/
public final class JSObjectUtils {
    private JSObjectUtils() {
    }

    @JSBody(params = {}, script = "return {}")
    public static native <T extends JSObject> T create();

    @JSBody(params = { "obj", "prop" }, script = "return obj.hasOwnProperty(prop)")
    public static native boolean hasOwnProperty(JSObject obj, String prop);

    @JSBody(params = { "obj" }, script = "return Object.keys(obj);")
    public static native String[] keys(JSObject obj);

    @JSBody(params = { "obj", "prop" }, script = "delete obj[prop]")
    public static native void delete(JSObject obj, String prop);

    @JSBody(params = { "obj", "callback" }, script = "for (var prop in obj) {callback.apply(obj, [prop, obj]);}")
    public static native void forIn(JSObject obj, JSForInCallback<?> callback);

    @JSBody(params = { "obj" }, script = "return Object.prototype.toString.call(obj) === '[object Array]';")
    public static native boolean isJSArray(JSObject obj);

    @JSBody(params = { "obj" }, script = "return Object.prototype.toString.call(obj) === '[object Number]';")
    public static native boolean isJSNumber(JSObject obj);

    @JSBody(params = { "obj" }, script = "return Object.prototype.toString.call(obj) === '[object Date]';")
    public static native boolean isJSDate(JSObject obj);

    @JSBody(params = { "obj" }, script = "return Object.prototype.toString.call(obj) === '[object String]';")
    public static native boolean isJSString(JSObject obj);

    @JSBody(params = { "obj" }, script = "return Object.prototype.toString.call(obj) === '[object RegExp]';")
    public static native boolean isJSRegExp(JSObject obj);

    @JSBody(params = { "obj" }, script = "return  obj === true || obj === false "
            + "|| Object.prototype.toString.call(obj) === '[object Boolean]';")
    public static native boolean isJSBoolean(JSObject obj);

}
