/*
 *  Copyright 2001-2013 Stephen Colebourne
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
package org.teavm.classlib.impl.tz;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeField;
import org.joda.time.LocalDate;
import org.joda.time.MutableDateTime;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.chrono.LenientChronology;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class ZoneInfoCompiler {
    static DateTimeOfYear cStartOfYear;

    static Chronology cLenientISO;

    static DateTimeOfYear getStartOfYear() {
        if (cStartOfYear == null) {
            cStartOfYear = new DateTimeOfYear();
        }
        return cStartOfYear;
    }

    static Chronology getLenientISOChronology() {
        if (cLenientISO == null) {
            cLenientISO = LenientChronology.getInstance(ISOChronology.getInstanceUTC());
        }
        return cLenientISO;
    }

    static int parseYear(String str, int def) {
        str = str.toLowerCase();
        switch (str) {
            case "minimum":
            case "min":
                return Integer.MIN_VALUE;
            case "maximum":
            case "max":
                return Integer.MAX_VALUE;
            case "only":
                return def;
        }
        return Integer.parseInt(str);
    }

    static int parseMonth(String str) {
        DateTimeField field = ISOChronology.getInstanceUTC().monthOfYear();
        return field.get(field.set(0, str, Locale.ENGLISH));
    }

    static int parseDayOfWeek(String str) {
        DateTimeField field = ISOChronology.getInstanceUTC().dayOfWeek();
        return field.get(field.set(0, str, Locale.ENGLISH));
    }

    static String parseOptional(String str) {
        return (str.equals("-")) ? null : str;
    }

    static int parseTime(String str) {
        DateTimeFormatter p = ISODateTimeFormat.hourMinuteSecondFraction();
        MutableDateTime mdt = new MutableDateTime(0, getLenientISOChronology());
        int pos = 0;
        if (str.startsWith("-")) {
            pos = 1;
        }
        int newPos = p.parseInto(mdt, str, pos);
        if (newPos == ~pos) {
            throw new IllegalArgumentException(str);
        }
        int millis = (int) mdt.getMillis();
        if (pos == 1) {
            millis = -millis;
        }
        return millis;
    }

    static char parseZoneChar(char c) {
        switch (c) {
        case 's': case 'S':
            // Standard time
            return 's';
        case 'u': case 'U': case 'g': case 'G': case 'z': case 'Z':
            // UTC
            return 'u';
        case 'w': case 'W': default:
            // Wall time
            return 'w';
        }
    }

    /**
     * @return false if error.
     */
    static boolean test(String id, DateTimeZone tz) {
        if (!id.equals(tz.getID())) {
            return true;
        }

        // Test to ensure that reported transitions are not duplicated.

        long millis = ISOChronology.getInstanceUTC().year().set(0, 1850);
        long end = ISOChronology.getInstanceUTC().year().set(0, 2050);

        int offset = tz.getOffset(millis);
        int stdOffset = tz.getStandardOffset(millis);

        List<Long> transitions = new ArrayList<>();

        while (true) {
            long next = tz.nextTransition(millis);
            if (next == millis || next > end) {
                break;
            }

            millis = next;

            int nextOffset = tz.getOffset(millis);
            int nextStdOffset = tz.getStandardOffset(millis);

            if (offset == nextOffset && stdOffset == nextStdOffset) {
                System.out.println("*d* Error in " + tz.getID() + " "
                        + new DateTime(millis, ISOChronology.getInstanceUTC()));
                return false;
            }

            transitions.add(millis);

            offset = nextOffset;
            stdOffset = nextStdOffset;
        }

        // Now verify that reverse transitions match up.

        millis = ISOChronology.getInstanceUTC().year().set(0, 2050);
        end = ISOChronology.getInstanceUTC().year().set(0, 1850);

        for (int i = transitions.size(); --i >= 0;) {
            long prev = tz.previousTransition(millis);
            if (prev == millis || prev < end) {
                break;
            }

            millis = prev;

            long trans = transitions.get(i);

            if (trans - 1 != millis) {
                System.out.println("*r* Error in " + tz.getID() + " "
                        + new DateTime(millis, ISOChronology.getInstanceUTC()) + " != "
                        + new DateTime(trans - 1, ISOChronology.getInstanceUTC()));

                return false;
            }
        }

        return true;
    }

    // Maps names to RuleSets.
    private Map<String, RuleSet> iRuleSets;

    // List of Zone objects.
    private List<Zone> iZones;

    // List String pairs to link.
    private List<String> iGoodLinks;

    // List String pairs to link.
    private List<String> iBackLinks;

    public ZoneInfoCompiler() {
        iRuleSets = new HashMap<>();
        iZones = new ArrayList<>();
        iGoodLinks = new ArrayList<>();
        iBackLinks = new ArrayList<>();
    }

    public Map<String, StorableDateTimeZone> compile() {
        Map<String, StorableDateTimeZone> map = new TreeMap<>();
        Map<String, Zone> sourceMap = new TreeMap<>();

        // write out the standard entries
        for (Zone zone : iZones) {
            DateTimeZoneBuilder builder = new DateTimeZoneBuilder();
            zone.addToBuilder(builder, iRuleSets);
            StorableDateTimeZone tz = new DateTimeZoneBuilder.RuleBasedZone(zone.iName, builder);
            if (test(tz.getID(), tz)) {
                map.put(tz.getID(), tz);
                sourceMap.put(tz.getID(), zone);
            }
        }

        // revive zones from "good" links
        for (int i = 0; i < iGoodLinks.size(); i += 2) {
            String baseId = iGoodLinks.get(i);
            String alias = iGoodLinks.get(i + 1);
            Zone sourceZone = sourceMap.get(baseId);
            if (sourceZone == null) {
                throw new RuntimeException("Cannot find source zone '" + baseId + "' to link alias '"
                        + alias + "' to");
            } else {
                DateTimeZoneBuilder builder = new DateTimeZoneBuilder();
                sourceZone.addToBuilder(builder, iRuleSets);
                StorableDateTimeZone revived = new DateTimeZoneBuilder.RuleBasedZone(alias, builder);
                if (test(revived.getID(), revived)) {
                    map.put(revived.getID(), revived);
                }
                map.put(revived.getID(), revived);
            }
        }

        // store "back" links as aliases (where name is permanently mapped
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < iBackLinks.size(); i += 2) {
                String id = iBackLinks.get(i);
                String alias = iBackLinks.get(i + 1);
                StorableDateTimeZone tz = map.get(id);
                if (tz == null) {
                    if (pass > 0) {
                        throw new RuntimeException("Cannot find time zone '" + id + "' to link alias '"
                                + alias + "' to");
                    }
                } else {
                    map.put(alias, new AliasDateTimeZone(alias, tz));
                }
            }
        }

        return map;
    }

    public void parseDataFile(BufferedReader in, boolean backward) throws IOException {
        Zone zone = null;
        String line;
        while ((line = in.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.length() == 0 || trimmed.charAt(0) == '#') {
                continue;
            }

            int index = line.indexOf('#');
            if (index >= 0) {
                line = line.substring(0, index);
            }

            //System.out.println(line);

            StringTokenizer st = new StringTokenizer(line, " \t");

            if (Character.isWhitespace(line.charAt(0)) && st.hasMoreTokens()) {
                if (zone != null) {
                    // Zone continuation
                    zone.chain(st);
                }
                continue;
            } else {
                if (zone != null) {
                    iZones.add(zone);
                }
                zone = null;
            }

            if (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (token.equalsIgnoreCase("Rule")) {
                    Rule r = new Rule(st);
                    RuleSet rs = iRuleSets.get(r.iName);
                    if (rs == null) {
                        rs = new RuleSet(r);
                        iRuleSets.put(r.iName, rs);
                    } else {
                        rs.addRule(r);
                    }
                } else if (token.equalsIgnoreCase("Zone")) {
                    zone = new Zone(st);
                } else if (token.equalsIgnoreCase("Link")) {
                    String real = st.nextToken();
                    String alias = st.nextToken();
                    // links in "backward" are deprecated names
                    // links in other files should be kept
                    // special case a few to try to repair terrible damage to tzdb
                    if (backward || alias.equals("US/Pacific-New") || alias.startsWith("Etc/") || alias.equals("GMT")) {
                        iBackLinks.add(real);
                        iBackLinks.add(alias);
                    } else {
                        iGoodLinks.add(real);
                        iGoodLinks.add(alias);
                    }
                } else {
                    System.out.println("Unknown line: " + line);
                }
            }
        }

        if (zone != null) {
            iZones.add(zone);
        }
    }

    static class DateTimeOfYear {
        public final int iMonthOfYear;
        public final int iDayOfMonth;
        public final int iDayOfWeek;
        public final boolean iAdvanceDayOfWeek;
        public final int iMillisOfDay;
        public final char iZoneChar;

        DateTimeOfYear() {
            iMonthOfYear = 1;
            iDayOfMonth = 1;
            iDayOfWeek = 0;
            iAdvanceDayOfWeek = false;
            iMillisOfDay = 0;
            iZoneChar = 'w';
        }

        DateTimeOfYear(StringTokenizer st) {
            int month = 1;
            int day = 1;
            int dayOfWeek = 0;
            int millis = 0;
            boolean advance = false;
            char zoneChar = 'w';

            if (st.hasMoreTokens()) {
                month = parseMonth(st.nextToken());

                if (st.hasMoreTokens()) {
                    String str = st.nextToken();
                    if (str.startsWith("last")) {
                        day = -1;
                        dayOfWeek = parseDayOfWeek(str.substring(4));
                        advance = false;
                    } else {
                        try {
                            day = Integer.parseInt(str);
                            dayOfWeek = 0;
                            advance = false;
                        } catch (NumberFormatException e) {
                            int index = str.indexOf(">=");
                            if (index > 0) {
                                day = Integer.parseInt(str.substring(index + 2));
                                dayOfWeek = parseDayOfWeek(str.substring(0, index));
                                advance = true;
                            } else {
                                index = str.indexOf("<=");
                                if (index > 0) {
                                    day = Integer.parseInt(str.substring(index + 2));
                                    dayOfWeek = parseDayOfWeek(str.substring(0, index));
                                    advance = false;
                                } else {
                                    throw new IllegalArgumentException(str);
                                }
                            }
                        }
                    }

                    if (st.hasMoreTokens()) {
                        str = st.nextToken();
                        zoneChar = parseZoneChar(str.charAt(str.length() - 1));
                        if (str.equals("24:00")) {
                            // handle end of year
                            if (month == 12 && day == 31) {
                                millis = parseTime("23:59:59.999");
                            } else {
                                LocalDate date = day == -1 ? new LocalDate(2001, month, 1).plusMonths(1)
                                        : new LocalDate(2001, month, day).plusDays(1);
                                advance = day != -1 && dayOfWeek != 0;
                                month = date.getMonthOfYear();
                                day = date.getDayOfMonth();
                                if (dayOfWeek != 0) {
                                    dayOfWeek = ((dayOfWeek - 1 + 1) % 7) + 1;
                                }
                            }
                        } else {
                            millis = parseTime(str);
                        }
                    }
                }
            }

            iMonthOfYear = month;
            iDayOfMonth = day;
            iDayOfWeek = dayOfWeek;
            iAdvanceDayOfWeek = advance;
            iMillisOfDay = millis;
            iZoneChar = zoneChar;
        }

        /**
         * Adds a recurring savings rule to the builder.
         */
        public void addRecurring(DateTimeZoneBuilder builder, int saveMillis, int fromYear, int toYear) {
            builder.addRecurringSavings(saveMillis,
                                        fromYear, toYear,
                                        iZoneChar,
                                        iMonthOfYear,
                                        iDayOfMonth,
                                        iDayOfWeek,
                                        iAdvanceDayOfWeek,
                                        iMillisOfDay);
        }

        /**
         * Adds a cutover to the builder.
         */
        public void addCutover(DateTimeZoneBuilder builder, int year) {
            builder.addCutover(year,
                               iZoneChar,
                               iMonthOfYear,
                               iDayOfMonth,
                               iDayOfWeek,
                               iAdvanceDayOfWeek,
                               iMillisOfDay);
        }

        @Override
        public String toString() {
            return "MonthOfYear: " + iMonthOfYear + "\n" + "DayOfMonth: " + iDayOfMonth + "\n" + "DayOfWeek: "
                    + iDayOfWeek + "\n" + "AdvanceDayOfWeek: " + iAdvanceDayOfWeek + "\n" + "MillisOfDay: "
                    + iMillisOfDay + "\n" + "ZoneChar: " + iZoneChar + "\n";
        }
    }

    private static class Rule {
        public final String iName;
        public final int iFromYear;
        public final int iToYear;
        public final String iType;
        public final DateTimeOfYear iDateTimeOfYear;
        public final int iSaveMillis;
        public final String iLetterS;

        Rule(StringTokenizer st) {
            iName = st.nextToken().intern();
            iFromYear = parseYear(st.nextToken(), 0);
            iToYear = parseYear(st.nextToken(), iFromYear);
            if (iToYear < iFromYear) {
                throw new IllegalArgumentException();
            }
            iType = parseOptional(st.nextToken());
            iDateTimeOfYear = new DateTimeOfYear(st);
            iSaveMillis = parseTime(st.nextToken());
            iLetterS = parseOptional(st.nextToken());
        }

        /**
         * Adds a recurring savings rule to the builder.
         */
        public void addRecurring(DateTimeZoneBuilder builder) {
            iDateTimeOfYear.addRecurring(builder, iSaveMillis, iFromYear, iToYear);
        }

        @Override
        public String toString() {
            return "[Rule]\n" + "Name: " + iName + "\n" + "FromYear: " + iFromYear + "\n" + "ToYear: " + iToYear + "\n"
                    + "Type: " + iType + "\n" + iDateTimeOfYear + "SaveMillis: " + iSaveMillis + "\n" + "LetterS: "
                    + iLetterS + "\n";
        }
    }

    private static class RuleSet {
        private List<Rule> iRules;

        RuleSet(Rule rule) {
            iRules = new ArrayList<>();
            iRules.add(rule);
        }

        void addRule(Rule rule) {
            if (!(rule.iName.equals(iRules.get(0).iName))) {
                throw new IllegalArgumentException("Rule name mismatch");
            }
            iRules.add(rule);
        }

        /**
         * Adds recurring savings rules to the builder.
         */
        public void addRecurring(DateTimeZoneBuilder builder) {
            for (int i = 0; i < iRules.size(); i++) {
                Rule rule = iRules.get(i);
                rule.addRecurring(builder);
            }
        }
    }

    private static class Zone {
        public final String iName;
        public final int iOffsetMillis;
        public final String iRules;
        public final String iFormat;
        public final int iUntilYear;
        public final DateTimeOfYear iUntilDateTimeOfYear;

        private Zone iNext;

        Zone(StringTokenizer st) {
            this(st.nextToken(), st);
        }

        private Zone(String name, StringTokenizer st) {
            iName = name.intern();
            iOffsetMillis = parseTime(st.nextToken());
            iRules = parseOptional(st.nextToken());
            iFormat = st.nextToken().intern();

            int year = Integer.MAX_VALUE;
            DateTimeOfYear dtOfYear = getStartOfYear();

            if (st.hasMoreTokens()) {
                year = Integer.parseInt(st.nextToken());
                if (st.hasMoreTokens()) {
                    dtOfYear = new DateTimeOfYear(st);
                }
            }

            iUntilYear = year;
            iUntilDateTimeOfYear = dtOfYear;
        }

        void chain(StringTokenizer st) {
            if (iNext != null) {
                iNext.chain(st);
            } else {
                iNext = new Zone(iName, st);
            }
        }

        /*
        public DateTimeZone buildDateTimeZone(Map ruleSets) {
            DateTimeZoneBuilder builder = new DateTimeZoneBuilder();
            addToBuilder(builder, ruleSets);
            return builder.toDateTimeZone(iName);
        }
        */

        /**
         * Adds zone info to the builder.
         */
        public void addToBuilder(DateTimeZoneBuilder builder, Map<String, RuleSet> ruleSets) {
            addToBuilder(this, builder, ruleSets);
        }

        private static void addToBuilder(Zone zone, DateTimeZoneBuilder builder, Map<String, RuleSet> ruleSets) {
            for (; zone != null; zone = zone.iNext) {
                builder.setStandardOffset(zone.iOffsetMillis);

                if (zone.iRules == null) {
                    builder.setFixedSavings(zone.iFormat, 0);
                } else {
                    try {
                        // Check if iRules actually just refers to a savings.
                        int saveMillis = parseTime(zone.iRules);
                        builder.setFixedSavings(zone.iFormat, saveMillis);
                    } catch (Exception e) {
                        RuleSet rs = ruleSets.get(zone.iRules);
                        if (rs == null) {
                            throw new IllegalArgumentException("Rules not found: " + zone.iRules);
                        }
                        rs.addRecurring(builder);
                    }
                }

                if (zone.iUntilYear == Integer.MAX_VALUE) {
                    break;
                }

                zone.iUntilDateTimeOfYear.addCutover(builder, zone.iUntilYear);
            }
        }

        @Override
        public String toString() {
            String str = "[Zone]\n" + "Name: " + iName + "\n" + "OffsetMillis: " + iOffsetMillis + "\n" + "Rules: "
                    + iRules + "\n" + "Format: " + iFormat + "\n" + "UntilYear: " + iUntilYear + "\n"
                    + iUntilDateTimeOfYear;

            if (iNext == null) {
                return str;
            }

            return str + "...\n" + iNext;
        }
    }
}

