package org.teavm.classlib.java.time.chrono;

import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_WEEK_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_WEEK_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.ERA;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.PROLEPTIC_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR_OF_ERA;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MONTHS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.WEEKS;

import java.util.List;
import java.util.Map;

import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TDayOfWeek;
import org.teavm.classlib.java.time.format.TResolverStyle;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporalAdjusters;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TValueRange;

public abstract class TAbstractChronology implements TChronology {
  @Override
  public int compareTo(TChronology other) {

    return getId().compareTo(other.getId());
  }

  @Override
  public TChronoLocalDate resolveDate(Map<TTemporalField, Long> fieldValues, TResolverStyle resolverStyle) {

    if (fieldValues.containsKey(EPOCH_DAY)) {
      return dateEpochDay(fieldValues.remove(EPOCH_DAY));
    }

    resolveProlepticMonth(fieldValues, resolverStyle);

    TChronoLocalDate resolved = resolveYearOfEra(fieldValues, resolverStyle);
    if (resolved != null) {
      return resolved;
    }

    if (fieldValues.containsKey(YEAR)) {
      if (fieldValues.containsKey(MONTH_OF_YEAR)) {
        if (fieldValues.containsKey(DAY_OF_MONTH)) {
          return resolveYMD(fieldValues, resolverStyle);
        }
        if (fieldValues.containsKey(ALIGNED_WEEK_OF_MONTH)) {
          if (fieldValues.containsKey(ALIGNED_DAY_OF_WEEK_IN_MONTH)) {
            return resolveYMAA(fieldValues, resolverStyle);
          }
          if (fieldValues.containsKey(DAY_OF_WEEK)) {
            return resolveYMAD(fieldValues, resolverStyle);
          }
        }
      }
      if (fieldValues.containsKey(DAY_OF_YEAR)) {
        return resolveYD(fieldValues, resolverStyle);
      }
      if (fieldValues.containsKey(ALIGNED_WEEK_OF_YEAR)) {
        if (fieldValues.containsKey(ALIGNED_DAY_OF_WEEK_IN_YEAR)) {
          return resolveYAA(fieldValues, resolverStyle);
        }
        if (fieldValues.containsKey(DAY_OF_WEEK)) {
          return resolveYAD(fieldValues, resolverStyle);
        }
      }
    }
    return null;
  }

  void resolveProlepticMonth(Map<TTemporalField, Long> fieldValues, TResolverStyle resolverStyle) {

    Long pMonth = fieldValues.remove(PROLEPTIC_MONTH);
    if (pMonth != null) {
      if (resolverStyle != TResolverStyle.LENIENT) {
        PROLEPTIC_MONTH.checkValidValue(pMonth);
      }
      TChronoLocalDate chronoDate = dateNow().with(DAY_OF_MONTH, 1).with(PROLEPTIC_MONTH, pMonth);
      addFieldValue(fieldValues, MONTH_OF_YEAR, chronoDate.get(MONTH_OF_YEAR));
      addFieldValue(fieldValues, YEAR, chronoDate.get(YEAR));
    }
  }

  TChronoLocalDate resolveYearOfEra(Map<TTemporalField, Long> fieldValues, TResolverStyle resolverStyle) {

    Long yoeLong = fieldValues.remove(YEAR_OF_ERA);
    if (yoeLong != null) {
      Long eraLong = fieldValues.remove(ERA);
      int yoe;
      if (resolverStyle != TResolverStyle.LENIENT) {
        yoe = range(YEAR_OF_ERA).checkValidIntValue(yoeLong, YEAR_OF_ERA);
      } else {
        yoe = Math.toIntExact(yoeLong);
      }
      if (eraLong != null) {
        TEra eraObj = eraOf(range(ERA).checkValidIntValue(eraLong, ERA));
        addFieldValue(fieldValues, YEAR, prolepticYear(eraObj, yoe));
      } else {
        if (fieldValues.containsKey(YEAR)) {
          int year = range(YEAR).checkValidIntValue(fieldValues.get(YEAR), YEAR);
          TChronoLocalDate chronoDate = dateYearDay(year, 1);
          addFieldValue(fieldValues, YEAR, prolepticYear(chronoDate.getEra(), yoe));
        } else if (resolverStyle == TResolverStyle.STRICT) {
          fieldValues.put(YEAR_OF_ERA, yoeLong);
        } else {
          List<TEra> eras = eras();
          if (eras.isEmpty()) {
            addFieldValue(fieldValues, YEAR, yoe);
          } else {
            TEra eraObj = eras.get(eras.size() - 1);
            addFieldValue(fieldValues, YEAR, prolepticYear(eraObj, yoe));
          }
        }
      }
    } else if (fieldValues.containsKey(ERA)) {
      range(ERA).checkValidValue(fieldValues.get(ERA), ERA);
    }
    return null;
  }

  TChronoLocalDate resolveYMD(Map<TTemporalField, Long> fieldValues, TResolverStyle resolverStyle) {

    int y = range(YEAR).checkValidIntValue(fieldValues.remove(YEAR), YEAR);
    if (resolverStyle == TResolverStyle.LENIENT) {
      long months = Math.subtractExact(fieldValues.remove(MONTH_OF_YEAR), 1);
      long days = Math.subtractExact(fieldValues.remove(DAY_OF_MONTH), 1);
      return date(y, 1, 1).plus(months, MONTHS).plus(days, DAYS);
    }
    int moy = range(MONTH_OF_YEAR).checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR), MONTH_OF_YEAR);
    TValueRange domRange = range(DAY_OF_MONTH);
    int dom = domRange.checkValidIntValue(fieldValues.remove(DAY_OF_MONTH), DAY_OF_MONTH);
    if (resolverStyle == TResolverStyle.SMART) {
      try {
        return date(y, moy, dom);
      } catch (TDateTimeException ex) {
        return date(y, moy, 1).with(TTemporalAdjusters.lastDayOfMonth());
      }
    }
    return date(y, moy, dom);
  }

  TChronoLocalDate resolveYD(Map<TTemporalField, Long> fieldValues, TResolverStyle resolverStyle) {

    int y = range(YEAR).checkValidIntValue(fieldValues.remove(YEAR), YEAR);
    if (resolverStyle == TResolverStyle.LENIENT) {
      long days = Math.subtractExact(fieldValues.remove(DAY_OF_YEAR), 1);
      return dateYearDay(y, 1).plus(days, DAYS);
    }
    int doy = range(DAY_OF_YEAR).checkValidIntValue(fieldValues.remove(DAY_OF_YEAR), DAY_OF_YEAR);
    return dateYearDay(y, doy);
  }

  TChronoLocalDate resolveYMAA(Map<TTemporalField, Long> fieldValues, TResolverStyle resolverStyle) {

    int y = range(YEAR).checkValidIntValue(fieldValues.remove(YEAR), YEAR);
    if (resolverStyle == TResolverStyle.LENIENT) {
      long months = Math.subtractExact(fieldValues.remove(MONTH_OF_YEAR), 1);
      long weeks = Math.subtractExact(fieldValues.remove(ALIGNED_WEEK_OF_MONTH), 1);
      long days = Math.subtractExact(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_MONTH), 1);
      return date(y, 1, 1).plus(months, MONTHS).plus(weeks, WEEKS).plus(days, DAYS);
    }
    int moy = range(MONTH_OF_YEAR).checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR), MONTH_OF_YEAR);
    int aw = range(ALIGNED_WEEK_OF_MONTH).checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_MONTH),
        ALIGNED_WEEK_OF_MONTH);
    int ad = range(ALIGNED_DAY_OF_WEEK_IN_MONTH).checkValidIntValue(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_MONTH),
        ALIGNED_DAY_OF_WEEK_IN_MONTH);
    TChronoLocalDate date = date(y, moy, 1).plus((aw - 1) * 7 + (ad - 1), DAYS);
    if (resolverStyle == TResolverStyle.STRICT && date.get(MONTH_OF_YEAR) != moy) {
      throw new TDateTimeException("Strict mode rejected resolved date as it is in a different month");
    }
    return date;
  }

  TChronoLocalDate resolveYMAD(Map<TTemporalField, Long> fieldValues, TResolverStyle resolverStyle) {

    int y = range(YEAR).checkValidIntValue(fieldValues.remove(YEAR), YEAR);
    if (resolverStyle == TResolverStyle.LENIENT) {
      long months = Math.subtractExact(fieldValues.remove(MONTH_OF_YEAR), 1);
      long weeks = Math.subtractExact(fieldValues.remove(ALIGNED_WEEK_OF_MONTH), 1);
      long dow = Math.subtractExact(fieldValues.remove(DAY_OF_WEEK), 1);
      return resolveAligned(date(y, 1, 1), months, weeks, dow);
    }
    int moy = range(MONTH_OF_YEAR).checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR), MONTH_OF_YEAR);
    int aw = range(ALIGNED_WEEK_OF_MONTH).checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_MONTH),
        ALIGNED_WEEK_OF_MONTH);
    int dow = range(DAY_OF_WEEK).checkValidIntValue(fieldValues.remove(DAY_OF_WEEK), DAY_OF_WEEK);
    TChronoLocalDate date = date(y, moy, 1).plus((aw - 1) * 7, DAYS)
        .with(TTemporalAdjusters.nextOrSame(TDayOfWeek.of(dow)));
    if (resolverStyle == TResolverStyle.STRICT && date.get(MONTH_OF_YEAR) != moy) {
      throw new TDateTimeException("Strict mode rejected resolved date as it is in a different month");
    }
    return date;
  }

  TChronoLocalDate resolveYAA(Map<TTemporalField, Long> fieldValues, TResolverStyle resolverStyle) {

    int y = range(YEAR).checkValidIntValue(fieldValues.remove(YEAR), YEAR);
    if (resolverStyle == TResolverStyle.LENIENT) {
      long weeks = Math.subtractExact(fieldValues.remove(ALIGNED_WEEK_OF_YEAR), 1);
      long days = Math.subtractExact(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_YEAR), 1);
      return dateYearDay(y, 1).plus(weeks, WEEKS).plus(days, DAYS);
    }
    int aw = range(ALIGNED_WEEK_OF_YEAR).checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_YEAR),
        ALIGNED_WEEK_OF_YEAR);
    int ad = range(ALIGNED_DAY_OF_WEEK_IN_YEAR).checkValidIntValue(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_YEAR),
        ALIGNED_DAY_OF_WEEK_IN_YEAR);
    TChronoLocalDate date = dateYearDay(y, 1).plus((aw - 1) * 7 + (ad - 1), DAYS);
    if (resolverStyle == TResolverStyle.STRICT && date.get(YEAR) != y) {
      throw new TDateTimeException("Strict mode rejected resolved date as it is in a different year");
    }
    return date;
  }

  TChronoLocalDate resolveYAD(Map<TTemporalField, Long> fieldValues, TResolverStyle resolverStyle) {

    int y = range(YEAR).checkValidIntValue(fieldValues.remove(YEAR), YEAR);
    if (resolverStyle == TResolverStyle.LENIENT) {
      long weeks = Math.subtractExact(fieldValues.remove(ALIGNED_WEEK_OF_YEAR), 1);
      long dow = Math.subtractExact(fieldValues.remove(DAY_OF_WEEK), 1);
      return resolveAligned(dateYearDay(y, 1), 0, weeks, dow);
    }
    int aw = range(ALIGNED_WEEK_OF_YEAR).checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_YEAR),
        ALIGNED_WEEK_OF_YEAR);
    int dow = range(DAY_OF_WEEK).checkValidIntValue(fieldValues.remove(DAY_OF_WEEK), DAY_OF_WEEK);
    TChronoLocalDate date = dateYearDay(y, 1).plus((aw - 1) * 7, DAYS)
        .with(TTemporalAdjusters.nextOrSame(TDayOfWeek.of(dow)));
    if (resolverStyle == TResolverStyle.STRICT && date.get(YEAR) != y) {
      throw new TDateTimeException("Strict mode rejected resolved date as it is in a different year");
    }
    return date;
  }

  TChronoLocalDate resolveAligned(TChronoLocalDate base, long months, long weeks, long dow) {

    TChronoLocalDate date = base.plus(months, MONTHS).plus(weeks, WEEKS);
    if (dow > 7) {
      date = date.plus((dow - 1) / 7, WEEKS);
      dow = ((dow - 1) % 7) + 1;
    } else if (dow < 1) {
      date = date.plus(Math.subtractExact(dow, 7) / 7, WEEKS);
      dow = ((dow + 6) % 7) + 1;
    }
    return date.with(TTemporalAdjusters.nextOrSame(TDayOfWeek.of((int) dow)));
  }

  void addFieldValue(Map<TTemporalField, Long> fieldValues, TChronoField field, long value) {

    Long old = fieldValues.get(field);
    if (old != null && old.longValue() != value) {
      throw new TDateTimeException("Conflict found: " + field + " " + old + " differs from " + field + " " + value);
    }
    fieldValues.put(field, value);
  }

  @Override
  public boolean equals(Object obj) {

    if (this == obj) {
      return true;
    }
    if (obj instanceof TAbstractChronology) {
      return compareTo((TAbstractChronology) obj) == 0;
    }
    return false;
  }

  @Override
  public int hashCode() {

    return getClass().hashCode() ^ getId().hashCode();
  }

  @Override
  public String toString() {

    return getId();
  }

}
