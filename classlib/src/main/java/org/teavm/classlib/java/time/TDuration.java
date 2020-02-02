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
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.NANOS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.SECONDS;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.time.format.TDateTimeParseException;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;

public final class TDuration implements TTemporalAmount, Comparable<TDuration>, TSerializable {

    public static final TDuration ZERO = new TDuration(0, 0);

    private static final int NANOS_PER_SECOND = 1000000000;

    private static final int NANOS_PER_MILLI = 1000000;

    private static final BigInteger BI_NANOS_PER_SECOND = BigInteger.valueOf(NANOS_PER_SECOND);

    private final static Pattern PATTERN = Pattern.compile(
            "([-+]?)P(?:([-+]?[0-9]+)D)?"
                    + "(T(?:([-+]?[0-9]+)H)?(?:([-+]?[0-9]+)M)?(?:([-+]?[0-9]+)(?:[.,]([0-9]{0,9}))?S)?)?",
            Pattern.CASE_INSENSITIVE);

    private final long seconds;

    private final int nanos;

    public static TDuration ofDays(long days) {

        return create(TJdk8Methods.safeMultiply(days, 86400), 0);
    }

    public static TDuration ofHours(long hours) {

        return create(TJdk8Methods.safeMultiply(hours, 3600), 0);
    }

    public static TDuration ofMinutes(long minutes) {

        return create(TJdk8Methods.safeMultiply(minutes, 60), 0);
    }

    public static TDuration ofSeconds(long seconds) {

        return create(seconds, 0);
    }

    public static TDuration ofSeconds(long seconds, long nanoAdjustment) {

        long secs = TJdk8Methods.safeAdd(seconds, TJdk8Methods.floorDiv(nanoAdjustment, NANOS_PER_SECOND));
        int nos = TJdk8Methods.floorMod(nanoAdjustment, NANOS_PER_SECOND);
        return create(secs, nos);
    }

    public static TDuration ofMillis(long millis) {

        long secs = millis / 1000;
        int mos = (int) (millis % 1000);
        if (mos < 0) {
            mos += 1000;
            secs--;
        }
        return create(secs, mos * NANOS_PER_MILLI);
    }

    public static TDuration ofNanos(long nanos) {

        long secs = nanos / NANOS_PER_SECOND;
        int nos = (int) (nanos % NANOS_PER_SECOND);
        if (nos < 0) {
            nos += NANOS_PER_SECOND;
            secs--;
        }
        return create(secs, nos);
    }

    public static TDuration of(long amount, TTemporalUnit unit) {

        return ZERO.plus(amount, unit);
    }

    public static TDuration from(TTemporalAmount amount) {

        TJdk8Methods.requireNonNull(amount, "amount");
        TDuration duration = ZERO;
        for (TTemporalUnit unit : amount.getUnits()) {
            duration = duration.plus(amount.get(unit), unit);
        }
        return duration;
    }

    public static TDuration between(TTemporal startInclusive, TTemporal endExclusive) {

        long secs = startInclusive.until(endExclusive, SECONDS);
        long nanos = 0;
        if (startInclusive.isSupported(NANO_OF_SECOND) && endExclusive.isSupported(NANO_OF_SECOND)) {
            try {
                long startNos = startInclusive.getLong(NANO_OF_SECOND);
                nanos = endExclusive.getLong(NANO_OF_SECOND) - startNos;
                if (secs > 0 && nanos < 0) {
                    nanos += NANOS_PER_SECOND;
                } else if (secs < 0 && nanos > 0) {
                    nanos -= NANOS_PER_SECOND;
                } else if (secs == 0 && nanos != 0) {
                    // two possible meanings for result, so recalculate secs
                    TTemporal adjustedEnd = endExclusive.with(NANO_OF_SECOND, startNos);
                    secs = startInclusive.until(adjustedEnd, SECONDS);
                    ;
                }
            } catch (TDateTimeException ex2) {
                // ignore and only use seconds
            } catch (ArithmeticException ex2) {
                // ignore and only use seconds
            }
        }
        return ofSeconds(secs, nanos);
    }

    public static TDuration parse(CharSequence text) {

        TJdk8Methods.requireNonNull(text, "text");
        Matcher matcher = PATTERN.matcher(text);
        if (matcher.matches()) {
            // check for letter T but no time sections
            if ("T".equals(matcher.group(3)) == false) {
                boolean negate = "-".equals(matcher.group(1));
                String dayMatch = matcher.group(2);
                String hourMatch = matcher.group(4);
                String minuteMatch = matcher.group(5);
                String secondMatch = matcher.group(6);
                String fractionMatch = matcher.group(7);
                if (dayMatch != null || hourMatch != null || minuteMatch != null || secondMatch != null) {
                    long daysAsSecs = parseNumber(text, dayMatch, SECONDS_PER_DAY, "days");
                    long hoursAsSecs = parseNumber(text, hourMatch, SECONDS_PER_HOUR, "hours");
                    long minsAsSecs = parseNumber(text, minuteMatch, SECONDS_PER_MINUTE, "minutes");
                    long seconds = parseNumber(text, secondMatch, 1, "seconds");
                    boolean negativeSecs = secondMatch != null && secondMatch.charAt(0) == '-';
                    int nanos = parseFraction(text, fractionMatch, negativeSecs ? -1 : 1);
                    try {
                        return create(negate, daysAsSecs, hoursAsSecs, minsAsSecs, seconds, nanos);
                    } catch (ArithmeticException ex) {
                        throw (TDateTimeParseException) new TDateTimeParseException(
                                "Text cannot be parsed to a TDuration: overflow", text, 0).initCause(ex);
                    }
                }
            }
        }
        throw new TDateTimeParseException("Text cannot be parsed to a TDuration", text, 0);
    }

    private static long parseNumber(CharSequence text, String parsed, int multiplier, String errorText) {

        // regex limits to [-+]?[0-9]+
        if (parsed == null) {
            return 0;
        }
        try {
            if (parsed.startsWith("+")) {
                parsed = parsed.substring(1);
            }
            long val = Long.parseLong(parsed);
            return TJdk8Methods.safeMultiply(val, multiplier);
        } catch (NumberFormatException ex) {
            throw (TDateTimeParseException) new TDateTimeParseException(
                    "Text cannot be parsed to a TDuration: " + errorText, text, 0).initCause(ex);
        } catch (ArithmeticException ex) {
            throw (TDateTimeParseException) new TDateTimeParseException(
                    "Text cannot be parsed to a TDuration: " + errorText, text, 0).initCause(ex);
        }
    }

    private static int parseFraction(CharSequence text, String parsed, int negate) {

        // regex limits to [0-9]{0,9}
        if (parsed == null || parsed.length() == 0) {
            return 0;
        }
        try {
            parsed = (parsed + "000000000").substring(0, 9);
            return Integer.parseInt(parsed) * negate;
        } catch (NumberFormatException ex) {
            throw (TDateTimeParseException) new TDateTimeParseException(
                    "Text cannot be parsed to a TDuration: fraction", text, 0).initCause(ex);
        } catch (ArithmeticException ex) {
            throw (TDateTimeParseException) new TDateTimeParseException(
                    "Text cannot be parsed to a TDuration: fraction", text, 0).initCause(ex);
        }
    }

    private static TDuration create(boolean negate, long daysAsSecs, long hoursAsSecs, long minsAsSecs, long secs,
            int nanos) {

        long seconds = TJdk8Methods.safeAdd(daysAsSecs,
                TJdk8Methods.safeAdd(hoursAsSecs, TJdk8Methods.safeAdd(minsAsSecs, secs)));
        if (negate) {
            return ofSeconds(seconds, nanos).negated();
        }
        return ofSeconds(seconds, nanos);
    }

    private static TDuration create(long seconds, int nanoAdjustment) {

        if ((seconds | nanoAdjustment) == 0) {
            return ZERO;
        }
        return new TDuration(seconds, nanoAdjustment);
    }

    private TDuration(long seconds, int nanos) {

        super();
        this.seconds = seconds;
        this.nanos = nanos;
    }

    @Override
    public List<TTemporalUnit> getUnits() {

        return Collections.<TTemporalUnit> unmodifiableList(Arrays.asList(SECONDS, NANOS));
    }

    @Override
    public long get(TTemporalUnit unit) {

        if (unit == SECONDS) {
            return this.seconds;
        }
        if (unit == NANOS) {
            return this.nanos;
        }
        throw new TUnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    public boolean isZero() {

        return (this.seconds | this.nanos) == 0;
    }

    public boolean isNegative() {

        return this.seconds < 0;
    }

    public long getSeconds() {

        return this.seconds;
    }

    public int getNano() {

        return this.nanos;
    }

    public TDuration withSeconds(long seconds) {

        return create(seconds, this.nanos);
    }

    public TDuration withNanos(int nanoOfSecond) {

        NANO_OF_SECOND.checkValidIntValue(nanoOfSecond);
        return create(this.seconds, nanoOfSecond);
    }

    public TDuration plus(TDuration duration) {

        return plus(duration.getSeconds(), duration.getNano());
    }

    public TDuration plus(long amountToAdd, TTemporalUnit unit) {

        TJdk8Methods.requireNonNull(unit, "unit");
        if (unit == DAYS) {
            return plus(TJdk8Methods.safeMultiply(amountToAdd, SECONDS_PER_DAY), 0);
        }
        if (unit.isDurationEstimated()) {
            throw new TDateTimeException("Unit must not have an estimated duration");
        }
        if (amountToAdd == 0) {
            return this;
        }
        if (unit instanceof TChronoUnit) {
            switch ((TChronoUnit) unit) {
                case NANOS:
                    return plusNanos(amountToAdd);
                case MICROS:
                    return plusSeconds((amountToAdd / (1000000L * 1000)) * 1000)
                            .plusNanos((amountToAdd % (1000000L * 1000)) * 1000);
                case MILLIS:
                    return plusMillis(amountToAdd);
                case SECONDS:
                    return plusSeconds(amountToAdd);
            }
            return plusSeconds(TJdk8Methods.safeMultiply(unit.getDuration().seconds, amountToAdd));
        }
        TDuration duration = unit.getDuration().multipliedBy(amountToAdd);
        return plusSeconds(duration.getSeconds()).plusNanos(duration.getNano());
    }

    public TDuration plusDays(long daysToAdd) {

        return plus(TJdk8Methods.safeMultiply(daysToAdd, SECONDS_PER_DAY), 0);
    }

    public TDuration plusHours(long hoursToAdd) {

        return plus(TJdk8Methods.safeMultiply(hoursToAdd, SECONDS_PER_HOUR), 0);
    }

    public TDuration plusMinutes(long minutesToAdd) {

        return plus(TJdk8Methods.safeMultiply(minutesToAdd, SECONDS_PER_MINUTE), 0);
    }

    public TDuration plusSeconds(long secondsToAdd) {

        return plus(secondsToAdd, 0);
    }

    public TDuration plusMillis(long millisToAdd) {

        return plus(millisToAdd / 1000, (millisToAdd % 1000) * NANOS_PER_MILLI);
    }

    public TDuration plusNanos(long nanosToAdd) {

        return plus(0, nanosToAdd);
    }

    private TDuration plus(long secondsToAdd, long nanosToAdd) {

        if ((secondsToAdd | nanosToAdd) == 0) {
            return this;
        }
        long epochSec = TJdk8Methods.safeAdd(this.seconds, secondsToAdd);
        epochSec = TJdk8Methods.safeAdd(epochSec, nanosToAdd / NANOS_PER_SECOND);
        nanosToAdd = nanosToAdd % NANOS_PER_SECOND;
        long nanoAdjustment = this.nanos + nanosToAdd; // safe int+NANOS_PER_SECOND
        return ofSeconds(epochSec, nanoAdjustment);
    }

    public TDuration minus(TDuration duration) {

        long secsToSubtract = duration.getSeconds();
        int nanosToSubtract = duration.getNano();
        if (secsToSubtract == Long.MIN_VALUE) {
            return plus(Long.MAX_VALUE, -nanosToSubtract).plus(1, 0);
        }
        return plus(-secsToSubtract, -nanosToSubtract);
    }

    public TDuration minus(long amountToSubtract, TTemporalUnit unit) {

        return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit)
                : plus(-amountToSubtract, unit));
    }

    public TDuration minusDays(long daysToSubtract) {

        return (daysToSubtract == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-daysToSubtract));
    }

    public TDuration minusHours(long hoursToSubtract) {

        return (hoursToSubtract == Long.MIN_VALUE ? plusHours(Long.MAX_VALUE).plusHours(1)
                : plusHours(-hoursToSubtract));
    }

    public TDuration minusMinutes(long minutesToSubtract) {

        return (minutesToSubtract == Long.MIN_VALUE ? plusMinutes(Long.MAX_VALUE).plusMinutes(1)
                : plusMinutes(-minutesToSubtract));
    }

    public TDuration minusSeconds(long secondsToSubtract) {

        return (secondsToSubtract == Long.MIN_VALUE ? plusSeconds(Long.MAX_VALUE).plusSeconds(1)
                : plusSeconds(-secondsToSubtract));
    }

    public TDuration minusMillis(long millisToSubtract) {

        return (millisToSubtract == Long.MIN_VALUE ? plusMillis(Long.MAX_VALUE).plusMillis(1)
                : plusMillis(-millisToSubtract));
    }

    public TDuration minusNanos(long nanosToSubtract) {

        return (nanosToSubtract == Long.MIN_VALUE ? plusNanos(Long.MAX_VALUE).plusNanos(1)
                : plusNanos(-nanosToSubtract));
    }

    public TDuration multipliedBy(long multiplicand) {

        if (multiplicand == 0) {
            return ZERO;
        }
        if (multiplicand == 1) {
            return this;
        }
        return create(toSeconds().multiply(BigDecimal.valueOf(multiplicand)));
    }

    public TDuration dividedBy(long divisor) {

        if (divisor == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        if (divisor == 1) {
            return this;
        }
        return create(toSeconds().divide(BigDecimal.valueOf(divisor), RoundingMode.DOWN));
    }

    private BigDecimal toSeconds() {

        return BigDecimal.valueOf(this.seconds).add(BigDecimal.valueOf(this.nanos, 9));
    }

    private static TDuration create(BigDecimal seconds) {

        BigInteger nanos = seconds.movePointRight(9).toBigIntegerExact();
        BigInteger[] divRem = nanos.divideAndRemainder(BI_NANOS_PER_SECOND);
        if (divRem[0].bitLength() > 63) {
            throw new ArithmeticException("Exceeds capacity of TDuration: " + nanos);
        }
        return ofSeconds(divRem[0].longValue(), divRem[1].intValue());
    }

    public TDuration negated() {

        return multipliedBy(-1);
    }

    public TDuration abs() {

        return isNegative() ? negated() : this;
    }

    @Override
    public TTemporal addTo(TTemporal temporal) {

        if (this.seconds != 0) {
            temporal = temporal.plus(this.seconds, SECONDS);
        }
        if (this.nanos != 0) {
            temporal = temporal.plus(this.nanos, NANOS);
        }
        return temporal;
    }

    @Override
    public TTemporal subtractFrom(TTemporal temporal) {

        if (this.seconds != 0) {
            temporal = temporal.minus(this.seconds, SECONDS);
        }
        if (this.nanos != 0) {
            temporal = temporal.minus(this.nanos, NANOS);
        }
        return temporal;
    }

    public long toDays() {

        return this.seconds / SECONDS_PER_DAY;
    }

    public long toHours() {

        return this.seconds / SECONDS_PER_HOUR;
    }

    public long toMinutes() {

        return this.seconds / SECONDS_PER_MINUTE;
    }

    public long toMillis() {

        long result = TJdk8Methods.safeMultiply(this.seconds, 1000);
        result = TJdk8Methods.safeAdd(result, this.nanos / NANOS_PER_MILLI);
        return result;
    }

    public long toNanos() {

        long result = TJdk8Methods.safeMultiply(this.seconds, NANOS_PER_SECOND);
        result = TJdk8Methods.safeAdd(result, this.nanos);
        return result;
    }

    @Override
    public int compareTo(TDuration otherDuration) {

        int cmp = TJdk8Methods.compareLongs(this.seconds, otherDuration.seconds);
        if (cmp != 0) {
            return cmp;
        }
        return this.nanos - otherDuration.nanos;
    }

    @Override
    public boolean equals(Object otherDuration) {

        if (this == otherDuration) {
            return true;
        }
        if (otherDuration instanceof TDuration) {
            TDuration other = (TDuration) otherDuration;
            return this.seconds == other.seconds && this.nanos == other.nanos;
        }
        return false;
    }

    @Override
    public int hashCode() {

        return ((int) (this.seconds ^ (this.seconds >>> 32))) + (51 * this.nanos);
    }

    @Override
    public String toString() {

        if (this == ZERO) {
            return "PT0S";
        }
        long hours = this.seconds / SECONDS_PER_HOUR;
        int minutes = (int) ((this.seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE);
        int secs = (int) (this.seconds % SECONDS_PER_MINUTE);
        StringBuilder buf = new StringBuilder(24);
        buf.append("PT");
        if (hours != 0) {
            buf.append(hours).append('H');
        }
        if (minutes != 0) {
            buf.append(minutes).append('M');
        }
        if (secs == 0 && this.nanos == 0 && buf.length() > 2) {
            return buf.toString();
        }
        if (secs < 0 && this.nanos > 0) {
            if (secs == -1) {
                buf.append("-0");
            } else {
                buf.append(secs + 1);
            }
        } else {
            buf.append(secs);
        }
        if (this.nanos > 0) {
            int pos = buf.length();
            if (secs < 0) {
                buf.append(2 * NANOS_PER_SECOND - this.nanos);
            } else {
                buf.append(this.nanos + NANOS_PER_SECOND);
            }
            while (buf.charAt(buf.length() - 1) == '0') {
                buf.setLength(buf.length() - 1);
            }
            buf.setCharAt(pos, '.');
        }
        buf.append('S');
        return buf.toString();
    }

}
