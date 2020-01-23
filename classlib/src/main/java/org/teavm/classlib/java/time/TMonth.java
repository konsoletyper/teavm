package org.teavm.classlib.java.time;

import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MONTHS;

import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder;
import org.teavm.classlib.java.time.format.TTextStyle;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.temporal.TValueRange;
import org.teavm.classlib.java.util.TLocale;

public enum TMonth implements TTemporalAccessor, TTemporalAdjuster {
  JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER;

  private static final TMonth[] ENUMS = TMonth.values();

  public int getValue() {

    return ordinal() + 1;
  }

  public String getDisplayName(TTextStyle style, TLocale locale) {

    return new TDateTimeFormatterBuilder().appendText(MONTH_OF_YEAR, style).toFormatter(locale).format(this);
  }

  @Override
  public boolean isSupported(TTemporalField field) {

    if (field instanceof TChronoField) {
      return field == MONTH_OF_YEAR;
    }
    return field != null && field.isSupportedBy(this);
  }

  @Override
  public TValueRange range(TTemporalField field) {

    if (field == MONTH_OF_YEAR) {
      return field.range();
    }
    return TTemporalAccessor.super.range(field);
  }

  @Override
  public int get(TTemporalField field) {

    if (field == MONTH_OF_YEAR) {
      return getValue();
    }
    return TTemporalAccessor.super.get(field);
  }

  @Override
  public long getLong(TTemporalField field) {

    if (field == MONTH_OF_YEAR) {
      return getValue();
    } else if (field instanceof TChronoField) {
      throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
    }
    return field.getFrom(this);
  }

  public TMonth plus(long months) {

    int amount = (int) (months % 12);
    return ENUMS[(ordinal() + (amount + 12)) % 12];
  }

  public TMonth minus(long months) {

    return plus(-(months % 12));
  }

  public int length(boolean leapYear) {

    switch (this) {
      case FEBRUARY:
        return (leapYear ? 29 : 28);
      case APRIL:
      case JUNE:
      case SEPTEMBER:
      case NOVEMBER:
        return 30;
      default:
        return 31;
    }
  }

  public int minLength() {

    switch (this) {
      case FEBRUARY:
        return 28;
      case APRIL:
      case JUNE:
      case SEPTEMBER:
      case NOVEMBER:
        return 30;
      default:
        return 31;
    }
  }

  public int maxLength() {

    switch (this) {
      case FEBRUARY:
        return 29;
      case APRIL:
      case JUNE:
      case SEPTEMBER:
      case NOVEMBER:
        return 30;
      default:
        return 31;
    }
  }

  public int firstDayOfYear(boolean leapYear) {

    int leap = leapYear ? 1 : 0;
    switch (this) {
      case JANUARY:
        return 1;
      case FEBRUARY:
        return 32;
      case MARCH:
        return 60 + leap;
      case APRIL:
        return 91 + leap;
      case MAY:
        return 121 + leap;
      case JUNE:
        return 152 + leap;
      case JULY:
        return 182 + leap;
      case AUGUST:
        return 213 + leap;
      case SEPTEMBER:
        return 244 + leap;
      case OCTOBER:
        return 274 + leap;
      case NOVEMBER:
        return 305 + leap;
      case DECEMBER:
      default:
        return 335 + leap;
    }
  }

  public TMonth firstMonthOfQuarter() {

    return ENUMS[(ordinal() / 3) * 3];
  }

  @SuppressWarnings("unchecked")
  @Override
  public <R> R query(TTemporalQuery<R> query) {

    if (query == TTemporalQueries.chronology()) {
      return (R) TIsoChronology.INSTANCE;
    } else if (query == TTemporalQueries.precision()) {
      return (R) MONTHS;
    }
    return TTemporalAccessor.super.query(query);
  }

  @Override
  public TTemporal adjustInto(TTemporal temporal) {

    return temporal.with(MONTH_OF_YEAR, getValue());
  }

  public static TMonth of(int month) {

    if (month < 1 || month > 12) {
      throw new TDateTimeException("Invalid value for MonthOfYear: " + month);
    }
    return ENUMS[month - 1];
  }

  public static TMonth from(TTemporalAccessor temporal) {

    if (temporal instanceof TMonth) {
      return (TMonth) temporal;
    }
    try {
      if (TIsoChronology.INSTANCE.equals(TChronology.from(temporal)) == false) {
        temporal = TLocalDate.from(temporal);
      }
      return of(temporal.get(MONTH_OF_YEAR));
    } catch (TDateTimeException ex) {
      throw new TDateTimeException(
          "Unable to obtain Month from TemporalAccessor: " + temporal + " of type " + temporal.getClass().getName(),
          ex);
    }
  }
}
