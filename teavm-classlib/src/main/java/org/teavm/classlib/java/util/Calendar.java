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
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teavm.classlib.java.util;

public abstract class Calendar implements TSerializable, TCloneable, TComparable<Calendar> {

    private static final long serialVersionUID = -1807547505821590642L;

    /**
     * Set to {@code true} when the calendar fields have been set from the time, set to
     * {@code false} when a field is changed and the fields must be recomputed.
     */
    protected boolean areFieldsSet;

    /**
     * An integer array of calendar fields. The length is {@code FIELD_COUNT}.
     */
    protected int[] fields;

    /**
     * A boolean array. Each element indicates if the corresponding field has
     * been set. The length is {@code FIELD_COUNT}.
     */
    protected boolean[] isSet;

    /**
     * Set to {@code true} when the time has been set, set to {@code false} when a field is
     * changed and the time must be recomputed.
     */
    protected boolean isTimeSet;

    /**
     * The time in milliseconds since January 1, 1970.
     */
    protected long time;

    transient int lastTimeFieldSet;

    transient int lastDateFieldSet;

    private boolean lenient;

    private int firstDayOfWeek;

    private int minimalDaysInFirstWeek;

    private TimeZone zone;

    /**
     * Value of the {@code MONTH} field indicating the first month of the
     * year.
     */
    public static final int JANUARY = 0;

    /**
     * Value of the {@code MONTH} field indicating the second month of
     * the year.
     */
    public static final int FEBRUARY = 1;

    /**
     * Value of the {@code MONTH} field indicating the third month of the
     * year.
     */
    public static final int MARCH = 2;

    /**
     * Value of the {@code MONTH} field indicating the fourth month of
     * the year.
     */
    public static final int APRIL = 3;

    /**
     * Value of the {@code MONTH} field indicating the fifth month of the
     * year.
     */
    public static final int MAY = 4;

    /**
     * Value of the {@code MONTH} field indicating the sixth month of the
     * year.
     */
    public static final int JUNE = 5;

    /**
     * Value of the {@code MONTH} field indicating the seventh month of
     * the year.
     */
    public static final int JULY = 6;

    /**
     * Value of the {@code MONTH} field indicating the eighth month of
     * the year.
     */
    public static final int AUGUST = 7;

    /**
     * Value of the {@code MONTH} field indicating the ninth month of the
     * year.
     */
    public static final int SEPTEMBER = 8;

    /**
     * Value of the {@code MONTH} field indicating the tenth month of the
     * year.
     */
    public static final int OCTOBER = 9;

    /**
     * Value of the {@code MONTH} field indicating the eleventh month of
     * the year.
     */
    public static final int NOVEMBER = 10;

    /**
     * Value of the {@code MONTH} field indicating the twelfth month of
     * the year.
     */
    public static final int DECEMBER = 11;

    /**
     * Value of the {@code MONTH} field indicating the thirteenth month
     * of the year. Although {@code GregorianCalendar} does not use this
     * value, lunar calendars do.
     */
    public static final int UNDECIMBER = 12;

    /**
     * Value of the {@code DAY_OF_WEEK} field indicating Sunday.
     */
    public static final int SUNDAY = 1;

    /**
     * Value of the {@code DAY_OF_WEEK} field indicating Monday.
     */
    public static final int MONDAY = 2;

    /**
     * Value of the {@code DAY_OF_WEEK} field indicating Tuesday.
     */
    public static final int TUESDAY = 3;

    /**
     * Value of the {@code DAY_OF_WEEK} field indicating Wednesday.
     */
    public static final int WEDNESDAY = 4;

    /**
     * Value of the {@code DAY_OF_WEEK} field indicating Thursday.
     */
    public static final int THURSDAY = 5;

    /**
     * Value of the {@code DAY_OF_WEEK} field indicating Friday.
     */
    public static final int FRIDAY = 6;

    /**
     * Value of the {@code DAY_OF_WEEK} field indicating Saturday.
     */
    public static final int SATURDAY = 7;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * era, e.g., AD or BC in the Julian calendar. This is a calendar-specific
     * value; see subclass documentation.
     *
     * @see GregorianCalendar#AD
     * @see GregorianCalendar#BC
     */
    public static final int ERA = 0;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * year. This is a calendar-specific value; see subclass documentation.
     */
    public static final int YEAR = 1;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * month. This is a calendar-specific value. The first month of the year is
     * {@code JANUARY}; the last depends on the number of months in a
     * year.
     *
     * @see #JANUARY
     * @see #FEBRUARY
     * @see #MARCH
     * @see #APRIL
     * @see #MAY
     * @see #JUNE
     * @see #JULY
     * @see #AUGUST
     * @see #SEPTEMBER
     * @see #OCTOBER
     * @see #NOVEMBER
     * @see #DECEMBER
     * @see #UNDECIMBER
     */
    public static final int MONTH = 2;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * week number within the current year. The first week of the year, as
     * defined by {@code getFirstDayOfWeek()} and
     * {@code getMinimalDaysInFirstWeek()}, has value 1. Subclasses
     * define the value of {@code WEEK_OF_YEAR} for days before the first
     * week of the year.
     *
     * @see #getFirstDayOfWeek
     * @see #getMinimalDaysInFirstWeek
     */
    public static final int WEEK_OF_YEAR = 3;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * week number within the current month. The first week of the month, as
     * defined by {@code getFirstDayOfWeek()} and
     * {@code getMinimalDaysInFirstWeek()}, has value 1. Subclasses
     * define the value of {@code WEEK_OF_MONTH} for days before the
     * first week of the month.
     *
     * @see #getFirstDayOfWeek
     * @see #getMinimalDaysInFirstWeek
     */
    public static final int WEEK_OF_MONTH = 4;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * day of the month. This is a synonym for {@code DAY_OF_MONTH}. The
     * first day of the month has value 1.
     *
     * @see #DAY_OF_MONTH
     */
    public static final int DATE = 5;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * day of the month. This is a synonym for {@code DATE}. The first
     * day of the month has value 1.
     *
     * @see #DATE
     */
    public static final int DAY_OF_MONTH = 5;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * day number within the current year. The first day of the year has value
     * 1.
     */
    public static final int DAY_OF_YEAR = 6;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * day of the week. This field takes values {@code SUNDAY},
     * {@code MONDAY}, {@code TUESDAY}, {@code WEDNESDAY},
     * {@code THURSDAY}, {@code FRIDAY}, and
     * {@code SATURDAY}.
     *
     * @see #SUNDAY
     * @see #MONDAY
     * @see #TUESDAY
     * @see #WEDNESDAY
     * @see #THURSDAY
     * @see #FRIDAY
     * @see #SATURDAY
     */
    public static final int DAY_OF_WEEK = 7;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * ordinal number of the day of the week within the current month. Together
     * with the {@code DAY_OF_WEEK} field, this uniquely specifies a day
     * within a month. Unlike {@code WEEK_OF_MONTH} and
     * {@code WEEK_OF_YEAR}, this field's value does <em>not</em>
     * depend on {@code getFirstDayOfWeek()} or
     * {@code getMinimalDaysInFirstWeek()}. {@code DAY_OF_MONTH 1}
     * through {@code 7} always correspond to <code>DAY_OF_WEEK_IN_MONTH
     * 1</code>;
     * {@code 8} through {@code 15} correspond to
     * {@code DAY_OF_WEEK_IN_MONTH 2}, and so on.
     * {@code DAY_OF_WEEK_IN_MONTH 0} indicates the week before
     * {@code DAY_OF_WEEK_IN_MONTH 1}. Negative values count back from
     * the end of the month, so the last Sunday of a month is specified as
     * {@code DAY_OF_WEEK = SUNDAY, DAY_OF_WEEK_IN_MONTH = -1}. Because
     * negative values count backward they will usually be aligned differently
     * within the month than positive values. For example, if a month has 31
     * days, {@code DAY_OF_WEEK_IN_MONTH -1} will overlap
     * {@code DAY_OF_WEEK_IN_MONTH 5} and the end of {@code 4}.
     *
     * @see #DAY_OF_WEEK
     * @see #WEEK_OF_MONTH
     */
    public static final int DAY_OF_WEEK_IN_MONTH = 8;

    /**
     * Field number for {@code get} and {@code set} indicating
     * whether the {@code HOUR} is before or after noon. E.g., at
     * 10:04:15.250 PM the {@code AM_PM} is {@code PM}.
     *
     * @see #AM
     * @see #PM
     * @see #HOUR
     */
    public static final int AM_PM = 9;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * hour of the morning or afternoon. {@code HOUR} is used for the
     * 12-hour clock. E.g., at 10:04:15.250 PM the {@code HOUR} is 10.
     *
     * @see #AM_PM
     * @see #HOUR_OF_DAY
     */
    public static final int HOUR = 10;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * hour of the day. {@code HOUR_OF_DAY} is used for the 24-hour
     * clock. E.g., at 10:04:15.250 PM the {@code HOUR_OF_DAY} is 22.
     *
     * @see #HOUR
     */
    public static final int HOUR_OF_DAY = 11;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * minute within the hour. E.g., at 10:04:15.250 PM the {@code MINUTE}
     * is 4.
     */
    public static final int MINUTE = 12;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * second within the minute. E.g., at 10:04:15.250 PM the
     * {@code SECOND} is 15.
     */
    public static final int SECOND = 13;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * millisecond within the second. E.g., at 10:04:15.250 PM the
     * {@code MILLISECOND} is 250.
     */
    public static final int MILLISECOND = 14;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * raw offset from GMT in milliseconds.
     */
    public static final int ZONE_OFFSET = 15;

    /**
     * Field number for {@code get} and {@code set} indicating the
     * daylight savings offset in milliseconds.
     */
    public static final int DST_OFFSET = 16;

    /**
     * This is the total number of fields in this calendar.
     */
    public static final int FIELD_COUNT = 17;

    /**
     * Value of the {@code AM_PM} field indicating the period of the day
     * from midnight to just before noon.
     */
    public static final int AM = 0;

    /**
     * Value of the {@code AM_PM} field indicating the period of the day
     * from noon to just before midnight.
     */
    public static final int PM = 1;

    @SuppressWarnings("nls")
    private static String[] fieldNames = { "ERA=", "YEAR=", "MONTH=",
            "WEEK_OF_YEAR=", "WEEK_OF_MONTH=", "DAY_OF_MONTH=", "DAY_OF_YEAR=",
            "DAY_OF_WEEK=", "DAY_OF_WEEK_IN_MONTH=", "AM_PM=", "HOUR=",
            "HOUR_OF_DAY", "MINUTE=", "SECOND=", "MILLISECOND=",
            "ZONE_OFFSET=", "DST_OFFSET=" };

    /**
     * Constructs a {@code Calendar} instance using the default {@code TimeZone} and {@code Locale}.
     */
    protected Calendar() {
        this(TimeZone.getDefault(), Locale.getDefault());
    }

    Calendar(TimeZone timezone) {
        fields = new int[FIELD_COUNT];
        isSet = new boolean[FIELD_COUNT];
        areFieldsSet = isTimeSet = false;
        setLenient(true);
        setTimeZone(timezone);
    }

    /**
     * Constructs a {@code Calendar} instance using the specified {@code TimeZone} and {@code Locale}.
     *
     * @param timezone
     *            the timezone.
     * @param locale
     *            the locale.
     */
    protected Calendar(TimeZone timezone, Locale locale) {
        this(timezone);
        com.ibm.icu.util.Calendar icuCalendar = com.ibm.icu.util.Calendar
                .getInstance(com.ibm.icu.util.SimpleTimeZone
                        .getTimeZone(timezone.getID()), locale);
        setFirstDayOfWeek(icuCalendar.getFirstDayOfWeek());
        setMinimalDaysInFirstWeek(icuCalendar.getMinimalDaysInFirstWeek());
    }


    /**
     * Adds the specified amount to a {@code Calendar} field.
     *
     * @param field
     *            the {@code Calendar} field to modify.
     * @param value
     *            the amount to add to the field.
     * @throws IllegalArgumentException
     *                if {@code field} is {@code DST_OFFSET} or {@code
     *                ZONE_OFFSET}.
     */
    abstract public void add(int field, int value);

    /**
     * Returns whether the {@code Date} specified by this {@code Calendar} instance is after the {@code Date}
     * specified by the parameter. The comparison is not dependent on the time
     * zones of the {@code Calendar}.
     *
     * @param calendar
     *            the {@code Calendar} instance to compare.
     * @return {@code true} when this Calendar is after calendar, {@code false} otherwise.
     * @throws IllegalArgumentException
     *                if the time is not set and the time cannot be computed
     *                from the current field values.
     */
    public boolean after(Object calendar) {
        if (!(calendar instanceof Calendar)) {
            return false;
        }
        return getTimeInMillis() > ((Calendar) calendar).getTimeInMillis();
    }

    /**
     * Returns whether the {@code Date} specified by this {@code Calendar} instance is before the
     * {@code Date} specified by the parameter. The comparison is not dependent on the
     * time zones of the {@code Calendar}.
     *
     * @param calendar
     *            the {@code Calendar} instance to compare.
     * @return {@code true} when this Calendar is before calendar, {@code false} otherwise.
     * @throws IllegalArgumentException
     *                if the time is not set and the time cannot be computed
     *                from the current field values.
     */
    public boolean before(Object calendar) {
        if (!(calendar instanceof Calendar)) {
            return false;
        }
        return getTimeInMillis() < ((Calendar) calendar).getTimeInMillis();
    }

    /**
     * Clears all of the fields of this {@code Calendar}. All fields are initialized to
     * zero.
     */
    public final void clear() {
        for (int i = 0; i < FIELD_COUNT; i++) {
            fields[i] = 0;
            isSet[i] = false;
        }
        areFieldsSet = isTimeSet = false;
    }

    /**
     * Clears the specified field to zero and sets the isSet flag to {@code false}.
     *
     * @param field
     *            the field to clear.
     */
    public final void clear(int field) {
        fields[field] = 0;
        isSet[field] = false;
        areFieldsSet = isTimeSet = false;
    }

    /**
     * Returns a new {@code Calendar} with the same properties.
     *
     * @return a shallow copy of this {@code Calendar}.
     *
     * @see java.lang.Cloneable
     */
    @Override
    public Object clone() {
        try {
            Calendar clone = (Calendar) super.clone();
            clone.fields = fields.clone();
            clone.isSet = isSet.clone();
            clone.zone = (TimeZone) zone.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /**
     * Computes the time from the fields if the time has not already been set.
     * Computes the fields from the time if the fields are not already set.
     *
     * @throws IllegalArgumentException
     *                if the time is not set and the time cannot be computed
     *                from the current field values.
     */
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

    /**
     * Computes the {@code Calendar} fields from {@code time}.
     */
    protected abstract void computeFields();

    /**
     * Computes {@code time} from the Calendar fields.
     *
     * @throws IllegalArgumentException
     *                if the time cannot be computed from the current field
     *                values.
     */
    protected abstract void computeTime();

    /**
     * Compares the specified object to this {@code Calendar} and returns whether they are
     * equal. The object must be an instance of {@code Calendar} and have the same
     * properties.
     *
     * @param object
     *            the object to compare with this object.
     * @return {@code true} if the specified object is equal to this {@code Calendar}, {@code false}
     *         otherwise.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Calendar)) {
            return false;
        }
        Calendar cal = (Calendar) object;
        return getTimeInMillis() == cal.getTimeInMillis()
                && isLenient() == cal.isLenient()
                && getFirstDayOfWeek() == cal.getFirstDayOfWeek()
                && getMinimalDaysInFirstWeek() == cal
                        .getMinimalDaysInFirstWeek()
                && getTimeZone().equals(cal.getTimeZone());
    }

    /**
     * Gets the value of the specified field after computing the field values by
     * calling {@code complete()} first.
     *
     * @param field
     *            the field to get.
     * @return the value of the specified field.
     *
     * @throws IllegalArgumentException
     *                if the fields are not set, the time is not set, and the
     *                time cannot be computed from the current field values.
     * @throws ArrayIndexOutOfBoundsException
     *                if the field is not inside the range of possible fields.
     *                The range is starting at 0 up to {@code FIELD_COUNT}.
     */
    public int get(int field) {
        complete();
        return fields[field];
    }

    /**
     * Gets the maximum value of the specified field for the current date.
     *
     * @param field
     *            the field.
     * @return the maximum value of the specified field.
     */
    public int getActualMaximum(int field) {
        int value, next;
        if (getMaximum(field) == (next = getLeastMaximum(field))) {
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

    /**
     * Gets the minimum value of the specified field for the current date.
     *
     * @param field
     *            the field.
     * @return the minimum value of the specified field.
     */
    public int getActualMinimum(int field) {
        int value, next;
        if (getMinimum(field) == (next = getGreatestMinimum(field))) {
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

    /**
     * Gets the list of installed {@code Locale}s which support {@code Calendar}.
     *
     * @return an array of {@code Locale}.
     */
    public static synchronized Locale[] getAvailableLocales() {
        return Locale.getAvailableLocales();
    }

    /**
     * Gets the first day of the week for this {@code Calendar}.
     *
     * @return the first day of the week.
     */
    public int getFirstDayOfWeek() {
        return firstDayOfWeek;
    }

    /**
     * Gets the greatest minimum value of the specified field. This is the
     * biggest value that {@code getActualMinimum} can return for any possible
     * time.
     *
     * @param field
     *            the field.
     * @return the greatest minimum value of the specified field.
     */
    abstract public int getGreatestMinimum(int field);

    /**
     * Constructs a new instance of the {@code Calendar} subclass appropriate for the
     * default {@code Locale}.
     *
     * @return a {@code Calendar} subclass instance set to the current date and time in
     *         the default {@code Timezone}.
     */
    public static synchronized Calendar getInstance() {
        return new GregorianCalendar();
    }

    /**
     * Constructs a new instance of the {@code Calendar} subclass appropriate for the
     * specified {@code Locale}.
     *
     * @param locale
     *            the locale to use.
     * @return a {@code Calendar} subclass instance set to the current date and time.
     */
    public static synchronized Calendar getInstance(Locale locale) {
        return new GregorianCalendar(locale);
    }

    /**
     * Constructs a new instance of the {@code Calendar} subclass appropriate for the
     * default {@code Locale}, using the specified {@code TimeZone}.
     *
     * @param timezone
     *            the {@code TimeZone} to use.
     * @return a {@code Calendar} subclass instance set to the current date and time in
     *         the specified timezone.
     */
    public static synchronized Calendar getInstance(TimeZone timezone) {
        return new GregorianCalendar(timezone);
    }

    /**
     * Constructs a new instance of the {@code Calendar} subclass appropriate for the
     * specified {@code Locale}.
     *
     * @param timezone
     *            the {@code TimeZone} to use.
     * @param locale
     *            the {@code Locale} to use.
     * @return a {@code Calendar} subclass instance set to the current date and time in
     *         the specified timezone.
     */
    public static synchronized Calendar getInstance(TimeZone timezone,
            Locale locale) {
        return new GregorianCalendar(timezone, locale);
    }

    /**
     * Gets the smallest maximum value of the specified field. This is the
     * smallest value that {@code getActualMaximum()} can return for any
     * possible time.
     *
     * @param field
     *            the field number.
     * @return the smallest maximum value of the specified field.
     */
    abstract public int getLeastMaximum(int field);

    /**
     * Gets the greatest maximum value of the specified field. This returns the
     * biggest value that {@code get} can return for the specified field.
     *
     * @param field
     *            the field.
     * @return the greatest maximum value of the specified field.
     */
    abstract public int getMaximum(int field);

    /**
     * Gets the minimal days in the first week of the year.
     *
     * @return the minimal days in the first week of the year.
     */
    public int getMinimalDaysInFirstWeek() {
        return minimalDaysInFirstWeek;
    }

    /**
     * Gets the smallest minimum value of the specified field. this returns the
     * smallest value thet {@code get} can return for the specified field.
     *
     * @param field
     *            the field number.
     * @return the smallest minimum value of the specified field.
     */
    abstract public int getMinimum(int field);

    /**
     * Gets the time of this {@code Calendar} as a {@code Date} object.
     *
     * @return a new {@code Date} initialized to the time of this {@code Calendar}.
     *
     * @throws IllegalArgumentException
     *                if the time is not set and the time cannot be computed
     *                from the current field values.
     */
    public final Date getTime() {
        return new Date(getTimeInMillis());
    }

    /**
     * Computes the time from the fields if required and returns the time.
     *
     * @return the time of this {@code Calendar}.
     *
     * @throws IllegalArgumentException
     *                if the time is not set and the time cannot be computed
     *                from the current field values.
     */
    public long getTimeInMillis() {
        if (!isTimeSet) {
            computeTime();
            isTimeSet = true;
        }
        return time;
    }

    /**
     * Gets the timezone of this {@code Calendar}.
     *
     * @return the {@code TimeZone} used by this {@code Calendar}.
     */
    public TimeZone getTimeZone() {
        return zone;
    }

    /**
     * Returns an integer hash code for the receiver. Objects which are equal
     * return the same value for this method.
     *
     * @return the receiver's hash.
     *
     * @see #equals
     */
    @Override
    public int hashCode() {
        return (isLenient() ? 1237 : 1231) + getFirstDayOfWeek()
                + getMinimalDaysInFirstWeek() + getTimeZone().hashCode();
    }

    /**
     * Gets the value of the specified field without recomputing.
     *
     * @param field
     *            the field.
     * @return the value of the specified field.
     */
    protected final int internalGet(int field) {
        return fields[field];
    }

    /**
     * Returns if this {@code Calendar} accepts field values which are outside the valid
     * range for the field.
     *
     * @return {@code true} if this {@code Calendar} is lenient, {@code false} otherwise.
     */
    public boolean isLenient() {
        return lenient;
    }

    /**
     * Returns whether the specified field is set.
     *
     * @param field
     *            a {@code Calendar} field number.
     * @return {@code true} if the specified field is set, {@code false} otherwise.
     */
    public final boolean isSet(int field) {
        return isSet[field];
    }

    /**
     * Adds the specified amount to the specified field and wraps the value of
     * the field when it goes beyond the maximum or minimum value for the
     * current date. Other fields will be adjusted as required to maintain a
     * consistent date.
     *
     * @param field
     *            the field to roll.
     * @param value
     *            the amount to add.
     */
    public void roll(int field, int value) {
        boolean increment = value >= 0;
        int count = increment ? value : -value;
        for (int i = 0; i < count; i++) {
            roll(field, increment);
        }
    }

    /**
     * Increment or decrement the specified field and wrap the value of the
     * field when it goes beyond the maximum or minimum value for the current
     * date. Other fields will be adjusted as required to maintain a consistent
     * date.
     *
     * @param field
     *            the number indicating the field to roll.
     * @param increment
     *            {@code true} to increment the field, {@code false} to decrement.
     */
    abstract public void roll(int field, boolean increment);

    /**
     * Sets a field to the specified value.
     *
     * @param field
     *            the code indicating the {@code Calendar} field to modify.
     * @param value
     *            the value.
     */
    public void set(int field, int value) {
        fields[field] = value;
        isSet[field] = true;
        areFieldsSet = isTimeSet = false;
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

    /**
     * Sets the year, month and day of the month fields. Other fields are not
     * changed.
     *
     * @param year
     *            the year.
     * @param month
     *            the month.
     * @param day
     *            the day of the month.
     */
    public final void set(int year, int month, int day) {
        set(YEAR, year);
        set(MONTH, month);
        set(DATE, day);
    }

    /**
     * Sets the year, month, day of the month, hour of day and minute fields.
     * Other fields are not changed.
     *
     * @param year
     *            the year.
     * @param month
     *            the month.
     * @param day
     *            the day of the month.
     * @param hourOfDay
     *            the hour of day.
     * @param minute
     *            the minute.
     */
    public final void set(int year, int month, int day, int hourOfDay,
            int minute) {
        set(year, month, day);
        set(HOUR_OF_DAY, hourOfDay);
        set(MINUTE, minute);
    }

    /**
     * Sets the year, month, day of the month, hour of day, minute and second
     * fields. Other fields are not changed.
     *
     * @param year
     *            the year.
     * @param month
     *            the month.
     * @param day
     *            the day of the month.
     * @param hourOfDay
     *            the hour of day.
     * @param minute
     *            the minute.
     * @param second
     *            the second.
     */
    public final void set(int year, int month, int day, int hourOfDay,
            int minute, int second) {
        set(year, month, day, hourOfDay, minute);
        set(SECOND, second);
    }

    /**
     * Sets the first day of the week for this {@code Calendar}.
     *
     * @param value
     *            a {@code Calendar} day of the week.
     */
    public void setFirstDayOfWeek(int value) {
        firstDayOfWeek = value;
    }

    /**
     * Sets this {@code Calendar} to accept field values which are outside the valid
     * range for the field.
     *
     * @param value
     *            a boolean value.
     */
    public void setLenient(boolean value) {
        lenient = value;
    }

    /**
     * Sets the minimal days in the first week of the year.
     *
     * @param value
     *            the minimal days in the first week of the year.
     */
    public void setMinimalDaysInFirstWeek(int value) {
        minimalDaysInFirstWeek = value;
    }

    /**
     * Sets the time of this {@code Calendar}.
     *
     * @param date
     *            a {@code Date} object.
     */
    public final void setTime(Date date) {
        setTimeInMillis(date.getTime());
    }

    /**
     * Sets the time of this {@code Calendar}.
     *
     * @param milliseconds
     *            the time as the number of milliseconds since Jan. 1, 1970.
     */
    public void setTimeInMillis(long milliseconds) {
        if (!isTimeSet || !areFieldsSet || time != milliseconds) {
            time = milliseconds;
            isTimeSet = true;
            areFieldsSet = false;
            complete();
        }
    }

    /**
     * Sets the {@code TimeZone} used by this Calendar.
     *
     * @param timezone
     *            a {@code TimeZone}.
     */
    public void setTimeZone(TimeZone timezone) {
        zone = timezone;
        areFieldsSet = false;
    }

    /**
     * Returns the string representation of this {@code Calendar}.
     *
     * @return the string representation of this {@code Calendar}.
     */
    @Override
    @SuppressWarnings("nls")
    public String toString() {
        StringBuilder result = new StringBuilder(getClass().getName() + "[time="
                + (isTimeSet ? String.valueOf(time) : "?")
                + ",areFieldsSet="
                + areFieldsSet
                + // ",areAllFieldsSet=" + areAllFieldsSet +
                ",lenient=" + lenient + ",zone=" + zone + ",firstDayOfWeek="
                + firstDayOfWeek + ",minimalDaysInFirstWeek="
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

    /**
     * Compares the times of the two {@code Calendar}, which represent the milliseconds
     * from the January 1, 1970 00:00:00.000 GMT (Gregorian).
     *
     * @param anotherCalendar
     *            another calendar that this one is compared with.
     * @return 0 if the times of the two {@code Calendar}s are equal, -1 if the time of
     *         this {@code Calendar} is before the other one, 1 if the time of this
     *         {@code Calendar} is after the other one.
     * @throws NullPointerException
     *             if the argument is null.
     * @throws IllegalArgumentException
     *             if the argument does not include a valid time
     *             value.
     */
    public int compareTo(Calendar anotherCalendar) {
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

    @SuppressWarnings("nls")
    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("areFieldsSet", Boolean.TYPE),
            new ObjectStreamField("fields", int[].class),
            new ObjectStreamField("firstDayOfWeek", Integer.TYPE),
            new ObjectStreamField("isSet", boolean[].class),
            new ObjectStreamField("isTimeSet", Boolean.TYPE),
            new ObjectStreamField("lenient", Boolean.TYPE),
            new ObjectStreamField("minimalDaysInFirstWeek", Integer.TYPE),
            new ObjectStreamField("nextStamp", Integer.TYPE),
            new ObjectStreamField("serialVersionOnStream", Integer.TYPE),
            new ObjectStreamField("time", Long.TYPE),
            new ObjectStreamField("zone", TimeZone.class), };

    @SuppressWarnings("nls")
    private void writeObject(ObjectOutputStream stream) throws IOException {
        complete();
        ObjectOutputStream.PutField putFields = stream.putFields();
        putFields.put("areFieldsSet", areFieldsSet);
        putFields.put("fields", this.fields);
        putFields.put("firstDayOfWeek", firstDayOfWeek);
        putFields.put("isSet", isSet);
        putFields.put("isTimeSet", isTimeSet);
        putFields.put("lenient", lenient);
        putFields.put("minimalDaysInFirstWeek", minimalDaysInFirstWeek);
        putFields.put("nextStamp", 2 /* MINIMUM_USER_STAMP */);
        putFields.put("serialVersionOnStream", 1);
        putFields.put("time", time);
        putFields.put("zone", zone);
        stream.writeFields();
    }

    @SuppressWarnings("nls")
    private void readObject(ObjectInputStream stream) throws IOException,
            ClassNotFoundException {
        ObjectInputStream.GetField readFields = stream.readFields();
        areFieldsSet = readFields.get("areFieldsSet", false);
        this.fields = (int[]) readFields.get("fields", null);
        firstDayOfWeek = readFields.get("firstDayOfWeek", Calendar.SUNDAY);
        isSet = (boolean[]) readFields.get("isSet", null);
        isTimeSet = readFields.get("isTimeSet", false);
        lenient = readFields.get("lenient", true);
        minimalDaysInFirstWeek = readFields.get("minimalDaysInFirstWeek", 1);
        time = readFields.get("time", 0L);
        zone = (TimeZone) readFields.get("zone", null);
    }
}
