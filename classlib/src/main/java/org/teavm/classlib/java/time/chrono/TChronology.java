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
package org.teavm.classlib.java.time.chrono;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.teavm.classlib.java.lang.TComparable;
import org.teavm.classlib.java.time.TClock;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder;
import org.teavm.classlib.java.time.format.TResolverStyle;
import org.teavm.classlib.java.time.format.TTextStyle;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.temporal.TValueRange;

public interface TChronology extends TComparable<TChronology> {

    static TChronology from(TTemporalAccessor temporal) {

        Objects.requireNonNull(temporal, "temporal");
        TChronology obj = temporal.query(TTemporalQueries.chronology());
        return (obj != null ? obj : TIsoChronology.INSTANCE);
    }

    static TChronology ofLocale(Locale locale) {

        return TAbstractChronology.ofLocale(locale);
    }

    static TChronology of(String id) {

        return TAbstractChronology.of(id);
    }

    static Set<TChronology> getAvailableChronologies() {

        return TAbstractChronology.getAvailableChronologies();
    }

    String getId();

    String getCalendarType();

    default TChronoLocalDate date(TEra era, int yearOfEra, int month, int dayOfMonth) {

        return date(prolepticYear(era, yearOfEra), month, dayOfMonth);
    }

    TChronoLocalDate date(int prolepticYear, int month, int dayOfMonth);

    default TChronoLocalDate dateYearDay(TEra era, int yearOfEra, int dayOfYear) {

        return dateYearDay(prolepticYear(era, yearOfEra), dayOfYear);
    }

    TChronoLocalDate dateYearDay(int prolepticYear, int dayOfYear);

    TChronoLocalDate dateEpochDay(long epochDay);

    TChronoLocalDate date(TTemporalAccessor temporal);

    default TChronoLocalDate dateNow() {

        return dateNow(TClock.systemDefaultZone());
    }

    default TChronoLocalDate dateNow(TZoneId zone) {

        return dateNow(TClock.system(zone));
    }

    default TChronoLocalDate dateNow(TClock clock) {

        Objects.requireNonNull(clock, "clock");
        return date(TLocalDate.now(clock));
    }

    default TChronoLocalDateTime<?> localDateTime(TTemporalAccessor temporal) {

        try {
            TChronoLocalDate date = date(temporal);
            return date.atTime(TLocalTime.from(temporal));
        } catch (TDateTimeException ex) {
            throw new TDateTimeException(
                    "Unable to obtain ChronoLocalDateTime from TemporalAccessor: " + temporal.getClass(), ex);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    default TChronoZonedDateTime<?> zonedDateTime(TTemporalAccessor temporal) {

        try {
            TZoneId zone = TZoneId.from(temporal);
            try {
                TInstant instant = TInstant.from(temporal);
                return zonedDateTime(instant, zone);

            } catch (TDateTimeException ex1) {
                TChronoLocalDateTime cldt = localDateTime(temporal);
                TChronoLocalDateTimeImpl cldtImpl = ((TAbstractChronology) this).ensureChronoLocalDateTime(cldt);
                return TChronoZonedDateTimeImpl.ofBest(cldtImpl, zone, null);
            }
        } catch (TDateTimeException ex) {
            throw new TDateTimeException(
                    "Unable to obtain ChronoZonedDateTime from TemporalAccessor: " + temporal.getClass(), ex);
        }
    }

    default TChronoZonedDateTime<?> zonedDateTime(TInstant instant, TZoneId zone) {

        TChronoZonedDateTime<? extends TChronoLocalDate> result = TChronoZonedDateTimeImpl.ofInstant(this, instant,
                zone);
        return result;
    }

    default TChronoPeriod period(int years, int months, int days) {

        return new TChronoPeriodImpl(this, years, months, days);
    }

    boolean isLeapYear(long prolepticYear);

    int prolepticYear(TEra era, int yearOfEra);

    TEra eraOf(int eraValue);

    List<TEra> eras();

    TValueRange range(TChronoField field);

    default String getDisplayName(TTextStyle style, Locale locale) {

        return new TDateTimeFormatterBuilder().appendChronologyText(style).toFormatter(locale)
                .format(new TTemporalAccessor() {
                    @Override
                    public boolean isSupported(TTemporalField field) {

                        return false;
                    }

                    @Override
                    public long getLong(TTemporalField field) {

                        throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public <R> R query(TTemporalQuery<R> query) {

                        if (query == TTemporalQueries.chronology()) {
                            return (R) TChronology.this;
                        }
                        return TTemporalAccessor.super.query(query);
                    }
                });
    }

    TChronoLocalDate resolveDate(Map<TTemporalField, Long> fieldValues, TResolverStyle resolverStyle);

}
