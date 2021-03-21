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

import static org.threeten.bp.temporal.TemporalAdjusters.nextOrSame;
import static org.threeten.bp.temporal.TemporalAdjusters.previousOrSame;
import java.io.Serializable;
import java.util.Objects;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.Month;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.chrono.IsoChronology;
import org.threeten.bp.jdk8.Jdk8Methods;

/**
 * A rule expressing how to create a transition.
 * <p>
 * This class allows rules for identifying future transitions to be expressed.
 * A rule might be written in many forms:
 * <p><ul>
 * <li>the 16th March
 * <li>the Sunday on or after the 16th March
 * <li>the Sunday on or before the 16th March
 * <li>the last Sunday in February
 * </ul><p>
 * These different rule types can be expressed and queried.
 *
 * <h3>Specification for implementors</h3>
 * This class is immutable and thread-safe.
 */
public final class ZoneOffsetTransitionRule implements Serializable {

    /**
     * The number of seconds per day.
     */
    private static final int SECS_PER_DAY = 86400;

    /**
     * The month of the month-day of the first day of the cutover week.
     * The actual date will be adjusted by the dowChange field.
     */
    private final Month month;
    /**
     * The day-of-month of the month-day of the cutover week.
     * If positive, it is the start of the week where the cutover can occur.
     * If negative, it represents the end of the week where cutover can occur.
     * The value is the number of days from the end of the month, such that
     * {@code -1} is the last day of the month, {@code -2} is the second
     * to last day, and so on.
     */
    private final byte dom;
    /**
     * The cutover day-of-week, null to retain the day-of-month.
     */
    private final DayOfWeek dow;
    /**
     * The cutover time in the 'before' offset.
     */
    private final LocalTime time;
    /**
     * The number of days to adjust by.
     */
    private final int adjustDays;
    /**
     * The definition of how the local time should be interpreted.
     */
    private final TimeDefinition timeDefinition;
    /**
     * The standard offset at the cutover.
     */
    private final ZoneOffset standardOffset;
    /**
     * The offset before the cutover.
     */
    private final ZoneOffset offsetBefore;
    /**
     * The offset after the cutover.
     */
    private final ZoneOffset offsetAfter;

    /**
     * Obtains an instance defining the yearly rule to create transitions between two offsets.
     * <p>
     * Applications should normally obtain an instance from {@link ZoneRules}.
     * This factory is only intended for use when creating {@link ZoneRules}.
     *
     * @param month  the month of the month-day of the first day of the cutover week, not null
     * @param dayOfMonthIndicator  the day of the month-day of the cutover week, positive if the week is that
     *  day or later, negative if the week is that day or earlier, counting from the last day of the month,
     *  from -28 to 31 excluding 0
     * @param dayOfWeek  the required day-of-week, null if the month-day should not be changed
     * @param time  the cutover time in the 'before' offset, not null
     * @param timeEndOfDay  whether the time is midnight at the end of day
     * @param timeDefnition  how to interpret the cutover
     * @param standardOffset  the standard offset in force at the cutover, not null
     * @param offsetBefore  the offset before the cutover, not null
     * @param offsetAfter  the offset after the cutover, not null
     * @return the rule, not null
     * @throws IllegalArgumentException if the day of month indicator is invalid
     * @throws IllegalArgumentException if the end of day flag is true when the time is not midnight
     */
    public static ZoneOffsetTransitionRule of(
            Month month,
            int dayOfMonthIndicator,
            DayOfWeek dayOfWeek,
            LocalTime time,
            boolean timeEndOfDay,
            TimeDefinition timeDefnition,
            ZoneOffset standardOffset,
            ZoneOffset offsetBefore,
            ZoneOffset offsetAfter) {
        Objects.requireNonNull(month, "month");
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(timeDefnition, "timeDefnition");
        Objects.requireNonNull(standardOffset, "standardOffset");
        Objects.requireNonNull(offsetBefore, "offsetBefore");
        Objects.requireNonNull(offsetAfter, "offsetAfter");
        if (dayOfMonthIndicator < -28 || dayOfMonthIndicator > 31 || dayOfMonthIndicator == 0) {
            throw new IllegalArgumentException("Day of month indicator must be between -28 and 31"
                    + " inclusive excluding zero");
        }
        if (timeEndOfDay && !time.equals(LocalTime.MIDNIGHT)) {
            throw new IllegalArgumentException("Time must be midnight when end of day flag is true");
        }
        return new ZoneOffsetTransitionRule(month, dayOfMonthIndicator, dayOfWeek, time, timeEndOfDay ? 1 : 0,
                timeDefnition, standardOffset, offsetBefore, offsetAfter);
    }

    /**
     * Creates an instance defining the yearly rule to create transitions between two offsets.
     *
     * @param month  the month of the month-day of the first day of the cutover week, not null
     * @param dayOfMonthIndicator  the day of the month-day of the cutover week, positive if the week is that
     *  day or later, negative if the week is that day or earlier, counting from the last day of the month,
     *  from -28 to 31 excluding 0
     * @param dayOfWeek  the required day-of-week, null if the month-day should not be changed
     * @param time  the cutover time in the 'before' offset, not null
     * @param adjustDays  the time days adjustment
     * @param timeDefnition  how to interpret the cutover
     * @param standardOffset  the standard offset in force at the cutover, not null
     * @param offsetBefore  the offset before the cutover, not null
     * @param offsetAfter  the offset after the cutover, not null
     * @throws IllegalArgumentException if the day of month indicator is invalid
     * @throws IllegalArgumentException if the end of day flag is true when the time is not midnight
     */
    ZoneOffsetTransitionRule(
            Month month,
            int dayOfMonthIndicator,
            DayOfWeek dayOfWeek,
            LocalTime time,
            int adjustDays,
            TimeDefinition timeDefnition,
            ZoneOffset standardOffset,
            ZoneOffset offsetBefore,
            ZoneOffset offsetAfter) {
        this.month = month;
        this.dom = (byte) dayOfMonthIndicator;
        this.dow = dayOfWeek;
        this.time = time;
        this.adjustDays = adjustDays;
        this.timeDefinition = timeDefnition;
        this.standardOffset = standardOffset;
        this.offsetBefore = offsetBefore;
        this.offsetAfter = offsetAfter;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the month of the transition.
     * <p>
     * If the rule defines an exact date then the month is the month of that date.
     * <p>
     * If the rule defines a week where the transition might occur, then the month
     * if the month of either the earliest or latest possible date of the cutover.
     *
     * @return the month of the transition, not null
     */
    public Month getMonth() {
        return month;
    }

    /**
     * Gets the indicator of the day-of-month of the transition.
     * <p>
     * If the rule defines an exact date then the day is the month of that date.
     * <p>
     * If the rule defines a week where the transition might occur, then the day
     * defines either the start of the end of the transition week.
     * <p>
     * If the value is positive, then it represents a normal day-of-month, and is the
     * earliest possible date that the transition can be.
     * The date may refer to 29th February which should be treated as 1st March in non-leap years.
     * <p>
     * If the value is negative, then it represents the number of days back from the
     * end of the month where {@code -1} is the last day of the month.
     * In this case, the day identified is the latest possible date that the transition can be.
     *
     * @return the day-of-month indicator, from -28 to 31 excluding 0
     */
    public int getDayOfMonthIndicator() {
        return dom;
    }

    /**
     * Gets the day-of-week of the transition.
     * <p>
     * If the rule defines an exact date then this returns null.
     * <p>
     * If the rule defines a week where the cutover might occur, then this method
     * returns the day-of-week that the month-day will be adjusted to.
     * If the day is positive then the adjustment is later.
     * If the day is negative then the adjustment is earlier.
     *
     * @return the day-of-week that the transition occurs, null if the rule defines an exact date
     */
    public DayOfWeek getDayOfWeek() {
        return dow;
    }

    /**
     * Gets the local time of day of the transition which must be checked with
     * {@link #isMidnightEndOfDay()}.
     * <p>
     * The time is converted into an instant using the time definition.
     *
     * @return the local time of day of the transition, not null
     */
    public LocalTime getLocalTime() {
        return time;
    }

    /**
     * Is the transition local time midnight at the end of day.
     * <p>
     * The transition may be represented as occurring at 24:00.
     *
     * @return whether a local time of midnight is at the start or end of the day
     */
    public boolean isMidnightEndOfDay() {
        return adjustDays == 1 && time.equals(LocalTime.MIDNIGHT);
    }

    /**
     * Gets the time definition, specifying how to convert the time to an instant.
     * <p>
     * The local time can be converted to an instant using the standard offset,
     * the wall offset or UTC.
     *
     * @return the time definition, not null
     */
    public TimeDefinition getTimeDefinition() {
        return timeDefinition;
    }

    /**
     * Gets the standard offset in force at the transition.
     *
     * @return the standard offset, not null
     */
    public ZoneOffset getStandardOffset() {
        return standardOffset;
    }

    /**
     * Gets the offset before the transition.
     *
     * @return the offset before, not null
     */
    public ZoneOffset getOffsetBefore() {
        return offsetBefore;
    }

    /**
     * Gets the offset after the transition.
     *
     * @return the offset after, not null
     */
    public ZoneOffset getOffsetAfter() {
        return offsetAfter;
    }

    //-----------------------------------------------------------------------
    /**
     * Creates a transition instance for the specified year.
     * <p>
     * Calculations are performed using the ISO-8601 chronology.
     *
     * @param year  the year to create a transition for, not null
     * @return the transition instance, not null
     */
    public ZoneOffsetTransition createTransition(int year) {
        LocalDate date;
        if (dom < 0) {
            date = LocalDate.of(year, month, month.length(IsoChronology.INSTANCE.isLeapYear(year)) + 1 + dom);
            if (dow != null) {
                date = date.with(previousOrSame(dow));
            }
        } else {
            date = LocalDate.of(year, month, dom);
            if (dow != null) {
                date = date.with(nextOrSame(dow));
            }
        }
        LocalDateTime localDT = LocalDateTime.of(date.plusDays(adjustDays), time);
        LocalDateTime transition = timeDefinition.createDateTime(localDT, standardOffset, offsetBefore);
        return new ZoneOffsetTransition(transition, offsetBefore, offsetAfter);
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if this object equals another.
     * <p>
     * The entire state of the object is compared.
     *
     * @param otherRule  the other object to compare to, null returns false
     * @return true if equal
     */
    @Override
    public boolean equals(Object otherRule) {
        if (otherRule == this) {
            return true;
        }
        if (otherRule instanceof ZoneOffsetTransitionRule) {
            ZoneOffsetTransitionRule other = (ZoneOffsetTransitionRule) otherRule;
            return month == other.month && dom == other.dom && dow == other.dow
                    && timeDefinition == other.timeDefinition
                    && adjustDays == other.adjustDays
                    && time.equals(other.time)
                    && standardOffset.equals(other.standardOffset)
                    && offsetBefore.equals(other.offsetBefore)
                    && offsetAfter.equals(other.offsetAfter);
        }
        return false;
    }

    /**
     * Returns a suitable hash code.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int hash = ((time.toSecondOfDay() + adjustDays) << 15)
                + (month.ordinal() << 11) + ((dom + 32) << 5)
                + ((dow == null ? 7 : dow.ordinal()) << 2) + (timeDefinition.ordinal());
        return hash ^ standardOffset.hashCode() ^ offsetBefore.hashCode() ^ offsetAfter.hashCode();
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a string describing this object.
     *
     * @return a string for debugging, not null
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("TransitionRule[")
            .append(offsetBefore.compareTo(offsetAfter) > 0 ? "Gap " : "Overlap ")
            .append(offsetBefore).append(" to ").append(offsetAfter).append(", ");
        if (dow != null) {
            if (dom == -1) {
                buf.append(dow.name()).append(" on or before last day of ").append(month.name());
            } else if (dom < 0) {
                buf.append(dow.name()).append(" on or before last day minus ").append(-dom - 1)
                        .append(" of ").append(month.name());
            } else {
                buf.append(dow.name()).append(" on or after ").append(month.name()).append(' ').append(dom);
            }
        } else {
            buf.append(month.name()).append(' ').append(dom);
        }
        buf.append(" at ");
        if (adjustDays == 0) {
            buf.append(time);
        } else {
            long timeOfDaysMins = time.toSecondOfDay() / 60 + adjustDays * 24 * 60;
            appendZeroPad(buf, Jdk8Methods.floorDiv(timeOfDaysMins, 60));
            buf.append(':');
            appendZeroPad(buf, Jdk8Methods.floorMod(timeOfDaysMins, 60));
        }
        buf.append(" ").append(timeDefinition)
            .append(", standard offset ").append(standardOffset)
            .append(']');
        return buf.toString();
    }

    private void appendZeroPad(StringBuilder sb, long number) {
        if (number < 10) {
            sb.append(0);
        }
        sb.append(number);
    }

    //-----------------------------------------------------------------------
    /**
     * A definition of the way a local time can be converted to the actual
     * transition date-time.
     * <p>
     * Time zone rules are expressed in one of three ways:
     * <p><ul>
     * <li>Relative to UTC</li>
     * <li>Relative to the standard offset in force</li>
     * <li>Relative to the wall offset (what you would see on a clock on the wall)</li>
     * </ul><p>
     */
    public enum TimeDefinition {
        /** The local date-time is expressed in terms of the UTC offset. */
        UTC,
        /** The local date-time is expressed in terms of the wall offset. */
        WALL,
        /** The local date-time is expressed in terms of the standard offset. */
        STANDARD;

        /**
         * Converts the specified local date-time to the local date-time actually
         * seen on a wall clock.
         * <p>
         * This method converts using the type of this enum.
         * The output is defined relative to the 'before' offset of the transition.
         * <p>
         * The UTC type uses the UTC offset.
         * The STANDARD type uses the standard offset.
         * The WALL type returns the input date-time.
         * The result is intended for use with the wall-offset.
         *
         * @param dateTime  the local date-time, not null
         * @param standardOffset  the standard offset, not null
         * @param wallOffset  the wall offset, not null
         * @return the date-time relative to the wall/before offset, not null
         */
        public LocalDateTime createDateTime(LocalDateTime dateTime, ZoneOffset standardOffset, ZoneOffset wallOffset) {
            switch (this) {
                case UTC: {
                    int difference = wallOffset.getTotalSeconds() - ZoneOffset.UTC.getTotalSeconds();
                    return dateTime.plusSeconds(difference);
                }
                case STANDARD: {
                    int difference = wallOffset.getTotalSeconds() - standardOffset.getTotalSeconds();
                    return dateTime.plusSeconds(difference);
                }
                default:  // WALL
                    return dateTime;
            }
        }
    }

}
