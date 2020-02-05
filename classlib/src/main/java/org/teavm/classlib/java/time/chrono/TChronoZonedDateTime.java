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

import static org.teavm.classlib.java.time.temporal.TChronoField.INSTANT_SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoField.OFFSET_SECONDS;
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
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.temporal.TValueRange;

public interface TChronoZonedDateTime<D extends TChronoLocalDate>
        extends TTemporal, Comparable<TChronoZonedDateTime<?>> {

    public static Comparator<TChronoZonedDateTime<?>> timeLineOrder() {

        return (datetime1, datetime2) -> {
            int cmp = Long.compare(datetime1.toEpochSecond(), datetime2.toEpochSecond());
            if (cmp == 0) {
                cmp = Long.compare(datetime1.toLocalTime().toNanoOfDay(), datetime2.toLocalTime().toNanoOfDay());
            }
            return cmp;
        };
    }

    static TChronoZonedDateTime<?> from(TTemporalAccessor temporal) {

        Objects.requireNonNull(temporal, "temporal");
        if (temporal instanceof TChronoZonedDateTime) {
            return (TChronoZonedDateTime<?>) temporal;
        }
        TChronology chrono = temporal.query(TTemporalQueries.chronology());
        if (chrono == null) {
            throw new TDateTimeException("No TChronology found to create TChronoZonedDateTime: " + temporal.getClass());
        }
        return chrono.zonedDateTime(temporal);
    }

    @Override
    default TValueRange range(TTemporalField field) {

        if (field instanceof TChronoField) {
            if (field == INSTANT_SECONDS || field == OFFSET_SECONDS) {
                return field.range();
            }
            return toLocalDateTime().range(field);
        }
        return field.rangeRefinedBy(this);
    }

    @Override
    default int get(TTemporalField field) {

        if (field instanceof TChronoField) {
            switch ((TChronoField) field) {
                case INSTANT_SECONDS:
                    throw new TUnsupportedTemporalTypeException("Field too large for an int: " + field);
                case OFFSET_SECONDS:
                    return getOffset().getTotalSeconds();
            }
            return toLocalDateTime().get(field);
        }
        return TTemporal.super.get(field);
    }

    @Override
    default long getLong(TTemporalField field) {

        if (field instanceof TChronoField) {
            switch ((TChronoField) field) {
                case INSTANT_SECONDS:
                    return toEpochSecond();
                case OFFSET_SECONDS:
                    return getOffset().getTotalSeconds();
            }
            return toLocalDateTime().getLong(field);
        }
        return field.getFrom(this);
    }

    default D toLocalDate() {

        return toLocalDateTime().toLocalDate();
    }

    default TLocalTime toLocalTime() {

        return toLocalDateTime().toLocalTime();
    }

    TChronoLocalDateTime<D> toLocalDateTime();

    default TChronology getChronology() {

        return toLocalDate().getChronology();
    }

    TZoneOffset getOffset();

    TZoneId getZone();

    TChronoZonedDateTime<D> withEarlierOffsetAtOverlap();

    TChronoZonedDateTime<D> withLaterOffsetAtOverlap();

    TChronoZonedDateTime<D> withZoneSameLocal(TZoneId zoneId);

    TChronoZonedDateTime<D> withZoneSameInstant(TZoneId zoneId);

    @Override
    default TChronoZonedDateTime<D> with(TTemporalAdjuster adjuster) {

        return ((TAbstractChronology) getChronology()).ensureChronoZonedDateTime(TTemporal.super.with(adjuster));
    }

    @Override
    TChronoZonedDateTime<D> with(TTemporalField field, long newValue);

    @Override
    default TChronoZonedDateTime<D> plus(TTemporalAmount amount) {

        return ((TAbstractChronology) getChronology()).ensureChronoZonedDateTime(TTemporal.super.plus(amount));
    }

    @Override
    TChronoZonedDateTime<D> plus(long amountToAdd, TTemporalUnit unit);

    @Override
    default TChronoZonedDateTime<D> minus(TTemporalAmount amount) {

        return ((TAbstractChronology) getChronology()).ensureChronoZonedDateTime(TTemporal.super.minus(amount));
    }

    @Override
    default TChronoZonedDateTime<D> minus(long amountToSubtract, TTemporalUnit unit) {

        return ((TAbstractChronology) getChronology())
                .ensureChronoZonedDateTime(TTemporal.super.minus(amountToSubtract, unit));
    }

    @SuppressWarnings("unchecked")
    @Override
    default <R> R query(TTemporalQuery<R> query) {

        if (query == TTemporalQueries.zoneId() || query == TTemporalQueries.zone()) {
            return (R) getZone();
        } else if (query == TTemporalQueries.chronology()) {
            return (R) toLocalDate().getChronology();
        } else if (query == TTemporalQueries.precision()) {
            return (R) NANOS;
        } else if (query == TTemporalQueries.offset()) {
            return (R) getOffset();
        } else if (query == TTemporalQueries.localDate()) {
            return (R) TLocalDate.ofEpochDay(toLocalDate().toEpochDay());
        } else if (query == TTemporalQueries.localTime()) {
            return (R) toLocalTime();
        }
        return TTemporal.super.query(query);
    }

    default String format(TDateTimeFormatter formatter) {

        Objects.requireNonNull(formatter, "formatter");
        return formatter.format(this);
    }

    default TInstant toInstant() {

        return TInstant.ofEpochSecond(toEpochSecond(), toLocalTime().getNano());
    }

    default long toEpochSecond() {

        long epochDay = toLocalDate().toEpochDay();
        long secs = epochDay * 86400 + toLocalTime().toSecondOfDay();
        secs -= getOffset().getTotalSeconds();
        return secs;
    }

    @Override
    default int compareTo(TChronoZonedDateTime<?> other) {

        int cmp = Long.compare(toEpochSecond(), other.toEpochSecond());
        if (cmp == 0) {
            cmp = toLocalTime().getNano() - other.toLocalTime().getNano();
            if (cmp == 0) {
                cmp = toLocalDateTime().compareTo(other.toLocalDateTime());
                if (cmp == 0) {
                    cmp = getZone().getId().compareTo(other.getZone().getId());
                    if (cmp == 0) {
                        cmp = toLocalDate().getChronology().compareTo(other.toLocalDate().getChronology());
                    }
                }
            }
        }
        return cmp;
    }

    default boolean isAfter(TChronoZonedDateTime<?> other) {

        long thisEpochSec = toEpochSecond();
        long otherEpochSec = other.toEpochSecond();
        return thisEpochSec > otherEpochSec
                || (thisEpochSec == otherEpochSec && toLocalTime().getNano() > other.toLocalTime().getNano());
    }

    default boolean isBefore(TChronoZonedDateTime<?> other) {

        long thisEpochSec = toEpochSecond();
        long otherEpochSec = other.toEpochSecond();
        return thisEpochSec < otherEpochSec
                || (thisEpochSec == otherEpochSec && toLocalTime().getNano() < other.toLocalTime().getNano());
    }

    default boolean isEqual(TChronoZonedDateTime<?> other) {

        return toEpochSecond() == other.toEpochSecond() && toLocalTime().getNano() == other.toLocalTime().getNano();
    }

}
