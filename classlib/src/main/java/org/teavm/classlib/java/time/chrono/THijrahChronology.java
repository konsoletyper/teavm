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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.teavm.classlib.java.time.TClock;
import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TDayOfWeek;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.format.TResolverStyle;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TValueRange;

public final class THijrahChronology extends TAbstractChronology implements Serializable {

    public static final THijrahChronology INSTANCE = new THijrahChronology();

    private static final long serialVersionUID = 3127340209035924785L;

    private static final HashMap<String, String[]> ERA_NARROW_NAMES = new HashMap<String, String[]>();

    private static final HashMap<String, String[]> ERA_SHORT_NAMES = new HashMap<String, String[]>();

    private static final HashMap<String, String[]> ERA_FULL_NAMES = new HashMap<String, String[]>();

    private static final String FALLBACK_LANGUAGE = "en";

    // private static final String TARGET_LANGUAGE = "ar";
    static {
        ERA_NARROW_NAMES.put(FALLBACK_LANGUAGE, new String[] { "BH", "HE" });
        ERA_SHORT_NAMES.put(FALLBACK_LANGUAGE, new String[] { "B.H.", "H.E." });
        ERA_FULL_NAMES.put(FALLBACK_LANGUAGE, new String[] { "Before Hijrah", "Hijrah TEra" });
    }

    private THijrahChronology() {

    }

    @Override
    public String getId() {

        return "Hijrah-umalqura";
    }

    @Override
    public String getCalendarType() {

        return "islamic-umalqura";
    }

    @Override
    public THijrahDate date(TEra era, int yearOfEra, int month, int dayOfMonth) {

        return (THijrahDate) super.date(era, yearOfEra, month, dayOfMonth);
    }

    @Override
    public THijrahDate date(int prolepticYear, int month, int dayOfMonth) {

        return THijrahDate.of(prolepticYear, month, dayOfMonth);
    }

    @Override
    public THijrahDate dateYearDay(TEra era, int yearOfEra, int dayOfYear) {

        return (THijrahDate) super.dateYearDay(era, yearOfEra, dayOfYear);
    }

    @Override
    public THijrahDate dateYearDay(int prolepticYear, int dayOfYear) {

        return THijrahDate.of(prolepticYear, 1, 1).plusDays(dayOfYear - 1); // TODO better
    }

    @Override
    public THijrahDate dateEpochDay(long epochDay) {

        return THijrahDate.of(TLocalDate.ofEpochDay(epochDay));
    }

    @Override
    public THijrahDate date(TTemporalAccessor temporal) {

        if (temporal instanceof THijrahDate) {
            return (THijrahDate) temporal;
        }
        return THijrahDate.ofEpochDay(temporal.getLong(EPOCH_DAY));
    }

    @SuppressWarnings("unchecked")
    @Override
    public TChronoLocalDateTime<THijrahDate> localDateTime(TTemporalAccessor temporal) {

        return (TChronoLocalDateTime<THijrahDate>) super.localDateTime(temporal);
    }

    @SuppressWarnings("unchecked")
    @Override
    public TChronoZonedDateTime<THijrahDate> zonedDateTime(TTemporalAccessor temporal) {

        return (TChronoZonedDateTime<THijrahDate>) super.zonedDateTime(temporal);
    }

    @SuppressWarnings("unchecked")
    @Override
    public TChronoZonedDateTime<THijrahDate> zonedDateTime(TInstant instant, TZoneId zone) {

        return (TChronoZonedDateTime<THijrahDate>) super.zonedDateTime(instant, zone);
    }

    @Override
    public THijrahDate dateNow() {

        return (THijrahDate) super.dateNow();
    }

    @Override
    public THijrahDate dateNow(TZoneId zone) {

        return (THijrahDate) super.dateNow(zone);
    }

    @Override
    public THijrahDate dateNow(TClock clock) {

        Objects.requireNonNull(clock, "clock");
        return (THijrahDate) super.dateNow(clock);
    }

    @Override
    public boolean isLeapYear(long prolepticYear) {

        return THijrahDate.isLeapYear(prolepticYear);
    }

    @Override
    public int prolepticYear(TEra era, int yearOfEra) {

        if (era instanceof THijrahEra == false) {
            throw new ClassCastException("TEra must be THijrahEra");
        }
        return (era == THijrahEra.AH ? yearOfEra : 1 - yearOfEra);
    }

    @Override
    public THijrahEra eraOf(int eraValue) {

        switch (eraValue) {
            case 0:
                return THijrahEra.BEFORE_AH;
            case 1:
                return THijrahEra.AH;
            default:
                throw new TDateTimeException("invalid Hijrah era");
        }
    }

    @Override
    public List<TEra> eras() {

        return Arrays.<TEra> asList(THijrahEra.values());
    }

    @Override
    public TValueRange range(TChronoField field) {

        return field.range();
    }

    @Override
    public THijrahDate resolveDate(Map<TTemporalField, Long> fieldValues, TResolverStyle resolverStyle) {

        if (fieldValues.containsKey(EPOCH_DAY)) {
            return dateEpochDay(fieldValues.remove(EPOCH_DAY));
        }

        // normalize fields
        Long prolepticMonth = fieldValues.remove(PROLEPTIC_MONTH);
        if (prolepticMonth != null) {
            if (resolverStyle != TResolverStyle.LENIENT) {
                PROLEPTIC_MONTH.checkValidValue(prolepticMonth);
            }
            updateResolveMap(fieldValues, MONTH_OF_YEAR, Math.floorMod(prolepticMonth, 12) + 1);
            updateResolveMap(fieldValues, YEAR, Math.floorDiv(prolepticMonth, 12));
        }

        // eras
        Long yoeLong = fieldValues.remove(YEAR_OF_ERA);
        if (yoeLong != null) {
            if (resolverStyle != TResolverStyle.LENIENT) {
                YEAR_OF_ERA.checkValidValue(yoeLong);
            }
            Long era = fieldValues.remove(ERA);
            if (era == null) {
                Long year = fieldValues.get(YEAR);
                if (resolverStyle == TResolverStyle.STRICT) {
                    // do not invent era if strict, but do cross-check with year
                    if (year != null) {
                        updateResolveMap(fieldValues, YEAR, (year > 0 ? yoeLong : Math.subtractExact(1, yoeLong)));
                    } else {
                        // reinstate the field removed earlier, no cross-check issues
                        fieldValues.put(YEAR_OF_ERA, yoeLong);
                    }
                } else {
                    // invent era
                    updateResolveMap(fieldValues, YEAR,
                            (year == null || year > 0 ? yoeLong : Math.subtractExact(1, yoeLong)));
                }
            } else if (era.longValue() == 1L) {
                updateResolveMap(fieldValues, YEAR, yoeLong);
            } else if (era.longValue() == 0L) {
                updateResolveMap(fieldValues, YEAR, Math.subtractExact(1, yoeLong));
            } else {
                throw new TDateTimeException("Invalid value for era: " + era);
            }
        } else if (fieldValues.containsKey(ERA)) {
            ERA.checkValidValue(fieldValues.get(ERA)); // always validated
        }

        // build date
        if (fieldValues.containsKey(YEAR)) {
            if (fieldValues.containsKey(MONTH_OF_YEAR)) {
                if (fieldValues.containsKey(DAY_OF_MONTH)) {
                    int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                    if (resolverStyle == TResolverStyle.LENIENT) {
                        long months = Math.subtractExact(fieldValues.remove(MONTH_OF_YEAR), 1);
                        long days = Math.subtractExact(fieldValues.remove(DAY_OF_MONTH), 1);
                        return date(y, 1, 1).plusMonths(months).plusDays(days);
                    } else {
                        int moy = range(MONTH_OF_YEAR).checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR),
                                MONTH_OF_YEAR);
                        int dom = range(DAY_OF_MONTH).checkValidIntValue(fieldValues.remove(DAY_OF_MONTH),
                                DAY_OF_MONTH);
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
                            long months = Math.subtractExact(fieldValues.remove(MONTH_OF_YEAR), 1);
                            long weeks = Math.subtractExact(fieldValues.remove(ALIGNED_WEEK_OF_MONTH), 1);
                            long days = Math.subtractExact(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_MONTH), 1);
                            return date(y, 1, 1).plus(months, MONTHS).plus(weeks, WEEKS).plus(days, DAYS);
                        }
                        int moy = MONTH_OF_YEAR.checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR));
                        int aw = ALIGNED_WEEK_OF_MONTH.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_MONTH));
                        int ad = ALIGNED_DAY_OF_WEEK_IN_MONTH
                                .checkValidIntValue(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_MONTH));
                        THijrahDate date = date(y, moy, 1).plus((aw - 1) * 7 + (ad - 1), DAYS);
                        if (resolverStyle == TResolverStyle.STRICT && date.get(MONTH_OF_YEAR) != moy) {
                            throw new TDateTimeException("Strict mode rejected date parsed to a different month");
                        }
                        return date;
                    }
                    if (fieldValues.containsKey(DAY_OF_WEEK)) {
                        int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                        if (resolverStyle == TResolverStyle.LENIENT) {
                            long months = Math.subtractExact(fieldValues.remove(MONTH_OF_YEAR), 1);
                            long weeks = Math.subtractExact(fieldValues.remove(ALIGNED_WEEK_OF_MONTH), 1);
                            long days = Math.subtractExact(fieldValues.remove(DAY_OF_WEEK), 1);
                            return date(y, 1, 1).plus(months, MONTHS).plus(weeks, WEEKS).plus(days, DAYS);
                        }
                        int moy = MONTH_OF_YEAR.checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR));
                        int aw = ALIGNED_WEEK_OF_MONTH.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_MONTH));
                        int dow = DAY_OF_WEEK.checkValidIntValue(fieldValues.remove(DAY_OF_WEEK));
                        THijrahDate date = date(y, moy, 1).plus(aw - 1, WEEKS).with(nextOrSame(TDayOfWeek.of(dow)));
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
                    long days = Math.subtractExact(fieldValues.remove(DAY_OF_YEAR), 1);
                    return dateYearDay(y, 1).plusDays(days);
                }
                int doy = DAY_OF_YEAR.checkValidIntValue(fieldValues.remove(DAY_OF_YEAR));
                return dateYearDay(y, doy);
            }
            if (fieldValues.containsKey(ALIGNED_WEEK_OF_YEAR)) {
                if (fieldValues.containsKey(ALIGNED_DAY_OF_WEEK_IN_YEAR)) {
                    int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                    if (resolverStyle == TResolverStyle.LENIENT) {
                        long weeks = Math.subtractExact(fieldValues.remove(ALIGNED_WEEK_OF_YEAR), 1);
                        long days = Math.subtractExact(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_YEAR), 1);
                        return date(y, 1, 1).plus(weeks, WEEKS).plus(days, DAYS);
                    }
                    int aw = ALIGNED_WEEK_OF_YEAR.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_YEAR));
                    int ad = ALIGNED_DAY_OF_WEEK_IN_YEAR
                            .checkValidIntValue(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_YEAR));
                    THijrahDate date = date(y, 1, 1).plusDays((aw - 1) * 7 + (ad - 1));
                    if (resolverStyle == TResolverStyle.STRICT && date.get(YEAR) != y) {
                        throw new TDateTimeException("Strict mode rejected date parsed to a different year");
                    }
                    return date;
                }
                if (fieldValues.containsKey(DAY_OF_WEEK)) {
                    int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                    if (resolverStyle == TResolverStyle.LENIENT) {
                        long weeks = Math.subtractExact(fieldValues.remove(ALIGNED_WEEK_OF_YEAR), 1);
                        long days = Math.subtractExact(fieldValues.remove(DAY_OF_WEEK), 1);
                        return date(y, 1, 1).plus(weeks, WEEKS).plus(days, DAYS);
                    }
                    int aw = ALIGNED_WEEK_OF_YEAR.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_YEAR));
                    int dow = DAY_OF_WEEK.checkValidIntValue(fieldValues.remove(DAY_OF_WEEK));
                    THijrahDate date = date(y, 1, 1).plus(aw - 1, WEEKS).with(nextOrSame(TDayOfWeek.of(dow)));
                    if (resolverStyle == TResolverStyle.STRICT && date.get(YEAR) != y) {
                        throw new TDateTimeException("Strict mode rejected date parsed to a different month");
                    }
                    return date;
                }
            }
        }
        return null;
    }

}
