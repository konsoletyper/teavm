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

import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.ERAS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.FOREVER;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.HALF_DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.HOURS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MICROS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MILLIS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MINUTES;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MONTHS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.NANOS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.WEEKS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.YEARS;

import java.util.Map;

import org.teavm.classlib.java.time.TYear;
import org.teavm.classlib.java.time.format.TResolverStyle;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;
import org.teavm.classlib.java.util.TLocale;

public enum TChronoField implements TTemporalField {

    NANO_OF_SECOND("NanoOfSecond", NANOS, SECONDS, TValueRange.of(0, 999999999)), //
    NANO_OF_DAY("NanoOfDay", NANOS, DAYS, TValueRange.of(0, 86400L * 1000000000L - 1)), //
    MICRO_OF_SECOND("MicroOfSecond", MICROS, SECONDS, TValueRange.of(0, 999999)), //
    MICRO_OF_DAY("MicroOfDay", MICROS, DAYS, TValueRange.of(0, 86400L * 1000000L - 1)), //
    MILLI_OF_SECOND("MilliOfSecond", MILLIS, SECONDS, TValueRange.of(0, 999)), //
    MILLI_OF_DAY("MilliOfDay", MILLIS, DAYS, TValueRange.of(0, 86400L * 1000L - 1)), //
    SECOND_OF_MINUTE("SecondOfMinute", SECONDS, MINUTES, TValueRange.of(0, 59)), //
    SECOND_OF_DAY("SecondOfDay", SECONDS, DAYS, TValueRange.of(0, 86400L - 1)), //
    MINUTE_OF_HOUR("MinuteOfHour", MINUTES, HOURS, TValueRange.of(0, 59)), //
    MINUTE_OF_DAY("MinuteOfDay", MINUTES, DAYS, TValueRange.of(0, (24 * 60) - 1)), //
    HOUR_OF_AMPM("HourOfAmPm", HOURS, HALF_DAYS, TValueRange.of(0, 11)), //
    CLOCK_HOUR_OF_AMPM("ClockHourOfAmPm", HOURS, HALF_DAYS, TValueRange.of(1, 12)), //
    HOUR_OF_DAY("HourOfDay", HOURS, DAYS, TValueRange.of(0, 23)), //
    CLOCK_HOUR_OF_DAY("ClockHourOfDay", HOURS, DAYS, TValueRange.of(1, 24)), //
    AMPM_OF_DAY("AmPmOfDay", HALF_DAYS, DAYS, TValueRange.of(0, 1)), //
    DAY_OF_WEEK("TDayOfWeek", DAYS, WEEKS, TValueRange.of(1, 7)), //
    ALIGNED_DAY_OF_WEEK_IN_MONTH("AlignedDayOfWeekInMonth", DAYS, WEEKS, TValueRange.of(1, 7)), //
    ALIGNED_DAY_OF_WEEK_IN_YEAR("AlignedDayOfWeekInYear", DAYS, WEEKS, TValueRange.of(1, 7)), //
    DAY_OF_MONTH("DayOfMonth", DAYS, MONTHS, TValueRange.of(1, 28, 31)), //
    DAY_OF_YEAR("DayOfYear", DAYS, YEARS, TValueRange.of(1, 365, 366)), //
    EPOCH_DAY("EpochDay", DAYS, FOREVER, TValueRange.of(-365243219162L, 365241780471L)), //
    ALIGNED_WEEK_OF_MONTH("AlignedWeekOfMonth", WEEKS, MONTHS, TValueRange.of(1, 4, 5)), //
    ALIGNED_WEEK_OF_YEAR("AlignedWeekOfYear", WEEKS, YEARS, TValueRange.of(1, 53)), //
    MONTH_OF_YEAR("MonthOfYear", MONTHS, YEARS, TValueRange.of(1, 12)), //
    PROLEPTIC_MONTH("ProlepticMonth", MONTHS, FOREVER,
            TValueRange.of(TYear.MIN_VALUE * 12L, TYear.MAX_VALUE * 12L + 11)), //
    YEAR_OF_ERA("YearOfEra", YEARS, FOREVER, TValueRange.of(1, TYear.MAX_VALUE, TYear.MAX_VALUE + 1)), //
    YEAR("TYear", YEARS, FOREVER, TValueRange.of(TYear.MIN_VALUE, TYear.MAX_VALUE)), //
    ERA("TEra", ERAS, FOREVER, TValueRange.of(0, 1)), //
    INSTANT_SECONDS("InstantSeconds", SECONDS, FOREVER, TValueRange.of(Long.MIN_VALUE, Long.MAX_VALUE)), //
    OFFSET_SECONDS("OffsetSeconds", SECONDS, FOREVER, TValueRange.of(-18 * 3600, 18 * 3600)); //

    private final String name;

    private final TTemporalUnit baseUnit;

    private final TTemporalUnit rangeUnit;

    private final TValueRange range;

    private TChronoField(String name, TTemporalUnit baseUnit, TTemporalUnit rangeUnit, TValueRange range) {

        this.name = name;
        this.baseUnit = baseUnit;
        this.rangeUnit = rangeUnit;
        this.range = range;
    }

    @Override
    public TTemporalUnit getBaseUnit() {

        return this.baseUnit;
    }

    @Override
    public TTemporalUnit getRangeUnit() {

        return this.rangeUnit;
    }

    @Override
    public TValueRange range() {

        return this.range;
    }

    @Override
    public boolean isDateBased() {

        return ordinal() >= DAY_OF_WEEK.ordinal() && ordinal() <= ERA.ordinal();
    }

    @Override
    public boolean isTimeBased() {

        return ordinal() < DAY_OF_WEEK.ordinal();
    }

    public long checkValidValue(long value) {

        return range().checkValidValue(value, this);
    }

    public int checkValidIntValue(long value) {

        return range().checkValidIntValue(value, this);
    }

    @Override
    public boolean isSupportedBy(TTemporalAccessor temporal) {

        return temporal.isSupported(this);
    }

    @Override
    public TValueRange rangeRefinedBy(TTemporalAccessor temporal) {

        return temporal.range(this);
    }

    @Override
    public long getFrom(TTemporalAccessor temporal) {

        return temporal.getLong(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends TTemporal> R adjustInto(R temporal, long newValue) {

        return (R) temporal.with(this, newValue);
    }

    @Override
    public String getDisplayName(TLocale locale) {

        TJdk8Methods.requireNonNull(locale, "locale");
        return toString();
    }

    @Override
    public TTemporalAccessor resolve(Map<TTemporalField, Long> fieldValues, TTemporalAccessor partialTemporal,
            TResolverStyle resolverStyle) {

        return null; // resolve implemented in builder
    }

    @Override
    public String toString() {

        return this.name;
    }

}
