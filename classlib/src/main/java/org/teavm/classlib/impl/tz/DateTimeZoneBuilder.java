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

import java.util.*;
import org.teavm.classlib.impl.Base46;
import org.teavm.classlib.impl.CharFlow;

/**
 * DateTimeZoneBuilder allows complex DateTimeZones to be constructed. Since
 * creating a new DateTimeZone this way is a relatively expensive operation,
 * built zones can be written to a file. Reading back the encoded data is a
 * quick operation.
 * <p>
 * DateTimeZoneBuilder itself is mutable and not thread-safe, but the
 * DateTimeZone objects that it builds are thread-safe and immutable.
 * <p>
 * It is intended that {@link ZoneInfoCompiler} be used to read time zone data
 * files, indirectly calling DateTimeZoneBuilder. The following complex
 * example defines the America/Los_Angeles time zone, with all historical
 * transitions:
 *
 * <pre>
 * DateTimeZone America_Los_Angeles = new DateTimeZoneBuilder()
 *     .addCutover(-2147483648, 'w', 1, 1, 0, false, 0)
 *     .setStandardOffset(-28378000)
 *     .setFixedSavings("LMT", 0)
 *     .addCutover(1883, 'w', 11, 18, 0, false, 43200000)
 *     .setStandardOffset(-28800000)
 *     .addRecurringSavings("PDT", 3600000, 1918, 1919, 'w',  3, -1, 7, false, 7200000)
 *     .addRecurringSavings("PST",       0, 1918, 1919, 'w', 10, -1, 7, false, 7200000)
 *     .addRecurringSavings("PWT", 3600000, 1942, 1942, 'w',  2,  9, 0, false, 7200000)
 *     .addRecurringSavings("PPT", 3600000, 1945, 1945, 'u',  8, 14, 0, false, 82800000)
 *     .addRecurringSavings("PST",       0, 1945, 1945, 'w',  9, 30, 0, false, 7200000)
 *     .addRecurringSavings("PDT", 3600000, 1948, 1948, 'w',  3, 14, 0, false, 7200000)
 *     .addRecurringSavings("PST",       0, 1949, 1949, 'w',  1,  1, 0, false, 7200000)
 *     .addRecurringSavings("PDT", 3600000, 1950, 1966, 'w',  4, -1, 7, false, 7200000)
 *     .addRecurringSavings("PST",       0, 1950, 1961, 'w',  9, -1, 7, false, 7200000)
 *     .addRecurringSavings("PST",       0, 1962, 1966, 'w', 10, -1, 7, false, 7200000)
 *     .addRecurringSavings("PST",       0, 1967, 2147483647, 'w', 10, -1, 7, false, 7200000)
 *     .addRecurringSavings("PDT", 3600000, 1967, 1973, 'w', 4, -1,  7, false, 7200000)
 *     .addRecurringSavings("PDT", 3600000, 1974, 1974, 'w', 1,  6,  0, false, 7200000)
 *     .addRecurringSavings("PDT", 3600000, 1975, 1975, 'w', 2, 23,  0, false, 7200000)
 *     .addRecurringSavings("PDT", 3600000, 1976, 1986, 'w', 4, -1,  7, false, 7200000)
 *     .addRecurringSavings("PDT", 3600000, 1987, 2147483647, 'w', 4, 1, 7, true, 7200000)
 *     .toDateTimeZone("America/Los_Angeles", true);
 * </pre>
 *
 * @author Brian S O'Neill
 * @see ZoneInfoCompiler
 * @since 1.0
 */
public class DateTimeZoneBuilder {
    private static TimeZone gmtCache;
    private static StorableDateTimeZone buildFixedZone(String id, int wallOffset, int standardOffset) {
        return new FixedDateTimeZone(id, wallOffset, standardOffset);
    }

    // List of RuleSets.
    private final ArrayList<RuleSet> iRuleSets;

    public DateTimeZoneBuilder() {
        iRuleSets = new ArrayList<>(10);
    }

    private static TimeZone getGMT() {
        if (gmtCache == null) {
            gmtCache = TimeZone.getTimeZone("GMT+00:00");
        }
        return gmtCache;
    }

    /**
     * Adds a cutover for added rules. The standard offset at the cutover
     * defaults to 0. Call setStandardOffset afterwards to change it.
     *
     * @param year  the year of cutover
     * @param mode 'u' - cutover is measured against UTC, 'w' - against wall
     *  offset, 's' - against standard offset
     * @param monthOfYear  the month from 1 (January) to 12 (December)
     * @param dayOfMonth  if negative, set to ((last day of month) - ~dayOfMonth).
     *  For example, if -1, set to last day of month
     * @param dayOfWeek  from 1 (Monday) to 7 (Sunday), if 0 then ignore
     * @param advanceDayOfWeek  if dayOfMonth does not fall on dayOfWeek, advance to
     *  dayOfWeek when true, retreat when false.
     * @param millisOfDay  additional precision for specifying time of day of cutover
     */
    public DateTimeZoneBuilder addCutover(int year, char mode, int monthOfYear, int dayOfMonth, int dayOfWeek,
            boolean advanceDayOfWeek, int millisOfDay) {
        if (iRuleSets.size() > 0) {
            OfYear ofYear = new OfYear(mode, monthOfYear, dayOfMonth, dayOfWeek, advanceDayOfWeek, millisOfDay);
            RuleSet lastRuleSet = iRuleSets.get(iRuleSets.size() - 1);
            lastRuleSet.setUpperLimit(year, ofYear);
        }
        iRuleSets.add(new RuleSet());
        return this;
    }

    /**
     * Sets the standard offset to use for newly added rules until the next
     * cutover is added.
     * @param standardOffset  the standard offset in millis
     */
    public DateTimeZoneBuilder setStandardOffset(int standardOffset) {
        getLastRuleSet().setStandardOffset(standardOffset);
        return this;
    }

    /**
     * Set a fixed savings rule at the cutover.
     */
    public DateTimeZoneBuilder setFixedSavings(String nameKey, int saveMillis) {
        getLastRuleSet().setFixedSavings(nameKey, saveMillis);
        return this;
    }

    /**
     * Add a recurring daylight saving time rule.
     *
     * @param saveMillis  the milliseconds to add to standard offset
     * @param fromYear  the first year that rule is in effect, MIN_VALUE indicates
     * beginning of time
     * @param toYear  the last year (inclusive) that rule is in effect, MAX_VALUE
     *  indicates end of time
     * @param mode  'u' - transitions are calculated against UTC, 'w' -
     *  transitions are calculated against wall offset, 's' - transitions are
     *  calculated against standard offset
     * @param monthOfYear  the month from 1 (January) to 12 (December)
     * @param dayOfMonth  if negative, set to ((last day of month) - ~dayOfMonth).
     *  For example, if -1, set to last day of month
     * @param dayOfWeek  from 1 (Monday) to 7 (Sunday), if 0 then ignore
     * @param advanceDayOfWeek  if dayOfMonth does not fall on dayOfWeek, advance to
     *  dayOfWeek when true, retreat when false.
     * @param millisOfDay  additional precision for specifying time of day of transitions
     */
    public DateTimeZoneBuilder addRecurringSavings(int saveMillis,
                                                   int fromYear, int toYear,
                                                   char mode,
                                                   int monthOfYear,
                                                   int dayOfMonth,
                                                   int dayOfWeek,
                                                   boolean advanceDayOfWeek,
                                                   int millisOfDay) {
        if (fromYear <= toYear) {
            OfYear ofYear = new OfYear(mode, monthOfYear, dayOfMonth, dayOfWeek, advanceDayOfWeek, millisOfDay);
            Recurrence recurrence = new Recurrence(ofYear, saveMillis);
            Rule rule = new Rule(recurrence, fromYear, toYear);
            getLastRuleSet().addRule(rule);
        }
        return this;
    }

    private RuleSet getLastRuleSet() {
        if (iRuleSets.size() == 0) {
            addCutover(Integer.MIN_VALUE, 'w', 1, 1, 0, false, 0);
        }
        return iRuleSets.get(iRuleSets.size() - 1);
    }

    /**
     * Processes all the rules and builds a DateTimeZone.
     *
     * @param id  time zone id to assign
     * @param outputID  true if the zone id should be output
     */
    public StorableDateTimeZone toDateTimeZone(String id, boolean outputID) {
        if (id == null) {
            throw new IllegalArgumentException();
        }

        // Discover where all the transitions occur and store the results in
        // these lists.
        ArrayList<Transition> transitions = new ArrayList<>();

        // Tail zone picks up remaining transitions in the form of an endless
        // DST cycle.
        DSTZone tailZone = null;

        long millis = Long.MIN_VALUE;
        int saveMillis = 0;

        int ruleSetCount = iRuleSets.size();
        for (int i = 0; i < ruleSetCount; i++) {
            RuleSet rs = iRuleSets.get(i);
            Transition next = rs.firstTransition(millis);
            if (next == null) {
                continue;
            }
            addTransition(transitions, next);
            millis = next.getMillis();
            saveMillis = next.getSaveMillis();

            // Copy it since we're going to destroy it.
            rs = new RuleSet(rs);

            while ((next = rs.nextTransition(millis, saveMillis)) != null) {
                if (addTransition(transitions, next)) {
                    if (tailZone != null) {
                        // Got the extra transition before DSTZone.
                        break;
                    }
                }
                millis = next.getMillis();
                saveMillis = next.getSaveMillis();
                if (tailZone == null && i == ruleSetCount - 1) {
                    tailZone = rs.buildTailZone(id);
                    // If tailZone is not null, don't break out of main loop until
                    // at least one more transition is calculated. This ensures a
                    // correct 'seam' to the DSTZone.
                }
            }

            millis = rs.getUpperLimit(saveMillis);
        }

        // Check if a simpler zone implementation can be returned.
        if (transitions.size() == 0) {
            if (tailZone != null) {
                // This shouldn't happen, but handle just in case.
                return tailZone;
            }
            return buildFixedZone(id, 0, 0);
        }
        if (transitions.size() == 1 && tailZone == null) {
            Transition tr = transitions.get(0);
            return buildFixedZone(id, tr.getWallOffset(), tr.getStandardOffset());
        }

        PrecalculatedZone zone = PrecalculatedZone.create(id, outputID, transitions, tailZone);
        if (zone.isCachable()) {
            return CachedDateTimeZone.forZone(zone);
        }
        return zone;
    }

    private boolean addTransition(ArrayList<Transition> transitions, Transition tr) {
        int size = transitions.size();
        if (size == 0) {
            transitions.add(tr);
            return true;
        }

        Transition last = transitions.get(size - 1);
        if (!tr.isTransitionFrom(last)) {
            return false;
        }

        // If local time of new transition is same as last local time, just
        // replace last transition with new one.
        int offsetForLast = 0;
        if (size >= 2) {
            offsetForLast = transitions.get(size - 2).getWallOffset();
        }
        int offsetForNew = last.getWallOffset();

        long lastLocal = last.getMillis() + offsetForLast;
        long newLocal = tr.getMillis() + offsetForNew;

        if (newLocal != lastLocal) {
            transitions.add(tr);
            return true;
        }

        transitions.remove(size - 1);
        return addTransition(transitions, tr);
    }

    /**
     * Supports setting fields of year and moving between transitions.
     */
    static final class OfYear {
        // Is 'u', 'w', or 's'.
        final char iMode;

        final int iMonthOfYear;
        final int iDayOfMonth;
        final int iDayOfWeek;
        final boolean iAdvance;
        final int iMillisOfDay;

        OfYear(char mode, int monthOfYear, int dayOfMonth, int dayOfWeek, boolean advanceDayOfWeek, int millisOfDay) {
            if (mode != 'u' && mode != 'w' && mode != 's') {
                throw new IllegalArgumentException("Unknown mode: " + mode);
            }

            iMode = mode;
            iMonthOfYear = monthOfYear;
            iDayOfMonth = dayOfMonth;
            iDayOfWeek = dayOfWeek;
            iAdvance = advanceDayOfWeek;
            iMillisOfDay = millisOfDay;
        }

        public void write(StringBuilder sb) {
            sb.append(iMode);
            Base46.encodeUnsigned(sb, iMonthOfYear);
            Base46.encodeUnsigned(sb, iDayOfMonth);
            Base46.encode(sb, iDayOfWeek);
            sb.append(iAdvance ? 'y' : 'n');
            StorableDateTimeZone.writeUnsignedTime(sb, iMillisOfDay);
        }

        public static OfYear read(CharFlow flow) {
            char mode = flow.characters[flow.pointer++];
            int monthOfYear = Base46.decodeUnsigned(flow);
            int dayOfMonth = Base46.decodeUnsigned(flow);
            int dayOfWeek = Base46.decode(flow);
            boolean advance = flow.characters[flow.pointer++] == 'y';
            int millisOfDay = (int) StorableDateTimeZone.readUnsignedTime(flow);
            return new OfYear(mode, monthOfYear, dayOfMonth, dayOfWeek, advance, millisOfDay);
        }

        /**
         * @param standardOffset standard offset just before instant
         */
        public long setInstant(int year, int standardOffset, int saveMillis) {
            int offset;
            if (iMode == 'w') {
                offset = standardOffset + saveMillis;
            } else if (iMode == 's') {
                offset = standardOffset;
            } else {
                offset = 0;
            }

            Calendar calendar = Calendar.getInstance(getGMT());
            calendar.setTimeInMillis(0);
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, iMonthOfYear - 1);
            calendar.set(Calendar.HOUR, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.add(Calendar.MILLISECOND, iMillisOfDay);
            setDayOfMonth(calendar);

            if (iDayOfWeek != 0) {
                setDayOfWeek(calendar);
            }

            // Convert from local time to UTC.
            return calendar.getTimeInMillis() - offset;
        }

        /**
         * @param standardOffset standard offset just before next recurrence
         */
        public long next(long instant, int standardOffset, int saveMillis) {
            int offset;
            if (iMode == 'w') {
                offset = standardOffset + saveMillis;
            } else if (iMode == 's') {
                offset = standardOffset;
            } else {
                offset = 0;
            }

            // Convert from UTC to local time.
            instant += offset;

            GregorianCalendar calendar = new GregorianCalendar(getGMT());
            calendar.setTimeInMillis(instant);
            calendar.set(Calendar.MONTH, iMonthOfYear - 1);
            calendar.set(Calendar.DATE, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.add(Calendar.MILLISECOND, iMillisOfDay);
            setDayOfMonthNext(calendar);

            if (iDayOfWeek == 0) {
                if (calendar.getTimeInMillis() <= instant) {
                    calendar.add(Calendar.YEAR, 1);
                    setDayOfMonthNext(calendar);
                }
            } else {
                setDayOfWeek(calendar);
                if (calendar.getTimeInMillis() <= instant) {
                    calendar.add(Calendar.YEAR, 1);
                    calendar.set(Calendar.MONTH, iMonthOfYear - 1);
                    setDayOfMonthNext(calendar);
                    setDayOfWeek(calendar);
                }
            }

            // Convert from local time to UTC.
            return calendar.getTimeInMillis() - offset;
        }

        /**
         * @param standardOffset standard offset just before previous recurrence
         */
        public long previous(long instant, int standardOffset, int saveMillis) {
            int offset;
            if (iMode == 'w') {
                offset = standardOffset + saveMillis;
            } else if (iMode == 's') {
                offset = standardOffset;
            } else {
                offset = 0;
            }

            // Convert from UTC to local time.
            instant += offset;

            GregorianCalendar calendar = new GregorianCalendar(getGMT());
            calendar.setTimeInMillis(instant);
            calendar.set(Calendar.MONTH, iMonthOfYear - 1);
            calendar.set(Calendar.DATE, 1);
            // Be lenient with millisOfDay.
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.add(Calendar.MILLISECOND, iMillisOfDay);
            setDayOfMonthPrevious(calendar);

            if (iDayOfWeek == 0) {
                if (calendar.getTimeInMillis() >= instant) {
                    calendar.add(Calendar.YEAR, -1);
                    setDayOfMonthPrevious(calendar);
                }
            } else {
                setDayOfWeek(calendar);
                if (calendar.getTimeInMillis() >= instant) {
                    calendar.add(Calendar.YEAR, -1);
                    calendar.set(Calendar.MONTH, iMonthOfYear - 1);
                    setDayOfMonthPrevious(calendar);
                    setDayOfWeek(calendar);
                }
            }

            // Convert from local time to UTC.
            return calendar.getTimeInMillis() - offset;
        }

        /**
         * If month-day is 02-29 and year isn't leap, advances to next leap year.
         */
        private void setDayOfMonthNext(GregorianCalendar calendar) {
            if (calendar.get(Calendar.MONTH) == Calendar.FEBRUARY && calendar.get(Calendar.DATE) == 29) {
                while (!calendar.isLeapYear(calendar.get(Calendar.YEAR))) {
                    calendar.add(Calendar.YEAR, 1);
                }
            }
            setDayOfMonth(calendar);
        }

        /**
         * If month-day is 02-29 and year isn't leap, retreats to previous leap year.
         */
        private void setDayOfMonthPrevious(GregorianCalendar calendar) {
            if (calendar.get(Calendar.MONTH) == Calendar.FEBRUARY && calendar.get(Calendar.DATE) == 29) {
                while (!calendar.isLeapYear(calendar.get(Calendar.YEAR))) {
                    calendar.add(Calendar.YEAR, -1);
                }
            }
            setDayOfMonth(calendar);
        }

        private void setDayOfMonth(Calendar calendar) {
            if (iDayOfMonth >= 0) {
                calendar.set(Calendar.DAY_OF_MONTH, iDayOfMonth);
            } else {
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.add(Calendar.MONTH, 1);
                calendar.add(Calendar.DAY_OF_MONTH, iDayOfMonth);
            }
        }

        private void setDayOfWeek(Calendar calendar) {
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            int daysToAdd = (iDayOfWeek == 7 ? 1 : dayOfWeek + 1) - dayOfWeek;
            if (daysToAdd != 0) {
                if (iAdvance) {
                    if (daysToAdd < 0) {
                        daysToAdd += 7;
                    }
                } else {
                    if (daysToAdd > 0) {
                        daysToAdd -= 7;
                    }
                }
                calendar.add(Calendar.DATE, daysToAdd);
            }
        }
    }

    /**
     * Extends OfYear with a nameKey and savings.
     */
    static final class Recurrence {
        final OfYear iOfYear;
        final int iSaveMillis;

        Recurrence(OfYear ofYear, int saveMillis) {
            iOfYear = ofYear;
            iSaveMillis = saveMillis;
        }

        public OfYear getOfYear() {
            return iOfYear;
        }

        /**
         * @param standardOffset standard offset just before next recurrence
         */
        public long next(long instant, int standardOffset, int saveMillis) {
            return iOfYear.next(instant, standardOffset, saveMillis);
        }

        /**
         * @param standardOffset standard offset just before previous recurrence
         */
        public long previous(long instant, int standardOffset, int saveMillis) {
            return iOfYear.previous(instant, standardOffset, saveMillis);
        }

        public int getSaveMillis() {
            return iSaveMillis;
        }

        public void write(StringBuilder sb) {
            iOfYear.write(sb);
            StorableDateTimeZone.writeTime(sb, iSaveMillis);
        }

        public static Recurrence read(CharFlow flow) {
            OfYear ofYear = OfYear.read(flow);
            int saveMillis = (int) StorableDateTimeZone.readTime(flow);
            return new Recurrence(ofYear, saveMillis);
        }
    }

    /**
     * Extends Recurrence with inclusive year limits.
     */
    private static final class Rule {
        final Recurrence iRecurrence;
        final int iFromYear; // inclusive
        final int iToYear;   // inclusive

        Rule(Recurrence recurrence, int fromYear, int toYear) {
            iRecurrence = recurrence;
            iFromYear = fromYear;
            iToYear = toYear;
        }

        @SuppressWarnings("unused")
        public int getFromYear() {
            return iFromYear;
        }

        public int getToYear() {
            return iToYear;
        }

        @SuppressWarnings("unused")
        public OfYear getOfYear() {
            return iRecurrence.getOfYear();
        }

        public int getSaveMillis() {
            return iRecurrence.getSaveMillis();
        }

        public long next(final long instant, int standardOffset, int saveMillis) {
            Calendar calendar = Calendar.getInstance(getGMT());
            final int wallOffset = standardOffset + saveMillis;
            long testInstant = instant;

            int year;
            if (instant == Long.MIN_VALUE) {
                year = Integer.MIN_VALUE;
            } else {
                calendar.setTimeInMillis(instant + wallOffset);
                year = calendar.get(Calendar.YEAR);
            }

            if (year < iFromYear) {
                calendar.setTimeInMillis(0);
                calendar.set(Calendar.YEAR, iFromYear);
                // First advance instant to start of from year.
                testInstant = calendar.getTimeInMillis() - wallOffset;
                // Back off one millisecond to account for next recurrence
                // being exactly at the beginning of the year.
                testInstant -= 1;
            }

            long next = iRecurrence.next(testInstant, standardOffset, saveMillis);

            if (next > instant) {
                calendar.setTimeInMillis(next + wallOffset);
                year = calendar.get(Calendar.YEAR);
                if (year > iToYear) {
                    // Out of range, return original value.
                    next = instant;
                }
            }

            return next;
        }
    }

    private static final class Transition {
        private final long iMillis;
        private final int iWallOffset;
        private final int iStandardOffset;

        Transition(long millis, Transition tr) {
            iMillis = millis;
            iWallOffset = tr.iWallOffset;
            iStandardOffset = tr.iStandardOffset;
        }

        Transition(long millis, Rule rule, int standardOffset) {
            iMillis = millis;
            iWallOffset = standardOffset + rule.getSaveMillis();
            iStandardOffset = standardOffset;
        }

        Transition(long millis, int wallOffset, int standardOffset) {
            iMillis = millis;
            iWallOffset = wallOffset;
            iStandardOffset = standardOffset;
        }

        public long getMillis() {
            return iMillis;
        }

        public int getWallOffset() {
            return iWallOffset;
        }

        public int getStandardOffset() {
            return iStandardOffset;
        }

        public int getSaveMillis() {
            return iWallOffset - iStandardOffset;
        }

        /**
         * There must be a change in the millis, wall offsets or name keys.
         */
        public boolean isTransitionFrom(Transition other) {
            if (other == null) {
                return true;
            }
            return iMillis > other.iMillis && iWallOffset != other.iWallOffset;
        }
    }

    private static final class RuleSet {
        private static final int YEAR_LIMIT;

        static {
            // Don't pre-calculate more than 100 years into the future. Almost
            // all zones will stop pre-calculating far sooner anyhow. Either a
            // simple DST cycle is detected or the last rule is a fixed
            // offset. If a zone has a fixed offset set more than 100 years
            // into the future, then it won't be observed.
            Calendar calendar = Calendar.getInstance();
            YEAR_LIMIT = calendar.get(Calendar.YEAR) + 100;
        }

        private int iStandardOffset;
        private ArrayList<Rule> iRules;

        // Optional.
        private String iInitialNameKey;
        private int iInitialSaveMillis;

        // Upper limit is exclusive.
        private int iUpperYear;
        private OfYear iUpperOfYear;

        RuleSet() {
            iRules = new ArrayList<>(10);
            iUpperYear = Integer.MAX_VALUE;
        }

        /**
         * Copy constructor.
         */
        RuleSet(RuleSet rs) {
            iStandardOffset = rs.iStandardOffset;
            iRules = new ArrayList<>(rs.iRules);
            iInitialSaveMillis = rs.iInitialSaveMillis;
            iUpperYear = rs.iUpperYear;
            iUpperOfYear = rs.iUpperOfYear;
        }

        @SuppressWarnings("unused")
        public int getStandardOffset() {
            return iStandardOffset;
        }

        public void setStandardOffset(int standardOffset) {
            iStandardOffset = standardOffset;
        }

        public void setFixedSavings(String nameKey, int saveMillis) {
            iInitialNameKey = nameKey;
            iInitialSaveMillis = saveMillis;
        }

        public void addRule(Rule rule) {
            if (!iRules.contains(rule)) {
                iRules.add(rule);
            }
        }

        public void setUpperLimit(int year, OfYear ofYear) {
            iUpperYear = year;
            iUpperOfYear = ofYear;
        }

        /**
         * Returns a transition at firstMillis with the first name key and
         * offsets for this rule set. This method may return null.
         *
         * @param firstMillis millis of first transition
         */
        public Transition firstTransition(final long firstMillis) {
            if (iInitialNameKey != null) {
                // Initial zone info explicitly set, so don't search the rules.
                return new Transition(firstMillis, iStandardOffset + iInitialSaveMillis, iStandardOffset);
            }

            // Make a copy before we destroy the rules.
            ArrayList<Rule> copy = new ArrayList<>(iRules);

            // Iterate through all the transitions until firstMillis is
            // reached. Use the name key and savings for whatever rule reaches
            // the limit.

            long millis = Long.MIN_VALUE;
            int saveMillis = 0;
            Transition first = null;

            Transition next;
            while ((next = nextTransition(millis, saveMillis)) != null) {
                millis = next.getMillis();

                if (millis == firstMillis) {
                    first = new Transition(firstMillis, next);
                    break;
                }

                if (millis > firstMillis) {
                    if (first == null) {
                        // Find first rule without savings. This way a more
                        // accurate nameKey is found even though no rule
                        // extends to the RuleSet's lower limit.
                        for (Rule rule : copy) {
                            if (rule.getSaveMillis() == 0) {
                                first = new Transition(firstMillis, rule, iStandardOffset);
                                break;
                            }
                        }
                    }
                    if (first == null) {
                        // Found no rule without savings. Create a transition
                        // with no savings anyhow, and use the best available
                        // name key.
                        first = new Transition(firstMillis, iStandardOffset, iStandardOffset);
                    }
                    break;
                }

                // Set first to the best transition found so far, but next
                // iteration may find something closer to lower limit.
                first = new Transition(firstMillis, next);

                saveMillis = next.getSaveMillis();
            }

            iRules = copy;
            return first;
        }

        /**
         * Returns null if RuleSet is exhausted or upper limit reached. Calling
         * this method will throw away rules as they each become
         * exhausted. Copy the RuleSet before using it to compute transitions.
         *
         * Returned transition may be a duplicate from previous
         * transition. Caller must call isTransitionFrom to filter out
         * duplicates.
         *
         * @param saveMillis savings before next transition
         */
        public Transition nextTransition(final long instant, final int saveMillis) {
            // Find next matching rule.
            Rule nextRule = null;
            long nextMillis = Long.MAX_VALUE;

            Iterator<Rule> it = iRules.iterator();
            while (it.hasNext()) {
                Rule rule = it.next();
                long next = rule.next(instant, iStandardOffset, saveMillis);
                if (next <= instant) {
                    it.remove();
                    continue;
                }
                // Even if next is same as previous next, choose the rule
                // in order for more recently added rules to override.
                if (next <= nextMillis) {
                    // Found a better match.
                    nextRule = rule;
                    nextMillis = next;
                }
            }

            if (nextRule == null) {
                return null;
            }

            // Stop precalculating if year reaches some arbitrary limit.
            Calendar c = Calendar.getInstance(getGMT());
            c.setTimeInMillis(nextMillis);
            if (c.get(Calendar.YEAR) >= YEAR_LIMIT) {
                return null;
            }

            // Check if upper limit reached or passed.
            if (iUpperYear < Integer.MAX_VALUE) {
                long upperMillis =
                    iUpperOfYear.setInstant(iUpperYear, iStandardOffset, saveMillis);
                if (nextMillis >= upperMillis) {
                    // At or after upper limit.
                    return null;
                }
            }

            return new Transition(nextMillis, nextRule, iStandardOffset);
        }

        /**
         * @param saveMillis savings before upper limit
         */
        public long getUpperLimit(int saveMillis) {
            if (iUpperYear == Integer.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            return iUpperOfYear.setInstant(iUpperYear, iStandardOffset, saveMillis);
        }

        /**
         * Returns null if none can be built.
         */
        public DSTZone buildTailZone(String id) {
            if (iRules.size() == 2) {
                Rule startRule = iRules.get(0);
                Rule endRule = iRules.get(1);
                if (startRule.getToYear() == Integer.MAX_VALUE && endRule.getToYear() == Integer.MAX_VALUE) {

                    // With exactly two infinitely recurring rules left, a
                    // simple DSTZone can be formed.

                    // The order of rules can come in any order, and it doesn't
                    // really matter which rule was chosen the 'start' and
                    // which is chosen the 'end'. DSTZone works properly either
                    // way.
                    return new DSTZone(id, iStandardOffset,
                                       startRule.iRecurrence, endRule.iRecurrence);
                }
            }
            return null;
        }
    }

    static final class DSTZone extends StorableDateTimeZone {
        final int iStandardOffset;
        final Recurrence iStartRecurrence;
        final Recurrence iEndRecurrence;

        DSTZone(String id, int standardOffset, Recurrence startRecurrence, Recurrence endRecurrence) {
            super(id);
            iStandardOffset = standardOffset;
            iStartRecurrence = startRecurrence;
            iEndRecurrence = endRecurrence;
        }

        @Override
        public int getOffset(long instant) {
            return iStandardOffset + findMatchingRecurrence(instant).getSaveMillis();
        }

        @Override
        public int getStandardOffset(long instant) {
            return iStandardOffset;
        }

        @Override
        public boolean isFixed() {
            return false;
        }

        @Override
        public long nextTransition(long instant) {
            int standardOffset = iStandardOffset;
            Recurrence startRecurrence = iStartRecurrence;
            Recurrence endRecurrence = iEndRecurrence;

            long start;
            long end;

            try {
                start = startRecurrence.next(instant, standardOffset, endRecurrence.getSaveMillis());
                if (instant > 0 && start < 0) {
                    // Overflowed.
                    start = instant;
                }
            } catch (IllegalArgumentException | ArithmeticException e) {
                // Overflowed.
                start = instant;
            }

            try {
                end = endRecurrence.next(instant, standardOffset, startRecurrence.getSaveMillis());
                if (instant > 0 && end < 0) {
                    // Overflowed.
                    end = instant;
                }
            } catch (IllegalArgumentException | ArithmeticException e) {
                // Overflowed.
                end = instant;
            }

            return (start > end) ? end : start;
        }

        @Override
        public long previousTransition(long instant) {
            // Increment in order to handle the case where instant is exactly at
            // a transition.
            instant++;

            int standardOffset = iStandardOffset;
            Recurrence startRecurrence = iStartRecurrence;
            Recurrence endRecurrence = iEndRecurrence;

            long start;
            long end;

            try {
                start = startRecurrence.previous(instant, standardOffset, endRecurrence.getSaveMillis());
                if (instant < 0 && start > 0) {
                    // Overflowed.
                    start = instant;
                }
            } catch (IllegalArgumentException | ArithmeticException e) {
                // Overflowed.
                start = instant;
            }

            try {
                end = endRecurrence.previous(instant, standardOffset, startRecurrence.getSaveMillis());
                if (instant < 0 && end > 0) {
                    // Overflowed.
                    end = instant;
                }
            } catch (IllegalArgumentException | ArithmeticException e) {
                // Overflowed.
                end = instant;
            }

            return (start > end ? start : end) - 1;
        }

        private Recurrence findMatchingRecurrence(long instant) {
            int standardOffset = iStandardOffset;
            Recurrence startRecurrence = iStartRecurrence;
            Recurrence endRecurrence = iEndRecurrence;

            long start;
            long end;

            try {
                start = startRecurrence.next(instant, standardOffset, endRecurrence.getSaveMillis());
            } catch (IllegalArgumentException | ArithmeticException e) {
                // Overflowed.
                start = instant;
            }

            try {
                end = endRecurrence.next(instant, standardOffset, startRecurrence.getSaveMillis());
            } catch (IllegalArgumentException | ArithmeticException e) {
                // Overflowed.
                end = instant;
            }

            return (start > end) ? startRecurrence : endRecurrence;
        }

        @Override
        public void write(StringBuilder sb) {
            Base46.encodeUnsigned(sb, DST);
            writeTime(sb, iStandardOffset);
            iStartRecurrence.write(sb);
            iEndRecurrence.write(sb);
        }

        public static DSTZone readZone(String id, CharFlow flow) {
            int standardOffset = (int) readTime(flow);
            Recurrence startRecurrence = Recurrence.read(flow);
            Recurrence endRecurrence = Recurrence.read(flow);
            return new DSTZone(id, standardOffset, startRecurrence, endRecurrence);
        }
    }

    static final class PrecalculatedZone extends StorableDateTimeZone {
        /**
         * Factory to create instance from builder.
         *
         * @param id  the zone id
         * @param outputID  true if the zone id should be output
         * @param transitions  the list of Transition objects
         * @param tailZone  optional zone for getting info beyond precalculated tables
         */
        static PrecalculatedZone create(String id, boolean outputID, ArrayList<Transition> transitions,
                DSTZone tailZone) {
            int size = transitions.size();
            if (size == 0) {
                throw new IllegalArgumentException();
            }

            long[] trans = new long[size];
            int[] wallOffsets = new int[size];
            int[] standardOffsets = new int[size];

            Transition last = null;
            for (int i = 0; i < size; i++) {
                Transition tr = transitions.get(i);

                if (!tr.isTransitionFrom(last)) {
                    throw new IllegalArgumentException(id);
                }

                trans[i] = tr.getMillis();
                wallOffsets[i] = tr.getWallOffset();
                standardOffsets[i] = tr.getStandardOffset();

                last = tr;
            }

            return new PrecalculatedZone(outputID ? id : "", trans, wallOffsets, standardOffsets, tailZone);
        }

        // All array fields have the same length.

        private final long[] iTransitions;

        private final int[] iWallOffsets;
        private final int[] iStandardOffsets;

        private final DSTZone iTailZone;

        /**
         * Constructor used ONLY for valid input, loaded via static methods.
         */
        private PrecalculatedZone(String id, long[] transitions, int[] wallOffsets, int[] standardOffsets,
                DSTZone tailZone) {
            super(id);
            iTransitions = transitions;
            iWallOffsets = wallOffsets;
            iStandardOffsets = standardOffsets;
            iTailZone = tailZone;
        }

        @Override
        public void write(StringBuilder sb) {
            int start = 0;
            while (start + 1 < iTransitions.length && iTransitions[start + 1] < 631170000000L) {
                ++start;
            }

            Base46.encodeUnsigned(sb, PRECALCULATED);
            Base46.encodeUnsigned(sb, iTransitions.length - start);

            long[] transitions = iTransitions.clone();
            for (int i = 0; i < transitions.length; ++i) {
                transitions[i] = (transitions[i] / 60_000) * 60_000;
            }

            writeTime(sb, transitions[start]);
            for (int i = start + 1; i < transitions.length; ++i) {
                writeTime(sb, transitions[i] - transitions[i - 1] - (365 * 3600 * 1000 / 2));
            }

            writeTimeArray(sb, Arrays.copyOfRange(iWallOffsets, start, transitions.length));
            writeTimeArray(sb, Arrays.copyOfRange(iStandardOffsets, start, transitions.length));

            if (iTailZone != null) {
                sb.append('y');
                iTailZone.write(sb);
            } else {
                sb.append('n');
            }
        }

        public static StorableDateTimeZone readZone(String id, CharFlow flow) {
            int length = Base46.decodeUnsigned(flow);
            long[] transitions = new long[length];
            int[] wallOffsets = new int[length];
            int[] standardOffsets = new int[length];

            transitions[0] = readTime(flow);
            for (int i = 1; i < length; ++i) {
                transitions[i] = transitions[i - 1] + readTime(flow) + 365 * 3600 * 1000 / 2;
            }

            readTimeArray(flow, wallOffsets);
            readTimeArray(flow, standardOffsets);

            DSTZone tailZone;
            if (flow.characters[flow.pointer++] == 'y') {
                flow.pointer++;
                tailZone = DSTZone.readZone(id, flow);
            } else {
                tailZone = null;
            }

            PrecalculatedZone result = new PrecalculatedZone(id, transitions, wallOffsets, standardOffsets, tailZone);
            return result.isCachable() ? CachedDateTimeZone.forZone(result) : result;
        }

        @Override
        public int getOffset(long instant) {
            long[] transitions = iTransitions;
            int i = Arrays.binarySearch(transitions, instant);
            if (i >= 0) {
                return iWallOffsets[i];
            }
            i = ~i;
            if (i < transitions.length) {
                if (i > 0) {
                    return iWallOffsets[i - 1];
                }
                return 0;
            }
            if (iTailZone == null) {
                return iWallOffsets[i - 1];
            }
            return iTailZone.getOffset(instant);
        }

        @Override
        public int getStandardOffset(long instant) {
            long[] transitions = iTransitions;
            int i = Arrays.binarySearch(transitions, instant);
            if (i >= 0) {
                return iStandardOffsets[i];
            }
            i = ~i;
            if (i < transitions.length) {
                if (i > 0) {
                    return iStandardOffsets[i - 1];
                }
                return 0;
            }
            if (iTailZone == null) {
                return iStandardOffsets[i - 1];
            }
            return iTailZone.getStandardOffset(instant);
        }

        @Override
        public boolean isFixed() {
            return false;
        }

        @Override
        public long nextTransition(long instant) {
            long[] transitions = iTransitions;
            int i = Arrays.binarySearch(transitions, instant);
            i = (i >= 0) ? (i + 1) : ~i;
            if (i < transitions.length) {
                return transitions[i];
            }
            if (iTailZone == null) {
                return instant;
            }
            long end = transitions[transitions.length - 1];
            if (instant < end) {
                instant = end;
            }
            return iTailZone.nextTransition(instant);
        }

        @Override
        public long previousTransition(long instant) {
            long[] transitions = iTransitions;
            int i = Arrays.binarySearch(transitions, instant);
            if (i >= 0) {
                if (instant > Long.MIN_VALUE) {
                    return instant - 1;
                }
                return instant;
            }
            i = ~i;
            if (i < transitions.length) {
                if (i > 0) {
                    long prev = transitions[i - 1];
                    if (prev > Long.MIN_VALUE) {
                        return prev - 1;
                    }
                }
                return instant;
            }
            if (iTailZone != null) {
                long prev = iTailZone.previousTransition(instant);
                if (prev < instant) {
                    return prev;
                }
            }
            long prev = transitions[i - 1];
            if (prev > Long.MIN_VALUE) {
                return prev - 1;
            }
            return instant;
        }

        public boolean isCachable() {
            if (iTailZone != null) {
                return true;
            }
            long[] transitions = iTransitions;
            if (transitions.length <= 1) {
                return false;
            }

            // Add up all the distances between transitions that are less than
            // about two years.
            double distances = 0;
            int count = 0;

            for (int i = 1; i < transitions.length; i++) {
                long diff = transitions[i] - transitions[i - 1];
                if (diff < ((366L + 365) * 24 * 60 * 60 * 1000)) {
                    distances += diff;
                    count++;
                }
            }

            if (count > 0) {
                double avg = distances / count;
                avg /= 24 * 60 * 60 * 1000;
                if (avg >= 25) {
                    // Only bother caching if average distance between
                    // transitions is at least 25 days. Why 25?
                    // CachedDateTimeZone is more efficient if the distance
                    // between transitions is large. With an average of 25, it
                    // will on average perform about 2 tests per cache
                    // hit. (49.7 / 25) is approximately 2.
                    return true;
                }
            }

            return false;
        }
    }
}
