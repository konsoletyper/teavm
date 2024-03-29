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

import org.teavm.interop.NoSideEffects;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

@JSClass(name = "RegExp")
public class JSRegExp implements JSObject {
    public JSRegExp(String pattern) {
    }

    public JSRegExp(String pattern, String flags) {
    }

    @JSBody(params = "pattern", script = "return new RegExp(pattern);")
    @NoSideEffects
    @Deprecated
    public static native JSRegExp create(String pattern);

    @JSBody(params = { "pattern", "flags" }, script = "return new RegExp(pattern, flags);")
    @NoSideEffects
    @Deprecated
    public static native JSRegExp create(String pattern, String flags);

    public static JSRegExp create(String pattern, JSRegExpFlag... flags) {
        boolean global = false;
        boolean ignoreCase = false;
        boolean multiline = false;
        for (JSRegExpFlag flag : flags) {
            switch (flag) {
                case GLOBAL:
                    global = true;
                    break;
                case IGNORE_CASE:
                    ignoreCase = true;
                    break;
                case MULTILINE:
                    multiline = true;
                    break;
            }
        }
        StringBuilder sb = new StringBuilder();
        if (global) {
            sb.append('g');
        }
        if (ignoreCase) {
            sb.append('i');
        }
        if (multiline) {
            sb.append('m');
        }
        return create(pattern, sb.toString());
    }

    @JSProperty
    public native boolean isGlobal();

    @JSProperty
    public native boolean isIgnoreCase();

    @JSProperty
    public native boolean isMultiline();

    @JSProperty
    public native int getLastIndex();

    @JSProperty
    public native JSString getSource();

    public native JSArray<JSString> exec(JSString text);

    public native boolean test(JSString text);

    public native boolean test(String text);
}
