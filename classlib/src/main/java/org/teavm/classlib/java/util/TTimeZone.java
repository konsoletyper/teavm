/*
 *  Copyright 2015 Alexey Andreev.
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

import java.io.Serializable;
import java.util.Arrays;
import org.teavm.classlib.impl.tz.DateTimeZone;
import org.teavm.classlib.impl.tz.DateTimeZoneProvider;
import org.teavm.classlib.impl.tz.FixedDateTimeZone;
import org.teavm.classlib.impl.unicode.CLDRHelper;

/**
 * {@code TimeZone} represents a time zone offset, taking into account
 * daylight savings.
 * <p>
 * Typically, you get a {@code TimeZone} using {@code getDefault}
 * which creates a {@code TimeZone} based on the time zone where the
 * program is running. For example, for a program running in Japan,
 * {@code getDefault} creates a {@code TimeZone} object based on
 * Japanese Standard Time.
 * <p>
 * You can also get a {@code TimeZone} using {@code getTimeZone}
 * along with a time zone ID. For instance, the time zone ID for the U.S.
 * Pacific Time zone is "America/Los_Angeles". So, you can get a U.S. Pacific
 * Time {@code TimeZone} object with the following: <blockquote>
 *
 * <pre>
 * TimeZone tz = TimeZone.getTimeZone(&quot;America/Los_Angeles&quot;);
 * </pre>
 *
 * </blockquote> You can use the {@code getAvailableIDs} method to iterate
 * through all the supported time zone IDs. You can then choose a supported ID
 * to get a {@code TimeZone}. If the time zone you want is not
 * represented by one of the supported IDs, then you can create a custom time
 * zone ID with the following syntax: <blockquote>
 *
 * <pre>
 * GMT[+|-]hh[[:]mm]
 * </pre>
 *
 * </blockquote> For example, you might specify GMT+14:00 as a custom time zone
 * ID. The {@code TimeZone} that is returned when you specify a custom
 * time zone ID does not include daylight savings time.
 * <p>
 * For compatibility with JDK 1.1.x, some other three-letter time zone IDs (such
 * as "PST", "CTT", "AST") are also supported. However, <strong>their use is
 * deprecated</strong> because the same abbreviation is often used for multiple
 * time zones (for example, "CST" could be U.S. "Central Standard Time" and
 * "China Standard Time"), and the Java platform can then only recognize one of
 * them.
 * <p>
 * Please note the type returned by factory methods, i.e. {@code getDefault()}
 * and {@code getTimeZone(String)}, is implementation dependent, so it may
 * introduce serialization incompatibility issues between different
 * implementations.
 */
public abstract class TTimeZone implements Serializable, Cloneable {
    private static final long serialVersionUID = 3581463369166924961L;

    /**
     * The SHORT display name style.
     */
    public static final int SHORT = 0;

    /**
     * The LONG display name style.
     */
    public static final int LONG = 1;

    private static TTimeZone defaultTz;

    static final TTimeZone GMT = new TIANATimeZone(new FixedDateTimeZone("GMT", 0, 0));

    private String id;

    /**
     * Constructs a new instance of this class.
     */
    public TTimeZone() {
    }

    TTimeZone(String id) {
        this.id = id;
    }

    /**
     * Returns a new {@code TimeZone} with the same ID, {@code rawOffset} and daylight savings
     * time rules as this {@code TimeZone}.
     *
     * @return a shallow copy of this {@code TimeZone}.
     * @see java.lang.Cloneable
     */
    @Override
    public Object clone() {
        try {
            TTimeZone zone = (TTimeZone) super.clone();
            return zone;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /**
     * Gets the available time zone IDs. Any one of these IDs can be passed to
     * {@code get()} to create the corresponding {@code TimeZone} instance.
     *
     * @return an array of time zone ID strings.
     */
    public static String[] getAvailableIDs() {
        return DateTimeZoneProvider.getIds();
    }

    /**
     * Gets the available time zone IDs which match the specified offset from
     * GMT. Any one of these IDs can be passed to {@code get()} to create the corresponding
     * {@code TimeZone} instance.
     *
     * @param offset
     *            the offset from GMT in milliseconds.
     * @return an array of time zone ID strings.
     */
    public static String[] getAvailableIDs(int offset) {
        String[] all = DateTimeZoneProvider.getIds();
        String[] result = new String[all.length];
        int i = 0;
        for (String id : all) {
            DateTimeZone jodaTz = DateTimeZoneProvider.getTimeZone(id);
            if (jodaTz.getStandardOffset(System.currentTimeMillis()) == offset) {
                result[i++] = id;
            }
        }
        return Arrays.copyOf(result, i);
    }

    /**
     * Gets the default time zone.
     *
     * @return the default time zone.
     */
    public static TTimeZone getDefault() {
        if (defaultTz == null) {
            DateTimeZone innerTz = DateTimeZoneProvider.detectTimezone();
            if (innerTz != null) {
                defaultTz = new TIANATimeZone(innerTz);
            } else {
                defaultTz = TTimeZone.getTimeZone("UTC");
            }
        }
        return (TTimeZone) defaultTz.clone();
    }

    /**
     * Gets the LONG name for this {@code TimeZone} for the default {@code Locale} in standard
     * time. If the name is not available, the result is in the format
     * {@code GMT[+-]hh:mm}.
     *
     * @return the {@code TimeZone} name.
     */
    public final String getDisplayName() {
        return getDisplayName(false, LONG, TLocale.getDefault());
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
    public final String getDisplayName(TLocale locale) {
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
        return getDisplayName(daylightTime, style, TLocale.getDefault());
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
    public String getDisplayName(boolean daylightTime, int style, TLocale locale) {
        String name = CLDRHelper.getTimeZoneName(locale.getLanguage(), locale.getCountry(), id);
        if (name == null) {
            name = id;
        }
        return name;
    }

    /**
     * Gets the ID of this {@code TimeZone}.
     *
     * @return the time zone ID string.
     */
    public String getID() {
        return id;
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
        return inDaylightTime(new TDate(time)) ? getRawOffset() + getDSTSavings() : getRawOffset();
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
    public static TTimeZone getTimeZone(String name) {
        DateTimeZone jodaZone = DateTimeZoneProvider.getTimeZone(name);
        if (jodaZone != null) {
            return new TIANATimeZone(jodaZone);
        }
        if (name.startsWith("GMT") && name.length() > 3) {
            char sign = name.charAt(3);
            if (sign == '+' || sign == '-') {
                int[] position = new int[1];
                String formattedName = formatTimeZoneName(name, 4);
                int hour = parseNumber(formattedName, 4, position);
                if (hour < 0 || hour > 23) {
                    return (TTimeZone) GMT.clone();
                }
                int index = position[0];
                if (index != -1) {
                    int raw = hour * 3600000;
                    if (index < formattedName.length()
                            && formattedName.charAt(index) == ':') {
                        int minute = parseNumber(formattedName, index + 1,
                                position);
                        if (position[0] == -1 || minute < 0 || minute > 59) {
                            return (TTimeZone) GMT.clone();
                        }
                        raw += minute * 60000;
                    } else if (hour >= 30 || index > 6) {
                        raw = (hour / 100 * 3600000) + (hour % 100 * 60000);
                    }
                    if (sign == '-') {
                        raw = -raw;
                    }
                    return new TIANATimeZone(new FixedDateTimeZone(formattedName, raw, raw));
                }
            }
        }

        return (TTimeZone) GMT.clone();
    }

    private static String formatTimeZoneName(String name, int offset) {
        StringBuilder buf = new StringBuilder();
        int index = offset;
        int length = name.length();
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
    public boolean hasSameRules(TTimeZone zone) {
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
    abstract public boolean inDaylightTime(TDate time);

    private static int parseNumber(String string, int offset, int[] position) {
        int index = offset;
        int length = string.length();
        int digit;
        int result = 0;
        while (index < length) {
            digit = Character.digit(string.charAt(index), 10);
            if (digit == -1) {
                break;
            }
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
    public static void setDefault(TTimeZone timezone) {
        defaultTz = timezone != null ? (TTimeZone) timezone.clone() : null;
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
        id = name;
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != TTimeZone.class) {
            return false;
        }
        TTimeZone other = (TTimeZone) obj;
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
