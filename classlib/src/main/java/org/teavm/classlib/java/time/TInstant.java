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

import static org.teavm.classlib.java.time.TLocalTime.SECONDS_PER_DAY;
import static org.teavm.classlib.java.time.TLocalTime.SECONDS_PER_HOUR;
import static org.teavm.classlib.java.time.TLocalTime.SECONDS_PER_MINUTE;
import static org.teavm.classlib.java.time.temporal.TChronoField.INSTANT_SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoField.MICRO_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.MILLI_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.NANOS;

import java.io.Serializable;
import java.util.Objects;

import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.jdk8.TDefaultInterfaceTemporalAccessor;
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
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.temporal.TValueRange;

public final class TInstant extends TDefaultInterfaceTemporalAccessor
        implements TTemporal, TTemporalAdjuster, Comparable<TInstant>, Serializable {

    public static final TInstant EPOCH = new TInstant(0, 0);

    private static final long MIN_SECOND = -31557014167219200L;

    private static final long MAX_SECOND = 31556889864403199L;

    public static final TInstant MIN = TInstant.ofEpochSecond(MIN_SECOND, 0);

    public static final TInstant MAX = TInstant.ofEpochSecond(MAX_SECOND, 999999999);

    public static final TTemporalQuery<TInstant> FROM = new TTemporalQuery<TInstant>() {
        @Override
        public TInstant queryFrom(TTemporalAccessor temporal) {

            return TInstant.from(temporal);
        }
    };

    private static final int NANOS_PER_SECOND = 1000000000;

    private static final int NANOS_PER_MILLI = 1000000;

    private static final long MILLIS_PER_SEC = 1000;

    private final long seconds;

    private final int nanos;

    public static TInstant now() {

        return TClock.systemUTC().instant();
    }

    public static TInstant now(TClock clock) {

        Objects.requireNonNull(clock, "clock");
        return clock.instant();
    }

    public static TInstant ofEpochSecond(long epochSecond) {

        return create(epochSecond, 0);
    }

    public static TInstant ofEpochSecond(long epochSecond, long nanoAdjustment) {

        long secs = Math.addExact(epochSecond, Math.floorDiv(nanoAdjustment, NANOS_PER_SECOND));
        int nos = (int) Math.floorMod(nanoAdjustment, NANOS_PER_SECOND);
        return create(secs, nos);
    }

    public static TInstant ofEpochMilli(long epochMilli) {

        long secs = Math.floorDiv(epochMilli, 1000);
        int mos = (int) Math.floorMod(epochMilli, 1000);
        return create(secs, mos * NANOS_PER_MILLI);
    }

    public static TInstant from(TTemporalAccessor temporal) {

        try {
            long instantSecs = temporal.getLong(INSTANT_SECONDS);
            int nanoOfSecond = temporal.get(NANO_OF_SECOND);
            return TInstant.ofEpochSecond(instantSecs, nanoOfSecond);
        } catch (TDateTimeException ex) {
            throw new TDateTimeException("Unable to obtain TInstant from TTemporalAccessor: " + temporal + ", type "
                    + temporal.getClass().getName(), ex);
        }
    }

    public static TInstant parse(final CharSequence text) {

        return TDateTimeFormatter.ISO_INSTANT.parse(text, TInstant.FROM);
    }

    private static TInstant create(long seconds, int nanoOfSecond) {

        if ((seconds | nanoOfSecond) == 0) {
            return EPOCH;
        }
        if (seconds < MIN_SECOND || seconds > MAX_SECOND) {
            throw new TDateTimeException("TInstant exceeds minimum or maximum instant");
        }
        return new TInstant(seconds, nanoOfSecond);
    }

    private TInstant(long epochSecond, int nanos) {

        super();
        this.seconds = epochSecond;
        this.nanos = nanos;
    }

    @Override
    public boolean isSupported(TTemporalField field) {

        if (field instanceof TChronoField) {
            return field == INSTANT_SECONDS || field == NANO_OF_SECOND || field == MICRO_OF_SECOND
                    || field == MILLI_OF_SECOND;
        }
        return field != null && field.isSupportedBy(this);
    }

    @Override
    public boolean isSupported(TTemporalUnit unit) {

        if (unit instanceof TChronoUnit) {
            return unit.isTimeBased() || unit == DAYS;
        }
        return unit != null && unit.isSupportedBy(this);
    }

    @Override // override for Javadoc
    public TValueRange range(TTemporalField field) {

        return super.range(field);
    }

    @Override // override for Javadoc and performance
    public int get(TTemporalField field) {

        if (field instanceof TChronoField) {
            switch ((TChronoField) field) {
                case NANO_OF_SECOND:
                    return this.nanos;
                case MICRO_OF_SECOND:
                    return this.nanos / 1000;
                case MILLI_OF_SECOND:
                    return this.nanos / NANOS_PER_MILLI;
            }
            throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return range(field).checkValidIntValue(field.getFrom(this), field);
    }

    @Override
    public long getLong(TTemporalField field) {

        if (field instanceof TChronoField) {
            switch ((TChronoField) field) {
                case NANO_OF_SECOND:
                    return this.nanos;
                case MICRO_OF_SECOND:
                    return this.nanos / 1000;
                case MILLI_OF_SECOND:
                    return this.nanos / NANOS_PER_MILLI;
                case INSTANT_SECONDS:
                    return this.seconds;
            }
            throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.getFrom(this);
    }

    public long getEpochSecond() {

        return this.seconds;
    }

    public int getNano() {

        return this.nanos;
    }

    @Override
    public TInstant with(TTemporalAdjuster adjuster) {

        return (TInstant) adjuster.adjustInto(this);
    }

    @Override
    public TInstant with(TTemporalField field, long newValue) {

        if (field instanceof TChronoField) {
            TChronoField f = (TChronoField) field;
            f.checkValidValue(newValue);
            switch (f) {
                case MILLI_OF_SECOND: {
                    int nval = (int) newValue * NANOS_PER_MILLI;
                    return (nval != this.nanos ? create(this.seconds, nval) : this);
                }
                case MICRO_OF_SECOND: {
                    int nval = (int) newValue * 1000;
                    return (nval != this.nanos ? create(this.seconds, nval) : this);
                }
                case NANO_OF_SECOND:
                    return (newValue != this.nanos ? create(this.seconds, (int) newValue) : this);
                case INSTANT_SECONDS:
                    return (newValue != this.seconds ? create(newValue, this.nanos) : this);
            }
            throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.adjustInto(this, newValue);
    }

    public TInstant truncatedTo(TTemporalUnit unit) {

        if (unit == TChronoUnit.NANOS) {
            return this;
        }
        TDuration unitDur = unit.getDuration();
        if (unitDur.getSeconds() > TLocalTime.SECONDS_PER_DAY) {
            throw new TDateTimeException("Unit is too large to be used for truncation");
        }
        long dur = unitDur.toNanos();
        if ((TLocalTime.NANOS_PER_DAY % dur) != 0) {
            throw new TDateTimeException("Unit must divide into a standard day without remainder");
        }
        long nod = (this.seconds % TLocalTime.SECONDS_PER_DAY) * TLocalTime.NANOS_PER_SECOND + this.nanos;
        long result = Math.floorDiv(nod, dur) * dur;
        return plusNanos(result - nod);
    }

    @Override
    public TInstant plus(TTemporalAmount amount) {

        return (TInstant) amount.addTo(this);
    }

    @Override
    public TInstant plus(long amountToAdd, TTemporalUnit unit) {

        if (unit instanceof TChronoUnit) {
            switch ((TChronoUnit) unit) {
                case NANOS:
                    return plusNanos(amountToAdd);
                case MICROS:
                    return plus(amountToAdd / 1000000, (amountToAdd % 1000000) * 1000);
                case MILLIS:
                    return plusMillis(amountToAdd);
                case SECONDS:
                    return plusSeconds(amountToAdd);
                case MINUTES:
                    return plusSeconds(Math.multiplyExact(amountToAdd, SECONDS_PER_MINUTE));
                case HOURS:
                    return plusSeconds(Math.multiplyExact(amountToAdd, SECONDS_PER_HOUR));
                case HALF_DAYS:
                    return plusSeconds(Math.multiplyExact(amountToAdd, SECONDS_PER_DAY / 2));
                case DAYS:
                    return plusSeconds(Math.multiplyExact(amountToAdd, SECONDS_PER_DAY));
            }
            throw new TUnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
        return unit.addTo(this, amountToAdd);
    }

    public TInstant plusSeconds(long secondsToAdd) {

        return plus(secondsToAdd, 0);
    }

    public TInstant plusMillis(long millisToAdd) {

        return plus(millisToAdd / 1000, (millisToAdd % 1000) * NANOS_PER_MILLI);
    }

    public TInstant plusNanos(long nanosToAdd) {

        return plus(0, nanosToAdd);
    }

    private TInstant plus(long secondsToAdd, long nanosToAdd) {

        if ((secondsToAdd | nanosToAdd) == 0) {
            return this;
        }
        long epochSec = Math.addExact(this.seconds, secondsToAdd);
        epochSec = Math.addExact(epochSec, nanosToAdd / NANOS_PER_SECOND);
        nanosToAdd = nanosToAdd % NANOS_PER_SECOND;
        long nanoAdjustment = this.nanos + nanosToAdd; // safe int+NANOS_PER_SECOND
        return ofEpochSecond(epochSec, nanoAdjustment);
    }

    @Override
    public TInstant minus(TTemporalAmount amount) {

        return (TInstant) amount.subtractFrom(this);
    }

    @Override
    public TInstant minus(long amountToSubtract, TTemporalUnit unit) {

        return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit)
                : plus(-amountToSubtract, unit));
    }

    public TInstant minusSeconds(long secondsToSubtract) {

        if (secondsToSubtract == Long.MIN_VALUE) {
            return plusSeconds(Long.MAX_VALUE).plusSeconds(1);
        }
        return plusSeconds(-secondsToSubtract);
    }

    public TInstant minusMillis(long millisToSubtract) {

        if (millisToSubtract == Long.MIN_VALUE) {
            return plusMillis(Long.MAX_VALUE).plusMillis(1);
        }
        return plusMillis(-millisToSubtract);
    }

    public TInstant minusNanos(long nanosToSubtract) {

        if (nanosToSubtract == Long.MIN_VALUE) {
            return plusNanos(Long.MAX_VALUE).plusNanos(1);
        }
        return plusNanos(-nanosToSubtract);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TTemporalQuery<R> query) {

        if (query == TTemporalQueries.precision()) {
            return (R) NANOS;
        }
        // inline TTemporalAccessor.super.query(query) as an optimization
        if (query == TTemporalQueries.localDate() || query == TTemporalQueries.localTime()
                || query == TTemporalQueries.chronology() || query == TTemporalQueries.zoneId()
                || query == TTemporalQueries.zone() || query == TTemporalQueries.offset()) {
            return null;
        }
        return query.queryFrom(this);
    }

    @Override
    public TTemporal adjustInto(TTemporal temporal) {

        return temporal.with(INSTANT_SECONDS, this.seconds).with(NANO_OF_SECOND, this.nanos);
    }

    @Override
    public long until(TTemporal endExclusive, TTemporalUnit unit) {

        TInstant end = TInstant.from(endExclusive);
        if (unit instanceof TChronoUnit) {
            TChronoUnit f = (TChronoUnit) unit;
            switch (f) {
                case NANOS:
                    return nanosUntil(end);
                case MICROS:
                    return nanosUntil(end) / 1000;
                case MILLIS:
                    return Math.subtractExact(end.toEpochMilli(), toEpochMilli());
                case SECONDS:
                    return secondsUntil(end);
                case MINUTES:
                    return secondsUntil(end) / SECONDS_PER_MINUTE;
                case HOURS:
                    return secondsUntil(end) / SECONDS_PER_HOUR;
                case HALF_DAYS:
                    return secondsUntil(end) / (12 * SECONDS_PER_HOUR);
                case DAYS:
                    return secondsUntil(end) / (SECONDS_PER_DAY);
            }
            throw new TUnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
        return unit.between(this, end);
    }

    private long nanosUntil(TInstant end) {

        long secsDiff = Math.subtractExact(end.seconds, this.seconds);
        long totalNanos = Math.multiplyExact(secsDiff, NANOS_PER_SECOND);
        return Math.addExact(totalNanos, end.nanos - this.nanos);
    }

    private long secondsUntil(TInstant end) {

        long secsDiff = Math.subtractExact(end.seconds, this.seconds);
        long nanosDiff = end.nanos - this.nanos;
        if (secsDiff > 0 && nanosDiff < 0) {
            secsDiff--;
        } else if (secsDiff < 0 && nanosDiff > 0) {
            secsDiff++;
        }
        return secsDiff;
    }

    public TOffsetDateTime atOffset(TZoneOffset offset) {

        return TOffsetDateTime.ofInstant(this, offset);
    }

    public TZonedDateTime atZone(TZoneId zone) {

        return TZonedDateTime.ofInstant(this, zone);
    }

    public long toEpochMilli() {

        if (this.seconds >= 0) {
            long millis = Math.multiplyExact(this.seconds, MILLIS_PER_SEC);
            return Math.addExact(millis, this.nanos / NANOS_PER_MILLI);
        } else {
            // prevent an overflow in seconds * 1000
            // instead of going form the second farther away from 0
            // going toward 0
            // we go from the second closer to 0 away from 0
            // that way we always stay in the valid long range
            // seconds + 1 can not overflow because it is negative
            long millis = Math.multiplyExact(this.seconds + 1, MILLIS_PER_SEC);
            return Math.subtractExact(millis, (MILLIS_PER_SEC - this.nanos / NANOS_PER_MILLI));
        }
    }

    @Override
    public int compareTo(TInstant otherInstant) {

        int cmp = Long.compare(this.seconds, otherInstant.seconds);
        if (cmp != 0) {
            return cmp;
        }
        return this.nanos - otherInstant.nanos;
    }

    public boolean isAfter(TInstant otherInstant) {

        return compareTo(otherInstant) > 0;
    }

    public boolean isBefore(TInstant otherInstant) {

        return compareTo(otherInstant) < 0;
    }

    @Override
    public boolean equals(Object otherInstant) {

        if (this == otherInstant) {
            return true;
        }
        if (otherInstant instanceof TInstant) {
            TInstant other = (TInstant) otherInstant;
            return this.seconds == other.seconds && this.nanos == other.nanos;
        }
        return false;
    }

    @Override
    public int hashCode() {

        return ((int) (this.seconds ^ (this.seconds >>> 32))) + 51 * this.nanos;
    }

    @Override
    public String toString() {

        return TDateTimeFormatter.ISO_INSTANT.format(this);
    }

}
