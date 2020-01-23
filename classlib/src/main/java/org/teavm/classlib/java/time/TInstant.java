package org.teavm.classlib.java.time;

import static org.teavm.classlib.java.time.TLocalTime.NANOS_PER_SECOND;
import static org.teavm.classlib.java.time.TLocalTime.SECONDS_PER_DAY;
import static org.teavm.classlib.java.time.TLocalTime.SECONDS_PER_HOUR;
import static org.teavm.classlib.java.time.TLocalTime.SECONDS_PER_MINUTE;
import static org.teavm.classlib.java.time.temporal.TChronoField.INSTANT_SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoField.MICRO_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.MILLI_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.NANOS;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TComparable;
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
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.temporal.TValueRange;
import org.teavm.classlib.java.util.TObjects;

public final class TInstant implements TTemporal, TTemporalAdjuster, TComparable<TInstant>, TSerializable {

  public static final TInstant EPOCH = new TInstant(0, 0);

  private static final long MIN_SECOND = -31557014167219200L;

  private static final long MAX_SECOND = 31556889864403199L;

  public static final TInstant MIN = TInstant.ofEpochSecond(MIN_SECOND, 0);

  public static final TInstant MAX = TInstant.ofEpochSecond(MAX_SECOND, 999_999_999);

  private final long seconds;

  private final int nanos;

  private TInstant(long epochSecond, int nanos) {

    super();
    this.seconds = epochSecond;
    this.nanos = nanos;
  }

  public long getEpochSecond() {

    return this.seconds;
  }

  public int getNano() {

    return this.nanos;
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

  @Override
  public TValueRange range(TTemporalField field) {

    return TTemporal.super.range(field);
  }

  @Override
  public int get(TTemporalField field) {

    if (field instanceof TChronoField) {
      switch ((TChronoField) field) {
        case NANO_OF_SECOND:
          return this.nanos;
        case MICRO_OF_SECOND:
          return this.nanos / 1000;
        case MILLI_OF_SECOND:
          return this.nanos / 1000_000;
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
          return this.nanos / 1000_000;
        case INSTANT_SECONDS:
          return this.seconds;
      }
      throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
    }
    return field.getFrom(this);
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
          int nval = (int) newValue * 1000_000;
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
      throw new TUnsupportedTemporalTypeException("Unit is too large to be used for truncation");
    }
    long dur = unitDur.toNanos();
    if ((TLocalTime.NANOS_PER_DAY % dur) != 0) {
      throw new TUnsupportedTemporalTypeException("Unit must divide into a standard day without remainder");
    }
    long nod = (this.seconds % TLocalTime.SECONDS_PER_DAY) * TLocalTime.NANOS_PER_SECOND + this.nanos;
    long result = Math.floorDiv(nod, dur) * dur;
    return plusNanos(result - nod);
  }

  @Override
  public TInstant plus(TTemporalAmount amountToAdd) {

    return (TInstant) amountToAdd.addTo(this);
  }

  @Override
  public TInstant plus(long amountToAdd, TTemporalUnit unit) {

    if (unit instanceof TChronoUnit) {
      switch ((TChronoUnit) unit) {
        case NANOS:
          return plusNanos(amountToAdd);
        case MICROS:
          return plus(amountToAdd / 1000_000, (amountToAdd % 1000_000) * 1000);
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

    return plus(millisToAdd / 1000, (millisToAdd % 1000) * 1000_000);
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
  public TInstant minus(TTemporalAmount amountToSubtract) {

    return (TInstant) amountToSubtract.subtractFrom(this);
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
    if (query == TTemporalQueries.chronology() || query == TTemporalQueries.zoneId() || query == TTemporalQueries.zone()
        || query == TTemporalQueries.offset() || query == TTemporalQueries.localDate()
        || query == TTemporalQueries.localTime()) {
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

    if (this.seconds < 0 && this.nanos > 0) {
      long millis = Math.multiplyExact(this.seconds + 1, 1000);
      long adjustment = this.nanos / 1000_000 - 1000;
      return Math.addExact(millis, adjustment);
    } else {
      long millis = Math.multiplyExact(this.seconds, 1000);
      return Math.addExact(millis, this.nanos / 1000_000);
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

  public static TInstant now() {

    return TClock.systemUTC().instant();
  }

  public static TInstant now(TClock clock) {

    TObjects.requireNonNull(clock, "clock");
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
    int mos = Math.floorMod(epochMilli, 1000);
    return create(secs, mos * 1000_000);
  }

  public static TInstant from(TTemporalAccessor temporal) {

    if (temporal instanceof TInstant) {
      return (TInstant) temporal;
    }
    TObjects.requireNonNull(temporal, "temporal");
    try {
      long instantSecs = temporal.getLong(INSTANT_SECONDS);
      int nanoOfSecond = temporal.get(NANO_OF_SECOND);
      return TInstant.ofEpochSecond(instantSecs, nanoOfSecond);
    } catch (TDateTimeException ex) {
      throw new TDateTimeException(
          "Unable to obtain Instant from TemporalAccessor: " + temporal + " of type " + temporal.getClass().getName(),
          ex);
    }
  }

  public static TInstant parse(final CharSequence text) {

    return TDateTimeFormatter.ISO_INSTANT.parse(text, TInstant::from);
  }

  private static TInstant create(long seconds, int nanoOfSecond) {

    if ((seconds | nanoOfSecond) == 0) {
      return EPOCH;
    }
    if (seconds < MIN_SECOND || seconds > MAX_SECOND) {
      throw new TDateTimeException("Instant exceeds minimum or maximum instant");
    }
    return new TInstant(seconds, nanoOfSecond);
  }

}
