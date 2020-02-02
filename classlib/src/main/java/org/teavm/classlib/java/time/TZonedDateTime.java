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

import static org.teavm.classlib.java.time.temporal.TChronoField.INSTANT_SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.OFFSET_SECONDS;

import java.util.List;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.time.chrono.TChronoZonedDateTime;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;
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
import org.teavm.classlib.java.time.zone.TZoneOffsetTransition;
import org.teavm.classlib.java.time.zone.TZoneRules;

public final class TZonedDateTime extends TChronoZonedDateTime<TLocalDate> implements TTemporal, TSerializable {

    public static final TTemporalQuery<TZonedDateTime> FROM = new TTemporalQuery<TZonedDateTime>() {
        @Override
        public TZonedDateTime queryFrom(TTemporalAccessor temporal) {

            return TZonedDateTime.from(temporal);
        }
    };

    private final TLocalDateTime dateTime;

    private final TZoneOffset offset;

    private final TZoneId zone;

    public static TZonedDateTime now() {

        return now(TClock.systemDefaultZone());
    }

    public static TZonedDateTime now(TZoneId zone) {

        return now(TClock.system(zone));
    }

    public static TZonedDateTime now(TClock clock) {

        TJdk8Methods.requireNonNull(clock, "clock");
        final TInstant now = clock.instant(); // called once
        return ofInstant(now, clock.getZone());
    }

    public static TZonedDateTime of(TLocalDate date, TLocalTime time, TZoneId zone) {

        return of(TLocalDateTime.of(date, time), zone);
    }

    public static TZonedDateTime of(TLocalDateTime localDateTime, TZoneId zone) {

        return ofLocal(localDateTime, zone, null);
    }

    public static TZonedDateTime of(int year, int month, int dayOfMonth, int hour, int minute, int second,
            int nanoOfSecond, TZoneId zone) {

        TLocalDateTime dt = TLocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond);
        return ofLocal(dt, zone, null);
    }

    public static TZonedDateTime ofLocal(TLocalDateTime localDateTime, TZoneId zone, TZoneOffset preferredOffset) {

        TJdk8Methods.requireNonNull(localDateTime, "localDateTime");
        TJdk8Methods.requireNonNull(zone, "zone");
        if (zone instanceof TZoneOffset) {
            return new TZonedDateTime(localDateTime, (TZoneOffset) zone, zone);
        }
        TZoneRules rules = zone.getRules();
        List<TZoneOffset> validOffsets = rules.getValidOffsets(localDateTime);
        TZoneOffset offset;
        if (validOffsets.size() == 1) {
            offset = validOffsets.get(0);
        } else if (validOffsets.size() == 0) {
            TZoneOffsetTransition trans = rules.getTransition(localDateTime);
            localDateTime = localDateTime.plusSeconds(trans.getDuration().getSeconds());
            offset = trans.getOffsetAfter();
        } else {
            if (preferredOffset != null && validOffsets.contains(preferredOffset)) {
                offset = preferredOffset;
            } else {
                offset = TJdk8Methods.requireNonNull(validOffsets.get(0), "offset"); // protect against bad TZoneRules
            }
        }
        return new TZonedDateTime(localDateTime, offset, zone);
    }

    public static TZonedDateTime ofInstant(TInstant instant, TZoneId zone) {

        TJdk8Methods.requireNonNull(instant, "instant");
        TJdk8Methods.requireNonNull(zone, "zone");
        return create(instant.getEpochSecond(), instant.getNano(), zone);
    }

    public static TZonedDateTime ofInstant(TLocalDateTime localDateTime, TZoneOffset offset, TZoneId zone) {

        TJdk8Methods.requireNonNull(localDateTime, "localDateTime");
        TJdk8Methods.requireNonNull(offset, "offset");
        TJdk8Methods.requireNonNull(zone, "zone");
        return create(localDateTime.toEpochSecond(offset), localDateTime.getNano(), zone);
    }

    private static TZonedDateTime create(long epochSecond, int nanoOfSecond, TZoneId zone) {

        TZoneRules rules = zone.getRules();
        TInstant instant = TInstant.ofEpochSecond(epochSecond, nanoOfSecond); // TODO: rules should be queryable by
                                                                              // epochSeconds
        TZoneOffset offset = rules.getOffset(instant);
        TLocalDateTime ldt = TLocalDateTime.ofEpochSecond(epochSecond, nanoOfSecond, offset);
        return new TZonedDateTime(ldt, offset, zone);
    }

    public static TZonedDateTime ofStrict(TLocalDateTime localDateTime, TZoneOffset offset, TZoneId zone) {

        TJdk8Methods.requireNonNull(localDateTime, "localDateTime");
        TJdk8Methods.requireNonNull(offset, "offset");
        TJdk8Methods.requireNonNull(zone, "zone");
        TZoneRules rules = zone.getRules();
        if (rules.isValidOffset(localDateTime, offset) == false) {
            TZoneOffsetTransition trans = rules.getTransition(localDateTime);
            if (trans != null && trans.isGap()) {
                // error message says daylight savings for simplicity
                // even though there are other kinds of gaps
                throw new TDateTimeException("TLocalDateTime '" + localDateTime + "' does not exist in zone '" + zone
                        + "' due to a gap in the local time-line, typically caused by daylight savings");
            }
            throw new TDateTimeException("TZoneOffset '" + offset + "' is not valid for TLocalDateTime '"
                    + localDateTime + "' in zone '" + zone + "'");
        }
        return new TZonedDateTime(localDateTime, offset, zone);
    }

    private static TZonedDateTime ofLenient(TLocalDateTime localDateTime, TZoneOffset offset, TZoneId zone) {

        TJdk8Methods.requireNonNull(localDateTime, "localDateTime");
        TJdk8Methods.requireNonNull(offset, "offset");
        TJdk8Methods.requireNonNull(zone, "zone");
        if (zone instanceof TZoneOffset && offset.equals(zone) == false) {
            throw new IllegalArgumentException("TZoneId must match TZoneOffset");
        }
        return new TZonedDateTime(localDateTime, offset, zone);
    }

    public static TZonedDateTime from(TTemporalAccessor temporal) {

        if (temporal instanceof TZonedDateTime) {
            return (TZonedDateTime) temporal;
        }
        try {
            TZoneId zone = TZoneId.from(temporal);
            if (temporal.isSupported(INSTANT_SECONDS)) {
                try {
                    long epochSecond = temporal.getLong(INSTANT_SECONDS);
                    int nanoOfSecond = temporal.get(NANO_OF_SECOND);
                    return create(epochSecond, nanoOfSecond, zone);

                } catch (TDateTimeException ex) {
                    // ignore
                }
            }
            TLocalDateTime ldt = TLocalDateTime.from(temporal);
            return of(ldt, zone);
        } catch (TDateTimeException ex) {
            throw new TDateTimeException("Unable to obtain TZonedDateTime from TTemporalAccessor: " + temporal
                    + ", type " + temporal.getClass().getName());
        }
    }

    public static TZonedDateTime parse(CharSequence text) {

        return parse(text, TDateTimeFormatter.ISO_ZONED_DATE_TIME);
    }

    public static TZonedDateTime parse(CharSequence text, TDateTimeFormatter formatter) {

        TJdk8Methods.requireNonNull(formatter, "formatter");
        return formatter.parse(text, TZonedDateTime.FROM);
    }

    private TZonedDateTime(TLocalDateTime dateTime, TZoneOffset offset, TZoneId zone) {

        this.dateTime = dateTime;
        this.offset = offset;
        this.zone = zone;
    }

    private TZonedDateTime resolveLocal(TLocalDateTime newDateTime) {

        return ofLocal(newDateTime, this.zone, this.offset);
    }

    private TZonedDateTime resolveInstant(TLocalDateTime newDateTime) {

        return ofInstant(newDateTime, this.offset, this.zone);
    }

    private TZonedDateTime resolveOffset(TZoneOffset offset) {

        if (offset.equals(this.offset) == false && this.zone.getRules().isValidOffset(this.dateTime, offset)) {
            return new TZonedDateTime(this.dateTime, offset, this.zone);
        }
        return this;
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
        return super.get(field);
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

    @Override
    public TZoneOffset getOffset() {

        return this.offset;
    }

    @Override
    public TZonedDateTime withEarlierOffsetAtOverlap() {

        TZoneOffsetTransition trans = getZone().getRules().getTransition(this.dateTime);
        if (trans != null && trans.isOverlap()) {
            TZoneOffset earlierOffset = trans.getOffsetBefore();
            if (earlierOffset.equals(this.offset) == false) {
                return new TZonedDateTime(this.dateTime, earlierOffset, this.zone);
            }
        }
        return this;
    }

    @Override
    public TZonedDateTime withLaterOffsetAtOverlap() {

        TZoneOffsetTransition trans = getZone().getRules().getTransition(toLocalDateTime());
        if (trans != null) {
            TZoneOffset laterOffset = trans.getOffsetAfter();
            if (laterOffset.equals(this.offset) == false) {
                return new TZonedDateTime(this.dateTime, laterOffset, this.zone);
            }
        }
        return this;
    }

    @Override
    public TZoneId getZone() {

        return this.zone;
    }

    @Override
    public TZonedDateTime withZoneSameLocal(TZoneId zone) {

        TJdk8Methods.requireNonNull(zone, "zone");
        return this.zone.equals(zone) ? this : ofLocal(this.dateTime, zone, this.offset);
    }

    @Override
    public TZonedDateTime withZoneSameInstant(TZoneId zone) {

        TJdk8Methods.requireNonNull(zone, "zone");
        return this.zone.equals(zone) ? this
                : create(this.dateTime.toEpochSecond(this.offset), this.dateTime.getNano(), zone);
    }

    public TZonedDateTime withFixedOffsetZone() {

        return this.zone.equals(this.offset) ? this : new TZonedDateTime(this.dateTime, this.offset, this.offset);
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
    public TZonedDateTime with(TTemporalAdjuster adjuster) {

        // optimizations
        if (adjuster instanceof TLocalDate) {
            return resolveLocal(TLocalDateTime.of((TLocalDate) adjuster, this.dateTime.toLocalTime()));
        } else if (adjuster instanceof TLocalTime) {
            return resolveLocal(TLocalDateTime.of(this.dateTime.toLocalDate(), (TLocalTime) adjuster));
        } else if (adjuster instanceof TLocalDateTime) {
            return resolveLocal((TLocalDateTime) adjuster);
        } else if (adjuster instanceof TInstant) {
            TInstant instant = (TInstant) adjuster;
            return create(instant.getEpochSecond(), instant.getNano(), this.zone);
        } else if (adjuster instanceof TZoneOffset) {
            return resolveOffset((TZoneOffset) adjuster);
        }
        return (TZonedDateTime) adjuster.adjustInto(this);
    }

    @Override
    public TZonedDateTime with(TTemporalField field, long newValue) {

        if (field instanceof TChronoField) {
            TChronoField f = (TChronoField) field;
            switch (f) {
                case INSTANT_SECONDS:
                    return create(newValue, getNano(), this.zone);
                case OFFSET_SECONDS: {
                    TZoneOffset offset = TZoneOffset.ofTotalSeconds(f.checkValidIntValue(newValue));
                    return resolveOffset(offset);
                }
            }
            return resolveLocal(this.dateTime.with(field, newValue));
        }
        return field.adjustInto(this, newValue);
    }

    public TZonedDateTime withYear(int year) {

        return resolveLocal(this.dateTime.withYear(year));
    }

    public TZonedDateTime withMonth(int month) {

        return resolveLocal(this.dateTime.withMonth(month));
    }

    public TZonedDateTime withDayOfMonth(int dayOfMonth) {

        return resolveLocal(this.dateTime.withDayOfMonth(dayOfMonth));
    }

    public TZonedDateTime withDayOfYear(int dayOfYear) {

        return resolveLocal(this.dateTime.withDayOfYear(dayOfYear));
    }

    public TZonedDateTime withHour(int hour) {

        return resolveLocal(this.dateTime.withHour(hour));
    }

    public TZonedDateTime withMinute(int minute) {

        return resolveLocal(this.dateTime.withMinute(minute));
    }

    public TZonedDateTime withSecond(int second) {

        return resolveLocal(this.dateTime.withSecond(second));
    }

    public TZonedDateTime withNano(int nanoOfSecond) {

        return resolveLocal(this.dateTime.withNano(nanoOfSecond));
    }

    public TZonedDateTime truncatedTo(TTemporalUnit unit) {

        return resolveLocal(this.dateTime.truncatedTo(unit));
    }

    @Override
    public TZonedDateTime plus(TTemporalAmount amount) {

        return (TZonedDateTime) amount.addTo(this);
    }

    @Override
    public TZonedDateTime plus(long amountToAdd, TTemporalUnit unit) {

        if (unit instanceof TChronoUnit) {
            if (unit.isDateBased()) {
                return resolveLocal(this.dateTime.plus(amountToAdd, unit));
            } else {
                return resolveInstant(this.dateTime.plus(amountToAdd, unit));
            }
        }
        return unit.addTo(this, amountToAdd);
    }

    public TZonedDateTime plusYears(long years) {

        return resolveLocal(this.dateTime.plusYears(years));
    }

    public TZonedDateTime plusMonths(long months) {

        return resolveLocal(this.dateTime.plusMonths(months));
    }

    public TZonedDateTime plusWeeks(long weeks) {

        return resolveLocal(this.dateTime.plusWeeks(weeks));
    }

    public TZonedDateTime plusDays(long days) {

        return resolveLocal(this.dateTime.plusDays(days));
    }

    public TZonedDateTime plusHours(long hours) {

        return resolveInstant(this.dateTime.plusHours(hours));
    }

    public TZonedDateTime plusMinutes(long minutes) {

        return resolveInstant(this.dateTime.plusMinutes(minutes));
    }

    public TZonedDateTime plusSeconds(long seconds) {

        return resolveInstant(this.dateTime.plusSeconds(seconds));
    }

    public TZonedDateTime plusNanos(long nanos) {

        return resolveInstant(this.dateTime.plusNanos(nanos));
    }

    @Override
    public TZonedDateTime minus(TTemporalAmount amount) {

        return (TZonedDateTime) amount.subtractFrom(this);
    }

    @Override
    public TZonedDateTime minus(long amountToSubtract, TTemporalUnit unit) {

        return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit)
                : plus(-amountToSubtract, unit));
    }

    public TZonedDateTime minusYears(long years) {

        return (years == Long.MIN_VALUE ? plusYears(Long.MAX_VALUE).plusYears(1) : plusYears(-years));
    }

    public TZonedDateTime minusMonths(long months) {

        return (months == Long.MIN_VALUE ? plusMonths(Long.MAX_VALUE).plusMonths(1) : plusMonths(-months));
    }

    public TZonedDateTime minusWeeks(long weeks) {

        return (weeks == Long.MIN_VALUE ? plusWeeks(Long.MAX_VALUE).plusWeeks(1) : plusWeeks(-weeks));
    }

    public TZonedDateTime minusDays(long days) {

        return (days == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-days));
    }

    public TZonedDateTime minusHours(long hours) {

        return (hours == Long.MIN_VALUE ? plusHours(Long.MAX_VALUE).plusHours(1) : plusHours(-hours));
    }

    public TZonedDateTime minusMinutes(long minutes) {

        return (minutes == Long.MIN_VALUE ? plusMinutes(Long.MAX_VALUE).plusMinutes(1) : plusMinutes(-minutes));
    }

    public TZonedDateTime minusSeconds(long seconds) {

        return (seconds == Long.MIN_VALUE ? plusSeconds(Long.MAX_VALUE).plusSeconds(1) : plusSeconds(-seconds));
    }

    public TZonedDateTime minusNanos(long nanos) {

        return (nanos == Long.MIN_VALUE ? plusNanos(Long.MAX_VALUE).plusNanos(1) : plusNanos(-nanos));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TTemporalQuery<R> query) {

        if (query == TTemporalQueries.localDate()) {
            return (R) toLocalDate();
        }
        return super.query(query);
    }

    @Override
    public long until(TTemporal endExclusive, TTemporalUnit unit) {

        TZonedDateTime end = TZonedDateTime.from(endExclusive);
        if (unit instanceof TChronoUnit) {
            end = end.withZoneSameInstant(this.zone);
            if (unit.isDateBased()) {
                return this.dateTime.until(end.dateTime, unit);
            } else {
                return toOffsetDateTime().until(end.toOffsetDateTime(), unit);
            }
        }
        return unit.between(this, end);
    }

    @Override
    public TLocalDateTime toLocalDateTime() {

        return this.dateTime;
    }

    @Override
    public TLocalDate toLocalDate() {

        return this.dateTime.toLocalDate();
    }

    @Override
    public TLocalTime toLocalTime() {

        return this.dateTime.toLocalTime();
    }

    public TOffsetDateTime toOffsetDateTime() {

        return TOffsetDateTime.of(this.dateTime, this.offset);
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj instanceof TZonedDateTime) {
            TZonedDateTime other = (TZonedDateTime) obj;
            return this.dateTime.equals(other.dateTime) && this.offset.equals(other.offset)
                    && this.zone.equals(other.zone);
        }
        return false;
    }

    @Override
    public int hashCode() {

        return this.dateTime.hashCode() ^ this.offset.hashCode() ^ Integer.rotateLeft(this.zone.hashCode(), 3);
    }

    @Override
    public String toString() {

        String str = this.dateTime.toString() + this.offset.toString();
        if (this.offset != this.zone) {
            str += '[' + this.zone.toString() + ']';
        }
        return str;
    }

    @Override
    public String format(TDateTimeFormatter formatter) {

        return super.format(formatter);
    }

}
