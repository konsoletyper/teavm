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
package org.threeten.bp.temporal;

import static org.threeten.bp.temporal.ChronoField.DAY_OF_MONTH;
import static org.threeten.bp.temporal.ChronoField.DAY_OF_WEEK;
import static org.threeten.bp.temporal.ChronoField.DAY_OF_YEAR;
import static org.threeten.bp.temporal.ChronoField.MONTH_OF_YEAR;
import static org.threeten.bp.temporal.ChronoField.YEAR;
import static org.threeten.bp.temporal.ChronoUnit.DAYS;
import static org.threeten.bp.temporal.ChronoUnit.MONTHS;
import static org.threeten.bp.temporal.ChronoUnit.WEEKS;
import static org.threeten.bp.temporal.ChronoUnit.YEARS;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.threeten.bp.DateTimeException;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.Year;
import org.threeten.bp.chrono.ChronoLocalDate;
import org.threeten.bp.chrono.Chronology;
import org.threeten.bp.format.ResolverStyle;
import org.threeten.bp.jdk8.Jdk8Methods;

/**
 * Localized definitions of the day-of-week, week-of-month and week-of-year fields.
 * <p>
 * A standard week is seven days long, but cultures have different definitions for some
 * other aspects of a week. This class represents the definition of the week, for the
 * purpose of providing {@link TemporalField} instances.
 * <p>
 * WeekFields provides three fields,
 * {@link #dayOfWeek()}, {@link #weekOfMonth()}, and {@link #weekOfYear()}
 * that provide access to the values from any {@link Temporal temporal object}.
 * <p>
 * The computations for day-of-week, week-of-month, and week-of-year are based
 * on the  {@link ChronoField#YEAR proleptic-year},
 * {@link ChronoField#MONTH_OF_YEAR month-of-year},
 * {@link ChronoField#DAY_OF_MONTH day-of-month}, and
 * {@link ChronoField#DAY_OF_WEEK ISO day-of-week} which are based on the
 * {@link ChronoField#EPOCH_DAY epoch-day} and the chronology.
 * The values may not be aligned with the {@link ChronoField#YEAR_OF_ERA year-of-Era}
 * depending on the Chronology.
 * <p>A week is defined by:
 * <ul>
 * <li>The first day-of-week.
 * For example, the ISO-8601 standard considers Monday to be the first day-of-week.
 * <li>The minimal number of days in the first week.
 * For example, the ISO-8601 standard counts the first week as needing at least 4 days.
 * </ul><p>
 * Together these two values allow a year or month to be divided into weeks.
 * <p>
 * <h3>Week of Month</h3>
 * One field is used: week-of-month.
 * The calculation ensures that weeks never overlap a month boundary.
 * The month is divided into periods where each period starts on the defined first day-of-week.
 * The earliest period is referred to as week 0 if it has less than the minimal number of days
 * and week 1 if it has at least the minimal number of days.
 * <p>
 * <table cellpadding="0" cellspacing="3" border="0" style="text-align: left; width: 50%;">
 * <caption>Examples of WeekFields</caption>
 * <tr><th>Date</th><td>Day-of-week</td>
 *  <td>First day: Monday<br>Minimal days: 4</td><td>First day: Monday<br>Minimal days: 5</td></tr>
 * <tr><th>2008-12-31</th><td>Wednesday</td>
 *  <td>Week 5 of December 2008</td><td>Week 5 of December 2008</td></tr>
 * <tr><th>2009-01-01</th><td>Thursday</td>
 *  <td>Week 1 of January 2009</td><td>Week 0 of January 2009</td></tr>
 * <tr><th>2009-01-04</th><td>Sunday</td>
 *  <td>Week 1 of January 2009</td><td>Week 0 of January 2009</td></tr>
 * <tr><th>2009-01-05</th><td>Monday</td>
 *  <td>Week 2 of January 2009</td><td>Week 1 of January 2009</td></tr>
 * </table>
 * <p>
 * <h3>Week of Year</h3>
 * One field is used: week-of-year.
 * The calculation ensures that weeks never overlap a year boundary.
 * The year is divided into periods where each period starts on the defined first day-of-week.
 * The earliest period is referred to as week 0 if it has less than the minimal number of days
 * and week 1 if it has at least the minimal number of days.
 * <p>
 * This class is immutable and thread-safe.
 */
public final class WeekFields implements Serializable {
    // implementation notes
    // querying week-of-month or week-of-year should return the week value bound within the month/year
    // however, setting the week value should be lenient (use plus/minus weeks)
    // allow week-of-month outer range [0 to 5]
    // allow week-of-year outer range [0 to 53]
    // this is because callers shouldn't be expected to know the details of validity

    /**
     * The cache of rules by firstDayOfWeek plus minimalDays.
     * Initialized first to be available for definition of ISO, etc.
     */
    private static final Map<String, WeekFields> CACHE = new HashMap<>();

    /**
     * The ISO-8601 definition, where a week starts on Monday and the first week
     * has a minimum of 4 days.
     * <p>
     * The ISO-8601 standard defines a calendar system based on weeks.
     * It uses the week-based-year and week-of-week-based-year concepts to split
     * up the passage of days instead of the standard year/month/day.
     * <p>
     * Note that the first week may start in the previous calendar year.
     * Note also that the first few days of a calendar year may be in the
     * week-based-year corresponding to the previous calendar year.
     */
    public static final WeekFields ISO = new WeekFields(DayOfWeek.MONDAY, 4);

    /**
     * The common definition of a week that starts on Sunday.
     * <p>
     * Defined as starting on Sunday and with a minimum of 1 day in the month.
     * This week definition is in use in the US and other European countries.
     *
     */
    public static final WeekFields SUNDAY_START = WeekFields.of(DayOfWeek.SUNDAY, 1);

    /**
     * The first day-of-week.
     */
    private final DayOfWeek firstDayOfWeek;
    /**
     * The minimal number of days in the first week.
     */
    private final int minimalDays;

    /**
     * The field used to access the computed DayOfWeek.
     */
    private transient final TemporalField dayOfWeek = ComputedDayOfField.ofDayOfWeekField(this);
    /**
     * The field used to access the computed WeekOfMonth.
     */
    private transient final TemporalField weekOfMonth = ComputedDayOfField.ofWeekOfMonthField(this);
    /**
     * The field used to access the computed WeekOfYear.
     */
    private transient final TemporalField weekOfYear = ComputedDayOfField.ofWeekOfYearField(this);
    /**
     * The field used to access the computed WeekOfWeekBasedYear.
     */
    private transient final TemporalField weekOfWeekBasedYear = ComputedDayOfField.ofWeekOfWeekBasedYearField(this);
    /**
     * The field used to access the computed WeekBasedYear.
     */
    private transient final TemporalField weekBasedYear = ComputedDayOfField.ofWeekBasedYearField(this);

    /**
     * Obtains an instance of {@code WeekFields} appropriate for a locale.
     * <p>
     * This will look up appropriate values from the provider of localization data.
     *
     * @param locale  the locale to use, not null
     * @return the week-definition, not null
     */
    public static WeekFields of(Locale locale) {
        Objects.requireNonNull(locale, "locale");
        locale = new Locale(locale.getLanguage(), locale.getCountry());  // elminate variants

        // obtain these from GregorianCalendar for now
        GregorianCalendar gcal = new GregorianCalendar(locale);
        int calDow = gcal.getFirstDayOfWeek();
        DayOfWeek dow = DayOfWeek.SUNDAY.plus(calDow - 1);
        int minDays = gcal.getMinimalDaysInFirstWeek();
        return WeekFields.of(dow, minDays);
    }

    /**
     * Obtains an instance of {@code WeekFields} from the first day-of-week and minimal days.
     * <p>
     * The first day-of-week defines the ISO {@code DayOfWeek} that is day 1 of the week.
     * The minimal number of days in the first week defines how many days must be present
     * in a month or year, starting from the first day-of-week, before the week is counted
     * as the first week. A value of 1 will count the first day of the month or year as part
     * of the first week, whereas a value of 7 will require the whole seven days to be in
     * the new month or year.
     * <p>
     * WeekFields instances are singletons; for each unique combination
     * of {@code firstDayOfWeek} and {@code minimalDaysInFirstWeek} the
     * the same instance will be returned.
     *
     * @param firstDayOfWeek  the first day of the week, not null
     * @param minimalDaysInFirstWeek  the minimal number of days in the first week, from 1 to 7
     * @return the week-definition, not null
     * @throws IllegalArgumentException if the minimal days value is less than one
     *      or greater than 7
     */
    public static WeekFields of(DayOfWeek firstDayOfWeek, int minimalDaysInFirstWeek) {
        String key = firstDayOfWeek.toString() + minimalDaysInFirstWeek;
        WeekFields rules = CACHE.get(key);
        if (rules == null) {
            rules = new WeekFields(firstDayOfWeek, minimalDaysInFirstWeek);
            CACHE.putIfAbsent(key, rules);
            rules = CACHE.get(key);
        }
        return rules;
    }

    //-----------------------------------------------------------------------
    /**
     * Creates an instance of the definition.
     *
     * @param firstDayOfWeek  the first day of the week, not null
     * @param minimalDaysInFirstWeek  the minimal number of days in the first week, from 1 to 7
     * @throws IllegalArgumentException if the minimal days value is invalid
     */
    private WeekFields(DayOfWeek firstDayOfWeek, int minimalDaysInFirstWeek) {
        Objects.requireNonNull(firstDayOfWeek, "firstDayOfWeek");
        if (minimalDaysInFirstWeek < 1 || minimalDaysInFirstWeek > 7) {
            throw new IllegalArgumentException("Minimal number of days is invalid");
        }
        this.firstDayOfWeek = firstDayOfWeek;
        this.minimalDays = minimalDaysInFirstWeek;
    }

    /**
     * Ensure valid singleton.
     * 
     * @return the valid week fields instance, not null
     * @throws InvalidObjectException if invalid
     */
    private Object readResolve() throws InvalidObjectException {
        try {
            return WeekFields.of(firstDayOfWeek, minimalDays);
        } catch (IllegalArgumentException ex) {
            throw new InvalidObjectException("Invalid WeekFields" + ex.getMessage());
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the first day-of-week.
     * <p>
     * The first day-of-week varies by culture.
     * For example, the US uses Sunday, while France and the ISO-8601 standard use Monday.
     * This method returns the first day using the standard {@code DayOfWeek} enum.
     *
     * @return the first day-of-week, not null
     */
    public DayOfWeek getFirstDayOfWeek() {
        return firstDayOfWeek;
    }

    /**
     * Gets the minimal number of days in the first week.
     * <p>
     * The number of days considered to define the first week of a month or year
     * varies by culture.
     * For example, the ISO-8601 requires 4 days (more than half a week) to
     * be present before counting the first week.
     *
     * @return the minimal number of days in the first week of a month or year, from 1 to 7
     */
    public int getMinimalDaysInFirstWeek() {
        return minimalDays;
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a field to access the day of week based on this {@code WeekFields}.
     * <p>
     * This is similar to {@link ChronoField#DAY_OF_WEEK} but uses values for
     * the day-of-week based on this {@code WeekFields}.
     * The days are numbered from 1 to 7 where the
     * {@link #getFirstDayOfWeek() first day-of-week} is assigned the value 1.
     * <p>
     * For example, if the first day-of-week is Sunday, then that will have the
     * value 1, with other days ranging from Monday as 2 to Saturday as 7.
     * <p>
     * In the resolving phase of parsing, a localized day-of-week will be converted
     * to a standardized {@code ChronoField} day-of-week.
     * The day-of-week must be in the valid range 1 to 7.
     * Other fields in this class build dates using the standardized day-of-week.
     *
     * @return a field providing access to the day-of-week with localized numbering, not null
     */
    public TemporalField dayOfWeek() {
        return dayOfWeek;
    }

    /**
     * Returns a field to access the week of month based on this {@code WeekFields}.
     * <p>
     * This represents the concept of the count of weeks within the month where weeks
     * start on a fixed day-of-week, such as Monday.
     * This field is typically used with {@link WeekFields#dayOfWeek()}.
     * <p>
     * Week one (1) is the week starting on the {@link WeekFields#getFirstDayOfWeek}
     * where there are at least {@link WeekFields#getMinimalDaysInFirstWeek()} days in the month.
     * Thus, week one may start up to {@code minDays} days before the start of the month.
     * If the first week starts after the start of the month then the period before is week zero (0).
     * <p>
     * For example:<br>
     * - if the 1st day of the month is a Monday, week one starts on the 1st and there is no week zero<br>
     * - if the 2nd day of the month is a Monday, week one starts on the 2nd and the 1st is in week zero<br>
     * - if the 4th day of the month is a Monday, week one starts on the 4th and the 1st to 3rd is in week zero<br>
     * - if the 5th day of the month is a Monday, week two starts on the 5th and the 1st to 4th is in week one<br>
     * <p>
     * This field can be used with any calendar system.
     * <p>
     * In the resolving phase of parsing, a date can be created from a year,
     * week-of-month, month-of-year and day-of-week.
     * <p>
     * In {@linkplain ResolverStyle#STRICT strict mode}, all four fields are
     * validated against their range of valid values. The week-of-month field
     * is validated to ensure that the resulting month is the month requested.
     * <p>
     * In {@linkplain ResolverStyle#SMART smart mode}, all four fields are
     * validated against their range of valid values. The week-of-month field
     * is validated from 0 to 6, meaning that the resulting date can be in a
     * different month to that specified.
     * <p>
     * In {@linkplain ResolverStyle#LENIENT lenient mode}, the year and day-of-week
     * are validated against the range of valid values. The resulting date is calculated
     * equivalent to the following four stage approach.
     * First, create a date on the first day of the first week of January in the requested year.
     * Then take the month-of-year, subtract one, and add the amount in months to the date.
     * Then take the week-of-month, subtract one, and add the amount in weeks to the date.
     * Finally, adjust to the correct day-of-week within the localized week.
     *
     * @return a field providing access to the week-of-month, not null
     */
    public TemporalField weekOfMonth() {
        return weekOfMonth;
    }

    /**
     * Returns a field to access the week of year based on this {@code WeekFields}.
     * <p>
     * This represents the concept of the count of weeks within the year where weeks
     * start on a fixed day-of-week, such as Monday.
     * This field is typically used with {@link WeekFields#dayOfWeek()}.
     * <p>
     * Week one(1) is the week starting on the {@link WeekFields#getFirstDayOfWeek}
     * where there are at least {@link WeekFields#getMinimalDaysInFirstWeek()} days in the year.
     * Thus, week one may start up to {@code minDays} days before the start of the year.
     * If the first week starts after the start of the year then the period before is week zero (0).
     * <p>
     * For example:<br>
     * - if the 1st day of the year is a Monday, week one starts on the 1st and there is no week zero<br>
     * - if the 2nd day of the year is a Monday, week one starts on the 2nd and the 1st is in week zero<br>
     * - if the 4th day of the year is a Monday, week one starts on the 4th and the 1st to 3rd is in week zero<br>
     * - if the 5th day of the year is a Monday, week two starts on the 5th and the 1st to 4th is in week one<br>
     * <p>
     * This field can be used with any calendar system.
     * <p>
     * In the resolving phase of parsing, a date can be created from a year,
     * week-of-year and day-of-week.
     * <p>
     * In {@linkplain ResolverStyle#STRICT strict mode}, all three fields are
     * validated against their range of valid values. The week-of-year field
     * is validated to ensure that the resulting year is the year requested.
     * <p>
     * In {@linkplain ResolverStyle#SMART smart mode}, all three fields are
     * validated against their range of valid values. The week-of-year field
     * is validated from 0 to 54, meaning that the resulting date can be in a
     * different year to that specified.
     * <p>
     * In {@linkplain ResolverStyle#LENIENT lenient mode}, the year and day-of-week
     * are validated against the range of valid values. The resulting date is calculated
     * equivalent to the following three stage approach.
     * First, create a date on the first day of the first week in the requested year.
     * Then take the week-of-year, subtract one, and add the amount in weeks to the date.
     * Finally, adjust to the correct day-of-week within the localized week.
     *
     * @return a field providing access to the week-of-year, not null
     */
    public TemporalField weekOfYear() {
        return weekOfYear;
    }

    /**
     * Returns a field to access the week of a week-based-year based on this {@code WeekFields}.
     * <p>
     * This represents the concept of the count of weeks within the year where weeks
     * start on a fixed day-of-week, such as Monday and each week belongs to exactly one year.
     * This field is typically used with {@link WeekFields#dayOfWeek()} and
     * {@link WeekFields#weekBasedYear()}.
     * <p>
     * Week one(1) is the week starting on the {@link WeekFields#getFirstDayOfWeek}
     * where there are at least {@link WeekFields#getMinimalDaysInFirstWeek()} days in the year.
     * If the first week starts after the start of the year then the period before
     * is in the last week of the previous year.
     * <p>
     * For example:<br>
     * - if the 1st day of the year is a Monday, week one starts on the 1st<br>
     * - if the 2nd day of the year is a Monday, week one starts on the 2nd and
     *   the 1st is in the last week of the previous year<br>
     * - if the 4th day of the year is a Monday, week one starts on the 4th and
     *   the 1st to 3rd is in the last week of the previous year<br>
     * - if the 5th day of the year is a Monday, week two starts on the 5th and
     *   the 1st to 4th is in week one<br>
     * <p>
     * This field can be used with any calendar system.
     * <p>
     * In the resolving phase of parsing, a date can be created from a week-based-year,
     * week-of-year and day-of-week.
     * <p>
     * In {@linkplain ResolverStyle#STRICT strict mode}, all three fields are
     * validated against their range of valid values. The week-of-year field
     * is validated to ensure that the resulting week-based-year is the
     * week-based-year requested.
     * <p>
     * In {@linkplain ResolverStyle#SMART smart mode}, all three fields are
     * validated against their range of valid values. The week-of-week-based-year field
     * is validated from 1 to 53, meaning that the resulting date can be in the
     * following week-based-year to that specified.
     * <p>
     * In {@linkplain ResolverStyle#LENIENT lenient mode}, the year and day-of-week
     * are validated against the range of valid values. The resulting date is calculated
     * equivalent to the following three stage approach.
     * First, create a date on the first day of the first week in the requested week-based-year.
     * Then take the week-of-week-based-year, subtract one, and add the amount in weeks to the date.
     * Finally, adjust to the correct day-of-week within the localized week.
     *
     * @return a field providing access to the week-of-week-based-year, not null
     */
    public TemporalField weekOfWeekBasedYear() {
        return weekOfWeekBasedYear;
    }

    /**
     * Returns a field to access the year of a week-based-year based on this {@code WeekFields}.
     * <p>
     * This represents the concept of the year where weeks start on a fixed day-of-week,
     * such as Monday and each week belongs to exactly one year.
     * This field is typically used with {@link WeekFields#dayOfWeek()} and
     * {@link WeekFields#weekOfWeekBasedYear()}.
     * <p>
     * Week one(1) is the week starting on the {@link WeekFields#getFirstDayOfWeek}
     * where there are at least {@link WeekFields#getMinimalDaysInFirstWeek()} days in the year.
     * Thus, week one may start before the start of the year.
     * If the first week starts after the start of the year then the period before
     * is in the last week of the previous year.
     * <p>
     * This field can be used with any calendar system.
     * <p>
     * In the resolving phase of parsing, a date can be created from a week-based-year,
     * week-of-year and day-of-week.
     * <p>
     * In {@linkplain ResolverStyle#STRICT strict mode}, all three fields are
     * validated against their range of valid values. The week-of-year field
     * is validated to ensure that the resulting week-based-year is the
     * week-based-year requested.
     * <p>
     * In {@linkplain ResolverStyle#SMART smart mode}, all three fields are
     * validated against their range of valid values. The week-of-week-based-year field
     * is validated from 1 to 53, meaning that the resulting date can be in the
     * following week-based-year to that specified.
     * <p>
     * In {@linkplain ResolverStyle#LENIENT lenient mode}, the year and day-of-week
     * are validated against the range of valid values. The resulting date is calculated
     * equivalent to the following three stage approach.
     * First, create a date on the first day of the first week in the requested week-based-year.
     * Then take the week-of-week-based-year, subtract one, and add the amount in weeks to the date.
     * Finally, adjust to the correct day-of-week within the localized week.
     *
     * @return a field providing access to the week-based-year, not null
     */
    public TemporalField weekBasedYear() {
        return weekBasedYear;
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if this {@code WeekFields} is equal to the specified object.
     * <p>
     * The comparison is based on the entire state of the rules, which is
     * the first day-of-week and minimal days.
     *
     * @param object  the other rules to compare to, null returns false
     * @return true if this is equal to the specified rules
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof WeekFields) {
            return hashCode() == object.hashCode();
        }
        return false;
    }

    /**
     * A hash code for this {@code WeekFields}.
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return firstDayOfWeek.ordinal() * 7 + minimalDays;
    }

    //-----------------------------------------------------------------------
    /**
     * A string representation of this {@code WeekFields} instance.
     *
     * @return the string representation, not null
     */
    @Override
    public String toString() {
        return "WeekFields[" + firstDayOfWeek + ',' + minimalDays + ']';
    }

    //-----------------------------------------------------------------------
    /**
     * Field type that computes DayOfWeek, WeekOfMonth, and WeekOfYear
     * based on a WeekFields.
     * A separate Field instance is required for each different WeekFields;
     * combination of start of week and minimum number of days.
     * Constructors are provided to create fields for DayOfWeek, WeekOfMonth,
     * and WeekOfYear.
     */
    static class ComputedDayOfField implements TemporalField {

        /**
         * Returns a field to access the day of week,
         * computed based on a WeekFields.
         * <p>
         * The WeekDefintion of the first day of the week is used with
         * the ISO DAY_OF_WEEK field to compute week boundaries.
         */
        static ComputedDayOfField ofDayOfWeekField(WeekFields weekDef) {
            return new ComputedDayOfField("DayOfWeek", weekDef,
                    ChronoUnit.DAYS, ChronoUnit.WEEKS, DAY_OF_WEEK_RANGE);
        }

        /**
         * Returns a field to access the week of month,
         * computed based on a WeekFields.
         * @see WeekFields#weekOfMonth()
         */
        static ComputedDayOfField ofWeekOfMonthField(WeekFields weekDef) {
            return new ComputedDayOfField("WeekOfMonth", weekDef,
                    ChronoUnit.WEEKS, ChronoUnit.MONTHS, WEEK_OF_MONTH_RANGE);
        }

        /**
         * Returns a field to access the week of year,
         * computed based on a WeekFields.
         * @see WeekFields#weekOfYear()
         */
        static ComputedDayOfField ofWeekOfYearField(WeekFields weekDef) {
            return new ComputedDayOfField("WeekOfYear", weekDef,
                    ChronoUnit.WEEKS, ChronoUnit.YEARS, WEEK_OF_YEAR_RANGE);
        }

        /**
         * Returns a field to access the week of week-based-year,
         * computed based on a WeekFields.
         * @see WeekFields#weekOfWeekBasedYear()
         */
        static ComputedDayOfField ofWeekOfWeekBasedYearField(WeekFields weekDef) {
            return new ComputedDayOfField("WeekOfWeekBasedYear", weekDef,
                    ChronoUnit.WEEKS, IsoFields.WEEK_BASED_YEARS, WEEK_OF_WEEK_BASED_YEAR_RANGE);
        }

        /**
         * Returns a field to access the week-based-year,
         * computed based on a WeekFields.
         * @see WeekFields#weekBasedYear()
         */
        static ComputedDayOfField ofWeekBasedYearField(WeekFields weekDef) {
            return new ComputedDayOfField("WeekBasedYear", weekDef,
                    IsoFields.WEEK_BASED_YEARS, ChronoUnit.FOREVER, WEEK_BASED_YEAR_RANGE);
        }

        private final String name;
        private final WeekFields weekDef;
        private final TemporalUnit baseUnit;
        private final TemporalUnit rangeUnit;
        private final ValueRange range;

        private ComputedDayOfField(String name, WeekFields weekDef, TemporalUnit baseUnit, TemporalUnit rangeUnit,
                ValueRange range) {
            this.name = name;
            this.weekDef = weekDef;
            this.baseUnit = baseUnit;
            this.rangeUnit = rangeUnit;
            this.range = range;
        }

        private static final ValueRange DAY_OF_WEEK_RANGE = ValueRange.of(1, 7);
        private static final ValueRange WEEK_OF_MONTH_RANGE = ValueRange.of(0, 1, 4, 6);
        private static final ValueRange WEEK_OF_YEAR_RANGE = ValueRange.of(0, 1, 52, 54);
        private static final ValueRange WEEK_OF_WEEK_BASED_YEAR_RANGE = ValueRange.of(1, 52, 53);
        private static final ValueRange WEEK_BASED_YEAR_RANGE = YEAR.range();

        @Override
        public long getFrom(TemporalAccessor temporal) {
            // Offset the ISO DOW by the start of this week
            int sow = weekDef.getFirstDayOfWeek().getValue();
            int isoDow = temporal.get(ChronoField.DAY_OF_WEEK);
            int dow = Jdk8Methods.floorMod(isoDow - sow, 7) + 1;

            if (rangeUnit == ChronoUnit.WEEKS) {
                return dow;
            } else if (rangeUnit == ChronoUnit.MONTHS) {
                int dom = temporal.get(ChronoField.DAY_OF_MONTH);
                int offset = startOfWeekOffset(dom, dow);
                return computeWeek(offset, dom);
            } else if (rangeUnit == ChronoUnit.YEARS) {
                int doy = temporal.get(ChronoField.DAY_OF_YEAR);
                int offset = startOfWeekOffset(doy, dow);
                return computeWeek(offset, doy);
            } else if (rangeUnit == IsoFields.WEEK_BASED_YEARS) {
                return localizedWOWBY(temporal);
            } else if (rangeUnit == ChronoUnit.FOREVER) {
                return localizedWBY(temporal);
            } else {
                throw new IllegalStateException("unreachable");
            }
        }

        private int localizedDayOfWeek(TemporalAccessor temporal, int sow) {
            int isoDow = temporal.get(DAY_OF_WEEK);
            return Jdk8Methods.floorMod(isoDow - sow, 7) + 1;
        }

        private long localizedWeekOfMonth(TemporalAccessor temporal, int dow) {
            int dom = temporal.get(DAY_OF_MONTH);
            int offset = startOfWeekOffset(dom, dow);
            return computeWeek(offset, dom);
        }

        private long localizedWeekOfYear(TemporalAccessor temporal, int dow) {
            int doy = temporal.get(DAY_OF_YEAR);
            int offset = startOfWeekOffset(doy, dow);
            return computeWeek(offset, doy);
        }

        private int localizedWOWBY(TemporalAccessor temporal) {
            int sow = weekDef.getFirstDayOfWeek().getValue();
            int isoDow = temporal.get(DAY_OF_WEEK);
            int dow = Jdk8Methods.floorMod(isoDow - sow, 7) + 1;
            long woy = localizedWeekOfYear(temporal, dow);
            if (woy == 0) {
                ChronoLocalDate previous = Chronology.from(temporal).date(temporal).minus(1, ChronoUnit.WEEKS);
                return (int) localizedWeekOfYear(previous, dow) + 1;
            } else if (woy >= 53) {
                int offset = startOfWeekOffset(temporal.get(DAY_OF_YEAR), dow);
                int year = temporal.get(YEAR);
                int yearLen = Year.isLeap(year) ? 366 : 365;
                int weekIndexOfFirstWeekNextYear = computeWeek(offset, yearLen + weekDef.getMinimalDaysInFirstWeek());
                if (woy >= weekIndexOfFirstWeekNextYear) {
                    return (int) (woy - (weekIndexOfFirstWeekNextYear - 1));
                }
            }
            return (int) woy;
        }

        private int localizedWBY(TemporalAccessor temporal) {
            int sow = weekDef.getFirstDayOfWeek().getValue();
            int isoDow = temporal.get(DAY_OF_WEEK);
            int dow = Jdk8Methods.floorMod(isoDow - sow, 7) + 1;
            int year = temporal.get(YEAR);
            long woy = localizedWeekOfYear(temporal, dow);
            if (woy == 0) {
                return year - 1;
            } else if (woy < 53) {
                return year;
            }
            int offset = startOfWeekOffset(temporal.get(DAY_OF_YEAR), dow);
            int yearLen = Year.isLeap(year) ? 366 : 365;
            int weekIndexOfFirstWeekNextYear = computeWeek(offset, yearLen + weekDef.getMinimalDaysInFirstWeek());
            if (woy >= weekIndexOfFirstWeekNextYear) {
                return year + 1;
            }
            return year;
        }

        /**
         * Returns an offset to align week start with a day of month or day of year.
         *
         * @param day the day; 1 through infinity
         * @param dow the day of the week of that day; 1 through 7
         * @return an offset in days to align a day with the start of the first 'full' week
         */
        private int startOfWeekOffset(int day, int dow) {
            // offset of first day corresponding to the day of week in first 7 days (zero origin)
            int weekStart = Jdk8Methods.floorMod(day - dow, 7);
            int offset = -weekStart;
            if (weekStart + 1 > weekDef.getMinimalDaysInFirstWeek()) {
                // The previous week has the minimum days in the current month to be a 'week'
                offset = 7 - weekStart;
            }
            return offset;
        }

        /**
         * Returns the week number computed from the reference day and reference dayOfWeek.
         *
         * @param offset the offset to align a date with the start of week
         *     from {@link #startOfWeekOffset}.
         * @param day  the day for which to compute the week number
         * @return the week number where zero is used for a partial week and 1 for the first full week
         */
        private int computeWeek(int offset, int day) {
            return (7 + offset + (day - 1)) / 7;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R extends Temporal> R adjustInto(R temporal, long newValue) {
            // Check the new value and get the old value of the field
            int newVal = range.checkValidIntValue(newValue, this);
            int currentVal = temporal.get(this);
            if (newVal == currentVal) {
                return temporal;
            }
            if (rangeUnit == ChronoUnit.FOREVER) {
                // adjust in whole weeks so dow never changes
                int baseWowby = temporal.get(weekDef.weekOfWeekBasedYear);
                long diffWeeks = (long) ((newValue - currentVal) * 52.1775);
                Temporal result = temporal.plus(diffWeeks, ChronoUnit.WEEKS);
                if (result.get(this) > newVal) {
                    // ended up in later week-based-year
                    // move to last week of previous year
                    int newWowby = result.get(weekDef.weekOfWeekBasedYear);
                    result = result.minus(newWowby, ChronoUnit.WEEKS);
                } else {
                    if (result.get(this) < newVal) {
                        // ended up in earlier week-based-year
                        result = result.plus(2, ChronoUnit.WEEKS);
                    }
                    // reset the week-of-week-based-year
                    int newWowby = result.get(weekDef.weekOfWeekBasedYear);
                    result = result.plus(baseWowby - newWowby, ChronoUnit.WEEKS);
                    if (result.get(this) > newVal) {
                        result = result.minus(1, ChronoUnit.WEEKS);
                    }
                }
                return (R) result;
            }
            // Compute the difference and add that using the base using of the field
            int delta = newVal - currentVal;
            return (R) temporal.plus(delta, baseUnit);
        }

        @Override
        public TemporalAccessor resolve(Map<TemporalField, Long> fieldValues,
                        TemporalAccessor partialTemporal, ResolverStyle resolverStyle) {
            int sow = weekDef.getFirstDayOfWeek().getValue();
            if (rangeUnit == WEEKS) {  // day-of-week
                final long value = fieldValues.remove(this);
                int localDow = range.checkValidIntValue(value, this);
                int isoDow = Jdk8Methods.floorMod((sow - 1) + (localDow - 1), 7) + 1;
                fieldValues.put(DAY_OF_WEEK, (long) isoDow);
                return null;
            }
            if (!fieldValues.containsKey(DAY_OF_WEEK)) {
                return null;
            }
            
            // week-based-year
            if (rangeUnit == ChronoUnit.FOREVER) {
                if (!fieldValues.containsKey(weekDef.weekOfWeekBasedYear)) {
                    return null;
                }
                Chronology chrono = Chronology.from(partialTemporal);  // defaults to ISO
                int isoDow = DAY_OF_WEEK.checkValidIntValue(fieldValues.get(DAY_OF_WEEK));
                int dow = Jdk8Methods.floorMod(isoDow - sow, 7) + 1;
                final int wby = range().checkValidIntValue(fieldValues.get(this), this);
                ChronoLocalDate date;
                long days;
                if (resolverStyle == ResolverStyle.LENIENT) {
                    date = chrono.date(wby, 1, weekDef.getMinimalDaysInFirstWeek());
                    long wowby = fieldValues.get(weekDef.weekOfWeekBasedYear);
                    int dateDow = localizedDayOfWeek(date, sow);
                    long weeks = wowby - localizedWeekOfYear(date, dateDow);
                    days = weeks * 7 + (dow - dateDow);
                } else {
                    date = chrono.date(wby, 1, weekDef.getMinimalDaysInFirstWeek());
                    long wowby = weekDef.weekOfWeekBasedYear.range().checkValidIntValue(
                                    fieldValues.get(weekDef.weekOfWeekBasedYear), weekDef.weekOfWeekBasedYear);
                    int dateDow = localizedDayOfWeek(date, sow);
                    long weeks = wowby - localizedWeekOfYear(date, dateDow);
                    days = weeks * 7 + (dow - dateDow);
                }
                date = date.plus(days, DAYS);
                if (resolverStyle == ResolverStyle.STRICT) {
                    if (date.getLong(this) != fieldValues.get(this)) {
                        throw new DateTimeException("Strict mode rejected date parsed to a different year");
                    }
                }
                fieldValues.remove(this);
                fieldValues.remove(weekDef.weekOfWeekBasedYear);
                fieldValues.remove(DAY_OF_WEEK);
                return date;
            }
            
            if (!fieldValues.containsKey(YEAR)) {
                return null;
            }
            int isoDow = DAY_OF_WEEK.checkValidIntValue(fieldValues.get(DAY_OF_WEEK));
            int dow = Jdk8Methods.floorMod(isoDow - sow, 7) + 1;
            int year = YEAR.checkValidIntValue(fieldValues.get(YEAR));
            Chronology chrono = Chronology.from(partialTemporal);  // defaults to ISO
            if (rangeUnit == MONTHS) {  // week-of-month
                if (!fieldValues.containsKey(MONTH_OF_YEAR)) {
                    return null;
                }
                final long value = fieldValues.remove(this);
                ChronoLocalDate date;
                long days;
                if (resolverStyle == ResolverStyle.LENIENT) {
                    long month = fieldValues.get(MONTH_OF_YEAR);
                    date = chrono.date(year, 1, 1);
                    date = date.plus(month - 1, MONTHS);
                    int dateDow = localizedDayOfWeek(date, sow);
                    long weeks = value - localizedWeekOfMonth(date, dateDow);
                    days = weeks * 7 + (dow - dateDow);
                } else {
                    int month = MONTH_OF_YEAR.checkValidIntValue(fieldValues.get(MONTH_OF_YEAR));
                    date = chrono.date(year, month, 8);
                    int dateDow = localizedDayOfWeek(date, sow);
                    int wom = range.checkValidIntValue(value, this);
                    long weeks = wom - localizedWeekOfMonth(date, dateDow);
                    days = weeks * 7 + (dow - dateDow);
                }
                date = date.plus(days, DAYS);
                if (resolverStyle == ResolverStyle.STRICT) {
                    if (date.getLong(MONTH_OF_YEAR) != fieldValues.get(MONTH_OF_YEAR)) {
                        throw new DateTimeException("Strict mode rejected date parsed to a different month");
                    }
                }
                fieldValues.remove(this);
                fieldValues.remove(YEAR);
                fieldValues.remove(MONTH_OF_YEAR);
                fieldValues.remove(DAY_OF_WEEK);
                return date;
            } else if (rangeUnit == YEARS) {  // week-of-year
                final long value = fieldValues.remove(this);
                ChronoLocalDate date = chrono.date(year, 1, 1);
                long days;
                if (resolverStyle == ResolverStyle.LENIENT) {
                    int dateDow = localizedDayOfWeek(date, sow);
                    long weeks = value - localizedWeekOfYear(date, dateDow);
                    days = weeks * 7 + (dow - dateDow);
                } else {
                    int dateDow = localizedDayOfWeek(date, sow);
                    int woy = range.checkValidIntValue(value, this);
                    long weeks = woy - localizedWeekOfYear(date, dateDow);
                    days = weeks * 7 + (dow - dateDow);
                }
                date = date.plus(days, DAYS);
                if (resolverStyle == ResolverStyle.STRICT) {
                    if (date.getLong(YEAR) != fieldValues.get(YEAR)) {
                        throw new DateTimeException("Strict mode rejected date parsed to a different year");
                    }
                }
                fieldValues.remove(this);
                fieldValues.remove(YEAR);
                fieldValues.remove(DAY_OF_WEEK);
                return date;
            } else {
                throw new IllegalStateException("unreachable");
            }
        }

        //-----------------------------------------------------------------------
        @Override
        public TemporalUnit getBaseUnit() {
            return baseUnit;
        }

        @Override
        public TemporalUnit getRangeUnit() {
            return rangeUnit;
        }

        @Override
        public ValueRange range() {
            return range;
        }

        //-----------------------------------------------------------------------
        @Override
        public boolean isDateBased() {
            return true;
        }

        @Override
        public boolean isTimeBased() {
            return false;
        }

        @Override
        public boolean isSupportedBy(TemporalAccessor temporal) {
            if (temporal.isSupported(ChronoField.DAY_OF_WEEK)) {
                if (rangeUnit == ChronoUnit.WEEKS) {
                    return true;
                } else if (rangeUnit == ChronoUnit.MONTHS) {
                    return temporal.isSupported(ChronoField.DAY_OF_MONTH);
                } else if (rangeUnit == ChronoUnit.YEARS) {
                    return temporal.isSupported(ChronoField.DAY_OF_YEAR);
                } else if (rangeUnit == IsoFields.WEEK_BASED_YEARS) {
                    return temporal.isSupported(ChronoField.EPOCH_DAY);
                } else if (rangeUnit == ChronoUnit.FOREVER) {
                    return temporal.isSupported(ChronoField.EPOCH_DAY);
                }
            }
            return false;
        }

        @Override
        public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
            if (rangeUnit == ChronoUnit.WEEKS) {
                return range;
            }

            TemporalField field = null;
            if (rangeUnit == ChronoUnit.MONTHS) {
                field = ChronoField.DAY_OF_MONTH;
            } else if (rangeUnit == ChronoUnit.YEARS) {
                field = ChronoField.DAY_OF_YEAR;
            } else if (rangeUnit == IsoFields.WEEK_BASED_YEARS) {
                return rangeWOWBY(temporal);
            } else if (rangeUnit == ChronoUnit.FOREVER) {
                return temporal.range(YEAR);
            } else {
                throw new IllegalStateException("unreachable");
            }

            // Offset the ISO DOW by the start of this week
            int sow = weekDef.getFirstDayOfWeek().getValue();
            int isoDow = temporal.get(ChronoField.DAY_OF_WEEK);
            int dow = Jdk8Methods.floorMod(isoDow - sow, 7) + 1;

            int offset = startOfWeekOffset(temporal.get(field), dow);
            ValueRange fieldRange = temporal.range(field);
            return ValueRange.of(computeWeek(offset, (int) fieldRange.getMinimum()),
                    computeWeek(offset, (int) fieldRange.getMaximum()));
        }

        private ValueRange rangeWOWBY(TemporalAccessor temporal) {
            int sow = weekDef.getFirstDayOfWeek().getValue();
            int isoDow = temporal.get(DAY_OF_WEEK);
            int dow = Jdk8Methods.floorMod(isoDow - sow, 7) + 1;
            long woy = localizedWeekOfYear(temporal, dow);
            if (woy == 0) {
                return rangeWOWBY(Chronology.from(temporal).date(temporal).minus(2, ChronoUnit.WEEKS));
            }
            int offset = startOfWeekOffset(temporal.get(DAY_OF_YEAR), dow);
            int year = temporal.get(YEAR);
            int yearLen = Year.isLeap(year) ? 366 : 365;
            int weekIndexOfFirstWeekNextYear = computeWeek(offset, yearLen + weekDef.getMinimalDaysInFirstWeek());
            if (woy >= weekIndexOfFirstWeekNextYear) {
                return rangeWOWBY(Chronology.from(temporal).date(temporal).plus(2, ChronoUnit.WEEKS));
            }
            return ValueRange.of(1, weekIndexOfFirstWeekNextYear - 1);
        }

        @Override
        public String getDisplayName(Locale locale) {
            Objects.requireNonNull(locale, "locale");
            if (rangeUnit == YEARS) {  // week-of-year
                return "Week";
            }
            return toString();
        }

        //-----------------------------------------------------------------------
        @Override
        public String toString() {
            return name + "[" + weekDef.toString() + "]";
        }
    }

}
