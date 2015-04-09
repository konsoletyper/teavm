/*
 * Copyright 2015 shannah.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teavm.classlib.java.util;

/**
 * {@code SimpleTimeZone} is a concrete subclass of {@code TimeZone}
 * that represents a time zone for use with a Gregorian calendar. This class
 * does not handle historical changes.
 * <p>
 * Use a negative value for {@code dayOfWeekInMonth} to indicate that
 * {@code SimpleTimeZone} should count from the end of the month
 * backwards. For example, Daylight Savings Time ends at the last
 * (dayOfWeekInMonth = -1) Sunday in October, at 2 AM in standard time.
 *
 * @see Calendar
 * @see GregorianCalendar
 * @see TimeZone
 */
public class TSimpleTimeZone extends TTimeZone {
       
    // BEGIN android-removed
    // private static com.ibm.icu.util.TimeZone getICUTimeZone(final String name){
    //     return AccessController.doPrivileged(new PrivilegedAction<com.ibm.icu.util.TimeZone>(){
    //         public com.ibm.icu.util.TimeZone run() {
    //             return com.ibm.icu.util.TimeZone.getTimeZone(name);
    //         }
    //     });
    // }
    // END android-removed
    
    private int rawOffset;
    
    private int startYear, startMonth, startDay, startDayOfWeek, startTime;
    
    private int endMonth, endDay, endDayOfWeek, endTime;
    
    private int startMode, endMode;
    
    private static final int DOM_MODE = 1, DOW_IN_MONTH_MODE = 2,
    DOW_GE_DOM_MODE = 3, DOW_LE_DOM_MODE = 4;
    
    /**
     * The constant for representing a start or end time in GMT time mode.
     */
    public static final int UTC_TIME = 2;
    
    /**
     * The constant for representing a start or end time in standard local time mode,
     * based on timezone's raw offset from GMT; does not include Daylight
     * savings.
     */
    public static final int STANDARD_TIME = 1;
    
    /**
     * The constant for representing a start or end time in local wall clock time
     * mode, based on timezone's adjusted offset from GMT; includes
     * Daylight savings.
     */
    public static final int WALL_TIME = 0;
    
    private boolean useDaylight;
    
    private int dstSavings = 3600000;

    
    private static final int MILLIS_PER_SECOND = 1000;
    private static final int MILLIS_PER_MINUTE = 60*MILLIS_PER_SECOND;
    private static final int MILLIS_PER_HOUR = 60*MILLIS_PER_MINUTE;
    
    //  January 1, 1 CE Gregorian
    private static final int JULIAN_1_CE = 1721426;
    
    //  January 1, 1970 CE Gregorian
    private static final int JULIAN_1970_CE = 2440588;
    
    private static final int[] MONTH_LENGTH = new int[] {
    31,28,31,30,31,30,31,31,30,31,30,31,
    31,29,31,30,31,30,31,31,30,31,30,31
    };
    
    private static final int[] DAYS_BEFORE = new int[] {
    0,31,59,90,120,151,181,212,243,273,304,334,
    0,31,60,91,121,152,182,213,244,274,305,335 };
    
    /**
     * Return the number of days in the given month.
     * @param year Gregorian year, with 0 == 1 BCE, -1 == 2 BCE, etc.
     * @param month 0-based month, with 0==Jan
     * @return the number of days in the given month
     */
    private static final int monthLength(int year, int month) {
        return MONTH_LENGTH[month + (isLeapYear(year) ? 12 : 0)];
    }
    
    
    // BEGIN android-removed
    // private final transient com.ibm.icu.util.TimeZone icuTZ;
    //
    // private final transient boolean isSimple;
    // END android-removed
    
    /**
     * Constructs a {@code SimpleTimeZone} with the given base time zone offset from GMT
     * and time zone ID. Timezone IDs can be obtained from
     * {@code TimeZone.getAvailableIDs}. Normally you should use {@code TimeZone.getDefault} to
     * construct a {@code TimeZone}.
     *
     * @param offset
     *            the given base time zone offset to GMT.
     * @param name
     *            the time zone ID which is obtained from
     *            {@code TimeZone.getAvailableIDs}.
     */
    public TSimpleTimeZone(int offset, final String name) {
        setID(name);
        rawOffset = offset;
        // BEGIN android-removed
        // icuTZ = getICUTimeZone(name);
        // if (icuTZ instanceof com.ibm.icu.util.SimpleTimeZone) {
        //     isSimple = true;
        //     icuTZ.setRawOffset(offset);
        // } else {
        //     isSimple = false;
        // }
        // useDaylight = icuTZ.useDaylightTime();
        // END android-removed
    }
    
    /**
     * Constructs a {@code SimpleTimeZone} with the given base time zone offset from GMT,
     * time zone ID, and times to start and end the daylight savings time. Timezone IDs can
     * be obtained from {@code TimeZone.getAvailableIDs}. Normally you should use
     * {@code TimeZone.getDefault} to create a {@code TimeZone}. For a time zone that does not
     * use daylight saving time, do not use this constructor; instead you should
     * use {@code SimpleTimeZone(rawOffset, ID)}.
     * <p>
     * By default, this constructor specifies day-of-week-in-month rules. That
     * is, if the {@code startDay} is 1, and the {@code startDayOfWeek} is {@code SUNDAY}, then this
     * indicates the first Sunday in the {@code startMonth}. A {@code startDay} of -1 likewise
     * indicates the last Sunday. However, by using negative or zero values for
     * certain parameters, other types of rules can be specified.
     * <p>
     * Day of month: To specify an exact day of the month, such as March 1, set
     * {@code startDayOfWeek} to zero.
     * <p>
     * Day of week after day of month: To specify the first day of the week
     * occurring on or after an exact day of the month, make the day of the week
     * negative. For example, if {@code startDay} is 5 and {@code startDayOfWeek} is {@code -MONDAY},
     * this indicates the first Monday on or after the 5th day of the
     * {@code startMonth}.
     * <p>
     * Day of week before day of month: To specify the last day of the week
     * occurring on or before an exact day of the month, make the day of the
     * week and the day of the month negative. For example, if {@code startDay} is {@code -21}
     * and {@code startDayOfWeek} is {@code -WEDNESDAY}, this indicates the last Wednesday on or
     * before the 21st of the {@code startMonth}.
     * <p>
     * The above examples refer to the {@code startMonth}, {@code startDay}, and {@code startDayOfWeek};
     * the same applies for the {@code endMonth}, {@code endDay}, and {@code endDayOfWeek}.
     * <p>
     * The daylight savings time difference is set to the default value: one hour.
     *
     * @param offset
     *            the given base time zone offset to GMT.
     * @param name
     *            the time zone ID which is obtained from
     *            {@code TimeZone.getAvailableIDs}.
     * @param startMonth
     *            the daylight savings starting month. The month indexing is 0-based. eg, 0
     *            for January.
     * @param startDay
     *            the daylight savings starting day-of-week-in-month. Please see
     *            the member description for an example.
     * @param startDayOfWeek
     *            the daylight savings starting day-of-week. Please see the
     *            member description for an example.
     * @param startTime
     *            the daylight savings starting time in local wall time, which
     *            is standard time in this case. Please see the member
     *            description for an example.
     * @param endMonth
     *            the daylight savings ending month. The month indexing is 0-based. eg, 0 for
     *            January.
     * @param endDay
     *            the daylight savings ending day-of-week-in-month. Please see
     *            the member description for an example.
     * @param endDayOfWeek
     *            the daylight savings ending day-of-week. Please see the member
     *            description for an example.
     * @param endTime
     *            the daylight savings ending time in local wall time, which is
     *            daylight time in this case. Please see the member description
     *            for an example.
     * @throws IllegalArgumentException
     *             if the month, day, dayOfWeek, or time parameters are out of
     *             range for the start or end rule.
     */
    public TSimpleTimeZone(int offset, String name, int startMonth,
                          int startDay, int startDayOfWeek, int startTime, int endMonth,
                          int endDay, int endDayOfWeek, int endTime) {
        this(offset, name, startMonth, startDay, startDayOfWeek, startTime,
             endMonth, endDay, endDayOfWeek, endTime, 3600000);
    }
    
    /**
     * Constructs a {@code SimpleTimeZone} with the given base time zone offset from GMT,
     * time zone ID, times to start and end the daylight savings time, and
     * the daylight savings time difference in milliseconds.
     *
     * @param offset
     *            the given base time zone offset to GMT.
     * @param name
     *            the time zone ID which is obtained from
     *            {@code TimeZone.getAvailableIDs}.
     * @param startMonth
     *            the daylight savings starting month. Month is 0-based. eg, 0
     *            for January.
     * @param startDay
     *            the daylight savings starting day-of-week-in-month. Please see
     *            the description of {@link #SimpleTimeZone(int, String, int, int, int, int, int, int, int, int)} for an example.
     * @param startDayOfWeek
     *            the daylight savings starting day-of-week. Please see the
     *            description of {@link #SimpleTimeZone(int, String, int, int, int, int, int, int, int, int)} for an example.
     * @param startTime
     *            The daylight savings starting time in local wall time, which
     *            is standard time in this case. Please see the description of
     *            {@link #SimpleTimeZone(int, String, int, int, int, int, int, int, int, int)} for an example.
     * @param endMonth
     *            the daylight savings ending month. Month is 0-based. eg, 0 for
     *            January.
     * @param endDay
     *            the daylight savings ending day-of-week-in-month. Please see
     *            the description of {@link #SimpleTimeZone(int, String, int, int, int, int, int, int, int, int)} for an example.
     * @param endDayOfWeek
     *            the daylight savings ending day-of-week. Please see the description of
     *            {@link #SimpleTimeZone(int, String, int, int, int, int, int, int, int, int)} for an example.
     * @param endTime
     *            the daylight savings ending time in local wall time, which is
     *            daylight time in this case. Please see the description of {@link #SimpleTimeZone(int, String, int, int, int, int, int, int, int, int)}
     *            for an example.
     * @param daylightSavings
     *            the daylight savings time difference in milliseconds.
     * @throws IllegalArgumentException
     *                if the month, day, dayOfWeek, or time parameters are out of
     *                range for the start or end rule.
     */
    public TSimpleTimeZone(int offset, String name, int startMonth,
                          int startDay, int startDayOfWeek, int startTime, int endMonth,
                          int endDay, int endDayOfWeek, int endTime, int daylightSavings) {
        // BEGIN android-changed
        // icuTZ = getICUTimeZone(name);
        // if (icuTZ instanceof com.ibm.icu.util.SimpleTimeZone) {
        //     isSimple = true;
        //     com.ibm.icu.util.SimpleTimeZone tz = (com.ibm.icu.util.SimpleTimeZone)icuTZ;
        //     tz.setRawOffset(offset);
        //     tz.setStartRule(startMonth, startDay, startDayOfWeek, startTime);
        //     tz.setEndRule(endMonth, endDay, endDayOfWeek, endTime);
        //     tz.setDSTSavings(daylightSavings);
        // } else {
        //     isSimple = false;
        // }
        // setID(name);
        // rawOffset = offset;
        this(offset, name);
        // END android-changed
        if (daylightSavings <= 0) {
            throw new IllegalArgumentException("Invalid daylightSavings: " + daylightSavings);
        }
        dstSavings = daylightSavings;
        
        setStartRule(startMonth, startDay, startDayOfWeek, startTime);
        setEndRule(endMonth, endDay, endDayOfWeek, endTime);
        
        // BEGIN android-removed
        // useDaylight = daylightSavings > 0 || icuTZ.useDaylightTime();
        // END android-removed
    }
    
    /**
     * Construct a {@code SimpleTimeZone} with the given base time zone offset from GMT,
     * time zone ID, times to start and end the daylight savings time including a
     * mode specifier, the daylight savings time difference in milliseconds.
     * The mode specifies either {@link #WALL_TIME}, {@link #STANDARD_TIME}, or
     * {@link #UTC_TIME}.
     *
     * @param offset
     *            the given base time zone offset to GMT.
     * @param name
     *            the time zone ID which is obtained from
     *            {@code TimeZone.getAvailableIDs}.
     * @param startMonth
     *            the daylight savings starting month. The month indexing is 0-based. eg, 0
     *            for January.
     * @param startDay
     *            the daylight savings starting day-of-week-in-month. Please see
     *            the description of {@link #SimpleTimeZone(int, String, int, int, int, int, int, int, int, int)} for an example.
     * @param startDayOfWeek
     *            the daylight savings starting day-of-week. Please see the
     *            description of {@link #SimpleTimeZone(int, String, int, int, int, int, int, int, int, int)} for an example.
     * @param startTime
     *            the time of day in milliseconds on which daylight savings
     *            time starts, based on the {@code startTimeMode}.
     * @param startTimeMode
     *            the mode (UTC, standard, or wall time) of the start time
     *            value.
     * @param endDay
     *            the day of the week on which daylight savings time ends.
     * @param endMonth
     *            the daylight savings ending month. The month indexing is 0-based. eg, 0 for
     *            January.
     * @param endDayOfWeek
     *            the daylight savings ending day-of-week. Please see the description of
     *            {@link #SimpleTimeZone(int, String, int, int, int, int, int, int, int, int)} for an example.
     * @param endTime
     *            the time of day in milliseconds on which daylight savings
     *            time ends, based on the {@code endTimeMode}.
     * @param endTimeMode
     *            the mode (UTC, standard, or wall time) of the end time value.
     * @param daylightSavings
     *            the daylight savings time difference in milliseconds.
     * @throws IllegalArgumentException
     *             if the month, day, dayOfWeek, or time parameters are out of
     *             range for the start or end rule.
     */
    public TSimpleTimeZone(int offset, String name, int startMonth,
                          int startDay, int startDayOfWeek, int startTime, int startTimeMode,
                          int endMonth, int endDay, int endDayOfWeek, int endTime,
                          int endTimeMode, int daylightSavings) {
        
        this(offset, name, startMonth, startDay, startDayOfWeek, startTime,
             endMonth, endDay, endDayOfWeek, endTime, daylightSavings);
        startMode = startTimeMode;
        endMode = endTimeMode;
    }
        
    /**
     * Compares the specified object to this {@code SimpleTimeZone} and returns whether they
     * are equal. The object must be an instance of {@code SimpleTimeZone} and have the
     * same internal data.
     *
     * @param object
     *            the object to compare with this object.
     * @return {@code true} if the specified object is equal to this
     *         {@code SimpleTimeZone}, {@code false} otherwise.
     * @see #hashCode
     */
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof TSimpleTimeZone)) {
            return false;
        }
        TSimpleTimeZone tz = (TSimpleTimeZone) object;
        return getID().equals(tz.getID())
        && rawOffset == tz.rawOffset
        && useDaylight == tz.useDaylight
        && (!useDaylight || (startYear == tz.startYear
                             && startMonth == tz.startMonth
                             && startDay == tz.startDay && startMode == tz.startMode
                             && startDayOfWeek == tz.startDayOfWeek
                             && startTime == tz.startTime && endMonth == tz.endMonth
                             && endDay == tz.endDay
                             && endDayOfWeek == tz.endDayOfWeek
                             && endTime == tz.endTime && endMode == tz.endMode && dstSavings == tz.dstSavings));
    }
    
    @Override
    public int getDSTSavings() {
        if (!useDaylight) {
            return 0;
        }
        return dstSavings;
    }
    
    @Override
    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int time) {
        if (era != TGregorianCalendar.BC && era != TGregorianCalendar.AD) {
            throw new IllegalArgumentException("Invalid era: " + era);
        }
        checkRange(month, dayOfWeek, time);
        if (month != TCalendar.FEBRUARY || day != 29 || !isLeapYear(year)) {
            checkDay(month, day);
        }
        
        // BEGIN android-changed
        // return icuTZ.getOffset(era, year, month, day, dayOfWeek, time);
        if (!useDaylightTime() || era != TGregorianCalendar.AD || year < startYear) {
            return rawOffset;
        }
        if (endMonth < startMonth) {
            if (month > endMonth && month < startMonth) {
                return rawOffset;
            }
        } else {
            if (month < startMonth || month > endMonth) {
                return rawOffset;
            }
        }
        
        int ruleDay = 0, daysInMonth, firstDayOfMonth = mod7(dayOfWeek - day);
        if (month == startMonth) {
            switch (startMode) {
                case DOM_MODE:
                    ruleDay = startDay;
                    break;
                case DOW_IN_MONTH_MODE:
                    if (startDay >= 0) {
                        ruleDay = mod7(startDayOfWeek - firstDayOfMonth) + 1
                        + (startDay - 1) * 7;
                    } else {
                        daysInMonth = TGregorianCalendar.DaysInMonth[startMonth];
                        if (startMonth == TCalendar.FEBRUARY && isLeapYear(
                                                                          year)) {
                            daysInMonth += 1;
                        }
                        ruleDay = daysInMonth
                        + 1
                        + mod7(startDayOfWeek
                               - (firstDayOfMonth + daysInMonth))
                        + startDay * 7;
                    }
                    break;
                case DOW_GE_DOM_MODE:
                    ruleDay = startDay
                    + mod7(startDayOfWeek
                           - (firstDayOfMonth + startDay - 1));
                    break;
                case DOW_LE_DOM_MODE:
                    ruleDay = startDay
                    + mod7(startDayOfWeek
                           - (firstDayOfMonth + startDay - 1));
                    if (ruleDay != startDay) {
                        ruleDay -= 7;
                    }
                    break;
            }
            if (ruleDay > day || ruleDay == day && time < startTime) {
                return rawOffset;
            }
        }
        
        int ruleTime = endTime - dstSavings;
        int nextMonth = (month + 1) % 12;
        if (month == endMonth || (ruleTime < 0 && nextMonth == endMonth)) {
            switch (endMode) {
                case DOM_MODE:
                    ruleDay = endDay;
                    break;
                case DOW_IN_MONTH_MODE:
                    if (endDay >= 0) {
                        ruleDay = mod7(endDayOfWeek - firstDayOfMonth) + 1
                        + (endDay - 1) * 7;
                    } else {
                        daysInMonth = TGregorianCalendar.DaysInMonth[endMonth];
                        if (endMonth == TCalendar.FEBRUARY && isLeapYear(year)) {
                            daysInMonth++;
                        }
                        ruleDay = daysInMonth
                        + 1
                        + mod7(endDayOfWeek
                               - (firstDayOfMonth + daysInMonth)) + endDay
                        * 7;
                    }
                    break;
                case DOW_GE_DOM_MODE:
                    ruleDay = endDay
                    + mod7(
                           endDayOfWeek - (firstDayOfMonth + endDay - 1));
                    break;
                case DOW_LE_DOM_MODE:
                    ruleDay = endDay
                    + mod7(
                           endDayOfWeek - (firstDayOfMonth + endDay - 1));
                    if (ruleDay != endDay) {
                        ruleDay -= 7;
                    }
                    break;
            }
            
            int ruleMonth = endMonth;
            if (ruleTime < 0) {
                int changeDays = 1 - (ruleTime / 86400000);
                ruleTime = (ruleTime % 86400000) + 86400000;
                ruleDay -= changeDays;
                if (ruleDay <= 0) {
                    if (--ruleMonth < TCalendar.JANUARY) {
                        ruleMonth = TCalendar.DECEMBER;
                    }
                    ruleDay += TGregorianCalendar.DaysInMonth[ruleMonth];
                    if (ruleMonth == TCalendar.FEBRUARY && isLeapYear(year)) {
                        ruleDay++;
                    }
                }
            }
            
            if (month == ruleMonth) {
                if (ruleDay < day || ruleDay == day && time >= ruleTime) {
                    return rawOffset;
                }
            } else if (nextMonth != ruleMonth) {
                return rawOffset;
            }
        }
        return rawOffset + dstSavings;
        // END android-changed
    }
    
    public int getOffset(long time) {
        // BEGIN android-changed: simplified variant of the ICU4J code.
        if (!useDaylightTime()) {
            return rawOffset;
        }
        int[] fields = timeToFields(time + rawOffset, null);
        return getOffset(TGregorianCalendar.AD, fields[0], fields[1], fields[2],
                         fields[3], fields[5]);
        // END android-changed
    }

    private static int[] timeToFields(long time, int[] fields) {
        if (fields == null || fields.length < 6) {
            fields = new int[6];
        }
        long[] remainder = new long[1];
        long day = floorDivide(time, 24*60*60*1000 /* milliseconds per day */, remainder);
        dayToFields(day, fields);
        fields[5] = (int)remainder[0];
        return fields;
    }
    
    private static long floorDivide(long numerator, long denominator) {
        // We do this computation in order to handle
        // a numerator of Long.MIN_VALUE correctly
        return (numerator >= 0) ?
        numerator / denominator :
        ((numerator + 1) / denominator) - 1;
    }

    private static long floorDivide(long numerator, long denominator, long[] remainder) {
        if (numerator >= 0) {
            remainder[0] = numerator % denominator;
            return numerator / denominator;
        }
        long quotient = ((numerator + 1) / denominator) - 1;
        remainder[0] = numerator - (quotient * denominator);
        return quotient;
    }
    
    public static int[] dayToFields(long day, int[] fields) {
        if (fields == null || fields.length < 5) {
            fields = new int[5];
        }
        // Convert from 1970 CE epoch to 1 CE epoch (Gregorian calendar)
        day += JULIAN_1970_CE - JULIAN_1_CE;
        
        long[] rem = new long[1];
        long n400 = floorDivide(day, 146097, rem);
        long n100 = floorDivide(rem[0], 36524, rem);
        long n4 = floorDivide(rem[0], 1461, rem);
        long n1 = floorDivide(rem[0], 365, rem);
        
        int year = (int)(400 * n400 + 100 * n100 + 4 * n4 + n1);
        int dayOfYear = (int)rem[0];
        if (n100 == 4 || n1 == 4) {
            dayOfYear = 365;    // Dec 31 at end of 4- or 400-yr cycle
        }
        else {
            ++year;
        }
        
        boolean isLeap = isLeapYear(year);
        int correction = 0;
        int march1 = isLeap ? 60 : 59;  // zero-based DOY for March 1
        if (dayOfYear >= march1) {
            correction = isLeap ? 1 : 2;
        }
        int month = (12 * (dayOfYear + correction) + 6) / 367;  // zero-based month
        int dayOfMonth = dayOfYear - DAYS_BEFORE[isLeap ? month + 12 : month] + 1; // one-based DOM
        int dayOfWeek = (int)((day + 2) % 7);  // day 0 is Monday(2)
        if (dayOfWeek < 1 /* Sunday */) {
            dayOfWeek += 7;
        }
        dayOfYear++; // 1-based day of year
        
        fields[0] = year;
        fields[1] = month;
        fields[2] = dayOfMonth;
        fields[3] = dayOfWeek;
        fields[4] = dayOfYear;
        
        return fields;
    }    
    
    @Override
    public int getRawOffset() {
        return rawOffset;
    }
    
    /**
     * Returns an integer hash code for the receiver. Objects which are equal
     * return the same value for this method.
     *
     * @return the receiver's hash.
     * @see #equals
     */
    @Override
    public synchronized int hashCode() {
        int hashCode = getID().hashCode() + rawOffset;
        if (useDaylight) {
            hashCode += startYear + startMonth + startDay + startDayOfWeek
            + startTime + startMode + endMonth + endDay + endDayOfWeek
            + endTime + endMode + dstSavings;
        }
        return hashCode;
    }
    
    public boolean hasSameRules(TTimeZone zone) {
        if (!(zone instanceof TSimpleTimeZone)) {
            return false;
        }
        TSimpleTimeZone tz = (TSimpleTimeZone) zone;
        if (useDaylight != tz.useDaylight) {
            return false;
        }
        if (!useDaylight) {
            return rawOffset == tz.rawOffset;
        }
        return rawOffset == tz.rawOffset && dstSavings == tz.dstSavings
        && startYear == tz.startYear && startMonth == tz.startMonth
        && startDay == tz.startDay && startMode == tz.startMode
        && startDayOfWeek == tz.startDayOfWeek
        && startTime == tz.startTime && endMonth == tz.endMonth
        && endDay == tz.endDay && endDayOfWeek == tz.endDayOfWeek
        && endTime == tz.endTime && endMode == tz.endMode;
    }
    
    @Override
    public boolean inDaylightTime(TDate time) {
        // BEGIN android-changed: reuse getOffset.
        return useDaylightTime() && getOffset(time.getTime()) != rawOffset;
        // END android-changed
    }
    
    private static boolean isLeapYear(int year) {
        if (year > 1582) {
            return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
        }
        return year % 4 == 0;
    }
    
    // BEGIN android-added
    private int mod7(int num1) {
        int rem = num1 % 7;
        return (num1 < 0 && rem < 0) ? 7 + rem : rem;
    }
    // END android-added
    
    /**
     * Sets the daylight savings offset in milliseconds for this {@code SimpleTimeZone}.
     *
     * @param milliseconds
     *            the daylight savings offset in milliseconds.
     */
    public void setDSTSavings(int milliseconds) {
        if (milliseconds > 0) {
            dstSavings = milliseconds;
        } else {
            throw new IllegalArgumentException();
        }
    }
    
    private void checkRange(int month, int dayOfWeek, int time) {
        if (month < TCalendar.JANUARY || month > TCalendar.DECEMBER) {
            throw new IllegalArgumentException("Invalid month: " + month);
        }
        if (dayOfWeek < TCalendar.SUNDAY || dayOfWeek > TCalendar.SATURDAY) {
            throw new IllegalArgumentException("Invalid day of week: " + dayOfWeek);
        }
        if (time < 0 || time >= 24 * 3600000) {
            throw new IllegalArgumentException("Invalid time: " + time);
        }
    }
    
    private void checkDay(int month, int day) {
        if (day <= 0 || day > TGregorianCalendar.DaysInMonth[month]) {
            throw new IllegalArgumentException("Invalid day of month: " + day);
        }
    }
    
    private void setEndMode() {
        if (endDayOfWeek == 0) {
            endMode = DOM_MODE;
        } else if (endDayOfWeek < 0) {
            endDayOfWeek = -endDayOfWeek;
            if (endDay < 0) {
                endDay = -endDay;
                endMode = DOW_LE_DOM_MODE;
            } else {
                endMode = DOW_GE_DOM_MODE;
            }
        } else {
            endMode = DOW_IN_MONTH_MODE;
        }
        useDaylight = startDay != 0 && endDay != 0;
        if (endDay != 0) {
            checkRange(endMonth, endMode == DOM_MODE ? 1 : endDayOfWeek,
                       endTime);
            if (endMode != DOW_IN_MONTH_MODE) {
                checkDay(endMonth, endDay);
            } else {
                if (endDay < -5 || endDay > 5) {
                    throw new IllegalArgumentException("Day of week in month: " + endDay);
                }
            }
        }
        if (endMode != DOM_MODE) {
            endDayOfWeek--;
        }
    }
    
    /**
     * Sets the rule which specifies the end of daylight savings time.
     *
     * @param month
     *            the {@code Calendar} month in which daylight savings time ends.
     * @param dayOfMonth
     *            the {@code Calendar} day of the month on which daylight savings time
     *            ends.
     * @param time
     *            the time of day in milliseconds standard time on which
     *            daylight savings time ends.
     */
    public void setEndRule(int month, int dayOfMonth, int time) {
        endMonth = month;
        endDay = dayOfMonth;
        endDayOfWeek = 0; // Initialize this value for hasSameRules()
        endTime = time;
        setEndMode();
        // BEGIN android-removed
        // if (isSimple) {
        //     ((com.ibm.icu.util.SimpleTimeZone) icuTZ).setEndRule(month,
        //             dayOfMonth, time);
        // }
        // END android-removed
    }
    
    /**
     * Sets the rule which specifies the end of daylight savings time.
     *
     * @param month
     *            the {@code Calendar} month in which daylight savings time ends.
     * @param day
     *            the occurrence of the day of the week on which daylight
     *            savings time ends.
     * @param dayOfWeek
     *            the {@code Calendar} day of the week on which daylight savings time
     *            ends.
     * @param time
     *            the time of day in milliseconds standard time on which
     *            daylight savings time ends.
     */
    public void setEndRule(int month, int day, int dayOfWeek, int time) {
        endMonth = month;
        endDay = day;
        endDayOfWeek = dayOfWeek;
        endTime = time;
        setEndMode();
        // BEGIN android-removed
        // if (isSimple) {
        //     ((com.ibm.icu.util.SimpleTimeZone) icuTZ).setEndRule(month, day,
        //             dayOfWeek, time);
        // }
        // END android-removed
    }
    
    /**
     * Sets the rule which specifies the end of daylight savings time.
     *
     * @param month
     *            the {@code Calendar} month in which daylight savings time ends.
     * @param day
     *            the {@code Calendar} day of the month.
     * @param dayOfWeek
     *            the {@code Calendar} day of the week on which daylight savings time
     *            ends.
     * @param time
     *            the time of day in milliseconds on which daylight savings time
     *            ends.
     * @param after
     *            selects the day after or before the day of month.
     */
    public void setEndRule(int month, int day, int dayOfWeek, int time,
                           boolean after) {
        endMonth = month;
        endDay = after ? day : -day;
        endDayOfWeek = -dayOfWeek;
        endTime = time;
        setEndMode();
        // BEGIN android-removed
        // if (isSimple) {
        //     ((com.ibm.icu.util.SimpleTimeZone) icuTZ).setEndRule(month, day,
        //             dayOfWeek, time, after);
        // }
        // END android-removed
    }
    
    /**
     * Sets the offset for standard time from GMT for this {@code SimpleTimeZone}.
     *
     * @param offset
     *            the offset from GMT of standard time in milliseconds.
     */
    public void setRawOffset(int offset) {
        rawOffset = offset;
        // BEGIN android-removed
        // icuTZ.setRawOffset(offset);
        // END android-removed
    }
    
    private void setStartMode() {
        if (startDayOfWeek == 0) {
            startMode = DOM_MODE;
        } else if (startDayOfWeek < 0) {
            startDayOfWeek = -startDayOfWeek;
            if (startDay < 0) {
                startDay = -startDay;
                startMode = DOW_LE_DOM_MODE;
            } else {
                startMode = DOW_GE_DOM_MODE;
            }
        } else {
            startMode = DOW_IN_MONTH_MODE;
        }
        useDaylight = startDay != 0 && endDay != 0;
        if (startDay != 0) {
            checkRange(startMonth, startMode == DOM_MODE ? 1 : startDayOfWeek,
                       startTime);
            if (startMode != DOW_IN_MONTH_MODE) {
                checkDay(startMonth, startDay);
            } else {
                if (startDay < -5 || startDay > 5) {
                    throw new IllegalArgumentException("Day of week in month: " + startDay);
                }
            }
        }
        if (startMode != DOM_MODE) {
            startDayOfWeek--;
        }
    }
    
    /**
     * Sets the rule which specifies the start of daylight savings time.
     *
     * @param month
     *            the {@code Calendar} month in which daylight savings time starts.
     * @param dayOfMonth
     *            the {@code Calendar} day of the month on which daylight savings time
     *            starts.
     * @param time
     *            the time of day in milliseconds on which daylight savings time
     *            starts.
     */
    public void setStartRule(int month, int dayOfMonth, int time) {
        startMonth = month;
        startDay = dayOfMonth;
        startDayOfWeek = 0; // Initialize this value for hasSameRules()
        startTime = time;
        setStartMode();
        // BEGIN android-removed
        // if (isSimple) {
        //     ((com.ibm.icu.util.SimpleTimeZone) icuTZ).setStartRule(month,
        //             dayOfMonth, time);
        // }
        // END android-removed
    }
    
    /**
     * Sets the rule which specifies the start of daylight savings time.
     *
     * @param month
     *            the {@code Calendar} month in which daylight savings time starts.
     * @param day
     *            the occurrence of the day of the week on which daylight
     *            savings time starts.
     * @param dayOfWeek
     *            the {@code Calendar} day of the week on which daylight savings time
     *            starts.
     * @param time
     *            the time of day in milliseconds on which daylight savings time
     *            starts.
     */
    public void setStartRule(int month, int day, int dayOfWeek, int time) {
        startMonth = month;
        startDay = day;
        startDayOfWeek = dayOfWeek;
        startTime = time;
        setStartMode();
        // BEGIN android-removed
        // if (isSimple) {
        //     ((com.ibm.icu.util.SimpleTimeZone) icuTZ).setStartRule(month, day,
        //             dayOfWeek, time);
        // }
        // END android-removed
    }
    
    /**
     * Sets the rule which specifies the start of daylight savings time.
     *
     * @param month
     *            the {@code Calendar} month in which daylight savings time starts.
     * @param day
     *            the {@code Calendar} day of the month.
     * @param dayOfWeek
     *            the {@code Calendar} day of the week on which daylight savings time
     *            starts.
     * @param time
     *            the time of day in milliseconds on which daylight savings time
     *            starts.
     * @param after
     *            selects the day after or before the day of month.
     */
    public void setStartRule(int month, int day, int dayOfWeek, int time,
                             boolean after) {
        startMonth = month;
        startDay = after ? day : -day;
        startDayOfWeek = -dayOfWeek;
        startTime = time;
        setStartMode();
        // BEGIN android-removed
        // if (isSimple) {
        //     ((com.ibm.icu.util.SimpleTimeZone) icuTZ).setStartRule(month, day,
        //             dayOfWeek, time, after);
        // }
        // END android-removed
    }
    
    /**
     * Sets the starting year for daylight savings time in this {@code SimpleTimeZone}.
     * Years before this start year will always be in standard time.
     *
     * @param year
     *            the starting year.
     */
    public void setStartYear(int year) {
        startYear = year;
        useDaylight = true;
    }
    
    /**
     * Returns the string representation of this {@code SimpleTimeZone}.
     *
     * @return the string representation of this {@code SimpleTimeZone}.
     */
    @Override
    public String toString() {
        return getClass().getName()
        + "[id="
        + getID()
        + ",offset="
        + rawOffset
        + ",dstSavings="
        + dstSavings
        + ",useDaylight="
        + useDaylight
        + ",startYear="
        + startYear
        + ",startMode="
        + startMode
        + ",startMonth="
        + startMonth
        + ",startDay="
        + startDay
        + ",startDayOfWeek="
        + (useDaylight && (startMode != DOM_MODE) ? startDayOfWeek + 1
           : 0) + ",startTime=" + startTime + ",endMode="
        + endMode + ",endMonth=" + endMonth + ",endDay=" + endDay
        + ",endDayOfWeek="
        + (useDaylight && (endMode != DOM_MODE) ? endDayOfWeek + 1 : 0)
        + ",endTime=" + endTime + "]";
    }
    
    @Override
    public boolean useDaylightTime() {
        return useDaylight;
    }    
}
