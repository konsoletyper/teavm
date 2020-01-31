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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Comparator;

import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.format.TDateTimeParseException;
import org.teavm.classlib.java.time.jdk8.TDefaultInterfaceTemporal;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalAdjusters;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;
import org.teavm.classlib.java.time.temporal.TValueRange;
import org.teavm.classlib.java.time.zone.TZoneRules;

public final class TOffsetDateTime
        extends TDefaultInterfaceTemporal
        implements TTemporal, TTemporalAdjuster, Comparable<TOffsetDateTime>, Serializable {

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
            int cmp = TJdk8Methods.compareLongs(datetime1.toEpochSecond(), datetime2.toEpochSecond());
            if (cmp == 0) {
                cmp = TJdk8Methods.compareLongs(datetime1.getNano(), datetime2.getNano());
            }
            return cmp;
        }
    };

    private static final long serialVersionUID = 2287754244819255394L;

    private final TLocalDateTime dateTime;
    private final TZoneOffset offset;

    //-----------------------------------------------------------------------
    public static TOffsetDateTime now() {
        return now(TClock.systemDefaultZone());
    }

    public static TOffsetDateTime now(TZoneId zone) {
        return now(TClock.system(zone));
    }

    public static TOffsetDateTime now(TClock clock) {
        TJdk8Methods.requireNonNull(clock, "clock");
        final TInstant now = clock.instant();  // called once
        return ofInstant(now, clock.getZone().getRules().getOffset(now));
    }

    //-----------------------------------------------------------------------
    public static TOffsetDateTime of(TLocalDate date, TLocalTime time, TZoneOffset offset) {
        TLocalDateTime dt = TLocalDateTime.of(date, time);
        return new TOffsetDateTime(dt, offset);
    }

    public static TOffsetDateTime of(TLocalDateTime dateTime, TZoneOffset offset) {
        return new TOffsetDateTime(dateTime, offset);
    }

    public static TOffsetDateTime of(
            int year, int month, int dayOfMonth,
            int hour, int minute, int second, int nanoOfSecond, TZoneOffset offset) {
        TLocalDateTime dt = TLocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond);
        return new TOffsetDateTime(dt, offset);
    }

    //-----------------------------------------------------------------------
    public static TOffsetDateTime ofInstant(TInstant instant, TZoneId zone) {
        TJdk8Methods.requireNonNull(instant, "instant");
        TJdk8Methods.requireNonNull(zone, "zone");
        TZoneRules rules = zone.getRules();
        TZoneOffset offset = rules.getOffset(instant);
        TLocalDateTime ldt = TLocalDateTime.ofEpochSecond(instant.getEpochSecond(), instant.getNano(), offset);
        return new TOffsetDateTime(ldt, offset);
    }

    //-----------------------------------------------------------------------
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
            throw new TDateTimeException("Unable to obtain TOffsetDateTime from TTemporalAccessor: " +
                    temporal + ", type " + temporal.getClass().getName());
        }
    }

    //-----------------------------------------------------------------------
    public static TOffsetDateTime parse(CharSequence text) {
        return parse(text, TDateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static TOffsetDateTime parse(CharSequence text, TDateTimeFormatter formatter) {
        TJdk8Methods.requireNonNull(formatter, "formatter");
        return formatter.parse(text, TOffsetDateTime.FROM);
    }

    //-----------------------------------------------------------------------
    private TOffsetDateTime(TLocalDateTime dateTime, TZoneOffset offset) {
        this.dateTime = TJdk8Methods.requireNonNull(dateTime, "dateTime");
        this.offset = TJdk8Methods.requireNonNull(offset, "offset");
    }

    private TOffsetDateTime with(TLocalDateTime dateTime, TZoneOffset offset) {
        if (this.dateTime == dateTime && this.offset.equals(offset)) {
            return this;
        }
        return new TOffsetDateTime(dateTime, offset);
    }

    //-----------------------------------------------------------------------
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
            return dateTime.range(field);
        }
        return field.rangeRefinedBy(this);
    }

    @Override
    public int get(TTemporalField field) {
        if (field instanceof TChronoField) {
            switch ((TChronoField) field) {
                case INSTANT_SECONDS: throw new TDateTimeException("Field too large for an int: " + field);
                case OFFSET_SECONDS: return getOffset().getTotalSeconds();
            }
            return dateTime.get(field);
        }
        return super.get(field);
    }

    @Override
    public long getLong(TTemporalField field) {
        if (field instanceof TChronoField) {
            switch ((TChronoField) field) {
                case INSTANT_SECONDS: return toEpochSecond();
                case OFFSET_SECONDS: return getOffset().getTotalSeconds();
            }
            return dateTime.getLong(field);
        }
        return field.getFrom(this);
    }

    //-----------------------------------------------------------------------
    public TZoneOffset getOffset() {
        return offset;
    }

    public TOffsetDateTime withOffsetSameLocal(TZoneOffset offset) {
        return with(dateTime, offset);
    }

    public TOffsetDateTime withOffsetSameInstant(TZoneOffset offset) {
        if (offset.equals(this.offset)) {
            return this;
        }
        int difference = offset.getTotalSeconds() - this.offset.getTotalSeconds();
        TLocalDateTime adjusted = dateTime.plusSeconds(difference);
        return new TOffsetDateTime(adjusted, offset);
    }

    //-----------------------------------------------------------------------
    public int getYear() {
        return dateTime.getYear();
    }

    public int getMonthValue() {
        return dateTime.getMonthValue();
    }

    public TMonth getMonth() {
        return dateTime.getMonth();
    }

    public int getDayOfMonth() {
        return dateTime.getDayOfMonth();
    }

    public int getDayOfYear() {
        return dateTime.getDayOfYear();
    }

    public TDayOfWeek getDayOfWeek() {
        return dateTime.getDayOfWeek();
    }

    //-----------------------------------------------------------------------
    public int getHour() {
        return dateTime.getHour();
    }

    public int getMinute() {
        return dateTime.getMinute();
    }

    public int getSecond() {
        return dateTime.getSecond();
    }

    public int getNano() {
        return dateTime.getNano();
    }

    //-----------------------------------------------------------------------
    @Override
    public TOffsetDateTime with(TTemporalAdjuster adjuster) {
        // optimizations
        if (adjuster instanceof TLocalDate || adjuster instanceof TLocalTime || adjuster instanceof TLocalDateTime) {
            return with(dateTime.with(adjuster), offset);
        } else if (adjuster instanceof TInstant) {
            return ofInstant((TInstant) adjuster, offset);
        } else if (adjuster instanceof TZoneOffset) {
            return with(dateTime, (TZoneOffset) adjuster);
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
                case INSTANT_SECONDS: return ofInstant(TInstant.ofEpochSecond(newValue, getNano()), offset);
                case OFFSET_SECONDS: {
                    return with(dateTime, TZoneOffset.ofTotalSeconds(f.checkValidIntValue(newValue)));
                }
            }
            return with(dateTime.with(field, newValue), offset);
        }
        return field.adjustInto(this, newValue);
    }

    //-----------------------------------------------------------------------
    public TOffsetDateTime withYear(int year) {
        return with(dateTime.withYear(year), offset);
    }

    public TOffsetDateTime withMonth(int month) {
        return with(dateTime.withMonth(month), offset);
    }

    public TOffsetDateTime withDayOfMonth(int dayOfMonth) {
        return with(dateTime.withDayOfMonth(dayOfMonth), offset);
    }

    public TOffsetDateTime withDayOfYear(int dayOfYear) {
        return with(dateTime.withDayOfYear(dayOfYear), offset);
    }

    //-----------------------------------------------------------------------
    public TOffsetDateTime withHour(int hour) {
        return with(dateTime.withHour(hour), offset);
    }

    public TOffsetDateTime withMinute(int minute) {
        return with(dateTime.withMinute(minute), offset);
    }

    public TOffsetDateTime withSecond(int second) {
        return with(dateTime.withSecond(second), offset);
    }

    public TOffsetDateTime withNano(int nanoOfSecond) {
        return with(dateTime.withNano(nanoOfSecond), offset);
    }

    //-----------------------------------------------------------------------
    public TOffsetDateTime truncatedTo(TTemporalUnit unit) {
        return with(dateTime.truncatedTo(unit), offset);
    }

    //-----------------------------------------------------------------------
    @Override
    public TOffsetDateTime plus(TTemporalAmount amount) {
        return (TOffsetDateTime) amount.addTo(this);
    }

    @Override
    public TOffsetDateTime plus(long amountToAdd, TTemporalUnit unit) {
        if (unit instanceof TChronoUnit) {
            return with(dateTime.plus(amountToAdd, unit), offset);
        }
        return unit.addTo(this, amountToAdd);
    }

    //-----------------------------------------------------------------------
    public TOffsetDateTime plusYears(long years) {
        return with(dateTime.plusYears(years), offset);
    }

    public TOffsetDateTime plusMonths(long months) {
        return with(dateTime.plusMonths(months), offset);
    }

    public TOffsetDateTime plusWeeks(long weeks) {
        return with(dateTime.plusWeeks(weeks), offset);
    }

    public TOffsetDateTime plusDays(long days) {
        return with(dateTime.plusDays(days), offset);
    }

    public TOffsetDateTime plusHours(long hours) {
        return with(dateTime.plusHours(hours), offset);
    }

    public TOffsetDateTime plusMinutes(long minutes) {
        return with(dateTime.plusMinutes(minutes), offset);
    }

    public TOffsetDateTime plusSeconds(long seconds) {
        return with(dateTime.plusSeconds(seconds), offset);
    }

    public TOffsetDateTime plusNanos(long nanos) {
        return with(dateTime.plusNanos(nanos), offset);
    }

    //-----------------------------------------------------------------------
    @Override
    public TOffsetDateTime minus(TTemporalAmount amount) {
        return (TOffsetDateTime) amount.subtractFrom(this);
    }

    @Override
    public TOffsetDateTime minus(long amountToSubtract, TTemporalUnit unit) {
        return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit) : plus(-amountToSubtract, unit));
    }

    //-----------------------------------------------------------------------
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

    //-----------------------------------------------------------------------
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
        return super.query(query);
    }

    @Override
    public TTemporal adjustInto(TTemporal temporal) {
        return temporal
                .with(EPOCH_DAY, toLocalDate().toEpochDay())
                .with(NANO_OF_DAY, toLocalTime().toNanoOfDay())
                .with(OFFSET_SECONDS, getOffset().getTotalSeconds());
    }

    @Override
    public long until(TTemporal endExclusive, TTemporalUnit unit) {
        TOffsetDateTime end = TOffsetDateTime.from(endExclusive);
        if (unit instanceof TChronoUnit) {
            end = end.withOffsetSameInstant(offset);
            return dateTime.until(end.dateTime, unit);
        }
        return unit.between(this, end);
    }

    //-----------------------------------------------------------------------
    public TZonedDateTime atZoneSameInstant(TZoneId zone) {
        return TZonedDateTime.ofInstant(dateTime, offset, zone);
    }

    public TZonedDateTime atZoneSimilarLocal(TZoneId zone) {
        return TZonedDateTime.ofLocal(dateTime, zone, offset);
    }

    //-----------------------------------------------------------------------
    public TLocalDateTime toLocalDateTime() {
        return dateTime;
    }

    public TLocalDate toLocalDate() {
        return dateTime.toLocalDate();
    }

    public TLocalTime toLocalTime() {
        return dateTime.toLocalTime();
    }

    //-----------------------------------------------------------------------
    public TOffsetTime toOffsetTime() {
        return TOffsetTime.of(dateTime.toLocalTime(), offset);
    }

    public TZonedDateTime toZonedDateTime() {
        return TZonedDateTime.of(dateTime, offset);
    }

    public TInstant toInstant() {
        return dateTime.toInstant(offset);
    }

    public long toEpochSecond() {
        return dateTime.toEpochSecond(offset);
    }

    //-----------------------------------------------------------------------
    @Override
    public int compareTo(TOffsetDateTime other) {
        if (getOffset().equals(other.getOffset())) {
            return toLocalDateTime().compareTo(other.toLocalDateTime());
        }
        int cmp = TJdk8Methods.compareLongs(toEpochSecond(), other.toEpochSecond());
        if (cmp == 0) {
            cmp = toLocalTime().getNano() - other.toLocalTime().getNano();
            if (cmp == 0) {
                cmp = toLocalDateTime().compareTo(other.toLocalDateTime());
            }
        }
        return cmp;
    }

    //-----------------------------------------------------------------------
    public boolean isAfter(TOffsetDateTime other) {
        long thisEpochSec = toEpochSecond();
        long otherEpochSec = other.toEpochSecond();
        return thisEpochSec > otherEpochSec ||
            (thisEpochSec == otherEpochSec && toLocalTime().getNano() > other.toLocalTime().getNano());
    }

    public boolean isBefore(TOffsetDateTime other) {
        long thisEpochSec = toEpochSecond();
        long otherEpochSec = other.toEpochSecond();
        return thisEpochSec < otherEpochSec ||
            (thisEpochSec == otherEpochSec && toLocalTime().getNano() < other.toLocalTime().getNano());
    }

    public boolean isEqual(TOffsetDateTime other) {
        return toEpochSecond() == other.toEpochSecond() &&
                toLocalTime().getNano() == other.toLocalTime().getNano();
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TOffsetDateTime) {
            TOffsetDateTime other = (TOffsetDateTime) obj;
            return dateTime.equals(other.dateTime) && offset.equals(other.offset);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return dateTime.hashCode() ^ offset.hashCode();
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
        return dateTime.toString() + offset.toString();
    }

    public String format(TDateTimeFormatter formatter) {
        TJdk8Methods.requireNonNull(formatter, "formatter");
        return formatter.format(this);
    }

    //-----------------------------------------------------------------------
    private Object writeReplace() {
        return new Ser(Ser.OFFSET_DATE_TIME_TYPE, this);
    }

    private Object readResolve() throws ObjectStreamException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    void writeExternal(DataOutput out) throws IOException {
        dateTime.writeExternal(out);
        offset.writeExternal(out);
    }

    static TOffsetDateTime readExternal(DataInput in) throws IOException {
        TLocalDateTime dateTime = TLocalDateTime.readExternal(in);
        TZoneOffset offset = TZoneOffset.readExternal(in);
        return TOffsetDateTime.of(dateTime, offset);
    }

}
