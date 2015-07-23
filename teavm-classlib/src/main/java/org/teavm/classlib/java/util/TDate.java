/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.java.util;

import java.util.TimeZone;
import org.teavm.classlib.java.lang.TComparable;
import org.teavm.classlib.java.lang.TSystem;
import org.teavm.dependency.PluggableDependency;
import org.teavm.javascript.spi.GeneratedBy;

/**
 *
 * @author Alexey Andreev
 */
public class TDate implements TComparable<TDate> {
    private long value;

    @GeneratedBy(DateNativeGenerator.class)
    private static native void initNativeDate();

    public TDate() {
        value = TSystem.currentTimeMillis();
    }

    public TDate(long date) {
        this.value = date;
    }

    @Deprecated
    public TDate(int year, int month, int date) {
        this(year, month, date, 0, 0);
    }

    @Deprecated
    public TDate(int year, int month, int date, int hrs, int min) {
        this(year, month, date, hrs, min, 0);
    }

    @Deprecated
    public TDate(int year, int month, int date, int hrs, int min, int sec) {
        this((long) buildNumericTime(year, month, date, hrs, min, sec));
        setFullYear(value, year + 1900);
    }

    public TDate(String s) {
        this(parse(s));
    }

    @Override
    public Object clone() {
        return new TDate(value);
    }

    @Deprecated
    public static long UTC(int year, int month, int date, int hrs, int min, int sec) {
        return (long) buildNumericUTC(year, month, date, hrs, min, sec);
    }

    @Deprecated
    public static long parse(String s) {
        double value = parseNumericTime(s);
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("Can't parse date: " + s);
        }
        return (long) value;
    }

    @Deprecated
    public int getYear() {
        return getFullYear(value) - 1900;
    }

    @Deprecated
    public void setYear(int year) {
        this.value = (long) setFullYear(value, year + 1900);
    }

    @Deprecated
    public int getMonth() {
        return getMonth(value);
    }

    @Deprecated
    public void setMonth(int month) {
        this.value = (long) setMonth(value, month);
    }

    @Deprecated
    public int getDate() {
        return getDate(value);
    }

    @Deprecated
    public void setDate(int date) {
        this.value = (long) setDate(value, date);
    }

    @Deprecated
    public int getDay() {
        return getDay(value);
    }

    @Deprecated
    public int getHours() {
        return getHours(value);
    }

    @Deprecated
    public void setHours(int hours) {
        this.value = (long) setHours(value, hours);
    }

    @Deprecated
    public int getMinutes() {
        return getMinutes(value);
    }

    @Deprecated
    public void setMinutes(int minutes) {
        this.value = (long) setMinutes(value, minutes);
    }

    @Deprecated
    public int getSeconds() {
        return getSeconds(value);
    }

    @Deprecated
    public void setSeconds(int seconds) {
        this.value = (long) setSeconds(value, seconds);
    }

    public long getTime() {
        return value;
    }

    public void setTime(long time) {
        value = time;
    }

    public boolean before(TDate when) {
        return value < when.value;
    }

    public boolean after(TDate when) {
        return value > when.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TDate)) {
            return false;
        }
        TDate other = (TDate) obj;
        return value == other.value;
    }

    @Override
    public int compareTo(TDate other) {
        return Long.compare(value, other.value);
    }

    @Override
    public int hashCode() {
        return (int) value ^ (int) (value >>> 32);
    }

    @Override
    public String toString() {
        return toString(value);
    }

    @Deprecated
    public String toLocaleString() {
        return toLocaleFormat(value, "%c");
    }

    @Deprecated
    public String toGMTString() {
        return toGMTString(value);
    }

    @Deprecated
    public int getTimezoneOffset() {
        //return getTimezoneOffset(value);
        return -TimeZone.getDefault().getOffset(value) / (1000 * 60);
    }

    @GeneratedBy(DateNativeGenerator.class)
    private static native int getFullYear(double date);

    @GeneratedBy(DateNativeGenerator.class)
    private static native double setFullYear(double date, int year);

    @GeneratedBy(DateNativeGenerator.class)
    private static native int getMonth(double date);

    @GeneratedBy(DateNativeGenerator.class)
    private static native double setMonth(double date, int month);

    @GeneratedBy(DateNativeGenerator.class)
    private static native int getDate(double date);

    @GeneratedBy(DateNativeGenerator.class)
    private static native double setDate(double dateVal, int date);

    @GeneratedBy(DateNativeGenerator.class)
    private static native int getDay(double date);

    @GeneratedBy(DateNativeGenerator.class)
    private static native int getHours(double date);

    @GeneratedBy(DateNativeGenerator.class)
    private static native double setHours(double date, int hours);

    @GeneratedBy(DateNativeGenerator.class)
    private static native int getMinutes(double date);

    @GeneratedBy(DateNativeGenerator.class)
    private static native double setMinutes(double date, int minutes);

    @GeneratedBy(DateNativeGenerator.class)
    private static native int getSeconds(double date);

    @GeneratedBy(DateNativeGenerator.class)
    private static native double setSeconds(double date, int seconds);

    @GeneratedBy(DateNativeGenerator.class)
    private static native double buildNumericTime(int year, int month, int date, int hrs, int min, int sec);

    @GeneratedBy(DateNativeGenerator.class)
    private static native double parseNumericTime(String dateString);

    @GeneratedBy(DateNativeGenerator.class)
    private static native double buildNumericUTC(int year, int month, int date, int hrs, int min, int sec);

    @GeneratedBy(DateNativeGenerator.class)
    @PluggableDependency(DateNativeGenerator.class)
    private static native String toString(double value);

    @GeneratedBy(DateNativeGenerator.class)
    @PluggableDependency(DateNativeGenerator.class)
    private static native String toLocaleFormat(double value, String format);

    @GeneratedBy(DateNativeGenerator.class)
    @PluggableDependency(DateNativeGenerator.class)
    private static native String toGMTString(double value);

    @GeneratedBy(DateNativeGenerator.class)
    @PluggableDependency(DateNativeGenerator.class)
    static native int getTimezoneOffset(double value);
}
