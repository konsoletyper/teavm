package org.teavm.classlib.java.time.chrono;

import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.ERA;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;

import org.teavm.classlib.java.lang.TComparable;
import org.teavm.classlib.java.time.TLocalTime;
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
import org.teavm.classlib.java.util.TObjects;

public interface TChronoLocalDate extends TTemporal, TTemporalAdjuster, TComparable<TChronoLocalDate> {
  TChronology getChronology();

  default TEra getEra() {

    return getChronology().eraOf(get(ERA));
  }

  default boolean isLeapYear() {

    return getChronology().isLeapYear(getLong(YEAR));
  }

  int lengthOfMonth();

  default int lengthOfYear() {

    return (isLeapYear() ? 366 : 365);
  }

  @Override
  default boolean isSupported(TTemporalField field) {

    if (field instanceof TChronoField) {
      return field.isDateBased();
    }
    return field != null && field.isSupportedBy(this);
  }

  @Override
  default boolean isSupported(TTemporalUnit unit) {

    if (unit instanceof TChronoUnit) {
      return unit.isDateBased();
    }
    return unit != null && unit.isSupportedBy(this);
  }

  TChronoLocalDateTime<?> atTime(TLocalTime localTime);

  @Override
  TChronoLocalDate with(TTemporalAdjuster adjuster);

  @Override
  TChronoLocalDate with(TTemporalField field, long newValue);

  @Override
  TChronoLocalDate plus(TTemporalAmount amount);

  @Override
  TChronoLocalDate plus(long amountToAdd, TTemporalUnit unit);

  @Override
  TChronoLocalDate minus(TTemporalAmount amount);

  @Override
  TChronoLocalDate minus(long amountToSubtract, TTemporalUnit unit);

  @Override
  @SuppressWarnings("unchecked")
  default <R> R query(TTemporalQuery<R> query) {

    if (query == TTemporalQueries.zoneId() || query == TTemporalQueries.zone() || query == TTemporalQueries.offset()) {
      return null;
    } else if (query == TTemporalQueries.localTime()) {
      return null;
    } else if (query == TTemporalQueries.chronology()) {
      return (R) getChronology();
    } else if (query == TTemporalQueries.precision()) {
      return (R) DAYS;
    }
    return query.queryFrom(this);
  }

  @Override
  default TTemporal adjustInto(TTemporal temporal) {

    return temporal.with(EPOCH_DAY, toEpochDay());
  }

  TChronoPeriod until(TChronoLocalDate endDateExclusive);

  default String format(TDateTimeFormatter formatter) {

    TObjects.requireNonNull(formatter, "formatter");
    return formatter.format(this);
  }

  // TChronoLocalDateTime<?> atTime(TLocalTime localTime);
  default long toEpochDay() {

    return getLong(EPOCH_DAY);
  }

  @Override
  default int compareTo(TChronoLocalDate other) {

    int cmp = Long.compare(toEpochDay(), other.toEpochDay());
    if (cmp == 0) {
      cmp = getChronology().compareTo(other.getChronology());
    }
    return cmp;
  }

  default boolean isAfter(TChronoLocalDate other) {

    return toEpochDay() > other.toEpochDay();
  }

  default boolean isBefore(TChronoLocalDate other) {

    return toEpochDay() < other.toEpochDay();
  }

  default boolean isEqual(TChronoLocalDate other) {

    return toEpochDay() == other.toEpochDay();
  }

}
