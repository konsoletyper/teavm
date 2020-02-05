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
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TMonth;
import org.teavm.classlib.java.time.TYear;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZonedDateTime;
import org.teavm.classlib.java.time.format.TResolverStyle;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TValueRange;

public final class TIsoChronology extends TAbstractChronology implements TSerializable {

    public static final TIsoChronology INSTANCE = new TIsoChronology();

    private TIsoChronology() {

    }

    @Override
    public String getId() {

        return "ISO";
    }

    @Override
    public String getCalendarType() {

        return "iso8601";
    }

    @Override
    public TLocalDate date(TEra era, int yearOfEra, int month, int dayOfMonth) {

        return date(prolepticYear(era, yearOfEra), month, dayOfMonth);
    }

    @Override
    public TLocalDate date(int prolepticYear, int month, int dayOfMonth) {

        return TLocalDate.of(prolepticYear, month, dayOfMonth);
    }

    @Override
    public TLocalDate dateYearDay(TEra era, int yearOfEra, int dayOfYear) {

        return dateYearDay(prolepticYear(era, yearOfEra), dayOfYear);
    }

    @Override
    public TLocalDate dateYearDay(int prolepticYear, int dayOfYear) {

        return TLocalDate.ofYearDay(prolepticYear, dayOfYear);
    }

    @Override
    public TLocalDate dateEpochDay(long epochDay) {

        return TLocalDate.ofEpochDay(epochDay);
    }

    @Override
    public TLocalDate date(TTemporalAccessor temporal) {

        return TLocalDate.from(temporal);
    }

    @Override
    public TLocalDateTime localDateTime(TTemporalAccessor temporal) {

        return TLocalDateTime.from(temporal);
    }

    @Override
    public TZonedDateTime zonedDateTime(TTemporalAccessor temporal) {

        return TZonedDateTime.from(temporal);
    }

    @Override
    public TZonedDateTime zonedDateTime(TInstant instant, TZoneId zone) {

        return TZonedDateTime.ofInstant(instant, zone);
    }

    @Override
    public TLocalDate dateNow() {

        return dateNow(TClock.systemDefaultZone());
    }

    @Override
    public TLocalDate dateNow(TZoneId zone) {

        return dateNow(TClock.system(zone));
    }

    @Override
    public TLocalDate dateNow(TClock clock) {

        Objects.requireNonNull(clock, "clock");
        return date(TLocalDate.now(clock));
    }

    @Override
    public boolean isLeapYear(long prolepticYear) {

        return ((prolepticYear & 3) == 0) && ((prolepticYear % 100) != 0 || (prolepticYear % 400) == 0);
    }

    @Override
    public int prolepticYear(TEra era, int yearOfEra) {

        if (era instanceof TIsoEra == false) {
            throw new ClassCastException("Era must be IsoEra");
        }
        return (era == TIsoEra.CE ? yearOfEra : 1 - yearOfEra);
    }

    @Override
    public TIsoEra eraOf(int eraValue) {

        return TIsoEra.of(eraValue);
    }

    @Override
    public List<TEra> eras() {

        return Arrays.<TEra> asList(TIsoEra.values());
    }

    @Override
    public TValueRange range(TChronoField field) {

        return field.range();
    }

    @Override
    public TLocalDate resolveDate(Map<TTemporalField, Long> fieldValues, TResolverStyle resolverStyle) {

        if (fieldValues.containsKey(EPOCH_DAY)) {
            return TLocalDate.ofEpochDay(fieldValues.remove(EPOCH_DAY));
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
                    int moy = Math.toIntExact(fieldValues.remove(MONTH_OF_YEAR));
                    int dom = Math.toIntExact(fieldValues.remove(DAY_OF_MONTH));
                    if (resolverStyle == TResolverStyle.LENIENT) {
                        long months = Math.subtractExact(moy, 1);
                        long days = Math.subtractExact(dom, 1);
                        return TLocalDate.of(y, 1, 1).plusMonths(months).plusDays(days);
                    } else if (resolverStyle == TResolverStyle.SMART) {
                        DAY_OF_MONTH.checkValidValue(dom);
                        if (moy == 4 || moy == 6 || moy == 9 || moy == 11) {
                            dom = Math.min(dom, 30);
                        } else if (moy == 2) {
                            dom = Math.min(dom, TMonth.FEBRUARY.length(TYear.isLeap(y)));
                        }
                        return TLocalDate.of(y, moy, dom);
                    } else {
                        return TLocalDate.of(y, moy, dom);
                    }
                }
                if (fieldValues.containsKey(ALIGNED_WEEK_OF_MONTH)) {
                    if (fieldValues.containsKey(ALIGNED_DAY_OF_WEEK_IN_MONTH)) {
                        int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                        if (resolverStyle == TResolverStyle.LENIENT) {
                            long months = Math.subtractExact(fieldValues.remove(MONTH_OF_YEAR), 1);
                            long weeks = Math.subtractExact(fieldValues.remove(ALIGNED_WEEK_OF_MONTH), 1);
                            long days = Math.subtractExact(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_MONTH), 1);
                            return TLocalDate.of(y, 1, 1).plusMonths(months).plusWeeks(weeks).plusDays(days);
                        }
                        int moy = MONTH_OF_YEAR.checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR));
                        int aw = ALIGNED_WEEK_OF_MONTH.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_MONTH));
                        int ad = ALIGNED_DAY_OF_WEEK_IN_MONTH
                                .checkValidIntValue(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_MONTH));
                        TLocalDate date = TLocalDate.of(y, moy, 1).plusDays((aw - 1) * 7 + (ad - 1));
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
                            return TLocalDate.of(y, 1, 1).plusMonths(months).plusWeeks(weeks).plusDays(days);
                        }
                        int moy = MONTH_OF_YEAR.checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR));
                        int aw = ALIGNED_WEEK_OF_MONTH.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_MONTH));
                        int dow = DAY_OF_WEEK.checkValidIntValue(fieldValues.remove(DAY_OF_WEEK));
                        TLocalDate date = TLocalDate.of(y, moy, 1).plusWeeks(aw - 1)
                                .with(nextOrSame(TDayOfWeek.of(dow)));
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
                    return TLocalDate.ofYearDay(y, 1).plusDays(days);
                }
                int doy = DAY_OF_YEAR.checkValidIntValue(fieldValues.remove(DAY_OF_YEAR));
                return TLocalDate.ofYearDay(y, doy);
            }
            if (fieldValues.containsKey(ALIGNED_WEEK_OF_YEAR)) {
                if (fieldValues.containsKey(ALIGNED_DAY_OF_WEEK_IN_YEAR)) {
                    int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                    if (resolverStyle == TResolverStyle.LENIENT) {
                        long weeks = Math.subtractExact(fieldValues.remove(ALIGNED_WEEK_OF_YEAR), 1);
                        long days = Math.subtractExact(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_YEAR), 1);
                        return TLocalDate.of(y, 1, 1).plusWeeks(weeks).plusDays(days);
                    }
                    int aw = ALIGNED_WEEK_OF_YEAR.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_YEAR));
                    int ad = ALIGNED_DAY_OF_WEEK_IN_YEAR
                            .checkValidIntValue(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_YEAR));
                    TLocalDate date = TLocalDate.of(y, 1, 1).plusDays((aw - 1) * 7 + (ad - 1));
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
                        return TLocalDate.of(y, 1, 1).plusWeeks(weeks).plusDays(days);
                    }
                    int aw = ALIGNED_WEEK_OF_YEAR.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_YEAR));
                    int dow = DAY_OF_WEEK.checkValidIntValue(fieldValues.remove(DAY_OF_WEEK));
                    TLocalDate date = TLocalDate.of(y, 1, 1).plusWeeks(aw - 1).with(nextOrSame(TDayOfWeek.of(dow)));
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
