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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.harmony.luni.internal.nls.Messages;

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
public class SimpleTimeZone extends TimeZone {

    private static final long serialVersionUID = -403250971215465050L;

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

    private GregorianCalendar daylightSavings;

    private int dstSavings = 3600000;

    private final transient com.ibm.icu.util.TimeZone icuTZ;

    private final transient boolean isSimple;

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
    public SimpleTimeZone(int offset, final String name) {
        setID(name);
        rawOffset = offset;
        icuTZ = getICUTimeZone(name);
        if (icuTZ instanceof com.ibm.icu.util.SimpleTimeZone) {
            isSimple = true;
            icuTZ.setRawOffset(offset);
        } else {
            isSimple = false;
        }
        useDaylight = icuTZ.useDaylightTime();
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
    public SimpleTimeZone(int offset, String name, int startMonth,
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
    public SimpleTimeZone(int offset, String name, int startMonth,
            int startDay, int startDayOfWeek, int startTime, int endMonth,
            int endDay, int endDayOfWeek, int endTime, int daylightSavings) {
        icuTZ = getICUTimeZone(name);
        if (icuTZ instanceof com.ibm.icu.util.SimpleTimeZone) {
            isSimple = true;
            com.ibm.icu.util.SimpleTimeZone tz = (com.ibm.icu.util.SimpleTimeZone)icuTZ;
            tz.setRawOffset(offset);
            tz.setStartRule(startMonth, startDay, startDayOfWeek, startTime);
            tz.setEndRule(endMonth, endDay, endDayOfWeek, endTime);
            tz.setDSTSavings(daylightSavings);
        } else {
            isSimple = false;
        }
        setID(name);
        rawOffset = offset;
        if (daylightSavings <= 0) {
            throw new IllegalArgumentException(Messages.getString(
                    "luni.3B", daylightSavings)); //$NON-NLS-1$
        }
        dstSavings = daylightSavings;

        setStartRule(startMonth, startDay, startDayOfWeek, startTime);
        setEndRule(endMonth, endDay, endDayOfWeek, endTime);

        useDaylight = daylightSavings > 0 || icuTZ.useDaylightTime();
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
    public SimpleTimeZone(int offset, String name, int startMonth,
            int startDay, int startDayOfWeek, int startTime, int startTimeMode,
            int endMonth, int endDay, int endDayOfWeek, int endTime,
            int endTimeMode, int daylightSavings) {

        this(offset, name, startMonth, startDay, startDayOfWeek, startTime,
                endMonth, endDay, endDayOfWeek, endTime, daylightSavings);
        startMode = startTimeMode;
        endMode = endTimeMode;
    }

    /**
     * Returns a new {@code SimpleTimeZone} with the same ID, {@code rawOffset} and daylight
     * savings time rules as this SimpleTimeZone.
     *
     * @return a shallow copy of this {@code SimpleTimeZone}.
     * @see java.lang.Cloneable
     */
    @Override
    public Object clone() {
        SimpleTimeZone zone = (SimpleTimeZone) super.clone();
        if (daylightSavings != null) {
            zone.daylightSavings = (GregorianCalendar) daylightSavings.clone();
        }
        return zone;
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
        if (!(object instanceof SimpleTimeZone)) {
            return false;
        }
        SimpleTimeZone tz = (SimpleTimeZone) object;
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
    public int getOffset(int era, int year, int month, int day, int dayOfWeek,
            int time) {
        if (era != GregorianCalendar.BC && era != GregorianCalendar.AD) {
            throw new IllegalArgumentException(Messages.getString("luni.3C", era)); //$NON-NLS-1$
        }
        checkRange(month, dayOfWeek, time);
        if (month != Calendar.FEBRUARY || day != 29 || !isLeapYear(year)) {
            checkDay(month, day);
        }
        return icuTZ.getOffset(era, year, month, day, dayOfWeek, time);
    }

    @Override
    public int getOffset(long time) {
        return icuTZ.getOffset(time);
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

    @Override
    public boolean hasSameRules(TimeZone zone) {
        if (!(zone instanceof SimpleTimeZone)) {
            return false;
        }
        SimpleTimeZone tz = (SimpleTimeZone) zone;
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
    public boolean inDaylightTime(Date time) {
        return icuTZ.inDaylightTime(time);
    }

    private boolean isLeapYear(int year) {
        if (year > 1582) {
            return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
        }
        return year % 4 == 0;
    }

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
        if (month < Calendar.JANUARY || month > Calendar.DECEMBER) {
            throw new IllegalArgumentException(Messages.getString("luni.3D", month)); //$NON-NLS-1$
        }
        if (dayOfWeek < Calendar.SUNDAY || dayOfWeek > Calendar.SATURDAY) {
            throw new IllegalArgumentException(Messages
                    .getString("luni.48", dayOfWeek)); //$NON-NLS-1$
        }
        if (time < 0 || time >= 24 * 3600000) {
            throw new IllegalArgumentException(Messages.getString("luni.3E", time)); //$NON-NLS-1$
        }
    }

    private void checkDay(int month, int day) {
        if (day <= 0 || day > GregorianCalendar.DaysInMonth[month]) {
            throw new IllegalArgumentException(Messages.getString("luni.3F", day)); //$NON-NLS-1$
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
                    throw new IllegalArgumentException(Messages.getString(
                            "luni.40", endDay)); //$NON-NLS-1$
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
        if (isSimple) {
            ((com.ibm.icu.util.SimpleTimeZone) icuTZ).setEndRule(month,
                    dayOfMonth, time);
        }
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
        if (isSimple) {
            ((com.ibm.icu.util.SimpleTimeZone) icuTZ).setEndRule(month, day,
                    dayOfWeek, time);
        }
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
        if (isSimple) {
            ((com.ibm.icu.util.SimpleTimeZone) icuTZ).setEndRule(month, day,
                    dayOfWeek, time, after);
        }
    }

    /**
     * Sets the offset for standard time from GMT for this {@code SimpleTimeZone}.
     *
     * @param offset
     *            the offset from GMT of standard time in milliseconds.
     */
    @Override
    public void setRawOffset(int offset) {
        rawOffset = offset;
        icuTZ.setRawOffset(offset);
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
                    throw new IllegalArgumentException(Messages.getString(
                            "luni.40", startDay)); //$NON-NLS-1$
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
        if (isSimple) {
            ((com.ibm.icu.util.SimpleTimeZone) icuTZ).setStartRule(month,
                    dayOfMonth, time);
        }
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
        if (isSimple) {
            ((com.ibm.icu.util.SimpleTimeZone) icuTZ).setStartRule(month, day,
                    dayOfWeek, time);
        }
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
        if (isSimple) {
            ((com.ibm.icu.util.SimpleTimeZone) icuTZ).setStartRule(month, day,
                    dayOfWeek, time, after);
        }
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
                + "[id=" //$NON-NLS-1$
                + getID()
                + ",offset=" //$NON-NLS-1$
                + rawOffset
                + ",dstSavings=" //$NON-NLS-1$
                + dstSavings
                + ",useDaylight=" //$NON-NLS-1$
                + useDaylight
                + ",startYear=" //$NON-NLS-1$
                + startYear
                + ",startMode=" //$NON-NLS-1$
                + startMode
                + ",startMonth=" //$NON-NLS-1$
                + startMonth
                + ",startDay=" //$NON-NLS-1$
                + startDay
                + ",startDayOfWeek=" //$NON-NLS-1$
                + (useDaylight && (startMode != DOM_MODE) ? startDayOfWeek + 1
                        : 0) + ",startTime=" + startTime + ",endMode=" //$NON-NLS-1$ //$NON-NLS-2$
                + endMode + ",endMonth=" + endMonth + ",endDay=" + endDay //$NON-NLS-1$ //$NON-NLS-2$
                + ",endDayOfWeek=" //$NON-NLS-1$
                + (useDaylight && (endMode != DOM_MODE) ? endDayOfWeek + 1 : 0)
                + ",endTime=" + endTime + "]"; //$NON-NLS-1$//$NON-NLS-2$
    }

    @Override
    public boolean useDaylightTime() {
        return useDaylight;
    }

    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("dstSavings", Integer.TYPE), //$NON-NLS-1$
            new ObjectStreamField("endDay", Integer.TYPE), //$NON-NLS-1$
            new ObjectStreamField("endDayOfWeek", Integer.TYPE), //$NON-NLS-1$
            new ObjectStreamField("endMode", Integer.TYPE), //$NON-NLS-1$
            new ObjectStreamField("endMonth", Integer.TYPE), //$NON-NLS-1$
            new ObjectStreamField("endTime", Integer.TYPE), //$NON-NLS-1$
            new ObjectStreamField("monthLength", byte[].class), //$NON-NLS-1$
            new ObjectStreamField("rawOffset", Integer.TYPE), //$NON-NLS-1$
            new ObjectStreamField("serialVersionOnStream", Integer.TYPE), //$NON-NLS-1$
            new ObjectStreamField("startDay", Integer.TYPE), //$NON-NLS-1$
            new ObjectStreamField("startDayOfWeek", Integer.TYPE), //$NON-NLS-1$
            new ObjectStreamField("startMode", Integer.TYPE), //$NON-NLS-1$
            new ObjectStreamField("startMonth", Integer.TYPE), //$NON-NLS-1$
            new ObjectStreamField("startTime", Integer.TYPE), //$NON-NLS-1$
            new ObjectStreamField("startYear", Integer.TYPE), //$NON-NLS-1$
            new ObjectStreamField("useDaylight", Boolean.TYPE), }; //$NON-NLS-1$

    private void writeObject(ObjectOutputStream stream) throws IOException {
        int sEndDay = endDay, sEndDayOfWeek = endDayOfWeek + 1, sStartDay = startDay, sStartDayOfWeek = startDayOfWeek + 1;
        if (useDaylight
                && (startMode != DOW_IN_MONTH_MODE || endMode != DOW_IN_MONTH_MODE)) {
            Calendar cal = new GregorianCalendar(this);
            if (endMode != DOW_IN_MONTH_MODE) {
                cal.set(Calendar.MONTH, endMonth);
                cal.set(Calendar.DATE, endDay);
                sEndDay = cal.get(Calendar.DAY_OF_WEEK_IN_MONTH);
                if (endMode == DOM_MODE) {
                    sEndDayOfWeek = cal.getFirstDayOfWeek();
                }
            }
            if (startMode != DOW_IN_MONTH_MODE) {
                cal.set(Calendar.MONTH, startMonth);
                cal.set(Calendar.DATE, startDay);
                sStartDay = cal.get(Calendar.DAY_OF_WEEK_IN_MONTH);
                if (startMode == DOM_MODE) {
                    sStartDayOfWeek = cal.getFirstDayOfWeek();
                }
            }
        }
        ObjectOutputStream.PutField fields = stream.putFields();
        fields.put("dstSavings", dstSavings); //$NON-NLS-1$
        fields.put("endDay", sEndDay); //$NON-NLS-1$
        fields.put("endDayOfWeek", sEndDayOfWeek); //$NON-NLS-1$
        fields.put("endMode", endMode); //$NON-NLS-1$
        fields.put("endMonth", endMonth); //$NON-NLS-1$
        fields.put("endTime", endTime); //$NON-NLS-1$
        fields.put("monthLength", GregorianCalendar.DaysInMonth); //$NON-NLS-1$
        fields.put("rawOffset", rawOffset); //$NON-NLS-1$
        fields.put("serialVersionOnStream", 1); //$NON-NLS-1$
        fields.put("startDay", sStartDay); //$NON-NLS-1$
        fields.put("startDayOfWeek", sStartDayOfWeek); //$NON-NLS-1$
        fields.put("startMode", startMode); //$NON-NLS-1$
        fields.put("startMonth", startMonth); //$NON-NLS-1$
        fields.put("startTime", startTime); //$NON-NLS-1$
        fields.put("startYear", startYear); //$NON-NLS-1$
        fields.put("useDaylight", useDaylight); //$NON-NLS-1$
        stream.writeFields();
        stream.writeInt(4);
        byte[] values = new byte[4];
        values[0] = (byte) startDay;
        values[1] = (byte) (startMode == DOM_MODE ? 0 : startDayOfWeek + 1);
        values[2] = (byte) endDay;
        values[3] = (byte) (endMode == DOM_MODE ? 0 : endDayOfWeek + 1);
        stream.write(values);
    }

    private void readObject(ObjectInputStream stream) throws IOException,
            ClassNotFoundException {
        ObjectInputStream.GetField fields = stream.readFields();
        rawOffset = fields.get("rawOffset", 0); //$NON-NLS-1$
        useDaylight = fields.get("useDaylight", false); //$NON-NLS-1$
        if (useDaylight) {
            endMonth = fields.get("endMonth", 0); //$NON-NLS-1$
            endTime = fields.get("endTime", 0); //$NON-NLS-1$
            startMonth = fields.get("startMonth", 0); //$NON-NLS-1$
            startTime = fields.get("startTime", 0); //$NON-NLS-1$
            startYear = fields.get("startYear", 0); //$NON-NLS-1$
        }
        if (fields.get("serialVersionOnStream", 0) == 0) { //$NON-NLS-1$
            if (useDaylight) {
                startMode = endMode = DOW_IN_MONTH_MODE;
                endDay = fields.get("endDay", 0); //$NON-NLS-1$
                endDayOfWeek = fields.get("endDayOfWeek", 0) - 1; //$NON-NLS-1$
                startDay = fields.get("startDay", 0); //$NON-NLS-1$
                startDayOfWeek = fields.get("startDayOfWeek", 0) - 1; //$NON-NLS-1$
            }
        } else {
            dstSavings = fields.get("dstSavings", 0); //$NON-NLS-1$
            if (useDaylight) {
                endMode = fields.get("endMode", 0); //$NON-NLS-1$
                startMode = fields.get("startMode", 0); //$NON-NLS-1$
                int length = stream.readInt();
                byte[] values = new byte[length];
                stream.readFully(values);
                if (length >= 4) {
                    startDay = values[0];
                    startDayOfWeek = values[1];
                    if (startMode != DOM_MODE) {
                        startDayOfWeek--;
                    }
                    endDay = values[2];
                    endDayOfWeek = values[3];
                    if (endMode != DOM_MODE) {
                        endDayOfWeek--;
                    }
                }
            }
        }
    }

}
