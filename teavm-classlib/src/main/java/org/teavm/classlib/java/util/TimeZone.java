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

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TCloneable;

public abstract class TimeZone implements TSerializable, TCloneable {
    private static final long serialVersionUID = 3581463369166924961L;

    public static final int SHORT = 0;

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
        TList<String> result = new TArrayList<>();
        for (TIterator<TimeZone> iter = AvailableZones.values().iterator(); iter.hasNext();) {
            TimeZone tz = iter.next();
            if (tz.getRawOffset() == offset) {
                result.add(tz.getID());
            }
        }
        return result.toArray(new String[0]);
    }

    public static synchronized TimeZone getDefault() {
        if (Default == null) {
            setDefault(null);
        }
        return (TimeZone) Default.clone();
    }

    public final String getDisplayName() {
        return getDisplayName(false, LONG, TLocale.getDefault());
    }

    public final String getDisplayName(TLocale locale) {
        return getDisplayName(false, LONG, locale);
    }

    public final String getDisplayName(boolean daylightTime, int style) {
        return getDisplayName(daylightTime, style, TLocale.getDefault());
    }

    public String getDisplayName(boolean daylightTime, int style, TLocale locale) {
        if (icuTimeZone == null || !ID.equals(icuTimeZone.getID())) {
            icuTimeZone = com.ibm.icu.util.TimeZone.getTimeZone(ID);
        }
        return icuTimeZone.getDisplayName(daylightTime, style, locale);
    }

    public String getID() {
        return ID;
    }

    public int getDSTSavings() {
        if (useDaylightTime()) {
            return 3600000;
        }
        return 0;
    }

    public int getOffset(long time) {
        if (inDaylightTime(new Date(time))) {
            return getRawOffset() + getDSTSavings();
        }
        return getRawOffset();
    }

    abstract public int getOffset(int era, int year, int month, int day, int dayOfWeek, int time);

    abstract public int getRawOffset();

    public static synchronized TimeZone getTimeZone(String name) {
        if (AvailableZones == null) {
            initializeAvailable();
        }

        TimeZone zone = AvailableZones.get(name);
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
                        if (index < formattedName.length() && formattedName.charAt(index) == ':') {
                            int minute = parseNumber(formattedName, index + 1, position);
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

    public boolean hasSameRules(TimeZone zone) {
        if (zone == null) {
            return false;
        }
        return getRawOffset() == zone.getRawOffset();
    }

    abstract public boolean inDaylightTime(Date time);

    private static int parseNumber(String string, int offset, int[] position) {
        int index = offset, length = string.length(), digit, result = 0;
        while (index < length && (digit = Character.digit(string.charAt(index), 10)) != -1) {
            index++;
            result = result * 10 + digit;
        }
        position[0] = index == offset ? -1 : index;
        return result;
    }

    public static synchronized void setDefault(TimeZone timezone) {
        if (timezone != null) {
            setICUDefaultTimeZone(timezone);
            Default = timezone;
            return;
        }

        String zone = AccessController.doPrivileged(new PriviAction<String>("user.timezone"));

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
                        Default = new SimpleTimeZone(tzinfo[0], zoneId, tzinfo[5], tzinfo[4], tzinfo[3], tzinfo[2],
                                tzinfo[9], tzinfo[8], tzinfo[7], tzinfo[6], tzinfo[1]);
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
        final com.ibm.icu.util.TimeZone icuTZ = com.ibm.icu.util.TimeZone.getTimeZone(timezone.getID());

        AccessController.doPrivileged(new PrivilegedAction<java.lang.reflect.Field>() {
            public java.lang.reflect.Field run() {
                java.lang.reflect.Field field = null;
                try {
                    field = com.ibm.icu.util.TimeZone.class.getDeclaredField("defaultZone");
                    field.setAccessible(true);
                    field.set("defaultZone", icuTZ);
                } catch (Exception e) {
                    return null;
                }
                return field;
            }
        });
    }

    public void setID(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        ID = name;
    }

    abstract public void setRawOffset(int offset);

    abstract public boolean useDaylightTime();

    private static native String getCustomTimeZone(int[] tzinfo, boolean[] isCustomTimeZone);
}
