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
import static org.teavm.classlib.java.time.temporal.TTemporalAdjusters.nextOrSame;

import java.io.Serializable;
import java.util.Arrays;
import org.teavm.classlib.java.util.TCalendar;
import java.util.HashMap;
import java.util.List;
import org.teavm.classlib.java.util.TLocale;
import java.util.Map;

import org.teavm.classlib.java.time.TClock;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TDayOfWeek;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.format.TResolverStyle;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TValueRange;

public final class TJapaneseChronology extends TChronology implements Serializable {

    // TLocale for creating a JapaneseImpericalCalendar.
    static final TLocale LOCALE = new TLocale("ja", "JP", "JP");

    public static final TJapaneseChronology INSTANCE = new TJapaneseChronology();

    private static final long serialVersionUID = 459996390165777884L;

    private static final Map<String, String[]> ERA_NARROW_NAMES = new HashMap<String, String[]>();
    private static final Map<String, String[]> ERA_SHORT_NAMES = new HashMap<String, String[]>();
    private static final Map<String, String[]> ERA_FULL_NAMES = new HashMap<String, String[]>();
    private static final String FALLBACK_LANGUAGE = "en";
    private static final String TARGET_LANGUAGE = "ja";

    // TODO: replace all the hard-coded Maps with locale resources
    static {
        ERA_NARROW_NAMES.put(FALLBACK_LANGUAGE, new String[]{"Unknown", "K", "M", "T", "S", "H"});
        ERA_NARROW_NAMES.put(TARGET_LANGUAGE, new String[]{"Unknown", "K", "M", "T", "S", "H"});
        ERA_SHORT_NAMES.put(FALLBACK_LANGUAGE, new String[]{"Unknown", "K", "M", "T", "S", "H"});
        ERA_SHORT_NAMES.put(TARGET_LANGUAGE, new String[]{"Unknown", "\u6176", "\u660e", "\u5927", "\u662d", "\u5e73"});
        ERA_FULL_NAMES.put(FALLBACK_LANGUAGE, new String[]{"Unknown", "Keio", "Meiji", "Taisho", "Showa", "Heisei"});
        ERA_FULL_NAMES.put(TARGET_LANGUAGE,
                new String[]{"Unknown", "\u6176\u5fdc", "\u660e\u6cbb", "\u5927\u6b63", "\u662d\u548c", "\u5e73\u6210"});
    }

    //-----------------------------------------------------------------------
    private TJapaneseChronology() {
    }

    private Object readResolve() {
        return INSTANCE;
    }

    //-----------------------------------------------------------------------
    @Override
    public String getId() {
        return "Japanese";
    }

    @Override
    public String getCalendarType() {
        return "japanese";
    }

    //-----------------------------------------------------------------------
    @Override  // override with covariant return type
    public TJapaneseDate date(TEra era, int yearOfEra, int month, int dayOfMonth) {
        if (era instanceof TJapaneseEra == false) {
            throw new ClassCastException("TEra must be TJapaneseEra");
        }
        return TJapaneseDate.of((TJapaneseEra) era, yearOfEra, month, dayOfMonth);
    }

    @Override  // override with covariant return type
    public TJapaneseDate date(int prolepticYear, int month, int dayOfMonth) {
        return new TJapaneseDate(TLocalDate.of(prolepticYear, month, dayOfMonth));
    }

    @Override
    public TJapaneseDate dateYearDay(TEra era, int yearOfEra, int dayOfYear) {
        if (era instanceof TJapaneseEra == false) {
            throw new ClassCastException("TEra must be TJapaneseEra");
        }
        return TJapaneseDate.ofYearDay((TJapaneseEra) era, yearOfEra, dayOfYear);
    }

    @Override
    public TJapaneseDate dateYearDay(int prolepticYear, int dayOfYear) {
        TLocalDate date = TLocalDate.ofYearDay(prolepticYear, dayOfYear);
        return date(prolepticYear, date.getMonthValue(), date.getDayOfMonth());
    }

    @Override
    public TJapaneseDate dateEpochDay(long epochDay) {
        return new TJapaneseDate(TLocalDate.ofEpochDay(epochDay));
    }

    //-----------------------------------------------------------------------
    @Override  // override with covariant return type
    public TJapaneseDate date(TTemporalAccessor temporal) {
        if (temporal instanceof TJapaneseDate) {
            return (TJapaneseDate) temporal;
        }
        return new TJapaneseDate(TLocalDate.from(temporal));
    }

    @SuppressWarnings("unchecked")
    @Override  // override with covariant return type
    public TChronoLocalDateTime<TJapaneseDate> localDateTime(TTemporalAccessor temporal) {
        return (TChronoLocalDateTime<TJapaneseDate>) super.localDateTime(temporal);
    }

    @SuppressWarnings("unchecked")
    @Override  // override with covariant return type
    public TChronoZonedDateTime<TJapaneseDate> zonedDateTime(TTemporalAccessor temporal) {
        return (TChronoZonedDateTime<TJapaneseDate>) super.zonedDateTime(temporal);
    }

    @SuppressWarnings("unchecked")
    @Override  // override with covariant return type
    public TChronoZonedDateTime<TJapaneseDate> zonedDateTime(TInstant instant, TZoneId zone) {
        return (TChronoZonedDateTime<TJapaneseDate>) super.zonedDateTime(instant, zone);
    }

    //-----------------------------------------------------------------------
    @Override  // override with covariant return type
    public TJapaneseDate dateNow() {
        return (TJapaneseDate) super.dateNow();
    }

    @Override  // override with covariant return type
    public TJapaneseDate dateNow(TZoneId zone) {
        return (TJapaneseDate) super.dateNow(zone);
    }

    @Override  // override with covariant return type
    public TJapaneseDate dateNow(TClock clock) {
        TJdk8Methods.requireNonNull(clock, "clock");
        return (TJapaneseDate) super.dateNow(clock);
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean isLeapYear(long prolepticYear) {
        return TIsoChronology.INSTANCE.isLeapYear(prolepticYear);
    }

    @Override
    public int prolepticYear(TEra era, int yearOfEra) {
        if (era instanceof TJapaneseEra == false) {
            throw new ClassCastException("TEra must be TJapaneseEra");
        }
        TJapaneseEra jera = (TJapaneseEra) era;
        int isoYear = jera.startDate().getYear() + yearOfEra - 1;
        TValueRange range = TValueRange.of(1, jera.endDate().getYear() - jera.startDate().getYear() + 1);
        range.checkValidValue(yearOfEra, YEAR_OF_ERA);
        return isoYear;
    }

    @Override
    public TJapaneseEra eraOf(int eraValue) {
        return TJapaneseEra.of(eraValue);
    }

    @Override
    public List<TEra> eras() {
        return Arrays.<TEra>asList(TJapaneseEra.values());
    }

    //-----------------------------------------------------------------------
    @Override
    public TValueRange range(TChronoField field) {
        switch (field) {
            case DAY_OF_MONTH:
            case DAY_OF_WEEK:
            case MICRO_OF_DAY:
            case MICRO_OF_SECOND:
            case HOUR_OF_DAY:
            case HOUR_OF_AMPM:
            case MINUTE_OF_DAY:
            case MINUTE_OF_HOUR:
            case SECOND_OF_DAY:
            case SECOND_OF_MINUTE:
            case MILLI_OF_DAY:
            case MILLI_OF_SECOND:
            case NANO_OF_DAY:
            case NANO_OF_SECOND:
            case CLOCK_HOUR_OF_DAY:
            case CLOCK_HOUR_OF_AMPM:
            case EPOCH_DAY:
            case PROLEPTIC_MONTH:
                return field.range();
        }
        TCalendar jcal = TCalendar.getInstance(LOCALE);
        switch (field) {
            case ERA: {
                TJapaneseEra[] eras = TJapaneseEra.values();
                return TValueRange.of(eras[0].getValue(), eras[eras.length - 1].getValue());
            }
            case YEAR: {
                TJapaneseEra[] eras = TJapaneseEra.values();
                return TValueRange.of(TJapaneseDate.MIN_DATE.getYear(), eras[eras.length - 1].endDate().getYear());
            }
            case YEAR_OF_ERA: {
                TJapaneseEra[] eras = TJapaneseEra.values();
                int maxIso = eras[eras.length - 1].endDate().getYear();
                int maxJapanese = maxIso - eras[eras.length - 1].startDate().getYear() + 1;
                int min = Integer.MAX_VALUE;
                for (int i = 0; i < eras.length; i++) {
                    min = Math.min(min, eras[i].endDate().getYear() - eras[i].startDate().getYear() + 1);
                }
                return TValueRange.of(1, 6, min, maxJapanese);
            }
            case MONTH_OF_YEAR:
                return TValueRange.of(jcal.getMinimum(TCalendar.MONTH) + 1, jcal.getGreatestMinimum(TCalendar.MONTH) + 1,
                                             jcal.getLeastMaximum(TCalendar.MONTH) + 1, jcal.getMaximum(TCalendar.MONTH) + 1);
            case DAY_OF_YEAR: {
                TJapaneseEra[] eras = TJapaneseEra.values();
                int min = 366;
                for (int i = 0; i < eras.length; i++) {
                    min = Math.min(min, eras[i].startDate().lengthOfYear() - eras[i].startDate().getDayOfYear() + 1);
                }
                return TValueRange.of(1, min, 366);
            }
            default:
                 // TODO: review the remaining fields
                throw new UnsupportedOperationException("Unimplementable field: " + field);
        }
    }

    @Override
    public TJapaneseDate resolveDate(Map<TTemporalField, Long> fieldValues, TResolverStyle resolverStyle) {
        if (fieldValues.containsKey(EPOCH_DAY)) {
            return dateEpochDay(fieldValues.remove(EPOCH_DAY));
        }

        // normalize fields
        Long prolepticMonth = fieldValues.remove(PROLEPTIC_MONTH);
        if (prolepticMonth != null) {
            if (resolverStyle != TResolverStyle.LENIENT) {
                PROLEPTIC_MONTH.checkValidValue(prolepticMonth);
            }
            updateResolveMap(fieldValues, MONTH_OF_YEAR, TJdk8Methods.floorMod(prolepticMonth, 12) + 1);
            updateResolveMap(fieldValues, YEAR, TJdk8Methods.floorDiv(prolepticMonth, 12));
        }

        // eras
        Long eraLong = fieldValues.get(ERA);
        TJapaneseEra era = null;
        if (eraLong != null) {
            era = eraOf(range(ERA).checkValidIntValue(eraLong, ERA));
        }
        Long yoeLong = fieldValues.get(YEAR_OF_ERA);
        if (yoeLong != null) {
            int yoe= range(YEAR_OF_ERA).checkValidIntValue(yoeLong, YEAR_OF_ERA);
            if (era == null && resolverStyle != TResolverStyle.STRICT && fieldValues.containsKey(YEAR) == false) {
                List<TEra> eras = eras();
                era = (TJapaneseEra) eras.get(eras.size() - 1);
            }
            // can only resolve to dates, not to proleptic-year
            if (era != null && fieldValues.containsKey(MONTH_OF_YEAR) && fieldValues.containsKey(DAY_OF_MONTH)) {
                fieldValues.remove(ERA);
                fieldValues.remove(YEAR_OF_ERA);
                return resolveEYMD(fieldValues, resolverStyle, era, yoe);
            }
            if (era != null && fieldValues.containsKey(DAY_OF_YEAR)) {
                fieldValues.remove(ERA);
                fieldValues.remove(YEAR_OF_ERA);
                return resolveEYD(fieldValues, resolverStyle, era, yoe);
            }
        }

        // build date
        if (fieldValues.containsKey(YEAR)) {
            if (fieldValues.containsKey(MONTH_OF_YEAR)) {
                if (fieldValues.containsKey(DAY_OF_MONTH)) {
                    int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                    if (resolverStyle == TResolverStyle.LENIENT) {
                        long months = TJdk8Methods.safeSubtract(fieldValues.remove(MONTH_OF_YEAR), 1);
                        long days = TJdk8Methods.safeSubtract(fieldValues.remove(DAY_OF_MONTH), 1);
                        return date(y, 1, 1).plusMonths(months).plusDays(days);
                    } else {
                        int moy = range(MONTH_OF_YEAR).checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR), MONTH_OF_YEAR);
                        int dom = range(DAY_OF_MONTH).checkValidIntValue(fieldValues.remove(DAY_OF_MONTH), DAY_OF_MONTH);
                        if (resolverStyle == TResolverStyle.SMART && dom > 28) {
                            dom = Math.min(dom, date(y, moy, 1).lengthOfMonth());
                        }
                        return date(y, moy, dom);
                    }
                }
                if (fieldValues.containsKey(ALIGNED_WEEK_OF_MONTH)) {
                    if (fieldValues.containsKey(ALIGNED_DAY_OF_WEEK_IN_MONTH)) {
                        int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                        if (resolverStyle == TResolverStyle.LENIENT) {
                            long months = TJdk8Methods.safeSubtract(fieldValues.remove(MONTH_OF_YEAR), 1);
                            long weeks = TJdk8Methods.safeSubtract(fieldValues.remove(ALIGNED_WEEK_OF_MONTH), 1);
                            long days = TJdk8Methods.safeSubtract(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_MONTH), 1);
                            return date(y, 1, 1).plus(months, MONTHS).plus(weeks, WEEKS).plus(days, DAYS);
                        }
                        int moy = MONTH_OF_YEAR.checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR));
                        int aw = ALIGNED_WEEK_OF_MONTH.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_MONTH));
                        int ad = ALIGNED_DAY_OF_WEEK_IN_MONTH.checkValidIntValue(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_MONTH));
                        TJapaneseDate date = date(y, moy, 1).plus((aw - 1) * 7 + (ad - 1), DAYS);
                        if (resolverStyle == TResolverStyle.STRICT && date.get(MONTH_OF_YEAR) != moy) {
                            throw new TDateTimeException("Strict mode rejected date parsed to a different month");
                        }
                        return date;
                    }
                    if (fieldValues.containsKey(DAY_OF_WEEK)) {
                        int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                        if (resolverStyle == TResolverStyle.LENIENT) {
                            long months = TJdk8Methods.safeSubtract(fieldValues.remove(MONTH_OF_YEAR), 1);
                            long weeks = TJdk8Methods.safeSubtract(fieldValues.remove(ALIGNED_WEEK_OF_MONTH), 1);
                            long days = TJdk8Methods.safeSubtract(fieldValues.remove(DAY_OF_WEEK), 1);
                            return date(y, 1, 1).plus(months, MONTHS).plus(weeks, WEEKS).plus(days, DAYS);
                        }
                        int moy = MONTH_OF_YEAR.checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR));
                        int aw = ALIGNED_WEEK_OF_MONTH.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_MONTH));
                        int dow = DAY_OF_WEEK.checkValidIntValue(fieldValues.remove(DAY_OF_WEEK));
                        TJapaneseDate date = date(y, moy, 1).plus(aw - 1, WEEKS).with(nextOrSame(TDayOfWeek.of(dow)));
                        if (resolverStyle == TResolverStyle.STRICT && date.get(MONTH_OF_YEAR) != moy) {
                            throw new TDateTimeException("Strict mode rejected date parsed to a different month");
                        }
                        return date;
                    }
                }
            }
            if (fieldValues.containsKey(DAY_OF_YEAR)) {
                int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                if (resolverStyle == TResolverStyle.LENIENT) {
                    long days = TJdk8Methods.safeSubtract(fieldValues.remove(DAY_OF_YEAR), 1);
                    return dateYearDay(y, 1).plusDays(days);
                }
                int doy = DAY_OF_YEAR.checkValidIntValue(fieldValues.remove(DAY_OF_YEAR));
                return dateYearDay(y, doy);
            }
            if (fieldValues.containsKey(ALIGNED_WEEK_OF_YEAR)) {
                if (fieldValues.containsKey(ALIGNED_DAY_OF_WEEK_IN_YEAR)) {
                    int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                    if (resolverStyle == TResolverStyle.LENIENT) {
                        long weeks = TJdk8Methods.safeSubtract(fieldValues.remove(ALIGNED_WEEK_OF_YEAR), 1);
                        long days = TJdk8Methods.safeSubtract(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_YEAR), 1);
                        return date(y, 1, 1).plus(weeks, WEEKS).plus(days, DAYS);
                    }
                    int aw = ALIGNED_WEEK_OF_YEAR.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_YEAR));
                    int ad = ALIGNED_DAY_OF_WEEK_IN_YEAR.checkValidIntValue(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_YEAR));
                    TJapaneseDate date = date(y, 1, 1).plusDays((aw - 1) * 7 + (ad - 1));
                    if (resolverStyle == TResolverStyle.STRICT && date.get(YEAR) != y) {
                        throw new TDateTimeException("Strict mode rejected date parsed to a different year");
                    }
                    return date;
                }
                if (fieldValues.containsKey(DAY_OF_WEEK)) {
                    int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                    if (resolverStyle == TResolverStyle.LENIENT) {
                        long weeks = TJdk8Methods.safeSubtract(fieldValues.remove(ALIGNED_WEEK_OF_YEAR), 1);
                        long days = TJdk8Methods.safeSubtract(fieldValues.remove(DAY_OF_WEEK), 1);
                        return date(y, 1, 1).plus(weeks, WEEKS).plus(days, DAYS);
                    }
                    int aw = ALIGNED_WEEK_OF_YEAR.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_YEAR));
                    int dow = DAY_OF_WEEK.checkValidIntValue(fieldValues.remove(DAY_OF_WEEK));
                    TJapaneseDate date = date(y, 1, 1).plus(aw - 1, WEEKS).with(nextOrSame(TDayOfWeek.of(dow)));
                    if (resolverStyle == TResolverStyle.STRICT && date.get(YEAR) != y) {
                        throw new TDateTimeException("Strict mode rejected date parsed to a different month");
                    }
                    return date;
                }
            }
        }
        return null;
    }

    private TJapaneseDate resolveEYMD(Map<TTemporalField, Long> fieldValues, TResolverStyle resolverStyle, TJapaneseEra era, int yoe) {
        if (resolverStyle == TResolverStyle.LENIENT) {
            int y = era.startDate().getYear() + yoe - 1;
            long months = TJdk8Methods.safeSubtract(fieldValues.remove(MONTH_OF_YEAR), 1);
            long days = TJdk8Methods.safeSubtract(fieldValues.remove(DAY_OF_MONTH), 1);
            return date(y, 1, 1).plus(months, MONTHS).plus(days, DAYS);
        }
        int moy = range(MONTH_OF_YEAR).checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR), MONTH_OF_YEAR);
        int dom = range(DAY_OF_MONTH).checkValidIntValue(fieldValues.remove(DAY_OF_MONTH), DAY_OF_MONTH);
        if (resolverStyle == TResolverStyle.SMART) {  // previous valid
            if (yoe < 1) {
                throw new TDateTimeException("Invalid YearOfEra: " + yoe);
            }
            int y = era.startDate().getYear() + yoe - 1;
            if (dom > 28) {
                dom = Math.min(dom, date(y, moy, 1).lengthOfMonth());
            }
            TJapaneseDate jd = date(y, moy, dom);
            if (jd.getEra() != era) {
                // ensure within calendar year of change
                if (Math.abs(jd.getEra().getValue() - era.getValue()) > 1) {
                    throw new TDateTimeException("Invalid TEra/YearOfEra: " + era + " " + yoe);
                }
                if (jd.get(YEAR_OF_ERA) != 1 && yoe != 1) {
                    throw new TDateTimeException("Invalid TEra/YearOfEra: " + era + " " + yoe);
                }
            }
            return jd;
        }
        return date(era, yoe, moy, dom);
    }

    private TJapaneseDate resolveEYD(Map<TTemporalField, Long> fieldValues, TResolverStyle resolverStyle, TJapaneseEra era, int yoe) {
        if (resolverStyle == TResolverStyle.LENIENT) {
            int y = era.startDate().getYear() + yoe - 1;
            long days = TJdk8Methods.safeSubtract(fieldValues.remove(DAY_OF_YEAR), 1);
            return dateYearDay(y, 1).plus(days, DAYS);
        }
        int doy = range(DAY_OF_YEAR).checkValidIntValue(fieldValues.remove(DAY_OF_YEAR), DAY_OF_YEAR);
        return dateYearDay(era, yoe, doy);  // smart is same as strict
    }

}
