/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.teavm.classlib.java.text;

import org.teavm.classlib.java.util.*;

public abstract class DateFormat extends Format {
    protected Calendar calendar;
    protected NumberFormat numberFormat;
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

    protected DateFormat() {
    }

    @Override
    public Object clone() {
        DateFormat clone = (DateFormat) super.clone();
        clone.calendar = (Calendar) calendar.clone();
        clone.numberFormat = (NumberFormat) numberFormat.clone();
        return clone;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DateFormat)) {
            return false;
        }
        DateFormat dateFormat = (DateFormat) object;
        return numberFormat.equals(dateFormat.numberFormat) &&
                calendar.getTimeZone().equals(dateFormat.calendar.getTimeZone()) &&
                calendar.getFirstDayOfWeek() == dateFormat.calendar.getFirstDayOfWeek() &&
                calendar.getMinimalDaysInFirstWeek() == dateFormat.calendar.getMinimalDaysInFirstWeek() &&
                calendar.isLenient() == dateFormat.calendar.isLenient();
    }

    @Override
    public final StringBuffer format(Object object, StringBuffer buffer, FieldPosition field) {
        if (object instanceof Date) {
            return format((Date) object, buffer, field);
        }
        if (object instanceof Number) {
            return format(new Date(((Number) object).longValue()), buffer, field);
        }
        throw new IllegalArgumentException();
    }

    public final String format(Date date) {
        return format(date, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public abstract StringBuffer format(Date date, StringBuffer buffer, FieldPosition field);

    public static TLocale[] getAvailableLocales() {
        return TLocale.getAvailableLocales();
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public final static DateFormat getDateInstance() {
        return getDateInstance(DEFAULT);
    }

    public final static DateFormat getDateInstance(int style) {
        checkDateStyle(style);
        return getDateInstance(style, TLocale.getDefault());
    }

    public final static DateFormat getDateInstance(int style, TLocale locale) {
        checkDateStyle(style);
        com.ibm.icu.text.DateFormat icuFormat = com.ibm.icu.text.DateFormat.getDateInstance(style, locale);
        return new SimpleDateFormat(locale, (com.ibm.icu.text.SimpleDateFormat) icuFormat);
    }

    public final static DateFormat getDateTimeInstance() {
        return getDateTimeInstance(DEFAULT, DEFAULT);
    }

    public final static DateFormat getDateTimeInstance(int dateStyle, int timeStyle) {
        checkTimeStyle(timeStyle);
        checkDateStyle(dateStyle);
        return getDateTimeInstance(dateStyle, timeStyle, TLocale.getDefault());
    }

    public final static DateFormat getDateTimeInstance(int dateStyle, int timeStyle, TLocale locale) {
        checkTimeStyle(timeStyle);
        checkDateStyle(dateStyle);
        com.ibm.icu.text.DateFormat icuFormat = com.ibm.icu.text.DateFormat.getDateTimeInstance(dateStyle, timeStyle,
                locale);
        return new SimpleDateFormat(locale, (com.ibm.icu.text.SimpleDateFormat) icuFormat);
    }

    public final static DateFormat getInstance() {
        return getDateTimeInstance(SHORT, SHORT);
    }

    public NumberFormat getNumberFormat() {
        return numberFormat;
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

    public final static DateFormat getTimeInstance() {
        return getTimeInstance(DEFAULT);
    }

    public final static DateFormat getTimeInstance(int style) {
        checkTimeStyle(style);
        return getTimeInstance(style, TLocale.getDefault());
    }

    public final static DateFormat getTimeInstance(int style, TLocale locale) {
        checkTimeStyle(style);
        com.ibm.icu.text.DateFormat icuFormat = com.ibm.icu.text.DateFormat.getTimeInstance(style, locale);
        return new SimpleDateFormat(locale, (com.ibm.icu.text.SimpleDateFormat) icuFormat);
    }

    public TimeZone getTimeZone() {
        return calendar.getTimeZone();
    }

    @Override
    public int hashCode() {
        return calendar.getFirstDayOfWeek() + calendar.getMinimalDaysInFirstWeek() + calendar.getTimeZone().hashCode() +
                (calendar.isLenient() ? 1231 : 1237) + numberFormat.hashCode();
    }

    public boolean isLenient() {
        return calendar.isLenient();
    }

    public Date parse(String string) throws ParseException {
        ParsePosition position = new ParsePosition(0);
        Date date = parse(string, position);
        if (position.getIndex() == 0) {
            throw new ParseException("Unparseable date" + string, position.getErrorIndex());
        }
        return date;
    }

    public abstract Date parse(String string, ParsePosition position);

    @Override
    public Object parseObject(String string, ParsePosition position) {
        return parse(string, position);
    }

    public void setCalendar(Calendar cal) {
        calendar = cal;
    }

    public void setLenient(boolean value) {
        calendar.setLenient(value);
    }

    public void setNumberFormat(NumberFormat format) {
        numberFormat = format;
    }

    public void setTimeZone(TimeZone timezone) {
        calendar.setTimeZone(timezone);
    }

    public static class Field extends Format.Field {
        private static THashMap<Integer, Field> table = new THashMap<>();
        public final static Field ERA = new Field("era", Calendar.ERA);
        public final static Field YEAR = new Field("year", Calendar.YEAR);
        public final static Field MONTH = new Field("month", Calendar.MONTH);
        public final static Field HOUR_OF_DAY0 = new Field("hour of day", Calendar.HOUR_OF_DAY);
        public final static Field HOUR_OF_DAY1 = new Field("hour of day 1", -1);
        public final static Field MINUTE = new Field("minute", Calendar.MINUTE);
        public final static Field SECOND = new Field("second", Calendar.SECOND);
        public final static Field MILLISECOND = new Field("millisecond", Calendar.MILLISECOND);
        public final static Field DAY_OF_WEEK = new Field("day of week", Calendar.DAY_OF_WEEK);
        public final static Field DAY_OF_MONTH = new Field("day of month", Calendar.DAY_OF_MONTH);
        public final static Field DAY_OF_YEAR = new Field("day of year", Calendar.DAY_OF_YEAR);
        public final static Field DAY_OF_WEEK_IN_MONTH = new Field("day of week in month",
                Calendar.DAY_OF_WEEK_IN_MONTH);
        public final static Field WEEK_OF_YEAR = new Field("week of year", Calendar.WEEK_OF_YEAR);
        public final static Field WEEK_OF_MONTH = new Field("week of month", Calendar.WEEK_OF_MONTH);
        public final static Field AM_PM = new Field("am pm", Calendar.AM_PM);
        public final static Field HOUR0 = new Field("hour", Calendar.HOUR);
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
            if (calendarField < 0 || calendarField >= Calendar.FIELD_COUNT) {
                throw new IllegalArgumentException();
            }

            return table.get(new Integer(calendarField));
        }
    }

    private static void checkDateStyle(int style) {
        if (!(style == SHORT || style == MEDIUM || style == LONG || style == FULL || style == DEFAULT)) {
            throw new IllegalArgumentException("Illegal date style: " + style);
        }
    }

    private static void checkTimeStyle(int style) {
        if (!(style == SHORT || style == MEDIUM || style == LONG || style == FULL || style == DEFAULT)) {
            // text.0F=Illegal time style: {0}
            throw new IllegalArgumentException("Illegal time style: " + style);
        }
    }
}
