/*
 *  Copyright 2020 adopted to TeaVM by Joerg Hohwiller
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
package org.teavm.classlib.java.time.zone;

import static org.teavm.classlib.java.time.temporal.TTemporalAdjusters.nextOrSame;
import static org.teavm.classlib.java.time.temporal.TTemporalAdjusters.previousOrSame;

import java.util.Objects;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.time.TDayOfWeek;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TMonth;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.chrono.TIsoChronology;

public final class TZoneOffsetTransitionRule implements TSerializable {

    private static final int SECS_PER_DAY = 86400;

    private final TMonth month;

    private final byte dom;

    private final TDayOfWeek dow;

    private final TLocalTime time;

    private final int adjustDays;

    private final TimeDefinition timeDefinition;

    private final TZoneOffset standardOffset;

    private final TZoneOffset offsetBefore;

    private final TZoneOffset offsetAfter;

    public static TZoneOffsetTransitionRule of(TMonth month, int dayOfMonthIndicator, TDayOfWeek dayOfWeek,
            TLocalTime time, boolean timeEndOfDay, TimeDefinition timeDefnition, TZoneOffset standardOffset,
            TZoneOffset offsetBefore, TZoneOffset offsetAfter) {

        Objects.requireNonNull(month, "month");
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(timeDefnition, "timeDefnition");
        Objects.requireNonNull(standardOffset, "standardOffset");
        Objects.requireNonNull(offsetBefore, "offsetBefore");
        Objects.requireNonNull(offsetAfter, "offsetAfter");
        if (dayOfMonthIndicator < -28 || dayOfMonthIndicator > 31 || dayOfMonthIndicator == 0) {
            throw new IllegalArgumentException(
                    "Day of month indicator must be between -28 and 31 inclusive excluding zero");
        }
        if (timeEndOfDay && time.equals(TLocalTime.MIDNIGHT) == false) {
            throw new IllegalArgumentException("Time must be midnight when end of day flag is true");
        }
        return new TZoneOffsetTransitionRule(month, dayOfMonthIndicator, dayOfWeek, time, timeEndOfDay ? 1 : 0,
                timeDefnition, standardOffset, offsetBefore, offsetAfter);
    }

    TZoneOffsetTransitionRule(int month, int dayOfMonthIndicator, Integer dayOfWeek, long time, int adjustDays,
            int timeDefnition, int standardOffset, int offsetBefore, int offsetAfter) {

        this(TMonth.of(month), dayOfMonthIndicator, (dayOfWeek == null) ? null : TDayOfWeek.of(dayOfWeek.intValue()),
                TLocalTime.ofSecondOfDay(time), adjustDays, TimeDefinition.ofOrdinal(timeDefnition),
                TZoneOffset.ofTotalSeconds(standardOffset), TZoneOffset.ofTotalSeconds(offsetBefore),
                TZoneOffset.ofTotalSeconds(offsetAfter));
    }

    TZoneOffsetTransitionRule(TMonth month, int dayOfMonthIndicator, TDayOfWeek dayOfWeek, TLocalTime time,
            int adjustDays, TimeDefinition timeDefnition, TZoneOffset standardOffset, TZoneOffset offsetBefore,
            TZoneOffset offsetAfter) {

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

    public TMonth getMonth() {

        return this.month;
    }

    public int getDayOfMonthIndicator() {

        return this.dom;
    }

    public TDayOfWeek getDayOfWeek() {

        return this.dow;
    }

    public TLocalTime getLocalTime() {

        return this.time;
    }

    public boolean isMidnightEndOfDay() {

        return this.adjustDays == 1 && this.time.equals(TLocalTime.MIDNIGHT);
    }

    public TimeDefinition getTimeDefinition() {

        return this.timeDefinition;
    }

    public TZoneOffset getStandardOffset() {

        return this.standardOffset;
    }

    public TZoneOffset getOffsetBefore() {

        return this.offsetBefore;
    }

    public TZoneOffset getOffsetAfter() {

        return this.offsetAfter;
    }

    public TZoneOffsetTransition createTransition(int year) {

        TLocalDate date;
        if (this.dom < 0) {
            date = TLocalDate.of(year, this.month,
                    this.month.length(TIsoChronology.INSTANCE.isLeapYear(year)) + 1 + this.dom);
            if (this.dow != null) {
                date = date.with(previousOrSame(this.dow));
            }
        } else {
            date = TLocalDate.of(year, this.month, this.dom);
            if (this.dow != null) {
                date = date.with(nextOrSame(this.dow));
            }
        }
        TLocalDateTime localDT = TLocalDateTime.of(date.plusDays(this.adjustDays), this.time);
        TLocalDateTime transition = this.timeDefinition.createDateTime(localDT, this.standardOffset, this.offsetBefore);
        return new TZoneOffsetTransition(transition, this.offsetBefore, this.offsetAfter);
    }

    @Override
    public boolean equals(Object otherRule) {

        if (otherRule == this) {
            return true;
        }
        if (otherRule instanceof TZoneOffsetTransitionRule) {
            TZoneOffsetTransitionRule other = (TZoneOffsetTransitionRule) otherRule;
            return this.month == other.month && this.dom == other.dom && this.dow == other.dow
                    && this.timeDefinition == other.timeDefinition && this.adjustDays == other.adjustDays
                    && this.time.equals(other.time) && this.standardOffset.equals(other.standardOffset)
                    && this.offsetBefore.equals(other.offsetBefore) && this.offsetAfter.equals(other.offsetAfter);
        }
        return false;
    }

    @Override
    public int hashCode() {

        int hash = ((this.time.toSecondOfDay() + this.adjustDays) << 15) + (this.month.ordinal() << 11)
                + ((this.dom + 32) << 5) + ((this.dow == null ? 7 : this.dow.ordinal()) << 2)
                + (this.timeDefinition.ordinal());
        return hash ^ this.standardOffset.hashCode() ^ this.offsetBefore.hashCode() ^ this.offsetAfter.hashCode();
    }

    @Override
    public String toString() {

        StringBuilder buf = new StringBuilder();
        buf.append("TransitionRule[").append(this.offsetBefore.compareTo(this.offsetAfter) > 0 ? "Gap " : "Overlap ")
                .append(this.offsetBefore).append(" to ").append(this.offsetAfter).append(", ");
        if (this.dow != null) {
            if (this.dom == -1) {
                buf.append(this.dow.name()).append(" on or before last day of ").append(this.month.name());
            } else if (this.dom < 0) {
                buf.append(this.dow.name()).append(" on or before last day minus ").append(-this.dom - 1).append(" of ")
                        .append(this.month.name());
            } else {
                buf.append(this.dow.name()).append(" on or after ").append(this.month.name()).append(' ')
                        .append(this.dom);
            }
        } else {
            buf.append(this.month.name()).append(' ').append(this.dom);
        }
        buf.append(" at ");
        if (this.adjustDays == 0) {
            buf.append(this.time);
        } else {
            long timeOfDaysMins = this.time.toSecondOfDay() / 60 + this.adjustDays * 24 * 60;
            appendZeroPad(buf, Math.floorDiv(timeOfDaysMins, 60));
            buf.append(':');
            appendZeroPad(buf, Math.floorMod(timeOfDaysMins, 60));
        }
        buf.append(" ").append(this.timeDefinition).append(", standard offset ").append(this.standardOffset)
                .append(']');
        return buf.toString();
    }

    private void appendZeroPad(StringBuilder sb, long number) {

        if (number < 10) {
            sb.append(0);
        }
        sb.append(number);
    }

    public static enum TimeDefinition {
        UTC, WALL, STANDARD;

        public TLocalDateTime createDateTime(TLocalDateTime dateTime, TZoneOffset standardOffset,
                TZoneOffset wallOffset) {

            switch (this) {
                case UTC: {
                    int difference = wallOffset.getTotalSeconds() - TZoneOffset.UTC.getTotalSeconds();
                    return dateTime.plusSeconds(difference);
                }
                case STANDARD: {
                    int difference = wallOffset.getTotalSeconds() - standardOffset.getTotalSeconds();
                    return dateTime.plusSeconds(difference);
                }
                default: // WALL
                    return dateTime;
            }
        }

        private static TimeDefinition ofOrdinal(int ordinal) {

            switch (ordinal) {
                case 0:
                    return UTC;
                case 1:
                    return WALL;
                case 2:
                    return STANDARD;
                default:
                    throw new IllegalArgumentException(Integer.toString(ordinal));
            }
        }
    }

}
