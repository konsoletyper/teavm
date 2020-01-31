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

import static org.teavm.classlib.java.time.TLocalTime.HOURS_PER_DAY;
import static org.teavm.classlib.java.time.TLocalTime.MICROS_PER_DAY;
import static org.teavm.classlib.java.time.TLocalTime.MILLIS_PER_DAY;
import static org.teavm.classlib.java.time.TLocalTime.MINUTES_PER_DAY;
import static org.teavm.classlib.java.time.TLocalTime.NANOS_PER_DAY;
import static org.teavm.classlib.java.time.TLocalTime.NANOS_PER_HOUR;
import static org.teavm.classlib.java.time.TLocalTime.NANOS_PER_MINUTE;
import static org.teavm.classlib.java.time.TLocalTime.NANOS_PER_SECOND;
import static org.teavm.classlib.java.time.TLocalTime.SECONDS_PER_DAY;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import org.teavm.classlib.java.time.chrono.TChronoLocalDateTime;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.format.TDateTimeParseException;
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
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.temporal.TValueRange;
import org.teavm.classlib.java.time.zone.TZoneRules;

public final class TLocalDateTime
        extends TChronoLocalDateTime<TLocalDate>
        implements TTemporal, TTemporalAdjuster, Serializable {

    public static final TLocalDateTime MIN = TLocalDateTime.of(TLocalDate.MIN, TLocalTime.MIN);
    public static final TLocalDateTime MAX = TLocalDateTime.of(TLocalDate.MAX, TLocalTime.MAX);
    public static final TTemporalQuery<TLocalDateTime> FROM = new TTemporalQuery<TLocalDateTime>() {
        @Override
        public TLocalDateTime queryFrom(TTemporalAccessor temporal) {
            return TLocalDateTime.from(temporal);
        }
    };

    private static final long serialVersionUID = 6207766400415563566L;

    private final TLocalDate date;
    private final TLocalTime time;

    //-----------------------------------------------------------------------
    public static TLocalDateTime now() {
        return now(TClock.systemDefaultZone());
    }

    public static TLocalDateTime now(TZoneId zone) {
        return now(TClock.system(zone));
    }

    public static TLocalDateTime now(TClock clock) {
        TJdk8Methods.requireNonNull(clock, "clock");
        final TInstant now = clock.instant();  // called once
        TZoneOffset offset = clock.getZone().getRules().getOffset(now);
        return ofEpochSecond(now.getEpochSecond(), now.getNano(), offset);
    }

    //-----------------------------------------------------------------------
    public static TLocalDateTime of(int year, TMonth month, int dayOfMonth, int hour, int minute) {
        TLocalDate date = TLocalDate.of(year, month, dayOfMonth);
        TLocalTime time = TLocalTime.of(hour, minute);
        return new TLocalDateTime(date, time);
    }

    public static TLocalDateTime of(int year, TMonth month, int dayOfMonth, int hour, int minute, int second) {
        TLocalDate date = TLocalDate.of(year, month, dayOfMonth);
        TLocalTime time = TLocalTime.of(hour, minute, second);
        return new TLocalDateTime(date, time);
    }

    public static TLocalDateTime of(int year, TMonth month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond) {
        TLocalDate date = TLocalDate.of(year, month, dayOfMonth);
        TLocalTime time = TLocalTime.of(hour, minute, second, nanoOfSecond);
        return new TLocalDateTime(date, time);
    }

    //-----------------------------------------------------------------------
    public static TLocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute) {
        TLocalDate date = TLocalDate.of(year, month, dayOfMonth);
        TLocalTime time = TLocalTime.of(hour, minute);
        return new TLocalDateTime(date, time);
    }

    public static TLocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute, int second) {
        TLocalDate date = TLocalDate.of(year, month, dayOfMonth);
        TLocalTime time = TLocalTime.of(hour, minute, second);
        return new TLocalDateTime(date, time);
    }

    public static TLocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond) {
        TLocalDate date = TLocalDate.of(year, month, dayOfMonth);
        TLocalTime time = TLocalTime.of(hour, minute, second, nanoOfSecond);
        return new TLocalDateTime(date, time);
    }

    public static TLocalDateTime of(TLocalDate date, TLocalTime time) {
        TJdk8Methods.requireNonNull(date, "date");
        TJdk8Methods.requireNonNull(time, "time");
        return new TLocalDateTime(date, time);
    }

    //-------------------------------------------------------------------------
    public static TLocalDateTime ofInstant(TInstant instant, TZoneId zone) {
        TJdk8Methods.requireNonNull(instant, "instant");
        TJdk8Methods.requireNonNull(zone, "zone");
        TZoneRules rules = zone.getRules();
        TZoneOffset offset = rules.getOffset(instant);
        return ofEpochSecond(instant.getEpochSecond(), instant.getNano(), offset);
    }

    public static TLocalDateTime ofEpochSecond(long epochSecond, int nanoOfSecond, TZoneOffset offset) {
        TJdk8Methods.requireNonNull(offset, "offset");
        long localSecond = epochSecond + offset.getTotalSeconds();  // overflow caught later
        long localEpochDay = TJdk8Methods.floorDiv(localSecond, SECONDS_PER_DAY);
        int secsOfDay = TJdk8Methods.floorMod(localSecond, SECONDS_PER_DAY);
        TLocalDate date = TLocalDate.ofEpochDay(localEpochDay);
        TLocalTime time = TLocalTime.ofSecondOfDay(secsOfDay, nanoOfSecond);
        return new TLocalDateTime(date, time);
    }

    //-----------------------------------------------------------------------
    public static TLocalDateTime from(TTemporalAccessor temporal) {
        if (temporal instanceof TLocalDateTime) {
            return (TLocalDateTime) temporal;
        } else if (temporal instanceof TZonedDateTime) {
            return ((TZonedDateTime) temporal).toLocalDateTime();
        }
        try {
            TLocalDate date = TLocalDate.from(temporal);
            TLocalTime time = TLocalTime.from(temporal);
            return new TLocalDateTime(date, time);
        } catch (TDateTimeException ex) {
            throw new TDateTimeException("Unable to obtain TLocalDateTime from TTemporalAccessor: " +
                    temporal + ", type " + temporal.getClass().getName());
        }
    }

    //-----------------------------------------------------------------------
    public static TLocalDateTime parse(CharSequence text) {
        return parse(text, TDateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public static TLocalDateTime parse(CharSequence text, TDateTimeFormatter formatter) {
        TJdk8Methods.requireNonNull(formatter, "formatter");
        return formatter.parse(text, TLocalDateTime.FROM);
    }

    //-----------------------------------------------------------------------
    private TLocalDateTime(TLocalDate date, TLocalTime time) {
        this.date = date;
        this.time = time;
    }

    private TLocalDateTime with(TLocalDate newDate, TLocalTime newTime) {
        if (date == newDate && time == newTime) {
            return this;
        }
        return new TLocalDateTime(newDate, newTime);
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean isSupported(TTemporalField field) {
        if (field instanceof TChronoField) {
            return field.isDateBased() || field.isTimeBased();
        }
        return field != null && field.isSupportedBy(this);
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
            return (field.isTimeBased() ? time.range(field) : date.range(field));
        }
        return field.rangeRefinedBy(this);
    }

    @Override
    public int get(TTemporalField field) {
        if (field instanceof TChronoField) {
            return (field.isTimeBased() ? time.get(field) : date.get(field));
        }
        return super.get(field);
    }

    @Override
    public long getLong(TTemporalField field) {
        if (field instanceof TChronoField) {
            return (field.isTimeBased() ? time.getLong(field) : date.getLong(field));
        }
        return field.getFrom(this);
    }

    //-----------------------------------------------------------------------
    public int getYear() {
        return date.getYear();
    }

    public int getMonthValue() {
        return date.getMonthValue();
    }

    public TMonth getMonth() {
        return date.getMonth();
    }

    public int getDayOfMonth() {
        return date.getDayOfMonth();
    }

    public int getDayOfYear() {
        return date.getDayOfYear();
    }

    public TDayOfWeek getDayOfWeek() {
        return date.getDayOfWeek();
    }

    //-----------------------------------------------------------------------
    public int getHour() {
        return time.getHour();
    }

    public int getMinute() {
        return time.getMinute();
    }

    public int getSecond() {
        return time.getSecond();
    }

    public int getNano() {
        return time.getNano();
    }

    //-----------------------------------------------------------------------
    @Override
    public TLocalDateTime with(TTemporalAdjuster adjuster) {
        // optimizations
        if (adjuster instanceof TLocalDate) {
            return with((TLocalDate) adjuster, time);
        } else if (adjuster instanceof TLocalTime) {
            return with(date, (TLocalTime) adjuster);
        } else if (adjuster instanceof TLocalDateTime) {
            return (TLocalDateTime) adjuster;
        }
        return (TLocalDateTime) adjuster.adjustInto(this);
    }

    @Override
    public TLocalDateTime with(TTemporalField field, long newValue) {
        if (field instanceof TChronoField) {
            if (field.isTimeBased()) {
                return with(date, time.with(field, newValue));
            } else {
                return with(date.with(field, newValue), time);
            }
        }
        return field.adjustInto(this, newValue);
    }

    //-----------------------------------------------------------------------
    public TLocalDateTime withYear(int year) {
        return with(date.withYear(year), time);
    }

    public TLocalDateTime withMonth(int month) {
        return with(date.withMonth(month), time);
    }

    public TLocalDateTime withDayOfMonth(int dayOfMonth) {
        return with(date.withDayOfMonth(dayOfMonth), time);
    }

    public TLocalDateTime withDayOfYear(int dayOfYear) {
        return with(date.withDayOfYear(dayOfYear), time);
    }

    //-----------------------------------------------------------------------
    public TLocalDateTime withHour(int hour) {
        TLocalTime newTime = time.withHour(hour);
        return with(date, newTime);
    }

    public TLocalDateTime withMinute(int minute) {
        TLocalTime newTime = time.withMinute(minute);
        return with(date, newTime);
    }

    public TLocalDateTime withSecond(int second) {
        TLocalTime newTime = time.withSecond(second);
        return with(date, newTime);
    }

    public TLocalDateTime withNano(int nanoOfSecond) {
        TLocalTime newTime = time.withNano(nanoOfSecond);
        return with(date, newTime);
    }

    //-----------------------------------------------------------------------
    public TLocalDateTime truncatedTo(TTemporalUnit unit) {
        return with(date, time.truncatedTo(unit));
    }

    //-----------------------------------------------------------------------
    @Override
    public TLocalDateTime plus(TTemporalAmount amount) {
        return (TLocalDateTime) amount.addTo(this);
    }

    @Override
    public TLocalDateTime plus(long amountToAdd, TTemporalUnit unit) {
        if (unit instanceof TChronoUnit) {
            TChronoUnit f = (TChronoUnit) unit;
            switch (f) {
                case NANOS: return plusNanos(amountToAdd);
                case MICROS: return plusDays(amountToAdd / MICROS_PER_DAY).plusNanos((amountToAdd % MICROS_PER_DAY) * 1000);
                case MILLIS: return plusDays(amountToAdd / MILLIS_PER_DAY).plusNanos((amountToAdd % MILLIS_PER_DAY) * 1000000);
                case SECONDS: return plusSeconds(amountToAdd);
                case MINUTES: return plusMinutes(amountToAdd);
                case HOURS: return plusHours(amountToAdd);
                case HALF_DAYS: return plusDays(amountToAdd / 256).plusHours((amountToAdd % 256) * 12);  // no overflow (256 is multiple of 2)
            }
            return with(date.plus(amountToAdd, unit), time);
        }
        return unit.addTo(this, amountToAdd);
    }

    //-----------------------------------------------------------------------
    public TLocalDateTime plusYears(long years) {
        TLocalDate newDate = date.plusYears(years);
        return with(newDate, time);
    }

    public TLocalDateTime plusMonths(long months) {
        TLocalDate newDate = date.plusMonths(months);
        return with(newDate, time);
    }

    public TLocalDateTime plusWeeks(long weeks) {
        TLocalDate newDate = date.plusWeeks(weeks);
        return with(newDate, time);
    }

    public TLocalDateTime plusDays(long days) {
        TLocalDate newDate = date.plusDays(days);
        return with(newDate, time);
    }

    //-----------------------------------------------------------------------
    public TLocalDateTime plusHours(long hours) {
        return plusWithOverflow(date, hours, 0, 0, 0, 1);
    }

    public TLocalDateTime plusMinutes(long minutes) {
        return plusWithOverflow(date, 0, minutes, 0, 0, 1);
    }

    public TLocalDateTime plusSeconds(long seconds) {
        return plusWithOverflow(date, 0, 0, seconds, 0, 1);
    }

    public TLocalDateTime plusNanos(long nanos) {
        return plusWithOverflow(date, 0, 0, 0, nanos, 1);
    }

    //-----------------------------------------------------------------------
    @Override
    public TLocalDateTime minus(TTemporalAmount amount) {
        return (TLocalDateTime) amount.subtractFrom(this);
    }

    @Override
    public TLocalDateTime minus(long amountToSubtract, TTemporalUnit unit) {
        return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit) : plus(-amountToSubtract, unit));
    }

    //-----------------------------------------------------------------------
    public TLocalDateTime minusYears(long years) {
        return (years == Long.MIN_VALUE ? plusYears(Long.MAX_VALUE).plusYears(1) : plusYears(-years));
    }

    public TLocalDateTime minusMonths(long months) {
        return (months == Long.MIN_VALUE ? plusMonths(Long.MAX_VALUE).plusMonths(1) : plusMonths(-months));
    }

    public TLocalDateTime minusWeeks(long weeks) {
        return (weeks == Long.MIN_VALUE ? plusWeeks(Long.MAX_VALUE).plusWeeks(1) : plusWeeks(-weeks));
    }

    public TLocalDateTime minusDays(long days) {
        return (days == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-days));
    }

    //-----------------------------------------------------------------------
    public TLocalDateTime minusHours(long hours) {
        return plusWithOverflow(date, hours, 0, 0, 0, -1);
   }

    public TLocalDateTime minusMinutes(long minutes) {
        return plusWithOverflow(date, 0, minutes, 0, 0, -1);
    }

    public TLocalDateTime minusSeconds(long seconds) {
        return plusWithOverflow(date, 0, 0, seconds, 0, -1);
    }

    public TLocalDateTime minusNanos(long nanos) {
        return plusWithOverflow(date, 0, 0, 0, nanos, -1);
    }

    //-----------------------------------------------------------------------
    private TLocalDateTime plusWithOverflow(TLocalDate newDate, long hours, long minutes, long seconds, long nanos, int sign) {
        // 9223372036854775808 long, 2147483648 int
        if ((hours | minutes | seconds | nanos) == 0) {
            return with(newDate, time);
        }
        long totDays = nanos / NANOS_PER_DAY +             //   max/24*60*60*1B
                seconds / SECONDS_PER_DAY +                //   max/24*60*60
                minutes / MINUTES_PER_DAY +                //   max/24*60
                hours / HOURS_PER_DAY;                     //   max/24
        totDays *= sign;                                   // total max*0.4237...
        long totNanos = nanos % NANOS_PER_DAY +                    //   max  86400000000000
                (seconds % SECONDS_PER_DAY) * NANOS_PER_SECOND +   //   max  86400000000000
                (minutes % MINUTES_PER_DAY) * NANOS_PER_MINUTE +   //   max  86400000000000
                (hours % HOURS_PER_DAY) * NANOS_PER_HOUR;          //   max  86400000000000
        long curNoD = time.toNanoOfDay();                       //   max  86400000000000
        totNanos = totNanos * sign + curNoD;                    // total 432000000000000
        totDays += TJdk8Methods.floorDiv(totNanos, NANOS_PER_DAY);
        long newNoD = TJdk8Methods.floorMod(totNanos, NANOS_PER_DAY);
        TLocalTime newTime = (newNoD == curNoD ? time : TLocalTime.ofNanoOfDay(newNoD));
        return with(newDate.plusDays(totDays), newTime);
    }

    //-----------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    @Override  // override for Javadoc
    public <R> R query(TTemporalQuery<R> query) {
        if (query == TTemporalQueries.localDate()) {
            return (R) toLocalDate();
        }
        return super.query(query);
    }

    @Override  // override for Javadoc
    public TTemporal adjustInto(TTemporal temporal) {
        return super.adjustInto(temporal);
    }

    @Override
    public long until(TTemporal endExclusive, TTemporalUnit unit) {
        TLocalDateTime end = TLocalDateTime.from(endExclusive);
        if (unit instanceof TChronoUnit) {
            TChronoUnit f = (TChronoUnit) unit;
            if (f.isTimeBased()) {
                long daysUntil = date.daysUntil(end.date);
                long timeUntil = end.time.toNanoOfDay() - time.toNanoOfDay();
                if (daysUntil > 0 && timeUntil < 0) {
                    daysUntil--;
                    timeUntil += NANOS_PER_DAY;
                } else if (daysUntil < 0 && timeUntil > 0) {
                    daysUntil++;
                    timeUntil -= NANOS_PER_DAY;
                }
                long amount = daysUntil;
                switch (f) {
                    case NANOS:
                        amount = TJdk8Methods.safeMultiply(amount, NANOS_PER_DAY);
                        return TJdk8Methods.safeAdd(amount, timeUntil);
                    case MICROS:
                        amount = TJdk8Methods.safeMultiply(amount, MICROS_PER_DAY);
                        return TJdk8Methods.safeAdd(amount, timeUntil / 1000);
                    case MILLIS:
                        amount = TJdk8Methods.safeMultiply(amount, MILLIS_PER_DAY);
                        return TJdk8Methods.safeAdd(amount, timeUntil / 1000000);
                    case SECONDS:
                        amount = TJdk8Methods.safeMultiply(amount, SECONDS_PER_DAY);
                        return TJdk8Methods.safeAdd(amount, timeUntil / NANOS_PER_SECOND);
                    case MINUTES:
                        amount = TJdk8Methods.safeMultiply(amount, MINUTES_PER_DAY);
                        return TJdk8Methods.safeAdd(amount, timeUntil / NANOS_PER_MINUTE);
                    case HOURS:
                        amount = TJdk8Methods.safeMultiply(amount, HOURS_PER_DAY);
                        return TJdk8Methods.safeAdd(amount, timeUntil / NANOS_PER_HOUR);
                    case HALF_DAYS:
                        amount = TJdk8Methods.safeMultiply(amount, 2);
                        return TJdk8Methods.safeAdd(amount, timeUntil / (NANOS_PER_HOUR * 12));
                }
                throw new TUnsupportedTemporalTypeException("Unsupported unit: " + unit);
            }
            TLocalDate endDate = end.date;
            if (endDate.isAfter(date) && end.time.isBefore(time)) {
                endDate = endDate.minusDays(1);
            } else if (endDate.isBefore(date) && end.time.isAfter(time)) {
                endDate = endDate.plusDays(1);
            }
            return date.until(endDate, unit);
        }
        return unit.between(this, end);
    }

    //-----------------------------------------------------------------------
    public TOffsetDateTime atOffset(TZoneOffset offset) {
        return TOffsetDateTime.of(this, offset);
    }

    @Override
    public TZonedDateTime atZone(TZoneId zone) {
        return TZonedDateTime.of(this, zone);
    }

    //-----------------------------------------------------------------------
    @Override
    public TLocalDate toLocalDate() {
        return date;
    }

    @Override
    public TLocalTime toLocalTime() {
        return time;
    }

    //-----------------------------------------------------------------------
    @Override  // override for Javadoc and performance
    public int compareTo(TChronoLocalDateTime<?> other) {
        if (other instanceof TLocalDateTime) {
            return compareTo0((TLocalDateTime) other);
        }
        return super.compareTo(other);
    }

    private int compareTo0(TLocalDateTime other) {
        int cmp = date.compareTo0(other.toLocalDate());
        if (cmp == 0) {
            cmp = time.compareTo(other.toLocalTime());
        }
        return cmp;
    }

    @Override  // override for Javadoc and performance
    public boolean isAfter(TChronoLocalDateTime<?> other) {
        if (other instanceof TLocalDateTime) {
            return compareTo0((TLocalDateTime) other) > 0;
        }
        return super.isAfter(other);
    }

    @Override  // override for Javadoc and performance
    public boolean isBefore(TChronoLocalDateTime<?> other) {
        if (other instanceof TLocalDateTime) {
            return compareTo0((TLocalDateTime) other) < 0;
        }
        return super.isBefore(other);
    }

    @Override  // override for Javadoc and performance
    public boolean isEqual(TChronoLocalDateTime<?> other) {
        if (other instanceof TLocalDateTime) {
            return compareTo0((TLocalDateTime) other) == 0;
        }
        return super.isEqual(other);
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TLocalDateTime) {
            TLocalDateTime other = (TLocalDateTime) obj;
            return date.equals(other.date) && time.equals(other.time);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return date.hashCode() ^ time.hashCode();
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
        return date.toString() + 'T' + time.toString();
    }

    @Override  // override for Javadoc
    public String format(TDateTimeFormatter formatter) {
        return super.format(formatter);
    }

    //-----------------------------------------------------------------------
    private Object writeReplace() {
        return new Ser(Ser.LOCAL_DATE_TIME_TYPE, this);
    }

    private Object readResolve() throws ObjectStreamException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    void writeExternal(DataOutput out) throws IOException {
        date.writeExternal(out);
        time.writeExternal(out);
    }

    static TLocalDateTime readExternal(DataInput in) throws IOException {
        TLocalDate date = TLocalDate.readExternal(in);
        TLocalTime time = TLocalTime.readExternal(in);
        return TLocalDateTime.of(date, time);
    }

}
