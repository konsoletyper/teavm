package org.teavm.classlib.java.time.temporal;

import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MONTHS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.YEARS;

import org.teavm.classlib.java.time.TDayOfWeek;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.util.TObjects;
import org.teavm.classlib.java.util.function.TUnaryOperator;

public final class TTemporalAdjusters {
  private TTemporalAdjusters() {

  }

  public static TTemporalAdjuster ofDateAdjuster(TUnaryOperator<TLocalDate> dateBasedAdjuster) {

    TObjects.requireNonNull(dateBasedAdjuster, "dateBasedAdjuster");
    return (temporal) -> {
      TLocalDate input = TLocalDate.from(temporal);
      TLocalDate output = dateBasedAdjuster.apply(input);
      return temporal.with(output);
    };
  }

  public static TTemporalAdjuster firstDayOfMonth() {

    return (temporal) -> temporal.with(DAY_OF_MONTH, 1);
  }

  public static TTemporalAdjuster lastDayOfMonth() {

    return (temporal) -> temporal.with(DAY_OF_MONTH, temporal.range(DAY_OF_MONTH).getMaximum());
  }

  public static TTemporalAdjuster firstDayOfNextMonth() {

    return (temporal) -> temporal.with(DAY_OF_MONTH, 1).plus(1, MONTHS);
  }

  public static TTemporalAdjuster firstDayOfYear() {

    return (temporal) -> temporal.with(DAY_OF_YEAR, 1);
  }

  public static TTemporalAdjuster lastDayOfYear() {

    return (temporal) -> temporal.with(DAY_OF_YEAR, temporal.range(DAY_OF_YEAR).getMaximum());
  }

  public static TTemporalAdjuster firstDayOfNextYear() {

    return (temporal) -> temporal.with(DAY_OF_YEAR, 1).plus(1, YEARS);
  }

  public static TTemporalAdjuster firstInMonth(TDayOfWeek dayOfWeek) {

    return TTemporalAdjusters.dayOfWeekInMonth(1, dayOfWeek);
  }

  public static TTemporalAdjuster lastInMonth(TDayOfWeek dayOfWeek) {

    return TTemporalAdjusters.dayOfWeekInMonth(-1, dayOfWeek);
  }

  public static TTemporalAdjuster dayOfWeekInMonth(int ordinal, TDayOfWeek dayOfWeek) {

    TObjects.requireNonNull(dayOfWeek, "dayOfWeek");
    int dowValue = dayOfWeek.getValue();
    if (ordinal >= 0) {
      return (temporal) -> {
        TTemporal temp = temporal.with(DAY_OF_MONTH, 1);
        int curDow = temp.get(DAY_OF_WEEK);
        int dowDiff = (dowValue - curDow + 7) % 7;
        dowDiff += (ordinal - 1L) * 7L;
        return temp.plus(dowDiff, DAYS);
      };
    } else {
      return (temporal) -> {
        TTemporal temp = temporal.with(DAY_OF_MONTH, temporal.range(DAY_OF_MONTH).getMaximum());
        int curDow = temp.get(DAY_OF_WEEK);
        int daysDiff = dowValue - curDow;
        daysDiff = (daysDiff == 0 ? 0 : (daysDiff > 0 ? daysDiff - 7 : daysDiff));
        daysDiff -= (-ordinal - 1L) * 7L;
        return temp.plus(daysDiff, DAYS);
      };
    }
  }

  public static TTemporalAdjuster next(TDayOfWeek dayOfWeek) {

    int dowValue = dayOfWeek.getValue();
    return (temporal) -> {
      int calDow = temporal.get(DAY_OF_WEEK);
      int daysDiff = calDow - dowValue;
      return temporal.plus(daysDiff >= 0 ? 7 - daysDiff : -daysDiff, DAYS);
    };
  }

  public static TTemporalAdjuster nextOrSame(TDayOfWeek dayOfWeek) {

    int dowValue = dayOfWeek.getValue();
    return (temporal) -> {
      int calDow = temporal.get(DAY_OF_WEEK);
      if (calDow == dowValue) {
        return temporal;
      }
      int daysDiff = calDow - dowValue;
      return temporal.plus(daysDiff >= 0 ? 7 - daysDiff : -daysDiff, DAYS);
    };
  }

  public static TTemporalAdjuster previous(TDayOfWeek dayOfWeek) {

    int dowValue = dayOfWeek.getValue();
    return (temporal) -> {
      int calDow = temporal.get(DAY_OF_WEEK);
      int daysDiff = dowValue - calDow;
      return temporal.minus(daysDiff >= 0 ? 7 - daysDiff : -daysDiff, DAYS);
    };
  }

  public static TTemporalAdjuster previousOrSame(TDayOfWeek dayOfWeek) {

    int dowValue = dayOfWeek.getValue();
    return (temporal) -> {
      int calDow = temporal.get(DAY_OF_WEEK);
      if (calDow == dowValue) {
        return temporal;
      }
      int daysDiff = dowValue - calDow;
      return temporal.minus(daysDiff >= 0 ? 7 - daysDiff : -daysDiff, DAYS);
    };
  }

}
