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
package org.teavm.classlib.java.time.zone;

import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;
import static org.teavm.classlib.java.time.temporal.TTemporalAdjusters.nextOrSame;
import static org.teavm.classlib.java.time.temporal.TTemporalAdjusters.previousOrSame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teavm.classlib.java.time.TDayOfWeek;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TMonth;
import org.teavm.classlib.java.time.TYear;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;
import org.teavm.classlib.java.time.zone.TZoneOffsetTransitionRule.TimeDefinition;

class TZoneRulesBuilder {

    private List<TZWindow> windowList = new ArrayList<TZoneRulesBuilder.TZWindow>();

    private Map<Object, Object> deduplicateMap;

    public TZoneRulesBuilder() {

    }

    public TZoneRulesBuilder addWindow(TZoneOffset standardOffset, TLocalDateTime until,
            TimeDefinition untilDefinition) {

        TJdk8Methods.requireNonNull(standardOffset, "standardOffset");
        TJdk8Methods.requireNonNull(until, "until");
        TJdk8Methods.requireNonNull(untilDefinition, "untilDefinition");
        TZWindow window = new TZWindow(standardOffset, until, untilDefinition);
        if (this.windowList.size() > 0) {
            TZWindow previous = this.windowList.get(this.windowList.size() - 1);
            window.validateWindowOrder(previous);
        }
        this.windowList.add(window);
        return this;
    }

    public TZoneRulesBuilder addWindowForever(TZoneOffset standardOffset) {

        return addWindow(standardOffset, TLocalDateTime.MAX, TimeDefinition.WALL);
    }

    public TZoneRulesBuilder setFixedSavingsToWindow(int fixedSavingAmountSecs) {

        if (this.windowList.isEmpty()) {
            throw new IllegalStateException("Must add a window before setting the fixed savings");
        }
        TZWindow window = this.windowList.get(this.windowList.size() - 1);
        window.setFixedSavings(fixedSavingAmountSecs);
        return this;
    }

    public TZoneRulesBuilder addRuleToWindow(TLocalDateTime transitionDateTime, TimeDefinition timeDefinition,
            int savingAmountSecs) {

        TJdk8Methods.requireNonNull(transitionDateTime, "transitionDateTime");
        return addRuleToWindow(transitionDateTime.getYear(), transitionDateTime.getYear(),
                transitionDateTime.getMonth(), transitionDateTime.getDayOfMonth(), null,
                transitionDateTime.toLocalTime(), false, timeDefinition, savingAmountSecs);
    }

    public TZoneRulesBuilder addRuleToWindow(int year, TMonth month, int dayOfMonthIndicator, TLocalTime time,
            boolean timeEndOfDay, TimeDefinition timeDefinition, int savingAmountSecs) {

        return addRuleToWindow(year, year, month, dayOfMonthIndicator, null, time, timeEndOfDay, timeDefinition,
                savingAmountSecs);
    }

    public TZoneRulesBuilder addRuleToWindow(int startYear, int endYear, TMonth month, int dayOfMonthIndicator,
            TDayOfWeek dayOfWeek, TLocalTime time, boolean timeEndOfDay, TimeDefinition timeDefinition,
            int savingAmountSecs) {

        TJdk8Methods.requireNonNull(month, "month");
        TJdk8Methods.requireNonNull(time, "time");
        TJdk8Methods.requireNonNull(timeDefinition, "timeDefinition");
        YEAR.checkValidValue(startYear);
        YEAR.checkValidValue(endYear);
        if (dayOfMonthIndicator < -28 || dayOfMonthIndicator > 31 || dayOfMonthIndicator == 0) {
            throw new IllegalArgumentException(
                    "Day of month indicator must be between -28 and 31 inclusive excluding zero");
        }
        if (timeEndOfDay && time.equals(TLocalTime.MIDNIGHT) == false) {
            throw new IllegalArgumentException("Time must be midnight when end of day flag is true");
        }
        if (this.windowList.isEmpty()) {
            throw new IllegalStateException("Must add a window before adding a rule");
        }
        TZWindow window = this.windowList.get(this.windowList.size() - 1);
        window.addRule(startYear, endYear, month, dayOfMonthIndicator, dayOfWeek, time, timeEndOfDay ? 1 : 0,
                timeDefinition, savingAmountSecs);
        return this;
    }

    TZoneRulesBuilder addRuleToWindow(int startYear, int endYear, TMonth month, int dayOfMonthIndicator,
            TDayOfWeek dayOfWeek, TLocalTime time, int adjustDays, TimeDefinition timeDefinition,
            int savingAmountSecs) {

        TJdk8Methods.requireNonNull(month, "month");
        TJdk8Methods.requireNonNull(timeDefinition, "timeDefinition");
        YEAR.checkValidValue(startYear);
        YEAR.checkValidValue(endYear);
        if (dayOfMonthIndicator < -28 || dayOfMonthIndicator > 31 || dayOfMonthIndicator == 0) {
            throw new IllegalArgumentException(
                    "Day of month indicator must be between -28 and 31 inclusive excluding zero");
        }
        if (this.windowList.isEmpty()) {
            throw new IllegalStateException("Must add a window before adding a rule");
        }
        TZWindow window = this.windowList.get(this.windowList.size() - 1);
        window.addRule(startYear, endYear, month, dayOfMonthIndicator, dayOfWeek, time, adjustDays, timeDefinition,
                savingAmountSecs);
        return this;
    }

    public TZoneRules toRules(String zoneId) {

        return toRules(zoneId, new HashMap<Object, Object>());
    }

    TZoneRules toRules(String zoneId, Map<Object, Object> deduplicateMap) {

        TJdk8Methods.requireNonNull(zoneId, "zoneId");
        this.deduplicateMap = deduplicateMap;
        if (this.windowList.isEmpty()) {
            throw new IllegalStateException("No windows have been added to the builder");
        }

        final List<TZoneOffsetTransition> standardTransitionList = new ArrayList<TZoneOffsetTransition>(4);
        final List<TZoneOffsetTransition> transitionList = new ArrayList<TZoneOffsetTransition>(256);
        final List<TZoneOffsetTransitionRule> lastTransitionRuleList = new ArrayList<TZoneOffsetTransitionRule>(2);

        // initialize the standard offset calculation
        final TZWindow firstWindow = this.windowList.get(0);
        TZoneOffset loopStandardOffset = firstWindow.standardOffset;
        int loopSavings = 0;
        if (firstWindow.fixedSavingAmountSecs != null) {
            loopSavings = firstWindow.fixedSavingAmountSecs;
        }
        final TZoneOffset firstWallOffset = deduplicate(
                TZoneOffset.ofTotalSeconds(loopStandardOffset.getTotalSeconds() + loopSavings));
        TLocalDateTime loopWindowStart = deduplicate(TLocalDateTime.of(TYear.MIN_VALUE, 1, 1, 0, 0));
        TZoneOffset loopWindowOffset = firstWallOffset;

        // build the windows and rules to interesting data
        for (TZWindow window : this.windowList) {
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
                    TZoneOffsetTransition trans = rule.toTransition(loopStandardOffset, loopSavings);
                    if (trans.toEpochSecond() > loopWindowStart.toEpochSecond(loopWindowOffset)) {
                        // previous savings amount found, which could be the savings amount at
                        // the instant that the window starts (hence isAfter)
                        break;
                    }
                    effectiveSavings = rule.savingAmountSecs;
                }
            }

            // check if standard offset changed, and update it
            if (loopStandardOffset.equals(window.standardOffset) == false) {
                standardTransitionList.add(deduplicate(new TZoneOffsetTransition(TLocalDateTime
                        .ofEpochSecond(loopWindowStart.toEpochSecond(loopWindowOffset), 0, loopStandardOffset),
                        loopStandardOffset, window.standardOffset)));
                loopStandardOffset = deduplicate(window.standardOffset);
            }

            // check if the start of the window represents a transition
            TZoneOffset effectiveWallOffset = deduplicate(
                    TZoneOffset.ofTotalSeconds(loopStandardOffset.getTotalSeconds() + effectiveSavings));
            if (loopWindowOffset.equals(effectiveWallOffset) == false) {
                TZoneOffsetTransition trans = deduplicate(
                        new TZoneOffsetTransition(loopWindowStart, loopWindowOffset, effectiveWallOffset));
                transitionList.add(trans);
            }
            loopSavings = effectiveSavings;

            // apply rules within the window
            for (TZRule rule : window.ruleList) {
                TZoneOffsetTransition trans = deduplicate(rule.toTransition(loopStandardOffset, loopSavings));
                if (trans.toEpochSecond() < loopWindowStart.toEpochSecond(loopWindowOffset) == false
                        && trans.toEpochSecond() < window.createDateTimeEpochSecond(loopSavings)
                        && trans.getOffsetBefore().equals(trans.getOffsetAfter()) == false) {
                    transitionList.add(trans);
                    loopSavings = rule.savingAmountSecs;
                }
            }

            // calculate last rules
            for (TZRule lastRule : window.lastRuleList) {
                TZoneOffsetTransitionRule transitionRule = deduplicate(
                        lastRule.toTransitionRule(loopStandardOffset, loopSavings));
                lastTransitionRuleList.add(transitionRule);
                loopSavings = lastRule.savingAmountSecs;
            }

            // finally we can calculate the true end of the window, passing it to the next window
            loopWindowOffset = deduplicate(window.createWallOffset(loopSavings));
            loopWindowStart = deduplicate(
                    TLocalDateTime.ofEpochSecond(window.createDateTimeEpochSecond(loopSavings), 0, loopWindowOffset));
        }
        return new TStandardZoneRules(firstWindow.standardOffset, firstWallOffset, standardTransitionList,
                transitionList, lastTransitionRuleList);
    }

    @SuppressWarnings("unchecked")
    <T> T deduplicate(T object) {

        if (this.deduplicateMap.containsKey(object) == false) {
            this.deduplicateMap.put(object, object);
        }
        return (T) this.deduplicateMap.get(object);
    }

    class TZWindow {
        private final TZoneOffset standardOffset;

        private final TLocalDateTime windowEnd;

        private final TimeDefinition timeDefinition;

        private Integer fixedSavingAmountSecs;

        private List<TZRule> ruleList = new ArrayList<TZRule>();

        private int maxLastRuleStartYear = TYear.MIN_VALUE;

        private List<TZRule> lastRuleList = new ArrayList<TZRule>();

        TZWindow(TZoneOffset standardOffset, TLocalDateTime windowEnd, TimeDefinition timeDefinition) {

            super();
            this.windowEnd = windowEnd;
            this.timeDefinition = timeDefinition;
            this.standardOffset = standardOffset;
        }

        void setFixedSavings(int fixedSavingAmount) {

            if (this.ruleList.size() > 0 || this.lastRuleList.size() > 0) {
                throw new IllegalStateException("Window has DST rules, so cannot have fixed savings");
            }
            this.fixedSavingAmountSecs = fixedSavingAmount;
        }

        void addRule(int startYear, int endYear, TMonth month, int dayOfMonthIndicator, TDayOfWeek dayOfWeek,
                TLocalTime time, int adjustDays, TimeDefinition timeDefinition, int savingAmountSecs) {

            if (this.fixedSavingAmountSecs != null) {
                throw new IllegalStateException("Window has a fixed DST saving, so cannot have DST rules");
            }
            if (this.ruleList.size() >= 2000) {
                throw new IllegalStateException("Window has reached the maximum number of allowed rules");
            }
            boolean lastRule = false;
            if (endYear == TYear.MAX_VALUE) {
                lastRule = true;
                endYear = startYear;
            }
            int year = startYear;
            while (year <= endYear) {
                TZRule rule = new TZRule(year, month, dayOfMonthIndicator, dayOfWeek, time, adjustDays, timeDefinition,
                        savingAmountSecs);
                if (lastRule) {
                    this.lastRuleList.add(rule);
                    this.maxLastRuleStartYear = Math.max(startYear, this.maxLastRuleStartYear);
                } else {
                    this.ruleList.add(rule);
                }
                year++;
            }
        }

        void validateWindowOrder(TZWindow previous) {

            if (this.windowEnd.isBefore(previous.windowEnd)) {
                throw new IllegalStateException(
                        "Windows must be added in date-time order: " + this.windowEnd + " < " + previous.windowEnd);
            }
        }

        void tidy(int windowStartYear) {

            if (this.lastRuleList.size() == 1) {
                throw new IllegalStateException("Cannot have only one rule defined as being forever");
            }

            // handle last rules
            if (this.windowEnd.equals(TLocalDateTime.MAX)) {
                // setup at least one real rule, which closes off other windows nicely
                this.maxLastRuleStartYear = Math.max(this.maxLastRuleStartYear, windowStartYear) + 1;
                for (TZRule lastRule : this.lastRuleList) {
                    addRule(lastRule.year, this.maxLastRuleStartYear, lastRule.month, lastRule.dayOfMonthIndicator,
                            lastRule.dayOfWeek, lastRule.time, lastRule.adjustDays, lastRule.timeDefinition,
                            lastRule.savingAmountSecs);
                    lastRule.year = this.maxLastRuleStartYear + 1;
                }
                if (this.maxLastRuleStartYear == TYear.MAX_VALUE) {
                    this.lastRuleList.clear();
                } else {
                    this.maxLastRuleStartYear++;
                }
            } else {
                // convert all within the endYear limit
                int endYear = this.windowEnd.getYear();
                for (TZRule lastRule : this.lastRuleList) {
                    addRule(lastRule.year, endYear + 1, lastRule.month, lastRule.dayOfMonthIndicator,
                            lastRule.dayOfWeek, lastRule.time, lastRule.adjustDays, lastRule.timeDefinition,
                            lastRule.savingAmountSecs);
                }
                this.lastRuleList.clear();
                this.maxLastRuleStartYear = TYear.MAX_VALUE;
            }

            // ensure lists are sorted
            Collections.sort(this.ruleList);
            Collections.sort(this.lastRuleList);

            // default fixed savings to zero
            if (this.ruleList.size() == 0 && this.fixedSavingAmountSecs == null) {
                this.fixedSavingAmountSecs = 0;
            }
        }

        boolean isSingleWindowStandardOffset() {

            return this.windowEnd.equals(TLocalDateTime.MAX) && this.timeDefinition == TimeDefinition.WALL
                    && this.fixedSavingAmountSecs == null && this.lastRuleList.isEmpty() && this.ruleList.isEmpty();
        }

        TZoneOffset createWallOffset(int savingsSecs) {

            return TZoneOffset.ofTotalSeconds(this.standardOffset.getTotalSeconds() + savingsSecs);
        }

        long createDateTimeEpochSecond(int savingsSecs) {

            TZoneOffset wallOffset = createWallOffset(savingsSecs);
            TLocalDateTime ldt = this.timeDefinition.createDateTime(this.windowEnd, this.standardOffset, wallOffset);
            return ldt.toEpochSecond(wallOffset);
        }
    }

    class TZRule implements Comparable<TZRule> {
        private int year;

        private TMonth month;

        private int dayOfMonthIndicator;

        private TDayOfWeek dayOfWeek;

        private TLocalTime time;

        private int adjustDays;

        private TimeDefinition timeDefinition;

        private int savingAmountSecs;

        TZRule(int year, TMonth month, int dayOfMonthIndicator, TDayOfWeek dayOfWeek, TLocalTime time, int adjustDays,
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

        TZoneOffsetTransition toTransition(TZoneOffset standardOffset, int savingsBeforeSecs) {

            // copy of code in TZoneOffsetTransitionRule to avoid infinite loop
            TLocalDate date = toLocalDate();
            date = deduplicate(date);
            TLocalDateTime ldt = deduplicate(TLocalDateTime.of(date.plusDays(this.adjustDays), this.time));
            TZoneOffset wallOffset = deduplicate(
                    TZoneOffset.ofTotalSeconds(standardOffset.getTotalSeconds() + savingsBeforeSecs));
            TLocalDateTime dt = deduplicate(this.timeDefinition.createDateTime(ldt, standardOffset, wallOffset));
            TZoneOffset offsetAfter = deduplicate(
                    TZoneOffset.ofTotalSeconds(standardOffset.getTotalSeconds() + this.savingAmountSecs));
            return new TZoneOffsetTransition(dt, wallOffset, offsetAfter);
        }

        TZoneOffsetTransitionRule toTransitionRule(TZoneOffset standardOffset, int savingsBeforeSecs) {

            // optimize stored format
            if (this.dayOfMonthIndicator < 0) {
                if (this.month != TMonth.FEBRUARY) {
                    this.dayOfMonthIndicator = this.month.maxLength() - 6;
                }
            }

            // build rule
            TZoneOffsetTransition trans = toTransition(standardOffset, savingsBeforeSecs);
            return new TZoneOffsetTransitionRule(this.month, this.dayOfMonthIndicator, this.dayOfWeek, this.time,
                    this.adjustDays, this.timeDefinition, standardOffset, trans.getOffsetBefore(),
                    trans.getOffsetAfter());
        }

        @Override
        public int compareTo(TZRule other) {

            int cmp = this.year - other.year;
            cmp = (cmp == 0 ? this.month.compareTo(other.month) : cmp);
            if (cmp == 0) {
                // convert to date to handle dow/domIndicator/timeEndOfDay
                TLocalDate thisDate = toLocalDate();
                TLocalDate otherDate = other.toLocalDate();
                cmp = thisDate.compareTo(otherDate);
            }
            if (cmp != 0) {
                return cmp;
            }
            long timeSecs1 = this.time.toSecondOfDay() + this.adjustDays * 86400;
            long timeSecs2 = other.time.toSecondOfDay() + other.adjustDays * 86400;
            return timeSecs1 < timeSecs2 ? -1 : (timeSecs1 > timeSecs2 ? 1 : 0);
        }

        private TLocalDate toLocalDate() {

            TLocalDate date;
            if (this.dayOfMonthIndicator < 0) {
                int monthLen = this.month.length(TIsoChronology.INSTANCE.isLeapYear(this.year));
                date = TLocalDate.of(this.year, this.month, monthLen + 1 + this.dayOfMonthIndicator);
                if (this.dayOfWeek != null) {
                    date = date.with(previousOrSame(this.dayOfWeek));
                }
            } else {
                date = TLocalDate.of(this.year, this.month, this.dayOfMonthIndicator);
                if (this.dayOfWeek != null) {
                    date = date.with(nextOrSame(this.dayOfWeek));
                }
            }
            return date;
        }

        // no equals() or hashCode()
    }

}
