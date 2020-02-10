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
package org.teavm.classlib.java.time;

import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.INSTANT_SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.OFFSET_SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.NANOS;

import java.util.Comparator;
import java.util.Objects;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
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
import org.teavm.classlib.java.time.temporal.TValueRange;
import org.teavm.classlib.java.time.zone.TZoneRules;

public final class TOffsetDateTime implements TTemporal, TTemporalAdjuster, Comparable<TOffsetDateTime>, TSerializable {

    public static final TOffsetDateTime MIN = TLocalDateTime.MIN.atOffset(TZoneOffset.MAX);

    public static final TOffsetDateTime MAX = TLocalDateTime.MAX.atOffset(TZoneOffset.MIN);

    public static final TTemporalQuery<TOffsetDateTime> FROM = new TTemporalQuery<TOffsetDateTime>() {
        @Override
        public TOffsetDateTime queryFrom(TTemporalAccessor temporal) {

            return TOffsetDateTime.from(temporal);
        }
    };

    public static Comparator<TOffsetDateTime> timeLineOrder() {

        return INSTANT_COMPARATOR;
    }

    private static final Comparator<TOffsetDateTime> INSTANT_COMPARATOR = new Comparator<TOffsetDateTime>() {
        @Override
        public int compare(TOffsetDateTime datetime1, TOffsetDateTime datetime2) {

            int cmp = Long.compare(datetime1.toEpochSecond(), datetime2.toEpochSecond());
            if (cmp == 0) {
                cmp = Long.compare(datetime1.getNano(), datetime2.getNano());
            }
            return cmp;
        }
    };

    private final TLocalDateTime dateTime;

    private final TZoneOffset offset;

    public static TOffsetDateTime now() {

        return now(TClock.systemDefaultZone());
    }

    public static TOffsetDateTime now(TZoneId zone) {

        return now(TClock.system(zone));
    }

    public static TOffsetDateTime now(TClock clock) {

        Objects.requireNonNull(clock, "clock");
        final TInstant now = clock.instant(); // called once
        return ofInstant(now, clock.getZone().getRules().getOffset(now));
    }

    public static TOffsetDateTime of(TLocalDate date, TLocalTime time, TZoneOffset offset) {

        TLocalDateTime dt = TLocalDateTime.of(date, time);
        return new TOffsetDateTime(dt, offset);
    }

    public static TOffsetDateTime of(TLocalDateTime dateTime, TZoneOffset offset) {

        return new TOffsetDateTime(dateTime, offset);
    }

    public static TOffsetDateTime of(int year, int month, int dayOfMonth, int hour, int minute, int second,
            int nanoOfSecond, TZoneOffset offset) {

        TLocalDateTime dt = TLocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond);
        return new TOffsetDateTime(dt, offset);
    }

    public static TOffsetDateTime ofInstant(TInstant instant, TZoneId zone) {

        Objects.requireNonNull(instant, "instant");
        Objects.requireNonNull(zone, "zone");
        TZoneRules rules = zone.getRules();
        TZoneOffset offset = rules.getOffset(instant);
        TLocalDateTime ldt = TLocalDateTime.ofEpochSecond(instant.getEpochSecond(), instant.getNano(), offset);
        return new TOffsetDateTime(ldt, offset);
    }

    public static TOffsetDateTime from(TTemporalAccessor temporal) {

        if (temporal instanceof TOffsetDateTime) {
            return (TOffsetDateTime) temporal;
        }
        try {
            TZoneOffset offset = TZoneOffset.from(temporal);
            try {
                TLocalDateTime ldt = TLocalDateTime.from(temporal);
                return TOffsetDateTime.of(ldt, offset);
            } catch (TDateTimeException ignore) {
                TInstant instant = TInstant.from(temporal);
                return TOffsetDateTime.ofInstant(instant, offset);
            }
        } catch (TDateTimeException ex) {
            throw new TDateTimeException("Unable to obtain TOffsetDateTime from TTemporalAccessor: " + temporal
                    + ", type " + temporal.getClass().getName());
        }
    }

    public static TOffsetDateTime parse(CharSequence text) {

        return parse(text, TDateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static TOffsetDateTime parse(CharSequence text, TDateTimeFormatter formatter) {

        Objects.requireNonNull(formatter, "formatter");
        return formatter.parse(text, TOffsetDateTime.FROM);
    }

    private TOffsetDateTime(TLocalDateTime dateTime, TZoneOffset offset) {

        this.dateTime = Objects.requireNonNull(dateTime, "dateTime");
        this.offset = Objects.requireNonNull(offset, "offset");
    }

    private TOffsetDateTime with(TLocalDateTime dateTime, TZoneOffset offset) {

        if (this.dateTime == dateTime && this.offset.equals(offset)) {
            return this;
        }
        return new TOffsetDateTime(dateTime, offset);
    }

    @Override
    public boolean isSupported(TTemporalField field) {

        return field instanceof TChronoField || (field != null && field.isSupportedBy(this));
    }

    @Override
    public boolean isSupported(TTemporalUnit unit) {

        if (unit instanceof TChronoUnit) {
            return unit.isDateBased() || unit.isTimeBased();
        }
        return unit != null && unit.isSupportedBy(this);
    }

    @Override
    public TValueRange range(TTemporalField field) {

        if (field instanceof TChronoField) {
            if (field == INSTANT_SECONDS || field == OFFSET_SECONDS) {
                return field.range();
            }
            return this.dateTime.range(field);
        }
        return field.rangeRefinedBy(this);
    }

    @Override
    public int get(TTemporalField field) {

        if (field instanceof TChronoField) {
            switch ((TChronoField) field) {
                case INSTANT_SECONDS:
                    throw new TDateTimeException("Field too large for an int: " + field);
                case OFFSET_SECONDS:
                    return getOffset().getTotalSeconds();
            }
            return this.dateTime.get(field);
        }
        return TTemporal.super.get(field);
    }

    @Override
    public long getLong(TTemporalField field) {

        if (field instanceof TChronoField) {
            switch ((TChronoField) field) {
                case INSTANT_SECONDS:
                    return toEpochSecond();
                case OFFSET_SECONDS:
                    return getOffset().getTotalSeconds();
            }
            return this.dateTime.getLong(field);
        }
        return field.getFrom(this);
    }

    public TZoneOffset getOffset() {

        return this.offset;
    }

    public TOffsetDateTime withOffsetSameLocal(TZoneOffset offset) {

        return with(this.dateTime, offset);
    }

    public TOffsetDateTime withOffsetSameInstant(TZoneOffset offset) {

        if (offset.equals(this.offset)) {
            return this;
        }
        int difference = offset.getTotalSeconds() - this.offset.getTotalSeconds();
        TLocalDateTime adjusted = this.dateTime.plusSeconds(difference);
        return new TOffsetDateTime(adjusted, offset);
    }

    public int getYear() {

        return this.dateTime.getYear();
    }

    public int getMonthValue() {

        return this.dateTime.getMonthValue();
    }

    public TMonth getMonth() {

        return this.dateTime.getMonth();
    }

    public int getDayOfMonth() {

        return this.dateTime.getDayOfMonth();
    }

    public int getDayOfYear() {

        return this.dateTime.getDayOfYear();
    }

    public TDayOfWeek getDayOfWeek() {

        return this.dateTime.getDayOfWeek();
    }

    public int getHour() {

        return this.dateTime.getHour();
    }

    public int getMinute() {

        return this.dateTime.getMinute();
    }

    public int getSecond() {

        return this.dateTime.getSecond();
    }

    public int getNano() {

        return this.dateTime.getNano();
    }

    @Override
    public TOffsetDateTime with(TTemporalAdjuster adjuster) {

        // optimizations
        if (adjuster instanceof TLocalDate || adjuster instanceof TLocalTime || adjuster instanceof TLocalDateTime) {
            return with(this.dateTime.with(adjuster), this.offset);
        } else if (adjuster instanceof TInstant) {
            return ofInstant((TInstant) adjuster, this.offset);
        } else if (adjuster instanceof TZoneOffset) {
            return with(this.dateTime, (TZoneOffset) adjuster);
        } else if (adjuster instanceof TOffsetDateTime) {
            return (TOffsetDateTime) adjuster;
        }
        return (TOffsetDateTime) adjuster.adjustInto(this);
    }

    @Override
    public TOffsetDateTime with(TTemporalField field, long newValue) {

        if (field instanceof TChronoField) {
            TChronoField f = (TChronoField) field;
            switch (f) {
                case INSTANT_SECONDS:
                    return ofInstant(TInstant.ofEpochSecond(newValue, getNano()), this.offset);
                case OFFSET_SECONDS: {
                    return with(this.dateTime, TZoneOffset.ofTotalSeconds(f.checkValidIntValue(newValue)));
                }
            }
            return with(this.dateTime.with(field, newValue), this.offset);
        }
        return field.adjustInto(this, newValue);
    }

    public TOffsetDateTime withYear(int year) {

        return with(this.dateTime.withYear(year), this.offset);
    }

    public TOffsetDateTime withMonth(int month) {

        return with(this.dateTime.withMonth(month), this.offset);
    }

    public TOffsetDateTime withDayOfMonth(int dayOfMonth) {

        return with(this.dateTime.withDayOfMonth(dayOfMonth), this.offset);
    }

    public TOffsetDateTime withDayOfYear(int dayOfYear) {

        return with(this.dateTime.withDayOfYear(dayOfYear), this.offset);
    }

    public TOffsetDateTime withHour(int hour) {

        return with(this.dateTime.withHour(hour), this.offset);
    }

    public TOffsetDateTime withMinute(int minute) {

        return with(this.dateTime.withMinute(minute), this.offset);
    }

    public TOffsetDateTime withSecond(int second) {

        return with(this.dateTime.withSecond(second), this.offset);
    }

    public TOffsetDateTime withNano(int nanoOfSecond) {

        return with(this.dateTime.withNano(nanoOfSecond), this.offset);
    }

    public TOffsetDateTime truncatedTo(TTemporalUnit unit) {

        return with(this.dateTime.truncatedTo(unit), this.offset);
    }

    @Override
    public TOffsetDateTime plus(TTemporalAmount amount) {

        return (TOffsetDateTime) amount.addTo(this);
    }

    @Override
    public TOffsetDateTime plus(long amountToAdd, TTemporalUnit unit) {

        if (unit instanceof TChronoUnit) {
            return with(this.dateTime.plus(amountToAdd, unit), this.offset);
        }
        return unit.addTo(this, amountToAdd);
    }

    public TOffsetDateTime plusYears(long years) {

        return with(this.dateTime.plusYears(years), this.offset);
    }

    public TOffsetDateTime plusMonths(long months) {

        return with(this.dateTime.plusMonths(months), this.offset);
    }

    public TOffsetDateTime plusWeeks(long weeks) {

        return with(this.dateTime.plusWeeks(weeks), this.offset);
    }

    public TOffsetDateTime plusDays(long days) {

        return with(this.dateTime.plusDays(days), this.offset);
    }

    public TOffsetDateTime plusHours(long hours) {

        return with(this.dateTime.plusHours(hours), this.offset);
    }

    public TOffsetDateTime plusMinutes(long minutes) {

        return with(this.dateTime.plusMinutes(minutes), this.offset);
    }

    public TOffsetDateTime plusSeconds(long seconds) {

        return with(this.dateTime.plusSeconds(seconds), this.offset);
    }

    public TOffsetDateTime plusNanos(long nanos) {

        return with(this.dateTime.plusNanos(nanos), this.offset);
    }

    @Override
    public TOffsetDateTime minus(TTemporalAmount amount) {

        return (TOffsetDateTime) amount.subtractFrom(this);
    }

    @Override
    public TOffsetDateTime minus(long amountToSubtract, TTemporalUnit unit) {

        return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit)
                : plus(-amountToSubtract, unit));
    }

    public TOffsetDateTime minusYears(long years) {

        return (years == Long.MIN_VALUE ? plusYears(Long.MAX_VALUE).plusYears(1) : plusYears(-years));
    }

    public TOffsetDateTime minusMonths(long months) {

        return (months == Long.MIN_VALUE ? plusMonths(Long.MAX_VALUE).plusMonths(1) : plusMonths(-months));
    }

    public TOffsetDateTime minusWeeks(long weeks) {

        return (weeks == Long.MIN_VALUE ? plusWeeks(Long.MAX_VALUE).plusWeeks(1) : plusWeeks(-weeks));
    }

    public TOffsetDateTime minusDays(long days) {

        return (days == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-days));
    }

    public TOffsetDateTime minusHours(long hours) {

        return (hours == Long.MIN_VALUE ? plusHours(Long.MAX_VALUE).plusHours(1) : plusHours(-hours));
    }

    public TOffsetDateTime minusMinutes(long minutes) {

        return (minutes == Long.MIN_VALUE ? plusMinutes(Long.MAX_VALUE).plusMinutes(1) : plusMinutes(-minutes));
    }

    public TOffsetDateTime minusSeconds(long seconds) {

        return (seconds == Long.MIN_VALUE ? plusSeconds(Long.MAX_VALUE).plusSeconds(1) : plusSeconds(-seconds));
    }

    public TOffsetDateTime minusNanos(long nanos) {

        return (nanos == Long.MIN_VALUE ? plusNanos(Long.MAX_VALUE).plusNanos(1) : plusNanos(-nanos));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TTemporalQuery<R> query) {

        if (query == TTemporalQueries.chronology()) {
            return (R) TIsoChronology.INSTANCE;
        } else if (query == TTemporalQueries.precision()) {
            return (R) NANOS;
        } else if (query == TTemporalQueries.offset() || query == TTemporalQueries.zone()) {
            return (R) getOffset();
        } else if (query == TTemporalQueries.localDate()) {
            return (R) toLocalDate();
        } else if (query == TTemporalQueries.localTime()) {
            return (R) toLocalTime();
        } else if (query == TTemporalQueries.zoneId()) {
            return null;
        }
        return TTemporal.super.query(query);
    }

    @Override
    public TTemporal adjustInto(TTemporal temporal) {

        return temporal.with(EPOCH_DAY, toLocalDate().toEpochDay()).with(NANO_OF_DAY, toLocalTime().toNanoOfDay())
                .with(OFFSET_SECONDS, getOffset().getTotalSeconds());
    }

    @Override
    public long until(TTemporal endExclusive, TTemporalUnit unit) {

        TOffsetDateTime end = TOffsetDateTime.from(endExclusive);
        if (unit instanceof TChronoUnit) {
            end = end.withOffsetSameInstant(this.offset);
            return this.dateTime.until(end.dateTime, unit);
        }
        return unit.between(this, end);
    }

    public TZonedDateTime atZoneSameInstant(TZoneId zone) {

        return TZonedDateTime.ofInstant(this.dateTime, this.offset, zone);
    }

    public TZonedDateTime atZoneSimilarLocal(TZoneId zone) {

        return TZonedDateTime.ofLocal(this.dateTime, zone, this.offset);
    }

    public TLocalDateTime toLocalDateTime() {

        return this.dateTime;
    }

    public TLocalDate toLocalDate() {

        return this.dateTime.toLocalDate();
    }

    public TLocalTime toLocalTime() {

        return this.dateTime.toLocalTime();
    }

    public TOffsetTime toOffsetTime() {

        return TOffsetTime.of(this.dateTime.toLocalTime(), this.offset);
    }

    public TZonedDateTime toZonedDateTime() {

        return TZonedDateTime.of(this.dateTime, this.offset);
    }

    public TInstant toInstant() {

        return this.dateTime.toInstant(this.offset);
    }

    public long toEpochSecond() {

        return this.dateTime.toEpochSecond(this.offset);
    }

    @Override
    public int compareTo(TOffsetDateTime other) {

        if (getOffset().equals(other.getOffset())) {
            return toLocalDateTime().compareTo(other.toLocalDateTime());
        }
        int cmp = Long.compare(toEpochSecond(), other.toEpochSecond());
        if (cmp == 0) {
            cmp = toLocalTime().getNano() - other.toLocalTime().getNano();
            if (cmp == 0) {
                cmp = toLocalDateTime().compareTo(other.toLocalDateTime());
            }
        }
        return cmp;
    }

    public boolean isAfter(TOffsetDateTime other) {

        long thisEpochSec = toEpochSecond();
        long otherEpochSec = other.toEpochSecond();
        return thisEpochSec > otherEpochSec
                || (thisEpochSec == otherEpochSec && toLocalTime().getNano() > other.toLocalTime().getNano());
    }

    public boolean isBefore(TOffsetDateTime other) {

        long thisEpochSec = toEpochSecond();
        long otherEpochSec = other.toEpochSecond();
        return thisEpochSec < otherEpochSec
                || (thisEpochSec == otherEpochSec && toLocalTime().getNano() < other.toLocalTime().getNano());
    }

    public boolean isEqual(TOffsetDateTime other) {

        return toEpochSecond() == other.toEpochSecond() && toLocalTime().getNano() == other.toLocalTime().getNano();
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj instanceof TOffsetDateTime) {
            TOffsetDateTime other = (TOffsetDateTime) obj;
            return this.dateTime.equals(other.dateTime) && this.offset.equals(other.offset);
        }
        return false;
    }

    @Override
    public int hashCode() {

        return this.dateTime.hashCode() ^ this.offset.hashCode();
    }

    @Override
    public String toString() {

        return this.dateTime.toString() + this.offset.toString();
    }

    public String format(TDateTimeFormatter formatter) {

        Objects.requireNonNull(formatter, "formatter");
        return formatter.format(this);
    }

}
