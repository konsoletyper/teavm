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
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.NANOS;

import java.util.Comparator;
import java.util.Objects;

import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;

public interface TChronoLocalDateTime<D extends TChronoLocalDate>
        extends TTemporal, TTemporalAdjuster, Comparable<TChronoLocalDateTime<?>> {

    static Comparator<TChronoLocalDateTime<?>> timeLineOrder() {

        return (datetime1, datetime2) -> {
            int cmp = Long.compare(datetime1.toLocalDate().toEpochDay(), datetime2.toLocalDate().toEpochDay());
            if (cmp == 0) {
                cmp = Long.compare(datetime1.toLocalTime().toNanoOfDay(), datetime2.toLocalTime().toNanoOfDay());
            }
            return cmp;
        };
    }

    static TChronoLocalDateTime<?> from(TTemporalAccessor temporal) {

        Objects.requireNonNull(temporal, "temporal");
        if (temporal instanceof TChronoLocalDateTime) {
            return (TChronoLocalDateTime<?>) temporal;
        }
        TChronology chrono = temporal.query(TTemporalQueries.chronology());
        if (chrono == null) {
            throw new TDateTimeException("No TChronology found to create TChronoLocalDateTime: " + temporal.getClass());
        }
        return chrono.localDateTime(temporal);
    }

    default TChronology getChronology() {

        return toLocalDate().getChronology();
    }

    D toLocalDate();

    TLocalTime toLocalTime();

    @Override
    default TChronoLocalDateTime<D> with(TTemporalAdjuster adjuster) {

        return ((TAbstractChronology) getChronology()).ensureChronoLocalDateTime(TTemporal.super.with(adjuster));
    }

    @Override
    TChronoLocalDateTime<D> with(TTemporalField field, long newValue);

    @Override
    default TChronoLocalDateTime<D> plus(TTemporalAmount amount) {

        return ((TAbstractChronology) getChronology()).ensureChronoLocalDateTime(TTemporal.super.plus(amount));
    }

    @Override
    TChronoLocalDateTime<D> plus(long amountToAdd, TTemporalUnit unit);

    @Override
    default TChronoLocalDateTime<D> minus(TTemporalAmount amount) {

        return ((TAbstractChronology) getChronology()).ensureChronoLocalDateTime(TTemporal.super.minus(amount));
    }

    @Override
    default TChronoLocalDateTime<D> minus(long amountToSubtract, TTemporalUnit unit) {

        return ((TAbstractChronology) getChronology())
                .ensureChronoLocalDateTime(TTemporal.super.minus(amountToSubtract, unit));
    }

    @SuppressWarnings("unchecked")
    @Override
    default <R> R query(TTemporalQuery<R> query) {

        if (query == TTemporalQueries.chronology()) {
            return (R) getChronology();
        } else if (query == TTemporalQueries.precision()) {
            return (R) NANOS;
        } else if (query == TTemporalQueries.localDate()) {
            return (R) TLocalDate.ofEpochDay(toLocalDate().toEpochDay());
        } else if (query == TTemporalQueries.localTime()) {
            return (R) toLocalTime();
        } else if (query == TTemporalQueries.zone() || query == TTemporalQueries.zoneId()
                || query == TTemporalQueries.offset()) {
            return null;
        }
        return TTemporal.super.query(query);
    }

    @Override
    default TTemporal adjustInto(TTemporal temporal) {

        return temporal.with(EPOCH_DAY, toLocalDate().toEpochDay()).with(NANO_OF_DAY, toLocalTime().toNanoOfDay());
    }

    default String format(TDateTimeFormatter formatter) {

        Objects.requireNonNull(formatter, "formatter");
        return formatter.format(this);
    }

    TChronoZonedDateTime<D> atZone(TZoneId zone);

    default TInstant toInstant(TZoneOffset offset) {

        return TInstant.ofEpochSecond(toEpochSecond(offset), toLocalTime().getNano());
    }

    default long toEpochSecond(TZoneOffset offset) {

        Objects.requireNonNull(offset, "offset");
        long epochDay = toLocalDate().toEpochDay();
        long secs = epochDay * 86400 + toLocalTime().toSecondOfDay();
        secs -= offset.getTotalSeconds();
        return secs;
    }

    @Override
    default int compareTo(TChronoLocalDateTime<?> other) {

        int cmp = toLocalDate().compareTo(other.toLocalDate());
        if (cmp == 0) {
            cmp = toLocalTime().compareTo(other.toLocalTime());
            if (cmp == 0) {
                cmp = getChronology().compareTo(other.getChronology());
            }
        }
        return cmp;
    }

    default boolean isAfter(TChronoLocalDateTime<?> other) {

        long thisEpDay = this.toLocalDate().toEpochDay();
        long otherEpDay = other.toLocalDate().toEpochDay();
        return thisEpDay > otherEpDay
                || (thisEpDay == otherEpDay && this.toLocalTime().toNanoOfDay() > other.toLocalTime().toNanoOfDay());
    }

    default boolean isBefore(TChronoLocalDateTime<?> other) {

        long thisEpDay = this.toLocalDate().toEpochDay();
        long otherEpDay = other.toLocalDate().toEpochDay();
        return thisEpDay < otherEpDay
                || (thisEpDay == otherEpDay && this.toLocalTime().toNanoOfDay() < other.toLocalTime().toNanoOfDay());
    }

    default boolean isEqual(TChronoLocalDateTime<?> other) {

        // Do the time check first, it is cheaper than computing EPOCH day.
        return this.toLocalTime().toNanoOfDay() == other.toLocalTime().toNanoOfDay()
                && this.toLocalDate().toEpochDay() == other.toLocalDate().toEpochDay();
    }

}
