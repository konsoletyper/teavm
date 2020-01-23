package org.teavm.classlib.java.time.chrono;

import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.HOUR_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.MINUTE_OF_HOUR;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.SECOND_OF_MINUTE;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;

import java.util.List;
import java.util.Map;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TClassCastException;
import org.teavm.classlib.java.time.TClock;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TMonth;
import org.teavm.classlib.java.time.TPeriod;
import org.teavm.classlib.java.time.TYear;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.TZonedDateTime;
import org.teavm.classlib.java.time.format.TResolverStyle;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TValueRange;
import org.teavm.classlib.java.util.TObjects;

public class TIsoChronology extends TAbstractChronology implements TSerializable {

  public static final TIsoChronology INSTANCE = new TIsoChronology();

  private static final long DAYS_0000_TO_1970 = (146097 * 5L) - (30L * 365L + 7L);

  private TIsoChronology() {

  }

  @Override
  public String getId() {

    return "ISO";
  }

  @Override
  public String getCalendarType() {

    return "iso8601";
  }

  @Override
  public TLocalDate date(TEra era, int yearOfEra, int month, int dayOfMonth) {

    return date(prolepticYear(era, yearOfEra), month, dayOfMonth);
  }

  @Override
  public TLocalDate date(int prolepticYear, int month, int dayOfMonth) {

    return TLocalDate.of(prolepticYear, month, dayOfMonth);
  }

  @Override
  public TLocalDate dateYearDay(TEra era, int yearOfEra, int dayOfYear) {

    return dateYearDay(prolepticYear(era, yearOfEra), dayOfYear);
  }

  @Override
  public TLocalDate dateYearDay(int prolepticYear, int dayOfYear) {

    return TLocalDate.ofYearDay(prolepticYear, dayOfYear);
  }

  @Override
  public TLocalDate dateEpochDay(long epochDay) {

    return TLocalDate.ofEpochDay(epochDay);
  }

  @Override
  public TLocalDate date(TTemporalAccessor temporal) {

    return TLocalDate.from(temporal);
  }

  @Override
  public long epochSecond(int prolepticYear, int month, int dayOfMonth, int hour, int minute, int second,
      TZoneOffset zoneOffset) {

    YEAR.checkValidValue(prolepticYear);
    MONTH_OF_YEAR.checkValidValue(month);
    DAY_OF_MONTH.checkValidValue(dayOfMonth);
    HOUR_OF_DAY.checkValidValue(hour);
    MINUTE_OF_HOUR.checkValidValue(minute);
    SECOND_OF_MINUTE.checkValidValue(second);
    TObjects.requireNonNull(zoneOffset, "zoneOffset");
    if (dayOfMonth > 28) {
      int dom = numberOfDaysOfMonth(prolepticYear, month);
      if (dayOfMonth > dom) {
        if (dayOfMonth == 29) {
          throw new TDateTimeException("Invalid date 'February 29' as '" + prolepticYear + "' is not a leap year");
        } else {
          throw new TDateTimeException("Invalid date '" + TMonth.of(month).name() + " " + dayOfMonth + "'");
        }
      }
    }

    long totalDays = 0;
    int timeinSec = 0;
    totalDays += 365L * prolepticYear;
    if (prolepticYear >= 0) {
      totalDays += (prolepticYear + 3L) / 4 - (prolepticYear + 99L) / 100 + (prolepticYear + 399L) / 400;
    } else {
      totalDays -= prolepticYear / -4 - prolepticYear / -100 + prolepticYear / -400;
    }
    totalDays += (367 * month - 362) / 12;
    totalDays += dayOfMonth - 1;
    if (month > 2) {
      totalDays--;
      if (TYear.isLeap(prolepticYear) == false) {
        totalDays--;
      }
    }
    totalDays -= DAYS_0000_TO_1970;
    timeinSec = (hour * 60 + minute) * 60 + second;
    return Math.addExact(Math.multiplyExact(totalDays, 86400L), timeinSec - zoneOffset.getTotalSeconds());
  }

  private int numberOfDaysOfMonth(int year, int month) {

    int dom;
    switch (month) {
      case 2:
        dom = (TYear.isLeap(year) ? 29 : 28);
        break;
      case 4:
      case 6:
      case 9:
      case 11:
        dom = 30;
        break;
      default:
        dom = 31;
        break;
    }
    return dom;
  }

  @Override
  public TLocalDateTime localDateTime(TTemporalAccessor temporal) {

    return TLocalDateTime.from(temporal);
  }

  @Override
  public TZonedDateTime zonedDateTime(TTemporalAccessor temporal) {

    return TZonedDateTime.from(temporal);
  }

  @Override
  public TZonedDateTime zonedDateTime(TInstant instant, TZoneId zone) {

    return TZonedDateTime.ofInstant(instant, zone);
  }

  @Override
  public TLocalDate dateNow() {

    return dateNow(TClock.systemDefaultZone());
  }

  @Override
  public TLocalDate dateNow(TZoneId zone) {

    return dateNow(TClock.system(zone));
  }

  @Override
  public TLocalDate dateNow(TClock clock) {

    TObjects.requireNonNull(clock, "clock");
    return date(TLocalDate.now(clock));
  }

  @Override
  public boolean isLeapYear(long prolepticYear) {

    return ((prolepticYear & 3) == 0) && ((prolepticYear % 100) != 0 || (prolepticYear % 400) == 0);
  }

  @Override
  public int prolepticYear(TEra era, int yearOfEra) {

    if (era instanceof TIsoEra == false) {
      throw new TClassCastException("Era must be IsoEra");
    }
    return (era == TIsoEra.CE ? yearOfEra : 1 - yearOfEra);
  }

  @Override
  public TIsoEra eraOf(int eraValue) {

    return TIsoEra.of(eraValue);
  }

  @Override
  public List<TEra> eras() {

    return List.of(TIsoEra.values());
  }

  @Override
  public TValueRange range(TChronoField field) {

    return field.range();
  }

  @Override
  public TPeriod period(int years, int months, int days) {

    return TPeriod.of(years, months, days);
  }

  @Override
  public TLocalDate resolveDate(Map<TTemporalField, Long> fieldValues, TResolverStyle resolverStyle) {

    return (TLocalDate) super.resolveDate(fieldValues, resolverStyle);
  }
}
