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
package org.teavm.classlib.java.time.temporal;

import static org.teavm.classlib.java.time.TDayOfWeek.THURSDAY;
import static org.teavm.classlib.java.time.TDayOfWeek.WEDNESDAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoField.YEAR;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.DAYS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.FOREVER;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.MONTHS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.WEEKS;
import static org.teavm.classlib.java.time.temporal.TChronoUnit.YEARS;

import java.util.Map;

import org.teavm.classlib.java.time.TDuration;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.format.TResolverStyle;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;
import org.teavm.classlib.java.util.TLocale;

public final class TIsoFields {

    public static final TTemporalField DAY_OF_QUARTER = Field.DAY_OF_QUARTER;

    public static final TTemporalField QUARTER_OF_YEAR = Field.QUARTER_OF_YEAR;

    public static final TTemporalField WEEK_OF_WEEK_BASED_YEAR = Field.WEEK_OF_WEEK_BASED_YEAR;

    public static final TTemporalField WEEK_BASED_YEAR = Field.WEEK_BASED_YEAR;

    public static final TTemporalUnit WEEK_BASED_YEARS = Unit.WEEK_BASED_YEARS;

    public static final TTemporalUnit QUARTER_YEARS = Unit.QUARTER_YEARS;

    private TIsoFields() {

        throw new AssertionError("Not instantiable");
    }

    private static enum Field implements TTemporalField {
        DAY_OF_QUARTER {
            @Override
            public String toString() {

                return "DayOfQuarter";
            }

            @Override
            public TTemporalUnit getBaseUnit() {

                return DAYS;
            }

            @Override
            public TTemporalUnit getRangeUnit() {

                return QUARTER_YEARS;
            }

            @Override
            public TValueRange range() {

                return TValueRange.of(1, 90, 92);
            }

            @Override
            public boolean isSupportedBy(TTemporalAccessor temporal) {

                return temporal.isSupported(DAY_OF_YEAR) && temporal.isSupported(MONTH_OF_YEAR)
                        && temporal.isSupported(YEAR) && isIso(temporal);
            }

            @Override
            public TValueRange rangeRefinedBy(TTemporalAccessor temporal) {

                if (temporal.isSupported(this) == false) {
                    throw new TUnsupportedTemporalTypeException("Unsupported field: DayOfQuarter");
                }
                long qoy = temporal.getLong(QUARTER_OF_YEAR);
                if (qoy == 1) {
                    long year = temporal.getLong(YEAR);
                    return (TIsoChronology.INSTANCE.isLeapYear(year) ? TValueRange.of(1, 91) : TValueRange.of(1, 90));
                } else if (qoy == 2) {
                    return TValueRange.of(1, 91);
                } else if (qoy == 3 || qoy == 4) {
                    return TValueRange.of(1, 92);
                } // else value not from 1 to 4, so drop through
                return range();
            }

            @Override
            public long getFrom(TTemporalAccessor temporal) {

                if (temporal.isSupported(this) == false) {
                    throw new TUnsupportedTemporalTypeException("Unsupported field: DayOfQuarter");
                }
                int doy = temporal.get(DAY_OF_YEAR);
                int moy = temporal.get(MONTH_OF_YEAR);
                long year = temporal.getLong(YEAR);
                return doy - QUARTER_DAYS[((moy - 1) / 3) + (TIsoChronology.INSTANCE.isLeapYear(year) ? 4 : 0)];
            }

            @SuppressWarnings("unchecked")
            @Override
            public <R extends TTemporal> R adjustInto(R temporal, long newValue) {

                long curValue = getFrom(temporal);
                range().checkValidValue(newValue, this);
                return (R) temporal.with(DAY_OF_YEAR, temporal.getLong(DAY_OF_YEAR) + (newValue - curValue));
            }

            @Override
            public TTemporalAccessor resolve(Map<TTemporalField, Long> fieldValues, TTemporalAccessor partialTemporal,
                    TResolverStyle resolverStyle) {

                Long yearLong = fieldValues.get(YEAR);
                Long qoyLong = fieldValues.get(QUARTER_OF_YEAR);
                if (yearLong == null || qoyLong == null) {
                    return null;
                }
                int y = YEAR.checkValidIntValue(yearLong);
                long doq = fieldValues.get(DAY_OF_QUARTER);
                TLocalDate date;
                if (resolverStyle == TResolverStyle.LENIENT) {
                    long qoy = qoyLong;
                    date = TLocalDate.of(y, 1, 1);
                    date = date.plusMonths(TJdk8Methods.safeMultiply(TJdk8Methods.safeSubtract(qoy, 1), 3));
                    date = date.plusDays(TJdk8Methods.safeSubtract(doq, 1));
                } else {
                    int qoy = QUARTER_OF_YEAR.range().checkValidIntValue(qoyLong, QUARTER_OF_YEAR);
                    if (resolverStyle == TResolverStyle.STRICT) {
                        int max = 92;
                        if (qoy == 1) {
                            max = (TIsoChronology.INSTANCE.isLeapYear(y) ? 91 : 90);
                        } else if (qoy == 2) {
                            max = 91;
                        }
                        TValueRange.of(1, max).checkValidValue(doq, this);
                    } else {
                        range().checkValidValue(doq, this); // leniently check from 1 to 92
                    }
                    date = TLocalDate.of(y, ((qoy - 1) * 3) + 1, 1).plusDays(doq - 1);
                }
                fieldValues.remove(this);
                fieldValues.remove(YEAR);
                fieldValues.remove(QUARTER_OF_YEAR);
                return date;
            }
        },
        QUARTER_OF_YEAR {
            @Override
            public String toString() {

                return "QuarterOfYear";
            }

            @Override
            public TTemporalUnit getBaseUnit() {

                return QUARTER_YEARS;
            }

            @Override
            public TTemporalUnit getRangeUnit() {

                return YEARS;
            }

            @Override
            public TValueRange range() {

                return TValueRange.of(1, 4);
            }

            @Override
            public boolean isSupportedBy(TTemporalAccessor temporal) {

                return temporal.isSupported(MONTH_OF_YEAR) && isIso(temporal);
            }

            @Override
            public TValueRange rangeRefinedBy(TTemporalAccessor temporal) {

                return range();
            }

            @Override
            public long getFrom(TTemporalAccessor temporal) {

                if (temporal.isSupported(this) == false) {
                    throw new TUnsupportedTemporalTypeException("Unsupported field: QuarterOfYear");
                }
                long moy = temporal.getLong(MONTH_OF_YEAR);
                return ((moy + 2) / 3);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <R extends TTemporal> R adjustInto(R temporal, long newValue) {

                long curValue = getFrom(temporal);
                range().checkValidValue(newValue, this);
                return (R) temporal.with(MONTH_OF_YEAR, temporal.getLong(MONTH_OF_YEAR) + (newValue - curValue) * 3);
            }
        },
        WEEK_OF_WEEK_BASED_YEAR {
            @Override
            public String toString() {

                return "WeekOfWeekBasedYear";
            }

            @Override
            public TTemporalUnit getBaseUnit() {

                return WEEKS;
            }

            @Override
            public TTemporalUnit getRangeUnit() {

                return WEEK_BASED_YEARS;
            }

            @Override
            public String getDisplayName(TLocale locale) {

                TJdk8Methods.requireNonNull(locale, "locale");
                return "Week";
            }

            @Override
            public TValueRange range() {

                return TValueRange.of(1, 52, 53);
            }

            @Override
            public boolean isSupportedBy(TTemporalAccessor temporal) {

                return temporal.isSupported(EPOCH_DAY) && isIso(temporal);
            }

            @Override
            public TValueRange rangeRefinedBy(TTemporalAccessor temporal) {

                if (temporal.isSupported(this) == false) {
                    throw new TUnsupportedTemporalTypeException("Unsupported field: WeekOfWeekBasedYear");
                }
                return getWeekRange(TLocalDate.from(temporal));
            }

            @Override
            public long getFrom(TTemporalAccessor temporal) {

                if (temporal.isSupported(this) == false) {
                    throw new TUnsupportedTemporalTypeException("Unsupported field: WeekOfWeekBasedYear");
                }
                return getWeek(TLocalDate.from(temporal));
            }

            @SuppressWarnings("unchecked")
            @Override
            public <R extends TTemporal> R adjustInto(R temporal, long newValue) {

                range().checkValidValue(newValue, this);
                return (R) temporal.plus(TJdk8Methods.safeSubtract(newValue, getFrom(temporal)), WEEKS);
            }

            @Override
            public TTemporalAccessor resolve(Map<TTemporalField, Long> fieldValues, TTemporalAccessor partialTemporal,
                    TResolverStyle resolverStyle) {

                Long wbyLong = fieldValues.get(WEEK_BASED_YEAR);
                Long dowLong = fieldValues.get(DAY_OF_WEEK);
                if (wbyLong == null || dowLong == null) {
                    return null;
                }
                int wby = WEEK_BASED_YEAR.range().checkValidIntValue(wbyLong, WEEK_BASED_YEAR);
                long wowby = fieldValues.get(WEEK_OF_WEEK_BASED_YEAR);
                TLocalDate date;
                if (resolverStyle == TResolverStyle.LENIENT) {
                    long dow = dowLong;
                    long weeks = 0;
                    if (dow > 7) {
                        weeks = (dow - 1) / 7;
                        dow = ((dow - 1) % 7) + 1;
                    } else if (dow < 1) {
                        weeks = (dow / 7) - 1;
                        dow = (dow % 7) + 7;
                    }
                    date = TLocalDate.of(wby, 1, 4).plusWeeks(wowby - 1).plusWeeks(weeks).with(DAY_OF_WEEK, dow);
                } else {
                    int dow = DAY_OF_WEEK.checkValidIntValue(dowLong);
                    if (resolverStyle == TResolverStyle.STRICT) {
                        TLocalDate temp = TLocalDate.of(wby, 1, 4);
                        TValueRange range = getWeekRange(temp);
                        range.checkValidValue(wowby, this);
                    } else {
                        range().checkValidValue(wowby, this); // leniently check from 1 to 53
                    }
                    date = TLocalDate.of(wby, 1, 4).plusWeeks(wowby - 1).with(DAY_OF_WEEK, dow);
                }
                fieldValues.remove(this);
                fieldValues.remove(WEEK_BASED_YEAR);
                fieldValues.remove(DAY_OF_WEEK);
                return date;
            }
        },
        WEEK_BASED_YEAR {
            @Override
            public String toString() {

                return "WeekBasedYear";
            }

            @Override
            public TTemporalUnit getBaseUnit() {

                return WEEK_BASED_YEARS;
            }

            @Override
            public TTemporalUnit getRangeUnit() {

                return FOREVER;
            }

            @Override
            public TValueRange range() {

                return YEAR.range();
            }

            @Override
            public boolean isSupportedBy(TTemporalAccessor temporal) {

                return temporal.isSupported(EPOCH_DAY) && isIso(temporal);
            }

            @Override
            public TValueRange rangeRefinedBy(TTemporalAccessor temporal) {

                return YEAR.range();
            }

            @Override
            public long getFrom(TTemporalAccessor temporal) {

                if (temporal.isSupported(this) == false) {
                    throw new TUnsupportedTemporalTypeException("Unsupported field: WeekBasedYear");
                }
                return getWeekBasedYear(TLocalDate.from(temporal));
            }

            @SuppressWarnings("unchecked")
            @Override
            public <R extends TTemporal> R adjustInto(R temporal, long newValue) {

                if (isSupportedBy(temporal) == false) {
                    throw new TUnsupportedTemporalTypeException("Unsupported field: WeekBasedYear");
                }
                int newWby = range().checkValidIntValue(newValue, WEEK_BASED_YEAR); // strict check
                TLocalDate date = TLocalDate.from(temporal);
                int dow = date.get(DAY_OF_WEEK);
                int week = getWeek(date);
                if (week == 53 && getWeekRange(newWby) == 52) {
                    week = 52;
                }
                TLocalDate resolved = TLocalDate.of(newWby, 1, 4); // 4th is guaranteed to be in week one
                int days = (dow - resolved.get(DAY_OF_WEEK)) + ((week - 1) * 7);
                resolved = resolved.plusDays(days);
                return (R) temporal.with(resolved);
            }
        };

        @Override
        public String getDisplayName(TLocale locale) {

            TJdk8Methods.requireNonNull(locale, "locale");
            return toString();
        }

        @Override
        public TTemporalAccessor resolve(Map<TTemporalField, Long> fieldValues, TTemporalAccessor partialTemporal,
                TResolverStyle resolverStyle) {

            return null;
        }

        private static final int[] QUARTER_DAYS = { 0, 90, 181, 273, 0, 91, 182, 274 };

        @Override
        public boolean isDateBased() {

            return true;
        }

        @Override
        public boolean isTimeBased() {

            return false;
        }

        private static boolean isIso(TTemporalAccessor temporal) {

            return TChronology.from(temporal).equals(TIsoChronology.INSTANCE);
        }

        private static TValueRange getWeekRange(TLocalDate date) {

            int wby = getWeekBasedYear(date);
            return TValueRange.of(1, getWeekRange(wby));
        }

        private static int getWeekRange(int wby) {

            TLocalDate date = TLocalDate.of(wby, 1, 1);
            // 53 weeks if standard year starts on Thursday, or Wed in a leap year
            if (date.getDayOfWeek() == THURSDAY || (date.getDayOfWeek() == WEDNESDAY && date.isLeapYear())) {
                return 53;
            }
            return 52;
        }

        private static int getWeek(TLocalDate date) {

            int dow0 = date.getDayOfWeek().ordinal();
            int doy0 = date.getDayOfYear() - 1;
            int doyThu0 = doy0 + (3 - dow0); // adjust to mid-week Thursday (which is 3 indexed from zero)
            int alignedWeek = doyThu0 / 7;
            int firstThuDoy0 = doyThu0 - (alignedWeek * 7);
            int firstMonDoy0 = firstThuDoy0 - 3;
            if (firstMonDoy0 < -3) {
                firstMonDoy0 += 7;
            }
            if (doy0 < firstMonDoy0) {
                return (int) getWeekRange(date.withDayOfYear(180).minusYears(1)).getMaximum();
            }
            int week = ((doy0 - firstMonDoy0) / 7) + 1;
            if (week == 53) {
                if ((firstMonDoy0 == -3 || (firstMonDoy0 == -2 && date.isLeapYear())) == false) {
                    week = 1;
                }
            }
            return week;
        }

        private static int getWeekBasedYear(TLocalDate date) {

            int year = date.getYear();
            int doy = date.getDayOfYear();
            if (doy <= 3) {
                int dow = date.getDayOfWeek().ordinal();
                if (doy - dow < -2) {
                    year--;
                }
            } else if (doy >= 363) {
                int dow = date.getDayOfWeek().ordinal();
                doy = doy - 363 - (date.isLeapYear() ? 1 : 0);
                if (doy - dow >= 0) {
                    year++;
                }
            }
            return year;
        }
    }

    private static enum Unit implements TTemporalUnit {
        WEEK_BASED_YEARS("WeekBasedYears", TDuration.ofSeconds(31556952L)), //
        QUARTER_YEARS("QuarterYears", TDuration.ofSeconds(31556952L / 4));

        private final String name;

        private final TDuration duration;

        private Unit(String name, TDuration estimatedDuration) {

            this.name = name;
            this.duration = estimatedDuration;
        }

        @Override
        public TDuration getDuration() {

            return this.duration;
        }

        @Override
        public boolean isDurationEstimated() {

            return true;
        }

        @Override
        public boolean isDateBased() {

            return true;
        }

        @Override
        public boolean isTimeBased() {

            return false;
        }

        @Override
        public boolean isSupportedBy(TTemporal temporal) {

            return temporal.isSupported(EPOCH_DAY);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R extends TTemporal> R addTo(R temporal, long periodToAdd) {

            switch (this) {
                case WEEK_BASED_YEARS:
                    long added = TJdk8Methods.safeAdd(temporal.get(WEEK_BASED_YEAR), periodToAdd);
                    return (R) temporal.with(WEEK_BASED_YEAR, added);
                case QUARTER_YEARS:
                    // no overflow (256 is multiple of 4)
                    return (R) temporal.plus(periodToAdd / 256, YEARS).plus((periodToAdd % 256) * 3, MONTHS);
                default:
                    throw new IllegalStateException("Unreachable");
            }
        }

        @Override
        public long between(TTemporal temporal1, TTemporal temporal2) {

            switch (this) {
                case WEEK_BASED_YEARS:
                    return TJdk8Methods.safeSubtract(temporal2.getLong(WEEK_BASED_YEAR),
                            temporal1.getLong(WEEK_BASED_YEAR));
                case QUARTER_YEARS:
                    return temporal1.until(temporal2, MONTHS) / 3;
                default:
                    throw new IllegalStateException("Unreachable");
            }
        }

        @Override
        public String toString() {

            return this.name;
        }
    }
}
