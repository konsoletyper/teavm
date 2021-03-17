/*
 *  Copyright 2020 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
package org.threeten.bp.chrono;

import static org.threeten.bp.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static org.threeten.bp.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static org.threeten.bp.temporal.ChronoField.ALIGNED_WEEK_OF_MONTH;
import static org.threeten.bp.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR;
import static org.threeten.bp.temporal.ChronoField.DAY_OF_MONTH;
import static org.threeten.bp.temporal.ChronoField.DAY_OF_WEEK;
import static org.threeten.bp.temporal.ChronoField.DAY_OF_YEAR;
import static org.threeten.bp.temporal.ChronoField.EPOCH_DAY;
import static org.threeten.bp.temporal.ChronoField.ERA;
import static org.threeten.bp.temporal.ChronoField.MONTH_OF_YEAR;
import static org.threeten.bp.temporal.ChronoField.PROLEPTIC_MONTH;
import static org.threeten.bp.temporal.ChronoField.YEAR;
import static org.threeten.bp.temporal.ChronoField.YEAR_OF_ERA;
import static org.threeten.bp.temporal.ChronoUnit.DAYS;
import static org.threeten.bp.temporal.ChronoUnit.MONTHS;
import static org.threeten.bp.temporal.ChronoUnit.WEEKS;
import static org.threeten.bp.temporal.TemporalAdjusters.nextOrSame;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.threeten.bp.Clock;
import org.threeten.bp.DateTimeException;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.ResolverStyle;
import org.threeten.bp.jdk8.Jdk8Methods;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.TemporalAccessor;
import org.threeten.bp.temporal.TemporalField;
import org.threeten.bp.temporal.ValueRange;

/**
 * The Japanese Imperial calendar system.
 * <p>
 * This chronology defines the rules of the Japanese Imperial calendar system.
 * This calendar system is primarily used in Japan.
 * The Japanese Imperial calendar system is the same as the ISO calendar system
 * apart from the era-based year numbering.
 * <p>
 * Japan introduced the Gregorian calendar starting with Meiji 6.
 * Only Meiji and later eras are supported;
 * dates before Meiji 6, January 1 are not supported.
 * <p>
 * The supported {@code ChronoField} instances are:
 * <ul>
 * <li>{@code DAY_OF_WEEK}
 * <li>{@code DAY_OF_MONTH}
 * <li>{@code DAY_OF_YEAR}
 * <li>{@code EPOCH_DAY}
 * <li>{@code MONTH_OF_YEAR}
 * <li>{@code PROLEPTIC_MONTH}
 * <li>{@code YEAR_OF_ERA}
 * <li>{@code YEAR}
 * <li>{@code ERA}
 * </ul>
 *
 * <h3>Specification for implementors</h3>
 * This class is immutable and thread-safe.
 */
public final class JapaneseChronology extends Chronology implements Serializable {

    // Locale for creating a JapaneseImpericalCalendar.
    static final Locale LOCALE = new Locale("ja", "JP", "JP");

    /**
     * Singleton instance for Japanese chronology.
     */
    public static final JapaneseChronology INSTANCE = new JapaneseChronology();

    /**
     * Narrow names for eras.
     */
    private static final Map<String, String[]> ERA_NARROW_NAMES = new HashMap<String, String[]>();
    /**
     * Short names for eras.
     */
    private static final Map<String, String[]> ERA_SHORT_NAMES = new HashMap<String, String[]>();
    /**
     * Full names for eras.
     */
    private static final Map<String, String[]> ERA_FULL_NAMES = new HashMap<String, String[]>();
    /**
     * Fallback language for the era names.
     */
    private static final String FALLBACK_LANGUAGE = "en";
    /**
     * Language that has the era names.
     */
    private static final String TARGET_LANGUAGE = "ja";

    /**
     * Name data.
     */
    // TODO: replace all the hard-coded Maps with locale resources
    static {
        ERA_NARROW_NAMES.put(FALLBACK_LANGUAGE, new String[]{"Unknown", "K", "M", "T", "S", "H"});
        ERA_NARROW_NAMES.put(TARGET_LANGUAGE, new String[]{"Unknown", "K", "M", "T", "S", "H"});
        ERA_SHORT_NAMES.put(FALLBACK_LANGUAGE, new String[]{"Unknown", "K", "M", "T", "S", "H"});
        ERA_SHORT_NAMES.put(TARGET_LANGUAGE, new String[]{"Unknown", "\u6176", "\u660e", "\u5927", "\u662d", "\u5e73"});
        ERA_FULL_NAMES.put(FALLBACK_LANGUAGE, new String[]{"Unknown", "Keio", "Meiji", "Taisho", "Showa", "Heisei"});
        ERA_FULL_NAMES.put(TARGET_LANGUAGE,
                new String[]{"Unknown", "\u6176\u5fdc", "\u660e\u6cbb", "\u5927\u6b63", "\u662d\u548c",
                        "\u5e73\u6210"});
    }

    //-----------------------------------------------------------------------
    /**
     * Restricted constructor.
     */
    private JapaneseChronology() {
    }

    /**
     * Resolve singleton.
     *
     * @return the singleton instance, not null
     */
    private Object readResolve() {
        return INSTANCE;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the ID of the chronology - 'Japanese'.
     * <p>
     * The ID uniquely identifies the {@code Chronology}.
     * It can be used to lookup the {@code Chronology} using {@link #of(String)}.
     *
     * @return the chronology ID - 'Japanese'
     * @see #getCalendarType()
     */
    @Override
    public String getId() {
        return "Japanese";
    }

    /**
     * Gets the calendar type of the underlying calendar system - 'japanese'.
     * <p>
     * The calendar type is an identifier defined by the
     * <em>Unicode Locale Data Markup Language (LDML)</em> specification.
     * It can be used to lookup the {@code Chronology} using {@link #of(String)}.
     * It can also be used as part of a locale, accessible via
     * {@link Locale#getUnicodeLocaleType(String)} with the key 'ca'.
     *
     * @return the calendar system type - 'japanese'
     * @see #getId()
     */
    @Override
    public String getCalendarType() {
        return "japanese";
    }

    //-----------------------------------------------------------------------
    @Override  // override with covariant return type
    public JapaneseDate date(Era era, int yearOfEra, int month, int dayOfMonth) {
        if (!(era instanceof JapaneseEra)) {
            throw new ClassCastException("Era must be JapaneseEra");
        }
        return JapaneseDate.of((JapaneseEra) era, yearOfEra, month, dayOfMonth);
    }

    @Override  // override with covariant return type
    public JapaneseDate date(int prolepticYear, int month, int dayOfMonth) {
        return new JapaneseDate(LocalDate.of(prolepticYear, month, dayOfMonth));
    }

    /**
     * Obtains a local date in Japanese calendar system from the
     * era, year-of-era and day-of-year fields.
     * <p>
     * The day-of-year in this factory is expressed relative to the start of the year-of-era.
     * This definition changes the normal meaning of day-of-year only in those years
     * where the year-of-era is reset to one due to a change in the era.
     * For example:
     * <pre>
     *  6th Jan Showa 64 = day-of-year 6
     *  7th Jan Showa 64 = day-of-year 7
     *  8th Jan Heisei 1 = day-of-year 1
     *  9th Jan Heisei 1 = day-of-year 2
     * </pre>
     *
     * @param era  the Japanese era, not null
     * @param yearOfEra  the year-of-era
     * @param dayOfYear  the day-of-year
     * @return the Japanese local date, not null
     * @throws DateTimeException if unable to create the date
     * @throws ClassCastException if the {@code era} is not a {@code JapaneseEra}
     */
    @Override
    public JapaneseDate dateYearDay(Era era, int yearOfEra, int dayOfYear) {
        if (!(era instanceof JapaneseEra)) {
            throw new ClassCastException("Era must be JapaneseEra");
        }
        return JapaneseDate.ofYearDay((JapaneseEra) era, yearOfEra, dayOfYear);
    }

    /**
     * Obtains a local date in Japanese calendar system from the
     * proleptic-year and day-of-year fields.
     * <p>
     * The day-of-year in this factory is expressed relative to the start of the proleptic year.
     * The Japanese proleptic year and day-of-year are the same as those in the ISO calendar system.
     * They are not reset when the era changes.
     *
     * @param prolepticYear  the proleptic-year
     * @param dayOfYear  the day-of-year
     * @return the Japanese local date, not null
     * @throws DateTimeException if unable to create the date
     */
    @Override
    public JapaneseDate dateYearDay(int prolepticYear, int dayOfYear) {
        LocalDate date = LocalDate.ofYearDay(prolepticYear, dayOfYear);
        return date(prolepticYear, date.getMonthValue(), date.getDayOfMonth());
    }

    @Override
    public JapaneseDate dateEpochDay(long epochDay) {
        return new JapaneseDate(LocalDate.ofEpochDay(epochDay));
    }

    //-----------------------------------------------------------------------
    @Override  // override with covariant return type
    public JapaneseDate date(TemporalAccessor temporal) {
        if (temporal instanceof JapaneseDate) {
            return (JapaneseDate) temporal;
        }
        return new JapaneseDate(LocalDate.from(temporal));
    }

    @SuppressWarnings("unchecked")
    @Override  // override with covariant return type
    public ChronoLocalDateTime<JapaneseDate> localDateTime(TemporalAccessor temporal) {
        return (ChronoLocalDateTime<JapaneseDate>) super.localDateTime(temporal);
    }

    @SuppressWarnings("unchecked")
    @Override  // override with covariant return type
    public ChronoZonedDateTime<JapaneseDate> zonedDateTime(TemporalAccessor temporal) {
        return (ChronoZonedDateTime<JapaneseDate>) super.zonedDateTime(temporal);
    }

    @SuppressWarnings("unchecked")
    @Override  // override with covariant return type
    public ChronoZonedDateTime<JapaneseDate> zonedDateTime(Instant instant, ZoneId zone) {
        return (ChronoZonedDateTime<JapaneseDate>) super.zonedDateTime(instant, zone);
    }

    //-----------------------------------------------------------------------
    @Override  // override with covariant return type
    public JapaneseDate dateNow() {
        return (JapaneseDate) super.dateNow();
    }

    @Override  // override with covariant return type
    public JapaneseDate dateNow(ZoneId zone) {
        return (JapaneseDate) super.dateNow(zone);
    }

    @Override  // override with covariant return type
    public JapaneseDate dateNow(Clock clock) {
        Objects.requireNonNull(clock, "clock");
        return (JapaneseDate) super.dateNow(clock);
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if the specified year is a leap year.
     * <p>
     * Japanese calendar leap years occur exactly in line with ISO leap years.
     * This method does not validate the year passed in, and only has a
     * well-defined result for years in the supported range.
     *
     * @param prolepticYear  the proleptic-year to check, not validated for range
     * @return true if the year is a leap year
     */
    @Override
    public boolean isLeapYear(long prolepticYear) {
        return IsoChronology.INSTANCE.isLeapYear(prolepticYear);
    }

    @Override
    public int prolepticYear(Era era, int yearOfEra) {
        if (!(era instanceof JapaneseEra)) {
            throw new ClassCastException("Era must be JapaneseEra");
        }
        JapaneseEra jera = (JapaneseEra) era;
        int isoYear = jera.startDate().getYear() + yearOfEra - 1;
        ValueRange range = ValueRange.of(1, jera.endDate().getYear() - jera.startDate().getYear() + 1);
        range.checkValidValue(yearOfEra, YEAR_OF_ERA);
        return isoYear;
    }

    /**
     * Returns the calendar system era object from the given numeric value.
     *
     * See the description of each Era for the numeric values of:
     * {@link JapaneseEra#HEISEI}, {@link JapaneseEra#SHOWA},{@link JapaneseEra#TAISHO},
     * {@link JapaneseEra#MEIJI}), only Meiji and later eras are supported.
     *
     * @param eraValue  the era value
     * @return the Japanese {@code Era} for the given numeric era value
     * @throws DateTimeException if {@code eraValue} is invalid
     */
    @Override
    public JapaneseEra eraOf(int eraValue) {
        return JapaneseEra.of(eraValue);
    }

    @Override
    public List<Era> eras() {
        return Arrays.asList(JapaneseEra.values());
    }

    //-----------------------------------------------------------------------
    @Override
    public ValueRange range(ChronoField field) {
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
        Calendar jcal = Calendar.getInstance(LOCALE);
        switch (field) {
            case ERA: {
                JapaneseEra[] eras = JapaneseEra.values();
                return ValueRange.of(eras[0].getValue(), eras[eras.length - 1].getValue());
            }
            case YEAR: {
                JapaneseEra[] eras = JapaneseEra.values();
                return ValueRange.of(JapaneseDate.MIN_DATE.getYear(), eras[eras.length - 1].endDate().getYear());
            }
            case YEAR_OF_ERA: {
                JapaneseEra[] eras = JapaneseEra.values();
                int maxIso = eras[eras.length - 1].endDate().getYear();
                int maxJapanese = maxIso - eras[eras.length - 1].startDate().getYear() + 1;
                int min = Integer.MAX_VALUE;
                for (int i = 0; i < eras.length; i++) {
                    min = Math.min(min, eras[i].endDate().getYear() - eras[i].startDate().getYear() + 1);
                }
                return ValueRange.of(1, 6, min, maxJapanese);
            }
            case MONTH_OF_YEAR:
                return ValueRange.of(jcal.getMinimum(Calendar.MONTH) + 1, jcal.getGreatestMinimum(Calendar.MONTH) + 1,
                        jcal.getLeastMaximum(Calendar.MONTH) + 1, jcal.getMaximum(Calendar.MONTH) + 1);
            case DAY_OF_YEAR: {
                JapaneseEra[] eras = JapaneseEra.values();
                int min = 366;
                for (int i = 0; i < eras.length; i++) {
                    min = Math.min(min, eras[i].startDate().lengthOfYear() - eras[i].startDate().getDayOfYear() + 1);
                }
                return ValueRange.of(1, min, 366);
            }
            default:
                 // TODO: review the remaining fields
                throw new UnsupportedOperationException("Unimplementable field: " + field);
        }
    }

    @Override
    public JapaneseDate resolveDate(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        if (fieldValues.containsKey(EPOCH_DAY)) {
            return dateEpochDay(fieldValues.remove(EPOCH_DAY));
        }

        // normalize fields
        Long prolepticMonth = fieldValues.remove(PROLEPTIC_MONTH);
        if (prolepticMonth != null) {
            if (resolverStyle != ResolverStyle.LENIENT) {
                PROLEPTIC_MONTH.checkValidValue(prolepticMonth);
            }
            updateResolveMap(fieldValues, MONTH_OF_YEAR, Jdk8Methods.floorMod(prolepticMonth, 12) + 1);
            updateResolveMap(fieldValues, YEAR, Jdk8Methods.floorDiv(prolepticMonth, 12));
        }

        // eras
        Long eraLong = fieldValues.get(ERA);
        JapaneseEra era = null;
        if (eraLong != null) {
            era = eraOf(range(ERA).checkValidIntValue(eraLong, ERA));
        }
        Long yoeLong = fieldValues.get(YEAR_OF_ERA);
        if (yoeLong != null) {
            int yoe = range(YEAR_OF_ERA).checkValidIntValue(yoeLong, YEAR_OF_ERA);
            if (era == null && resolverStyle != ResolverStyle.STRICT && !fieldValues.containsKey(YEAR)) {
                List<Era> eras = eras();
                era = (JapaneseEra) eras.get(eras.size() - 1);
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
                    if (resolverStyle == ResolverStyle.LENIENT) {
                        long months = Jdk8Methods.safeSubtract(fieldValues.remove(MONTH_OF_YEAR), 1);
                        long days = Jdk8Methods.safeSubtract(fieldValues.remove(DAY_OF_MONTH), 1);
                        return date(y, 1, 1).plusMonths(months).plusDays(days);
                    } else {
                        int moy = range(MONTH_OF_YEAR).checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR),
                                MONTH_OF_YEAR);
                        int dom = range(DAY_OF_MONTH).checkValidIntValue(fieldValues.remove(DAY_OF_MONTH),
                                DAY_OF_MONTH);
                        if (resolverStyle == ResolverStyle.SMART && dom > 28) {
                            dom = Math.min(dom, date(y, moy, 1).lengthOfMonth());
                        }
                        return date(y, moy, dom);
                    }
                }
                if (fieldValues.containsKey(ALIGNED_WEEK_OF_MONTH)) {
                    if (fieldValues.containsKey(ALIGNED_DAY_OF_WEEK_IN_MONTH)) {
                        int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                        if (resolverStyle == ResolverStyle.LENIENT) {
                            long months = Jdk8Methods.safeSubtract(fieldValues.remove(MONTH_OF_YEAR), 1);
                            long weeks = Jdk8Methods.safeSubtract(fieldValues.remove(ALIGNED_WEEK_OF_MONTH), 1);
                            long days = Jdk8Methods.safeSubtract(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_MONTH), 1);
                            return date(y, 1, 1).plus(months, MONTHS).plus(weeks, WEEKS).plus(days, DAYS);
                        }
                        int moy = MONTH_OF_YEAR.checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR));
                        int aw = ALIGNED_WEEK_OF_MONTH.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_MONTH));
                        int ad = ALIGNED_DAY_OF_WEEK_IN_MONTH.checkValidIntValue(
                                fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_MONTH));
                        JapaneseDate date = date(y, moy, 1).plus((aw - 1) * 7 + (ad - 1), DAYS);
                        if (resolverStyle == ResolverStyle.STRICT && date.get(MONTH_OF_YEAR) != moy) {
                            throw new DateTimeException("Strict mode rejected date parsed to a different month");
                        }
                        return date;
                    }
                    if (fieldValues.containsKey(DAY_OF_WEEK)) {
                        int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                        if (resolverStyle == ResolverStyle.LENIENT) {
                            long months = Jdk8Methods.safeSubtract(fieldValues.remove(MONTH_OF_YEAR), 1);
                            long weeks = Jdk8Methods.safeSubtract(fieldValues.remove(ALIGNED_WEEK_OF_MONTH), 1);
                            long days = Jdk8Methods.safeSubtract(fieldValues.remove(DAY_OF_WEEK), 1);
                            return date(y, 1, 1).plus(months, MONTHS).plus(weeks, WEEKS).plus(days, DAYS);
                        }
                        int moy = MONTH_OF_YEAR.checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR));
                        int aw = ALIGNED_WEEK_OF_MONTH.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_MONTH));
                        int dow = DAY_OF_WEEK.checkValidIntValue(fieldValues.remove(DAY_OF_WEEK));
                        JapaneseDate date = date(y, moy, 1).plus(aw - 1, WEEKS).with(nextOrSame(DayOfWeek.of(dow)));
                        if (resolverStyle == ResolverStyle.STRICT && date.get(MONTH_OF_YEAR) != moy) {
                            throw new DateTimeException("Strict mode rejected date parsed to a different month");
                        }
                        return date;
                    }
                }
            }
            if (fieldValues.containsKey(DAY_OF_YEAR)) {
                int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                if (resolverStyle == ResolverStyle.LENIENT) {
                    long days = Jdk8Methods.safeSubtract(fieldValues.remove(DAY_OF_YEAR), 1);
                    return dateYearDay(y, 1).plusDays(days);
                }
                int doy = DAY_OF_YEAR.checkValidIntValue(fieldValues.remove(DAY_OF_YEAR));
                return dateYearDay(y, doy);
            }
            if (fieldValues.containsKey(ALIGNED_WEEK_OF_YEAR)) {
                if (fieldValues.containsKey(ALIGNED_DAY_OF_WEEK_IN_YEAR)) {
                    int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                    if (resolverStyle == ResolverStyle.LENIENT) {
                        long weeks = Jdk8Methods.safeSubtract(fieldValues.remove(ALIGNED_WEEK_OF_YEAR), 1);
                        long days = Jdk8Methods.safeSubtract(fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_YEAR), 1);
                        return date(y, 1, 1).plus(weeks, WEEKS).plus(days, DAYS);
                    }
                    int aw = ALIGNED_WEEK_OF_YEAR.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_YEAR));
                    int ad = ALIGNED_DAY_OF_WEEK_IN_YEAR.checkValidIntValue(
                            fieldValues.remove(ALIGNED_DAY_OF_WEEK_IN_YEAR));
                    JapaneseDate date = date(y, 1, 1).plusDays((aw - 1) * 7 + (ad - 1));
                    if (resolverStyle == ResolverStyle.STRICT && date.get(YEAR) != y) {
                        throw new DateTimeException("Strict mode rejected date parsed to a different year");
                    }
                    return date;
                }
                if (fieldValues.containsKey(DAY_OF_WEEK)) {
                    int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
                    if (resolverStyle == ResolverStyle.LENIENT) {
                        long weeks = Jdk8Methods.safeSubtract(fieldValues.remove(ALIGNED_WEEK_OF_YEAR), 1);
                        long days = Jdk8Methods.safeSubtract(fieldValues.remove(DAY_OF_WEEK), 1);
                        return date(y, 1, 1).plus(weeks, WEEKS).plus(days, DAYS);
                    }
                    int aw = ALIGNED_WEEK_OF_YEAR.checkValidIntValue(fieldValues.remove(ALIGNED_WEEK_OF_YEAR));
                    int dow = DAY_OF_WEEK.checkValidIntValue(fieldValues.remove(DAY_OF_WEEK));
                    JapaneseDate date = date(y, 1, 1).plus(aw - 1, WEEKS).with(nextOrSame(DayOfWeek.of(dow)));
                    if (resolverStyle == ResolverStyle.STRICT && date.get(YEAR) != y) {
                        throw new DateTimeException("Strict mode rejected date parsed to a different month");
                    }
                    return date;
                }
            }
        }
        return null;
    }

    private JapaneseDate resolveEYMD(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle,
            JapaneseEra era, int yoe) {
        if (resolverStyle == ResolverStyle.LENIENT) {
            int y = era.startDate().getYear() + yoe - 1;
            long months = Jdk8Methods.safeSubtract(fieldValues.remove(MONTH_OF_YEAR), 1);
            long days = Jdk8Methods.safeSubtract(fieldValues.remove(DAY_OF_MONTH), 1);
            return date(y, 1, 1).plus(months, MONTHS).plus(days, DAYS);
        }
        int moy = range(MONTH_OF_YEAR).checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR), MONTH_OF_YEAR);
        int dom = range(DAY_OF_MONTH).checkValidIntValue(fieldValues.remove(DAY_OF_MONTH), DAY_OF_MONTH);
        if (resolverStyle == ResolverStyle.SMART) {  // previous valid
            if (yoe < 1) {
                throw new DateTimeException("Invalid YearOfEra: " + yoe);
            }
            int y = era.startDate().getYear() + yoe - 1;
            if (dom > 28) {
                dom = Math.min(dom, date(y, moy, 1).lengthOfMonth());
            }
            JapaneseDate jd = date(y, moy, dom);
            if (jd.getEra() != era) {
                // ensure within calendar year of change
                if (Math.abs(jd.getEra().getValue() - era.getValue()) > 1) {
                    throw new DateTimeException("Invalid Era/YearOfEra: " + era + " " + yoe);
                }
                if (jd.get(YEAR_OF_ERA) != 1 && yoe != 1) {
                    throw new DateTimeException("Invalid Era/YearOfEra: " + era + " " + yoe);
                }
            }
            return jd;
        }
        return date(era, yoe, moy, dom);
    }

    private JapaneseDate resolveEYD(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle,
            JapaneseEra era, int yoe) {
        if (resolverStyle == ResolverStyle.LENIENT) {
            int y = era.startDate().getYear() + yoe - 1;
            long days = Jdk8Methods.safeSubtract(fieldValues.remove(DAY_OF_YEAR), 1);
            return dateYearDay(y, 1).plus(days, DAYS);
        }
        int doy = range(DAY_OF_YEAR).checkValidIntValue(fieldValues.remove(DAY_OF_YEAR), DAY_OF_YEAR);
        return dateYearDay(era, yoe, doy);  // smart is same as strict
    }

}
