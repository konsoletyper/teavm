package org.teavm.classlib.java.time.chrono;

import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.FOREVER;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.NANOS;

import org.teavm.classlib.java.lang.TComparable;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;
import org.teavm.classlib.java.util.TObjects;

public interface TChronoLocalDateTime<D extends TChronoLocalDate>
    extends TTemporal, TTemporalAdjuster, TComparable<TChronoLocalDateTime<?>> {

  default TChronology getChronology() {

    return toLocalDate().getChronology();
  }

  D toLocalDate();

  TLocalTime toLocalTime();

  @Override
  default boolean isSupported(TTemporalUnit unit) {

    if (unit instanceof TChronoUnit) {
      return unit != FOREVER;
    }
    return unit != null && unit.isSupportedBy(this);
  }

  @Override
  TChronoLocalDateTime<D> with(TTemporalAdjuster adjuster);

  @Override
  TChronoLocalDateTime<D> with(TTemporalField field, long newValue);

  @Override
  TChronoLocalDateTime<D> plus(TTemporalAmount amount);

  @Override
  TChronoLocalDateTime<D> plus(long amountToAdd, TTemporalUnit unit);

  @Override
  TChronoLocalDateTime<D> minus(TTemporalAmount amount);

  @Override
  TChronoLocalDateTime<D> minus(long amountToSubtract, TTemporalUnit unit);

  @SuppressWarnings("unchecked")
  @Override
  default <R> R query(TTemporalQuery<R> query) {

    if (query == TTemporalQueries.zoneId() || query == TTemporalQueries.zone() || query == TTemporalQueries.offset()) {
      return null;
    } else if (query == TTemporalQueries.localTime()) {
      return (R) toLocalTime();
    } else if (query == TTemporalQueries.chronology()) {
      return (R) getChronology();
    } else if (query == TTemporalQueries.precision()) {
      return (R) NANOS;
    }
    return query.queryFrom(this);
  }

  @Override
  default TTemporal adjustInto(TTemporal temporal) {

    return temporal.with(EPOCH_DAY, toLocalDate().toEpochDay()).with(NANO_OF_DAY, toLocalTime().toNanoOfDay());
  }

  default String format(TDateTimeFormatter formatter) {

    TObjects.requireNonNull(formatter, "formatter");
    return formatter.format(this);
  }

  TChronoZonedDateTime<D> atZone(TZoneId zone);

  default TInstant toInstant(TZoneOffset offset) {

    return TInstant.ofEpochSecond(toEpochSecond(offset), toLocalTime().getNano());
  }

  default long toEpochSecond(TZoneOffset offset) {

    TObjects.requireNonNull(offset, "offset");
    long epochDay = toLocalDate().toEpochDay();
    long secs = epochDay * 86400 + toLocalTime().toSecondOfDay();
    secs -= offset.getTotalSeconds();
    return secs;
  }

  @Override
  default int compareTo(TChronoLocalDateTime<?> other) {

    int cmp = toLocalDate().compareTo(other.toLocalDate());
    if (cmp == 0) {
      cmp = toLocalTime().compareTo(other.toLocalTime());
      if (cmp == 0) {
        cmp = getChronology().compareTo(other.getChronology());
      }
    }
    return cmp;
  }

  default boolean isAfter(TChronoLocalDateTime<?> other) {

    long thisEpDay = this.toLocalDate().toEpochDay();
    long otherEpDay = other.toLocalDate().toEpochDay();
    return thisEpDay > otherEpDay
        || (thisEpDay == otherEpDay && this.toLocalTime().toNanoOfDay() > other.toLocalTime().toNanoOfDay());
  }

  default boolean isBefore(TChronoLocalDateTime<?> other) {

    long thisEpDay = this.toLocalDate().toEpochDay();
    long otherEpDay = other.toLocalDate().toEpochDay();
    return thisEpDay < otherEpDay
        || (thisEpDay == otherEpDay && this.toLocalTime().toNanoOfDay() < other.toLocalTime().toNanoOfDay());
  }

  default boolean isEqual(TChronoLocalDateTime<?> other) {

    // Do the time check first, it is cheaper than computing EPOCH day.
    return this.toLocalTime().toNanoOfDay() == other.toLocalTime().toNanoOfDay()
        && this.toLocalDate().toEpochDay() == other.toLocalDate().toEpochDay();
  }

}
