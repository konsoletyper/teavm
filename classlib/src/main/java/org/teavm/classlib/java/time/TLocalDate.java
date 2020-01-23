package org.teavm.classlib.java.time;

import static org.teavm.classlib.java.time.TLocalTime.SECONDS_PER_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_WEEK_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_WEEK_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.ERA;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.PROLEPTIC_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.TMath;
import org.teavm.classlib.java.lang.TStringBuilder;
import org.teavm.classlib.java.time.chrono.TChronoLocalDate;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.chrono.TIsoEra;
import org.teavm.classlib.java.time.format.TDateTimeFormatter;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.temporal.TValueRange;
import org.teavm.classlib.java.util.TObjects;
import org.teavm.classlib.java.util.stream.TLongStream;
import org.teavm.classlib.java.util.stream.TStream;

public final class TLocalDate implements TTemporal, TTemporalAdjuster, TChronoLocalDate, TSerializable {

  public static final TLocalDate MIN = TLocalDate.of(TYear.MIN_VALUE, 1, 1);

  public static final TLocalDate MAX = TLocalDate.of(TYear.MAX_VALUE, 12, 31);

  public static final TLocalDate EPOCH = TLocalDate.of(1970, 1, 1);

  private static final int DAYS_PER_CYCLE = 146097;

  static final long DAYS_0000_TO_1970 = (DAYS_PER_CYCLE * 5L) - (30L * 365L + 7L);

  private final int year;

  private final short month;

  private final short day;

  private TLocalDate(int year, int month, int dayOfMonth) {

    this.year = year;
    this.month = (short) month;
    this.day = (short) dayOfMonth;
  }

  @Override
  public boolean isSupported(TTemporalField field) {

    return TChronoLocalDate.super.isSupported(field);
  }

  @Override
  public boolean isSupported(TTemporalUnit unit) {

    return TChronoLocalDate.super.isSupported(unit);
  }

  @Override
  public TValueRange range(TTemporalField field) {

    if (field instanceof TChronoField) {
      TChronoField f = (TChronoField) field;
      if (f.isDateBased()) {
        switch (f) {
          case DAY_OF_MONTH:
            return TValueRange.of(1, lengthOfMonth());
          case DAY_OF_YEAR:
            return TValueRange.of(1, lengthOfYear());
          case ALIGNED_WEEK_OF_MONTH:
            return TValueRange.of(1, getMonth() == TMonth.FEBRUARY && isLeapYear() == false ? 4 : 5);
          case YEAR_OF_ERA:
            return (getYear() <= 0 ? TValueRange.of(1, TYear.MAX_VALUE + 1) : TValueRange.of(1, TYear.MAX_VALUE));
        }
        return field.range();
      }
      throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
    }
    return field.rangeRefinedBy(this);
  }

  @Override
  public int get(TTemporalField field) {

    if (field instanceof TChronoField) {
      return get0(field);
    }
    return TChronoLocalDate.super.get(field);
  }

  @Override
  public long getLong(TTemporalField field) {

    if (field instanceof TChronoField) {
      if (field == EPOCH_DAY) {
        return toEpochDay();
      }
      if (field == PROLEPTIC_MONTH) {
        return getProlepticMonth();
      }
      return get0(field);
    }
    return field.getFrom(this);
  }

  private int get0(TTemporalField field) {

    switch ((TChronoField) field) {
      case DAY_OF_WEEK:
        return getDayOfWeek().getValue();
      case ALIGNED_DAY_OF_WEEK_IN_MONTH:
        return ((this.day - 1) % 7) + 1;
      case ALIGNED_DAY_OF_WEEK_IN_YEAR:
        return ((getDayOfYear() - 1) % 7) + 1;
      case DAY_OF_MONTH:
        return this.day;
      case DAY_OF_YEAR:
        return getDayOfYear();
      case EPOCH_DAY:
        throw new TUnsupportedTemporalTypeException("Invalid field 'EpochDay' for get() method, use getLong() instead");
      case ALIGNED_WEEK_OF_MONTH:
        return ((this.day - 1) / 7) + 1;
      case ALIGNED_WEEK_OF_YEAR:
        return ((getDayOfYear() - 1) / 7) + 1;
      case MONTH_OF_YEAR:
        return this.month;
      case PROLEPTIC_MONTH:
        throw new TUnsupportedTemporalTypeException(
            "Invalid field 'ProlepticMonth' for get() method, use getLong() instead");
      case YEAR_OF_ERA:
        return (this.year >= 1 ? this.year : 1 - this.year);
      case YEAR:
        return this.year;
      case ERA:
        return (this.year >= 1 ? 1 : 0);
    }
    throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
  }

  private long getProlepticMonth() {

    return (this.year * 12L + this.month - 1);
  }

  @Override
  public TIsoChronology getChronology() {

    return TIsoChronology.INSTANCE;
  }

  @Override
  public TIsoEra getEra() {

    return (getYear() >= 1 ? TIsoEra.CE : TIsoEra.BCE);
  }

  public int getYear() {

    return this.year;
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

  public int getDayOfYear() {

    return getMonth().firstDayOfYear(isLeapYear()) + this.day - 1;
  }

  public TDayOfWeek getDayOfWeek() {

    int dow0 = Math.floorMod(toEpochDay() + 3, 7);
    return TDayOfWeek.of(dow0 + 1);
  }

  @Override
  public boolean isLeapYear() {

    return TYear.isLeap(this.year);
  }

  @Override
  public int lengthOfMonth() {

    switch (this.month) {
      case 2:
        return (isLeapYear() ? 29 : 28);
      case 4:
      case 6:
      case 9:
      case 11:
        return 30;
      default:
        return 31;
    }
  }

  @Override
  public int lengthOfYear() {

    return (isLeapYear() ? 366 : 365);
  }

  @Override
  public TLocalDate with(TTemporalAdjuster adjuster) {

    if (adjuster instanceof TLocalDate) {
      return (TLocalDate) adjuster;
    }
    return (TLocalDate) adjuster.adjustInto(this);
  }

  @Override
  public TLocalDate with(TTemporalField field, long newValue) {

    if (field instanceof TChronoField) {
      TChronoField f = (TChronoField) field;
      f.checkValidValue(newValue);
      switch (f) {
        case DAY_OF_WEEK:
          return plusDays(newValue - getDayOfWeek().getValue());
        case ALIGNED_DAY_OF_WEEK_IN_MONTH:
          return plusDays(newValue - getLong(ALIGNED_DAY_OF_WEEK_IN_MONTH));
        case ALIGNED_DAY_OF_WEEK_IN_YEAR:
          return plusDays(newValue - getLong(ALIGNED_DAY_OF_WEEK_IN_YEAR));
        case DAY_OF_MONTH:
          return withDayOfMonth((int) newValue);
        case DAY_OF_YEAR:
          return withDayOfYear((int) newValue);
        case EPOCH_DAY:
          return TLocalDate.ofEpochDay(newValue);
        case ALIGNED_WEEK_OF_MONTH:
          return plusWeeks(newValue - getLong(ALIGNED_WEEK_OF_MONTH));
        case ALIGNED_WEEK_OF_YEAR:
          return plusWeeks(newValue - getLong(ALIGNED_WEEK_OF_YEAR));
        case MONTH_OF_YEAR:
          return withMonth((int) newValue);
        case PROLEPTIC_MONTH:
          return plusMonths(newValue - getProlepticMonth());
        case YEAR_OF_ERA:
          return withYear((int) (this.year >= 1 ? newValue : 1 - newValue));
        case YEAR:
          return withYear((int) newValue);
        case ERA:
          return (getLong(ERA) == newValue ? this : withYear(1 - this.year));
      }
      throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
    }
    return field.adjustInto(this, newValue);
  }

  public TLocalDate withYear(int year) {

    if (this.year == year) {
      return this;
    }
    YEAR.checkValidValue(year);
    return resolvePreviousValid(year, this.month, this.day);
  }

  public TLocalDate withMonth(int month) {

    if (this.month == month) {
      return this;
    }
    MONTH_OF_YEAR.checkValidValue(month);
    return resolvePreviousValid(this.year, month, this.day);
  }

  public TLocalDate withDayOfMonth(int dayOfMonth) {

    if (this.day == dayOfMonth) {
      return this;
    }
    return of(this.year, this.month, dayOfMonth);
  }

  public TLocalDate withDayOfYear(int dayOfYear) {

    if (getDayOfYear() == dayOfYear) {
      return this;
    }
    return ofYearDay(this.year, dayOfYear);
  }

  @Override
  public TLocalDate plus(TTemporalAmount amountToAdd) {

    if (amountToAdd instanceof TPeriod) {
      TPeriod periodToAdd = (TPeriod) amountToAdd;
      return plusMonths(periodToAdd.toTotalMonths()).plusDays(periodToAdd.getDays());
    }
    TObjects.requireNonNull(amountToAdd, "amountToAdd");
    return (TLocalDate) amountToAdd.addTo(this);
  }

  @Override
  public TLocalDate plus(long amountToAdd, TTemporalUnit unit) {

    if (unit instanceof TChronoUnit) {
      TChronoUnit f = (TChronoUnit) unit;
      switch (f) {
        case DAYS:
          return plusDays(amountToAdd);
        case WEEKS:
          return plusWeeks(amountToAdd);
        case MONTHS:
          return plusMonths(amountToAdd);
        case YEARS:
          return plusYears(amountToAdd);
        case DECADES:
          return plusYears(Math.multiplyExact(amountToAdd, 10));
        case CENTURIES:
          return plusYears(Math.multiplyExact(amountToAdd, 100));
        case MILLENNIA:
          return plusYears(Math.multiplyExact(amountToAdd, 1000));
        case ERAS:
          return with(ERA, Math.addExact(getLong(ERA), amountToAdd));
      }
      throw new TUnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }
    return unit.addTo(this, amountToAdd);
  }

  public TLocalDate plusYears(long yearsToAdd) {

    if (yearsToAdd == 0) {
      return this;
    }
    int newYear = YEAR.checkValidIntValue(this.year + yearsToAdd);
    return resolvePreviousValid(newYear, this.month, this.day);
  }

  public TLocalDate plusMonths(long monthsToAdd) {

    if (monthsToAdd == 0) {
      return this;
    }
    long monthCount = this.year * 12L + (this.month - 1);
    long calcMonths = monthCount + monthsToAdd;
    int newYear = YEAR.checkValidIntValue(Math.floorDiv(calcMonths, 12));
    int newMonth = Math.floorMod(calcMonths, 12) + 1;
    return resolvePreviousValid(newYear, newMonth, this.day);
  }

  public TLocalDate plusWeeks(long weeksToAdd) {

    return plusDays(Math.multiplyExact(weeksToAdd, 7));
  }

  public TLocalDate plusDays(long daysToAdd) {

    if (daysToAdd == 0) {
      return this;
    }
    long dom = this.day + daysToAdd;
    if (dom > 0) {
      if (dom <= 28) {
        return new TLocalDate(this.year, this.month, (int) dom);
      } else if (dom <= 59) {
        long monthLen = lengthOfMonth();
        if (dom <= monthLen) {
          return new TLocalDate(this.year, this.month, (int) dom);
        } else if (this.month < 12) {
          return new TLocalDate(this.year, this.month + 1, (int) (dom - monthLen));
        } else {
          YEAR.checkValidValue(this.year + 1);
          return new TLocalDate(this.year + 1, 1, (int) (dom - monthLen));
        }
      }
    }

    long mjDay = Math.addExact(toEpochDay(), daysToAdd);
    return TLocalDate.ofEpochDay(mjDay);
  }

  @Override
  public TLocalDate minus(TTemporalAmount amountToSubtract) {

    if (amountToSubtract instanceof TPeriod) {
      TPeriod periodToSubtract = (TPeriod) amountToSubtract;
      return minusMonths(periodToSubtract.toTotalMonths()).minusDays(periodToSubtract.getDays());
    }
    TObjects.requireNonNull(amountToSubtract, "amountToSubtract");
    return (TLocalDate) amountToSubtract.subtractFrom(this);
  }

  @Override
  public TLocalDate minus(long amountToSubtract, TTemporalUnit unit) {

    return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit)
        : plus(-amountToSubtract, unit));
  }

  public TLocalDate minusYears(long yearsToSubtract) {

    return (yearsToSubtract == Long.MIN_VALUE ? plusYears(Long.MAX_VALUE).plusYears(1) : plusYears(-yearsToSubtract));
  }

  public TLocalDate minusMonths(long monthsToSubtract) {

    return (monthsToSubtract == Long.MIN_VALUE ? plusMonths(Long.MAX_VALUE).plusMonths(1)
        : plusMonths(-monthsToSubtract));
  }

  public TLocalDate minusWeeks(long weeksToSubtract) {

    return (weeksToSubtract == Long.MIN_VALUE ? plusWeeks(Long.MAX_VALUE).plusWeeks(1) : plusWeeks(-weeksToSubtract));
  }

  public TLocalDate minusDays(long daysToSubtract) {

    return (daysToSubtract == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-daysToSubtract));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <R> R query(TTemporalQuery<R> query) {

    if (query == TTemporalQueries.localDate()) {
      return (R) this;
    }
    return TChronoLocalDate.super.query(query);
  }

  @Override
  public TTemporal adjustInto(TTemporal temporal) {

    return TChronoLocalDate.super.adjustInto(temporal);
  }

  @Override
  public long until(TTemporal endExclusive, TTemporalUnit unit) {

    TLocalDate end = TLocalDate.from(endExclusive);
    if (unit instanceof TChronoUnit) {
      switch ((TChronoUnit) unit) {
        case DAYS:
          return daysUntil(end);
        case WEEKS:
          return daysUntil(end) / 7;
        case MONTHS:
          return monthsUntil(end);
        case YEARS:
          return monthsUntil(end) / 12;
        case DECADES:
          return monthsUntil(end) / 120;
        case CENTURIES:
          return monthsUntil(end) / 1200;
        case MILLENNIA:
          return monthsUntil(end) / 12000;
        case ERAS:
          return end.getLong(ERA) - getLong(ERA);
      }
      throw new TUnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }
    return unit.between(this, end);
  }

  long daysUntil(TLocalDate end) {

    return end.toEpochDay() - toEpochDay();
  }

  private long monthsUntil(TLocalDate end) {

    long packed1 = getProlepticMonth() * 32L + getDayOfMonth();
    long packed2 = end.getProlepticMonth() * 32L + end.getDayOfMonth();
    return (packed2 - packed1) / 32;
  }

  @Override
  public TPeriod until(TChronoLocalDate endDateExclusive) {

    TLocalDate end = TLocalDate.from(endDateExclusive);
    long totalMonths = end.getProlepticMonth() - getProlepticMonth();
    int days = end.day - this.day;
    if (totalMonths > 0 && days < 0) {
      totalMonths--;
      TLocalDate calcDate = plusMonths(totalMonths);
      days = (int) (end.toEpochDay() - calcDate.toEpochDay());
    } else if (totalMonths < 0 && days > 0) {
      totalMonths++;
      days -= end.lengthOfMonth();
    }
    long years = totalMonths / 12;
    int months = (int) (totalMonths % 12);
    return TPeriod.of(Math.toIntExact(years), months, days);
  }

  public TStream<TLocalDate> datesUntil(TLocalDate endExclusive) {

    long end = endExclusive.toEpochDay();
    long start = toEpochDay();
    if (end < start) {
      throw new IllegalArgumentException(endExclusive + " < " + this);
    }
    return TLongStream.range(start, end).mapToObj(TLocalDate::ofEpochDay);
  }

  public TStream<TLocalDate> datesUntil(TLocalDate endExclusive, TPeriod step) {

    if (step.isZero()) {
      throw new IllegalArgumentException("step is zero");
    }
    long end = endExclusive.toEpochDay();
    long start = toEpochDay();
    long until = end - start;
    long months = step.toTotalMonths();
    long days = step.getDays();
    if ((months < 0 && days > 0) || (months > 0 && days < 0)) {
      throw new IllegalArgumentException("period months and days are of opposite sign");
    }
    if (until == 0) {
      return TStream.empty();
    }
    int sign = months > 0 || days > 0 ? 1 : -1;
    if (sign < 0 ^ until < 0) {
      throw new TIllegalArgumentException(endExclusive + (sign < 0 ? " > " : " < ") + this);
    }
    if (months == 0) {
      long steps = (until - sign) / days;
      return TLongStream.rangeClosed(0, steps).mapToObj(n -> TLocalDate.ofEpochDay(start + n * days));
    }
    long steps = until * 1600 / (months * 48699 + days * 1600) + 1;
    long addMonths = months * steps;
    long addDays = days * steps;
    long maxAddMonths = months > 0 ? MAX.getProlepticMonth() - getProlepticMonth()
        : getProlepticMonth() - MIN.getProlepticMonth();
    if (addMonths * sign > maxAddMonths || (plusMonths(addMonths).toEpochDay() + addDays) * sign >= end * sign) {
      steps--;
      addMonths -= months;
      addDays -= days;
      if (addMonths * sign > maxAddMonths || (plusMonths(addMonths).toEpochDay() + addDays) * sign >= end * sign) {
        steps--;
      }
    }
    return TLongStream.rangeClosed(0, steps).mapToObj(n -> plusMonths(months * n).plusDays(days * n));
  }

  @Override
  public String format(TDateTimeFormatter formatter) {

    TObjects.requireNonNull(formatter, "formatter");
    return formatter.format(this);
  }

  @Override
  public TLocalDateTime atTime(TLocalTime time) {

    return TLocalDateTime.of(this, time);
  }

  public TLocalDateTime atTime(int hour, int minute) {

    return atTime(TLocalTime.of(hour, minute));
  }

  public TLocalDateTime atTime(int hour, int minute, int second) {

    return atTime(TLocalTime.of(hour, minute, second));
  }

  public TLocalDateTime atTime(int hour, int minute, int second, int nanoOfSecond) {

    return atTime(TLocalTime.of(hour, minute, second, nanoOfSecond));
  }

  public TOffsetDateTime atTime(TOffsetTime time) {

    return TOffsetDateTime.of(TLocalDateTime.of(this, time.toLocalTime()), time.getOffset());
  }

  public TLocalDateTime atStartOfDay() {

    return TLocalDateTime.of(this, TLocalTime.MIDNIGHT);
  }

  public TZonedDateTime atStartOfDay(TZoneId zone) {

    TObjects.requireNonNull(zone, "zone");
    // need to handle case where there is a gap from 11:30 to 00:30
    // standard ZDT factory would result in 01:00 rather than 00:30
    TLocalDateTime ldt = atTime(TLocalTime.MIDNIGHT);
    if (zone instanceof TZoneOffset == false) {
      // TZoneRules rules = zone.getRules();
      // TZoneOffsetTransition trans = rules.getTransition(ldt);
      // if (trans != null && trans.isGap()) {
      // ldt = trans.getDateTimeAfter();
      // }
    }
    return TZonedDateTime.of(ldt, zone);
  }

  @Override
  public long toEpochDay() {

    long y = this.year;
    long m = this.month;
    long total = 0;
    total += 365 * y;
    if (y >= 0) {
      total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400;
    } else {
      total -= y / -4 - y / -100 + y / -400;
    }
    total += ((367 * m - 362) / 12);
    total += this.day - 1;
    if (m > 2) {
      total--;
      if (isLeapYear() == false) {
        total--;
      }
    }
    return total - DAYS_0000_TO_1970;
  }

  public long toEpochSecond(TLocalTime time, TZoneOffset offset) {

    TObjects.requireNonNull(time, "time");
    TObjects.requireNonNull(offset, "offset");
    long secs = toEpochDay() * SECONDS_PER_DAY + time.toSecondOfDay();
    secs -= offset.getTotalSeconds();
    return secs;
  }

  @Override
  public int compareTo(TChronoLocalDate other) {

    if (other instanceof TLocalDate) {
      return compareTo0((TLocalDate) other);
    }
    return TChronoLocalDate.super.compareTo(other);
  }

  int compareTo0(TLocalDate otherDate) {

    int cmp = (this.year - otherDate.year);
    if (cmp == 0) {
      cmp = (this.month - otherDate.month);
      if (cmp == 0) {
        cmp = (this.day - otherDate.day);
      }
    }
    return cmp;
  }

  @Override
  public boolean isAfter(TChronoLocalDate other) {

    if (other instanceof TLocalDate) {
      return compareTo0((TLocalDate) other) > 0;
    }
    return TChronoLocalDate.super.isAfter(other);
  }

  @Override
  public boolean isBefore(TChronoLocalDate other) {

    if (other instanceof TLocalDate) {
      return compareTo0((TLocalDate) other) < 0;
    }
    return TChronoLocalDate.super.isBefore(other);
  }

  @Override
  public boolean isEqual(TChronoLocalDate other) {

    if (other instanceof TLocalDate) {
      return compareTo0((TLocalDate) other) == 0;
    }
    return TChronoLocalDate.super.isEqual(other);
  }

  @Override
  public boolean equals(Object obj) {

    if (this == obj) {
      return true;
    }
    if (obj instanceof TLocalDate) {
      return compareTo0((TLocalDate) obj) == 0;
    }
    return false;
  }

  @Override
  public int hashCode() {

    int yearValue = this.year;
    int monthValue = this.month;
    int dayValue = this.day;
    return (yearValue & 0xFFFFF800) ^ ((yearValue << 11) + (monthValue << 6) + (dayValue));
  }

  @Override
  public String toString() {

    int yearValue = this.year;
    int monthValue = this.month;
    int dayValue = this.day;
    int absYear = Math.abs(yearValue);
    TStringBuilder buf = new TStringBuilder(10);
    if (absYear < 1000) {
      if (yearValue < 0) {
        buf.append(yearValue - 10000).deleteCharAt(1);
      } else {
        buf.append(yearValue + 10000).deleteCharAt(0);
      }
    } else {
      if (yearValue > 9999) {
        buf.append('+');
      }
      buf.append(yearValue);
    }
    return buf.append(monthValue < 10 ? "-0" : "-").append(monthValue).append(dayValue < 10 ? "-0" : "-")
        .append(dayValue).toString();
  }

  public static TLocalDate now() {

    return now(TClock.systemDefaultZone());
  }

  public static TLocalDate now(TZoneId zone) {

    return now(TClock.system(zone));
  }

  public static TLocalDate now(TClock clock) {

    TObjects.requireNonNull(clock, "clock");
    final TInstant now = clock.instant();
    return ofInstant(now, clock.getZone());
  }

  public static TLocalDate of(int year, TMonth month, int dayOfMonth) {

    YEAR.checkValidValue(year);
    TObjects.requireNonNull(month, "month");
    DAY_OF_MONTH.checkValidValue(dayOfMonth);
    return create(year, month.getValue(), dayOfMonth);
  }

  public static TLocalDate of(int year, int month, int dayOfMonth) {

    YEAR.checkValidValue(year);
    MONTH_OF_YEAR.checkValidValue(month);
    DAY_OF_MONTH.checkValidValue(dayOfMonth);
    return create(year, month, dayOfMonth);
  }

  public static TLocalDate ofYearDay(int year, int dayOfYear) {

    YEAR.checkValidValue(year);
    DAY_OF_YEAR.checkValidValue(dayOfYear);
    boolean leap = TYear.isLeap(year);
    if (dayOfYear == 366 && leap == false) {
      throw new TDateTimeException("Invalid date 'DayOfYear 366' as '" + year + "' is not a leap year");
    }
    TMonth moy = TMonth.of((dayOfYear - 1) / 31 + 1);
    int monthEnd = moy.firstDayOfYear(leap) + moy.length(leap) - 1;
    if (dayOfYear > monthEnd) {
      moy = moy.plus(1);
    }
    int dom = dayOfYear - moy.firstDayOfYear(leap) + 1;
    return new TLocalDate(year, moy.getValue(), dom);
  }

  public static TLocalDate ofInstant(TInstant instant, TZoneId zone) {

    TObjects.requireNonNull(instant, "instant");
    TObjects.requireNonNull(zone, "zone");
    // TZoneRules rules = zone.getRules();
    TZoneOffset offset = null; // rules.getOffset(instant);
    long localSecond = instant.getEpochSecond() + offset.getTotalSeconds();
    long localEpochDay = Math.floorDiv(localSecond, SECONDS_PER_DAY);
    return ofEpochDay(localEpochDay);
  }

  public static TLocalDate ofEpochDay(long epochDay) {

    EPOCH_DAY.checkValidValue(epochDay);
    long zeroDay = epochDay + DAYS_0000_TO_1970;
    zeroDay -= 60;
    long adjust = 0;
    if (zeroDay < 0) {
      long adjustCycles = (zeroDay + 1) / DAYS_PER_CYCLE - 1;
      adjust = adjustCycles * 400;
      zeroDay += -adjustCycles * DAYS_PER_CYCLE;
    }
    long yearEst = (400 * zeroDay + 591) / DAYS_PER_CYCLE;
    long doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
    if (doyEst < 0) {
      yearEst--;
      doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
    }
    yearEst += adjust;
    int marchDoy0 = (int) doyEst;

    int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
    int month = (marchMonth0 + 2) % 12 + 1;
    int dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
    yearEst += marchMonth0 / 10;

    int year = YEAR.checkValidIntValue(yearEst);
    return new TLocalDate(year, month, dom);
  }

  public static TLocalDate from(TTemporalAccessor temporal) {

    TObjects.requireNonNull(temporal, "temporal");
    TLocalDate date = temporal.query(TTemporalQueries.localDate());
    if (date == null) {
      throw new TDateTimeException("Unable to obtain LocalDate from TemporalAccessor: " + temporal + " of type "
          + temporal.getClass().getName());
    }
    return date;
  }

  public static TLocalDate parse(CharSequence text) {

    return parse(text, TDateTimeFormatter.ISO_LOCAL_DATE);
  }

  public static TLocalDate parse(CharSequence text, TDateTimeFormatter formatter) {

    TObjects.requireNonNull(formatter, "formatter");
    return formatter.parse(text, TLocalDate::from);
  }

  private static TLocalDate create(int year, int month, int dayOfMonth) {

    if (dayOfMonth > 28) {
      int dom = 31;
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
      }
      if (dayOfMonth > dom) {
        if (dayOfMonth == 29) {
          throw new TDateTimeException("Invalid date 'February 29' as '" + year + "' is not a leap year");
        } else {
          throw new TDateTimeException("Invalid date '" + TMonth.of(month).name() + " " + dayOfMonth + "'");
        }
      }
    }
    return new TLocalDate(year, month, dayOfMonth);
  }

  private static TLocalDate resolvePreviousValid(int year, int month, int day) {

    switch (month) {
      case 2:
        day = TMath.min(day, TYear.isLeap(year) ? 29 : 28);
        break;
      case 4:
      case 6:
      case 9:
      case 11:
        day = TMath.min(day, 30);
        break;
    }
    return new TLocalDate(year, month, day);
  }
}
