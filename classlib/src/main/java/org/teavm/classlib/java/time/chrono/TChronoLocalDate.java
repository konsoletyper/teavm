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

import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.ERA;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;

import java.util.Comparator;
import java.util.Objects;

import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;

public interface TChronoLocalDate extends TTemporal, TTemporalAdjuster, Comparable<TChronoLocalDate> {

    public static Comparator<TChronoLocalDate> timeLineOrder() {

        return (date1, date2) -> {
            return Long.compare(date1.toEpochDay(), date2.toEpochDay());
        };
    }

    public static TChronoLocalDate from(TTemporalAccessor temporal) {

        Objects.requireNonNull(temporal, "temporal");
        if (temporal instanceof TChronoLocalDate) {
            return (TChronoLocalDate) temporal;
        }
        TChronology chrono = temporal.query(TTemporalQueries.chronology());
        if (chrono == null) {
            throw new TDateTimeException("No TChronology found to create TChronoLocalDate: " + temporal.getClass());
        }
        return chrono.date(temporal);
    }

    TChronology getChronology();

    default TEra getEra() {

        return getChronology().eraOf(get(ERA));
    }

    default boolean isLeapYear() {

        return getChronology().isLeapYear(getLong(YEAR));
    }

    int lengthOfMonth();

    default int lengthOfYear() {

        return (isLeapYear() ? 366 : 365);
    }

    @Override
    default boolean isSupported(TTemporalField field) {

        if (field instanceof TChronoField) {
            return field.isDateBased();
        }
        return field != null && field.isSupportedBy(this);
    }

    @Override
    default boolean isSupported(TTemporalUnit unit) {

        if (unit instanceof TChronoUnit) {
            return unit.isDateBased();
        }
        return unit != null && unit.isSupportedBy(this);
    }

    @Override
    default TChronoLocalDate with(TTemporalAdjuster adjuster) {

        return ((TAbstractChronology) getChronology()).ensureChronoLocalDate(TTemporal.super.with(adjuster));
    }

    @Override
    TChronoLocalDate with(TTemporalField field, long newValue);

    @Override
    default TChronoLocalDate plus(TTemporalAmount amount) {

        return ((TAbstractChronology) getChronology()).ensureChronoLocalDate(TTemporal.super.plus(amount));
    }

    @Override
    TChronoLocalDate plus(long amountToAdd, TTemporalUnit unit);

    @Override
    default TChronoLocalDate minus(TTemporalAmount amount) {

        return ((TAbstractChronology) getChronology()).ensureChronoLocalDate(TTemporal.super.minus(amount));
    }

    @Override
    default TChronoLocalDate minus(long amountToSubtract, TTemporalUnit unit) {

        return ((TAbstractChronology) getChronology())
                .ensureChronoLocalDate(TTemporal.super.minus(amountToSubtract, unit));
    }

    @SuppressWarnings("unchecked")
    @Override
    default <R> R query(TTemporalQuery<R> query) {

        if (query == TTemporalQueries.chronology()) {
            return (R) getChronology();
        } else if (query == TTemporalQueries.precision()) {
            return (R) TChronoUnit.DAYS;
        } else if (query == TTemporalQueries.localDate()) {
            return (R) TLocalDate.ofEpochDay(toEpochDay());
        } else if (query == TTemporalQueries.localTime() || query == TTemporalQueries.zone()
                || query == TTemporalQueries.zoneId() || query == TTemporalQueries.offset()) {
            return null;
        }
        return TTemporal.super.query(query);
    }

    @Override
    default TTemporal adjustInto(TTemporal temporal) {

        return temporal.with(EPOCH_DAY, toEpochDay());
    }

    TChronoPeriod until(TChronoLocalDate endDateExclusive);

    default String format(TDateTimeFormatter formatter) {

        Objects.requireNonNull(formatter, "formatter");
        return formatter.format(this);
    }

    default TChronoLocalDateTime<?> atTime(TLocalTime localTime) {

        return TChronoLocalDateTimeImpl.of(this, localTime);
    }

    default long toEpochDay() {

        return getLong(EPOCH_DAY);
    }

    @Override
    default int compareTo(TChronoLocalDate other) {

        int cmp = Long.compare(toEpochDay(), other.toEpochDay());
        if (cmp == 0) {
            cmp = getChronology().compareTo(other.getChronology());
        }
        return cmp;
    }

    default boolean isAfter(TChronoLocalDate other) {

        return toEpochDay() > other.toEpochDay();
    }

    default boolean isBefore(TChronoLocalDate other) {

        return toEpochDay() < other.toEpochDay();
    }

    default boolean isEqual(TChronoLocalDate other) {

        return toEpochDay() == other.toEpochDay();
    }

}
