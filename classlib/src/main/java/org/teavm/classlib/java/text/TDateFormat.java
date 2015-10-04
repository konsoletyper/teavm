/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.classlib.java.text;

import org.teavm.classlib.impl.unicode.CLDRHelper;
import org.teavm.classlib.impl.unicode.DateFormatCollection;
import org.teavm.classlib.java.util.*;

public abstract class TDateFormat extends TFormat {
    protected TCalendar calendar;
    public final static int DEFAULT = 2;
    public final static int FULL = 0;
    public final static int LONG = 1;
    public final static int MEDIUM = 2;
    public final static int SHORT = 3;
    public final static int ERA_FIELD = 0;
    public final static int YEAR_FIELD = 1;
    public final static int MONTH_FIELD = 2;
    public final static int DATE_FIELD = 3;
    public final static int HOUR_OF_DAY1_FIELD = 4;
    public final static int HOUR_OF_DAY0_FIELD = 5;
    public final static int MINUTE_FIELD = 6;
    public final static int SECOND_FIELD = 7;
    public final static int MILLISECOND_FIELD = 8;
    public final static int DAY_OF_WEEK_FIELD = 9;
    public final static int DAY_OF_YEAR_FIELD = 10;
    public final static int DAY_OF_WEEK_IN_MONTH_FIELD = 11;
    public final static int WEEK_OF_YEAR_FIELD = 12;
    public final static int WEEK_OF_MONTH_FIELD = 13;
    public final static int AM_PM_FIELD = 14;
    public final static int HOUR1_FIELD = 15;
    public final static int HOUR0_FIELD = 16;
    public final static int TIMEZONE_FIELD = 17;

    protected TDateFormat() {
        calendar = TCalendar.getInstance();
    }

    @Override
    public Object clone() {
        TDateFormat clone = (TDateFormat) super.clone();
        clone.calendar = (TCalendar) calendar.clone();
        return clone;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TDateFormat)) {
            return false;
        }
        TDateFormat dateFormat = (TDateFormat) object;
        return calendar.getFirstDayOfWeek() == dateFormat.calendar.getFirstDayOfWeek()
                && calendar.getMinimalDaysInFirstWeek() == dateFormat.calendar.getMinimalDaysInFirstWeek()
                && calendar.isLenient() == dateFormat.calendar.isLenient();
    }

    @Override
    public final StringBuffer format(Object object, StringBuffer buffer, TFieldPosition field) {
        if (object instanceof TDate) {
            return format((TDate) object, buffer, field);
        }
        if (object instanceof Number) {
            return format(new TDate(((Number) object).longValue()), buffer, field);
        }
        throw new IllegalArgumentException();
    }

    public final String format(TDate date) {
        return format(date, new StringBuffer(), new TFieldPosition(0)).toString();
    }

    public abstract StringBuffer format(TDate date, StringBuffer buffer, TFieldPosition field);

    public static TLocale[] getAvailableLocales() {
        return TLocale.getAvailableLocales();
    }

    public TCalendar getCalendar() {
        return calendar;
    }

    public TTimeZone getTimeZone() {
        return calendar.getTimeZone();
    }

    public void setTimeZone(TTimeZone timeZone) {
        calendar.setTimeZone(timeZone);
    }

    public static TDateFormat getDateInstance() {
        return getDateInstance(DEFAULT);
    }

    public static TDateFormat getDateInstance(int style) {
        return getDateInstance(style, TLocale.getDefault());
    }

    public static TDateFormat getDateInstance(int style, TLocale locale) {
        return new TSimpleDateFormat(getDateFormatString(style, locale), locale);
    }

    private static String getDateFormatString(int style, TLocale locale) {
        DateFormatCollection formats = CLDRHelper.resolveDateFormats(locale.getLanguage(), locale.getCountry());
        switch (style) {
            case SHORT:
                return formats.getShortFormat();
            case MEDIUM:
                return formats.getMediumFormat();
            case LONG:
                return formats.getLongFormat();
            case FULL:
                return formats.getFullFormat();
            default:
                throw new IllegalArgumentException("Unknown style: " + style);
        }
    }

    public static TDateFormat getDateTimeInstance() {
        return getDateTimeInstance(DEFAULT, DEFAULT);
    }

    public static TDateFormat getDateTimeInstance(int dateStyle, int timeStyle) {
        return getDateTimeInstance(dateStyle, timeStyle, TLocale.getDefault());
    }

    public static TDateFormat getDateTimeInstance(int dateStyle, int timeStyle, TLocale locale) {
        String pattern = getDateTimeFormatString(Math.max(dateStyle, timeStyle), locale);
        pattern = pattern.replace("{0}", getTimeFormatString(dateStyle, locale))
                .replace("{1}", getDateFormatString(timeStyle, locale));
        return new TSimpleDateFormat(pattern, locale);
    }

    public static String getDateTimeFormatString(int style, TLocale locale) {
        DateFormatCollection formats = CLDRHelper.resolveDateTimeFormats(locale.getLanguage(), locale.getCountry());
        switch (style) {
            case SHORT:
                return formats.getShortFormat();
            case MEDIUM:
                return formats.getMediumFormat();
            case LONG:
                return formats.getLongFormat();
            case FULL:
                return formats.getFullFormat();
            default:
                throw new IllegalArgumentException("Unknown style: " + style);
        }
    }

    public final static TDateFormat getInstance() {
        return getDateTimeInstance(SHORT, SHORT);
    }

    static String getStyleName(int style) {
        String styleName;
        switch (style) {
            case SHORT:
                styleName = "SHORT";
                break;
            case MEDIUM:
                styleName = "MEDIUM";
                break;
            case LONG:
                styleName = "LONG";
                break;
            case FULL:
                styleName = "FULL";
                break;
            default:
                styleName = "";
        }
        return styleName;
    }

    public final static TDateFormat getTimeInstance() {
        return getTimeInstance(DEFAULT);
    }

    public static TDateFormat getTimeInstance(int style) {
        return getTimeInstance(style, TLocale.getDefault());
    }

    public static TDateFormat getTimeInstance(int style, TLocale locale) {
        return new TSimpleDateFormat(getTimeFormatString(style, locale), locale);
    }

    private static String getTimeFormatString(int style, TLocale locale) {
        DateFormatCollection formats = CLDRHelper.resolveTimeFormats(locale.getLanguage(), locale.getCountry());
        switch (style) {
            case SHORT:
                return formats.getShortFormat();
            case MEDIUM:
                return formats.getMediumFormat();
            case LONG:
                return formats.getLongFormat();
            case FULL:
                return formats.getFullFormat();
            default:
                throw new IllegalArgumentException("Unknown style: " + style);
        }
    }

    @Override
    public int hashCode() {
        return calendar.getFirstDayOfWeek() + calendar.getMinimalDaysInFirstWeek()
                + (calendar.isLenient() ? 1231 : 1237);
    }

    public boolean isLenient() {
        return calendar.isLenient();
    }

    public TDate parse(String string) throws TParseException {
        TParsePosition position = new TParsePosition(0);
        TDate date = parse(string, position);
        if (position.getErrorIndex() > 0) {
            throw new TParseException("Unparseable date: " + string, position.getErrorIndex());
        }
        return date;
    }

    public abstract TDate parse(String string, TParsePosition position);

    @Override
    public Object parseObject(String string, TParsePosition position) {
        return parse(string, position);
    }

    public void setCalendar(TCalendar cal) {
        calendar = cal;
    }

    public void setLenient(boolean value) {
        calendar.setLenient(value);
    }

    public static class Field extends TFormat.Field {
        private static THashMap<Integer, Field> table = new THashMap<>();
        public final static Field ERA = new Field("era", TCalendar.ERA);
        public final static Field YEAR = new Field("year", TCalendar.YEAR);
        public final static Field MONTH = new Field("month", TCalendar.MONTH);
        public final static Field HOUR_OF_DAY0 = new Field("hour of day", TCalendar.HOUR_OF_DAY);
        public final static Field HOUR_OF_DAY1 = new Field("hour of day 1", -1);
        public final static Field MINUTE = new Field("minute", TCalendar.MINUTE);
        public final static Field SECOND = new Field("second", TCalendar.SECOND);
        public final static Field MILLISECOND = new Field("millisecond", TCalendar.MILLISECOND);
        public final static Field DAY_OF_WEEK = new Field("day of week", TCalendar.DAY_OF_WEEK);
        public final static Field DAY_OF_MONTH = new Field("day of month", TCalendar.DAY_OF_MONTH);
        public final static Field DAY_OF_YEAR = new Field("day of year", TCalendar.DAY_OF_YEAR);
        public final static Field DAY_OF_WEEK_IN_MONTH = new Field("day of week in month",
                TCalendar.DAY_OF_WEEK_IN_MONTH);
        public final static Field WEEK_OF_YEAR = new Field("week of year", TCalendar.WEEK_OF_YEAR);
        public final static Field WEEK_OF_MONTH = new Field("week of month", TCalendar.WEEK_OF_MONTH);
        public final static Field AM_PM = new Field("am pm", TCalendar.AM_PM);
        public final static Field HOUR0 = new Field("hour", TCalendar.HOUR);
        public final static Field HOUR1 = new Field("hour 1", -1);
        public final static Field TIME_ZONE = new Field("time zone", -1);
        private int calendarField = -1;

        protected Field(String fieldName, int calendarField) {
            super(fieldName);
            this.calendarField = calendarField;
            if (calendarField != -1 && table.get(new Integer(calendarField)) == null) {
                table.put(new Integer(calendarField), this);
            }
        }

        public int getCalendarField() {
            return calendarField;
        }

        public static Field ofCalendarField(int calendarField) {
            if (calendarField < 0 || calendarField >= TCalendar.FIELD_COUNT) {
                throw new IllegalArgumentException();
            }

            return table.get(new Integer(calendarField));
        }
    }
}
