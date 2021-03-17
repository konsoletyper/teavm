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

import java.io.Serializable;
import org.threeten.bp.DateTimeException;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.jdk8.Jdk8Methods;
import org.threeten.bp.temporal.ChronoUnit;
import org.threeten.bp.temporal.Temporal;
import org.threeten.bp.temporal.TemporalAdjuster;
import org.threeten.bp.temporal.TemporalUnit;

/**
 * A date expressed in terms of a standard year-month-day calendar system.
 * <p>
 * This class is used by applications seeking to handle dates in non-ISO calendar systems.
 * For example, the Japanese, Minguo, Thai Buddhist and others.
 * <p>
 * {@code ChronoLocalDate} is built on the generic concepts of year, month and day.
 * The calendar system, represented by a {@link Chronology}, expresses the relationship between
 * the fields and this class allows the resulting date to be manipulated.
 * <p>
 * Note that not all calendar systems are suitable for use with this class.
 * For example, the Mayan calendar uses a system that bears no relation to years, months and days.
 * <p>
 * The API design encourages the use of {@code LocalDate} for the majority of the application.
 * This includes code to read and write from a persistent data store, such as a database,
 * and to send dates and times across a network. The {@code ChronoLocalDate} instance is then used
 * at the user interface level to deal with localized input/output.
 *
 * <P>Example: </p>
 * <pre>
 *        System.out.printf("Example()%n");
 *        // Enumerate the list of available calendars and print today for each
 *        Set&lt;Chrono&gt; chronos = Chrono.getAvailableChronologies();
 *        for (Chrono chrono : chronos) {
 *            ChronoLocalDate date = chrono.dateNow();
 *            System.out.printf("   %20s: %s%n", chrono.getID(), date.toString());
 *        }
 *
 *        // Print the Hijrah date and calendar
 *        ChronoLocalDate date = Chrono.of("Hijrah").dateNow();
 *        int day = date.get(ChronoField.DAY_OF_MONTH);
 *        int dow = date.get(ChronoField.DAY_OF_WEEK);
 *        int month = date.get(ChronoField.MONTH_OF_YEAR);
 *        int year = date.get(ChronoField.YEAR);
 *        System.out.printf("  Today is %s %s %d-%s-%d%n", date.getChrono().getID(),
 *                dow, day, month, year);

 *        // Print today's date and the last day of the year
 *        ChronoLocalDate now1 = Chrono.of("Hijrah").dateNow();
 *        ChronoLocalDate first = now1.with(ChronoField.DAY_OF_MONTH, 1)
 *                .with(ChronoField.MONTH_OF_YEAR, 1);
 *        ChronoLocalDate last = first.plus(1, ChronoUnit.YEARS)
 *                .minus(1, ChronoUnit.DAYS);
 *        System.out.printf("  Today is %s: start: %s; end: %s%n", last.getChrono().getID(),
 *                first, last);
 * </pre>
 *
 * <h4>Adding Calendars</h4>
 * <p> The set of calendars is extensible by defining a subclass of {@link ChronoLocalDate}
 * to represent a date instance and an implementation of {@code Chronology}
 * to be the factory for the ChronoLocalDate subclass.
 * </p>
 * <p> To permit the discovery of the additional calendar types the implementation of
 * {@code Chronology} must be registered as a Service implementing the {@code Chronology} interface
 * in the {@code META-INF/Services} file as per the specification of {@link java.util.ServiceLoader}.
 * The subclass must function according to the {@code Chronology} class description and must provide its
 * {@link Chronology#getId()}  calendar name} and
 * {@link Chronology#getCalendarType() calendar type}. </p>
 *
 * <h3>Specification for implementors</h3>
 * This abstract class must be implemented with care to ensure other classes operate correctly.
 * All implementations that can be instantiated must be final, immutable and thread-safe.
 * Subclasses should be Serializable wherever possible.
 *
 * @param <D> the date type
 */
abstract class ChronoDateImpl<D extends ChronoLocalDate>
        extends ChronoLocalDate
        implements Temporal, TemporalAdjuster, Serializable {

    /**
     * Creates an instance.
     */
    ChronoDateImpl() {
    }

    //-----------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    @Override
    public ChronoDateImpl<D> plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            ChronoUnit f = (ChronoUnit) unit;
            switch (f) {
                case DAYS: return plusDays(amountToAdd);
                case WEEKS: return plusDays(Jdk8Methods.safeMultiply(amountToAdd, 7));
                case MONTHS: return plusMonths(amountToAdd);
                case YEARS: return plusYears(amountToAdd);
                case DECADES: return plusYears(Jdk8Methods.safeMultiply(amountToAdd, 10));
                case CENTURIES: return plusYears(Jdk8Methods.safeMultiply(amountToAdd, 100));
                case MILLENNIA: return plusYears(Jdk8Methods.safeMultiply(amountToAdd, 1000));
//                case ERAS: throw new DateTimeException("Unable to add era,
//                standard calendar system only has one era");
//                case FOREVER: return (period == 0 ? this : (period > 0 ? LocalDate.MAX_DATE : LocalDate.MIN_DATE));
            }
            throw new DateTimeException(unit + " not valid for chronology " + getChronology().getId());
        }
        return (ChronoDateImpl<D>) getChronology().ensureChronoLocalDate(unit.addTo(this, amountToAdd));
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this date with the specified period in years added.
     * <p>
     * This adds the specified period in years to the date.
     * In some cases, adding years can cause the resulting date to become invalid.
     * If this occurs, then other fields, typically the day-of-month, will be adjusted to ensure
     * that the result is valid. Typically this will select the last valid day of the month.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param yearsToAdd  the years to add, may be negative
     * @return a date based on this one with the years added, not null
     * @throws DateTimeException if the result exceeds the supported date range
     */
    abstract ChronoDateImpl<D> plusYears(long yearsToAdd);

    /**
     * Returns a copy of this date with the specified period in months added.
     * <p>
     * This adds the specified period in months to the date.
     * In some cases, adding months can cause the resulting date to become invalid.
     * If this occurs, then other fields, typically the day-of-month, will be adjusted to ensure
     * that the result is valid. Typically this will select the last valid day of the month.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param monthsToAdd  the months to add, may be negative
     * @return a date based on this one with the months added, not null
     * @throws DateTimeException if the result exceeds the supported date range
     */
    abstract ChronoDateImpl<D> plusMonths(long monthsToAdd);

    /**
     * Returns a copy of this date with the specified period in weeks added.
     * <p>
     * This adds the specified period in weeks to the date.
     * In some cases, adding weeks can cause the resulting date to become invalid.
     * If this occurs, then other fields will be adjusted to ensure that the result is valid.
     * <p>
     * The default implementation uses {@link #plusDays(long)} using a 7 day week.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param weeksToAdd  the weeks to add, may be negative
     * @return a date based on this one with the weeks added, not null
     * @throws DateTimeException if the result exceeds the supported date range
     */
    ChronoDateImpl<D> plusWeeks(long weeksToAdd) {
        return plusDays(Jdk8Methods.safeMultiply(weeksToAdd, 7));
    }

    /**
     * Returns a copy of this date with the specified number of days added.
     * <p>
     * This adds the specified period in days to the date.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param daysToAdd  the days to add, may be negative
     * @return a date based on this one with the days added, not null
     * @throws DateTimeException if the result exceeds the supported date range
     */
    abstract ChronoDateImpl<D> plusDays(long daysToAdd);

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this date with the specified period in years subtracted.
     * <p>
     * This subtracts the specified period in years to the date.
     * In some cases, subtracting years can cause the resulting date to become invalid.
     * If this occurs, then other fields, typically the day-of-month, will be adjusted to ensure
     * that the result is valid. Typically this will select the last valid day of the month.
     * <p>
     * The default implementation uses {@link #plusYears(long)}.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param yearsToSubtract  the years to subtract, may be negative
     * @return a date based on this one with the years subtracted, not null
     * @throws DateTimeException if the result exceeds the supported date range
     */
    ChronoDateImpl<D> minusYears(long yearsToSubtract) {
        return yearsToSubtract == Long.MIN_VALUE
                ? plusYears(Long.MAX_VALUE).plusYears(1)
                : plusYears(-yearsToSubtract);
    }

    /**
     * Returns a copy of this date with the specified period in months subtracted.
     * <p>
     * This subtracts the specified period in months to the date.
     * In some cases, subtracting months can cause the resulting date to become invalid.
     * If this occurs, then other fields, typically the day-of-month, will be adjusted to ensure
     * that the result is valid. Typically this will select the last valid day of the month.
     * <p>
     * The default implementation uses {@link #plusMonths(long)}.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param monthsToSubtract  the months to subtract, may be negative
     * @return a date based on this one with the months subtracted, not null
     * @throws DateTimeException if the result exceeds the supported date range
     */
    ChronoDateImpl<D> minusMonths(long monthsToSubtract) {
        return monthsToSubtract == Long.MIN_VALUE
                ? plusMonths(Long.MAX_VALUE).plusMonths(1)
                : plusMonths(-monthsToSubtract);
    }

    /**
     * Returns a copy of this date with the specified period in weeks subtracted.
     * <p>
     * This subtracts the specified period in weeks to the date.
     * In some cases, subtracting weeks can cause the resulting date to become invalid.
     * If this occurs, then other fields will be adjusted to ensure that the result is valid.
     * <p>
     * The default implementation uses {@link #plusWeeks(long)}.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param weeksToSubtract  the weeks to subtract, may be negative
     * @return a date based on this one with the weeks subtracted, not null
     * @throws DateTimeException if the result exceeds the supported date range
     */
    ChronoDateImpl<D> minusWeeks(long weeksToSubtract) {
        return weeksToSubtract == Long.MIN_VALUE
                ? plusWeeks(Long.MAX_VALUE).plusWeeks(1)
                : plusWeeks(-weeksToSubtract);
    }

    /**
     * Returns a copy of this date with the specified number of days subtracted.
     * <p>
     * This subtracts the specified period in days to the date.
     * <p>
     * The default implementation uses {@link #plusDays(long)}.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param daysToSubtract  the days to subtract, may be negative
     * @return a date based on this one with the days subtracted, not null
     * @throws DateTimeException if the result exceeds the supported date range
     */
    ChronoDateImpl<D> minusDays(long daysToSubtract) {
        return daysToSubtract == Long.MIN_VALUE
                ? plusDays(Long.MAX_VALUE).plusDays(1)
                : plusDays(-daysToSubtract);
    }

    @Override
    public ChronoLocalDateTime<?> atTime(LocalTime localTime) {
        return ChronoLocalDateTimeImpl.of(this, localTime);
    }

    //-----------------------------------------------------------------------
    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        ChronoLocalDate end = getChronology().date(endExclusive);
        if (unit instanceof ChronoUnit) {
            return LocalDate.from(this).until(end, unit);  // TODO: this is wrong
        }
        return unit.between(this, end);
    }

    @Override
    public ChronoPeriod until(ChronoLocalDate endDate) {
        throw new UnsupportedOperationException("Not supported in ThreeTen backport");
    }

}
