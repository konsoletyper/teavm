package org.teavm.classlib.java.time.chrono;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.teavm.classlib.java.lang.TComparable;
import org.teavm.classlib.java.time.TClock;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.format.TResolverStyle;
import org.teavm.classlib.java.time.format.TTextStyle;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TValueRange;
import org.teavm.classlib.java.util.TLocale;
import org.teavm.classlib.java.util.TObjects;

public interface TChronology extends TComparable<TChronology> {

  String getId();

  String getCalendarType();

  default TChronoLocalDate date(TEra era, int yearOfEra, int month, int dayOfMonth) {

    return date(prolepticYear(era, yearOfEra), month, dayOfMonth);
  }

  TChronoLocalDate date(int prolepticYear, int month, int dayOfMonth);

  default TChronoLocalDate dateYearDay(TEra era, int yearOfEra, int dayOfYear) {

    return dateYearDay(prolepticYear(era, yearOfEra), dayOfYear);
  }

  TChronoLocalDate dateYearDay(int prolepticYear, int dayOfYear);

  TChronoLocalDate dateEpochDay(long epochDay);

  default TChronoLocalDate dateNow() {

    return dateNow(TClock.systemDefaultZone());
  }

  default TChronoLocalDate dateNow(TZoneId zone) {

    return dateNow(TClock.system(zone));
  }

  default TChronoLocalDate dateNow(TClock clock) {

    TObjects.requireNonNull(clock, "clock");
    return date(TLocalDate.now(clock));
  }

  TChronoLocalDate date(TTemporalAccessor temporal);

  default TChronoLocalDateTime<? extends TChronoLocalDate> localDateTime(TTemporalAccessor temporal) {

    try {
      return date(temporal).atTime(TLocalTime.from(temporal));
    } catch (TDateTimeException ex) {
      throw new TDateTimeException("Unable to obtain ChronoLocalDateTime from TemporalAccessor: " + temporal.getClass(),
          ex);
    }
  }

  TChronoZonedDateTime<? extends TChronoLocalDate> zonedDateTime(TInstant instant, TZoneId zone);

  TChronoZonedDateTime<? extends TChronoLocalDate> zonedDateTime(TTemporalAccessor temporal);

  boolean isLeapYear(long prolepticYear);

  int prolepticYear(TEra era, int yearOfEra);

  TEra eraOf(int eraValue);

  List<TEra> eras();

  TValueRange range(TChronoField field);

  default String getDisplayName(TTextStyle style, TLocale locale) {

    return getId();
  }

  TChronoPeriod period(int years, int months, int days);

  default long epochSecond(int prolepticYear, int month, int dayOfMonth, int hour, int minute, int second,
      TZoneOffset zoneOffset) {

    TObjects.requireNonNull(zoneOffset, "zoneOffset");
    HOUR_OF_DAY.checkValidValue(hour);
    MINUTE_OF_HOUR.checkValidValue(minute);
    SECOND_OF_MINUTE.checkValidValue(second);
    long daysInSec = Math.multiplyExact(date(prolepticYear, month, dayOfMonth).toEpochDay(), 86400);
    long timeinSec = (hour * 60 + minute) * 60 + second;
    return Math.addExact(daysInSec, timeinSec - zoneOffset.getTotalSeconds());
  }

  public default long epochSecond(TEra era, int yearOfEra, int month, int dayOfMonth, int hour, int minute, int second,
      TZoneOffset zoneOffset) {

    TObjects.requireNonNull(era, "era");
    return epochSecond(prolepticYear(era, yearOfEra), month, dayOfMonth, hour, minute, second, zoneOffset);
  }

  TChronoLocalDate resolveDate(Map<TTemporalField, Long> fieldValues, TResolverStyle resolverStyle);

  static TChronology from(TTemporalAccessor temporal) {

    TObjects.requireNonNull(temporal, "temporal");
    TChronology obj = temporal.query(TTemporalQueries.chronology());
    return Objects.requireNonNullElse(obj, TIsoChronology.INSTANCE);
  }

  static TChronology ofLocale(TLocale locale) {

    return TIsoChronology.INSTANCE;
  }

  static TChronology of(String id) {

    if (id.equals(TIsoChronology.INSTANCE.getId())) {
      return TIsoChronology.INSTANCE;
    }
    throw new TDateTimeException("Unknown chronology: " + id);
  }

  static Set<TChronology> getAvailableChronologies() {

    return Collections.singleton(TIsoChronology.INSTANCE);
  }
}
