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

import static org.threeten.bp.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static org.threeten.bp.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static org.threeten.bp.temporal.ChronoField.ALIGNED_WEEK_OF_MONTH;
import static org.threeten.bp.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR;
import static org.threeten.bp.temporal.ChronoField.DAY_OF_MONTH;
import static org.threeten.bp.temporal.ChronoField.DAY_OF_WEEK;
import static org.threeten.bp.temporal.ChronoField.DAY_OF_YEAR;
import static org.threeten.bp.temporal.ChronoField.EPOCH_DAY;
import static org.threeten.bp.temporal.ChronoField.ERA;
import static org.threeten.bp.temporal.ChronoField.MONTH_OF_YEAR;
import static org.threeten.bp.temporal.ChronoField.PROLEPTIC_MONTH;
import static org.threeten.bp.temporal.ChronoField.YEAR;
import static org.threeten.bp.temporal.ChronoField.YEAR_OF_ERA;
import static org.threeten.bp.temporal.ChronoUnit.DAYS;
import static org.threeten.bp.temporal.ChronoUnit.MONTHS;
import static org.threeten.bp.temporal.ChronoUnit.WEEKS;
import static org.threeten.bp.temporal.TemporalAdjusters.nextOrSame;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.threeten.bp.Clock;
import org.threeten.bp.DateTimeException;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.ResolverStyle;
import org.threeten.bp.jdk8.Jdk8Methods;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.TemporalAccessor;
import org.threeten.bp.temporal.TemporalField;
import org.threeten.bp.temporal.ValueRange;

/**
 * The Minguo calendar system.
 * <p>
 * This chronology defines the rules of the Minguo calendar system.
 * This calendar system is primarily used in the Republic of China, often known as Taiwan.
 * Dates are aligned such that {@code 0001-01-01 (Minguo)} is {@code 1912-01-01 (ISO)}.
 * <p>
 * The fields are defined as follows:
 * <p><ul>
 * <li>era - There are two eras, the current 'Republic' (ROC) and the previous era (BEFORE_ROC).
 * <li>year-of-era - The year-of-era for the current era increases uniformly from the epoch at year one.
 *  For the previous era the year increases from one as time goes backwards.
 *  The value for the current era is equal to the ISO proleptic-year minus 1911.
 * <li>proleptic-year - The proleptic year is the same as the year-of-era for the
 *  current era. For the previous era, years have zero, then negative values.
 *  The value is equal to the ISO proleptic-year minus 1911.
 * <li>month-of-year - The Minguo month-of-year exactly matches ISO.
 * <li>day-of-month - The Minguo day-of-month exactly matches ISO.
 * <li>day-of-year - The Minguo day-of-year exactly matches ISO.
 * <li>leap-year - The Minguo leap-year pattern exactly matches ISO, such that the two calendars
 *  are never out of step.
 * </ul><p>
 *
 * <h3>Specification for implementors</h3>
 * This class is immutable and thread-safe.
 */
public final class MinguoChronology extends Chronology implements Serializable {

    /**
     * Singleton instance for the Minguo chronology.
     */
    public static final MinguoChronology INSTANCE = new MinguoChronology();

    /**
     * The difference in years between ISO and Minguo.
     */
    static final int YEARS_DIFFERENCE = 1911;

    /**
     * Restricted constructor.
     */
    private MinguoChronology() {
    }

    /**
     * Resolve singleton.
     *
     * @return the singleton instance, not null
     */
    private Object readResolve() {
        return INSTANCE;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the ID of the chronology - 'Minguo'.
     * <p>
     * The ID uniquely identifies the {@code Chronology}.
     * It can be used to lookup the {@code Chronology} using {@link #of(String)}.
     *
     * @return the chronology ID - 'Minguo'
     * @see #getCalendarType()
     */
    @Override
    public String getId() {
        return "Minguo";
    }

    /**
     * Gets the calendar type of the underlying calendar system - 'roc'.
     * <p>
     * The calendar type is an identifier defined by the
     * <em>Unicode Locale Data Markup Language (LDML)</em> specification.
     * It can be used to lookup the {@code Chronology} using {@link #of(String)}.
     * It can also be used as part of a locale, accessible via
     * {@link Locale#getUnicodeLocaleType(String)} with the key 'ca'.
     *
     * @return the calendar system type - 'roc'
     * @see #getId()
     */
    @Override
    public String getCalendarType() {
        return "roc";
    }

    //-----------------------------------------------------------------------
    @Override  // override with covariant return type
    public MinguoDate date(Era era, int yearOfEra, int month, int dayOfMonth) {
        return (MinguoDate) super.date(era, yearOfEra, month, dayOfMonth);
    }

    @Override  // override with covariant return type
    public MinguoDate date(int prolepticYear, int month, int dayOfMonth) {
        return new MinguoDate(LocalDate.of(prolepticYear + YEARS_DIFFERENCE, month, dayOfMonth));
    }

    @Override  // override with covariant return type
    public MinguoDate dateYearDay(Era era, int yearOfEra, int dayOfYear) {
        return (MinguoDate) super.dateYearDay(era, yearOfEra, dayOfYear);
    }

    @Override  // override with covariant return type
    public MinguoDate dateYearDay(int prolepticYear, int dayOfYear) {
        return new MinguoDate(LocalDate.ofYearDay(prolepticYear + YEARS_DIFFERENCE, dayOfYear));
    }

    @Override
    public MinguoDate dateEpochDay(long epochDay) {
        return new MinguoDate(LocalDate.ofEpochDay(epochDay));
    }

    //-----------------------------------------------------------------------
    @Override  // override with covariant return type
    public MinguoDate date(TemporalAccessor temporal) {
        if (temporal instanceof MinguoDate) {
            return (MinguoDate) temporal;
        }
        return new MinguoDate(LocalDate.from(temporal));
    }

    @SuppressWarnings("unchecked")
    @Override  // override with covariant return type
    public ChronoLocalDateTime<MinguoDate> localDateTime(TemporalAccessor temporal) {
        return (ChronoLocalDateTime<MinguoDate>) super.localDateTime(temporal);
    }

    @SuppressWarnings("unchecked")
    @Override  // override with covariant return type
    public ChronoZonedDateTime<MinguoDate> zonedDateTime(TemporalAccessor temporal) {
        return (ChronoZonedDateTime<MinguoDate>) super.zonedDateTime(temporal);
    }

    @SuppressWarnings("unchecked")
    @Override  // override with covariant return type
    public ChronoZonedDateTime<MinguoDate> zonedDateTime(Instant instant, ZoneId zone) {
        return (ChronoZonedDateTime<MinguoDate>) super.zonedDateTime(instant, zone);
    }

    //-----------------------------------------------------------------------
    @Override  // override with covariant return type
    public MinguoDate dateNow() {
        return (MinguoDate) super.dateNow();
    }

    @Override  // override with covariant return type
    public MinguoDate dateNow(ZoneId zone) {
        return (MinguoDate) super.dateNow(zone);
    }

    @Override  // override with covariant return type
    public MinguoDate dateNow(Clock clock) {
        Objects.requireNonNull(clock, "clock");
        return (MinguoDate) super.dateNow(clock);
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if the specified year is a leap year.
     * <p>
     * Minguo leap years occur exactly in line with ISO leap years.
     * This method does not validate the year passed in, and only has a
     * well-defined result for years in the supported range.
     *
     * @param prolepticYear  the proleptic-year to check, not validated for range
     * @return true if the year is a leap year
     */
    @Override
    public boolean isLeapYear(long prolepticYear) {
        return IsoChronology.INSTANCE.isLeapYear(prolepticYear + YEARS_DIFFERENCE);
    }

    @Override
    public int prolepticYear(Era era, int yearOfEra) {
        if (!(era instanceof MinguoEra)) {
            throw new ClassCastException("Era must be MinguoEra");
        }
        return era == MinguoEra.ROC ? yearOfEra : 1 - yearOfEra;
    }

    @Override
    public MinguoEra eraOf(int eraValue) {
        return MinguoEra.of(eraValue);
    }

    @Override
    public List<Era> eras() {
        return Arrays.<Era>asList(MinguoEra.values());
    }

    //-----------------------------------------------------------------------
    @Override
    public ValueRange range(ChronoField field) {
        switch (field) {
            case PROLEPTIC_MONTH: {
                ValueRange range = PROLEPTIC_MONTH.range();
                return ValueRange.of(range.getMinimum() - YEARS_DIFFERENCE * 12L,
                        range.getMaximum() - YEARS_DIFFERENCE * 12L);
            }
            case YEAR_OF_ERA: {
                ValueRange range = YEAR.range();
                return ValueRange.of(1, range.getMaximum() - YEARS_DIFFERENCE,
                        -range.getMinimum() + 1 + YEARS_DIFFERENCE);
            }
            case YEAR: {
                ValueRange range = YEAR.range();
                return ValueRange.of(range.getMinimum() - YEARS_DIFFERENCE, range.getMaximum() - YEARS_DIFFERENCE);
            }
        }
        return field.range();
    }

    @Override
    public MinguoDate resolveDate(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        if (fieldValues.containsKey(EPOCH_DAY)) {
            return dateEpochDay(fieldValues.remove(EPOCH_DAY));
        }

        // normalize fields
        Long prolepticMonth = fieldValues.remove(PROLEPTIC_MONTH);
        if (prolepticMonth != null) {
            if (resolverStyle != ResolverStyle.LENIENT) {
                PROLEPTIC_MONTH.checkValidValue(prolepticMonth);
            }
            updateResolveMap(fieldValues, MONTH_OF_YEAR, Jdk8Methods.floorMod(prolepticMonth, 12) + 1);
            updateResolveMap(fieldValues, YEAR, Jdk8Methods.floorDiv(prolepticMonth, 12));
        }

        // eras
        Long yoeLong = fieldValues.remove(YEAR_OF_ERA);
        if (yoeLong != null) {
            if (resolverStyle != ResolverStyle.LENIENT) {
                YEAR_OF_ERA.checkValidValue(yoeLong);
            }
            Long era = fieldValues.remove(ERA);
            if (era == null) {
                Long year = fieldValues.get(YEAR);
                if (resolverStyle == ResolverStyle.STRICT) {
                    // do not invent era if strict, but do cross-check with year
                    if (year != null) {
                        updateResolveMap(fieldValues, YEAR, year > 0 ? yoeLong : Jdk8Methods.safeSubtract(1, yoeLong));
                    } else {
                        // reinstate the field removed earlier, no cross-check issues
                        fieldValues.put(YEAR_OF_ERA, yoeLong);
                    }
                } else {
                    // invent era
                    updateResolveMap(fieldValues, YEAR, year == null || year > 0
                            ? yoeLong
                            : Jdk8Methods.safeSubtract(1, yoeLong));
                }
            } else if (era == 1L) {
                updateResolveMap(fieldValues, YEAR, yoeLong);
            } else if (era == 0L) {
                updateResolveMap(fieldValues, YEAR, Jdk8Methods.safeSubtract(1, yoeLong));
            } else {
                throw new DateTimeException("Invalid value for era: " + era);
            }
        } else if (fieldValues.containsKey(ERA)) {
            ERA.checkValidValue(fieldValues.get(ERA));  // always validated
        }

        // build date
        if (fieldValues.containsKey(YEAR)) {
            if (fieldValues.containsKey(MONTH_OF_YEAR)) {
                if (fieldValues.containsKey(DAY_OF_MONTH)) {
                    int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                    if (resolverStyle == ResolverStyle.LENIENT) {
                        long months = Jdk8Methods.safeSubtract(fieldValues.remove(MONTH_OF_YEAR), 1);
                        long days = Jdk8Methods.safeSubtract(fieldValues.remove(DAY_OF_MONTH), 1);
                        return date(y, 1, 1).plusMonths(months).plusDays(days);
                    } else {
                        int moy = range(MONTH_OF_YEAR).checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR),
                                MONTH_OF_YEAR);
                        int dom = range(DAY_OF_MONTH).checkValidIntValue(fieldValues.remove(DAY_OF_MONTH),
                                DAY_OF_MONTH);
                        if (resolverStyle == ResolverStyle.SMART && dom > 28) {
                            dom = Math.min(dom, date(y, moy, 1).lengthOfMonth());
                        }
                        return date(y, moy, dom);
                    }
                }
                if (fieldValues.containsKey(ALIGNED_WEEK_OF_MONTH)) {
                    if (fieldValues.containsKey(ALIGNED_DAY_OF_WEEK_IN_MONTH)) {
                        int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                        if (resolverStyle == ResolverStyle.LENIENT) {
                            long months = Jdk8Methods.safeSubtract(fieldValues.remove(MONTH_OF_YEAR), 1);
                            long weeks = Jdk8Methods.safeSubtract(fieldValues.remove(ALIGNED_WEEK_OF_MONTH), 1);
                            long days = Jdk8Methods.safeSubtract(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_MONTH), 1);
                            return date(y, 1, 1).plus(months, MONTHS).plus(weeks, WEEKS).plus(days, DAYS);
                        }
                        int moy = MONTH_OF_YEAR.checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR));
                        int aw = ALIGNED_WEEK_OF_MONTH.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_MONTH));
                        int ad = ALIGNED_DAY_OF_WEEK_IN_MONTH.checkValidIntValue(
                                fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_MONTH));
                        MinguoDate date = date(y, moy, 1).plus((aw - 1) * 7 + (ad - 1), DAYS);
                        if (resolverStyle == ResolverStyle.STRICT && date.get(MONTH_OF_YEAR) != moy) {
                            throw new DateTimeException("Strict mode rejected date parsed to a different month");
                        }
                        return date;
                    }
                    if (fieldValues.containsKey(DAY_OF_WEEK)) {
                        int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                        if (resolverStyle == ResolverStyle.LENIENT) {
                            long months = Jdk8Methods.safeSubtract(fieldValues.remove(MONTH_OF_YEAR), 1);
                            long weeks = Jdk8Methods.safeSubtract(fieldValues.remove(ALIGNED_WEEK_OF_MONTH), 1);
                            long days = Jdk8Methods.safeSubtract(fieldValues.remove(DAY_OF_WEEK), 1);
                            return date(y, 1, 1).plus(months, MONTHS).plus(weeks, WEEKS).plus(days, DAYS);
                        }
                        int moy = MONTH_OF_YEAR.checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR));
                        int aw = ALIGNED_WEEK_OF_MONTH.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_MONTH));
                        int dow = DAY_OF_WEEK.checkValidIntValue(fieldValues.remove(DAY_OF_WEEK));
                        MinguoDate date = date(y, moy, 1).plus(aw - 1, WEEKS).with(nextOrSame(DayOfWeek.of(dow)));
                        if (resolverStyle == ResolverStyle.STRICT && date.get(MONTH_OF_YEAR) != moy) {
                            throw new DateTimeException("Strict mode rejected date parsed to a different month");
                        }
                        return date;
                    }
                }
            }
            if (fieldValues.containsKey(DAY_OF_YEAR)) {
                int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                if (resolverStyle == ResolverStyle.LENIENT) {
                    long days = Jdk8Methods.safeSubtract(fieldValues.remove(DAY_OF_YEAR), 1);
                    return dateYearDay(y, 1).plusDays(days);
                }
                int doy = DAY_OF_YEAR.checkValidIntValue(fieldValues.remove(DAY_OF_YEAR));
                return dateYearDay(y, doy);
            }
            if (fieldValues.containsKey(ALIGNED_WEEK_OF_YEAR)) {
                if (fieldValues.containsKey(ALIGNED_DAY_OF_WEEK_IN_YEAR)) {
                    int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                    if (resolverStyle == ResolverStyle.LENIENT) {
                        long weeks = Jdk8Methods.safeSubtract(fieldValues.remove(ALIGNED_WEEK_OF_YEAR), 1);
                        long days = Jdk8Methods.safeSubtract(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_YEAR), 1);
                        return date(y, 1, 1).plus(weeks, WEEKS).plus(days, DAYS);
                    }
                    int aw = ALIGNED_WEEK_OF_YEAR.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_YEAR));
                    int ad = ALIGNED_DAY_OF_WEEK_IN_YEAR.checkValidIntValue(
                            fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_YEAR));
                    MinguoDate date = date(y, 1, 1).plusDays((aw - 1) * 7 + (ad - 1));
                    if (resolverStyle == ResolverStyle.STRICT && date.get(YEAR) != y) {
                        throw new DateTimeException("Strict mode rejected date parsed to a different year");
                    }
                    return date;
                }
                if (fieldValues.containsKey(DAY_OF_WEEK)) {
                    int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                    if (resolverStyle == ResolverStyle.LENIENT) {
                        long weeks = Jdk8Methods.safeSubtract(fieldValues.remove(ALIGNED_WEEK_OF_YEAR), 1);
                        long days = Jdk8Methods.safeSubtract(fieldValues.remove(DAY_OF_WEEK), 1);
                        return date(y, 1, 1).plus(weeks, WEEKS).plus(days, DAYS);
                    }
                    int aw = ALIGNED_WEEK_OF_YEAR.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_YEAR));
                    int dow = DAY_OF_WEEK.checkValidIntValue(fieldValues.remove(DAY_OF_WEEK));
                    MinguoDate date = date(y, 1, 1).plus(aw - 1, WEEKS).with(nextOrSame(DayOfWeek.of(dow)));
                    if (resolverStyle == ResolverStyle.STRICT && date.get(YEAR) != y) {
                        throw new DateTimeException("Strict mode rejected date parsed to a different month");
                    }
                    return date;
                }
            }
        }
        return null;
    }

}
