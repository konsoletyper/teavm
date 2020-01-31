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
package org.teavm.classlib.java.time.chrono;

import static org.teavm.classlib.java.time.chrono.TThaiBuddhistChronology.YEARS_DIFFERENCE;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.teavm.classlib.java.time.TClock;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TPeriod;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.temporal.TValueRange;

public final class TThaiBuddhistDate
        extends ChronoDateImpl<TThaiBuddhistDate>
        implements Serializable {

    private static final long serialVersionUID = -8722293800195731463L;

    private final TLocalDate isoDate;

    //-----------------------------------------------------------------------
    public static TThaiBuddhistDate now() {
        return now(TClock.systemDefaultZone());
    }

    public static TThaiBuddhistDate now(TZoneId zone) {
        return now(TClock.system(zone));
    }

    public static TThaiBuddhistDate now(TClock clock) {
        return new TThaiBuddhistDate(TLocalDate.now(clock));
    }

    public static TThaiBuddhistDate of(int prolepticYear, int month, int dayOfMonth) {
        return TThaiBuddhistChronology.INSTANCE.date(prolepticYear, month, dayOfMonth);
    }

    public static TThaiBuddhistDate from(TTemporalAccessor temporal) {
        return TThaiBuddhistChronology.INSTANCE.date(temporal);
    }

    //-----------------------------------------------------------------------
    TThaiBuddhistDate(TLocalDate date) {
        TJdk8Methods.requireNonNull(date, "date");
        this.isoDate = date;
    }

    //-----------------------------------------------------------------------
    @Override
    public TThaiBuddhistChronology getChronology() {
        return TThaiBuddhistChronology.INSTANCE;
    }

    @Override
    public TThaiBuddhistEra getEra() {
        return (TThaiBuddhistEra) super.getEra();
    }

    @Override
    public int lengthOfMonth() {
        return isoDate.lengthOfMonth();
    }

    @Override
    public TValueRange range(TTemporalField field) {
        if (field instanceof TChronoField) {
            if (isSupported(field)) {
                TChronoField f = (TChronoField) field;
                switch (f) {
                    case DAY_OF_MONTH:
                    case DAY_OF_YEAR:
                    case ALIGNED_WEEK_OF_MONTH:
                        return isoDate.range(field);
                    case YEAR_OF_ERA: {
                        TValueRange range = YEAR.range();
                        long max = (getProlepticYear() <= 0 ? -(range.getMinimum() + YEARS_DIFFERENCE) + 1 : range.getMaximum() + YEARS_DIFFERENCE);
                        return TValueRange.of(1, max);
                    }
                }
                return getChronology().range(f);
            }
            throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    @Override
    public long getLong(TTemporalField field) {
        if (field instanceof TChronoField) {
            switch ((TChronoField) field) {
                case PROLEPTIC_MONTH:
                    return getProlepticMonth();
                case YEAR_OF_ERA: {
                    int prolepticYear = getProlepticYear();
                    return (prolepticYear >= 1 ? prolepticYear : 1 - prolepticYear);
                }
                case YEAR:
                    return getProlepticYear();
                case ERA:
                    return (getProlepticYear() >= 1 ? 1 : 0);
            }
            return isoDate.getLong(field);
        }
        return field.getFrom(this);
    }

    private long getProlepticMonth() {
        return getProlepticYear() * 12L + isoDate.getMonthValue() - 1;
    }

    private int getProlepticYear() {
        return isoDate.getYear() + YEARS_DIFFERENCE;
    }

    //-----------------------------------------------------------------------
    @Override
    public TThaiBuddhistDate with(TTemporalAdjuster adjuster) {
        return (TThaiBuddhistDate) super.with(adjuster);
    }

    @Override
    public TThaiBuddhistDate with(TTemporalField field, long newValue) {
        if (field instanceof TChronoField) {
            TChronoField f = (TChronoField) field;
            if (getLong(f) == newValue) {
                return this;
            }
            switch (f) {
                case PROLEPTIC_MONTH:
                    getChronology().range(f).checkValidValue(newValue, f);
                    return plusMonths(newValue - getProlepticMonth());
                case YEAR_OF_ERA:
                case YEAR:
                case ERA: {
                    int nvalue = getChronology().range(f).checkValidIntValue(newValue, f);
                    switch (f) {
                        case YEAR_OF_ERA:
                            return with(isoDate.withYear((getProlepticYear() >= 1 ? nvalue : 1 - nvalue)  - YEARS_DIFFERENCE));
                        case YEAR:
                            return with(isoDate.withYear(nvalue - YEARS_DIFFERENCE));
                        case ERA:
                            return with(isoDate.withYear((1 - getProlepticYear()) - YEARS_DIFFERENCE));
                    }
                }
            }
            return with(isoDate.with(field, newValue));
        }
        return field.adjustInto(this, newValue);
    }

    @Override
    public TThaiBuddhistDate plus(TTemporalAmount amount) {
        return (TThaiBuddhistDate) super.plus(amount);
    }

    @Override
    public TThaiBuddhistDate plus(long amountToAdd, TTemporalUnit unit) {
        return (TThaiBuddhistDate) super.plus(amountToAdd, unit);
    }

    @Override
    public TThaiBuddhistDate minus(TTemporalAmount amount) {
        return (TThaiBuddhistDate) super.minus(amount);
    }

    @Override
    public TThaiBuddhistDate minus(long amountToAdd, TTemporalUnit unit) {
        return (TThaiBuddhistDate) super.minus(amountToAdd, unit);
    }

    //-----------------------------------------------------------------------
    @Override
    TThaiBuddhistDate plusYears(long years) {
        return with(isoDate.plusYears(years));
    }

    @Override
    TThaiBuddhistDate plusMonths(long months) {
        return with(isoDate.plusMonths(months));
    }

    @Override
    TThaiBuddhistDate plusDays(long days) {
        return with(isoDate.plusDays(days));
    }

    private TThaiBuddhistDate with(TLocalDate newDate) {
        return (newDate.equals(isoDate) ? this : new TThaiBuddhistDate(newDate));
    }

    @Override
    @SuppressWarnings("unchecked")
    public final TChronoLocalDateTime<TThaiBuddhistDate> atTime(TLocalTime localTime) {
        return (TChronoLocalDateTime<TThaiBuddhistDate>) super.atTime(localTime);
    }

    @Override
    public TChronoPeriod until(TChronoLocalDate endDate) {
        TPeriod period = isoDate.until(endDate);
        return getChronology().period(period.getYears(), period.getMonths(), period.getDays());
    }

    @Override  // override for performance
    public long toEpochDay() {
        return isoDate.toEpochDay();
    }

    //-------------------------------------------------------------------------
    @Override  // override for performance
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TThaiBuddhistDate) {
            TThaiBuddhistDate otherDate = (TThaiBuddhistDate) obj;
            return this.isoDate.equals(otherDate.isoDate);
        }
        return false;
    }

    @Override  // override for performance
    public int hashCode() {
        return getChronology().getId().hashCode() ^ isoDate.hashCode();
    }

    //-----------------------------------------------------------------------
    private Object writeReplace() {
        return new Ser(Ser.THAIBUDDHIST_DATE_TYPE, this);
    }

    void writeExternal(DataOutput out) throws IOException {
        // MinguoChrono is implicit in the THAIBUDDHIST_DATE_TYPE
        out.writeInt(this.get(YEAR));
        out.writeByte(this.get(MONTH_OF_YEAR));
        out.writeByte(this.get(DAY_OF_MONTH));
    }

    static TChronoLocalDate readExternal(DataInput in) throws IOException {
        int year = in.readInt();
        int month = in.readByte();
        int dayOfMonth = in.readByte();
        return TThaiBuddhistChronology.INSTANCE.date(year, month, dayOfMonth);
    }

}
