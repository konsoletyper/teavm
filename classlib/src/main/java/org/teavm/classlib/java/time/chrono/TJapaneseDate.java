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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Calendar;

import org.teavm.classlib.java.io.TSerializable;
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
import org.teavm.classlib.java.time.temporal.TTemporalUnit;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.temporal.TValueRange;
import org.teavm.classlib.java.util.TCalendar;

public final class TJapaneseDate extends ChronoDateImpl<TJapaneseDate> implements TSerializable {

    static final TLocalDate MIN_DATE = TLocalDate.of(1873, 1, 1);

    private final TLocalDate isoDate;

    private transient TJapaneseEra era;

    private transient int yearOfEra;

    public static TJapaneseDate now() {

        return now(TClock.systemDefaultZone());
    }

    public static TJapaneseDate now(TZoneId zone) {

        return now(TClock.system(zone));
    }

    public static TJapaneseDate now(TClock clock) {

        return new TJapaneseDate(TLocalDate.now(clock));
    }

    public static TJapaneseDate of(TJapaneseEra era, int yearOfEra, int month, int dayOfMonth) {

        TJdk8Methods.requireNonNull(era, "era");
        if (yearOfEra < 1) {
            throw new TDateTimeException("Invalid YearOfEra: " + yearOfEra);
        }
        TLocalDate eraStartDate = era.startDate();
        TLocalDate eraEndDate = era.endDate();
        int yearOffset = eraStartDate.getYear() - 1;
        TLocalDate date = TLocalDate.of(yearOfEra + yearOffset, month, dayOfMonth);
        if (date.isBefore(eraStartDate) || date.isAfter(eraEndDate)) {
            throw new TDateTimeException("Requested date is outside bounds of era " + era);
        }
        return new TJapaneseDate(era, yearOfEra, date);
    }

    static TJapaneseDate ofYearDay(TJapaneseEra era, int yearOfEra, int dayOfYear) {

        TJdk8Methods.requireNonNull(era, "era");
        if (yearOfEra < 1) {
            throw new TDateTimeException("Invalid YearOfEra: " + yearOfEra);
        }
        TLocalDate eraStartDate = era.startDate();
        TLocalDate eraEndDate = era.endDate();
        if (yearOfEra == 1) {
            dayOfYear += eraStartDate.getDayOfYear() - 1;
            if (dayOfYear > eraStartDate.lengthOfYear()) {
                throw new TDateTimeException("DayOfYear exceeds maximum allowed in the first year of era " + era);
            }
        }
        int yearOffset = eraStartDate.getYear() - 1;
        TLocalDate isoDate = TLocalDate.ofYearDay(yearOfEra + yearOffset, dayOfYear);
        if (isoDate.isBefore(eraStartDate) || isoDate.isAfter(eraEndDate)) {
            throw new TDateTimeException("Requested date is outside bounds of era " + era);
        }
        return new TJapaneseDate(era, yearOfEra, isoDate);
    }

    public static TJapaneseDate of(int prolepticYear, int month, int dayOfMonth) {

        return new TJapaneseDate(TLocalDate.of(prolepticYear, month, dayOfMonth));
    }

    public static TJapaneseDate from(TTemporalAccessor temporal) {

        return TJapaneseChronology.INSTANCE.date(temporal);
    }

    TJapaneseDate(TLocalDate isoDate) {

        if (isoDate.isBefore(MIN_DATE)) {
            throw new TDateTimeException("Minimum supported date is January 1st Meiji 6");
        }
        this.era = TJapaneseEra.from(isoDate);
        int yearOffset = this.era.startDate().getYear() - 1;
        this.yearOfEra = isoDate.getYear() - yearOffset;
        this.isoDate = isoDate;
    }

    TJapaneseDate(TJapaneseEra era, int year, TLocalDate isoDate) {

        if (isoDate.isBefore(MIN_DATE)) {
            throw new TDateTimeException("Minimum supported date is January 1st Meiji 6");
        }
        this.era = era;
        this.yearOfEra = year;
        this.isoDate = isoDate;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {

        stream.defaultReadObject();
        this.era = TJapaneseEra.from(this.isoDate);
        int yearOffset = this.era.startDate().getYear() - 1;
        this.yearOfEra = this.isoDate.getYear() - yearOffset;
    }

    @Override
    public TJapaneseChronology getChronology() {

        return TJapaneseChronology.INSTANCE;
    }

    @Override
    public TJapaneseEra getEra() {

        return this.era;
    }

    @Override
    public int lengthOfMonth() {

        return this.isoDate.lengthOfMonth();
    }

    @Override
    public int lengthOfYear() {

        Calendar jcal = Calendar.getInstance(TJapaneseChronology.LOCALE);
        jcal.set(Calendar.ERA, this.era.getValue() + TJapaneseEra.ERA_OFFSET);
        jcal.set(this.yearOfEra, this.isoDate.getMonthValue() - 1, this.isoDate.getDayOfMonth());
        return jcal.getActualMaximum(TCalendar.DAY_OF_YEAR);
    }

    @Override
    public boolean isSupported(TTemporalField field) {

        if (field == TChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH || field == TChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR
                || field == TChronoField.ALIGNED_WEEK_OF_MONTH || field == TChronoField.ALIGNED_WEEK_OF_YEAR) {
            return false;
        }
        return super.isSupported(field);
    }

    @Override
    public TValueRange range(TTemporalField field) {

        if (field instanceof TChronoField) {
            if (isSupported(field)) {
                TChronoField f = (TChronoField) field;
                switch (f) {
                    case DAY_OF_YEAR:
                        return actualRange(TCalendar.DAY_OF_YEAR);
                    case YEAR_OF_ERA:
                        return actualRange(TCalendar.YEAR);
                }
                return getChronology().range(f);
            }
            throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    private TValueRange actualRange(int calendarField) {

        Calendar jcal = Calendar.getInstance(TJapaneseChronology.LOCALE);
        jcal.set(TCalendar.ERA, this.era.getValue() + TJapaneseEra.ERA_OFFSET);
        jcal.set(this.yearOfEra, this.isoDate.getMonthValue() - 1, this.isoDate.getDayOfMonth());
        return TValueRange.of(jcal.getActualMinimum(calendarField), jcal.getActualMaximum(calendarField));
    }

    @Override
    public long getLong(TTemporalField field) {

        if (field instanceof TChronoField) {
            switch ((TChronoField) field) {
                case ALIGNED_DAY_OF_WEEK_IN_MONTH:
                case ALIGNED_DAY_OF_WEEK_IN_YEAR:
                case ALIGNED_WEEK_OF_MONTH:
                case ALIGNED_WEEK_OF_YEAR:
                    throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
                case YEAR_OF_ERA:
                    return this.yearOfEra;
                case ERA:
                    return this.era.getValue();
                case DAY_OF_YEAR:
                    return getDayOfYear();
            }
            return this.isoDate.getLong(field);
        }
        return field.getFrom(this);
    }

    private long getDayOfYear() {

        if (this.yearOfEra == 1) {
            return this.isoDate.getDayOfYear() - this.era.startDate().getDayOfYear() + 1;
        }
        return this.isoDate.getDayOfYear();
    }

    @Override
    public TJapaneseDate with(TTemporalAdjuster adjuster) {

        return (TJapaneseDate) super.with(adjuster);
    }

    @Override
    public TJapaneseDate with(TTemporalField field, long newValue) {

        if (field instanceof TChronoField) {
            TChronoField f = (TChronoField) field;
            if (getLong(f) == newValue) { // validates unsupported fields
                return this;
            }
            switch (f) {
                case DAY_OF_YEAR:
                case YEAR_OF_ERA:
                case ERA: {
                    int nvalue = getChronology().range(f).checkValidIntValue(newValue, f);
                    switch (f) {
                        case DAY_OF_YEAR:
                            return with(this.isoDate.plusDays(nvalue - getDayOfYear()));
                        case YEAR_OF_ERA:
                            return this.withYear(nvalue);
                        case ERA: {
                            return this.withYear(TJapaneseEra.of(nvalue), this.yearOfEra);
                        }
                    }
                }
            }
            return with(this.isoDate.with(field, newValue));
        }
        return field.adjustInto(this, newValue);
    }

    @Override
    public TJapaneseDate plus(TTemporalAmount amount) {

        return (TJapaneseDate) super.plus(amount);
    }

    @Override
    public TJapaneseDate plus(long amountToAdd, TTemporalUnit unit) {

        return (TJapaneseDate) super.plus(amountToAdd, unit);
    }

    @Override
    public TJapaneseDate minus(TTemporalAmount amount) {

        return (TJapaneseDate) super.minus(amount);
    }

    @Override
    public TJapaneseDate minus(long amountToAdd, TTemporalUnit unit) {

        return (TJapaneseDate) super.minus(amountToAdd, unit);
    }

    private TJapaneseDate withYear(TJapaneseEra era, int yearOfEra) {

        int year = TJapaneseChronology.INSTANCE.prolepticYear(era, yearOfEra);
        return with(this.isoDate.withYear(year));
    }

    private TJapaneseDate withYear(int year) {

        return withYear(getEra(), year);
    }

    @Override
    TJapaneseDate plusYears(long years) {

        return with(this.isoDate.plusYears(years));
    }

    @Override
    TJapaneseDate plusMonths(long months) {

        return with(this.isoDate.plusMonths(months));
    }

    @Override
    TJapaneseDate plusDays(long days) {

        return with(this.isoDate.plusDays(days));
    }

    private TJapaneseDate with(TLocalDate newDate) {

        return (newDate.equals(this.isoDate) ? this : new TJapaneseDate(newDate));
    }

    @Override
    @SuppressWarnings("unchecked")
    public final TChronoLocalDateTime<TJapaneseDate> atTime(TLocalTime localTime) {

        return (TChronoLocalDateTime<TJapaneseDate>) super.atTime(localTime);
    }

    @Override
    public TChronoPeriod until(TChronoLocalDate endDate) {

        TPeriod period = this.isoDate.until(endDate);
        return getChronology().period(period.getYears(), period.getMonths(), period.getDays());
    }

    @Override
    public long toEpochDay() {

        return this.isoDate.toEpochDay();
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj instanceof TJapaneseDate) {
            TJapaneseDate otherDate = (TJapaneseDate) obj;
            return this.isoDate.equals(otherDate.isoDate);
        }
        return false;
    }

    @Override
    public int hashCode() {

        return getChronology().getId().hashCode() ^ this.isoDate.hashCode();
    }

}
