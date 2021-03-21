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
package org.threeten.bp.chrono;

import static org.threeten.bp.chrono.MinguoChronology.YEARS_DIFFERENCE;
import static org.threeten.bp.temporal.ChronoField.YEAR;
import java.io.Serializable;
import java.util.Objects;
import org.threeten.bp.Clock;
import org.threeten.bp.DateTimeException;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.Period;
import org.threeten.bp.ZoneId;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.TemporalAccessor;
import org.threeten.bp.temporal.TemporalAdjuster;
import org.threeten.bp.temporal.TemporalAmount;
import org.threeten.bp.temporal.TemporalField;
import org.threeten.bp.temporal.TemporalQuery;
import org.threeten.bp.temporal.TemporalUnit;
import org.threeten.bp.temporal.UnsupportedTemporalTypeException;
import org.threeten.bp.temporal.ValueRange;

/**
 * A date in the Minguo calendar system.
 * <p>
 * This date operates using the {@linkplain MinguoChronology Minguo calendar}.
 * This calendar system is primarily used in the Republic of China, often known as Taiwan.
 * Dates are aligned such that {@code 0001-01-01 (Minguo)} is {@code 1912-01-01 (ISO)}.
 *
 * <h3>Specification for implementors</h3>
 * This class is immutable and thread-safe.
 */
public final class MinguoDate
        extends ChronoDateImpl<MinguoDate>
        implements Serializable {

    /**
     * The underlying date.
     */
    private final LocalDate isoDate;

    //-----------------------------------------------------------------------
    /**
     * Obtains the current {@code MinguoDate} from the system clock in the default time-zone.
     * <p>
     * This will query the {@link Clock#systemDefaultZone() system clock} in the default
     * time-zone to obtain the current date.
     * <p>
     * Using this method will prevent the ability to use an alternate clock for testing
     * because the clock is hard-coded.
     *
     * @return the current date using the system clock and default time-zone, not null
     */
    public static MinguoDate now() {
        return now(Clock.systemDefaultZone());
    }

    /**
     * Obtains the current {@code MinguoDate} from the system clock in the specified time-zone.
     * <p>
     * This will query the {@link Clock#system(ZoneId) system clock} to obtain the current date.
     * Specifying the time-zone avoids dependence on the default time-zone.
     * <p>
     * Using this method will prevent the ability to use an alternate clock for testing
     * because the clock is hard-coded.
     *
     * @param zone  the zone ID to use, not null
     * @return the current date using the system clock, not null
     */
    public static MinguoDate now(ZoneId zone) {
        return now(Clock.system(zone));
    }

    /**
     * Obtains the current {@code MinguoDate} from the specified clock.
     * <p>
     * This will query the specified clock to obtain the current date - today.
     * Using this method allows the use of an alternate clock for testing.
     * The alternate clock may be introduced using {@linkplain Clock dependency injection}.
     *
     * @param clock  the clock to use, not null
     * @return the current date, not null
     * @throws DateTimeException if the current date cannot be obtained
     */
    public static MinguoDate now(Clock clock) {
        return new MinguoDate(LocalDate.now(clock));
    }

    /**
     * Obtains a {@code MinguoDate} representing a date in the Minguo calendar
     * system from the proleptic-year, month-of-year and day-of-month fields.
     * <p>
     * This returns a {@code MinguoDate} with the specified fields.
     * The day must be valid for the year and month, otherwise an exception will be thrown.
     *
     * @param prolepticYear  the Minguo proleptic-year
     * @param month  the Minguo month-of-year, from 1 to 12
     * @param dayOfMonth  the Minguo day-of-month, from 1 to 31
     * @return the date in Minguo calendar system, not null
     * @throws DateTimeException if the value of any field is out of range,
     *  or if the day-of-month is invalid for the month-year
     */
    public static MinguoDate of(int prolepticYear, int month, int dayOfMonth) {
        return MinguoChronology.INSTANCE.date(prolepticYear, month, dayOfMonth);
    }

    /**
     * Obtains a {@code MinguoDate} from a temporal object.
     * <p>
     * This obtains a date in the Minguo calendar system based on the specified temporal.
     * A {@code TemporalAccessor} represents an arbitrary set of date and time information,
     * which this factory converts to an instance of {@code MinguoDate}.
     * <p>
     * The conversion typically uses the {@link ChronoField#EPOCH_DAY EPOCH_DAY}
     * field, which is standardized across calendar systems.
     * <p>
     * This method matches the signature of the functional interface {@link TemporalQuery}
     * allowing it to be used as a query via method reference, {@code MinguoDate::from}.
     *
     * @param temporal  the temporal object to convert, not null
     * @return the date in Minguo calendar system, not null
     * @throws DateTimeException if unable to convert to a {@code MinguoDate}
     */
    public static MinguoDate from(TemporalAccessor temporal) {
        return MinguoChronology.INSTANCE.date(temporal);
    }

    //-----------------------------------------------------------------------
    /**
     * Creates an instance from an ISO date.
     *
     * @param date  the standard local date, validated not null
     */
    MinguoDate(LocalDate date) {
        Objects.requireNonNull(date, "date");
        this.isoDate = date;
    }

    //-----------------------------------------------------------------------
    @Override
    public MinguoChronology getChronology() {
        return MinguoChronology.INSTANCE;
    }

    @Override
    public MinguoEra getEra() {
        return (MinguoEra) super.getEra();
    }

    @Override
    public int lengthOfMonth() {
        return isoDate.lengthOfMonth();
    }

    @Override
    public ValueRange range(TemporalField field) {
        if (field instanceof ChronoField) {
            if (isSupported(field)) {
                ChronoField f = (ChronoField) field;
                switch (f) {
                    case DAY_OF_MONTH:
                    case DAY_OF_YEAR:
                    case ALIGNED_WEEK_OF_MONTH:
                        return isoDate.range(field);
                    case YEAR_OF_ERA: {
                        ValueRange range = YEAR.range();
                        long max = getProlepticYear() <= 0
                                ? -range.getMinimum() + 1 + YEARS_DIFFERENCE
                                : range.getMaximum() - YEARS_DIFFERENCE;
                        return ValueRange.of(1, max);
                    }
                }
                return getChronology().range(f);
            }
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    @Override
    public long getLong(TemporalField field) {
        if (field instanceof ChronoField) {
            switch ((ChronoField) field) {
                case PROLEPTIC_MONTH:
                    return getProlepticMonth();
                case YEAR_OF_ERA: {
                    int prolepticYear = getProlepticYear();
                    return prolepticYear >= 1 ? prolepticYear : 1 - prolepticYear;
                }
                case YEAR:
                    return getProlepticYear();
                case ERA:
                    return getProlepticYear() >= 1 ? 1 : 0;
            }
            return isoDate.getLong(field);
        }
        return field.getFrom(this);
    }

    private long getProlepticMonth() {
        return getProlepticYear() * 12L + isoDate.getMonthValue() - 1;
    }

    private int getProlepticYear() {
        return isoDate.getYear() - YEARS_DIFFERENCE;
    }

    //-----------------------------------------------------------------------
    @Override
    public MinguoDate with(TemporalAdjuster adjuster) {
        return (MinguoDate) super.with(adjuster);
    }

    @Override
    public MinguoDate with(TemporalField field, long newValue) {
        if (field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            if (getLong(f) == newValue) {
                return this;
            }
            switch (f) {
                case PROLEPTIC_MONTH:
                    getChronology().range(f).checkValidValue(newValue, f);
                    return plusMonths(newValue - getProlepticMonth());
                case YEAR_OF_ERA:
                case YEAR:
                case ERA: {
                    int nvalue = getChronology().range(f).checkValidIntValue(newValue, f);
                    switch (f) {
                        case YEAR_OF_ERA:
                            return with(isoDate.withYear(getProlepticYear() >= 1
                                    ? nvalue + YEARS_DIFFERENCE
                                    : (1 - nvalue) + YEARS_DIFFERENCE));
                        case YEAR:
                            return with(isoDate.withYear(nvalue + YEARS_DIFFERENCE));
                        case ERA:
                            return with(isoDate.withYear((1 - getProlepticYear()) + YEARS_DIFFERENCE));
                    }
                }
            }
            return with(isoDate.with(field, newValue));
        }
        return field.adjustInto(this, newValue);
    }

    @Override
    public MinguoDate plus(TemporalAmount amount) {
        return (MinguoDate) super.plus(amount);
    }

    @Override
    public MinguoDate plus(long amountToAdd, TemporalUnit unit) {
        return (MinguoDate) super.plus(amountToAdd, unit);
    }

    @Override
    public MinguoDate minus(TemporalAmount amount) {
        return (MinguoDate) super.minus(amount);
    }

    @Override
    public MinguoDate minus(long amountToAdd, TemporalUnit unit) {
        return (MinguoDate) super.minus(amountToAdd, unit);
    }

    //-----------------------------------------------------------------------
    @Override
    MinguoDate plusYears(long years) {
        return with(isoDate.plusYears(years));
    }

    @Override
    MinguoDate plusMonths(long months) {
        return with(isoDate.plusMonths(months));
    }

    @Override
    MinguoDate plusDays(long days) {
        return with(isoDate.plusDays(days));
    }

    private MinguoDate with(LocalDate newDate) {
        return newDate.equals(isoDate) ? this : new MinguoDate(newDate);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChronoLocalDateTime<MinguoDate> atTime(LocalTime localTime) {
        return (ChronoLocalDateTime<MinguoDate>) super.atTime(localTime);
    }

    @Override
    public ChronoPeriod until(ChronoLocalDate endDate) {
        Period period = isoDate.until(endDate);
        return getChronology().period(period.getYears(), period.getMonths(), period.getDays());
    }

    @Override  // override for performance
    public long toEpochDay() {
        return isoDate.toEpochDay();
    }

    //-------------------------------------------------------------------------
    @Override  // override for performance
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MinguoDate) {
            MinguoDate otherDate = (MinguoDate) obj;
            return this.isoDate.equals(otherDate.isoDate);
        }
        return false;
    }

    @Override  // override for performance
    public int hashCode() {
        return getChronology().getId().hashCode() ^ isoDate.hashCode();
    }
}
