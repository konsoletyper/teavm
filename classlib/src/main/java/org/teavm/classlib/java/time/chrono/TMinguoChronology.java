/*
 *  Copyright 2020, adopted to TeaVM by Joerg Hohwiller
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
package org.teavm.classlib.java.time.chrono;

import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_WEEK_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_WEEK_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.ERA;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.PROLEPTIC_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR_OF_ERA;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MONTHS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.WEEKS;
import static org.teavm.classlib.java.time.temporal.TTemporalAdjusters.nextOrSame;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.time.TClock;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TDayOfWeek;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.format.TResolverStyle;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TValueRange;

public final class TMinguoChronology extends TAbstractChronology implements TSerializable {

    public static final TMinguoChronology INSTANCE = new TMinguoChronology();

    static final int YEARS_DIFFERENCE = 1911;

    private TMinguoChronology() {

    }

    @Override
    public String getId() {

        return "Minguo";
    }

    @Override
    public String getCalendarType() {

        return "roc";
    }

    @Override
    public TMinguoDate date(TEra era, int yearOfEra, int month, int dayOfMonth) {

        return (TMinguoDate) super.date(era, yearOfEra, month, dayOfMonth);
    }

    @Override
    public TMinguoDate date(int prolepticYear, int month, int dayOfMonth) {

        return new TMinguoDate(TLocalDate.of(prolepticYear + YEARS_DIFFERENCE, month, dayOfMonth));
    }

    @Override
    public TMinguoDate dateYearDay(TEra era, int yearOfEra, int dayOfYear) {

        return (TMinguoDate) super.dateYearDay(era, yearOfEra, dayOfYear);
    }

    @Override
    public TMinguoDate dateYearDay(int prolepticYear, int dayOfYear) {

        return new TMinguoDate(TLocalDate.ofYearDay(prolepticYear + YEARS_DIFFERENCE, dayOfYear));
    }

    @Override
    public TMinguoDate dateEpochDay(long epochDay) {

        return new TMinguoDate(TLocalDate.ofEpochDay(epochDay));
    }

    @Override
    public TMinguoDate date(TTemporalAccessor temporal) {

        if (temporal instanceof TMinguoDate) {
            return (TMinguoDate) temporal;
        }
        return new TMinguoDate(TLocalDate.from(temporal));
    }

    @SuppressWarnings("unchecked")
    @Override
    public TChronoLocalDateTime<TMinguoDate> localDateTime(TTemporalAccessor temporal) {

        return (TChronoLocalDateTime<TMinguoDate>) super.localDateTime(temporal);
    }

    @SuppressWarnings("unchecked")
    @Override
    public TChronoZonedDateTime<TMinguoDate> zonedDateTime(TTemporalAccessor temporal) {

        return (TChronoZonedDateTime<TMinguoDate>) super.zonedDateTime(temporal);
    }

    @SuppressWarnings("unchecked")
    @Override
    public TChronoZonedDateTime<TMinguoDate> zonedDateTime(TInstant instant, TZoneId zone) {

        return (TChronoZonedDateTime<TMinguoDate>) super.zonedDateTime(instant, zone);
    }

    @Override
    public TMinguoDate dateNow() {

        return (TMinguoDate) super.dateNow();
    }

    @Override
    public TMinguoDate dateNow(TZoneId zone) {

        return (TMinguoDate) super.dateNow(zone);
    }

    @Override
    public TMinguoDate dateNow(TClock clock) {

        Objects.requireNonNull(clock, "clock");
        return (TMinguoDate) super.dateNow(clock);
    }

    @Override
    public boolean isLeapYear(long prolepticYear) {

        return TIsoChronology.INSTANCE.isLeapYear(prolepticYear + YEARS_DIFFERENCE);
    }

    @Override
    public int prolepticYear(TEra era, int yearOfEra) {

        if (era instanceof TMinguoEra == false) {
            throw new ClassCastException("Era must be MinguoEra");
        }
        return (era == TMinguoEra.ROC ? yearOfEra : 1 - yearOfEra);
    }

    @Override
    public TMinguoEra eraOf(int eraValue) {

        return TMinguoEra.of(eraValue);
    }

    @Override
    public List<TEra> eras() {

        return Arrays.<TEra> asList(TMinguoEra.values());
    }

    @Override
    public TValueRange range(TChronoField field) {

        switch (field) {
            case PROLEPTIC_MONTH: {
                TValueRange range = PROLEPTIC_MONTH.range();
                return TValueRange.of(range.getMinimum() - YEARS_DIFFERENCE * 12L,
                        range.getMaximum() - YEARS_DIFFERENCE * 12L);
            }
            case YEAR_OF_ERA: {
                TValueRange range = YEAR.range();
                return TValueRange.of(1, range.getMaximum() - YEARS_DIFFERENCE,
                        -range.getMinimum() + 1 + YEARS_DIFFERENCE);
            }
            case YEAR: {
                TValueRange range = YEAR.range();
                return TValueRange.of(range.getMinimum() - YEARS_DIFFERENCE, range.getMaximum() - YEARS_DIFFERENCE);
            }
        }
        return field.range();
    }

    @Override
    public TMinguoDate resolveDate(Map<TTemporalField, Long> fieldValues, TResolverStyle resolverStyle) {

        if (fieldValues.containsKey(EPOCH_DAY)) {
            return dateEpochDay(fieldValues.remove(EPOCH_DAY));
        }

        // normalize fields
        Long prolepticMonth = fieldValues.remove(PROLEPTIC_MONTH);
        if (prolepticMonth != null) {
            if (resolverStyle != TResolverStyle.LENIENT) {
                PROLEPTIC_MONTH.checkValidValue(prolepticMonth);
            }
            updateResolveMap(fieldValues, MONTH_OF_YEAR, Math.floorMod(prolepticMonth, 12) + 1);
            updateResolveMap(fieldValues, YEAR, Math.floorDiv(prolepticMonth, 12));
        }

        // eras
        Long yoeLong = fieldValues.remove(YEAR_OF_ERA);
        if (yoeLong != null) {
            if (resolverStyle != TResolverStyle.LENIENT) {
                YEAR_OF_ERA.checkValidValue(yoeLong);
            }
            Long era = fieldValues.remove(ERA);
            if (era == null) {
                Long year = fieldValues.get(YEAR);
                if (resolverStyle == TResolverStyle.STRICT) {
                    // do not invent era if strict, but do cross-check with year
                    if (year != null) {
                        updateResolveMap(fieldValues, YEAR, (year > 0 ? yoeLong : Math.subtractExact(1, yoeLong)));
                    } else {
                        // reinstate the field removed earlier, no cross-check issues
                        fieldValues.put(YEAR_OF_ERA, yoeLong);
                    }
                } else {
                    // invent era
                    updateResolveMap(fieldValues, YEAR,
                            (year == null || year > 0 ? yoeLong : Math.subtractExact(1, yoeLong)));
                }
            } else if (era.longValue() == 1L) {
                updateResolveMap(fieldValues, YEAR, yoeLong);
            } else if (era.longValue() == 0L) {
                updateResolveMap(fieldValues, YEAR, Math.subtractExact(1, yoeLong));
            } else {
                throw new TDateTimeException("Invalid value for era: " + era);
            }
        } else if (fieldValues.containsKey(ERA)) {
            ERA.checkValidValue(fieldValues.get(ERA)); // always validated
        }

        // build date
        if (fieldValues.containsKey(YEAR)) {
            if (fieldValues.containsKey(MONTH_OF_YEAR)) {
                if (fieldValues.containsKey(DAY_OF_MONTH)) {
                    int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                    if (resolverStyle == TResolverStyle.LENIENT) {
                        long months = Math.subtractExact(fieldValues.remove(MONTH_OF_YEAR), 1);
                        long days = Math.subtractExact(fieldValues.remove(DAY_OF_MONTH), 1);
                        return date(y, 1, 1).plusMonths(months).plusDays(days);
                    } else {
                        int moy = range(MONTH_OF_YEAR).checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR),
                                MONTH_OF_YEAR);
                        int dom = range(DAY_OF_MONTH).checkValidIntValue(fieldValues.remove(DAY_OF_MONTH),
                                DAY_OF_MONTH);
                        if (resolverStyle == TResolverStyle.SMART && dom > 28) {
                            dom = Math.min(dom, date(y, moy, 1).lengthOfMonth());
                        }
                        return date(y, moy, dom);
                    }
                }
                if (fieldValues.containsKey(ALIGNED_WEEK_OF_MONTH)) {
                    if (fieldValues.containsKey(ALIGNED_DAY_OF_WEEK_IN_MONTH)) {
                        int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                        if (resolverStyle == TResolverStyle.LENIENT) {
                            long months = Math.subtractExact(fieldValues.remove(MONTH_OF_YEAR), 1);
                            long weeks = Math.subtractExact(fieldValues.remove(ALIGNED_WEEK_OF_MONTH), 1);
                            long days = Math.subtractExact(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_MONTH), 1);
                            return date(y, 1, 1).plus(months, MONTHS).plus(weeks, WEEKS).plus(days, DAYS);
                        }
                        int moy = MONTH_OF_YEAR.checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR));
                        int aw = ALIGNED_WEEK_OF_MONTH.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_MONTH));
                        int ad = ALIGNED_DAY_OF_WEEK_IN_MONTH
                                .checkValidIntValue(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_MONTH));
                        TMinguoDate date = date(y, moy, 1).plus((aw - 1) * 7 + (ad - 1), DAYS);
                        if (resolverStyle == TResolverStyle.STRICT && date.get(MONTH_OF_YEAR) != moy) {
                            throw new TDateTimeException("Strict mode rejected date parsed to a different month");
                        }
                        return date;
                    }
                    if (fieldValues.containsKey(DAY_OF_WEEK)) {
                        int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                        if (resolverStyle == TResolverStyle.LENIENT) {
                            long months = Math.subtractExact(fieldValues.remove(MONTH_OF_YEAR), 1);
                            long weeks = Math.subtractExact(fieldValues.remove(ALIGNED_WEEK_OF_MONTH), 1);
                            long days = Math.subtractExact(fieldValues.remove(DAY_OF_WEEK), 1);
                            return date(y, 1, 1).plus(months, MONTHS).plus(weeks, WEEKS).plus(days, DAYS);
                        }
                        int moy = MONTH_OF_YEAR.checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR));
                        int aw = ALIGNED_WEEK_OF_MONTH.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_MONTH));
                        int dow = DAY_OF_WEEK.checkValidIntValue(fieldValues.remove(DAY_OF_WEEK));
                        TMinguoDate date = date(y, moy, 1).plus(aw - 1, WEEKS).with(nextOrSame(TDayOfWeek.of(dow)));
                        if (resolverStyle == TResolverStyle.STRICT && date.get(MONTH_OF_YEAR) != moy) {
                            throw new TDateTimeException("Strict mode rejected date parsed to a different month");
                        }
                        return date;
                    }
                }
            }
            if (fieldValues.containsKey(DAY_OF_YEAR)) {
                int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                if (resolverStyle == TResolverStyle.LENIENT) {
                    long days = Math.subtractExact(fieldValues.remove(DAY_OF_YEAR), 1);
                    return dateYearDay(y, 1).plusDays(days);
                }
                int doy = DAY_OF_YEAR.checkValidIntValue(fieldValues.remove(DAY_OF_YEAR));
                return dateYearDay(y, doy);
            }
            if (fieldValues.containsKey(ALIGNED_WEEK_OF_YEAR)) {
                if (fieldValues.containsKey(ALIGNED_DAY_OF_WEEK_IN_YEAR)) {
                    int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                    if (resolverStyle == TResolverStyle.LENIENT) {
                        long weeks = Math.subtractExact(fieldValues.remove(ALIGNED_WEEK_OF_YEAR), 1);
                        long days = Math.subtractExact(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_YEAR), 1);
                        return date(y, 1, 1).plus(weeks, WEEKS).plus(days, DAYS);
                    }
                    int aw = ALIGNED_WEEK_OF_YEAR.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_YEAR));
                    int ad = ALIGNED_DAY_OF_WEEK_IN_YEAR
                            .checkValidIntValue(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_YEAR));
                    TMinguoDate date = date(y, 1, 1).plusDays((aw - 1) * 7 + (ad - 1));
                    if (resolverStyle == TResolverStyle.STRICT && date.get(YEAR) != y) {
                        throw new TDateTimeException("Strict mode rejected date parsed to a different year");
                    }
                    return date;
                }
                if (fieldValues.containsKey(DAY_OF_WEEK)) {
                    int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                    if (resolverStyle == TResolverStyle.LENIENT) {
                        long weeks = Math.subtractExact(fieldValues.remove(ALIGNED_WEEK_OF_YEAR), 1);
                        long days = Math.subtractExact(fieldValues.remove(DAY_OF_WEEK), 1);
                        return date(y, 1, 1).plus(weeks, WEEKS).plus(days, DAYS);
                    }
                    int aw = ALIGNED_WEEK_OF_YEAR.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_YEAR));
                    int dow = DAY_OF_WEEK.checkValidIntValue(fieldValues.remove(DAY_OF_WEEK));
                    TMinguoDate date = date(y, 1, 1).plus(aw - 1, WEEKS).with(nextOrSame(TDayOfWeek.of(dow)));
                    if (resolverStyle == TResolverStyle.STRICT && date.get(YEAR) != y) {
                        throw new TDateTimeException("Strict mode rejected date parsed to a different month");
                    }
                    return date;
                }
            }
        }
        return null;
    }

}
