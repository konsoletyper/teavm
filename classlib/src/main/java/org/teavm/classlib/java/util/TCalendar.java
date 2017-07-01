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
/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.teavm.classlib.java.util;

import org.teavm.classlib.impl.unicode.CLDRHelper;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TCloneable;
import org.teavm.classlib.java.lang.TComparable;
import org.teavm.platform.metadata.IntResource;
import org.teavm.platform.metadata.ResourceMap;

public abstract class TCalendar implements TSerializable, TCloneable, TComparable<TCalendar> {
    protected boolean areFieldsSet;

    protected int[] fields;

    protected boolean[] isSet;

    protected boolean isTimeSet;

    protected long time;

    transient int lastTimeFieldSet;

    transient int lastDateFieldSet;

    private boolean lenient;

    private int firstDayOfWeek;

    private int minimalDaysInFirstWeek;

    public static final int JANUARY = 0;

    public static final int FEBRUARY = 1;

    public static final int MARCH = 2;

    public static final int APRIL = 3;

    public static final int MAY = 4;

    public static final int JUNE = 5;

    public static final int JULY = 6;

    public static final int AUGUST = 7;

    public static final int SEPTEMBER = 8;

    public static final int OCTOBER = 9;

    public static final int NOVEMBER = 10;

    public static final int DECEMBER = 11;

    public static final int UNDECIMBER = 12;

    public static final int SUNDAY = 1;

    public static final int MONDAY = 2;

    public static final int TUESDAY = 3;

    public static final int WEDNESDAY = 4;

    public static final int THURSDAY = 5;

    public static final int FRIDAY = 6;

    public static final int SATURDAY = 7;

    public static final int ERA = 0;

    public static final int YEAR = 1;

    public static final int MONTH = 2;

    public static final int WEEK_OF_YEAR = 3;

    public static final int WEEK_OF_MONTH = 4;

    public static final int DATE = 5;

    public static final int DAY_OF_MONTH = 5;

    public static final int DAY_OF_YEAR = 6;

    public static final int DAY_OF_WEEK = 7;

    public static final int DAY_OF_WEEK_IN_MONTH = 8;

    public static final int AM_PM = 9;

    public static final int HOUR = 10;

    public static final int HOUR_OF_DAY = 11;

    public static final int MINUTE = 12;

    public static final int SECOND = 13;

    public static final int MILLISECOND = 14;

    public static final int ZONE_OFFSET = 15;

    public static final int DST_OFFSET = 16;

    public static final int FIELD_COUNT = 17;

    public static final int AM = 0;

    public static final int PM = 1;

    private static String[] fieldNames = { "ERA=", "YEAR=", "MONTH=", "WEEK_OF_YEAR=", "WEEK_OF_MONTH=",
            "DAY_OF_MONTH=", "DAY_OF_YEAR=", "DAY_OF_WEEK=", "DAY_OF_WEEK_IN_MONTH=", "AM_PM=", "HOUR=", "HOUR_OF_DAY",
            "MINUTE=", "SECOND=", "MILLISECOND=", "ZONE_OFFSET=", "DST_OFFSET=" };

    private TTimeZone zone;

    private static int firstDayOfWeekCache = -1;
    private static int minimalDaysInFirstWeekCache = -1;
    private static TLocale cacheFor;

    protected TCalendar() {
        this(TTimeZone.getDefault(), TLocale.getDefault());
    }

    TCalendar(TTimeZone timezone) {
        fields = new int[FIELD_COUNT];
        isSet = new boolean[FIELD_COUNT];
        areFieldsSet = false;
        isTimeSet = false;
        setLenient(true);
        setTimeZone(timezone);
    }

    protected TCalendar(TTimeZone timezone, TLocale locale) {
        this(timezone);
        fields = new int[FIELD_COUNT];
        isSet = new boolean[FIELD_COUNT];
        areFieldsSet = false;
        isTimeSet = false;
        setLenient(true);
        setFirstDayOfWeek(resolveFirstDayOfWeek(locale));
        setMinimalDaysInFirstWeek(resolveMinimalDaysInFirstWeek(locale));
        cacheFor = locale;
    }

    private static int resolveFirstDayOfWeek(TLocale locale) {
        if (locale == cacheFor && firstDayOfWeekCache >= 0) {
            return firstDayOfWeekCache;
        }
        String country = CLDRHelper.resolveCountry(locale.getLanguage(), locale.getCountry());
        ResourceMap<IntResource> dayMap = CLDRHelper.getFirstDayOfWeek();
        firstDayOfWeekCache = dayMap.has(country) ? dayMap.get(country).getValue() : dayMap.get("001").getValue();
        return firstDayOfWeekCache;
    }

    private static int resolveMinimalDaysInFirstWeek(TLocale locale) {
        if (locale == cacheFor && minimalDaysInFirstWeekCache >= 0) {
            return minimalDaysInFirstWeekCache;
        }
        String country = CLDRHelper.resolveCountry(locale.getLanguage(), locale.getCountry());
        ResourceMap<IntResource> dayMap = CLDRHelper.getMinimalDaysInFirstWeek();
        minimalDaysInFirstWeekCache = dayMap.has(country) ? dayMap.get(country).getValue()
                : dayMap.get("001").getValue();
        return minimalDaysInFirstWeekCache;
    }

    abstract public void add(int field, int value);

    public boolean after(Object calendar) {
        if (!(calendar instanceof TCalendar)) {
            return false;
        }
        return getTimeInMillis() > ((TCalendar) calendar).getTimeInMillis();
    }

    public boolean before(Object calendar) {
        if (!(calendar instanceof TCalendar)) {
            return false;
        }
        return getTimeInMillis() < ((TCalendar) calendar).getTimeInMillis();
    }

    public final void clear() {
        for (int i = 0; i < FIELD_COUNT; i++) {
            fields[i] = 0;
            isSet[i] = false;
        }
        areFieldsSet = false;
        isTimeSet = false;
    }

    public final void clear(int field) {
        fields[field] = 0;
        isSet[field] = false;
        areFieldsSet = false;
        isTimeSet = false;
    }

    @Override
    public Object clone() {
        try {
            TCalendar clone = (TCalendar) super.clone();
            clone.fields = fields.clone();
            clone.isSet = isSet.clone();
            clone.zone = (TTimeZone) zone.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    protected void complete() {
        if (!isTimeSet) {
            computeTime();
            isTimeSet = true;
        }
        if (!areFieldsSet) {
            computeFields();
            areFieldsSet = true;
        }
    }

    protected abstract void computeFields();

    protected abstract void computeTime();

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TCalendar)) {
            return false;
        }
        TCalendar cal = (TCalendar) object;
        return getTimeInMillis() == cal.getTimeInMillis() && isLenient() == cal.isLenient()
                && getFirstDayOfWeek() == cal.getFirstDayOfWeek()
                && getMinimalDaysInFirstWeek() == cal.getMinimalDaysInFirstWeek()
                && zone.equals(cal.zone);
    }

    public int get(int field) {
        complete();
        return fields[field];
    }

    public int getActualMaximum(int field) {
        int value;
        int max = getMaximum(field);
        int next = getLeastMaximum(field);
        if (max == next) {
            return next;
        }
        complete();
        long orgTime = time;
        set(field, next);
        do {
            value = next;
            roll(field, true);
            next = get(field);
        } while (next > value);
        time = orgTime;
        areFieldsSet = false;
        return value;
    }

    public int getActualMinimum(int field) {
        int value;
        int min = getMinimum(field);
        int next = getGreatestMinimum(field);
        if (min == next) {
            return next;
        }
        complete();
        long orgTime = time;
        set(field, next);
        do {
            value = next;
            roll(field, false);
            next = get(field);
        } while (next < value);
        time = orgTime;
        areFieldsSet = false;
        return value;
    }

    public static TLocale[] getAvailableLocales() {
        return TLocale.getAvailableLocales();
    }

    public int getFirstDayOfWeek() {
        return firstDayOfWeek;
    }

    abstract public int getGreatestMinimum(int field);

    public static TCalendar getInstance() {
        return new TGregorianCalendar();
    }

    public static TCalendar getInstance(TLocale locale) {
        return new TGregorianCalendar(locale);
    }

    public static TCalendar getInstance(TTimeZone zone, TLocale locale) {
        return new TGregorianCalendar(zone, locale);
    }

    public static TCalendar getInstance(TTimeZone timezone) {
        return new TGregorianCalendar(timezone);
    }

    abstract public int getLeastMaximum(int field);

    abstract public int getMaximum(int field);

    public int getMinimalDaysInFirstWeek() {
        return minimalDaysInFirstWeek;
    }

    abstract public int getMinimum(int field);

    public final TDate getTime() {
        return new TDate(getTimeInMillis());
    }

    public long getTimeInMillis() {
        if (!isTimeSet) {
            computeTime();
            isTimeSet = true;
        }
        return time;
    }

    public TTimeZone getTimeZone() {
        return zone;
    }

    public void setTimeZone(TTimeZone timezone) {
        zone = timezone;
        areFieldsSet = false;
    }

    @Override
    public int hashCode() {
        return (isLenient() ? 1237 : 1231) + getFirstDayOfWeek() + getMinimalDaysInFirstWeek();
    }

    protected final int internalGet(int field) {
        return fields[field];
    }

    public boolean isLenient() {
        return lenient;
    }

    public final boolean isSet(int field) {
        return isSet[field];
    }

    public void roll(int field, int value) {
        boolean increment = value >= 0;
        int count = increment ? value : -value;
        for (int i = 0; i < count; i++) {
            roll(field, increment);
        }
    }

    abstract public void roll(int field, boolean increment);

    public void set(int field, int value) {
        fields[field] = value;
        isSet[field] = true;
        areFieldsSet = false;
        isTimeSet = false;
        if (field > MONTH && field < AM_PM) {
            lastDateFieldSet = field;
        }
        if (field == HOUR || field == HOUR_OF_DAY) {
            lastTimeFieldSet = field;
        }
        if (field == AM_PM) {
            lastTimeFieldSet = HOUR;
        }
    }

    public final void set(int year, int month, int day) {
        set(YEAR, year);
        set(MONTH, month);
        set(DATE, day);
    }

    public final void set(int year, int month, int day, int hourOfDay, int minute) {
        set(year, month, day);
        set(HOUR_OF_DAY, hourOfDay);
        set(MINUTE, minute);
    }

    public final void set(int year, int month, int day, int hourOfDay, int minute, int second) {
        set(year, month, day, hourOfDay, minute);
        set(SECOND, second);
    }

    public void setFirstDayOfWeek(int value) {
        firstDayOfWeek = value;
    }

    public void setLenient(boolean value) {
        lenient = value;
    }

    public void setMinimalDaysInFirstWeek(int value) {
        minimalDaysInFirstWeek = value;
    }

    public final void setTime(TDate date) {
        setTimeInMillis(date.getTime());
    }

    public void setTimeInMillis(long milliseconds) {
        if (!isTimeSet || !areFieldsSet || time != milliseconds) {
            time = milliseconds;
            isTimeSet = true;
            areFieldsSet = false;
            complete();
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(getClass().getName() + "[time="
                + (isTimeSet ? String.valueOf(time) : "?") + ",areFieldsSet=" + areFieldsSet + ",lenient=" + lenient
                + ",firstDayOfWeek=" + firstDayOfWeek + ",minimalDaysInFirstWeek="
                + minimalDaysInFirstWeek);
        for (int i = 0; i < FIELD_COUNT; i++) {
            result.append(',');
            result.append(fieldNames[i]);
            result.append('=');
            if (isSet[i]) {
                result.append(fields[i]);
            } else {
                result.append('?');
            }
        }
        result.append(']');
        return result.toString();
    }

    @Override
    public int compareTo(TCalendar anotherCalendar) {
        if (null == anotherCalendar) {
            throw new NullPointerException();
        }
        long timeInMillis = getTimeInMillis();
        long anotherTimeInMillis = anotherCalendar.getTimeInMillis();
        if (timeInMillis > anotherTimeInMillis) {
            return 1;
        }
        if (timeInMillis == anotherTimeInMillis) {
            return 0;
        }
        return -1;
    }
}
