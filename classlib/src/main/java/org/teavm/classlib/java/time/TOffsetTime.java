package org.teavm.classlib.java.time;

import static org.teavm.classlib.java.time.TLocalTime.NANOS_PER_HOUR;
import static org.teavm.classlib.java.time.TLocalTime.NANOS_PER_MINUTE;
import static org.teavm.classlib.java.time.TLocalTime.NANOS_PER_SECOND;
import static org.teavm.classlib.java.time.TLocalTime.SECONDS_PER_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.OFFSET_SECONDS;
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
import org.teavm.classlib.java.time.zone.TZoneRules;
import org.teavm.classlib.java.util.TObjects;

public final class TOffsetTime implements TTemporal, TTemporalAdjuster, TComparable<TOffsetTime>, TSerializable {

  public static final TOffsetTime MIN = TLocalTime.MIN.atOffset(TZoneOffset.MAX);

  public static final TOffsetTime MAX = TLocalTime.MAX.atOffset(TZoneOffset.MIN);

  private final TLocalTime time;

  private final TZoneOffset offset;

  public static TOffsetTime now() {

    return now(TClock.systemDefaultZone());
  }

  public static TOffsetTime now(TZoneId zone) {

    return now(TClock.system(zone));
  }

  public static TOffsetTime now(TClock clock) {

    TObjects.requireNonNull(clock, "clock");
    final TInstant now = clock.instant();
    return ofInstant(now, clock.getZone().getRules().getOffset(now));
  }

  public static TOffsetTime of(TLocalTime time, TZoneOffset offset) {

    return new TOffsetTime(time, offset);
  }

  public static TOffsetTime of(int hour, int minute, int second, int nanoOfSecond, TZoneOffset offset) {

    return new TOffsetTime(TLocalTime.of(hour, minute, second, nanoOfSecond), offset);
  }

  public static TOffsetTime ofInstant(TInstant instant, TZoneId zone) {

    TObjects.requireNonNull(instant, "instant");
    TObjects.requireNonNull(zone, "zone");
    TZoneRules rules = zone.getRules();
    TZoneOffset offset = rules.getOffset(instant);
    long localSecond = instant.getEpochSecond() + offset.getTotalSeconds(); // overflow caught later
    int secsOfDay = Math.floorMod(localSecond, SECONDS_PER_DAY);
    TLocalTime time = TLocalTime.ofNanoOfDay(secsOfDay * NANOS_PER_SECOND + instant.getNano());
    return new TOffsetTime(time, offset);
  }

  public static TOffsetTime from(TTemporalAccessor temporal) {

    if (temporal instanceof TOffsetTime) {
      return (TOffsetTime) temporal;
    }
    try {
      TLocalTime time = TLocalTime.from(temporal);
      TZoneOffset offset = TZoneOffset.from(temporal);
      return new TOffsetTime(time, offset);
    } catch (TDateTimeException ex) {
      throw new TDateTimeException("Unable to obtain OffsetTime from TemporalAccessor: " + temporal + " of type "
          + temporal.getClass().getName(), ex);
    }
  }

  public static TOffsetTime parse(CharSequence text) {

    return parse(text, TDateTimeFormatter.ISO_OFFSET_TIME);
  }

  public static TOffsetTime parse(CharSequence text, TDateTimeFormatter formatter) {

    TObjects.requireNonNull(formatter, "formatter");
    return formatter.parse(text, TOffsetTime::from);
  }

  private TOffsetTime(TLocalTime time, TZoneOffset offset) {

    this.time = TObjects.requireNonNull(time, "time");
    this.offset = TObjects.requireNonNull(offset, "offset");
  }

  private TOffsetTime with(TLocalTime time, TZoneOffset offset) {

    if (this.time == time && this.offset.equals(offset)) {
      return this;
    }
    return new TOffsetTime(time, offset);
  }

  @Override
  public boolean isSupported(TTemporalField field) {

    if (field instanceof TChronoField) {
      return field.isTimeBased() || field == OFFSET_SECONDS;
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

    if (field instanceof TChronoField) {
      if (field == OFFSET_SECONDS) {
        return field.range();
      }
      return this.time.range(field);
    }
    return field.rangeRefinedBy(this);
  }

  @Override
  public int get(TTemporalField field) {

    return TTemporal.super.get(field);
  }

  @Override
  public long getLong(TTemporalField field) {

    if (field instanceof TChronoField) {
      if (field == OFFSET_SECONDS) {
        return this.offset.getTotalSeconds();
      }
      return this.time.getLong(field);
    }
    return field.getFrom(this);
  }

  public TZoneOffset getOffset() {

    return this.offset;
  }

  public TOffsetTime withOffsetSameLocal(TZoneOffset offset) {

    return offset != null && offset.equals(this.offset) ? this : new TOffsetTime(this.time, offset);
  }

  public TOffsetTime withOffsetSameInstant(TZoneOffset offset) {

    if (offset.equals(this.offset)) {
      return this;
    }
    int difference = offset.getTotalSeconds() - this.offset.getTotalSeconds();
    TLocalTime adjusted = this.time.plusSeconds(difference);
    return new TOffsetTime(adjusted, offset);
  }

  public TLocalTime toLocalTime() {

    return this.time;
  }

  public int getHour() {

    return this.time.getHour();
  }

  public int getMinute() {

    return this.time.getMinute();
  }

  public int getSecond() {

    return this.time.getSecond();
  }

  public int getNano() {

    return this.time.getNano();
  }

  @Override
  public TOffsetTime with(TTemporalAdjuster adjuster) {

    if (adjuster instanceof TLocalTime) {
      return with((TLocalTime) adjuster, this.offset);
    } else if (adjuster instanceof TZoneOffset) {
      return with(this.time, (TZoneOffset) adjuster);
    } else if (adjuster instanceof TOffsetTime) {
      return (TOffsetTime) adjuster;
    }
    return (TOffsetTime) adjuster.adjustInto(this);
  }

  @Override
  public TOffsetTime with(TTemporalField field, long newValue) {

    if (field instanceof TChronoField) {
      if (field == OFFSET_SECONDS) {
        TChronoField f = (TChronoField) field;
        return with(this.time, TZoneOffset.ofTotalSeconds(f.checkValidIntValue(newValue)));
      }
      return with(this.time.with(field, newValue), this.offset);
    }
    return field.adjustInto(this, newValue);
  }

  public TOffsetTime withHour(int hour) {

    return with(this.time.withHour(hour), this.offset);
  }

  public TOffsetTime withMinute(int minute) {

    return with(this.time.withMinute(minute), this.offset);
  }

  public TOffsetTime withSecond(int second) {

    return with(this.time.withSecond(second), this.offset);
  }

  public TOffsetTime withNano(int nanoOfSecond) {

    return with(this.time.withNano(nanoOfSecond), this.offset);
  }

  public TOffsetTime truncatedTo(TTemporalUnit unit) {

    return with(this.time.truncatedTo(unit), this.offset);
  }

  @Override
  public TOffsetTime plus(TTemporalAmount amountToAdd) {

    return (TOffsetTime) amountToAdd.addTo(this);
  }

  @Override
  public TOffsetTime plus(long amountToAdd, TTemporalUnit unit) {

    if (unit instanceof TChronoUnit) {
      return with(this.time.plus(amountToAdd, unit), this.offset);
    }
    return unit.addTo(this, amountToAdd);
  }

  public TOffsetTime plusHours(long hours) {

    return with(this.time.plusHours(hours), this.offset);
  }

  public TOffsetTime plusMinutes(long minutes) {

    return with(this.time.plusMinutes(minutes), this.offset);
  }

  public TOffsetTime plusSeconds(long seconds) {

    return with(this.time.plusSeconds(seconds), this.offset);
  }

  public TOffsetTime plusNanos(long nanos) {

    return with(this.time.plusNanos(nanos), this.offset);
  }

  @Override
  public TOffsetTime minus(TTemporalAmount amountToSubtract) {

    return (TOffsetTime) amountToSubtract.subtractFrom(this);
  }

  @Override
  public TOffsetTime minus(long amountToSubtract, TTemporalUnit unit) {

    return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit)
        : plus(-amountToSubtract, unit));
  }

  public TOffsetTime minusHours(long hours) {

    return with(this.time.minusHours(hours), this.offset);
  }

  public TOffsetTime minusMinutes(long minutes) {

    return with(this.time.minusMinutes(minutes), this.offset);
  }

  public TOffsetTime minusSeconds(long seconds) {

    return with(this.time.minusSeconds(seconds), this.offset);
  }

  public TOffsetTime minusNanos(long nanos) {

    return with(this.time.minusNanos(nanos), this.offset);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <R> R query(TTemporalQuery<R> query) {

    if (query == TTemporalQueries.offset() || query == TTemporalQueries.zone()) {
      return (R) this.offset;
    } else if (query == TTemporalQueries.zoneId() | query == TTemporalQueries.chronology()
        || query == TTemporalQueries.localDate()) {
      return null;
    } else if (query == TTemporalQueries.localTime()) {
      return (R) this.time;
    } else if (query == TTemporalQueries.precision()) {
      return (R) NANOS;
    }
    return query.queryFrom(this);
  }

  @Override
  public TTemporal adjustInto(TTemporal temporal) {

    return temporal.with(NANO_OF_DAY, this.time.toNanoOfDay()).with(OFFSET_SECONDS, this.offset.getTotalSeconds());
  }

  @Override
  public long until(TTemporal endExclusive, TTemporalUnit unit) {

    TOffsetTime end = TOffsetTime.from(endExclusive);
    if (unit instanceof TChronoUnit) {
      long nanosUntil = end.toEpochNano() - toEpochNano();
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

  public TOffsetDateTime atDate(TLocalDate date) {

    return TOffsetDateTime.of(date, this.time, this.offset);
  }

  private long toEpochNano() {

    long nod = this.time.toNanoOfDay();
    long offsetNanos = this.offset.getTotalSeconds() * NANOS_PER_SECOND;
    return nod - offsetNanos;
  }

  public long toEpochSecond(TLocalDate date) {

    TObjects.requireNonNull(date, "date");
    long epochDay = date.toEpochDay();
    long secs = epochDay * 86400 + this.time.toSecondOfDay();
    secs -= this.offset.getTotalSeconds();
    return secs;
  }

  @Override
  public int compareTo(TOffsetTime other) {

    if (this.offset.equals(other.offset)) {
      return this.time.compareTo(other.time);
    }
    int compare = Long.compare(toEpochNano(), other.toEpochNano());
    if (compare == 0) {
      compare = this.time.compareTo(other.time);
    }
    return compare;
  }

  public boolean isAfter(TOffsetTime other) {

    return toEpochNano() > other.toEpochNano();
  }

  public boolean isBefore(TOffsetTime other) {

    return toEpochNano() < other.toEpochNano();
  }

  public boolean isEqual(TOffsetTime other) {

    return toEpochNano() == other.toEpochNano();
  }

  @Override
  public boolean equals(Object obj) {

    if (this == obj) {
      return true;
    }
    if (obj instanceof TOffsetTime) {
      TOffsetTime other = (TOffsetTime) obj;
      return this.time.equals(other.time) && this.offset.equals(other.offset);
    }
    return false;
  }

  @Override
  public int hashCode() {

    return this.time.hashCode() ^ this.offset.hashCode();
  }

  @Override
  public String toString() {

    return this.time.toString() + this.offset.toString();
  }

}
