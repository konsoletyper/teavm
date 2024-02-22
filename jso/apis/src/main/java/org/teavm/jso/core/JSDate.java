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
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;

@JSClass(name = "Date")
public class JSDate implements JSObject {
    public JSDate() {
    }

    public JSDate(double millis) {
    }

    public JSDate(int year, int month) {
    }

    public JSDate(int year, int month, int day) {
    }

    public JSDate(int year, int month, int day, int hour) {
    }

    public JSDate(int year, int month, int day, int hour, int minute) {
    }

    public JSDate(int year, int month, int day, int hour, int minute, int second) {
    }

    public JSDate(int year, int month, int day, int hour, int minute, int second, int millisecond) {
    }

    @JSBody(script = "return new Date();")
    @NoSideEffects
    @Deprecated
    public static native JSDate create();

    @JSBody(params = "millis", script = "return new Date(millis);")
    @NoSideEffects
    @Deprecated
    public static native JSDate create(double millis);

    @JSBody(params = { "year", "month" }, script = "return new Date(year, month);")
    @NoSideEffects
    @Deprecated
    public static native JSDate create(int year, int month);

    @JSBody(params = { "year", "month", "day" }, script = "return new Date(year, month, day);")
    @NoSideEffects
    @Deprecated
    public static native JSDate create(int year, int month, int day);

    @JSBody(params = { "year", "month", "day", "hour" }, script = "return new Date(year, month, day, hour);")
    @NoSideEffects
    @Deprecated
    public static native JSDate create(int year, int month, int day, int hour);

    @JSBody(params = { "year", "month", "day", "hour", "minute" },
            script = "return new Date(year, month, day, hour, minute);")
    @NoSideEffects
    @Deprecated
    public static native JSDate create(int year, int month, int day, int hour, int minute);

    @JSBody(params = { "year", "month", "day", "hour", "minute", "second" },
            script = "return new Date(year, month, day, hour, minute, second);")
    @NoSideEffects
    @Deprecated
    public static native JSDate create(int year, int month, int day, int hour, int minute, int second);

    @JSBody(params = { "year", "month", "day", "hour", "minute", "second", "millisecond" },
            script = "return new Date(year, month, day, hour, minute, second, millisecond);")
    @NoSideEffects
    @Deprecated
    public static native JSDate create(int year, int month, int day, int hour, int minute, int second, int millisecond);

    @NoSideEffects
    public static native double now();

    @NoSideEffects
    public static native double parse(String stringValue);

    @NoSideEffects
    public static native double UTC(int year, int month);

    @NoSideEffects
    public static native double UTC(int year, int month, int day);

    @NoSideEffects
    public static native double UTC(int year, int month, int day, int hour);

    @NoSideEffects
    public static native double UTC(int year, int month, int day, int hour, int minute);

    @NoSideEffects
    public static native double UTC(int year, int month, int day, int hour, int minute, int second);

    @NoSideEffects
    public static native double UTC(int year, int month, int day, int hour, int minute, int second, int millisecond);

    public native int getDate();

    public native int getDay();

    public native int getFullYear();

    public native int getHours();

    public native int getMilliseconds();

    public native int getMinutes();

    public native int getMonth();

    public native int getSeconds();

    public native double getTime();

    public native int getTimezoneOffset();

    public native int getUTCDate();

    public native int getUTCDay();

    public native int getUTCFullYear();

    public native int getUTCHours();

    public native int getUTCMilliseconds();

    public native int getUTCMinutes();

    public native int getUTCMonth();

    public native int getUTCSeconds();

    public native void setDate(int date);

    public native void setFullYear(int fullYear);

    public native void setHours(int hours);

    public native void setMilliseconds(int milliseconds);

    public native void setMinutes(int minutes);

    public native void setMonth(int month);

    public native void setSeconds(int seconds);

    public native void setTime(double time);

    public native void setUTCDate(int date);

    public native void setUTCFullYear(int fullYear);

    public native void setUTCHours(int hours);

    public native void setUTCMilliseconds(int milliseconds);

    public native void setUTCMinutes(int minutes);

    public native void setUTCMonth(int month);

    public native void setUTCSeconds(int seconds);

    public native String toDateString();

    public native String toISOString();

    public native String toJSON();

    public native String toLocaleDateString();

    public native String toLocaleString();

    public native String toLocaleTimeString();

    @JSMethod("toString")
    public native String stringValue();

    public native String toTimeString();

    public native String toUTCString();

    public native String toLocaleFormat(String format);
}
