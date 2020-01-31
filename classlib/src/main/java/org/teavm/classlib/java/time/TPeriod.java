/*
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.teavm.classlib.java.time;

import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MONTHS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.YEARS;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.teavm.classlib.java.time.chrono.TChronoLocalDate;
import org.teavm.classlib.java.time.chrono.TChronoPeriod;
import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TDateTimeParseException;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;

public final class TPeriod
        extends TChronoPeriod
        implements Serializable {

    public static final TPeriod ZERO = new TPeriod(0, 0, 0);
    private static final long serialVersionUID = -8290556941213247973L;
    private final static Pattern PATTERN =
            Pattern.compile("([-+]?)P(?:([-+]?[0-9]+)Y)?(?:([-+]?[0-9]+)M)?(?:([-+]?[0-9]+)W)?(?:([-+]?[0-9]+)D)?", Pattern.CASE_INSENSITIVE);

    private final int years;
    private final int months;
    private final int days;

    //-----------------------------------------------------------------------
    public static TPeriod ofYears(int years) {
        return create(years, 0, 0);
    }

    public static TPeriod ofMonths(int months) {
        return create(0, months, 0);
    }

    public static TPeriod ofWeeks(int weeks) {
        return create(0, 0, TJdk8Methods.safeMultiply(weeks, 7));
    }

    public static TPeriod ofDays(int days) {
        return create(0, 0, days);
    }

    //-----------------------------------------------------------------------
    public static TPeriod of(int years, int months, int days) {
        return create(years, months, days);
    }

    //-----------------------------------------------------------------------
    public static TPeriod from(TTemporalAmount amount) {
        if (amount instanceof TPeriod) {
            return (TPeriod) amount;
        }
        if (amount instanceof TChronoPeriod) {
            if (TIsoChronology.INSTANCE.equals(((TChronoPeriod) amount).getChronology()) == false) {
                throw new TDateTimeException("TPeriod requires ISO chronology: " + amount);
            }
        }
        TJdk8Methods.requireNonNull(amount, "amount");
        int years = 0;
        int months = 0;
        int days = 0;
        for (TTemporalUnit unit : amount.getUnits()) {
            long unitAmount = amount.get(unit);
            if (unit == TChronoUnit.YEARS) {
                years = TJdk8Methods.safeToInt(unitAmount);
            } else if (unit == TChronoUnit.MONTHS) {
                months = TJdk8Methods.safeToInt(unitAmount);
            } else if (unit == TChronoUnit.DAYS) {
                days = TJdk8Methods.safeToInt(unitAmount);
            } else {
                throw new TDateTimeException("Unit must be Years, Months or Days, but was " + unit);
            }
        }
        return create(years, months, days);
    }

    //-----------------------------------------------------------------------
    public static TPeriod between(TLocalDate startDate, TLocalDate endDate) {
        return startDate.until(endDate);
    }

    //-----------------------------------------------------------------------
    public static TPeriod parse(CharSequence text) {
        TJdk8Methods.requireNonNull(text, "text");
        Matcher matcher = PATTERN.matcher(text);
        if (matcher.matches()) {
            int negate = ("-".equals(matcher.group(1)) ? -1 : 1);
            String yearMatch = matcher.group(2);
            String monthMatch = matcher.group(3);
            String weekMatch = matcher.group(4);
            String dayMatch = matcher.group(5);
            if (yearMatch != null || monthMatch != null || weekMatch != null || dayMatch != null) {
                try {
                    int years = parseNumber(text, yearMatch, negate);
                    int months = parseNumber(text, monthMatch, negate);
                    int weeks = parseNumber(text, weekMatch, negate);
                    int days = parseNumber(text, dayMatch, negate);
                    days = TJdk8Methods.safeAdd(days, TJdk8Methods.safeMultiply(weeks, 7));
                    return create(years, months, days);
                } catch (NumberFormatException ex) {
                    throw (TDateTimeParseException) new TDateTimeParseException("Text cannot be parsed to a TPeriod", text, 0).initCause(ex);
                }
            }
        }
        throw new TDateTimeParseException("Text cannot be parsed to a TPeriod", text, 0);
    }

    private static int parseNumber(CharSequence text, String str, int negate) {
        if (str == null) {
            return 0;
        }
        int val = Integer.parseInt(str);
        try {
            return TJdk8Methods.safeMultiply(val, negate);
        } catch (ArithmeticException ex) {
            throw (TDateTimeParseException) new TDateTimeParseException("Text cannot be parsed to a TPeriod", text, 0).initCause(ex);
        }
    }

    //-----------------------------------------------------------------------
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

    private Object readResolve() {
        if ((years | months | days) == 0) {
            return ZERO;
        }
        return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public List<TTemporalUnit> getUnits() {
        return Collections.<TTemporalUnit>unmodifiableList(Arrays.asList(YEARS, MONTHS, DAYS));
    }

    @Override
    public TChronology getChronology() {
        return TIsoChronology.INSTANCE;
    }

    @Override
    public long get(TTemporalUnit unit) {
        if (unit == YEARS) {
            return years;
        }
        if (unit == MONTHS) {
            return months;
        }
        if (unit == DAYS) {
            return days;
        }
        throw new TUnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    //-----------------------------------------------------------------------
    public boolean isZero() {
        return (this == ZERO);
    }

    public boolean isNegative() {
        return years < 0 || months < 0 || days < 0;
    }

    //-----------------------------------------------------------------------
    public int getYears() {
        return years;
    }

    public int getMonths() {
        return months;
    }

    public int getDays() {
        return days;
    }

    //-----------------------------------------------------------------------
    public TPeriod withYears(int years) {
        if (years == this.years) {
            return this;
        }
        return create(years, months, days);
    }

    public TPeriod withMonths(int months) {
        if (months == this.months) {
            return this;
        }
        return create(years, months, days);
    }

    public TPeriod withDays(int days) {
        if (days == this.days) {
            return this;
        }
        return create(years, months, days);
    }

    //-----------------------------------------------------------------------
    public TPeriod plus(TTemporalAmount amountToAdd) {
        TPeriod amount = TPeriod.from(amountToAdd);
        return create(
                TJdk8Methods.safeAdd(years, amount.years),
                TJdk8Methods.safeAdd(months, amount.months),
                TJdk8Methods.safeAdd(days, amount.days));
    }

    public TPeriod plusYears(long yearsToAdd) {
        if (yearsToAdd == 0) {
            return this;
        }
        return create(TJdk8Methods.safeToInt(TJdk8Methods.safeAdd(years, yearsToAdd)), months, days);
    }

    public TPeriod plusMonths(long monthsToAdd) {
        if (monthsToAdd == 0) {
            return this;
        }
        return create(years, TJdk8Methods.safeToInt(TJdk8Methods.safeAdd(months, monthsToAdd)), days);
    }

    public TPeriod plusDays(long daysToAdd) {
        if (daysToAdd == 0) {
            return this;
        }
        return create(years, months, TJdk8Methods.safeToInt(TJdk8Methods.safeAdd(days, daysToAdd)));
    }

    //-----------------------------------------------------------------------
    public TPeriod minus(TTemporalAmount amountToSubtract) {
        TPeriod amount = TPeriod.from(amountToSubtract);
        return create(
                TJdk8Methods.safeSubtract(years, amount.years),
                TJdk8Methods.safeSubtract(months, amount.months),
                TJdk8Methods.safeSubtract(days, amount.days));
    }

    public TPeriod minusYears(long yearsToSubtract) {
        return (yearsToSubtract == Long.MIN_VALUE ? plusYears(Long.MAX_VALUE).plusYears(1) : plusYears(-yearsToSubtract));
    }

    public TPeriod minusMonths(long monthsToSubtract) {
        return (monthsToSubtract == Long.MIN_VALUE ? plusMonths(Long.MAX_VALUE).plusMonths(1) : plusMonths(-monthsToSubtract));
    }

    public TPeriod minusDays(long daysToSubtract) {
        return (daysToSubtract == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-daysToSubtract));
    }

    //-----------------------------------------------------------------------
    public TPeriod multipliedBy(int scalar) {
        if (this == ZERO || scalar == 1) {
            return this;
        }
        return create(
                TJdk8Methods.safeMultiply(years, scalar),
                TJdk8Methods.safeMultiply(months, scalar),
                TJdk8Methods.safeMultiply(days, scalar));
    }

    public TPeriod negated() {
        return multipliedBy(-1);
    }

    //-----------------------------------------------------------------------
    public TPeriod normalized() {
        long totalMonths = toTotalMonths();
        long splitYears = totalMonths / 12;
        int splitMonths = (int) (totalMonths % 12);  // no overflow
        if (splitYears == years && splitMonths == months) {
            return this;
        }
        return create(TJdk8Methods.safeToInt(splitYears), splitMonths, days);
    }

    public long toTotalMonths() {
        return years * 12L + months;  // no overflow
    }

    //-------------------------------------------------------------------------
    @Override
    public TTemporal addTo(TTemporal temporal) {
        TJdk8Methods.requireNonNull(temporal, "temporal");
        if (years != 0) {
            if (months != 0) {
                temporal = temporal.plus(toTotalMonths(), MONTHS);
            } else {
                temporal = temporal.plus(years, YEARS);
            }
        } else if (months != 0) {
            temporal = temporal.plus(months, MONTHS);
        }
        if (days != 0) {
            temporal = temporal.plus(days, DAYS);
        }
        return temporal;
    }

    @Override
    public TTemporal subtractFrom(TTemporal temporal) {
        TJdk8Methods.requireNonNull(temporal, "temporal");
        if (years != 0) {
            if (months != 0) {
                temporal = temporal.minus(toTotalMonths(), MONTHS);
            } else {
                temporal = temporal.minus(years, YEARS);
            }
        } else if (months != 0) {
            temporal = temporal.minus(months, MONTHS);
        }
        if (days != 0) {
            temporal = temporal.minus(days, DAYS);
        }
        return temporal;
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TPeriod) {
            TPeriod other = (TPeriod) obj;
            return years == other.years &&
                    months == other.months &&
                    days == other.days;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return years + Integer.rotateLeft(months, 8) + Integer.rotateLeft(days, 16);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
        if (this == ZERO) {
            return "P0D";
        } else {
            StringBuilder buf = new StringBuilder();
            buf.append('P');
            if (years != 0) {
                buf.append(years).append('Y');
            }
            if (months != 0) {
                buf.append(months).append('M');
            }
            if (days != 0) {
                buf.append(days).append('D');
            }
            return buf.toString();
        }
    }

}
