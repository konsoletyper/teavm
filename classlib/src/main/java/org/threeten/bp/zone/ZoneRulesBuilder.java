/*
 *  Copyright 2020 Alexey Andreev.
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
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.threeten.bp.zone;

import static org.threeten.bp.temporal.ChronoField.YEAR;
import static org.threeten.bp.temporal.TemporalAdjusters.nextOrSame;
import static org.threeten.bp.temporal.TemporalAdjusters.previousOrSame;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.threeten.bp.DateTimeException;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.Month;
import org.threeten.bp.Year;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.chrono.IsoChronology;
import org.threeten.bp.zone.ZoneOffsetTransitionRule.TimeDefinition;

/**
 * A mutable builder used to create all the rules for a historic time-zone.
 * <p>
 * The rules of a time-zone describe how the offset changes over time.
 * The rules are created by building windows on the time-line within which
 * the different rules apply. The rules may be one of two kinds:
 * <p><ul>
 * <li>Fixed savings - A single fixed amount of savings from the standard offset will apply.</li>
 * <li>Rules - A set of one or more rules describe how daylight savings changes during the window.</li>
 * </ul><p>
 *
 * <h3>Specification for implementors</h3>
 * This class is a mutable builder used to create zone instances.
 * It must only be used from a single thread.
 * The created instances are immutable and thread-safe.
 */
class ZoneRulesBuilder {

    /**
     * The list of windows.
     */
    private List<TZWindow> windowList = new ArrayList<ZoneRulesBuilder.TZWindow>();
    /**
     * A map for deduplicating the output.
     */
    private Map<Object, Object> deduplicateMap;

    //-----------------------------------------------------------------------
    /**
     * Constructs an instance of the builder that can be used to create zone rules.
     * <p>
     * The builder is used by adding one or more windows representing portions
     * of the time-line. The standard offset from UTC/Greenwich will be constant
     * within a window, although two adjacent windows can have the same standard offset.
     * <p>
     * Within each window, there can either be a
     * {@link #setFixedSavingsToWindow fixed savings amount} or a
     * {@link #addRuleToWindow list of rules}.
     */
    public ZoneRulesBuilder() {
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a window to the builder that can be used to filter a set of rules.
     * <p>
     * This method defines and adds a window to the zone where the standard offset is specified.
     * The window limits the effect of subsequent additions of transition rules
     * or fixed savings. If neither rules or fixed savings are added to the window
     * then the window will default to no savings.
     * <p>
     * Each window must be added sequentially, as the start instant of the window
     * is derived from the until instant of the previous window.
     *
     * @param standardOffset  the standard offset, not null
     * @param until  the date-time that the offset applies until, not null
     * @param untilDefinition  the time type for the until date-time, not null
     * @return this, for chaining
     * @throws IllegalStateException if the window order is invalid
     */
    public ZoneRulesBuilder addWindow(
            ZoneOffset standardOffset,
            LocalDateTime until,
            TimeDefinition untilDefinition) {
        Objects.requireNonNull(standardOffset, "standardOffset");
        Objects.requireNonNull(until, "until");
        Objects.requireNonNull(untilDefinition, "untilDefinition");
        TZWindow window = new TZWindow(standardOffset, until, untilDefinition);
        if (windowList.size() > 0) {
            TZWindow previous = windowList.get(windowList.size() - 1);
            window.validateWindowOrder(previous);
        }
        windowList.add(window);
        return this;
    }

    /**
     * Adds a window that applies until the end of time to the builder that can be
     * used to filter a set of rules.
     * <p>
     * This method defines and adds a window to the zone where the standard offset is specified.
     * The window limits the effect of subsequent additions of transition rules
     * or fixed savings. If neither rules or fixed savings are added to the window
     * then the window will default to no savings.
     * <p>
     * This must be added after all other windows.
     * No more windows can be added after this one.
     *
     * @param standardOffset  the standard offset, not null
     * @return this, for chaining
     * @throws IllegalStateException if a forever window has already been added
     */
    public ZoneRulesBuilder addWindowForever(ZoneOffset standardOffset) {
        return addWindow(standardOffset, LocalDateTime.MAX, TimeDefinition.WALL);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the previously added window to have fixed savings.
     * <p>
     * Setting a window to have fixed savings simply means that a single daylight
     * savings amount applies throughout the window. The window could be small,
     * such as a single summer, or large, such as a multi-year daylight savings.
     * <p>
     * A window can either have fixed savings or rules but not both.
     *
     * @param fixedSavingAmountSecs  the amount of saving to use for the whole window, not null
     * @return this, for chaining
     * @throws IllegalStateException if no window has yet been added
     * @throws IllegalStateException if the window already has rules
     */
    public ZoneRulesBuilder setFixedSavingsToWindow(int fixedSavingAmountSecs) {
        if (windowList.isEmpty()) {
            throw new IllegalStateException("Must add a window before setting the fixed savings");
        }
        TZWindow window = windowList.get(windowList.size() - 1);
        window.setFixedSavings(fixedSavingAmountSecs);
        return this;
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a single transition rule to the current window.
     * <p>
     * This adds a rule such that the offset, expressed as a daylight savings amount,
     * changes at the specified date-time.
     *
     * @param transitionDateTime  the date-time that the transition occurs as defined by timeDefintion, not null
     * @param timeDefinition  the definition of how to convert local to actual time, not null
     * @param savingAmountSecs  the amount of saving from the standard offset after the transition in seconds
     * @return this, for chaining
     * @throws IllegalStateException if no window has yet been added
     * @throws IllegalStateException if the window already has fixed savings
     * @throws IllegalStateException if the window has reached the maximum capacity of 2000 rules
     */
    public ZoneRulesBuilder addRuleToWindow(
            LocalDateTime transitionDateTime,
            TimeDefinition timeDefinition,
            int savingAmountSecs) {
        Objects.requireNonNull(transitionDateTime, "transitionDateTime");
        return addRuleToWindow(
                transitionDateTime.getYear(), transitionDateTime.getYear(),
                transitionDateTime.getMonth(), transitionDateTime.getDayOfMonth(),
                null, transitionDateTime.toLocalTime(), false, timeDefinition, savingAmountSecs);
    }

    /**
     * Adds a single transition rule to the current window.
     * <p>
     * This adds a rule such that the offset, expressed as a daylight savings amount,
     * changes at the specified date-time.
     *
     * @param year  the year of the transition, from MIN_VALUE to MAX_VALUE
     * @param month  the month of the transition, not null
     * @param dayOfMonthIndicator  the day-of-month of the transition, adjusted by dayOfWeek,
     *   from 1 to 31 adjusted later, or -1 to -28 adjusted earlier from the last day of the month
     * @param time  the time that the transition occurs as defined by timeDefintion, not null
     * @param timeEndOfDay  whether midnight is at the end of day
     * @param timeDefinition  the definition of how to convert local to actual time, not null
     * @param savingAmountSecs  the amount of saving from the standard offset after the transition in seconds
     * @return this, for chaining
     * @throws DateTimeException if a date-time field is out of range
     * @throws IllegalStateException if no window has yet been added
     * @throws IllegalStateException if the window already has fixed savings
     * @throws IllegalStateException if the window has reached the maximum capacity of 2000 rules
     */
    public ZoneRulesBuilder addRuleToWindow(
            int year,
            Month month,
            int dayOfMonthIndicator,
            LocalTime time,
            boolean timeEndOfDay,
            TimeDefinition timeDefinition,
            int savingAmountSecs) {
        return addRuleToWindow(year, year, month, dayOfMonthIndicator, null, time, timeEndOfDay,
                timeDefinition, savingAmountSecs);
    }

    /**
     * Adds a multi-year transition rule to the current window.
     * <p>
     * This adds a rule such that the offset, expressed as a daylight savings amount,
     * changes at the specified date-time for each year in the range.
     *
     * @param startYear  the start year of the rule, from MIN_VALUE to MAX_VALUE
     * @param endYear  the end year of the rule, from MIN_VALUE to MAX_VALUE
     * @param month  the month of the transition, not null
     * @param dayOfMonthIndicator  the day-of-month of the transition, adjusted by dayOfWeek,
     *   from 1 to 31 adjusted later, or -1 to -28 adjusted earlier from the last day of the month
     * @param dayOfWeek  the day-of-week to adjust to, null if day-of-month should not be adjusted
     * @param time  the time that the transition occurs as defined by timeDefintion, not null
     * @param timeEndOfDay  whether midnight is at the end of day
     * @param timeDefinition  the definition of how to convert local to actual time, not null
     * @param savingAmountSecs  the amount of saving from the standard offset after the transition in seconds
     * @return this, for chaining
     * @throws DateTimeException if a date-time field is out of range
     * @throws IllegalArgumentException if the day of month indicator is invalid
     * @throws IllegalArgumentException if the end of day midnight flag does not match the time
     * @throws IllegalStateException if no window has yet been added
     * @throws IllegalStateException if the window already has fixed savings
     * @throws IllegalStateException if the window has reached the maximum capacity of 2000 rules
     */
    public ZoneRulesBuilder addRuleToWindow(
            int startYear,
            int endYear,
            Month month,
            int dayOfMonthIndicator,
            DayOfWeek dayOfWeek,
            LocalTime time,
            boolean timeEndOfDay,
            TimeDefinition timeDefinition,
            int savingAmountSecs) {
        Objects.requireNonNull(month, "month");
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(timeDefinition, "timeDefinition");
        YEAR.checkValidValue(startYear);
        YEAR.checkValidValue(endYear);
        if (dayOfMonthIndicator < -28 || dayOfMonthIndicator > 31 || dayOfMonthIndicator == 0) {
            throw new IllegalArgumentException("Day of month indicator must be between -28 and 31 "
                    + "inclusive excluding zero");
        }
        if (timeEndOfDay && !time.equals(LocalTime.MIDNIGHT)) {
            throw new IllegalArgumentException("Time must be midnight when end of day flag is true");
        }
        if (windowList.isEmpty()) {
            throw new IllegalStateException("Must add a window before adding a rule");
        }
        TZWindow window = windowList.get(windowList.size() - 1);
        window.addRule(startYear, endYear, month, dayOfMonthIndicator, dayOfWeek, time, timeEndOfDay ? 1 : 0,
                timeDefinition, savingAmountSecs);
        return this;
    }

    ZoneRulesBuilder addRuleToWindow(
            int startYear,
            int endYear,
            Month month,
            int dayOfMonthIndicator,
            DayOfWeek dayOfWeek,
            LocalTime time,
            int adjustDays,
            TimeDefinition timeDefinition,
            int savingAmountSecs) {
        Objects.requireNonNull(month, "month");
        Objects.requireNonNull(timeDefinition, "timeDefinition");
        YEAR.checkValidValue(startYear);
        YEAR.checkValidValue(endYear);
        if (dayOfMonthIndicator < -28 || dayOfMonthIndicator > 31 || dayOfMonthIndicator == 0) {
            throw new IllegalArgumentException("Day of month indicator must be between -28 and 31 "
                    + "inclusive excluding zero");
        }
        if (windowList.isEmpty()) {
            throw new IllegalStateException("Must add a window before adding a rule");
        }
        TZWindow window = windowList.get(windowList.size() - 1);
        window.addRule(startYear, endYear, month, dayOfMonthIndicator, dayOfWeek, time, adjustDays,
                timeDefinition, savingAmountSecs);
        return this;
    }

    //-----------------------------------------------------------------------
    /**
     * Completes the build converting the builder to a set of time-zone rules.
     * <p>
     * Calling this method alters the state of the builder.
     * Further rules should not be added to this builder once this method is called.
     *
     * @param zoneId  the time-zone ID, not null
     * @return the zone rules, not null
     * @throws IllegalStateException if no windows have been added
     * @throws IllegalStateException if there is only one rule defined as being forever for any given window
     */
    public ZoneRules toRules(String zoneId) {
        return toRules(zoneId, new HashMap<Object, Object>());
    }

    /**
     * Completes the build converting the builder to a set of time-zone rules.
     * <p>
     * Calling this method alters the state of the builder.
     * Further rules should not be added to this builder once this method is called.
     *
     * @param zoneId  the time-zone ID, not null
     * @param deduplicateMap  a map for deduplicating the values, not null
     * @return the zone rules, not null
     * @throws IllegalStateException if no windows have been added
     * @throws IllegalStateException if there is only one rule defined as being forever for any given window
     */
    ZoneRules toRules(String zoneId, Map<Object, Object> deduplicateMap) {
        Objects.requireNonNull(zoneId, "zoneId");
        this.deduplicateMap = deduplicateMap;
        if (windowList.isEmpty()) {
            throw new IllegalStateException("No windows have been added to the builder");
        }

        final List<ZoneOffsetTransition> standardTransitionList = new ArrayList<ZoneOffsetTransition>(4);
        final List<ZoneOffsetTransition> transitionList = new ArrayList<ZoneOffsetTransition>(256);
        final List<ZoneOffsetTransitionRule> lastTransitionRuleList = new ArrayList<ZoneOffsetTransitionRule>(2);

        // initialize the standard offset calculation
        final TZWindow firstWindow = windowList.get(0);
        ZoneOffset loopStandardOffset = firstWindow.standardOffset;
        int loopSavings = 0;
        if (firstWindow.fixedSavingAmountSecs != null) {
            loopSavings = firstWindow.fixedSavingAmountSecs;
        }
        final ZoneOffset firstWallOffset = deduplicate(ZoneOffset.ofTotalSeconds(
                loopStandardOffset.getTotalSeconds() + loopSavings));
        LocalDateTime loopWindowStart = deduplicate(LocalDateTime.of(Year.MIN_VALUE, 1, 1, 0, 0));
        ZoneOffset loopWindowOffset = firstWallOffset;

        // build the windows and rules to interesting data
        for (TZWindow window : windowList) {
            // tidy the state
            window.tidy(loopWindowStart.getYear());

            // calculate effective savings at the start of the window
            Integer effectiveSavings = window.fixedSavingAmountSecs;
            if (effectiveSavings == null) {
                // apply rules from this window together with the standard offset and
                // savings from the last window to find the savings amount applicable
                // at start of this window
                effectiveSavings = 0;
                for (TZRule rule : window.ruleList) {
                    ZoneOffsetTransition trans = rule.toTransition(loopStandardOffset, loopSavings);
                    if (trans.toEpochSecond() > loopWindowStart.toEpochSecond(loopWindowOffset)) {
                        // previous savings amount found, which could be the savings amount at
                        // the instant that the window starts (hence isAfter)
                        break;
                    }
                    effectiveSavings = rule.savingAmountSecs;
                }
            }

            // check if standard offset changed, and update it
            if (!loopStandardOffset.equals(window.standardOffset)) {
                standardTransitionList.add(deduplicate(
                    new ZoneOffsetTransition(
                        LocalDateTime.ofEpochSecond(loopWindowStart.toEpochSecond(loopWindowOffset), 0,
                                loopStandardOffset),
                        loopStandardOffset, window.standardOffset)));
                loopStandardOffset = deduplicate(window.standardOffset);
            }

            // check if the start of the window represents a transition
            ZoneOffset effectiveWallOffset = deduplicate(ZoneOffset.ofTotalSeconds(
                    loopStandardOffset.getTotalSeconds() + effectiveSavings));
            if (!loopWindowOffset.equals(effectiveWallOffset)) {
                ZoneOffsetTransition trans = deduplicate(
                    new ZoneOffsetTransition(loopWindowStart, loopWindowOffset, effectiveWallOffset));
                transitionList.add(trans);
            }
            loopSavings = effectiveSavings;

            // apply rules within the window
            for (TZRule rule : window.ruleList) {
                ZoneOffsetTransition trans = deduplicate(rule.toTransition(loopStandardOffset, loopSavings));
                if (trans.toEpochSecond() >= loopWindowStart.toEpochSecond(loopWindowOffset)
                        && trans.toEpochSecond() < window.createDateTimeEpochSecond(loopSavings)
                        && !trans.getOffsetBefore().equals(trans.getOffsetAfter())) {
                    transitionList.add(trans);
                    loopSavings = rule.savingAmountSecs;
                }
            }

            // calculate last rules
            for (TZRule lastRule : window.lastRuleList) {
                ZoneOffsetTransitionRule transitionRule = deduplicate(lastRule.toTransitionRule(loopStandardOffset,
                        loopSavings));
                lastTransitionRuleList.add(transitionRule);
                loopSavings = lastRule.savingAmountSecs;
            }

            // finally we can calculate the true end of the window, passing it to the next window
            loopWindowOffset = deduplicate(window.createWallOffset(loopSavings));
            loopWindowStart = deduplicate(LocalDateTime.ofEpochSecond(
                    window.createDateTimeEpochSecond(loopSavings), 0, loopWindowOffset));
        }
        return new StandardZoneRules(
                firstWindow.standardOffset, firstWallOffset, standardTransitionList,
                transitionList, lastTransitionRuleList);
    }

    /**
     * Deduplicates an object instance.
     *
     * @param <T> the generic type
     * @param object  the object to deduplicate
     * @return the deduplicated object
     */
    @SuppressWarnings("unchecked")
    <T> T deduplicate(T object) {
        if (!deduplicateMap.containsKey(object)) {
            deduplicateMap.put(object, object);
        }
        return (T) deduplicateMap.get(object);
    }

    //-----------------------------------------------------------------------
    /**
     * A definition of a window in the time-line.
     * The window will have one standard offset and will either have a
     * fixed DST savings or a set of rules.
     */
    class TZWindow {
        /** The standard offset during the window, not null. */
        private final ZoneOffset standardOffset;
        /** The end local time, not null. */
        private final LocalDateTime windowEnd;
        /** The type of the end time, not null. */
        private final TimeDefinition timeDefinition;

        /** The fixed amount of the saving to be applied during this window. */
        private Integer fixedSavingAmountSecs;
        /** The rules for the current window. */
        private List<TZRule> ruleList = new ArrayList<TZRule>();
        /** The latest year that the last year starts at. */
        private int maxLastRuleStartYear = Year.MIN_VALUE;
        /** The last rules. */
        private List<TZRule> lastRuleList = new ArrayList<TZRule>();

        /**
         * Constructor.
         *
         * @param standardOffset  the standard offset applicable during the window, not null
         * @param windowEnd  the end of the window, relative to the time definition, null if forever
         * @param timeDefinition  the time definition for calculating the true end, not null
         */
        TZWindow(
                ZoneOffset standardOffset,
                LocalDateTime windowEnd,
                TimeDefinition timeDefinition) {
            super();
            this.windowEnd = windowEnd;
            this.timeDefinition = timeDefinition;
            this.standardOffset = standardOffset;
        }

        /**
         * Sets the fixed savings amount for the window.
         *
         * @param fixedSavingAmount  the amount of daylight saving to apply throughout the window, may be null
         * @throws IllegalStateException if the window already has rules
         */
        void setFixedSavings(int fixedSavingAmount) {
            if (ruleList.size() > 0 || lastRuleList.size() > 0) {
                throw new IllegalStateException("Window has DST rules, so cannot have fixed savings");
            }
            this.fixedSavingAmountSecs = fixedSavingAmount;
        }

        /**
         * Adds a rule to the current window.
         *
         * @param startYear  the start year of the rule, from MIN_VALUE to MAX_VALUE
         * @param endYear  the end year of the rule, from MIN_VALUE to MAX_VALUE
         * @param month  the month of the transition, not null
         * @param dayOfMonthIndicator  the day-of-month of the transition, adjusted by dayOfWeek,
         *   from 1 to 31 adjusted later, or -1 to -28 adjusted earlier from the last day of the month
         * @param dayOfWeek  the day-of-week to adjust to, null if day-of-month should not be adjusted
         * @param time  the time that the transition occurs as defined by timeDefintion, not null
         * @param adjustDays  the time days adjustment
         * @param timeDefinition  the definition of how to convert local to actual time, not null
         * @param savingAmountSecs  the amount of saving from the standard offset in seconds
         * @throws IllegalStateException if the window already has fixed savings
         * @throws IllegalStateException if the window has reached the maximum capacity of 2000 rules
         */
        void addRule(
                int startYear,
                int endYear,
                Month month,
                int dayOfMonthIndicator,
                DayOfWeek dayOfWeek,
                LocalTime time,
                int adjustDays,
                TimeDefinition timeDefinition,
                int savingAmountSecs) {

            if (fixedSavingAmountSecs != null) {
                throw new IllegalStateException("Window has a fixed DST saving, so cannot have DST rules");
            }
            if (ruleList.size() >= 2000) {
                throw new IllegalStateException("Window has reached the maximum number of allowed rules");
            }
            boolean lastRule = false;
            if (endYear == Year.MAX_VALUE) {
                lastRule = true;
                endYear = startYear;
            }
            int year = startYear;
            while (year <= endYear) {
                TZRule rule = new TZRule(year, month, dayOfMonthIndicator, dayOfWeek, time, adjustDays,
                        timeDefinition, savingAmountSecs);
                if (lastRule) {
                    lastRuleList.add(rule);
                    maxLastRuleStartYear = Math.max(startYear, maxLastRuleStartYear);
                } else {
                    ruleList.add(rule);
                }
                year++;
            }
        }

        /**
         * Validates that this window is after the previous one.
         *
         * @param previous  the previous window, not null
         * @throws IllegalStateException if the window order is invalid
         */
        void validateWindowOrder(TZWindow previous) {
            if (windowEnd.isBefore(previous.windowEnd)) {
                throw new IllegalStateException("Windows must be added in date-time order: "
                        + windowEnd + " < " + previous.windowEnd);
            }
        }

        /**
         * Adds rules to make the last rules all start from the same year.
         * Also add one more year to avoid weird case where penultimate year has odd offset.
         *
         * @param windowStartYear  the window start year
         * @throws IllegalStateException if there is only one rule defined as being forever
         */
        void tidy(int windowStartYear) {
            if (lastRuleList.size() == 1) {
                throw new IllegalStateException("Cannot have only one rule defined as being forever");
            }

            // handle last rules
            if (windowEnd.equals(LocalDateTime.MAX)) {
                // setup at least one real rule, which closes off other windows nicely
                maxLastRuleStartYear = Math.max(maxLastRuleStartYear, windowStartYear) + 1;
                for (TZRule lastRule : lastRuleList) {
                    addRule(lastRule.year, maxLastRuleStartYear, lastRule.month, lastRule.dayOfMonthIndicator,
                        lastRule.dayOfWeek, lastRule.time, lastRule.adjustDays, lastRule.timeDefinition,
                            lastRule.savingAmountSecs);
                    lastRule.year = maxLastRuleStartYear + 1;
                }
                if (maxLastRuleStartYear == Year.MAX_VALUE) {
                    lastRuleList.clear();
                } else {
                    maxLastRuleStartYear++;
                }
            } else {
                // convert all within the endYear limit
                int endYear = windowEnd.getYear();
                for (TZRule lastRule : lastRuleList) {
                    addRule(lastRule.year, endYear + 1, lastRule.month, lastRule.dayOfMonthIndicator,
                        lastRule.dayOfWeek, lastRule.time, lastRule.adjustDays, lastRule.timeDefinition,
                            lastRule.savingAmountSecs);
                }
                lastRuleList.clear();
                maxLastRuleStartYear = Year.MAX_VALUE;
            }

            // ensure lists are sorted
            Collections.sort(ruleList);
            Collections.sort(lastRuleList);

            // default fixed savings to zero
            if (ruleList.size() == 0 && fixedSavingAmountSecs == null) {
                fixedSavingAmountSecs = 0;
            }
        }

        /**
         * Checks if the window is empty.
         *
         * @return true if the window is only a standard offset
         */
        boolean isSingleWindowStandardOffset() {
            return windowEnd.equals(LocalDateTime.MAX) && timeDefinition == TimeDefinition.WALL
                    && fixedSavingAmountSecs == null && lastRuleList.isEmpty() && ruleList.isEmpty();
        }

        /**
         * Creates the wall offset for the local date-time at the end of the window.
         *
         * @param savingsSecs  the amount of savings in use in seconds
         * @return the created date-time epoch second in the wall offset, not null
         */
        ZoneOffset createWallOffset(int savingsSecs) {
            return ZoneOffset.ofTotalSeconds(standardOffset.getTotalSeconds() + savingsSecs);
        }

        /**
         * Creates the offset date-time for the local date-time at the end of the window.
         *
         * @param savingsSecs  the amount of savings in use in seconds
         * @return the created date-time epoch second in the wall offset, not null
         */
        long createDateTimeEpochSecond(int savingsSecs) {
            ZoneOffset wallOffset = createWallOffset(savingsSecs);
            LocalDateTime ldt = timeDefinition.createDateTime(windowEnd, standardOffset, wallOffset);
            return ldt.toEpochSecond(wallOffset);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * A definition of the way a local time can be converted to an offset time.
     */
    class TZRule implements Comparable<TZRule> {
        /** The year. */
        private int year;
        /** The month. */
        private Month month;
        /** The day-of-month. */
        private int dayOfMonthIndicator;
        /** The day-of-month. */
        private DayOfWeek dayOfWeek;
        /** The local time. */
        private LocalTime time;
        /** The local time days adjustment. */
        private int adjustDays;
        /** The type of the time. */
        private TimeDefinition timeDefinition;
        /** The amount of the saving to be applied after this point. */
        private int savingAmountSecs;

        /**
         * Constructor.
         *
         * @param year  the year
         * @param month  the month, not null
         * @param dayOfMonthIndicator  the day-of-month of the transition, adjusted by dayOfWeek,
         *   from 1 to 31 adjusted later, or -1 to -28 adjusted earlier from the last day of the month
         * @param dayOfWeek  the day-of-week, null if day-of-month is exact
         * @param time  the time, not null
         * @param adjustDays  the time day adjustment
         * @param timeDefinition  the time definition, not null
         * @param savingAfterSecs  the savings amount in seconds
         */
        TZRule(int year, Month month, int dayOfMonthIndicator,
                DayOfWeek dayOfWeek, LocalTime time, int adjustDays,
                TimeDefinition timeDefinition, int savingAfterSecs) {
            super();
            this.year = year;
            this.month = month;
            this.dayOfMonthIndicator = dayOfMonthIndicator;
            this.dayOfWeek = dayOfWeek;
            this.time = time;
            this.adjustDays = adjustDays;
            this.timeDefinition = timeDefinition;
            this.savingAmountSecs = savingAfterSecs;
        }

        /**
         * Converts this to a transition.
         *
         * @param standardOffset  the active standard offset, not null
         * @param savingsBeforeSecs  the active savings in seconds
         * @return the transition, not null
         */
        ZoneOffsetTransition toTransition(ZoneOffset standardOffset, int savingsBeforeSecs) {
            // copy of code in ZoneOffsetTransitionRule to avoid infinite loop
            LocalDate date = toLocalDate();
            date = deduplicate(date);
            LocalDateTime ldt = deduplicate(LocalDateTime.of(date.plusDays(adjustDays), time));
            ZoneOffset wallOffset = deduplicate(ZoneOffset.ofTotalSeconds(standardOffset.getTotalSeconds()
                    + savingsBeforeSecs));
            LocalDateTime dt = deduplicate(timeDefinition.createDateTime(ldt, standardOffset, wallOffset));
            ZoneOffset offsetAfter = deduplicate(ZoneOffset.ofTotalSeconds(standardOffset.getTotalSeconds()
                    + savingAmountSecs));
            return new ZoneOffsetTransition(dt, wallOffset, offsetAfter);
        }

        /**
         * Converts this to a transition rule.
         *
         * @param standardOffset  the active standard offset, not null
         * @param savingsBeforeSecs  the active savings before the transition in seconds
         * @return the transition, not null
         */
        ZoneOffsetTransitionRule toTransitionRule(ZoneOffset standardOffset, int savingsBeforeSecs) {
            // optimize stored format
            if (dayOfMonthIndicator < 0) {
                if (month != Month.FEBRUARY) {
                    dayOfMonthIndicator = month.maxLength() - 6;
                }
            }

            // build rule
            ZoneOffsetTransition trans = toTransition(standardOffset, savingsBeforeSecs);
            return new ZoneOffsetTransitionRule(
                    month, dayOfMonthIndicator, dayOfWeek, time, adjustDays, timeDefinition,
                    standardOffset, trans.getOffsetBefore(), trans.getOffsetAfter());
        }

        @Override
        public int compareTo(TZRule other) {
            int cmp = year - other.year;
            cmp = cmp == 0 ? month.compareTo(other.month) : cmp;
            if (cmp == 0) {
                // convert to date to handle dow/domIndicator/timeEndOfDay
                LocalDate thisDate = toLocalDate();
                LocalDate otherDate = other.toLocalDate();
                cmp = thisDate.compareTo(otherDate);
            }
            if (cmp != 0) {
                return cmp;
            }
            long timeSecs1 = time.toSecondOfDay() + adjustDays * 86400;
            long timeSecs2 = other.time.toSecondOfDay() + other.adjustDays * 86400;
            return Long.compare(timeSecs1, timeSecs2);
        }

        private LocalDate toLocalDate() {
            LocalDate date;
            if (dayOfMonthIndicator < 0) {
                int monthLen = month.length(IsoChronology.INSTANCE.isLeapYear(year));
                date = LocalDate.of(year, month, monthLen + 1 + dayOfMonthIndicator);
                if (dayOfWeek != null) {
                    date = date.with(previousOrSame(dayOfWeek));
                }
            } else {
                date = LocalDate.of(year, month, dayOfMonthIndicator);
                if (dayOfWeek != null) {
                    date = date.with(nextOrSame(dayOfWeek));
                }
            }
            return date;
        }

        // no equals() or hashCode()
    }

}
