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
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;

public abstract class JSDate implements JSObject {
    @JSBody(script = "return new Date();")
    @NoSideEffects
    public static native JSDate create();

    @JSBody(params = "millis", script = "return new Date(millis);")
    @NoSideEffects
    public static native JSDate create(double millis);

    @JSBody(params = { "year", "month" }, script = "return new Date(year, month);")
    @NoSideEffects
    public static native JSDate create(int year, int month);

    @JSBody(params = { "year", "month", "day" }, script = "return new Date(year, month, day);")
    @NoSideEffects
    public static native JSDate create(int year, int month, int day);

    @JSBody(params = { "year", "month", "day", "hour" }, script = "return new Date(year, month, day, hour);")
    @NoSideEffects
    public static native JSDate create(int year, int month, int day, int hour);

    @JSBody(params = { "year", "month", "day", "hour", "minute" },
            script = "return new Date(year, month, day, hour, minute);")
    @NoSideEffects
    public static native JSDate create(int year, int month, int day, int hour, int minute);

    @JSBody(params = { "year", "month", "day", "hour", "minute", "second" },
            script = "return new Date(year, month, day, hour, minute, second);")
    @NoSideEffects
    public static native JSDate create(int year, int month, int day, int hour, int minute, int second);

    @JSBody(params = { "year", "month", "day", "hour", "minute", "second", "millisecond" },
            script = "return new Date(year, month, day, hour, minute, second, millisecond);")
    @NoSideEffects
    public static native JSDate create(int year, int month, int day, int hour, int minute, int second, int millisecond);

    @JSBody(params = {}, script = "return Date.now();")
    @NoSideEffects
    public static native double now();

    @JSBody(params = "stringValue", script = "return Date.parse(stringValue);")
    @NoSideEffects
    public static native double parse(String stringValue);

    @JSBody(params = { "year", "month" }, script = "return Date.UTC(year, month);")
    @NoSideEffects
    public static native double UTC(int year, int month);

    @JSBody(params = { "year", "month", "day" }, script = "return Date.UTC(year, month, day);")
    @NoSideEffects
    public static native double UTC(int year, int month, int day);

    @JSBody(params = { "year", "month", "day", "hour" }, script = "return Date.UTC(year, month, day, hour);")
    @NoSideEffects
    public static native double UTC(int year, int month, int day, int hour);

    @JSBody(params = { "year", "month", "day", "hour", "minute" },
            script = "return Date.UTC(year, month, day, hour, minute);")
    @NoSideEffects
    public static native double UTC(int year, int month, int day, int hour, int minute);

    @JSBody(params = { "year", "month", "day", "hour", "minute", "second" },
            script = "return Date.UTC(year, month, day, hour, minute, second);")
    @NoSideEffects
    public static native double UTC(int year, int month, int day, int hour, int minute, int second);

    @JSBody(params = { "year", "month", "day", "hour", "minute", "second", "millisecond" },
            script = "return Date.UTC(year, month, day, hour, minute, second, millisecond);")
    @NoSideEffects
    public static native double UTC(int year, int month, int day, int hour, int minute, int second, int millisecond);

    public abstract int getDate();

    public abstract int getDay();

    public abstract int getFullYear();

    public abstract int getHours();

    public abstract int getMilliseconds();

    public abstract int getMinutes();

    public abstract int getMonth();

    public abstract int getSeconds();

    public abstract double getTime();

    public abstract int getTimezoneOffset();

    public abstract int getUTCDate();

    public abstract int getUTCDay();

    public abstract int getUTCFullYear();

    public abstract int getUTCHours();

    public abstract int getUTCMilliseconds();

    public abstract int getUTCMinutes();

    public abstract int getUTCMonth();

    public abstract int getUTCSeconds();

    public abstract void setDate(int date);

    public abstract void setFullYear(int fullYear);

    public abstract void setHours(int hours);

    public abstract void setMilliseconds(int milliseconds);

    public abstract void setMinutes(int minutes);

    public abstract void setMonth(int month);

    public abstract void setSeconds(int seconds);

    public abstract void setTime(double time);

    public abstract void setUTCDate(int date);

    public abstract void setUTCFullYear(int fullYear);

    public abstract void setUTCHours(int hours);

    public abstract void setUTCMilliseconds(int milliseconds);

    public abstract void setUTCMinutes(int minutes);

    public abstract void setUTCMonth(int month);

    public abstract void setUTCSeconds(int seconds);

    public abstract String toDateString();

    public abstract String toISOString();

    public abstract String toJSON();

    public abstract String toLocaleDateString();

    public abstract String toLocaleString();

    public abstract String toLocaleTimeString();

    @JSMethod("toString")
    public abstract String stringValue();

    public abstract String toTimeString();

    public abstract String toUTCString();

    public abstract String toLocaleFormat(String format);
}
