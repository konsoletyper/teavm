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

import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_WEEK_OF_MONTH;
import static org.teavm.classlib.java.time.temporal.TChronoField.ALIGNED_WEEK_OF_YEAR;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.teavm.classlib.java.time.TClock;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TDayOfWeek;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.temporal.TValueRange;

public final class THijrahDate extends TChronoLocalDateImpl<THijrahDate> {
    // this class is package-scoped so that future conversion to public
    // would not change serialization

    private static final long serialVersionUID = -5207853542612002020L;

    public static final int MIN_VALUE_OF_ERA = 1;

    public static final int MAX_VALUE_OF_ERA = 9999;

    private static final int NUM_DAYS[] = { 0, 30, 59, 89, 118, 148, 177, 207, 236, 266, 295, 325 };

    private static final int LEAP_NUM_DAYS[] = { 0, 30, 59, 89, 118, 148, 177, 207, 236, 266, 295, 325 };

    private static final int MONTH_LENGTH[] = { 30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, 29 };

    private static final int LEAP_MONTH_LENGTH[] = { 30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, 30 };

    private static final int MIN_VALUES[] = { 0, MIN_VALUE_OF_ERA, 0, 1, 0, 1, 1 };

    private static final int LEAST_MAX_VALUES[] = { 1, MAX_VALUE_OF_ERA, 11, 51, 5, 29, 354 };

    private static final int MAX_VALUES[] = { 1, MAX_VALUE_OF_ERA, 11, 52, 6, 30, 355 };

    private static final int POSITION_DAY_OF_MONTH = 5;

    private static final int POSITION_DAY_OF_YEAR = 6;

    private static final int CYCLEYEAR_START_DATE[] = { 0, 354, 709, 1063, 1417, 1772, 2126, 2481, 2835, 3189, 3544,
    3898, 4252, 4607, 4961, 5315, 5670, 6024, 6379, 6733, 7087, 7442, 7796, 8150, 8505, 8859, 9214, 9568, 9922, 10277 };

    private static final char FILE_SEP = File.separatorChar;

    private static final String PATH_SEP = File.pathSeparator;

    private static final String DEFAULT_CONFIG_FILENAME = "hijrah_deviation.cfg";

    private static final String DEFAULT_CONFIG_PATH = "org" + FILE_SEP + "threeten" + FILE_SEP + "bp" + FILE_SEP
            + "chrono";

    private static final HashMap<Integer, Integer[]> ADJUSTED_MONTH_DAYS = new HashMap<Integer, Integer[]>();

    private static final HashMap<Integer, Integer[]> ADJUSTED_MONTH_LENGTHS = new HashMap<Integer, Integer[]>();

    private static final HashMap<Integer, Integer[]> ADJUSTED_CYCLE_YEARS = new HashMap<Integer, Integer[]>();

    private static final Long[] ADJUSTED_CYCLES;

    private static final Integer[] ADJUSTED_MIN_VALUES;

    private static final Integer[] ADJUSTED_LEAST_MAX_VALUES;

    private static final Integer[] ADJUSTED_MAX_VALUES;

    private static final Integer[] DEFAULT_MONTH_DAYS;

    private static final Integer[] DEFAULT_LEAP_MONTH_DAYS;

    private static final Integer[] DEFAULT_MONTH_LENGTHS;

    private static final Integer[] DEFAULT_LEAP_MONTH_LENGTHS;

    private static final Integer[] DEFAULT_CYCLE_YEARS;

    private static final int MAX_ADJUSTED_CYCLE = 334; // to support year 9999

    static { // Initialize the static integer array;

        DEFAULT_MONTH_DAYS = new Integer[NUM_DAYS.length];
        for (int i = 0; i < NUM_DAYS.length; i++) {
            DEFAULT_MONTH_DAYS[i] = Integer.valueOf(NUM_DAYS[i]);
        }

        DEFAULT_LEAP_MONTH_DAYS = new Integer[LEAP_NUM_DAYS.length];
        for (int i = 0; i < LEAP_NUM_DAYS.length; i++) {
            DEFAULT_LEAP_MONTH_DAYS[i] = Integer.valueOf(LEAP_NUM_DAYS[i]);
        }

        DEFAULT_MONTH_LENGTHS = new Integer[MONTH_LENGTH.length];
        for (int i = 0; i < MONTH_LENGTH.length; i++) {
            DEFAULT_MONTH_LENGTHS[i] = Integer.valueOf(MONTH_LENGTH[i]);
        }

        DEFAULT_LEAP_MONTH_LENGTHS = new Integer[LEAP_MONTH_LENGTH.length];
        for (int i = 0; i < LEAP_MONTH_LENGTH.length; i++) {
            DEFAULT_LEAP_MONTH_LENGTHS[i] = Integer.valueOf(LEAP_MONTH_LENGTH[i]);
        }

        DEFAULT_CYCLE_YEARS = new Integer[CYCLEYEAR_START_DATE.length];
        for (int i = 0; i < CYCLEYEAR_START_DATE.length; i++) {
            DEFAULT_CYCLE_YEARS[i] = Integer.valueOf(CYCLEYEAR_START_DATE[i]);
        }

        ADJUSTED_CYCLES = new Long[MAX_ADJUSTED_CYCLE];
        for (int i = 0; i < ADJUSTED_CYCLES.length; i++) {
            ADJUSTED_CYCLES[i] = Long.valueOf(10631 * i);
        }
        // Initialize min values, least max values and max values.
        ADJUSTED_MIN_VALUES = new Integer[MIN_VALUES.length];
        for (int i = 0; i < MIN_VALUES.length; i++) {
            ADJUSTED_MIN_VALUES[i] = Integer.valueOf(MIN_VALUES[i]);
        }
        ADJUSTED_LEAST_MAX_VALUES = new Integer[LEAST_MAX_VALUES.length];
        for (int i = 0; i < LEAST_MAX_VALUES.length; i++) {
            ADJUSTED_LEAST_MAX_VALUES[i] = Integer.valueOf(LEAST_MAX_VALUES[i]);
        }
        ADJUSTED_MAX_VALUES = new Integer[MAX_VALUES.length];
        for (int i = 0; i < MAX_VALUES.length; i++) {
            ADJUSTED_MAX_VALUES[i] = Integer.valueOf(MAX_VALUES[i]);
        }
        try {
            readDeviationConfig();
        } catch (IOException e) {
            // do nothing. Ignore deviation config.
            // e.printStackTrace();
        } catch (ParseException e) {
            // do nothing. Ignore deviation config.
            // e.printStackTrace();
        }
    }

    private static final int HIJRAH_JAN_1_1_GREGORIAN_DAY = -492148;

    private final transient THijrahEra era;

    private final transient int yearOfEra;

    private final transient int monthOfYear;

    private final transient int dayOfMonth;

    private final transient int dayOfYear;

    private final transient TDayOfWeek dayOfWeek;

    private final long gregorianEpochDay;

    private final transient boolean isLeapYear;

    public static THijrahDate now() {

        return now(TClock.systemDefaultZone());
    }

    public static THijrahDate now(TZoneId zone) {

        return now(TClock.system(zone));
    }

    public static THijrahDate now(TClock clock) {

        return THijrahChronology.INSTANCE.dateNow(clock);
    }

    public static THijrahDate of(int prolepticYear, int monthOfYear, int dayOfMonth) {

        return (prolepticYear >= 1) ? THijrahDate.of(THijrahEra.AH, prolepticYear, monthOfYear, dayOfMonth)
                : THijrahDate.of(THijrahEra.BEFORE_AH, 1 - prolepticYear, monthOfYear, dayOfMonth);
    }

    static THijrahDate of(THijrahEra era, int yearOfEra, int monthOfYear, int dayOfMonth) {

        Objects.requireNonNull(era, "era");
        checkValidYearOfEra(yearOfEra);
        checkValidMonth(monthOfYear);
        checkValidDayOfMonth(dayOfMonth);
        long gregorianDays = getGregorianEpochDay(era.prolepticYear(yearOfEra), monthOfYear, dayOfMonth);
        return new THijrahDate(gregorianDays);
    }

    private static void checkValidYearOfEra(int yearOfEra) {

        if (yearOfEra < MIN_VALUE_OF_ERA || yearOfEra > MAX_VALUE_OF_ERA) {
            throw new TDateTimeException("Invalid year of Hijrah TEra");
        }
    }

    private static void checkValidDayOfYear(int dayOfYear) {

        if (dayOfYear < 1 || dayOfYear > getMaximumDayOfYear()) {
            throw new TDateTimeException("Invalid day of year of Hijrah date");
        }
    }

    private static void checkValidMonth(int month) {

        if (month < 1 || month > 12) {
            throw new TDateTimeException("Invalid month of Hijrah date");
        }
    }

    private static void checkValidDayOfMonth(int dayOfMonth) {

        if (dayOfMonth < 1 || dayOfMonth > getMaximumDayOfMonth()) {
            throw new TDateTimeException("Invalid day of month of Hijrah date, day " + dayOfMonth + " greater than "
                    + getMaximumDayOfMonth() + " or less than 1");
        }
    }

    static THijrahDate of(TLocalDate date) {

        long gregorianDays = date.toEpochDay();
        return new THijrahDate(gregorianDays);
    }

    static THijrahDate ofEpochDay(long epochDay) {

        return new THijrahDate(epochDay);
    }

    public static THijrahDate from(TTemporalAccessor temporal) {

        return THijrahChronology.INSTANCE.date(temporal);
    }

    private THijrahDate(long gregorianDay) {

        int[] dateInfo = getHijrahDateInfo(gregorianDay);

        checkValidYearOfEra(dateInfo[1]);
        checkValidMonth(dateInfo[2]);
        checkValidDayOfMonth(dateInfo[3]);
        checkValidDayOfYear(dateInfo[4]);

        this.era = THijrahEra.of(dateInfo[0]);
        this.yearOfEra = dateInfo[1];
        this.monthOfYear = dateInfo[2];
        this.dayOfMonth = dateInfo[3];
        this.dayOfYear = dateInfo[4];
        this.dayOfWeek = TDayOfWeek.of(dateInfo[5]);
        this.gregorianEpochDay = gregorianDay;
        this.isLeapYear = isLeapYear(this.yearOfEra);
    }

    @Override
    public THijrahChronology getChronology() {

        return THijrahChronology.INSTANCE;
    }

    @Override
    public THijrahEra getEra() {

        return this.era;
    }

    @Override
    public TValueRange range(TTemporalField field) {

        if (field instanceof TChronoField) {
            if (isSupported(field)) {
                TChronoField f = (TChronoField) field;
                switch (f) {
                    case DAY_OF_MONTH:
                        return TValueRange.of(1, lengthOfMonth());
                    case DAY_OF_YEAR:
                        return TValueRange.of(1, lengthOfYear());
                    case ALIGNED_WEEK_OF_MONTH:
                        return TValueRange.of(1, 5); // TODO
                    case YEAR_OF_ERA:
                        return TValueRange.of(1, 1000); // TODO
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
                case DAY_OF_WEEK:
                    return this.dayOfWeek.getValue();
                case ALIGNED_DAY_OF_WEEK_IN_MONTH:
                    return ((this.dayOfMonth - 1) % 7) + 1;
                case ALIGNED_DAY_OF_WEEK_IN_YEAR:
                    return ((this.dayOfYear - 1) % 7) + 1;
                case DAY_OF_MONTH:
                    return this.dayOfMonth;
                case DAY_OF_YEAR:
                    return this.dayOfYear;
                case EPOCH_DAY:
                    return toEpochDay();
                case ALIGNED_WEEK_OF_MONTH:
                    return ((this.dayOfMonth - 1) / 7) + 1;
                case ALIGNED_WEEK_OF_YEAR:
                    return ((this.dayOfYear - 1) / 7) + 1;
                case MONTH_OF_YEAR:
                    return this.monthOfYear;
                case YEAR_OF_ERA:
                    return this.yearOfEra;
                case YEAR:
                    return this.yearOfEra;
                case ERA:
                    return this.era.getValue();
            }
            throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.getFrom(this);
    }

    @Override
    public THijrahDate with(TTemporalAdjuster adjuster) {

        return (THijrahDate) super.with(adjuster);
    }

    @Override
    public THijrahDate with(TTemporalField field, long newValue) {

        if (field instanceof TChronoField) {
            TChronoField f = (TChronoField) field;
            f.checkValidValue(newValue); // TODO: validate value
            int nvalue = (int) newValue;
            switch (f) {
                case DAY_OF_WEEK:
                    return plusDays(newValue - this.dayOfWeek.getValue());
                case ALIGNED_DAY_OF_WEEK_IN_MONTH:
                    return plusDays(newValue - getLong(ALIGNED_DAY_OF_WEEK_IN_MONTH));
                case ALIGNED_DAY_OF_WEEK_IN_YEAR:
                    return plusDays(newValue - getLong(ALIGNED_DAY_OF_WEEK_IN_YEAR));
                case DAY_OF_MONTH:
                    return resolvePreviousValid(this.yearOfEra, this.monthOfYear, nvalue);
                case DAY_OF_YEAR:
                    return resolvePreviousValid(this.yearOfEra, ((nvalue - 1) / 30) + 1, ((nvalue - 1) % 30) + 1);
                case EPOCH_DAY:
                    return new THijrahDate(nvalue);
                case ALIGNED_WEEK_OF_MONTH:
                    return plusDays((newValue - getLong(ALIGNED_WEEK_OF_MONTH)) * 7);
                case ALIGNED_WEEK_OF_YEAR:
                    return plusDays((newValue - getLong(ALIGNED_WEEK_OF_YEAR)) * 7);
                case MONTH_OF_YEAR:
                    return resolvePreviousValid(this.yearOfEra, nvalue, this.dayOfMonth);
                case YEAR_OF_ERA:
                    return resolvePreviousValid(this.yearOfEra >= 1 ? nvalue : 1 - nvalue, this.monthOfYear,
                            this.dayOfMonth);
                case YEAR:
                    return resolvePreviousValid(nvalue, this.monthOfYear, this.dayOfMonth);
                case ERA:
                    return resolvePreviousValid(1 - this.yearOfEra, this.monthOfYear, this.dayOfMonth);
            }
            throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.adjustInto(this, newValue);
    }

    private static THijrahDate resolvePreviousValid(int yearOfEra, int month, int day) {

        int monthDays = getMonthDays(month - 1, yearOfEra);
        if (day > monthDays) {
            day = monthDays;
        }
        return THijrahDate.of(yearOfEra, month, day);
    }

    @Override
    public THijrahDate plus(TTemporalAmount amount) {

        return (THijrahDate) super.plus(amount);
    }

    @Override
    public THijrahDate plus(long amountToAdd, TTemporalUnit unit) {

        return (THijrahDate) super.plus(amountToAdd, unit);
    }

    @Override
    public THijrahDate minus(TTemporalAmount amount) {

        return (THijrahDate) super.minus(amount);
    }

    @Override
    public THijrahDate minus(long amountToAdd, TTemporalUnit unit) {

        return (THijrahDate) super.minus(amountToAdd, unit);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final TChronoLocalDateTime<THijrahDate> atTime(TLocalTime localTime) {

        return (TChronoLocalDateTime<THijrahDate>) super.atTime(localTime);
    }

    @Override
    public long toEpochDay() {

        return getGregorianEpochDay(this.yearOfEra, this.monthOfYear, this.dayOfMonth);
    }

    @Override
    public boolean isLeapYear() {

        return this.isLeapYear;
    }

    @Override
    THijrahDate plusYears(long years) {

        if (years == 0) {
            return this;
        }
        int newYear = Math.addExact(this.yearOfEra, (int) years);
        return THijrahDate.of(this.era, newYear, this.monthOfYear, this.dayOfMonth);
    }

    @Override
    THijrahDate plusMonths(long months) {

        if (months == 0) {
            return this;
        }
        int newMonth = this.monthOfYear - 1;
        newMonth = newMonth + (int) months;
        int years = newMonth / 12;
        newMonth = newMonth % 12;
        while (newMonth < 0) {
            newMonth += 12;
            years = Math.subtractExact(years, 1);
        }
        int newYear = Math.addExact(this.yearOfEra, years);
        return THijrahDate.of(this.era, newYear, newMonth + 1, this.dayOfMonth);
    }

    @Override
    THijrahDate plusDays(long days) {

        return new THijrahDate(this.gregorianEpochDay + days);
    }

    private static int[] getHijrahDateInfo(long gregorianDays) {

        int era, year, month, date, dayOfWeek, dayOfYear;

        int cycleNumber, yearInCycle, dayOfCycle;

        long epochDay = gregorianDays - HIJRAH_JAN_1_1_GREGORIAN_DAY;

        if (epochDay >= 0) {
            cycleNumber = getCycleNumber(epochDay); // 0 - 99.
            dayOfCycle = getDayOfCycle(epochDay, cycleNumber); // 0 - 10631.
            yearInCycle = getYearInCycle(cycleNumber, dayOfCycle); // 0 - 29.
            dayOfYear = getDayOfYear(cycleNumber, dayOfCycle, yearInCycle);
            // 0 - 354/355
            year = cycleNumber * 30 + yearInCycle + 1; // 1-based year.
            month = getMonthOfYear(dayOfYear, year); // 0-based month-of-year
            date = getDayOfMonth(dayOfYear, month, year); // 0-based date
            ++date; // Convert from 0-based to 1-based
            era = THijrahEra.AH.getValue();
        } else {
            cycleNumber = (int) epochDay / 10631; // 0 or negative number.
            dayOfCycle = (int) epochDay % 10631; // -10630 - 0.
            if (dayOfCycle == 0) {
                dayOfCycle = -10631;
                cycleNumber++;
            }
            yearInCycle = getYearInCycle(cycleNumber, dayOfCycle); // 0 - 29.
            dayOfYear = getDayOfYear(cycleNumber, dayOfCycle, yearInCycle);
            year = cycleNumber * 30 - yearInCycle; // negative number.
            year = 1 - year;
            dayOfYear = (isLeapYear(year) ? (dayOfYear + 355) : (dayOfYear + 354));
            month = getMonthOfYear(dayOfYear, year);
            date = getDayOfMonth(dayOfYear, month, year);
            ++date; // Convert from 0-based to 1-based
            era = THijrahEra.BEFORE_AH.getValue();
        }
        // Hijrah day zero is a Friday
        dayOfWeek = (int) ((epochDay + 5) % 7);
        dayOfWeek += (dayOfWeek <= 0) ? 7 : 0;

        int dateInfo[] = new int[6];
        dateInfo[0] = era;
        dateInfo[1] = year;
        dateInfo[2] = month + 1; // change to 1-based.
        dateInfo[3] = date;
        dateInfo[4] = dayOfYear + 1; // change to 1-based.
        dateInfo[5] = dayOfWeek;
        return dateInfo;
    }

    private static long getGregorianEpochDay(int prolepticYear, int monthOfYear, int dayOfMonth) {

        long day = yearToGregorianEpochDay(prolepticYear);
        day += getMonthDays(monthOfYear - 1, prolepticYear);
        day += dayOfMonth;
        return day;
    }

    private static long yearToGregorianEpochDay(int prolepticYear) {

        int cycleNumber = (prolepticYear - 1) / 30; // 0-based.
        int yearInCycle = (prolepticYear - 1) % 30; // 0-based.

        int dayInCycle = getAdjustedCycle(cycleNumber)[Math.abs(yearInCycle)].intValue();

        if (yearInCycle < 0) {
            dayInCycle = -dayInCycle;
        }

        Long cycleDays;

        try {
            cycleDays = ADJUSTED_CYCLES[cycleNumber];
        } catch (ArrayIndexOutOfBoundsException e) {
            cycleDays = null;
        }

        if (cycleDays == null) {
            cycleDays = Long.valueOf(cycleNumber * 10631);
        }

        return (cycleDays.longValue() + dayInCycle + HIJRAH_JAN_1_1_GREGORIAN_DAY - 1);
    }

    private static int getCycleNumber(long epochDay) {

        Long[] days = ADJUSTED_CYCLES;
        int cycleNumber;
        try {
            for (int i = 0; i < days.length; i++) {
                if (epochDay < days[i].longValue()) {
                    return i - 1;
                }
            }
            cycleNumber = (int) epochDay / 10631;
        } catch (ArrayIndexOutOfBoundsException e) {
            cycleNumber = (int) epochDay / 10631;
        }
        return cycleNumber;
    }

    private static int getDayOfCycle(long epochDay, int cycleNumber) {

        Long day;

        try {
            day = ADJUSTED_CYCLES[cycleNumber];
        } catch (ArrayIndexOutOfBoundsException e) {
            day = null;
        }
        if (day == null) {
            day = Long.valueOf(cycleNumber * 10631);
        }
        return (int) (epochDay - day.longValue());
    }

    private static int getYearInCycle(int cycleNumber, long dayOfCycle) {

        Integer[] cycles = getAdjustedCycle(cycleNumber);
        if (dayOfCycle == 0) {
            return 0;
        }

        if (dayOfCycle > 0) {
            for (int i = 0; i < cycles.length; i++) {
                if (dayOfCycle < cycles[i].intValue()) {
                    return i - 1;
                }
            }
            return 29;
        } else {
            dayOfCycle = -dayOfCycle;
            for (int i = 0; i < cycles.length; i++) {
                if (dayOfCycle <= cycles[i].intValue()) {
                    return i - 1;
                }
            }
            return 29;
        }
    }

    private static Integer[] getAdjustedCycle(int cycleNumber) {

        Integer[] cycles;
        try {
            cycles = ADJUSTED_CYCLE_YEARS.get(Integer.valueOf(cycleNumber));
        } catch (ArrayIndexOutOfBoundsException e) {
            cycles = null;
        }
        if (cycles == null) {
            cycles = DEFAULT_CYCLE_YEARS;
        }
        return cycles;
    }

    private static Integer[] getAdjustedMonthDays(int year) {

        Integer[] newMonths;
        try {
            newMonths = ADJUSTED_MONTH_DAYS.get(Integer.valueOf(year));
        } catch (ArrayIndexOutOfBoundsException e) {
            newMonths = null;
        }
        if (newMonths == null) {
            if (isLeapYear(year)) {
                newMonths = DEFAULT_LEAP_MONTH_DAYS;
            } else {
                newMonths = DEFAULT_MONTH_DAYS;
            }
        }
        return newMonths;
    }

    private static Integer[] getAdjustedMonthLength(int year) {

        Integer[] newMonths;
        try {
            newMonths = ADJUSTED_MONTH_LENGTHS.get(Integer.valueOf(year));
        } catch (ArrayIndexOutOfBoundsException e) {
            newMonths = null;
        }
        if (newMonths == null) {
            if (isLeapYear(year)) {
                newMonths = DEFAULT_LEAP_MONTH_LENGTHS;
            } else {
                newMonths = DEFAULT_MONTH_LENGTHS;
            }
        }
        return newMonths;
    }

    private static int getDayOfYear(int cycleNumber, int dayOfCycle, int yearInCycle) {

        Integer[] cycles = getAdjustedCycle(cycleNumber);

        if (dayOfCycle > 0) {
            return dayOfCycle - cycles[yearInCycle].intValue();
        } else {
            return cycles[yearInCycle].intValue() + dayOfCycle;
        }
    }

    private static int getMonthOfYear(int dayOfYear, int year) {

        Integer[] newMonths = getAdjustedMonthDays(year);

        if (dayOfYear >= 0) {
            for (int i = 0; i < newMonths.length; i++) {
                if (dayOfYear < newMonths[i].intValue()) {
                    return i - 1;
                }
            }
            return 11;
        } else {
            dayOfYear = (isLeapYear(year) ? (dayOfYear + 355) : (dayOfYear + 354));
            for (int i = 0; i < newMonths.length; i++) {
                if (dayOfYear < newMonths[i].intValue()) {
                    return i - 1;
                }
            }
            return 11;
        }
    }

    private static int getDayOfMonth(int dayOfYear, int month, int year) {

        Integer[] newMonths = getAdjustedMonthDays(year);

        if (dayOfYear >= 0) {
            if (month > 0) {
                return dayOfYear - newMonths[month].intValue();
            } else {
                return dayOfYear;
            }
        } else {
            dayOfYear = (isLeapYear(year) ? (dayOfYear + 355) : (dayOfYear + 354));
            if (month > 0) {
                return dayOfYear - newMonths[month].intValue();
            } else {
                return dayOfYear;
            }
        }
    }

    static boolean isLeapYear(long year) {

        return (14 + 11 * (year > 0 ? year : -year)) % 30 < 11;
    }

    private static int getMonthDays(int month, int year) {

        Integer[] newMonths = getAdjustedMonthDays(year);
        return newMonths[month].intValue();
    }

    static int getMonthLength(int month, int year) {

        Integer[] newMonths = getAdjustedMonthLength(year);
        return newMonths[month].intValue();
    }

    @Override
    public int lengthOfMonth() {

        return getMonthLength(this.monthOfYear - 1, this.yearOfEra);
    }

    static int getYearLength(int year) {

        int cycleNumber = (year - 1) / 30;
        Integer[] cycleYears;
        try {
            cycleYears = ADJUSTED_CYCLE_YEARS.get(cycleNumber);
        } catch (ArrayIndexOutOfBoundsException e) {
            cycleYears = null;
        }
        if (cycleYears != null) {
            int yearInCycle = (year - 1) % 30;
            if (yearInCycle == 29) {
                return ADJUSTED_CYCLES[cycleNumber + 1].intValue() - ADJUSTED_CYCLES[cycleNumber].intValue()
                        - cycleYears[yearInCycle].intValue();
            }
            return cycleYears[yearInCycle + 1].intValue() - cycleYears[yearInCycle].intValue();
        } else {
            return isLeapYear(year) ? 355 : 354;
        }
    }

    @Override
    public int lengthOfYear() {

        return getYearLength(this.yearOfEra); // TODO: proleptic year
    }

    static int getMaximumDayOfMonth() {

        return ADJUSTED_MAX_VALUES[POSITION_DAY_OF_MONTH];
    }

    static int getSmallestMaximumDayOfMonth() {

        return ADJUSTED_LEAST_MAX_VALUES[POSITION_DAY_OF_MONTH];
    }

    static int getMaximumDayOfYear() {

        return ADJUSTED_MAX_VALUES[POSITION_DAY_OF_YEAR];
    }

    static int getSmallestMaximumDayOfYear() {

        return ADJUSTED_LEAST_MAX_VALUES[POSITION_DAY_OF_YEAR];
    }

    private static void addDeviationAsHijrah(int startYear, int startMonth, int endYear, int endMonth, int offset) {

        if (startYear < 1) {
            throw new IllegalArgumentException("startYear < 1");
        }
        if (endYear < 1) {
            throw new IllegalArgumentException("endYear < 1");
        }
        if (startMonth < 0 || startMonth > 11) {
            throw new IllegalArgumentException("startMonth < 0 || startMonth > 11");
        }
        if (endMonth < 0 || endMonth > 11) {
            throw new IllegalArgumentException("endMonth < 0 || endMonth > 11");
        }
        if (endYear > 9999) {
            throw new IllegalArgumentException("endYear > 9999");
        }
        if (endYear < startYear) {
            throw new IllegalArgumentException("startYear > endYear");
        }
        if (endYear == startYear && endMonth < startMonth) {
            throw new IllegalArgumentException("startYear == endYear && endMonth < startMonth");
        }

        // Adjusting start year.
        boolean isStartYLeap = isLeapYear(startYear);

        // Adjusting the number of month.
        Integer[] orgStartMonthNums = ADJUSTED_MONTH_DAYS.get(Integer.valueOf(startYear));
        if (orgStartMonthNums == null) {
            if (isStartYLeap) {
                orgStartMonthNums = new Integer[LEAP_NUM_DAYS.length];
                for (int l = 0; l < LEAP_NUM_DAYS.length; l++) {
                    orgStartMonthNums[l] = Integer.valueOf(LEAP_NUM_DAYS[l]);
                }
            } else {
                orgStartMonthNums = new Integer[NUM_DAYS.length];
                for (int l = 0; l < NUM_DAYS.length; l++) {
                    orgStartMonthNums[l] = Integer.valueOf(NUM_DAYS[l]);
                }
            }
        }

        Integer[] newStartMonthNums = new Integer[orgStartMonthNums.length];

        for (int month = 0; month < 12; month++) {
            if (month > startMonth) {
                newStartMonthNums[month] = Integer.valueOf(orgStartMonthNums[month].intValue() - offset);
            } else {
                newStartMonthNums[month] = Integer.valueOf(orgStartMonthNums[month].intValue());
            }
        }

        ADJUSTED_MONTH_DAYS.put(Integer.valueOf(startYear), newStartMonthNums);

        // Adjusting the days of month.

        Integer[] orgStartMonthLengths = ADJUSTED_MONTH_LENGTHS.get(Integer.valueOf(startYear));
        if (orgStartMonthLengths == null) {
            if (isStartYLeap) {
                orgStartMonthLengths = new Integer[LEAP_MONTH_LENGTH.length];
                for (int l = 0; l < LEAP_MONTH_LENGTH.length; l++) {
                    orgStartMonthLengths[l] = Integer.valueOf(LEAP_MONTH_LENGTH[l]);
                }
            } else {
                orgStartMonthLengths = new Integer[MONTH_LENGTH.length];
                for (int l = 0; l < MONTH_LENGTH.length; l++) {
                    orgStartMonthLengths[l] = Integer.valueOf(MONTH_LENGTH[l]);
                }
            }
        }

        Integer[] newStartMonthLengths = new Integer[orgStartMonthLengths.length];

        for (int month = 0; month < 12; month++) {
            if (month == startMonth) {
                newStartMonthLengths[month] = Integer.valueOf(orgStartMonthLengths[month].intValue() - offset);
            } else {
                newStartMonthLengths[month] = Integer.valueOf(orgStartMonthLengths[month].intValue());
            }
        }

        ADJUSTED_MONTH_LENGTHS.put(Integer.valueOf(startYear), newStartMonthLengths);

        if (startYear != endYear) {
            // System.out.println("over year");
            // Adjusting starting 30 year cycle.
            int sCycleNumber = (startYear - 1) / 30;
            int sYearInCycle = (startYear - 1) % 30; // 0-based.
            Integer[] startCycles = ADJUSTED_CYCLE_YEARS.get(Integer.valueOf(sCycleNumber));
            if (startCycles == null) {
                startCycles = new Integer[CYCLEYEAR_START_DATE.length];
                for (int j = 0; j < startCycles.length; j++) {
                    startCycles[j] = Integer.valueOf(CYCLEYEAR_START_DATE[j]);
                }
            }

            for (int j = sYearInCycle + 1; j < CYCLEYEAR_START_DATE.length; j++) {
                startCycles[j] = Integer.valueOf(startCycles[j].intValue() - offset);
            }

            // System.out.println(sCycleNumber + ":" + sYearInCycle);
            ADJUSTED_CYCLE_YEARS.put(Integer.valueOf(sCycleNumber), startCycles);

            int sYearInMaxY = (startYear - 1) / 30;
            int sEndInMaxY = (endYear - 1) / 30;

            if (sYearInMaxY != sEndInMaxY) {
                // System.out.println("over 30");
                // Adjusting starting 30 * MAX_ADJUSTED_CYCLE year cycle.
                // System.out.println(sYearInMaxY);

                for (int j = sYearInMaxY + 1; j < ADJUSTED_CYCLES.length; j++) {
                    ADJUSTED_CYCLES[j] = Long.valueOf(ADJUSTED_CYCLES[j].longValue() - offset);
                }

                // Adjusting ending 30 * MAX_ADJUSTED_CYCLE year cycles.
                for (int j = sEndInMaxY + 1; j < ADJUSTED_CYCLES.length; j++) {
                    ADJUSTED_CYCLES[j] = Long.valueOf(ADJUSTED_CYCLES[j].longValue() + offset);
                }
            }

            // Adjusting ending 30 year cycle.
            int eCycleNumber = (endYear - 1) / 30;
            int sEndInCycle = (endYear - 1) % 30; // 0-based.
            Integer[] endCycles = ADJUSTED_CYCLE_YEARS.get(Integer.valueOf(eCycleNumber));
            if (endCycles == null) {
                endCycles = new Integer[CYCLEYEAR_START_DATE.length];
                for (int j = 0; j < endCycles.length; j++) {
                    endCycles[j] = Integer.valueOf(CYCLEYEAR_START_DATE[j]);
                }
            }
            for (int j = sEndInCycle + 1; j < CYCLEYEAR_START_DATE.length; j++) {
                endCycles[j] = Integer.valueOf(endCycles[j].intValue() + offset);
            }
            ADJUSTED_CYCLE_YEARS.put(Integer.valueOf(eCycleNumber), endCycles);
        }

        // Adjusting ending year.
        boolean isEndYLeap = isLeapYear(endYear);

        Integer[] orgEndMonthDays = ADJUSTED_MONTH_DAYS.get(Integer.valueOf(endYear));

        if (orgEndMonthDays == null) {
            if (isEndYLeap) {
                orgEndMonthDays = new Integer[LEAP_NUM_DAYS.length];
                for (int l = 0; l < LEAP_NUM_DAYS.length; l++) {
                    orgEndMonthDays[l] = Integer.valueOf(LEAP_NUM_DAYS[l]);
                }
            } else {
                orgEndMonthDays = new Integer[NUM_DAYS.length];
                for (int l = 0; l < NUM_DAYS.length; l++) {
                    orgEndMonthDays[l] = Integer.valueOf(NUM_DAYS[l]);
                }
            }
        }

        Integer[] newEndMonthDays = new Integer[orgEndMonthDays.length];

        for (int month = 0; month < 12; month++) {
            if (month > endMonth) {
                newEndMonthDays[month] = Integer.valueOf(orgEndMonthDays[month].intValue() + offset);
            } else {
                newEndMonthDays[month] = Integer.valueOf(orgEndMonthDays[month].intValue());
            }
        }

        ADJUSTED_MONTH_DAYS.put(Integer.valueOf(endYear), newEndMonthDays);

        // Adjusting the days of month.
        Integer[] orgEndMonthLengths = ADJUSTED_MONTH_LENGTHS.get(Integer.valueOf(endYear));

        if (orgEndMonthLengths == null) {
            if (isEndYLeap) {
                orgEndMonthLengths = new Integer[LEAP_MONTH_LENGTH.length];
                for (int l = 0; l < LEAP_MONTH_LENGTH.length; l++) {
                    orgEndMonthLengths[l] = Integer.valueOf(LEAP_MONTH_LENGTH[l]);
                }
            } else {
                orgEndMonthLengths = new Integer[MONTH_LENGTH.length];
                for (int l = 0; l < MONTH_LENGTH.length; l++) {
                    orgEndMonthLengths[l] = Integer.valueOf(MONTH_LENGTH[l]);
                }
            }
        }

        Integer[] newEndMonthLengths = new Integer[orgEndMonthLengths.length];

        for (int month = 0; month < 12; month++) {
            if (month == endMonth) {
                newEndMonthLengths[month] = Integer.valueOf(orgEndMonthLengths[month].intValue() + offset);
            } else {
                newEndMonthLengths[month] = Integer.valueOf(orgEndMonthLengths[month].intValue());
            }
        }

        ADJUSTED_MONTH_LENGTHS.put(Integer.valueOf(endYear), newEndMonthLengths);

        Integer[] startMonthLengths = ADJUSTED_MONTH_LENGTHS.get(Integer.valueOf(startYear));
        Integer[] endMonthLengths = ADJUSTED_MONTH_LENGTHS.get(Integer.valueOf(endYear));
        Integer[] startMonthDays = ADJUSTED_MONTH_DAYS.get(Integer.valueOf(startYear));
        Integer[] endMonthDays = ADJUSTED_MONTH_DAYS.get(Integer.valueOf(endYear));

        int startMonthLength = startMonthLengths[startMonth].intValue();
        int endMonthLength = endMonthLengths[endMonth].intValue();
        int startMonthDay = startMonthDays[11].intValue() + startMonthLengths[11].intValue();
        int endMonthDay = endMonthDays[11].intValue() + endMonthLengths[11].intValue();

        int maxMonthLength = ADJUSTED_MAX_VALUES[POSITION_DAY_OF_MONTH].intValue();
        int leastMaxMonthLength = ADJUSTED_LEAST_MAX_VALUES[POSITION_DAY_OF_MONTH].intValue();

        if (maxMonthLength < startMonthLength) {
            maxMonthLength = startMonthLength;
        }
        if (maxMonthLength < endMonthLength) {
            maxMonthLength = endMonthLength;
        }
        ADJUSTED_MAX_VALUES[POSITION_DAY_OF_MONTH] = Integer.valueOf(maxMonthLength);

        if (leastMaxMonthLength > startMonthLength) {
            leastMaxMonthLength = startMonthLength;
        }
        if (leastMaxMonthLength > endMonthLength) {
            leastMaxMonthLength = endMonthLength;
        }
        ADJUSTED_LEAST_MAX_VALUES[POSITION_DAY_OF_MONTH] = Integer.valueOf(leastMaxMonthLength);

        int maxMonthDay = ADJUSTED_MAX_VALUES[POSITION_DAY_OF_YEAR].intValue();
        int leastMaxMonthDay = ADJUSTED_LEAST_MAX_VALUES[POSITION_DAY_OF_YEAR].intValue();

        if (maxMonthDay < startMonthDay) {
            maxMonthDay = startMonthDay;
        }
        if (maxMonthDay < endMonthDay) {
            maxMonthDay = endMonthDay;
        }

        ADJUSTED_MAX_VALUES[POSITION_DAY_OF_YEAR] = Integer.valueOf(maxMonthDay);

        if (leastMaxMonthDay > startMonthDay) {
            leastMaxMonthDay = startMonthDay;
        }
        if (leastMaxMonthDay > endMonthDay) {
            leastMaxMonthDay = endMonthDay;
        }
        ADJUSTED_LEAST_MAX_VALUES[POSITION_DAY_OF_YEAR] = Integer.valueOf(leastMaxMonthDay);
    }

    private static void readDeviationConfig() throws IOException, ParseException {

        InputStream is = getConfigFileInputStream();
        if (is != null) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(is));
                String line = "";
                int num = 0;
                while ((line = br.readLine()) != null) {
                    num++;
                    line = line.trim();
                    parseLine(line, num);
                }
            } finally {
                if (br != null) {
                    br.close();
                }
            }
        }
    }

    private static void parseLine(String line, int num) throws ParseException {

        StringTokenizer st = new StringTokenizer(line, ";");
        while (st.hasMoreTokens()) {
            String deviationElement = st.nextToken();
            int offsetIndex = deviationElement.indexOf(':');
            if (offsetIndex != -1) {
                String offsetString = deviationElement.substring(offsetIndex + 1, deviationElement.length());
                int offset;
                try {
                    offset = Integer.parseInt(offsetString);
                } catch (NumberFormatException ex) {
                    throw new ParseException("Offset is not properly set at line " + num + ".", num);
                }
                int separatorIndex = deviationElement.indexOf('-');
                if (separatorIndex != -1) {
                    String startDateStg = deviationElement.substring(0, separatorIndex);
                    String endDateStg = deviationElement.substring(separatorIndex + 1, offsetIndex);
                    int startDateYearSepIndex = startDateStg.indexOf('/');
                    int endDateYearSepIndex = endDateStg.indexOf('/');
                    int startYear = -1;
                    int endYear = -1;
                    int startMonth = -1;
                    int endMonth = -1;
                    if (startDateYearSepIndex != -1) {
                        String startYearStg = startDateStg.substring(0, startDateYearSepIndex);
                        String startMonthStg = startDateStg.substring(startDateYearSepIndex + 1, startDateStg.length());
                        try {
                            startYear = Integer.parseInt(startYearStg);
                        } catch (NumberFormatException ex) {
                            throw new ParseException("Start year is not properly set at line " + num + ".", num);
                        }
                        try {
                            startMonth = Integer.parseInt(startMonthStg);
                        } catch (NumberFormatException ex) {
                            throw new ParseException("Start month is not properly set at line " + num + ".", num);
                        }
                    } else {
                        throw new ParseException("Start year/month has incorrect format at line " + num + ".", num);
                    }
                    if (endDateYearSepIndex != -1) {
                        String endYearStg = endDateStg.substring(0, endDateYearSepIndex);
                        String endMonthStg = endDateStg.substring(endDateYearSepIndex + 1, endDateStg.length());
                        try {
                            endYear = Integer.parseInt(endYearStg);
                        } catch (NumberFormatException ex) {
                            throw new ParseException("End year is not properly set at line " + num + ".", num);
                        }
                        try {
                            endMonth = Integer.parseInt(endMonthStg);
                        } catch (NumberFormatException ex) {
                            throw new ParseException("End month is not properly set at line " + num + ".", num);
                        }
                    } else {
                        throw new ParseException("End year/month has incorrect format at line " + num + ".", num);
                    }
                    if (startYear != -1 && startMonth != -1 && endYear != -1 && endMonth != -1) {
                        addDeviationAsHijrah(startYear, startMonth, endYear, endMonth, offset);
                    } else {
                        throw new ParseException("Unknown error at line " + num + ".", num);
                    }
                } else {
                    throw new ParseException("Start and end year/month has incorrect format at line " + num + ".", num);
                }
            } else {
                throw new ParseException("Offset has incorrect format at line " + num + ".", num);
            }
        }
    }

    private static InputStream getConfigFileInputStream() throws IOException {

        String fileName = System.getProperty("org.teavm.classlib.java.time.i18n.THijrahDate.deviationConfigFile");

        if (fileName == null) {
            fileName = DEFAULT_CONFIG_FILENAME;
        }

        String dir = System.getProperty("org.teavm.classlib.java.time.i18n.THijrahDate.deviationConfigDir");

        if (dir != null) {
            if (!(dir.length() == 0 && dir.endsWith(System.getProperty("file.separator")))) {
                dir = dir + System.getProperty("file.separator");
            }
            File file = new File(dir + FILE_SEP + fileName);
            if (file.exists()) {
                try {
                    return new FileInputStream(file);
                } catch (IOException ioe) {
                    throw ioe;
                }
            } else {
                return null;
            }
        } else {
            String classPath = System.getProperty("java.class.path");
            StringTokenizer st = new StringTokenizer(classPath, PATH_SEP);
            while (st.hasMoreTokens()) {
                String path = st.nextToken();
                File file = new File(path);
                if (file.exists()) {
                    if (file.isDirectory()) {
                        File f = new File(path + FILE_SEP + DEFAULT_CONFIG_PATH, fileName);
                        if (f.exists()) {
                            try {
                                return new FileInputStream(path + FILE_SEP + DEFAULT_CONFIG_PATH + FILE_SEP + fileName);
                            } catch (IOException ioe) {
                                throw ioe;
                            }
                        }
                    } else {
                        ZipFile zip;
                        try {
                            zip = new ZipFile(file);
                        } catch (IOException ioe) {
                            zip = null;
                        }

                        if (zip != null) {
                            String targetFile = DEFAULT_CONFIG_PATH + FILE_SEP + fileName;
                            ZipEntry entry = zip.getEntry(targetFile);

                            if (entry == null) {
                                if (FILE_SEP == '/') {
                                    targetFile = targetFile.replace('/', '\\');
                                } else if (FILE_SEP == '\\') {
                                    targetFile = targetFile.replace('\\', '/');
                                }
                                entry = zip.getEntry(targetFile);
                            }

                            if (entry != null) {
                                try {
                                    return zip.getInputStream(entry);
                                } catch (IOException ioe) {
                                    throw ioe;
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }
    }

}
