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
import org.teavm.jso.core.JSDate;

public class TDate implements TComparable<TDate> {
    private long value;

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
        this((long) JSDate.create(year, month, date, hrs, min, sec).getTime());
        setYear(year);
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
        return (long) JSDate.UTC(year, month, date, hrs, min, sec);
    }

    @Deprecated
    public static long parse(String s) {
        double value = JSDate.parse(s);
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("Can't parse date: " + s);
        }
        return (long) value;
    }

    @Deprecated
    public int getYear() {
        return JSDate.create(value).getFullYear() - 1900;
    }

    @Deprecated
    public void setYear(int year) {
        JSDate date = JSDate.create(value);
        date.setFullYear(year + 1900);
        this.value = (long) date.getTime();
    }

    @Deprecated
    public int getMonth() {
        return JSDate.create(value).getMonth();
    }

    @Deprecated
    public void setMonth(int month) {
        JSDate date = JSDate.create(value);
        date.setMonth(month);
        this.value = (long) date.getTime();
    }

    @Deprecated
    public int getDate() {
        return JSDate.create(value).getDate();
    }

    @Deprecated
    public void setDate(int date) {
        JSDate d = JSDate.create(value);
        d.setDate(date);
        this.value = (long) d.getTime();
    }

    @Deprecated
    public int getDay() {
        return JSDate.create(value).getDay();
    }

    @Deprecated
    public int getHours() {
        return JSDate.create(value).getHours();
    }

    @Deprecated
    public void setHours(int hours) {
        JSDate date = JSDate.create(value);
        date.setHours(hours);
        this.value = (long) date.getTime();
    }

    @Deprecated
    public int getMinutes() {
        return JSDate.create(value).getMinutes();
    }

    @Deprecated
    public void setMinutes(int minutes) {
        JSDate date = JSDate.create(value);
        date.setMinutes(minutes);
        this.value = (long) date.getTime();
    }

    @Deprecated
    public int getSeconds() {
        return JSDate.create(value).getSeconds();
    }

    @Deprecated
    public void setSeconds(int seconds) {
        JSDate date = JSDate.create(value);
        date.setSeconds(seconds);
        this.value = (long) date.getTime();
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
        return JSDate.create(value).stringValue();
    }

    @Deprecated
    public String toLocaleString() {
        return JSDate.create(value).toLocaleFormat("%c");
    }

    @Deprecated
    public String toGMTString() {
        return JSDate.create(value).toUTCString();
    }

    @Deprecated
    public int getTimezoneOffset() {
        return -TimeZone.getDefault().getOffset(value) / (1000 * 60);
    }
}
