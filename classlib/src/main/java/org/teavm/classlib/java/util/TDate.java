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
import org.teavm.backend.c.intrinsic.RuntimeInclude;
import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.java.lang.TComparable;
import org.teavm.classlib.java.lang.TSystem;
import org.teavm.interop.Import;
import org.teavm.interop.NoSideEffects;
import org.teavm.interop.Platforms;
import org.teavm.interop.Unmanaged;
import org.teavm.interop.UnsupportedOn;
import org.teavm.jso.core.JSDate;

public class TDate implements TComparable<TDate> {
    private long value;

    static {
        if (PlatformDetector.isLowLevel()) {
            initLowLevel();
        }
    }

    @Import(name = "teavm_date_init")
    @RuntimeInclude("date.h")
    @UnsupportedOn(Platforms.WEBASSEMBLY)
    @NoSideEffects
    @Unmanaged
    private static native void initLowLevel();

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
        this(PlatformDetector.isLowLevel()
                ? initDateLowLevel(year, month, date, hrs, min, sec)
                : (long) JSDate.create(year, month, date, hrs, min, sec).getTime());
        if (!PlatformDetector.isLowLevel()) {
            setYear(year);
        }
    }

    @Import(name = "teavm_date_create")
    @NoSideEffects
    @Unmanaged
    @RuntimeInclude("date.h")
    @UnsupportedOn(Platforms.WEBASSEMBLY)
    private static native long initDateLowLevel(int year, int month, int date, int hrs, int min, int sec);

    public TDate(String s) {
        this(parse(s));
    }

    @Override
    public Object clone() {
        return new TDate(value);
    }

    @Deprecated
    public static long UTC(int year, int month, int date, int hrs, int min, int sec) {
        if (PlatformDetector.isLowLevel()) {
            return initUtcDateLowLevel(year, month, date, hrs, min, sec);
        } else {
            return (long) JSDate.UTC(year, month, date, hrs, min, sec);
        }
    }

    @Import(name = "teavm_date_createUtc")
    @NoSideEffects
    @Unmanaged
    @RuntimeInclude("date.h")
    @UnsupportedOn(Platforms.WEBASSEMBLY)
    private static native long initUtcDateLowLevel(int year, int month, int date, int hrs, int min, int sec);

    @Deprecated
    public static long parse(String s) {
        if (PlatformDetector.isLowLevel()) {
            return parseLowLevel(s);
        }

        double value = JSDate.parse(s);
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("Can't parse date: " + s);
        }
        return (long) value;
    }

    @Import(name = "teavm_date_parse")
    @NoSideEffects
    @Unmanaged
    @RuntimeInclude("date.h")
    @UnsupportedOn(Platforms.WEBASSEMBLY)
    private static native long parseLowLevel(String s);

    @Deprecated
    public int getYear() {
        if (PlatformDetector.isLowLevel()) {
            return getYearLowLevel(value);
        }
        return JSDate.create(value).getFullYear() - 1900;
    }

    @Import(name = "teavm_date_getYear")
    @NoSideEffects
    @Unmanaged
    @RuntimeInclude("date.h")
    @UnsupportedOn(Platforms.WEBASSEMBLY)
    private static native int getYearLowLevel(long date);

    @Deprecated
    public void setYear(int year) {
        if (PlatformDetector.isLowLevel()) {
            value = setYearLowLevel(value, year);
            return;
        }
        JSDate date = JSDate.create(value);
        date.setFullYear(year + 1900);
        value = (long) date.getTime();
    }

    @Import(name = "teavm_date_setYear")
    @NoSideEffects
    @Unmanaged
    @RuntimeInclude("date.h")
    @UnsupportedOn(Platforms.WEBASSEMBLY)
    private static native long setYearLowLevel(long date, int year);

    @Deprecated
    public int getMonth() {
        if (PlatformDetector.isLowLevel()) {
            return getMonthLowLevel(value);
        }
        return JSDate.create(value).getMonth();
    }

    @Import(name = "teavm_date_getMonth")
    @NoSideEffects
    @Unmanaged
    @RuntimeInclude("date.h")
    @UnsupportedOn(Platforms.WEBASSEMBLY)
    private static native int getMonthLowLevel(long date);

    @Deprecated
    public void setMonth(int month) {
        if (PlatformDetector.isLowLevel()) {
            value = setMonthLowLevel(value, month);
            return;
        }
        JSDate date = JSDate.create(value);
        date.setMonth(month);
        value = (long) date.getTime();
    }

    @Import(name = "teavm_date_setMonth")
    @NoSideEffects
    @Unmanaged
    @RuntimeInclude("date.h")
    @UnsupportedOn(Platforms.WEBASSEMBLY)
    private static native long setMonthLowLevel(long date, int month);

    @Deprecated
    public int getDate() {
        if (PlatformDetector.isLowLevel()) {
            return getDateLowLevel(value);
        }
        return JSDate.create(value).getDate();
    }

    @Import(name = "teavm_date_getDate")
    @NoSideEffects
    @Unmanaged
    @RuntimeInclude("date.h")
    @UnsupportedOn(Platforms.WEBASSEMBLY)
    private static native int getDateLowLevel(long date);

    @Deprecated
    public void setDate(int date) {
        if (PlatformDetector.isLowLevel()) {
            value = setDateLowLevel(value, date);
            return;
        }
        JSDate d = JSDate.create(value);
        d.setDate(date);
        this.value = (long) d.getTime();
    }

    @Import(name = "teavm_date_setDate")
    @NoSideEffects
    @Unmanaged
    @RuntimeInclude("date.h")
    @UnsupportedOn(Platforms.WEBASSEMBLY)
    private static native int setDateLowLevel(long target, int date);

    @Deprecated
    public int getDay() {
        if (PlatformDetector.isLowLevel()) {
            return getDayLowLevel(value);
        }
        return JSDate.create(value).getDay();
    }

    @Import(name = "teavm_date_getDay")
    @UnsupportedOn(Platforms.WEBASSEMBLY)
    @Unmanaged
    public static native int getDayLowLevel(long date);

    @Deprecated
    public int getHours() {
        if (PlatformDetector.isLowLevel()) {
            return getHoursLowLevel(value);
        }
        return JSDate.create(value).getHours();
    }

    @Import(name = "teavm_date_getHours")
    @NoSideEffects
    @Unmanaged
    @RuntimeInclude("date.h")
    @UnsupportedOn(Platforms.WEBASSEMBLY)
    private static native int getHoursLowLevel(long date);

    @Deprecated
    public void setHours(int hours) {
        if (PlatformDetector.isLowLevel()) {
            value = setHoursLowLevel(value, hours);
            return;
        }
        JSDate date = JSDate.create(value);
        date.setHours(hours);
        value = (long) date.getTime();
    }

    @Import(name = "teavm_date_setHours")
    @NoSideEffects
    @Unmanaged
    @RuntimeInclude("date.h")
    @UnsupportedOn(Platforms.WEBASSEMBLY)
    private static native int setHoursLowLevel(long date, int hours);

    @Deprecated
    public int getMinutes() {
        if (PlatformDetector.isLowLevel()) {
            return getMinutesLowLevel(value);
        }
        return JSDate.create(value).getMinutes();
    }

    @Import(name = "teavm_date_getMinutes")
    @NoSideEffects
    @Unmanaged
    @RuntimeInclude("date.h")
    @UnsupportedOn(Platforms.WEBASSEMBLY)
    private static native int getMinutesLowLevel(long date);

    @Deprecated
    public void setMinutes(int minutes) {
        if (PlatformDetector.isLowLevel()) {
            value = setMinutesLowLevel(value, minutes);
            return;
        }
        JSDate date = JSDate.create(value);
        date.setMinutes(minutes);
        this.value = (long) date.getTime();
    }

    @Import(name = "teavm_date_setMinutes")
    @NoSideEffects
    @Unmanaged
    @RuntimeInclude("date.h")
    @UnsupportedOn(Platforms.WEBASSEMBLY)
    private static native int setMinutesLowLevel(long date, int minutes);

    @Deprecated
    public int getSeconds() {
        if (PlatformDetector.isLowLevel()) {
            return getSecondsLowLevel(value);
        }
        return JSDate.create(value).getSeconds();
    }

    @Import(name = "teavm_date_getSeconds")
    @NoSideEffects
    @Unmanaged
    @RuntimeInclude("date.h")
    @UnsupportedOn(Platforms.WEBASSEMBLY)
    private static native int getSecondsLowLevel(long date);

    @Deprecated
    public void setSeconds(int seconds) {
        if (PlatformDetector.isLowLevel()) {
            value = setSecondsLowLevel(value, seconds);
            return;
        }
        JSDate date = JSDate.create(value);
        date.setSeconds(seconds);
        this.value = (long) date.getTime();
    }

    @Import(name = "teavm_date_setSeconds")
    @NoSideEffects
    @Unmanaged
    @RuntimeInclude("date.h")
    @UnsupportedOn(Platforms.WEBASSEMBLY)
    private static native int setSecondsLowLevel(long date, int seconds);

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
        if (PlatformDetector.isLowLevel()) {
            return toStringLowLevel(value);
        } else {
            return JSDate.create(value).stringValue();
        }
    }

    @Import(name = "teavm_date_format")
    @NoSideEffects
    @RuntimeInclude("date.h")
    @UnsupportedOn(Platforms.WEBASSEMBLY)
    private static native String toStringLowLevel(long date);

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
