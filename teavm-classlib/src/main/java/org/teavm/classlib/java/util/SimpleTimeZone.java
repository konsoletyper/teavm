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

public class SimpleTimeZone extends TimeZone {

    private int rawOffset;

    private int startYear, startMonth, startDay, startDayOfWeek, startTime;

    private int endMonth, endDay, endDayOfWeek, endTime;

    private int startMode, endMode;

    private static final int DOM_MODE = 1, DOW_IN_MONTH_MODE = 2, DOW_GE_DOM_MODE = 3, DOW_LE_DOM_MODE = 4;

    public static final int UTC_TIME = 2;

    public static final int STANDARD_TIME = 1;

    public static final int WALL_TIME = 0;

    private boolean useDaylight;

    private GregorianCalendar daylightSavings;

    private int dstSavings = 3600000;

    private final transient boolean isSimple;

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

    public SimpleTimeZone(int offset, String name, int startMonth, int startDay, int startDayOfWeek, int startTime,
            int endMonth, int endDay, int endDayOfWeek, int endTime) {
        this(offset, name, startMonth, startDay, startDayOfWeek, startTime, endMonth, endDay, endDayOfWeek, endTime,
                3600000);
    }

    public SimpleTimeZone(int offset, String name, int startMonth, int startDay, int startDayOfWeek, int startTime,
            int endMonth, int endDay, int endDayOfWeek, int endTime, int daylightSavings) {
        icuTZ = getICUTimeZone(name);
        if (icuTZ instanceof com.ibm.icu.util.SimpleTimeZone) {
            isSimple = true;
            com.ibm.icu.util.SimpleTimeZone tz = (com.ibm.icu.util.SimpleTimeZone) icuTZ;
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
            throw new IllegalArgumentException(String.valueOf(daylightSavings));
        }
        dstSavings = daylightSavings;

        setStartRule(startMonth, startDay, startDayOfWeek, startTime);
        setEndRule(endMonth, endDay, endDayOfWeek, endTime);

        useDaylight = daylightSavings > 0 || icuTZ.useDaylightTime();
    }

    public SimpleTimeZone(int offset, String name, int startMonth, int startDay, int startDayOfWeek, int startTime,
            int startTimeMode, int endMonth, int endDay, int endDayOfWeek, int endTime, int endTimeMode,
            int daylightSavings) {

        this(offset, name, startMonth, startDay, startDayOfWeek, startTime, endMonth, endDay, endDayOfWeek, endTime,
                daylightSavings);
        startMode = startTimeMode;
        endMode = endTimeMode;
    }

    @Override
    public Object clone() {
        SimpleTimeZone zone = (SimpleTimeZone) super.clone();
        if (daylightSavings != null) {
            zone.daylightSavings = (GregorianCalendar) daylightSavings.clone();
        }
        return zone;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof SimpleTimeZone)) {
            return false;
        }
        SimpleTimeZone tz = (SimpleTimeZone) object;
        return getID().equals(tz.getID()) &&
                rawOffset == tz.rawOffset &&
                useDaylight == tz.useDaylight &&
                (!useDaylight || (startYear == tz.startYear && startMonth == tz.startMonth && startDay == tz.startDay &&
                        startMode == tz.startMode && startDayOfWeek == tz.startDayOfWeek && startTime == tz.startTime &&
                        endMonth == tz.endMonth && endDay == tz.endDay && endDayOfWeek == tz.endDayOfWeek &&
                        endTime == tz.endTime && endMode == tz.endMode && dstSavings == tz.dstSavings));
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
        if (era != GregorianCalendar.BC && era != GregorianCalendar.AD) {
            throw new IllegalArgumentException(String.valueOf(era));
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

    @Override
    public synchronized int hashCode() {
        int hashCode = getID().hashCode() + rawOffset;
        if (useDaylight) {
            hashCode += startYear + startMonth + startDay + startDayOfWeek + startTime + startMode + endMonth + endDay +
                    endDayOfWeek + endTime + endMode + dstSavings;
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
        return rawOffset == tz.rawOffset && dstSavings == tz.dstSavings && startYear == tz.startYear &&
                startMonth == tz.startMonth && startDay == tz.startDay && startMode == tz.startMode &&
                startDayOfWeek == tz.startDayOfWeek && startTime == tz.startTime && endMonth == tz.endMonth &&
                endDay == tz.endDay && endDayOfWeek == tz.endDayOfWeek && endTime == tz.endTime &&
                endMode == tz.endMode;
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

    public void setDSTSavings(int milliseconds) {
        if (milliseconds > 0) {
            dstSavings = milliseconds;
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void checkRange(int month, int dayOfWeek, int time) {
        if (month < Calendar.JANUARY || month > Calendar.DECEMBER) {
            throw new IllegalArgumentException(String.valueOf(month));
        }
        if (dayOfWeek < Calendar.SUNDAY || dayOfWeek > Calendar.SATURDAY) {
            throw new IllegalArgumentException(String.valueOf(dayOfWeek));
        }
        if (time < 0 || time >= 24 * 3600000) {
            throw new IllegalArgumentException(String.valueOf(time));
        }
    }

    private void checkDay(int month, int day) {
        if (day <= 0 || day > GregorianCalendar.DaysInMonth[month]) {
            throw new IllegalArgumentException(String.valueOf(day));
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
            checkRange(endMonth, endMode == DOM_MODE ? 1 : endDayOfWeek, endTime);
            if (endMode != DOW_IN_MONTH_MODE) {
                checkDay(endMonth, endDay);
            } else {
                if (endDay < -5 || endDay > 5) {
                    throw new IllegalArgumentException(Messages.getString("luni.40", endDay)); //$NON-NLS-1$
                }
            }
        }
        if (endMode != DOM_MODE) {
            endDayOfWeek--;
        }
    }

    public void setEndRule(int month, int dayOfMonth, int time) {
        endMonth = month;
        endDay = dayOfMonth;
        endDayOfWeek = 0; // Initialize this value for hasSameRules()
        endTime = time;
        setEndMode();
        if (isSimple) {
            ((com.ibm.icu.util.SimpleTimeZone) icuTZ).setEndRule(month, dayOfMonth, time);
        }
    }

    public void setEndRule(int month, int day, int dayOfWeek, int time) {
        endMonth = month;
        endDay = day;
        endDayOfWeek = dayOfWeek;
        endTime = time;
        setEndMode();
        if (isSimple) {
            ((com.ibm.icu.util.SimpleTimeZone) icuTZ).setEndRule(month, day, dayOfWeek, time);
        }
    }

    public void setEndRule(int month, int day, int dayOfWeek, int time, boolean after) {
        endMonth = month;
        endDay = after ? day : -day;
        endDayOfWeek = -dayOfWeek;
        endTime = time;
        setEndMode();
        if (isSimple) {
            ((com.ibm.icu.util.SimpleTimeZone) icuTZ).setEndRule(month, day, dayOfWeek, time, after);
        }
    }

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
            checkRange(startMonth, startMode == DOM_MODE ? 1 : startDayOfWeek, startTime);
            if (startMode != DOW_IN_MONTH_MODE) {
                checkDay(startMonth, startDay);
            } else {
                if (startDay < -5 || startDay > 5) {
                    throw new IllegalArgumentException(Messages.getString("luni.40", startDay)); //$NON-NLS-1$
                }
            }
        }
        if (startMode != DOM_MODE) {
            startDayOfWeek--;
        }
    }

    public void setStartRule(int month, int dayOfMonth, int time) {
        startMonth = month;
        startDay = dayOfMonth;
        startDayOfWeek = 0; // Initialize this value for hasSameRules()
        startTime = time;
        setStartMode();
        if (isSimple) {
            ((com.ibm.icu.util.SimpleTimeZone) icuTZ).setStartRule(month, dayOfMonth, time);
        }
    }

    public void setStartRule(int month, int day, int dayOfWeek, int time) {
        startMonth = month;
        startDay = day;
        startDayOfWeek = dayOfWeek;
        startTime = time;
        setStartMode();
        if (isSimple) {
            ((com.ibm.icu.util.SimpleTimeZone) icuTZ).setStartRule(month, day, dayOfWeek, time);
        }
    }

    public void setStartRule(int month, int day, int dayOfWeek, int time, boolean after) {
        startMonth = month;
        startDay = after ? day : -day;
        startDayOfWeek = -dayOfWeek;
        startTime = time;
        setStartMode();
        if (isSimple) {
            ((com.ibm.icu.util.SimpleTimeZone) icuTZ).setStartRule(month, day, dayOfWeek, time, after);
        }
    }

    public void setStartYear(int year) {
        startYear = year;
        useDaylight = true;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[id=" + getID() + ",offset=" + rawOffset + ",dstSavings=" + dstSavings +
                ",useDaylight=" + useDaylight + ",startYear=" + startYear + ",startMode=" + startMode + ",startMonth=" +
                startMonth + ",startDay=" + startDay + ",startDayOfWeek=" +
                (useDaylight && (startMode != DOM_MODE) ? startDayOfWeek + 1 : 0) + ",startTime=" + startTime +
                ",endMode=" + endMode + ",endMonth=" + endMonth + ",endDay=" + endDay + ",endDayOfWeek=" +
                (useDaylight && (endMode != DOM_MODE) ? endDayOfWeek + 1 : 0) + ",endTime=" + endTime + "]";
    }

    @Override
    public boolean useDaylightTime() {
        return useDaylight;
    }
}
