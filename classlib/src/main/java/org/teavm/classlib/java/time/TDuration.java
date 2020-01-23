/* Copyright (c) The m-m-m Team, Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0 */
package org.teavm.classlib.java.time;

import static org.teavm.classlib.java.time.TLocalTime.MINUTES_PER_HOUR;
import static org.teavm.classlib.java.time.TLocalTime.NANOS_PER_MILLI;
import static org.teavm.classlib.java.time.TLocalTime.NANOS_PER_SECOND;
import static org.teavm.classlib.java.time.TLocalTime.SECONDS_PER_DAY;
import static org.teavm.classlib.java.time.TLocalTime.SECONDS_PER_HOUR;
import static org.teavm.classlib.java.time.TLocalTime.SECONDS_PER_MINUTE;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.NANOS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.SECONDS;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TArithmeticException;
import org.teavm.classlib.java.lang.TComparable;
import org.teavm.classlib.java.lang.TNumberFormatException;
import org.teavm.classlib.java.lang.TStringBuilder;
import org.teavm.classlib.java.math.TBigDecimal;
import org.teavm.classlib.java.math.TBigInteger;
import org.teavm.classlib.java.math.TRoundingMode;
import org.teavm.classlib.java.time.format.TDateTimeParseException;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.util.regex.TMatcher;
import org.teavm.classlib.java.util.regex.TPattern;

public final class TDuration implements TTemporalAmount, TComparable<TDuration>, TSerializable {

  public static final TDuration ZERO = new TDuration(0, 0);

  private static final TBigInteger BI_NANOS_PER_SECOND = TBigInteger.valueOf(NANOS_PER_SECOND);

  private final long seconds;

  private final int nanos;

  private TDuration(long seconds, int nanos) {

    super();
    this.seconds = seconds;
    this.nanos = nanos;
  }

  @Override
  public long get(TTemporalUnit unit) {

    if (unit == SECONDS) {
      return this.seconds;
    } else if (unit == NANOS) {
      return this.nanos;
    } else {
      throw new TUnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }
  }

  @Override
  public List<TTemporalUnit> getUnits() {

    return TDurationUnits.UNITS;
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

    Objects.requireNonNull(unit, "unit");
    if (unit == DAYS) {
      return plus(Math.multiplyExact(amountToAdd, SECONDS_PER_DAY), 0);
    }
    if (unit.isDurationEstimated()) {
      throw new TUnsupportedTemporalTypeException("Unit must not have an estimated duration");
    }
    if (amountToAdd == 0) {
      return this;
    }
    if (unit instanceof TChronoUnit) {
      switch ((TChronoUnit) unit) {
        case NANOS:
          return plusNanos(amountToAdd);
        case MICROS:
          return plusSeconds((amountToAdd / (1000_000L * 1000)) * 1000)
              .plusNanos((amountToAdd % (1000_000L * 1000)) * 1000);
        case MILLIS:
          return plusMillis(amountToAdd);
        case SECONDS:
          return plusSeconds(amountToAdd);
      }
      return plusSeconds(Math.multiplyExact(unit.getDuration().seconds, amountToAdd));
    }
    TDuration duration = unit.getDuration().multipliedBy(amountToAdd);
    return plusSeconds(duration.getSeconds()).plusNanos(duration.getNano());
  }

  // -----------------------------------------------------------------------
  public TDuration plusDays(long daysToAdd) {

    return plus(Math.multiplyExact(daysToAdd, SECONDS_PER_DAY), 0);
  }

  public TDuration plusHours(long hoursToAdd) {

    return plus(Math.multiplyExact(hoursToAdd, SECONDS_PER_HOUR), 0);
  }

  public TDuration plusMinutes(long minutesToAdd) {

    return plus(Math.multiplyExact(minutesToAdd, SECONDS_PER_MINUTE), 0);
  }

  public TDuration plusSeconds(long secondsToAdd) {

    return plus(secondsToAdd, 0);
  }

  public TDuration plusMillis(long millisToAdd) {

    return plus(millisToAdd / 1000, (millisToAdd % 1000) * 1000_000);
  }

  public TDuration plusNanos(long nanosToAdd) {

    return plus(0, nanosToAdd);
  }

  private TDuration plus(long secondsToAdd, long nanosToAdd) {

    if ((secondsToAdd | nanosToAdd) == 0) {
      return this;
    }
    long epochSec = Math.addExact(this.seconds, secondsToAdd);
    epochSec = Math.addExact(epochSec, nanosToAdd / NANOS_PER_SECOND);
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

    return (hoursToSubtract == Long.MIN_VALUE ? plusHours(Long.MAX_VALUE).plusHours(1) : plusHours(-hoursToSubtract));
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

    return (nanosToSubtract == Long.MIN_VALUE ? plusNanos(Long.MAX_VALUE).plusNanos(1) : plusNanos(-nanosToSubtract));
  }

  public TDuration multipliedBy(long multiplicand) {

    if (multiplicand == 0) {
      return ZERO;
    }
    if (multiplicand == 1) {
      return this;
    }
    return create(toBigDecimalSeconds().multiply(TBigDecimal.valueOf(multiplicand)));
  }

  public TDuration dividedBy(long divisor) {

    if (divisor == 0) {
      throw new ArithmeticException("Cannot divide by zero");
    }
    if (divisor == 1) {
      return this;
    }
    return create(toBigDecimalSeconds().divide(TBigDecimal.valueOf(divisor), TRoundingMode.DOWN));
  }

  public long dividedBy(TDuration divisor) {

    Objects.requireNonNull(divisor, "divisor");
    TBigDecimal dividendBigD = toBigDecimalSeconds();
    TBigDecimal divisorBigD = divisor.toBigDecimalSeconds();
    return dividendBigD.divideToIntegralValue(divisorBigD).longValueExact();
  }

  private TBigDecimal toBigDecimalSeconds() {

    return TBigDecimal.valueOf(this.seconds).add(TBigDecimal.valueOf(this.nanos, 9));
  }

  private static TDuration create(TBigDecimal seconds) {

    TBigInteger nanos = seconds.movePointRight(9).toBigIntegerExact();
    TBigInteger[] divRem = nanos.divideAndRemainder(BI_NANOS_PER_SECOND);
    if (divRem[0].bitLength() > 63) {
      throw new TArithmeticException("Exceeds capacity of Duration: " + nanos);
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

  public long toSeconds() {

    return this.seconds;
  }

  public long toMillis() {

    long tempSeconds = this.seconds;
    long tempNanos = this.nanos;
    if (tempSeconds < 0) {
      tempSeconds = tempSeconds + 1;
      tempNanos = tempNanos - NANOS_PER_SECOND;
    }
    long millis = Math.multiplyExact(tempSeconds, 1000);
    millis = Math.addExact(millis, tempNanos / NANOS_PER_MILLI);
    return millis;
  }

  public long toNanos() {

    long tempSeconds = this.seconds;
    long tempNanos = this.nanos;
    if (tempSeconds < 0) {
      tempSeconds = tempSeconds + 1;
      tempNanos = tempNanos - NANOS_PER_SECOND;
    }
    long totalNanos = Math.multiplyExact(tempSeconds, NANOS_PER_SECOND);
    totalNanos = Math.addExact(totalNanos, tempNanos);
    return totalNanos;
  }

  public long toDaysPart() {

    return this.seconds / SECONDS_PER_DAY;
  }

  public int toHoursPart() {

    return (int) (toHours() % 24);
  }

  public int toMinutesPart() {

    return (int) (toMinutes() % MINUTES_PER_HOUR);
  }

  public int toSecondsPart() {

    return (int) (this.seconds % SECONDS_PER_MINUTE);
  }

  public int toMillisPart() {

    return this.nanos / 1000_000;
  }

  public int toNanosPart() {

    return this.nanos;
  }

  public TDuration truncatedTo(TTemporalUnit unit) {

    Objects.requireNonNull(unit, "unit");
    if (unit == TChronoUnit.SECONDS && (this.seconds >= 0 || this.nanos == 0)) {
      return new TDuration(this.seconds, 0);
    } else if (unit == TChronoUnit.NANOS) {
      return this;
    }
    TDuration unitDur = unit.getDuration();
    if (unitDur.getSeconds() > TLocalTime.SECONDS_PER_DAY) {
      throw new TUnsupportedTemporalTypeException("Unit is too large to be used for truncation");
    }
    long dur = unitDur.toNanos();
    if ((TLocalTime.NANOS_PER_DAY % dur) != 0) {
      throw new TUnsupportedTemporalTypeException("Unit must divide into a standard day without remainder");
    }
    long nod = (this.seconds % TLocalTime.SECONDS_PER_DAY) * TLocalTime.NANOS_PER_SECOND + this.nanos;
    long result = (nod / dur) * dur;
    return plusNanos(result - nod);
  }

  @Override
  public int compareTo(TDuration otherDuration) {

    int cmp = Long.compare(this.seconds, otherDuration.seconds);
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
    long effectiveTotalSecs = this.seconds;
    if (this.seconds < 0 && this.nanos > 0) {
      effectiveTotalSecs++;
    }
    long hours = effectiveTotalSecs / SECONDS_PER_HOUR;
    int minutes = (int) ((effectiveTotalSecs % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE);
    int secs = (int) (effectiveTotalSecs % SECONDS_PER_MINUTE);
    TStringBuilder buf = new TStringBuilder(24);
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
    if (this.seconds < 0 && this.nanos > 0) {
      if (secs == 0) {
        buf.append("-0");
      } else {
        buf.append(secs);
      }
    } else {
      buf.append(secs);
    }
    if (this.nanos > 0) {
      int pos = buf.length();
      if (this.seconds < 0) {
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

  public static TDuration ofDays(long days) {

    return create(Math.multiplyExact(days, SECONDS_PER_DAY), 0);
  }

  public static TDuration ofHours(long hours) {

    return create(Math.multiplyExact(hours, SECONDS_PER_HOUR), 0);
  }

  public static TDuration ofMinutes(long minutes) {

    return create(Math.multiplyExact(minutes, SECONDS_PER_MINUTE), 0);
  }

  public static TDuration ofSeconds(long seconds) {

    return create(seconds, 0);
  }

  public static TDuration ofSeconds(long seconds, long nanoAdjustment) {

    long secs = Math.addExact(seconds, Math.floorDiv(nanoAdjustment, NANOS_PER_SECOND));
    int nos = (int) Math.floorMod(nanoAdjustment, NANOS_PER_SECOND);
    return create(secs, nos);
  }

  public static TDuration ofMillis(long millis) {

    long secs = millis / 1000;
    int mos = (int) (millis % 1000);
    if (mos < 0) {
      mos += 1000;
      secs--;
    }
    return create(secs, mos * 1000_000);
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

    Objects.requireNonNull(amount, "amount");
    TDuration duration = ZERO;
    for (TTemporalUnit unit : amount.getUnits()) {
      duration = duration.plus(amount.get(unit), unit);
    }
    return duration;
  }

  public static TDuration parse(CharSequence text) {

    Objects.requireNonNull(text, "text");
    TMatcher matcher = TLazy.PATTERN.matcher(text);
    if (matcher.matches()) {
      // check for letter T but no time sections
      if (!charMatch(text, matcher.start(3), matcher.end(3), 'T')) {
        boolean negate = charMatch(text, matcher.start(1), matcher.end(1), '-');

        int dayStart = matcher.start(2), dayEnd = matcher.end(2);
        int hourStart = matcher.start(4), hourEnd = matcher.end(4);
        int minuteStart = matcher.start(5), minuteEnd = matcher.end(5);
        int secondStart = matcher.start(6), secondEnd = matcher.end(6);
        int fractionStart = matcher.start(7), fractionEnd = matcher.end(7);

        if (dayStart >= 0 || hourStart >= 0 || minuteStart >= 0 || secondStart >= 0) {
          long daysAsSecs = parseNumber(text, dayStart, dayEnd, SECONDS_PER_DAY, "days");
          long hoursAsSecs = parseNumber(text, hourStart, hourEnd, SECONDS_PER_HOUR, "hours");
          long minsAsSecs = parseNumber(text, minuteStart, minuteEnd, SECONDS_PER_MINUTE, "minutes");
          long seconds = parseNumber(text, secondStart, secondEnd, 1, "seconds");
          boolean negativeSecs = secondStart >= 0 && text.charAt(secondStart) == '-';
          int nanos = parseFraction(text, fractionStart, fractionEnd, negativeSecs ? -1 : 1);
          try {
            return create(negate, daysAsSecs, hoursAsSecs, minsAsSecs, seconds, nanos);
          } catch (TArithmeticException ex) {
            throw new TDateTimeParseException("Text cannot be parsed to a Duration: overflow", text, 0, ex);
          }
        }
      }
    }
    throw new TDateTimeParseException("Text cannot be parsed to a Duration", text, 0);
  }

  private static boolean charMatch(CharSequence text, int start, int end, char c) {

    return (start >= 0 && end == start + 1 && text.charAt(start) == c);
  }

  private static long parseNumber(CharSequence text, int start, int end, int multiplier, String errorText) {

    // regex limits to [-+]?[0-9]+
    if (start < 0 || end < 0) {
      return 0;
    }
    try {
      long val = Long.parseLong(text, start, end, 10);
      return Math.multiplyExact(val, multiplier);
    } catch (TNumberFormatException | TArithmeticException ex) {
      throw new TDateTimeParseException("Text cannot be parsed to a Duration: " + errorText, text, 0, ex);
    }
  }

  private static int parseFraction(CharSequence text, int start, int end, int negate) {

    // regex limits to [0-9]{0,9}
    if (start < 0 || end < 0 || end - start == 0) {
      return 0;
    }
    try {
      int fraction = Integer.parseInt(text, start, end, 10);

      // for number strings smaller than 9 digits, interpret as if there
      // were trailing zeros
      for (int i = end - start; i < 9; i++) {
        fraction *= 10;
      }
      return fraction * negate;
    } catch (TNumberFormatException | TArithmeticException ex) {
      throw new TDateTimeParseException("Text cannot be parsed to a Duration: fraction", text, 0, ex);
    }
  }

  private static TDuration create(boolean negate, long daysAsSecs, long hoursAsSecs, long minsAsSecs, long secs,
      int nanos) {

    long seconds = Math.addExact(daysAsSecs, Math.addExact(hoursAsSecs, Math.addExact(minsAsSecs, secs)));
    if (negate) {
      return ofSeconds(seconds, nanos).negated();
    }
    return ofSeconds(seconds, nanos);
  }

  public static TDuration between(TTemporal startInclusive, TTemporal endExclusive) {

    try {
      return ofNanos(startInclusive.until(endExclusive, NANOS));
    } catch (TDateTimeException | TArithmeticException ex) {
      long secs = startInclusive.until(endExclusive, SECONDS);
      long nanos;
      try {
        nanos = endExclusive.getLong(NANO_OF_SECOND) - startInclusive.getLong(NANO_OF_SECOND);
        if (secs > 0 && nanos < 0) {
          secs++;
        } else if (secs < 0 && nanos > 0) {
          secs--;
        }
      } catch (TDateTimeException ex2) {
        nanos = 0;
      }
      return ofSeconds(secs, nanos);
    }
  }

  private static TDuration create(long seconds, int nanoAdjustment) {

    if ((seconds | nanoAdjustment) == 0) {
      return ZERO;
    }
    return new TDuration(seconds, nanoAdjustment);
  }

  private static class TDurationUnits {
    static final List<TTemporalUnit> UNITS = Collections.unmodifiableList(Arrays.asList(SECONDS, NANOS));
  }

  private static class TLazy {
    static final TPattern PATTERN = TPattern.compile(
        "([-+]?)P(?:([-+]?[0-9]+)D)?"
            + "(T(?:([-+]?[0-9]+)H)?(?:([-+]?[0-9]+)M)?(?:([-+]?[0-9]+)(?:[.,]([0-9]{0,9}))?S)?)?",
        TPattern.CASE_INSENSITIVE);
  }

}
