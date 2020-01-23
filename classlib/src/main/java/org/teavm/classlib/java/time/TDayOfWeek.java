package org.teavm.classlib.java.time;

import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;

import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.temporal.TValueRange;

public enum TDayOfWeek implements TTemporalAccessor, TTemporalAdjuster {
  MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;

  private static final TDayOfWeek[] ENUMS = TDayOfWeek.values();

  public int getValue() {

    return ordinal() + 1;
  }

  // public String getDisplayName(TTextStyle style, TLocale locale) {
  //
  // return new TDateTimeFormatterBuilder().appendText(DAY_OF_WEEK, style).toFormatter(locale).format(this);
  // }

  @Override
  public boolean isSupported(TTemporalField field) {

    if (field instanceof TChronoField) {
      return field == DAY_OF_WEEK;
    }
    return field != null && field.isSupportedBy(this);
  }

  @Override
  public TValueRange range(TTemporalField field) {

    if (field == DAY_OF_WEEK) {
      return field.range();
    }
    return TTemporalAccessor.super.range(field);
  }

  @Override
  public int get(TTemporalField field) {

    if (field == DAY_OF_WEEK) {
      return getValue();
    }
    return TTemporalAccessor.super.get(field);
  }

  @Override
  public long getLong(TTemporalField field) {

    if (field == DAY_OF_WEEK) {
      return getValue();
    } else if (field instanceof TChronoField) {
      throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
    }
    return field.getFrom(this);
  }

  public TDayOfWeek plus(long days) {

    int amount = (int) (days % 7);
    return ENUMS[(ordinal() + (amount + 7)) % 7];
  }

  public TDayOfWeek minus(long days) {

    return plus(-(days % 7));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <R> R query(TTemporalQuery<R> query) {

    if (query == TTemporalQueries.precision()) {
      return (R) DAYS;
    }
    return TTemporalAccessor.super.query(query);
  }

  @Override
  public TTemporal adjustInto(TTemporal temporal) {

    return temporal.with(DAY_OF_WEEK, getValue());
  }

  public static TDayOfWeek of(int dayOfWeek) {

    if (dayOfWeek < 1 || dayOfWeek > 7) {
      throw new TDateTimeException("Invalid value for DayOfWeek: " + dayOfWeek);
    }
    return ENUMS[dayOfWeek - 1];
  }

  public static TDayOfWeek from(TTemporalAccessor temporal) {

    if (temporal instanceof TDayOfWeek) {
      return (TDayOfWeek) temporal;
    }
    try {
      return of(temporal.get(DAY_OF_WEEK));
    } catch (TDateTimeException ex) {
      throw new TDateTimeException(
          "Unable to obtain DayOfWeek from TemporalAccessor: " + temporal + " of type " + temporal.getClass().getName(),
          ex);
    }
  }

}
