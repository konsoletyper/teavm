package org.teavm.classlib.java.time;

import static org.teavm.classlib.java.time.temporal.TChronoField.HOUR_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.MICRO_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.MINUTE_OF_HOUR;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.SECOND_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.SECOND_OF_MINUTE;
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

public final class TLocalTime implements TTemporal, TTemporalAdjuster, TComparable<TLocalTime>, TSerializable {

  public static final TLocalTime MIN;

  public static final TLocalTime MAX;

  public static final TLocalTime MIDNIGHT;

  public static final TLocalTime NOON;

  private static final TLocalTime[] HOURS = new TLocalTime[24];
  static {
    for (int i = 0; i < HOURS.length; i++) {
      HOURS[i] = new TLocalTime(i, 0, 0, 0);
    }
    MIDNIGHT = HOURS[0];
    NOON = HOURS[12];
    MIN = HOURS[0];
    MAX = new TLocalTime(23, 59, 59, 999_999_999);
  }

  static final int HOURS_PER_DAY = 24;

  static final int MINUTES_PER_HOUR = 60;

  static final int MINUTES_PER_DAY = MINUTES_PER_HOUR * HOURS_PER_DAY;

  static final int SECONDS_PER_MINUTE = 60;

  static final int SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR;

  static final int SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY;

  static final long MILLIS_PER_DAY = SECONDS_PER_DAY * 1000L;

  static final long MICROS_PER_DAY = SECONDS_PER_DAY * 1000_000L;

  static final long NANOS_PER_MILLI = 1000_000L;

  static final long NANOS_PER_SECOND = 1000_000_000L;

  static final long NANOS_PER_MINUTE = NANOS_PER_SECOND * SECONDS_PER_MINUTE;

  static final long NANOS_PER_HOUR = NANOS_PER_MINUTE * MINUTES_PER_HOUR;

  static final long NANOS_PER_DAY = NANOS_PER_HOUR * HOURS_PER_DAY;

  private final byte hour;

  private final byte minute;

  private final byte second;

  private final int nano;

  private TLocalTime(int hour, int minute, int second, int nanoOfSecond) {

    this.hour = (byte) hour;
    this.minute = (byte) minute;
    this.second = (byte) second;
    this.nano = nanoOfSecond;
  }

  @Override
  public boolean isSupported(TTemporalField field) {

    if (field instanceof TChronoField) {
      return field.isTimeBased();
    }
    return field != null && field.isSupportedBy(this);
  }

  @Override
  public boolean isSupported(TTemporalUnit unit) {

    if (unit instanceof TChronoUnit) {
      return unit.isTimeBased();
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
      return get0(field);
    }
    return TTemporal.super.get(field);
  }

  @Override
  public long getLong(TTemporalField field) {

    if (field instanceof TChronoField) {
      if (field == NANO_OF_DAY) {
        return toNanoOfDay();
      }
      if (field == MICRO_OF_DAY) {
        return toNanoOfDay() / 1000;
      }
      return get0(field);
    }
    return field.getFrom(this);
  }

  private int get0(TTemporalField field) {

    switch ((TChronoField) field) {
      case NANO_OF_SECOND:
        return this.nano;
      case NANO_OF_DAY:
        throw new TUnsupportedTemporalTypeException(
            "Invalid field 'NanoOfDay' for get() method, use getLong() instead");
      case MICRO_OF_SECOND:
        return this.nano / 1000;
      case MICRO_OF_DAY:
        throw new TUnsupportedTemporalTypeException(
            "Invalid field 'MicroOfDay' for get() method, use getLong() instead");
      case MILLI_OF_SECOND:
        return this.nano / 1000_000;
      case MILLI_OF_DAY:
        return (int) (toNanoOfDay() / 1000_000);
      case SECOND_OF_MINUTE:
        return this.second;
      case SECOND_OF_DAY:
        return toSecondOfDay();
      case MINUTE_OF_HOUR:
        return this.minute;
      case MINUTE_OF_DAY:
        return this.hour * 60 + this.minute;
      case HOUR_OF_AMPM:
        return this.hour % 12;
      case CLOCK_HOUR_OF_AMPM:
        int ham = this.hour % 12;
        return (ham % 12 == 0 ? 12 : ham);
      case HOUR_OF_DAY:
        return this.hour;
      case CLOCK_HOUR_OF_DAY:
        return (this.hour == 0 ? 24 : this.hour);
      case AMPM_OF_DAY:
        return this.hour / 12;
    }
    throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
  }

  public int getHour() {

    return this.hour;
  }

  public int getMinute() {

    return this.minute;
  }

  public int getSecond() {

    return this.second;
  }

  public int getNano() {

    return this.nano;
  }

  @Override
  public TLocalTime with(TTemporalAdjuster adjuster) {

    // optimizations
    if (adjuster instanceof TLocalTime) {
      return (TLocalTime) adjuster;
    }
    return (TLocalTime) adjuster.adjustInto(this);
  }

  @Override
  public TLocalTime with(TTemporalField field, long newValue) {

    if (field instanceof TChronoField) {
      TChronoField f = (TChronoField) field;
      f.checkValidValue(newValue);
      switch (f) {
        case NANO_OF_SECOND:
          return withNano((int) newValue);
        case NANO_OF_DAY:
          return TLocalTime.ofNanoOfDay(newValue);
        case MICRO_OF_SECOND:
          return withNano((int) newValue * 1000);
        case MICRO_OF_DAY:
          return TLocalTime.ofNanoOfDay(newValue * 1000);
        case MILLI_OF_SECOND:
          return withNano((int) newValue * 1000_000);
        case MILLI_OF_DAY:
          return TLocalTime.ofNanoOfDay(newValue * 1000_000);
        case SECOND_OF_MINUTE:
          return withSecond((int) newValue);
        case SECOND_OF_DAY:
          return plusSeconds(newValue - toSecondOfDay());
        case MINUTE_OF_HOUR:
          return withMinute((int) newValue);
        case MINUTE_OF_DAY:
          return plusMinutes(newValue - (this.hour * 60 + this.minute));
        case HOUR_OF_AMPM:
          return plusHours(newValue - (this.hour % 12));
        case CLOCK_HOUR_OF_AMPM:
          return plusHours((newValue == 12 ? 0 : newValue) - (this.hour % 12));
        case HOUR_OF_DAY:
          return withHour((int) newValue);
        case CLOCK_HOUR_OF_DAY:
          return withHour((int) (newValue == 24 ? 0 : newValue));
        case AMPM_OF_DAY:
          return plusHours((newValue - (this.hour / 12)) * 12);
      }
      throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
    }
    return field.adjustInto(this, newValue);
  }

  public TLocalTime withHour(int hour) {

    if (this.hour == hour) {
      return this;
    }
    HOUR_OF_DAY.checkValidValue(hour);
    return create(hour, this.minute, this.second, this.nano);
  }

  public TLocalTime withMinute(int minute) {

    if (this.minute == minute) {
      return this;
    }
    MINUTE_OF_HOUR.checkValidValue(minute);
    return create(this.hour, minute, this.second, this.nano);
  }

  public TLocalTime withSecond(int second) {

    if (this.second == second) {
      return this;
    }
    SECOND_OF_MINUTE.checkValidValue(second);
    return create(this.hour, this.minute, second, this.nano);
  }

  public TLocalTime withNano(int nanoOfSecond) {

    if (this.nano == nanoOfSecond) {
      return this;
    }
    NANO_OF_SECOND.checkValidValue(nanoOfSecond);
    return create(this.hour, this.minute, this.second, nanoOfSecond);
  }

  public TLocalTime truncatedTo(TTemporalUnit unit) {

    if (unit == TChronoUnit.NANOS) {
      return this;
    }
    TDuration unitDur = unit.getDuration();
    if (unitDur.getSeconds() > SECONDS_PER_DAY) {
      throw new TUnsupportedTemporalTypeException("Unit is too large to be used for truncation");
    }
    long dur = unitDur.toNanos();
    if ((NANOS_PER_DAY % dur) != 0) {
      throw new TUnsupportedTemporalTypeException("Unit must divide into a standard day without remainder");
    }
    long nod = toNanoOfDay();
    return ofNanoOfDay((nod / dur) * dur);
  }

  @Override
  public TLocalTime plus(TTemporalAmount amountToAdd) {

    return (TLocalTime) amountToAdd.addTo(this);
  }

  @Override
  public TLocalTime plus(long amountToAdd, TTemporalUnit unit) {

    if (unit instanceof TChronoUnit) {
      switch ((TChronoUnit) unit) {
        case NANOS:
          return plusNanos(amountToAdd);
        case MICROS:
          return plusNanos((amountToAdd % MICROS_PER_DAY) * 1000);
        case MILLIS:
          return plusNanos((amountToAdd % MILLIS_PER_DAY) * 1000_000);
        case SECONDS:
          return plusSeconds(amountToAdd);
        case MINUTES:
          return plusMinutes(amountToAdd);
        case HOURS:
          return plusHours(amountToAdd);
        case HALF_DAYS:
          return plusHours((amountToAdd % 2) * 12);
      }
      throw new TUnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }
    return unit.addTo(this, amountToAdd);
  }

  public TLocalTime plusHours(long hoursToAdd) {

    if (hoursToAdd == 0) {
      return this;
    }
    int newHour = ((int) (hoursToAdd % HOURS_PER_DAY) + this.hour + HOURS_PER_DAY) % HOURS_PER_DAY;
    return create(newHour, this.minute, this.second, this.nano);
  }

  public TLocalTime plusMinutes(long minutesToAdd) {

    if (minutesToAdd == 0) {
      return this;
    }
    int mofd = this.hour * MINUTES_PER_HOUR + this.minute;
    int newMofd = ((int) (minutesToAdd % MINUTES_PER_DAY) + mofd + MINUTES_PER_DAY) % MINUTES_PER_DAY;
    if (mofd == newMofd) {
      return this;
    }
    int newHour = newMofd / MINUTES_PER_HOUR;
    int newMinute = newMofd % MINUTES_PER_HOUR;
    return create(newHour, newMinute, this.second, this.nano);
  }

  public TLocalTime plusSeconds(long secondstoAdd) {

    if (secondstoAdd == 0) {
      return this;
    }
    int sofd = this.hour * SECONDS_PER_HOUR + this.minute * SECONDS_PER_MINUTE + this.second;
    int newSofd = ((int) (secondstoAdd % SECONDS_PER_DAY) + sofd + SECONDS_PER_DAY) % SECONDS_PER_DAY;
    if (sofd == newSofd) {
      return this;
    }
    int newHour = newSofd / SECONDS_PER_HOUR;
    int newMinute = (newSofd / SECONDS_PER_MINUTE) % MINUTES_PER_HOUR;
    int newSecond = newSofd % SECONDS_PER_MINUTE;
    return create(newHour, newMinute, newSecond, this.nano);
  }

  public TLocalTime plusNanos(long nanosToAdd) {

    if (nanosToAdd == 0) {
      return this;
    }
    long nofd = toNanoOfDay();
    long newNofd = ((nanosToAdd % NANOS_PER_DAY) + nofd + NANOS_PER_DAY) % NANOS_PER_DAY;
    if (nofd == newNofd) {
      return this;
    }
    int newHour = (int) (newNofd / NANOS_PER_HOUR);
    int newMinute = (int) ((newNofd / NANOS_PER_MINUTE) % MINUTES_PER_HOUR);
    int newSecond = (int) ((newNofd / NANOS_PER_SECOND) % SECONDS_PER_MINUTE);
    int newNano = (int) (newNofd % NANOS_PER_SECOND);
    return create(newHour, newMinute, newSecond, newNano);
  }

  @Override
  public TLocalTime minus(TTemporalAmount amountToSubtract) {

    return (TLocalTime) amountToSubtract.subtractFrom(this);
  }

  @Override
  public TLocalTime minus(long amountToSubtract, TTemporalUnit unit) {

    return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit)
        : plus(-amountToSubtract, unit));
  }

  public TLocalTime minusHours(long hoursToSubtract) {

    return plusHours(-(hoursToSubtract % HOURS_PER_DAY));
  }

  public TLocalTime minusMinutes(long minutesToSubtract) {

    return plusMinutes(-(minutesToSubtract % MINUTES_PER_DAY));
  }

  public TLocalTime minusSeconds(long secondsToSubtract) {

    return plusSeconds(-(secondsToSubtract % SECONDS_PER_DAY));
  }

  public TLocalTime minusNanos(long nanosToSubtract) {

    return plusNanos(-(nanosToSubtract % NANOS_PER_DAY));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <R> R query(TTemporalQuery<R> query) {

    if (query == TTemporalQueries.chronology() || query == TTemporalQueries.zoneId() || query == TTemporalQueries.zone()
        || query == TTemporalQueries.offset()) {
      return null;
    } else if (query == TTemporalQueries.localTime()) {
      return (R) this;
    } else if (query == TTemporalQueries.localDate()) {
      return null;
    } else if (query == TTemporalQueries.precision()) {
      return (R) NANOS;
    }
    return query.queryFrom(this);
  }

  @Override
  public TTemporal adjustInto(TTemporal temporal) {

    return temporal.with(NANO_OF_DAY, toNanoOfDay());
  }

  @Override
  public long until(TTemporal endExclusive, TTemporalUnit unit) {

    TLocalTime end = TLocalTime.from(endExclusive);
    if (unit instanceof TChronoUnit) {
      long nanosUntil = end.toNanoOfDay() - toNanoOfDay();
      switch ((TChronoUnit) unit) {
        case NANOS:
          return nanosUntil;
        case MICROS:
          return nanosUntil / 1000;
        case MILLIS:
          return nanosUntil / 1000_000;
        case SECONDS:
          return nanosUntil / NANOS_PER_SECOND;
        case MINUTES:
          return nanosUntil / NANOS_PER_MINUTE;
        case HOURS:
          return nanosUntil / NANOS_PER_HOUR;
        case HALF_DAYS:
          return nanosUntil / (12 * NANOS_PER_HOUR);
      }
      throw new TUnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }
    return unit.between(this, end);
  }

  public String format(TDateTimeFormatter formatter) {

    TObjects.requireNonNull(formatter, "formatter");
    return formatter.format(this);
  }

  public TLocalDateTime atDate(TLocalDate date) {

    return TLocalDateTime.of(date, this);
  }

  public TOffsetTime atOffset(TZoneOffset offset) {

    return TOffsetTime.of(this, offset);
  }

  public int toSecondOfDay() {

    int total = this.hour * SECONDS_PER_HOUR;
    total += this.minute * SECONDS_PER_MINUTE;
    total += this.second;
    return total;
  }

  public long toNanoOfDay() {

    long total = this.hour * NANOS_PER_HOUR;
    total += this.minute * NANOS_PER_MINUTE;
    total += this.second * NANOS_PER_SECOND;
    total += this.nano;
    return total;
  }

  public long toEpochSecond(TLocalDate date, TZoneOffset offset) {

    TObjects.requireNonNull(date, "date");
    TObjects.requireNonNull(offset, "offset");
    long epochDay = date.toEpochDay();
    long secs = epochDay * 86400 + toSecondOfDay();
    secs -= offset.getTotalSeconds();
    return secs;
  }

  @Override
  public int compareTo(TLocalTime other) {

    int cmp = Integer.compare(this.hour, other.hour);
    if (cmp == 0) {
      cmp = Integer.compare(this.minute, other.minute);
      if (cmp == 0) {
        cmp = Integer.compare(this.second, other.second);
        if (cmp == 0) {
          cmp = Integer.compare(this.nano, other.nano);
        }
      }
    }
    return cmp;
  }

  public boolean isAfter(TLocalTime other) {

    return compareTo(other) > 0;
  }

  public boolean isBefore(TLocalTime other) {

    return compareTo(other) < 0;
  }

  @Override
  public boolean equals(Object obj) {

    if (this == obj) {
      return true;
    }
    if (obj instanceof TLocalTime) {
      TLocalTime other = (TLocalTime) obj;
      return this.hour == other.hour && this.minute == other.minute && this.second == other.second
          && this.nano == other.nano;
    }
    return false;
  }

  @Override
  public int hashCode() {

    long nod = toNanoOfDay();
    return (int) (nod ^ (nod >>> 32));
  }

  @Override
  public String toString() {

    StringBuilder buf = new StringBuilder(18);
    int hourValue = this.hour;
    int minuteValue = this.minute;
    int secondValue = this.second;
    int nanoValue = this.nano;
    buf.append(hourValue < 10 ? "0" : "").append(hourValue).append(minuteValue < 10 ? ":0" : ":").append(minuteValue);
    if (secondValue > 0 || nanoValue > 0) {
      buf.append(secondValue < 10 ? ":0" : ":").append(secondValue);
      if (nanoValue > 0) {
        buf.append('.');
        if (nanoValue % 1000_000 == 0) {
          buf.append(Integer.toString((nanoValue / 1000_000) + 1000).substring(1));
        } else if (nanoValue % 1000 == 0) {
          buf.append(Integer.toString((nanoValue / 1000) + 1000_000).substring(1));
        } else {
          buf.append(Integer.toString((nanoValue) + 1000_000_000).substring(1));
        }
      }
    }
    return buf.toString();
  }

  public static TLocalTime now() {

    return now(TClock.systemDefaultZone());
  }

  public static TLocalTime now(TZoneId zone) {

    return now(TClock.system(zone));
  }

  public static TLocalTime now(TClock clock) {

    TObjects.requireNonNull(clock, "clock");
    final TInstant now = clock.instant();
    return ofInstant(now, clock.getZone());
  }

  public static TLocalTime of(int hour, int minute) {

    HOUR_OF_DAY.checkValidValue(hour);
    if (minute == 0) {
      return HOURS[hour];
    }
    MINUTE_OF_HOUR.checkValidValue(minute);
    return new TLocalTime(hour, minute, 0, 0);
  }

  public static TLocalTime of(int hour, int minute, int second) {

    HOUR_OF_DAY.checkValidValue(hour);
    if ((minute | second) == 0) {
      return HOURS[hour];
    }
    MINUTE_OF_HOUR.checkValidValue(minute);
    SECOND_OF_MINUTE.checkValidValue(second);
    return new TLocalTime(hour, minute, second, 0);
  }

  public static TLocalTime of(int hour, int minute, int second, int nanoOfSecond) {

    HOUR_OF_DAY.checkValidValue(hour);
    MINUTE_OF_HOUR.checkValidValue(minute);
    SECOND_OF_MINUTE.checkValidValue(second);
    NANO_OF_SECOND.checkValidValue(nanoOfSecond);
    return create(hour, minute, second, nanoOfSecond);
  }

  public static TLocalTime ofInstant(TInstant instant, TZoneId zone) {

    TObjects.requireNonNull(instant, "instant");
    TObjects.requireNonNull(zone, "zone");
    TZoneOffset offset = zone.getRules().getOffset(instant);
    long localSecond = instant.getEpochSecond() + offset.getTotalSeconds();
    int secsOfDay = Math.floorMod(localSecond, SECONDS_PER_DAY);
    return ofNanoOfDay(secsOfDay * NANOS_PER_SECOND + instant.getNano());
  }

  public static TLocalTime ofSecondOfDay(long secondOfDay) {

    SECOND_OF_DAY.checkValidValue(secondOfDay);
    int hours = (int) (secondOfDay / SECONDS_PER_HOUR);
    secondOfDay -= hours * SECONDS_PER_HOUR;
    int minutes = (int) (secondOfDay / SECONDS_PER_MINUTE);
    secondOfDay -= minutes * SECONDS_PER_MINUTE;
    return create(hours, minutes, (int) secondOfDay, 0);
  }

  public static TLocalTime ofNanoOfDay(long nanoOfDay) {

    NANO_OF_DAY.checkValidValue(nanoOfDay);
    int hours = (int) (nanoOfDay / NANOS_PER_HOUR);
    nanoOfDay -= hours * NANOS_PER_HOUR;
    int minutes = (int) (nanoOfDay / NANOS_PER_MINUTE);
    nanoOfDay -= minutes * NANOS_PER_MINUTE;
    int seconds = (int) (nanoOfDay / NANOS_PER_SECOND);
    nanoOfDay -= seconds * NANOS_PER_SECOND;
    return create(hours, minutes, seconds, (int) nanoOfDay);
  }

  public static TLocalTime from(TTemporalAccessor temporal) {

    TObjects.requireNonNull(temporal, "temporal");
    TLocalTime time = temporal.query(TTemporalQueries.localTime());
    if (time == null) {
      throw new TDateTimeException("Unable to obtain LocalTime from TemporalAccessor: " + temporal + " of type "
          + temporal.getClass().getName());
    }
    return time;
  }

  public static TLocalTime parse(CharSequence text) {

    return parse(text, TDateTimeFormatter.ISO_LOCAL_TIME);
  }

  public static TLocalTime parse(CharSequence text, TDateTimeFormatter formatter) {

    TObjects.requireNonNull(formatter, "formatter");
    return formatter.parse(text, TLocalTime::from);
  }

  private static TLocalTime create(int hour, int minute, int second, int nanoOfSecond) {

    if ((minute | second | nanoOfSecond) == 0) {
      return HOURS[hour];
    }
    return new TLocalTime(hour, minute, second, nanoOfSecond);
  }
}
