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
package org.teavm.classlib.java.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTest;
import org.teavm.junit.TestPlatform;

@TeaVMTest
public class SimpleDateFormatTest {
    public SimpleDateFormatTest() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    @Test
    @SkipJVM
    public void firstDayOfWeekMatches() {
        assertEquals(Calendar.MONDAY, new GregorianCalendar(Locale.ENGLISH).getFirstDayOfWeek());
        assertEquals(1, new GregorianCalendar(Locale.ENGLISH).getMinimalDaysInFirstWeek());
    }

    @Test
    public void fieldsFormatted() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        assertEquals("2014-06-24 09:33:49", format.format(getDateWithZoneOffset(1403602429504L)));
    }

    @Test
    public void fieldsParsed() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("2014-06-24 09:33:49")));
    }

    @Test
    public void eraHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("G yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        assertEquals("AD 2014-06-24 09:33:49", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("AD 2014-06-24 09:33:49")));
    }

    @Test
    public void shortYearHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yy-MM-dd HH:mm:ss", Locale.ENGLISH);
        assertEquals("14-06-24 09:33:49", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("14-06-24 09:33:49")));
    }

    @Test
    @SkipJVM
    public void weekInYearHandled() throws ParseException {
        long day = 24 * 3600 * 1000;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss www", Locale.ENGLISH);
        assertEquals("2014-06-24 09:33:49 026", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals("2014-06-28 09:33:49 026", format.format(getDateWithZoneOffset(1403602429504L + day * 4)));
        assertEquals("2014-06-29 09:33:49 026", format.format(getDateWithZoneOffset(1403602429504L + day * 5)));
        assertEquals("2014-06-30 09:33:49 027", format.format(getDateWithZoneOffset(1403602429504L + day * 6)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("2014-06-24 09:33:49 026")));
        assertEquals(1403602429000L + day * 4, getTimeWithoutZoneOffset(format.parse("2014-06-28 09:33:49 026")));
        assertEquals(1403602429000L + day * 5, getTimeWithoutZoneOffset(format.parse("2014-06-29 09:33:49 026")));
        assertEquals(1403602429000L + day * 6, getTimeWithoutZoneOffset(format.parse("2014-06-30 09:33:49 027")));
    }

    @Test
    @SkipJVM
    public void weekInMonthHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss WW", Locale.ENGLISH);
        assertEquals("2014-06-24 09:33:49 05", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("2014-06-24 09:33:49 05")));
    }

    @Test
    public void dayInYearHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss DD", Locale.ENGLISH);
        assertEquals("2014-06-24 09:33:49 175", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("2014-06-24 09:33:49 175")));
    }

    @Test
    public void weekdayInMonthHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss F", Locale.ENGLISH);
        assertEquals("2014-06-24 09:33:49 4", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("2014-06-24 09:33:49 4")));
    }

    @Test
    public void shortWeekdayHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("E yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        assertEquals("Tue 2014-06-24 09:33:49", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("Tue 2014-06-24 09:33:49")));
    }

    @Test
    public void longWeekdayHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("EEEE, yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        assertEquals("Tuesday, 2014-06-24 09:33:49", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("Tuesday, 2014-06-24 09:33:49")));
    }

    @Test
    public void numericWeekdayHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("u yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        assertEquals("2 2014-06-24 09:33:49", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("2 2014-06-24 09:33:49")));
    }

    @Test
    public void amPmHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd 'at' hh:mm:ss a", Locale.ENGLISH);
        assertEquals("2014-06-24 at 09:33:49 AM", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("2014-06-24 at 09:33:49 AM")));
    }

    @Test
    public void shortMonthHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("MMM, dd yyyy HH:mm:ss", Locale.ENGLISH);
        assertEquals("Jun, 24 2014 09:33:49", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("Jun, 24 2014 09:33:49")));
    }

    @Test
    public void longMonthHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("MMMM, dd yyyy HH:mm:ss", Locale.ENGLISH);
        assertEquals("June, 24 2014 09:33:49", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("June, 24 2014 09:33:49")));
    }

    private Date getDateWithZoneOffset(long milliseconds) {
        Calendar calendar = new GregorianCalendar(Locale.ENGLISH);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        calendar.setTimeInMillis(milliseconds);
        return new Date(milliseconds);
    }

    private long getTimeWithoutZoneOffset(Date date) {
        Calendar calendar = new GregorianCalendar(Locale.ENGLISH);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        calendar.setTime(date);
        return calendar.getTimeInMillis();
    }

    @Test
    public void fieldsParsedWithoutDelimiters() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmm", Locale.ENGLISH);
        assertEquals(1403602380000L, getTimeWithoutZoneOffset(format.parse("1406240933")));
    }

    @Test
    public void numericGeneralTimeZoneParsed() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH);
        assertEquals(1403602429000L, parseFully(format, "2014-06-24 09:33:49 GMT+00:00"));
        assertEquals(1403602429000L - 2 * 3600_000L, parseFully(format, "2014-06-24 09:33:49 GMT+02:00"));
        assertEquals(1403602429000L - 1800_000L, parseFully(format, "2014-06-24 09:33:49 GMT+00:30"));
        assertEquals(1403602429000L + 5 * 3600_000L + 1800_000L,
                parseFully(format, "2014-06-24 09:33:49 GMT-05:30"));
        assertEquals(1403602429000L - 3600_000L, parseFully(format, "2014-06-24 09:33:49 GMT+1:00"));
    }

    @Test
    public void rfc822TimeZoneParsed() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ENGLISH);
        assertEquals(1403602429000L - 5400_000L, parseFully(format, "2014-06-24 09:33:49 +0130"));
        assertEquals(1403602429000L + 1800_000L, parseFully(format, "2014-06-24 09:33:49 -0030"));
    }

    @Test
    public void iso8601TimeZoneKeepsSignOfZeroHours() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss XXX", Locale.ENGLISH);
        assertEquals(1403602429000L - 1800_000L, parseFully(format, "2014-06-24 09:33:49 +00:30"));
    }

    // The SimpleDateFormat Javadoc gives the general time zone grammar as "GMTOffsetTimeZone: GMT Sign
    // Hours : Minutes" and says that pattern letter Z also accepts general time zones when parsing.
    // It doesn't say whether "GMT" is matched with regard to case. OpenJDK ignores case, and so does
    // TeaVM's zone name lookup, so "gmt+05:30" must not stop at "gmt" and lose the offset.
    @Test
    public void gmtOffsetParsedInAnyCase() {
        String[] offsets = { "05:30", "1:00", "12:00", "00:30", "0:00" };
        long[] offsetMillis = { 5 * 3600_000L + 1800_000L, 3600_000L, 12 * 3600_000L, 1800_000L, 0 };
        for (String pattern : new String[] { "yyyy-MM-dd HH:mm:ss z", "yyyy-MM-dd HH:mm:ss Z" }) {
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
            for (int mask = 0; mask < 8; ++mask) {
                StringBuilder gmt = new StringBuilder();
                for (int i = 0; i < 3; ++i) {
                    char c = "GMT".charAt(i);
                    gmt.append((mask & (1 << i)) != 0 ? Character.toLowerCase(c) : c);
                }
                for (int i = 0; i < offsets.length; ++i) {
                    assertEquals(1403602429000L - offsetMillis[i],
                            parseFully(format, "2014-06-24 09:33:49 " + gmt + "+" + offsets[i]));
                    assertEquals(1403602429000L + offsetMillis[i],
                            parseFully(format, "2014-06-24 09:33:49 " + gmt + "-" + offsets[i]));
                }
            }
        }
    }

    @Test
    public void numericZoneWithoutGmtPrefixRejected() {
        for (String pattern : new String[] { "yyyy-MM-dd HH:mm:ss z", "yyyy-MM-dd HH:mm:ss Z" }) {
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
            for (String zone : new String[] { "GMX+05:30", "gmx+05:30", "GNT+05:30", "gm+05:30", "hmt+05:30" }) {
                ParsePosition position = new ParsePosition(0);
                String text = "2014-06-24 09:33:49 " + zone;
                assertNull(format.parse(text, position), text);
                assertEquals(20, position.getErrorIndex(), text);
            }
        }
    }

    @Test
    public void numericGeneralTimeZoneHoursOver23Rejected() {
        assertNotParsed("yyyy-MM-dd HH:mm:ss z", "2014-06-24 09:33:49 GMT+24:00");
    }

    @Test
    public void numericGeneralTimeZoneMinutesOver59Rejected() {
        assertNotParsed("yyyy-MM-dd HH:mm:ss z", "2014-06-24 09:33:49 GMT+05:60");
    }

    @Test
    public void rfc822TimeZoneHoursOver23Rejected() {
        assertNotParsed("yyyy-MM-dd HH:mm:ss Z", "2014-06-24 09:33:49 +2400");
    }

    @Test
    public void rfc822TimeZoneMinutesOver59Rejected() {
        assertNotParsed("yyyy-MM-dd HH:mm:ss Z", "2014-06-24 09:33:49 +0560");
    }

    @Test
    public void iso8601TimeZoneHoursOver23Rejected() {
        assertNotParsed("yyyy-MM-dd HH:mm:ss XXX", "2014-06-24 09:33:49 +24:00");
    }

    @Test
    public void iso8601TimeZoneMinutesOver59Rejected() {
        assertNotParsed("yyyy-MM-dd HH:mm:ss XXX", "2014-06-24 09:33:49 +05:60");
    }

    @Test
    public void largestNumericTimeZoneParsed() {
        long expected = 1403602429000L - 23 * 3600_000L - 59 * 60_000L;
        assertEquals(expected, parseFully(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH),
                "2014-06-24 09:33:49 GMT+23:59"));
        assertEquals(expected, parseFully(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ENGLISH),
                "2014-06-24 09:33:49 +2359"));
        assertEquals(expected, parseFully(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss XXX", Locale.ENGLISH),
                "2014-06-24 09:33:49 +23:59"));
    }

    // Exhaustive sweep over two-digit hours and minutes for every numeric zone form. The JDK's
    // SimpleDateFormat accepts a numeric zone only when hours <= 23 and minutes <= 59, and then the
    // offset is sign * (hours * 60 + minutes) minutes.
    // Skipped on C: this many parses (each clones a Calendar and its TimeZone) makes the C runtime's GC
    // hang in markFromOldGeneration or crash with SIGSEGV. That happens with or without the range checks
    // under test, so it is a separate C GC bug; the targeted tests above still run on C.
    @Test
    @SkipPlatform(TestPlatform.C)
    public void numericTimeZoneRangeSweep() {
        // One format per pattern, and messages built only on failure, to keep allocation low.
        var sweep = new ZoneSweep();
        for (int hours = 0; hours <= 99; ++hours) {
            for (int minutes : new int[] { 0, 1, 59, 60, 99 }) {
                sweep.checkAllForms(hours, minutes);
            }
        }
        for (int minutes = 0; minutes <= 99; ++minutes) {
            for (int hours : new int[] { 0, 1, 23, 24, 99 }) {
                sweep.checkAllForms(hours, minutes);
            }
        }
        assertEquals(0, sweep.failures, sweep.firstFailure);
    }

    private static class ZoneSweep {
        private static final String PREFIX = "2014-06-24 09:33:49 ";
        private final SimpleDateFormat general = format("z");
        private final SimpleDateFormat rfc822 = format("Z");
        private final SimpleDateFormat iso1 = format("X");
        private final SimpleDateFormat iso2 = format("XX");
        private final SimpleDateFormat iso3 = format("XXX");
        private final ParsePosition position = new ParsePosition(0);
        int failures;
        String firstFailure;

        private static SimpleDateFormat format(String zonePattern) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss " + zonePattern, Locale.ENGLISH);
        }

        void checkAllForms(int hours, int minutes) {
            String hh = twoDigits(hours);
            String mm = twoDigits(minutes);
            for (char sign : new char[] { '+', '-' }) {
                check(general, "GMT" + sign + hh + ":" + mm, sign, hours, minutes);
                if (hours <= 9) {
                    check(general, "GMT" + sign + hours + ":" + mm, sign, hours, minutes);
                }
                check(rfc822, sign + hh + mm, sign, hours, minutes);
                check(iso1, sign + hh, sign, hours, 0);
                check(iso2, sign + hh + mm, sign, hours, minutes);
                check(iso3, sign + hh + ":" + mm, sign, hours, minutes);
            }
        }

        private void check(SimpleDateFormat format, String zone, char sign, int hours, int minutes) {
            String text = PREFIX + zone;
            position.setIndex(0);
            position.setErrorIndex(-1);
            Date date = format.parse(text, position);
            boolean ok;
            if (hours <= 23 && minutes <= 59) {
                long offset = (sign == '-' ? -1 : 1) * (hours * 3600_000L + minutes * 60_000L);
                ok = date != null && position.getErrorIndex() == -1 && position.getIndex() == text.length()
                        && date.getTime() == 1403602429000L - offset;
            } else {
                ok = date == null && position.getErrorIndex() >= 0;
            }
            if (!ok && failures++ == 0) {
                firstFailure = format.toPattern() + " / " + text + ": date=" + date
                        + ", errorIndex=" + position.getErrorIndex() + ", index=" + position.getIndex();
            }
        }
    }

    // DateFormat.parse(String, ParsePosition): "If an error occurs, then ... the error index of pos is set
    // to the index of the character where the error occurred, and null is returned." Parsing must therefore
    // never throw, whatever prefix of a valid text it is given.
    @Test
    public void truncatedNumericTimeZoneNeverThrows() {
        String[][] cases = {
                { "z", "GMT+05:30" }, { "Z", "+0530" }, { "X", "+05" }, { "XX", "+0530" }, { "XXX", "+05:30" },
                { "XXX", "Z" }
        };
        for (String[] c : cases) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss " + c[0], Locale.ENGLISH);
            String full = "2014-06-24 09:33:49 " + c[1];
            for (int length = "2014-06-24 09:33:49".length(); length <= full.length(); ++length) {
                String text = full.substring(0, length);
                ParsePosition position = new ParsePosition(0);
                Date date = format.parse(text, position);
                if (date == null) {
                    assertTrue(position.getErrorIndex() >= 0, c[0] + " / " + text);
                }
            }
        }
    }

    static String twoDigits(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private void assertNotParsed(String pattern, String text) {
        ParsePosition position = new ParsePosition(0);
        assertNull(new SimpleDateFormat(pattern, Locale.ENGLISH).parse(text, position), text);
        assertTrue(position.getErrorIndex() >= 0, text);
    }

    @Test
    public void gmtFollowedByTextParsed() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH);
        ParsePosition position = new ParsePosition(0);
        Date date = format.parse("2014-06-24 09:33:49 GMT rest", position);
        assertEquals(-1, position.getErrorIndex());
        assertEquals(23, position.getIndex());
        assertEquals(1403602429000L, date.getTime());
    }

    @Test
    public void gmtBeforeOtherFieldsParsed() {
        assertEquals(1403602429000L, parseFully(new SimpleDateFormat("z yyyy-MM-dd HH:mm:ss", Locale.ENGLISH),
                "GMT 2014-06-24 09:33:49"));
    }

    @Test
    public void gmtBeforeLiteralTextParsed() {
        assertEquals(1403602429000L, parseFully(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z '(x)'", Locale.ENGLISH),
                "2014-06-24 09:33:49 GMT (x)"));
    }

    // TeaVM looks a zone up first by its display name and then by its ID (the JDK parses display names
    // only, so this test doesn't run on the JVM). Each lookup takes the longest name that the text starts
    // with, ignoring case. This checks that against a brute-force scan, for every display name and every
    // ID, in two cases, followed by text that may or may not continue a longer name.
    // Skipped on C: this many parses make the C runtime's GC hang in markFromOldGeneration or crash with
    // SIGSEGV. 30000 parses of "... Europe/Paris" crash the same way without this PR's change, so it is a
    // separate C GC bug (see numericTimeZoneRangeSweep); the targeted GMT tests above still run on C.
    @Test
    @SkipJVM
    @SkipPlatform(TestPlatform.C)
    public void everyZoneNameParsedUpToLongestMatch() {
        String[] ids = TimeZone.getAvailableIDs();
        String[] displayNames = new String[ids.length];
        long[] times = new long[ids.length];
        Calendar calendar = new GregorianCalendar(Locale.ENGLISH);
        for (int i = 0; i < ids.length; ++i) {
            TimeZone tz = TimeZone.getTimeZone(ids[i]);
            displayNames[i] = tz.getDisplayName(Locale.ENGLISH);
            calendar.clear();
            calendar.setTimeZone(tz);
            calendar.set(2014, Calendar.JUNE, 24, 9, 33, 49);
            times[i] = calendar.getTimeInMillis();
        }
        String[] suffixes = { "", " rest", "(", ",", "/", "_", "-", "+", "0", "1", "x", "\t" };
        String prefix = "2014-06-24 09:33:49 ";
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH);
        int checked = 0;
        Set<String> distinctNames = new LinkedHashSet<>(Arrays.asList(displayNames));
        distinctNames.addAll(Arrays.asList(ids));
        for (String name : distinctNames) {
            // GMT+h and GMT-h are read as numeric zones, which need minutes.
            if (name.regionMatches(true, 0, "GMT+", 0, 4) || name.regionMatches(true, 0, "GMT-", 0, 4)) {
                continue;
            }
            for (String zone : new String[] { name, name.toLowerCase(Locale.ROOT) }) {
                for (String suffix : suffixes) {
                    String text = prefix + zone + suffix;
                    String[] matchedNames = displayNames;
                    int length = longestMatch(displayNames, text, prefix.length());
                    if (length < 0) {
                        matchedNames = ids;
                        length = longestMatch(ids, text, prefix.length());
                    }
                    ParsePosition position = new ParsePosition(0);
                    Date date = format.parse(text, position);
                    assertEquals(-1, position.getErrorIndex(), text);
                    assertEquals(prefix.length() + length, position.getIndex(), text);
                    boolean found = false;
                    for (int i = 0; i < ids.length && !found; ++i) {
                        if (matchedNames[i].length() == length
                                && text.regionMatches(true, prefix.length(), matchedNames[i], 0, length)) {
                            found = date.getTime() == times[i];
                        }
                    }
                    assertTrue(found, text);
                    ++checked;
                }
            }
        }
        assertTrue(checked > 5000, "checked " + checked);
    }

    private static int longestMatch(String[] names, String text, int start) {
        int longest = -1;
        for (String name : names) {
            if (name.length() > longest && text.regionMatches(true, start, name, 0, name.length())) {
                longest = name.length();
            }
        }
        return longest;
    }

    // Known gap: TeaVM also accepts zone IDs, and GMT0 is one. So "GMT" followed by "0" matches GMT0,
    // while the JDK takes "GMT" alone and leaves "0" for the next field.
    @Test
    @Disabled("TeaVM matches the zone ID GMT0 where the JDK takes GMT and leaves the digit")
    public void gmtFollowedByDigitLeavesDigit() {
        assertEquals(1403602429000L, parseFully(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z'0'", Locale.ENGLISH),
                "2014-06-24 09:33:49 GMT0"));
    }

    private long parseFully(SimpleDateFormat format, String text) {
        ParsePosition position = new ParsePosition(0);
        Date date = format.parse(text, position);
        assertEquals(-1, position.getErrorIndex(), text);
        assertEquals(text.length(), position.getIndex(), text);
        return date.getTime();
    }
}
