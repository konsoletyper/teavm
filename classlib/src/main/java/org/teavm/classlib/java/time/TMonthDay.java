package org.teavm.classlib.java.time;

import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;

import java.util.Objects;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TComparable;
import org.teavm.classlib.java.lang.TMath;
import org.teavm.classlib.java.lang.TStringBuilder;
import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.temporal.TValueRange;
import org.teavm.classlib.java.util.TObjects;

public final class TMonthDay implements TTemporalAccessor, TTemporalAdjuster, TComparable<TMonthDay>, TSerializable {

  private static final TDateTimeFormatter PARSER = new TDateTimeFormatterBuilder().appendLiteral("--")
      .appendValue(MONTH_OF_YEAR, 2).appendLiteral('-').appendValue(DAY_OF_MONTH, 2).toFormatter();

  private final int month;

  private final int day;

  public static TMonthDay now() {

    return now(TClock.systemDefaultZone());
  }

  public static TMonthDay now(TZoneId zone) {

    return now(TClock.system(zone));
  }

  public static TMonthDay now(TClock clock) {

    final TLocalDate now = TLocalDate.now(clock);
    return TMonthDay.of(now.getMonth(), now.getDayOfMonth());
  }

  public static TMonthDay of(TMonth month, int dayOfMonth) {

    TObjects.requireNonNull(month, "month");
    DAY_OF_MONTH.checkValidValue(dayOfMonth);
    if (dayOfMonth > month.maxLength()) {
      throw new TDateTimeException(
          "Illegal value for DayOfMonth field, value " + dayOfMonth + " is not valid for month " + month.name());
    }
    return new TMonthDay(month.getValue(), dayOfMonth);
  }

  public static TMonthDay of(int month, int dayOfMonth) {

    return of(TMonth.of(month), dayOfMonth);
  }

  public static TMonthDay from(TTemporalAccessor temporal) {

    if (temporal instanceof TMonthDay) {
      return (TMonthDay) temporal;
    }
    try {
      if (TIsoChronology.INSTANCE.equals(TChronology.from(temporal)) == false) {
        temporal = TLocalDate.from(temporal);
      }
      return of(temporal.get(MONTH_OF_YEAR), temporal.get(DAY_OF_MONTH));
    } catch (TDateTimeException ex) {
      throw new TDateTimeException(
          "Unable to obtain MonthDay from TemporalAccessor: " + temporal + " of type " + temporal.getClass().getName(),
          ex);
    }
  }

  public static TMonthDay parse(CharSequence text) {

    return parse(text, PARSER);
  }

  public static TMonthDay parse(CharSequence text, TDateTimeFormatter formatter) {

    TObjects.requireNonNull(formatter, "formatter");
    return formatter.parse(text, TMonthDay::from);
  }

  private TMonthDay(int month, int dayOfMonth) {

    this.month = month;
    this.day = dayOfMonth;
  }

  @Override
  public boolean isSupported(TTemporalField field) {

    if (field instanceof TChronoField) {
      return field == MONTH_OF_YEAR || field == DAY_OF_MONTH;
    }
    return field != null && field.isSupportedBy(this);
  }

  @Override
  public TValueRange range(TTemporalField field) {

    if (field == MONTH_OF_YEAR) {
      return field.range();
    } else if (field == DAY_OF_MONTH) {
      return TValueRange.of(1, getMonth().minLength(), getMonth().maxLength());
    }
    return TTemporalAccessor.super.range(field);
  }

  @Override
  public int get(TTemporalField field) {

    return range(field).checkValidIntValue(getLong(field), field);
  }

  @Override
  public long getLong(TTemporalField field) {

    if (field instanceof TChronoField) {
      switch ((TChronoField) field) {
        case DAY_OF_MONTH:
          return this.day;
        case MONTH_OF_YEAR:
          return this.month;
      }
      throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
    }
    return field.getFrom(this);
  }

  public int getMonthValue() {

    return this.month;
  }

  public TMonth getMonth() {

    return TMonth.of(this.month);
  }

  public int getDayOfMonth() {

    return this.day;
  }

  public boolean isValidYear(int year) {

    return (this.day == 29 && this.month == 2 && TYear.isLeap(year) == false) == false;
  }

  public TMonthDay withMonth(int month) {

    return with(TMonth.of(month));
  }

  public TMonthDay with(TMonth month) {

    Objects.requireNonNull(month, "month");
    if (month.getValue() == this.month) {
      return this;
    }
    int day = TMath.min(this.day, month.maxLength());
    return new TMonthDay(month.getValue(), day);
  }

  public TMonthDay withDayOfMonth(int dayOfMonth) {

    if (dayOfMonth == this.day) {
      return this;
    }
    return of(this.month, dayOfMonth);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <R> R query(TTemporalQuery<R> query) {

    if (query == TTemporalQueries.chronology()) {
      return (R) TIsoChronology.INSTANCE;
    }
    return TTemporalAccessor.super.query(query);
  }

  @Override
  public TTemporal adjustInto(TTemporal temporal) {

    if (TChronology.from(temporal).equals(TIsoChronology.INSTANCE) == false) {
      throw new TDateTimeException("Adjustment only supported on ISO date-time");
    }
    temporal = temporal.with(MONTH_OF_YEAR, this.month);
    return temporal.with(DAY_OF_MONTH, Math.min(temporal.range(DAY_OF_MONTH).getMaximum(), this.day));
  }

  public String format(TDateTimeFormatter formatter) {

    TObjects.requireNonNull(formatter, "formatter");
    return formatter.format(this);
  }

  public TLocalDate atYear(int year) {

    return TLocalDate.of(year, this.month, isValidYear(year) ? this.day : 28);
  }

  @Override
  public int compareTo(TMonthDay other) {

    int cmp = (this.month - other.month);
    if (cmp == 0) {
      cmp = (this.day - other.day);
    }
    return cmp;
  }

  public boolean isAfter(TMonthDay other) {

    return compareTo(other) > 0;
  }

  public boolean isBefore(TMonthDay other) {

    return compareTo(other) < 0;
  }

  @Override
  public boolean equals(Object obj) {

    if (this == obj) {
      return true;
    }
    if (obj instanceof TMonthDay) {
      TMonthDay other = (TMonthDay) obj;
      return this.month == other.month && this.day == other.day;
    }
    return false;
  }

  @Override
  public int hashCode() {

    return (this.month << 6) + this.day;
  }

  @Override
  public String toString() {

    return new TStringBuilder(10).append("--").append(this.month < 10 ? "0" : "").append(this.month)
        .append(this.day < 10 ? "-0" : "-").append(this.day).toString();
  }

}
