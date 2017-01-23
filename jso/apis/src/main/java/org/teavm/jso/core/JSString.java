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
package org.teavm.jso.core;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public abstract class JSString implements JSObject {
    private JSString() {
    }

    public final String stringValue() {
        return stringValue(this);
    }

    @JSBody(params = "str", script = "return str;")
    private static native String stringValue(JSString str);

    @JSBody(params = "str", script = "return str;")
    public static native JSString valueOf(String str);

    @JSBody(params = "code", script = "return String.fromCharCode(code)")
    public static native JSString fromCharCode(int code);

    @JSProperty
    public abstract int getLength();

    public abstract JSString charAt(int index);

    public abstract int charCodeAt(int index);

    public abstract JSString concat(JSString a);

    public abstract JSString concat(JSString a, JSString b);

    public abstract JSString concat(JSString a, JSString b, JSString c);

    public abstract int indexOf(JSString a);

    public abstract int lastIndexOf(JSString a);

    public abstract JSArray<JSString> match(JSRegExp regexp);

    public abstract JSArray<JSString> match(JSString regexp);

    public abstract JSString replace(JSRegExp regexp, JSString replaceBy);

    public abstract JSString replace(JSRegExp regexp, JSReplaceFunction replaceBy);

    public abstract JSString replace(JSString regexp, JSString replaceBy);

    public abstract JSString replace(JSString regexp, JSReplaceFunction replaceBy);

    public abstract int search(JSRegExp regexp);

    public abstract int search(JSString regexp);

    public abstract JSString slice(int beginSlice);

    public abstract JSString slice(int beginSlice, int endSlice);

    public abstract JSString[] split(JSRegExp separator);

    public abstract JSString[] split(JSString separator);

    public abstract JSString[] split(JSRegExp separator, int limit);

    public abstract JSString[] split(JSString separator, int limit);

    public abstract JSString substr(int start);

    public abstract JSString substr(int start, int length);

    public abstract JSString substring(int start);

    public abstract JSString substring(int start, int end);

    public abstract JSString toLowerCase();

    public abstract JSString toUpperCase();

    public abstract JSString trim();

    @JSBody(params = "obj", script = "return typeof obj === 'string';")
    public static native boolean isInstance(JSObject obj);
}
