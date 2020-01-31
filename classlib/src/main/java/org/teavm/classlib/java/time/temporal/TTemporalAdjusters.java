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
package org.teavm.classlib.java.time.temporal;

import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MONTHS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.YEARS;

import org.teavm.classlib.java.time.TDayOfWeek;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;

public final class TTemporalAdjusters {

    private TTemporalAdjusters() {
    }

    //-----------------------------------------------------------------------
    public static TTemporalAdjuster firstDayOfMonth() {
        return Impl.FIRST_DAY_OF_MONTH;
    }

    public static TTemporalAdjuster lastDayOfMonth() {
        return Impl.LAST_DAY_OF_MONTH;
    }

    public static TTemporalAdjuster firstDayOfNextMonth() {
        return Impl.FIRST_DAY_OF_NEXT_MONTH;
    }

    //-----------------------------------------------------------------------
    public static TTemporalAdjuster firstDayOfYear() {
        return Impl.FIRST_DAY_OF_YEAR;
    }

    public static TTemporalAdjuster lastDayOfYear() {
        return Impl.LAST_DAY_OF_YEAR;
    }

    public static TTemporalAdjuster firstDayOfNextYear() {
        return Impl.FIRST_DAY_OF_NEXT_YEAR;
    }

    //-----------------------------------------------------------------------
    private static class Impl implements TTemporalAdjuster {
        private static final Impl FIRST_DAY_OF_MONTH = new Impl(0);
        private static final Impl LAST_DAY_OF_MONTH = new Impl(1);
        private static final Impl FIRST_DAY_OF_NEXT_MONTH = new Impl(2);
        private static final Impl FIRST_DAY_OF_YEAR = new Impl(3);
        private static final Impl LAST_DAY_OF_YEAR = new Impl(4);
        private static final Impl FIRST_DAY_OF_NEXT_YEAR = new Impl(5);
        private final int ordinal;
        private Impl(int ordinal) {
            this.ordinal = ordinal;
        }
        @Override
        public TTemporal adjustInto(TTemporal temporal) {
            switch (ordinal) {
                case 0: return temporal.with(DAY_OF_MONTH, 1);
                case 1: return temporal.with(DAY_OF_MONTH, temporal.range(DAY_OF_MONTH).getMaximum());
                case 2: return temporal.with(DAY_OF_MONTH, 1).plus(1, MONTHS);
                case 3: return temporal.with(DAY_OF_YEAR, 1);
                case 4: return temporal.with(DAY_OF_YEAR, temporal.range(DAY_OF_YEAR).getMaximum());
                case 5: return temporal.with(DAY_OF_YEAR, 1).plus(1, YEARS);
            }
            throw new IllegalStateException("Unreachable");
        }
    }

    //-----------------------------------------------------------------------
    public static TTemporalAdjuster firstInMonth(TDayOfWeek dayOfWeek) {
        TJdk8Methods.requireNonNull(dayOfWeek, "dayOfWeek");
        return new DayOfWeekInMonth(1, dayOfWeek);
    }

    public static TTemporalAdjuster lastInMonth(TDayOfWeek dayOfWeek) {
        TJdk8Methods.requireNonNull(dayOfWeek, "dayOfWeek");
        return new DayOfWeekInMonth(-1, dayOfWeek);
    }

    public static TTemporalAdjuster dayOfWeekInMonth(int ordinal, TDayOfWeek dayOfWeek) {
        TJdk8Methods.requireNonNull(dayOfWeek, "dayOfWeek");
        return new DayOfWeekInMonth(ordinal, dayOfWeek);
    }

    private static final class DayOfWeekInMonth implements TTemporalAdjuster {
        private final int ordinal;
        private final int dowValue;

        private DayOfWeekInMonth(int ordinal, TDayOfWeek dow) {
            super();
            this.ordinal = ordinal;
            this.dowValue = dow.getValue();
        }
        @Override
        public TTemporal adjustInto(TTemporal temporal) {
            if (ordinal >= 0) {
                TTemporal temp = temporal.with(DAY_OF_MONTH, 1);
                int curDow = temp.get(DAY_OF_WEEK);
                int dowDiff = (dowValue - curDow + 7) % 7;
                dowDiff += (ordinal - 1L) * 7L;  // safe from overflow
                return temp.plus(dowDiff, DAYS);
            } else {
                TTemporal temp = temporal.with(DAY_OF_MONTH, temporal.range(DAY_OF_MONTH).getMaximum());
                int curDow = temp.get(DAY_OF_WEEK);
                int daysDiff = dowValue - curDow;
                daysDiff = (daysDiff == 0 ? 0 : (daysDiff > 0 ? daysDiff - 7 : daysDiff));
                daysDiff -= (-ordinal - 1L) * 7L;  // safe from overflow
                return temp.plus(daysDiff, DAYS);
            }
        }
    }

    //-----------------------------------------------------------------------
    public static TTemporalAdjuster next(TDayOfWeek dayOfWeek) {
        return new RelativeDayOfWeek(2, dayOfWeek);
    }

    public static TTemporalAdjuster nextOrSame(TDayOfWeek dayOfWeek) {
        return new RelativeDayOfWeek(0, dayOfWeek);
    }

    public static TTemporalAdjuster previous(TDayOfWeek dayOfWeek) {
        return new RelativeDayOfWeek(3, dayOfWeek);
    }

    public static TTemporalAdjuster previousOrSame(TDayOfWeek dayOfWeek) {
        return new RelativeDayOfWeek(1, dayOfWeek);
    }

    private static final class RelativeDayOfWeek implements TTemporalAdjuster {
        private final int relative;
        private final int dowValue;

        private RelativeDayOfWeek(int relative, TDayOfWeek dayOfWeek) {
            TJdk8Methods.requireNonNull(dayOfWeek, "dayOfWeek");
            this.relative = relative;
            this.dowValue = dayOfWeek.getValue();
        }

        @Override
        public TTemporal adjustInto(TTemporal temporal) {
            int calDow = temporal.get(DAY_OF_WEEK);
            if (relative < 2 && calDow == dowValue) {
                return temporal;
            }
            if ((relative & 1) == 0) {
                int daysDiff = calDow - dowValue;
                return temporal.plus(daysDiff >= 0 ? 7 - daysDiff : -daysDiff, DAYS);
            } else {
                int daysDiff = dowValue - calDow;
                return temporal.minus(daysDiff >= 0 ? 7 - daysDiff : -daysDiff, DAYS);
            }
        }
    }

}
