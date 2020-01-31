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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.teavm.classlib.java.time.TDayOfWeek;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TMonth;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;

public final class TZoneOffsetTransitionRule implements Serializable {

    private static final long serialVersionUID = 6889046316657758795L;
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

    public static TZoneOffsetTransitionRule of(
            TMonth month,
            int dayOfMonthIndicator,
            TDayOfWeek dayOfWeek,
            TLocalTime time,
            boolean timeEndOfDay,
            TimeDefinition timeDefnition,
            TZoneOffset standardOffset,
            TZoneOffset offsetBefore,
            TZoneOffset offsetAfter) {
        TJdk8Methods.requireNonNull(month, "month");
        TJdk8Methods.requireNonNull(time, "time");
        TJdk8Methods.requireNonNull(timeDefnition, "timeDefnition");
        TJdk8Methods.requireNonNull(standardOffset, "standardOffset");
        TJdk8Methods.requireNonNull(offsetBefore, "offsetBefore");
        TJdk8Methods.requireNonNull(offsetAfter, "offsetAfter");
        if (dayOfMonthIndicator < -28 || dayOfMonthIndicator > 31 || dayOfMonthIndicator == 0) {
            throw new IllegalArgumentException("Day of month indicator must be between -28 and 31 inclusive excluding zero");
        }
        if (timeEndOfDay && time.equals(TLocalTime.MIDNIGHT) == false) {
            throw new IllegalArgumentException("Time must be midnight when end of day flag is true");
        }
        return new TZoneOffsetTransitionRule(month, dayOfMonthIndicator, dayOfWeek, time, timeEndOfDay ? 1 : 0, timeDefnition, standardOffset, offsetBefore, offsetAfter);
    }

    TZoneOffsetTransitionRule(
            TMonth month,
            int dayOfMonthIndicator,
            TDayOfWeek dayOfWeek,
            TLocalTime time,
            int adjustDays,
            TimeDefinition timeDefnition,
            TZoneOffset standardOffset,
            TZoneOffset offsetBefore,
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

    //-----------------------------------------------------------------------
    private Object writeReplace() {
        return new Ser(Ser.ZOTRULE, this);
    }

    void writeExternal(DataOutput out) throws IOException {
        final int timeSecs = time.toSecondOfDay() + adjustDays * SECS_PER_DAY;
        final int stdOffset = standardOffset.getTotalSeconds();
        final int beforeDiff = offsetBefore.getTotalSeconds() - stdOffset;
        final int afterDiff = offsetAfter.getTotalSeconds() - stdOffset;
        final int timeByte = (timeSecs % 3600 == 0 && timeSecs <= SECS_PER_DAY ?
                (timeSecs == SECS_PER_DAY ? 24 : time.getHour()) : 31);
        final int stdOffsetByte = (stdOffset % 900 == 0 ? stdOffset / 900 + 128 : 255);
        final int beforeByte = (beforeDiff == 0 || beforeDiff == 1800 || beforeDiff == 3600 ? beforeDiff / 1800 : 3);
        final int afterByte = (afterDiff == 0 || afterDiff == 1800 || afterDiff == 3600 ? afterDiff / 1800 : 3);
        final int dowByte = (dow == null ? 0 : dow.getValue());
        int b = (month.getValue() << 28) +          // 4 bits
                ((dom + 32) << 22) +                // 6 bits
                (dowByte << 19) +                   // 3 bits
                (timeByte << 14) +                  // 5 bits
                (timeDefinition.ordinal() << 12) +  // 2 bits
                (stdOffsetByte << 4) +              // 8 bits
                (beforeByte << 2) +                 // 2 bits
                afterByte;                          // 2 bits
        out.writeInt(b);
        if (timeByte == 31) {
            out.writeInt(timeSecs);
        }
        if (stdOffsetByte == 255) {
            out.writeInt(stdOffset);
        }
        if (beforeByte == 3) {
            out.writeInt(offsetBefore.getTotalSeconds());
        }
        if (afterByte == 3) {
            out.writeInt(offsetAfter.getTotalSeconds());
        }
    }

    static TZoneOffsetTransitionRule readExternal(DataInput in) throws IOException {
        int data = in.readInt();
        TMonth month = TMonth.of(data >>> 28);
        int dom = ((data & (63 << 22)) >>> 22) - 32;
        int dowByte = (data & (7 << 19)) >>> 19;
        TDayOfWeek dow = dowByte == 0 ? null : TDayOfWeek.of(dowByte);
        int timeByte = (data & (31 << 14)) >>> 14;
        TimeDefinition defn = TimeDefinition.values()[(data & (3 << 12)) >>> 12];
        int stdByte = (data & (255 << 4)) >>> 4;
        int beforeByte = (data & (3 << 2)) >>> 2;
        int afterByte = (data & 3);
        int timeOfDaysSecs = (timeByte == 31 ? in.readInt() : timeByte * 3600);
        TZoneOffset std = (stdByte == 255 ? TZoneOffset.ofTotalSeconds(in.readInt()) : TZoneOffset.ofTotalSeconds((stdByte - 128) * 900));
        TZoneOffset before = (beforeByte == 3 ? TZoneOffset.ofTotalSeconds(in.readInt()) : TZoneOffset.ofTotalSeconds(std.getTotalSeconds() + beforeByte * 1800));
        TZoneOffset after = (afterByte == 3 ? TZoneOffset.ofTotalSeconds(in.readInt()) : TZoneOffset.ofTotalSeconds(std.getTotalSeconds() + afterByte * 1800));
        // only bit of validation that we need to copy from public of() method
        if (dom < -28 || dom > 31 || dom == 0) {
            throw new IllegalArgumentException("Day of month indicator must be between -28 and 31 inclusive excluding zero");
        }
        TLocalTime time = TLocalTime.ofSecondOfDay(TJdk8Methods.floorMod(timeOfDaysSecs, SECS_PER_DAY));
        int adjustDays = TJdk8Methods.floorDiv(timeOfDaysSecs, SECS_PER_DAY);
        return new TZoneOffsetTransitionRule(month, dom, dow, time, adjustDays, defn, std, before, after);
    }

    //-----------------------------------------------------------------------
    public TMonth getMonth() {
        return month;
    }

    public int getDayOfMonthIndicator() {
        return dom;
    }

    public TDayOfWeek getDayOfWeek() {
        return dow;
    }

    public TLocalTime getLocalTime() {
        return time;
    }

    public boolean isMidnightEndOfDay() {
        return adjustDays == 1 && time.equals(TLocalTime.MIDNIGHT);
    }

    public TimeDefinition getTimeDefinition() {
        return timeDefinition;
    }

    public TZoneOffset getStandardOffset() {
        return standardOffset;
    }

    public TZoneOffset getOffsetBefore() {
        return offsetBefore;
    }

    public TZoneOffset getOffsetAfter() {
        return offsetAfter;
    }

    //-----------------------------------------------------------------------
    public TZoneOffsetTransition createTransition(int year) {
        TLocalDate date;
        if (dom < 0) {
            date = TLocalDate.of(year, month, month.length(TIsoChronology.INSTANCE.isLeapYear(year)) + 1 + dom);
            if (dow != null) {
                date = date.with(previousOrSame(dow));
            }
        } else {
            date = TLocalDate.of(year, month, dom);
            if (dow != null) {
                date = date.with(nextOrSame(dow));
            }
        }
        TLocalDateTime localDT = TLocalDateTime.of(date.plusDays(adjustDays), time);
        TLocalDateTime transition = timeDefinition.createDateTime(localDT, standardOffset, offsetBefore);
        return new TZoneOffsetTransition(transition, offsetBefore, offsetAfter);
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean equals(Object otherRule) {
        if (otherRule == this) {
            return true;
        }
        if (otherRule instanceof TZoneOffsetTransitionRule) {
            TZoneOffsetTransitionRule other = (TZoneOffsetTransitionRule) otherRule;
            return month == other.month && dom == other.dom && dow == other.dow &&
                timeDefinition == other.timeDefinition &&
                adjustDays == other.adjustDays &&
                time.equals(other.time) &&
                standardOffset.equals(other.standardOffset) &&
                offsetBefore.equals(other.offsetBefore) &&
                offsetAfter.equals(other.offsetAfter);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = ((time.toSecondOfDay() + adjustDays) << 15) +
                (month.ordinal() << 11) + ((dom + 32) << 5) +
                ((dow == null ? 7 : dow.ordinal()) << 2) + (timeDefinition.ordinal());
        return hash ^ standardOffset.hashCode() ^
                offsetBefore.hashCode() ^ offsetAfter.hashCode();
    }

    //-----------------------------------------------------------------------
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
                buf.append(dow.name()).append(" on or before last day minus ").append(-dom - 1).append(" of ").append(month.name());
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
            appendZeroPad(buf, TJdk8Methods.floorDiv(timeOfDaysMins, 60));
            buf.append(':');
            appendZeroPad(buf, TJdk8Methods.floorMod(timeOfDaysMins, 60));
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
    public static enum TimeDefinition {
        UTC,
        WALL,
        STANDARD;

        public TLocalDateTime createDateTime(TLocalDateTime dateTime, TZoneOffset standardOffset, TZoneOffset wallOffset) {
            switch (this) {
                case UTC: {
                    int difference = wallOffset.getTotalSeconds() - TZoneOffset.UTC.getTotalSeconds();
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
