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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TCloneable;

public abstract class TimeZone implements TSerializable, TCloneable {
    private static final long serialVersionUID = 3581463369166924961L;

    /**
     * The SHORT display name style.
     */
    public static final int SHORT = 0;

    /**
     * The LONG display name style.
     */
    public static final int LONG = 1;

    private static THashMap<String, TimeZone> AvailableZones;

    private static TimeZone Default;

    static TimeZone GMT = new SimpleTimeZone(0, "GMT"); // Greenwich Mean Time

    private String ID;

    private static void initializeAvailable() {
        TimeZone[] zones = TimeZones.getTimeZones();
        AvailableZones = new THashMap<>((zones.length + 1) * 4 / 3);
        AvailableZones.put(GMT.getID(), GMT);
        for (int i = 0; i < zones.length; i++) {
            AvailableZones.put(zones[i].getID(), zones[i]);
        }
    }

    public TimeZone() {
    }

    @Override
    public Object clone() {
        try {
            TimeZone zone = (TimeZone) super.clone();
            return zone;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public static synchronized String[] getAvailableIDs() {
        return AvailableZones.keySet().toArray(new String[0]);
    }

    public static synchronized String[] getAvailableIDs(int offset) {
        List<String> result = new ArrayList<>();
        for (TIterator<TimeZone> iter = AvailableZones.values().iterator(); iter.hasNext()) {
            if ()
        }
        return Arrays.copyOf(result, j);
        int count = 0;
        int length = availableIDs.length;
        String[] all = new String[length];
        for (int i = 0; i < length; i++) {
            if (tz.getRawOffset() == offset) {
                all[count++] = tz.getID();
            }
        }
        String[] answer = new String[count];
        System.arraycopy(all, 0, answer, 0, count);
        return answer;
    }

    /**
     * Gets the default time zone.
     *
     * @return the default time zone.
     */
    public static synchronized TimeZone getDefault() {
        if (Default == null) {
            setDefault(null);
        }
        return (TimeZone) Default.clone();
    }

    /**
     * Gets the LONG name for this {@code TimeZone} for the default {@code Locale} in standard
     * time. If the name is not available, the result is in the format
     * {@code GMT[+-]hh:mm}.
     *
     * @return the {@code TimeZone} name.
     */
    public final String getDisplayName() {
        return getDisplayName(false, LONG, Locale.getDefault());
    }

    /**
     * Gets the LONG name for this {@code TimeZone} for the specified {@code Locale} in standard
     * time. If the name is not available, the result is in the format
     * {@code GMT[+-]hh:mm}.
     *
     * @param locale
     *            the {@code Locale}.
     * @return the {@code TimeZone} name.
     */
    public final String getDisplayName(Locale locale) {
        return getDisplayName(false, LONG, locale);
    }

    /**
     * Gets the specified style of name ({@code LONG} or {@code SHORT}) for this {@code TimeZone} for
     * the default {@code Locale} in either standard or daylight time as specified. If
     * the name is not available, the result is in the format {@code GMT[+-]hh:mm}.
     *
     * @param daylightTime
     *            {@code true} for daylight time, {@code false} for standard
     *            time.
     * @param style
     *            either {@code LONG} or {@code SHORT}.
     * @return the {@code TimeZone} name.
     */
    public final String getDisplayName(boolean daylightTime, int style) {
        return getDisplayName(daylightTime, style, Locale.getDefault());
    }

    /**
     * Gets the specified style of name ({@code LONG} or {@code SHORT}) for this {@code TimeZone} for
     * the specified {@code Locale} in either standard or daylight time as specified. If
     * the name is not available, the result is in the format {@code GMT[+-]hh:mm}.
     *
     * @param daylightTime
     *            {@code true} for daylight time, {@code false} for standard
     *            time.
     * @param style
     *            either LONG or SHORT.
     * @param locale
     *            either {@code LONG} or {@code SHORT}.
     * @return the {@code TimeZone} name.
     */
    public String getDisplayName(boolean daylightTime, int style, Locale locale) {
        if(icuTimeZone == null || !ID.equals(icuTimeZone.getID())){
            icuTimeZone = com.ibm.icu.util.TimeZone.getTimeZone(ID);
        }
        return icuTimeZone.getDisplayName(
                daylightTime, style, locale);
    }

    /**
     * Gets the ID of this {@code TimeZone}.
     *
     * @return the time zone ID string.
     */
    public String getID() {
        return ID;
    }

    /**
     * Gets the daylight savings offset in milliseconds for this {@code TimeZone}.
     * <p>
     * This implementation returns 3600000 (1 hour), or 0 if the time zone does
     * not observe daylight savings.
     * <p>
     * Subclasses may override to return daylight savings values other than 1
     * hour.
     * <p>
     *
     * @return the daylight savings offset in milliseconds if this {@code TimeZone}
     *         observes daylight savings, zero otherwise.
     */
    public int getDSTSavings() {
        if (useDaylightTime()) {
            return 3600000;
        }
        return 0;
    }

    /**
     * Gets the offset from GMT of this {@code TimeZone} for the specified date. The
     * offset includes daylight savings time if the specified date is within the
     * daylight savings time period.
     *
     * @param time
     *            the date in milliseconds since January 1, 1970 00:00:00 GMT
     * @return the offset from GMT in milliseconds.
     */
    public int getOffset(long time) {
        if (inDaylightTime(new Date(time))) {
            return getRawOffset() + getDSTSavings();
        }
        return getRawOffset();
    }

    /**
     * Gets the offset from GMT of this {@code TimeZone} for the specified date and
     * time. The offset includes daylight savings time if the specified date and
     * time are within the daylight savings time period.
     *
     * @param era
     *            the {@code GregorianCalendar} era, either {@code GregorianCalendar.BC} or
     *            {@code GregorianCalendar.AD}.
     * @param year
     *            the year.
     * @param month
     *            the {@code Calendar} month.
     * @param day
     *            the day of the month.
     * @param dayOfWeek
     *            the {@code Calendar} day of the week.
     * @param time
     *            the time of day in milliseconds.
     * @return the offset from GMT in milliseconds.
     */
    abstract public int getOffset(int era, int year, int month, int day,
            int dayOfWeek, int time);

    /**
     * Gets the offset for standard time from GMT for this {@code TimeZone}.
     *
     * @return the offset from GMT in milliseconds.
     */
    abstract public int getRawOffset();

    /**
     * Gets the {@code TimeZone} with the specified ID.
     *
     * @param name
     *            a time zone string ID.
     * @return the {@code TimeZone} with the specified ID or null if no {@code TimeZone} with
     *         the specified ID exists.
     */
    public static synchronized TimeZone getTimeZone(String name) {
        if (AvailableZones == null) {
            initializeAvailable();
        }

        TimeZone zone = AvailableZones.get(name);
        if(zone == null && isAvailableIDInICU(name)){
            appendAvailableZones(name);
            zone = AvailableZones.get(name);
        }
        if (zone == null) {
            if (name.startsWith("GMT") && name.length() > 3) {
                char sign = name.charAt(3);
                if (sign == '+' || sign == '-') {
                    int[] position = new int[1];
                    String formattedName = formatTimeZoneName(name, 4);
                    int hour = parseNumber(formattedName, 4, position);
                    if (hour < 0 || hour > 23) {
                        return (TimeZone) GMT.clone();
                    }
                    int index = position[0];
                    if (index != -1) {
                        int raw = hour * 3600000;
                        if (index < formattedName.length()
                                && formattedName.charAt(index) == ':') {
                            int minute = parseNumber(formattedName, index + 1,
                                    position);
                            if (position[0] == -1 || minute < 0 || minute > 59) {
                                return (TimeZone) GMT.clone();
                            }
                            raw += minute * 60000;
                        } else if (hour >= 30 || index > 6) {
                            raw = (hour / 100 * 3600000) + (hour % 100 * 60000);
                        }
                        if (sign == '-') {
                            raw = -raw;
                        }
                        return new SimpleTimeZone(raw, formattedName);
                    }
                }
            }
            zone = GMT;
        }
        return (TimeZone) zone.clone();
    }

    private static String formatTimeZoneName(String name, int offset) {
        StringBuilder buf = new StringBuilder();
        int index = offset, length = name.length();
        buf.append(name.substring(0, offset));

        while (index < length) {
            if (Character.digit(name.charAt(index), 10) != -1) {
                buf.append(name.charAt(index));
                if ((length - (index + 1)) == 2) {
                    buf.append(':');
                }
            } else if (name.charAt(index) == ':') {
                buf.append(':');
            }
            index++;
        }

        if (buf.toString().indexOf(":") == -1) {
            buf.append(':');
            buf.append("00");
        }

        if (buf.toString().indexOf(":") == 5) {
            buf.insert(4, '0');
        }

        return buf.toString();
    }

    /**
     * Returns whether the specified {@code TimeZone} has the same raw offset as this
     * {@code TimeZone}.
     *
     * @param zone
     *            a {@code TimeZone}.
     * @return {@code true} when the {@code TimeZone} have the same raw offset, {@code false}
     *         otherwise.
     */
    public boolean hasSameRules(TimeZone zone) {
        if (zone == null) {
            return false;
        }
        return getRawOffset() == zone.getRawOffset();
    }

    /**
     * Returns whether the specified {@code Date} is in the daylight savings time period for
     * this {@code TimeZone}.
     *
     * @param time
     *            a {@code Date}.
     * @return {@code true} when the {@code Date} is in the daylight savings time period, {@code false}
     *         otherwise.
     */
    abstract public boolean inDaylightTime(Date time);

    private static int parseNumber(String string, int offset, int[] position) {
        int index = offset, length = string.length(), digit, result = 0;
        while (index < length
                && (digit = Character.digit(string.charAt(index), 10)) != -1) {
            index++;
            result = result * 10 + digit;
        }
        position[0] = index == offset ? -1 : index;
        return result;
    }

    /**
     * Sets the default time zone. If passed {@code null}, then the next
     * time {@link #getDefault} is called, the default time zone will be
     * determined. This behavior is slightly different than the canonical
     * description of this method, but it follows the spirit of it.
     *
     * @param timezone
     *            a {@code TimeZone} object.
     */
    public static synchronized void setDefault(TimeZone timezone) {
        if (timezone != null) {
            setICUDefaultTimeZone(timezone);
            Default = timezone;
            return;
        }

        String zone = AccessController.doPrivileged(new PriviAction<String>(
                "user.timezone"));

        // sometimes DRLVM incorrectly adds "\n" to the end of timezone ID
        if (zone != null && zone.contains("\n")) {
            zone = zone.substring(0, zone.indexOf("\n"));
        }

        // if property user.timezone is not set, we call the native method
        // getCustomTimeZone
        if (zone == null || zone.length() == 0) {
            int[] tzinfo = new int[10];
            boolean[] isCustomTimeZone = new boolean[1];

            String zoneId = getCustomTimeZone(tzinfo, isCustomTimeZone);

            // if returned TimeZone is a user customized TimeZone
            if (isCustomTimeZone[0]) {
                // build a new SimpleTimeZone
                switch (tzinfo[1]) {
                case 0:
                    // does not observe DST
                    Default = new SimpleTimeZone(tzinfo[0], zoneId);
                    break;
                default:
                    // observes DST
                    Default = new SimpleTimeZone(tzinfo[0], zoneId, tzinfo[5],
                            tzinfo[4], tzinfo[3], tzinfo[2], tzinfo[9],
                            tzinfo[8], tzinfo[7], tzinfo[6], tzinfo[1]);
                }
            } else {
                // get TimeZone
                Default = getTimeZone(zoneId);
            }
        } else {
            // if property user.timezone is set in command line (with -D option)
            Default = getTimeZone(zone);
        }
        setICUDefaultTimeZone(Default);
    }

    private static void setICUDefaultTimeZone(TimeZone timezone) {
        final com.ibm.icu.util.TimeZone icuTZ = com.ibm.icu.util.TimeZone
                .getTimeZone(timezone.getID());

        AccessController
                .doPrivileged(new PrivilegedAction<java.lang.reflect.Field>() {
                    public java.lang.reflect.Field run() {
                        java.lang.reflect.Field field = null;
                        try {
                            field = com.ibm.icu.util.TimeZone.class
                                    .getDeclaredField("defaultZone");
                            field.setAccessible(true);
                            field.set("defaultZone", icuTZ);
                        } catch (Exception e) {
                            return null;
                        }
                        return field;
                    }
                });
    }

    /**
     * Sets the ID of this {@code TimeZone}.
     *
     * @param name
     *            a string which is the time zone ID.
     */
    public void setID(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        ID = name;
    }

    /**
     * Sets the offset for standard time from GMT for this {@code TimeZone}.
     *
     * @param offset
     *            the offset from GMT in milliseconds.
     */
    abstract public void setRawOffset(int offset);

    /**
     * Returns whether this {@code TimeZone} has a daylight savings time period.
     *
     * @return {@code true} if this {@code TimeZone} has a daylight savings time period, {@code false}
     *         otherwise.
     */
    abstract public boolean useDaylightTime();

    /**
     * Gets the name and the details of the user-selected TimeZone on the
     * device.
     *
     * @param tzinfo
     *            int array of 10 elements to be filled with the TimeZone
     *            information. Once filled, the contents of the array are
     *            formatted as follows: tzinfo[0] -> the timezone offset;
     *            tzinfo[1] -> the dst adjustment; tzinfo[2] -> the dst start
     *            hour; tzinfo[3] -> the dst start day of week; tzinfo[4] -> the
     *            dst start week of month; tzinfo[5] -> the dst start month;
     *            tzinfo[6] -> the dst end hour; tzinfo[7] -> the dst end day of
     *            week; tzinfo[8] -> the dst end week of month; tzinfo[9] -> the
     *            dst end month;
     * @param isCustomTimeZone
     *            boolean array of size 1 that indicates if a timezone match is
     *            found
     * @return the name of the TimeZone or null if error occurs in native
     *         method.
     */
    private static native String getCustomTimeZone(int[] tzinfo,
            boolean[] isCustomTimeZone);
}
