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

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TCloneable;
import org.teavm.classlib.java.lang.TComparable;

public class Date implements TSerializable, TCloneable, TComparable<Date> {

    // Used by parse()
    private static int creationYear = new Date().getYear();

    private transient long milliseconds;

    @SuppressWarnings("nls")
    private static String[] dayOfWeekNames = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };

    @SuppressWarnings("nls")
    private static String[] monthNames = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov",
            "Dec" };

    public Date() {
        this(System.currentTimeMillis());
    }

    @Deprecated
    public Date(int year, int month, int day) {
        GregorianCalendar cal = new GregorianCalendar(false);
        cal.set(1900 + year, month, day);
        milliseconds = cal.getTimeInMillis();
    }

    @Deprecated
    public Date(int year, int month, int day, int hour, int minute) {
        GregorianCalendar cal = new GregorianCalendar(false);
        cal.set(1900 + year, month, day, hour, minute);
        milliseconds = cal.getTimeInMillis();
    }

    @Deprecated
    public Date(int year, int month, int day, int hour, int minute, int second) {
        GregorianCalendar cal = new GregorianCalendar(false);
        cal.set(1900 + year, month, day, hour, minute, second);
        milliseconds = cal.getTimeInMillis();
    }

    public Date(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    @Deprecated
    public Date(String string) {
        milliseconds = parse(string);
    }

    public boolean after(Date date) {
        return milliseconds > date.milliseconds;
    }

    public boolean before(Date date) {
        return milliseconds < date.milliseconds;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public int compareTo(Date date) {
        if (milliseconds < date.milliseconds) {
            return -1;
        }
        if (milliseconds == date.milliseconds) {
            return 0;
        }
        return 1;
    }

    @Override
    public boolean equals(Object object) {
        return (object == this) || (object instanceof Date) && (milliseconds == ((Date) object).milliseconds);
    }

    @Deprecated
    public int getDate() {
        return new GregorianCalendar(milliseconds).get(Calendar.DATE);
    }

    @Deprecated
    public int getDay() {
        return new GregorianCalendar(milliseconds).get(Calendar.DAY_OF_WEEK) - 1;
    }

    @Deprecated
    public int getHours() {
        return new GregorianCalendar(milliseconds).get(Calendar.HOUR_OF_DAY);
    }

    @Deprecated
    public int getMinutes() {
        return new GregorianCalendar(milliseconds).get(Calendar.MINUTE);
    }

    @Deprecated
    public int getMonth() {
        return new GregorianCalendar(milliseconds).get(Calendar.MONTH);
    }

    @Deprecated
    public int getSeconds() {
        return new GregorianCalendar(milliseconds).get(Calendar.SECOND);
    }

    public long getTime() {
        return milliseconds;
    }

    @Deprecated
    public int getTimezoneOffset() {
        GregorianCalendar cal = new GregorianCalendar(milliseconds);
        return -(cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 60000;
    }

    @Deprecated
    public int getYear() {
        return new GregorianCalendar(milliseconds).get(Calendar.YEAR) - 1900;
    }

    @Override
    public int hashCode() {
        return (int) (milliseconds >>> 32) ^ (int) milliseconds;
    }

    private static int parse(String string, String[] array) {
        for (int i = 0, alength = array.length, slength = string.length(); i < alength; i++) {
            if (string.regionMatches(true, 0, array[i], 0, slength)) {
                return i;
            }
        }
        return -1;
    }

    @Deprecated
    public static long parse(String string) {

        if (string == null) {
            // luni.06=The string argument is null
            throw new IllegalArgumentException();
        }

        char sign = 0;
        int commentLevel = 0;
        int offset = 0, length = string.length(), state = 0;
        int year = -1, month = -1, date = -1;
        int hour = -1, minute = -1, second = -1, zoneOffset = 0, minutesOffset = 0;
        boolean zone = false;
        final int PAD = 0, LETTERS = 1, NUMBERS = 2;
        StringBuilder buffer = new StringBuilder();

        while (offset <= length) {
            char next = offset < length ? string.charAt(offset) : '\r';
            offset++;

            if (next == '(') {
                commentLevel++;
            }
            if (commentLevel > 0) {
                if (next == ')') {
                    commentLevel--;
                }
                if (commentLevel == 0) {
                    next = ' ';
                } else {
                    continue;
                }
            }

            int nextState = PAD;
            if ('a' <= next && next <= 'z' || 'A' <= next && next <= 'Z') {
                nextState = LETTERS;
            } else if ('0' <= next && next <= '9') {
                nextState = NUMBERS;
            } else if (!Character.isSpace(next) && ",+-:/".indexOf(next) == -1) { //$NON-NLS-1$
                throw new IllegalArgumentException();
            }

            if (state == NUMBERS && nextState != NUMBERS) {
                int digit = Integer.parseInt(buffer.toString());
                buffer.setLength(0);
                if (sign == '+' || sign == '-') {
                    if (zoneOffset == 0) {
                        zone = true;
                        if (next == ':') {
                            minutesOffset = sign == '-' ? -Integer.parseInt(string.substring(offset, offset + 2))
                                    : Integer.parseInt(string.substring(offset, offset + 2));
                            offset += 2;
                        }
                        zoneOffset = sign == '-' ? -digit : digit;
                        sign = 0;
                    } else {
                        throw new IllegalArgumentException();
                    }
                } else if (digit >= 70) {
                    if (year == -1 && (Character.isSpace(next) || next == ',' || next == '/' || next == '\r')) {
                        year = digit;
                    } else {
                        throw new IllegalArgumentException();
                    }
                } else if (next == ':') {
                    if (hour == -1) {
                        hour = digit;
                    } else if (minute == -1) {
                        minute = digit;
                    } else {
                        throw new IllegalArgumentException();
                    }
                } else if (next == '/') {
                    if (month == -1) {
                        month = digit - 1;
                    } else if (date == -1) {
                        date = digit;
                    } else {
                        throw new IllegalArgumentException();
                    }
                } else if (Character.isSpace(next) || next == ',' || next == '-' || next == '\r') {
                    if (hour != -1 && minute == -1) {
                        minute = digit;
                    } else if (minute != -1 && second == -1) {
                        second = digit;
                    } else if (date == -1) {
                        date = digit;
                    } else if (year == -1) {
                        year = digit;
                    } else {
                        throw new IllegalArgumentException();
                    }
                } else if (year == -1 && month != -1 && date != -1) {
                    year = digit;
                } else {
                    throw new IllegalArgumentException();
                }
            } else if (state == LETTERS && nextState != LETTERS) {
                String text = buffer.toString().toUpperCase();
                buffer.setLength(0);
                if (text.length() == 1) {
                    throw new IllegalArgumentException();
                }
                if (text.equals("AM")) { //$NON-NLS-1$
                    if (hour == 12) {
                        hour = 0;
                    } else if (hour < 1 || hour > 12) {
                        throw new IllegalArgumentException();
                    }
                } else if (text.equals("PM")) { //$NON-NLS-1$
                    if (hour == 12) {
                        hour = 0;
                    } else if (hour < 1 || hour > 12) {
                        throw new IllegalArgumentException();
                    }
                    hour += 12;
                } else {
                    DateFormatSymbols symbols = new DateFormatSymbols(TLocale.US);
                    String[] weekdays = symbols.getWeekdays(), months = symbols.getMonths();
                    int value;
                    if (parse(text, weekdays) != -1) {/* empty */
                    } else if (month == -1 && (month = parse(text, months)) != -1) {/* empty */
                    } else if (text.equals("GMT") || text.equals("UT") || text.equals("UTC")) {
                        zone = true;
                        zoneOffset = 0;
                    } else if ((value = zone(text)) != 0) {
                        zone = true;
                        zoneOffset = value;
                    } else {
                        throw new IllegalArgumentException();
                    }
                }
            }

            if (next == '+' || (year != -1 && next == '-')) {
                sign = next;
            } else if (!Character.isSpace(next) && next != ',' && nextState != NUMBERS) {
                sign = 0;
            }

            if (nextState == LETTERS || nextState == NUMBERS) {
                buffer.append(next);
            }
            state = nextState;
        }

        if (year != -1 && month != -1 && date != -1) {
            if (hour == -1) {
                hour = 0;
            }
            if (minute == -1) {
                minute = 0;
            }
            if (second == -1) {
                second = 0;
            }
            if (year < (creationYear - 80)) {
                year += 2000;
            } else if (year < 100) {
                year += 1900;
            }
            minute -= minutesOffset;
            if (zone) {
                if (zoneOffset >= 24 || zoneOffset <= -24) {
                    hour -= zoneOffset / 100;
                    minute -= zoneOffset % 100;
                } else {
                    hour -= zoneOffset;
                }
                return UTC(year - 1900, month, date, hour, minute, second);
            }
            return new Date(year - 1900, month, date, hour, minute, second).getTime();
        }
        throw new IllegalArgumentException();
    }

    @Deprecated
    public void setDate(int day) {
        GregorianCalendar cal = new GregorianCalendar(milliseconds);
        cal.set(Calendar.DATE, day);
        milliseconds = cal.getTimeInMillis();
    }

    @Deprecated
    public void setHours(int hour) {
        GregorianCalendar cal = new GregorianCalendar(milliseconds);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        milliseconds = cal.getTimeInMillis();
    }

    @Deprecated
    public void setMinutes(int minute) {
        GregorianCalendar cal = new GregorianCalendar(milliseconds);
        cal.set(Calendar.MINUTE, minute);
        milliseconds = cal.getTimeInMillis();
    }

    @Deprecated
    public void setMonth(int month) {
        GregorianCalendar cal = new GregorianCalendar(milliseconds);
        cal.set(Calendar.MONTH, month);
        milliseconds = cal.getTimeInMillis();
    }

    @Deprecated
    public void setSeconds(int second) {
        GregorianCalendar cal = new GregorianCalendar(milliseconds);
        cal.set(Calendar.SECOND, second);
        milliseconds = cal.getTimeInMillis();
    }

    public void setTime(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    @Deprecated
    public void setYear(int year) {
        GregorianCalendar cal = new GregorianCalendar(milliseconds);
        cal.set(Calendar.YEAR, year + 1900);
        milliseconds = cal.getTimeInMillis();
    }

    @Deprecated
    public String toGMTString() {
        SimpleDateFormat format1 = new SimpleDateFormat("d MMM ", TLocale.US);
        SimpleDateFormat format2 = new SimpleDateFormat(" HH:mm:ss 'GMT'", TLocale.US);
        TimeZone gmtZone = TimeZone.getTimeZone("GMT");
        format1.setTimeZone(gmtZone);
        format2.setTimeZone(gmtZone);
        GregorianCalendar gc = new GregorianCalendar(gmtZone);
        gc.setTimeInMillis(milliseconds);
        return format1.format(this) + gc.get(Calendar.YEAR) + format2.format(this);
    }

    @Deprecated
    public String toLocaleString() {
        return DateFormat.getDateTimeInstance().format(this);
    }

    @Override
    public String toString() {
        Calendar cal = new GregorianCalendar(milliseconds);
        TimeZone zone = cal.getTimeZone();
        String zoneName = zone.getDisplayName(zone.inDaylightTime(this), TimeZone.SHORT, TLocale.getDefault());

        StringBuilder sb = new StringBuilder(34);
        sb.append(dayOfWeekNames[cal.get(Calendar.DAY_OF_WEEK) - 1]);
        sb.append(' ');
        sb.append(monthNames[cal.get(Calendar.MONTH)]);
        sb.append(' ');
        sb.append(toTwoDigits(cal.get(Calendar.DAY_OF_MONTH)));
        sb.append(' ');
        sb.append(toTwoDigits(cal.get(Calendar.HOUR_OF_DAY)));
        sb.append(':');
        sb.append(toTwoDigits(cal.get(Calendar.MINUTE)));
        sb.append(':');
        sb.append(toTwoDigits(cal.get(Calendar.SECOND)));
        sb.append(' ');
        sb.append(zoneName);
        sb.append(' ');
        sb.append(cal.get(Calendar.YEAR));

        return sb.toString();
    }

    private String toTwoDigits(int digit) {
        if (digit >= 10) {
            return "" + digit;
        } else {
            return "0" + digit;
        }
    }

    @Deprecated
    public static long UTC(int year, int month, int day, int hour, int minute, int second) {
        GregorianCalendar cal = new GregorianCalendar(false);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(1900 + year, month, day, hour, minute, second);
        return cal.getTimeInMillis();
    }

    private static int zone(String text) {
        if (text.equals("EST")) {
            return -5;
        }
        if (text.equals("EDT")) {
            return -4;
        }
        if (text.equals("CST")) {
            return -6;
        }
        if (text.equals("CDT")) {
            return -5;
        }
        if (text.equals("MST")) {
            return -7;
        }
        if (text.equals("MDT")) {
            return -6;
        }
        if (text.equals("PST")) {
            return -8;
        }
        if (text.equals("PDT")) {
            return -7;
        }
        return 0;
    }
}
