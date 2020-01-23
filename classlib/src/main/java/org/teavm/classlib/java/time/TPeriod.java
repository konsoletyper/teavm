package org.teavm.classlib.java.time;

import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MONTHS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.YEARS;

import java.util.List;
import java.util.Objects;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TArithmeticException;
import org.teavm.classlib.java.lang.TNumberFormatException;
import org.teavm.classlib.java.lang.TStringBuilder;
import org.teavm.classlib.java.time.chrono.TChronoPeriod;
import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeParseException;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.util.TObjects;
import org.teavm.classlib.java.util.regex.TMatcher;
import org.teavm.classlib.java.util.regex.TPattern;

public final class TPeriod implements TChronoPeriod, TSerializable {
  public static final TPeriod ZERO = new TPeriod(0, 0, 0);

  private static final TPattern PATTERN = TPattern.compile(
      "([-+]?)P(?:([-+]?[0-9]+)Y)?(?:([-+]?[0-9]+)M)?(?:([-+]?[0-9]+)W)?(?:([-+]?[0-9]+)D)?",
      TPattern.CASE_INSENSITIVE);

  private static final List<TTemporalUnit> SUPPORTED_UNITS = List.of(YEARS, MONTHS, DAYS);

  private final int years;

  private final int months;

  private final int days;

  public static TPeriod ofYears(int years) {

    return create(years, 0, 0);
  }

  public static TPeriod ofMonths(int months) {

    return create(0, months, 0);
  }

  public static TPeriod ofWeeks(int weeks) {

    return create(0, 0, Math.multiplyExact(weeks, 7));
  }

  public static TPeriod ofDays(int days) {

    return create(0, 0, days);
  }

  public static TPeriod of(int years, int months, int days) {

    return create(years, months, days);
  }

  public static TPeriod from(TTemporalAmount amount) {

    if (amount instanceof TPeriod) {
      return (TPeriod) amount;
    }
    if (amount instanceof TChronoPeriod) {
      if (TIsoChronology.INSTANCE.equals(((TChronoPeriod) amount).getChronology()) == false) {
        throw new TDateTimeException("Period requires ISO chronology: " + amount);
      }
    }
    TObjects.requireNonNull(amount, "amount");
    int years = 0;
    int months = 0;
    int days = 0;
    for (TTemporalUnit unit : amount.getUnits()) {
      long unitAmount = amount.get(unit);
      if (unit == TChronoUnit.YEARS) {
        years = Math.toIntExact(unitAmount);
      } else if (unit == TChronoUnit.MONTHS) {
        months = Math.toIntExact(unitAmount);
      } else if (unit == TChronoUnit.DAYS) {
        days = Math.toIntExact(unitAmount);
      } else {
        throw new TDateTimeException("Unit must be Years, Months or Days, but was " + unit);
      }
    }
    return create(years, months, days);
  }

  public static TPeriod parse(CharSequence text) {

    TObjects.requireNonNull(text, "text");
    TMatcher matcher = PATTERN.matcher(text);
    if (matcher.matches()) {
      int negate = (charMatch(text, matcher.start(1), matcher.end(1), '-') ? -1 : 1);
      int yearStart = matcher.start(2), yearEnd = matcher.end(2);
      int monthStart = matcher.start(3), monthEnd = matcher.end(3);
      int weekStart = matcher.start(4), weekEnd = matcher.end(4);
      int dayStart = matcher.start(5), dayEnd = matcher.end(5);
      if (yearStart >= 0 || monthStart >= 0 || weekStart >= 0 || dayStart >= 0) {
        try {
          int years = parseNumber(text, yearStart, yearEnd, negate);
          int months = parseNumber(text, monthStart, monthEnd, negate);
          int weeks = parseNumber(text, weekStart, weekEnd, negate);
          int days = parseNumber(text, dayStart, dayEnd, negate);
          days = Math.addExact(days, Math.multiplyExact(weeks, 7));
          return create(years, months, days);
        } catch (TNumberFormatException ex) {
          throw new TDateTimeParseException("Text cannot be parsed to a Period", text, 0, ex);
        }
      }
    }
    throw new TDateTimeParseException("Text cannot be parsed to a Period", text, 0);
  }

  private static boolean charMatch(CharSequence text, int start, int end, char c) {

    return (start >= 0 && end == start + 1 && text.charAt(start) == c);
  }

  private static int parseNumber(CharSequence text, int start, int end, int negate) {

    if (start < 0 || end < 0) {
      return 0;
    }
    int val = Integer.parseInt(text, start, end, 10);
    try {
      return Math.multiplyExact(val, negate);
    } catch (TArithmeticException ex) {
      throw new TDateTimeParseException("Text cannot be parsed to a Period", text, 0, ex);
    }
  }

  public static TPeriod between(TLocalDate startDateInclusive, TLocalDate endDateExclusive) {

    return startDateInclusive.until(endDateExclusive);
  }

  private static TPeriod create(int years, int months, int days) {

    if ((years | months | days) == 0) {
      return ZERO;
    }
    return new TPeriod(years, months, days);
  }

  private TPeriod(int years, int months, int days) {

    this.years = years;
    this.months = months;
    this.days = days;
  }

  @Override
  public long get(TTemporalUnit unit) {

    if (unit == TChronoUnit.YEARS) {
      return getYears();
    } else if (unit == TChronoUnit.MONTHS) {
      return getMonths();
    } else if (unit == TChronoUnit.DAYS) {
      return getDays();
    } else {
      throw new TUnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }
  }

  @Override
  public List<TTemporalUnit> getUnits() {

    return SUPPORTED_UNITS;
  }

  @Override
  public TIsoChronology getChronology() {

    return TIsoChronology.INSTANCE;
  }

  @Override
  public boolean isZero() {

    return (this == ZERO);
  }

  @Override
  public boolean isNegative() {

    return this.years < 0 || this.months < 0 || this.days < 0;
  }

  public int getYears() {

    return this.years;
  }

  public int getMonths() {

    return this.months;
  }

  public int getDays() {

    return this.days;
  }

  public TPeriod withYears(int years) {

    if (years == this.years) {
      return this;
    }
    return create(years, this.months, this.days);
  }

  public TPeriod withMonths(int months) {

    if (months == this.months) {
      return this;
    }
    return create(this.years, months, this.days);
  }

  public TPeriod withDays(int days) {

    if (days == this.days) {
      return this;
    }
    return create(this.years, this.months, days);
  }

  @Override
  public TPeriod plus(TTemporalAmount amountToAdd) {

    TPeriod isoAmount = TPeriod.from(amountToAdd);
    return create(Math.addExact(this.years, isoAmount.years), Math.addExact(this.months, isoAmount.months),
        Math.addExact(this.days, isoAmount.days));
  }

  public TPeriod plusYears(long yearsToAdd) {

    if (yearsToAdd == 0) {
      return this;
    }
    return create(Math.toIntExact(Math.addExact(this.years, yearsToAdd)), this.months, this.days);
  }

  public TPeriod plusMonths(long monthsToAdd) {

    if (monthsToAdd == 0) {
      return this;
    }
    return create(this.years, Math.toIntExact(Math.addExact(this.months, monthsToAdd)), this.days);
  }

  public TPeriod plusDays(long daysToAdd) {

    if (daysToAdd == 0) {
      return this;
    }
    return create(this.years, this.months, Math.toIntExact(Math.addExact(this.days, daysToAdd)));
  }

  @Override
  public TPeriod minus(TTemporalAmount amountToSubtract) {

    TPeriod isoAmount = TPeriod.from(amountToSubtract);
    return create(Math.subtractExact(this.years, isoAmount.years), Math.subtractExact(this.months, isoAmount.months),
        Math.subtractExact(this.days, isoAmount.days));
  }

  public TPeriod minusYears(long yearsToSubtract) {

    return (yearsToSubtract == Long.MIN_VALUE ? plusYears(Long.MAX_VALUE).plusYears(1) : plusYears(-yearsToSubtract));
  }

  public TPeriod minusMonths(long monthsToSubtract) {

    return (monthsToSubtract == Long.MIN_VALUE ? plusMonths(Long.MAX_VALUE).plusMonths(1)
        : plusMonths(-monthsToSubtract));
  }

  public TPeriod minusDays(long daysToSubtract) {

    return (daysToSubtract == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-daysToSubtract));
  }

  @Override
  public TPeriod multipliedBy(int scalar) {

    if (this == ZERO || scalar == 1) {
      return this;
    }
    return create(Math.multiplyExact(this.years, scalar), Math.multiplyExact(this.months, scalar),
        Math.multiplyExact(this.days, scalar));
  }

  @Override
  public TPeriod negated() {

    return multipliedBy(-1);
  }

  @Override
  public TPeriod normalized() {

    long totalMonths = toTotalMonths();
    long splitYears = totalMonths / 12;
    int splitMonths = (int) (totalMonths % 12);
    if (splitYears == this.years && splitMonths == this.months) {
      return this;
    }
    return create(Math.toIntExact(splitYears), splitMonths, this.days);
  }

  public long toTotalMonths() {

    return this.years * 12L + this.months;
  }

  @Override
  public TTemporal addTo(TTemporal temporal) {

    validateChrono(temporal);
    if (this.months == 0) {
      if (this.years != 0) {
        temporal = temporal.plus(this.years, YEARS);
      }
    } else {
      long totalMonths = toTotalMonths();
      if (totalMonths != 0) {
        temporal = temporal.plus(totalMonths, MONTHS);
      }
    }
    if (this.days != 0) {
      temporal = temporal.plus(this.days, DAYS);
    }
    return temporal;
  }

  @Override
  public TTemporal subtractFrom(TTemporal temporal) {

    validateChrono(temporal);
    if (this.months == 0) {
      if (this.years != 0) {
        temporal = temporal.minus(this.years, YEARS);
      }
    } else {
      long totalMonths = toTotalMonths();
      if (totalMonths != 0) {
        temporal = temporal.minus(totalMonths, MONTHS);
      }
    }
    if (this.days != 0) {
      temporal = temporal.minus(this.days, DAYS);
    }
    return temporal;
  }

  private void validateChrono(TTemporalAccessor temporal) {

    Objects.requireNonNull(temporal, "temporal");
    TChronology temporalChrono = temporal.query(TTemporalQueries.chronology());
    if (temporalChrono != null && TIsoChronology.INSTANCE.equals(temporalChrono) == false) {
      throw new TDateTimeException("Chronology mismatch, expected: ISO, actual: " + temporalChrono.getId());
    }
  }

  @Override
  public boolean equals(Object obj) {

    if (this == obj) {
      return true;
    }
    if (obj instanceof TPeriod) {
      TPeriod other = (TPeriod) obj;
      return this.years == other.years && this.months == other.months && this.days == other.days;
    }
    return false;
  }

  @Override
  public int hashCode() {

    return this.years + Integer.rotateLeft(this.months, 8) + Integer.rotateLeft(this.days, 16);
  }

  @Override
  public String toString() {

    if (this == ZERO) {
      return "P0D";
    } else {
      TStringBuilder buf = new TStringBuilder();
      buf.append('P');
      if (this.years != 0) {
        buf.append(this.years).append('Y');
      }
      if (this.months != 0) {
        buf.append(this.months).append('M');
      }
      if (this.days != 0) {
        buf.append(this.days).append('D');
      }
      return buf.toString();
    }
  }
}
