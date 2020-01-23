package org.teavm.classlib.java.time.chrono;

import static org.teavm.classlib.java.time.temporal.TChronoField.INSTANT_SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoField.OFFSET_SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.FOREVER;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.NANOS;

import org.teavm.classlib.java.lang.TComparable;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.temporal.TValueRange;
import org.teavm.classlib.java.util.TObjects;

public interface TChronoZonedDateTime<D extends TChronoLocalDate>
    extends TTemporal, TComparable<TChronoZonedDateTime<?>> {

  @Override
  default TValueRange range(TTemporalField field) {

    if (field instanceof TChronoField) {
      if (field == INSTANT_SECONDS || field == OFFSET_SECONDS) {
        return field.range();
      }
      return toLocalDateTime().range(field);
    }
    return field.rangeRefinedBy(this);
  }

  @Override
  default int get(TTemporalField field) {

    if (field instanceof TChronoField) {
      switch ((TChronoField) field) {
        case INSTANT_SECONDS:
          throw new TUnsupportedTemporalTypeException(
              "Invalid field 'InstantSeconds' for get() method, use getLong() instead");
        case OFFSET_SECONDS:
          return getOffset().getTotalSeconds();
      }
      return toLocalDateTime().get(field);
    }
    return TTemporal.super.get(field);
  }

  @Override
  default long getLong(TTemporalField field) {

    if (field instanceof TChronoField) {
      switch ((TChronoField) field) {
        case INSTANT_SECONDS:
          return toEpochSecond();
        case OFFSET_SECONDS:
          return getOffset().getTotalSeconds();
      }
      return toLocalDateTime().getLong(field);
    }
    return field.getFrom(this);
  }

  default D toLocalDate() {

    return toLocalDateTime().toLocalDate();
  }

  default TLocalTime toLocalTime() {

    return toLocalDateTime().toLocalTime();
  }

  TChronoLocalDateTime<D> toLocalDateTime();

  default TChronology getChronology() {

    return toLocalDate().getChronology();
  }

  TZoneOffset getOffset();

  TZoneId getZone();

  TChronoZonedDateTime<D> withEarlierOffsetAtOverlap();

  TChronoZonedDateTime<D> withLaterOffsetAtOverlap();

  TChronoZonedDateTime<D> withZoneSameLocal(TZoneId zone);

  TChronoZonedDateTime<D> withZoneSameInstant(TZoneId zone);

  @Override
  default boolean isSupported(TTemporalUnit unit) {

    if (unit instanceof TChronoUnit) {
      return unit != FOREVER;
    }
    return unit != null && unit.isSupportedBy(this);
  }

  @Override
  TChronoZonedDateTime<D> with(TTemporalAdjuster adjuster);

  @Override
  TChronoZonedDateTime<D> with(TTemporalField field, long newValue);

  @Override
  TChronoZonedDateTime<D> plus(TTemporalAmount amount);

  @Override
  TChronoZonedDateTime<D> plus(long amountToAdd, TTemporalUnit unit);

  @Override
  TChronoZonedDateTime<D> minus(TTemporalAmount amount);

  @Override
  TChronoZonedDateTime<D> minus(long amountToSubtract, TTemporalUnit unit);

  @SuppressWarnings("unchecked")
  @Override
  default <R> R query(TTemporalQuery<R> query) {

    if (query == TTemporalQueries.zone() || query == TTemporalQueries.zoneId()) {
      return (R) getZone();
    } else if (query == TTemporalQueries.offset()) {
      return (R) getOffset();
    } else if (query == TTemporalQueries.localTime()) {
      return (R) toLocalTime();
    } else if (query == TTemporalQueries.chronology()) {
      return (R) getChronology();
    } else if (query == TTemporalQueries.precision()) {
      return (R) NANOS;
    }
    return query.queryFrom(this);
  }

  default String format(TDateTimeFormatter formatter) {

    TObjects.requireNonNull(formatter, "formatter");
    return formatter.format(this);
  }

  default TInstant toInstant() {

    return TInstant.ofEpochSecond(toEpochSecond(), toLocalTime().getNano());
  }

  default long toEpochSecond() {

    long epochDay = toLocalDate().toEpochDay();
    long secs = epochDay * 86400 + toLocalTime().toSecondOfDay();
    secs -= getOffset().getTotalSeconds();
    return secs;
  }

  @Override
  default int compareTo(TChronoZonedDateTime<?> other) {

    int cmp = Long.compare(toEpochSecond(), other.toEpochSecond());
    if (cmp == 0) {
      cmp = toLocalTime().getNano() - other.toLocalTime().getNano();
      if (cmp == 0) {
        cmp = toLocalDateTime().compareTo(other.toLocalDateTime());
        if (cmp == 0) {
          cmp = getZone().getId().compareTo(other.getZone().getId());
          if (cmp == 0) {
            cmp = getChronology().compareTo(other.getChronology());
          }
        }
      }
    }
    return cmp;
  }

  default boolean isBefore(TChronoZonedDateTime<?> other) {

    long thisEpochSec = toEpochSecond();
    long otherEpochSec = other.toEpochSecond();
    return thisEpochSec < otherEpochSec
        || (thisEpochSec == otherEpochSec && toLocalTime().getNano() < other.toLocalTime().getNano());
  }

  default boolean isAfter(TChronoZonedDateTime<?> other) {

    long thisEpochSec = toEpochSecond();
    long otherEpochSec = other.toEpochSecond();
    return thisEpochSec > otherEpochSec
        || (thisEpochSec == otherEpochSec && toLocalTime().getNano() > other.toLocalTime().getNano());
  }

  default boolean isEqual(TChronoZonedDateTime<?> other) {

    return toEpochSecond() == other.toEpochSecond() && toLocalTime().getNano() == other.toLocalTime().getNano();
  }

}
