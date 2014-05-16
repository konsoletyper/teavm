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

public class SimpleDateFormat extends DateFormat {

    private static final long serialVersionUID = 4774881970558875024L;

    private static final String patternChars = "GyMdkHmsSEDFwWahKzYeugAZvcLQqV"; //$NON-NLS-1$

    private String pattern;

    private DateFormatSymbols formatData;

    transient private int creationYear;

    private Date defaultCenturyStart;

    private transient String tzId;

    private transient com.ibm.icu.text.SimpleDateFormat icuFormat;

    public SimpleDateFormat() {
        this(TLocale.getDefault());
        icuFormat = new com.ibm.icu.text.SimpleDateFormat();
        icuFormat.setTimeZone(com.ibm.icu.util.TimeZone.getTimeZone(tzId));
        pattern = (String) getInternalField("pattern", icuFormat);
        formatData = new DateFormatSymbols(TLocale.getDefault());
    }

    public SimpleDateFormat(String pattern) {
        this(pattern, TLocale.getDefault());
    }

    private void validateFormat(char format) {
        int index = patternChars.indexOf(format);
        if (index == -1) {
            throw new IllegalArgumentException("Unknown pattern character - " + format);
        }
    }

    private void validatePattern(String template) {
        boolean quote = false;
        int next, last = -1, count = 0;

        final int patternLength = template.length();
        for (int i = 0; i < patternLength; i++) {
            next = (template.charAt(i));
            if (next == '\'') {
                if (count > 0) {
                    validateFormat((char) last);
                    count = 0;
                }
                if (last == next) {
                    last = -1;
                } else {
                    last = next;
                }
                quote = !quote;
                continue;
            }
            if (!quote && (last == next || (next >= 'a' && next <= 'z') || (next >= 'A' && next <= 'Z'))) {
                if (last == next) {
                    count++;
                } else {
                    if (count > 0) {
                        validateFormat((char) last);
                    }
                    last = next;
                    count = 1;
                }
            } else {
                if (count > 0) {
                    validateFormat((char) last);
                    count = 0;
                }
                last = -1;
            }
        }
        if (count > 0) {
            validateFormat((char) last);
        }

        if (quote) {
            throw new IllegalArgumentException("Unterminated quote");
        }

    }

    public SimpleDateFormat(String template, DateFormatSymbols value) {
        this(TLocale.getDefault());
        validatePattern(template);
        icuFormat = new com.ibm.icu.text.SimpleDateFormat(template, Locale.getDefault());
        icuFormat.setTimeZone(com.ibm.icu.util.TimeZone.getTimeZone(tzId));
        pattern = template;
        formatData = (DateFormatSymbols) value.clone();
    }

    private void copySymbols(DateFormatSymbols value, com.ibm.icu.text.DateFormatSymbols icuSymbols) {
        icuSymbols.setAmPmStrings(value.getAmPmStrings());
        icuSymbols.setEras(value.getEras());
        icuSymbols.setLocalPatternChars(value.getLocalPatternChars());
        icuSymbols.setMonths(value.getMonths());
        icuSymbols.setShortMonths(value.getShortMonths());
        icuSymbols.setShortWeekdays(value.getShortWeekdays());
        icuSymbols.setWeekdays(value.getWeekdays());
        icuSymbols.setZoneStrings(value.getZoneStrings());
    }

    public SimpleDateFormat(String template, TLocale locale) {
        this(locale);
        validatePattern(template);
        icuFormat = new com.ibm.icu.text.SimpleDateFormat(template, locale);
        icuFormat.setTimeZone(com.ibm.icu.util.TimeZone.getTimeZone(tzId));
        pattern = template;
        formatData = new DateFormatSymbols(locale, icuFormat.getDateFormatSymbols());
    }

    SimpleDateFormat(TLocale locale, com.ibm.icu.text.SimpleDateFormat icuFormat) {
        this(locale);
        this.icuFormat = icuFormat;
        this.icuFormat.setTimeZone(com.ibm.icu.util.TimeZone.getTimeZone(tzId));
        pattern = (String) Format.getInternalField("pattern", icuFormat);
        formatData = new DateFormatSymbols(locale);
    }

    private SimpleDateFormat(TLocale locale) {
        numberFormat = NumberFormat.getInstance(locale);
        numberFormat.setParseIntegerOnly(true);
        numberFormat.setGroupingUsed(false);
        calendar = new GregorianCalendar(locale);
        calendar.add(Calendar.YEAR, -80);
        tzId = calendar.getTimeZone().getID();
        creationYear = calendar.get(Calendar.YEAR);
        defaultCenturyStart = calendar.getTime();
    }

    public void applyLocalizedPattern(String template) {
        icuFormat.applyLocalizedPattern(template);
        pattern = icuFormat.toPattern();
    }

    public void applyPattern(String template) {
        validatePattern(template);
        /*
         * ICU spec explicitly mentions that "ICU interprets a single 'y'
         * differently than Java." We need to do a trick here to follow Java
         * spec.
         */
        String templateForICU = patternForICU(template);
        icuFormat.applyPattern(templateForICU);
        pattern = template;
    }

    @SuppressWarnings("nls")
    private String patternForICU(String p) {
        String[] subPatterns = p.split("'");
        boolean quote = false;
        boolean first = true;
        StringBuilder result = new StringBuilder();
        for (String subPattern : subPatterns) {
            if (!quote) {
                // replace 'y' with 'yy' for ICU to follow Java spec
                result.append((first ? "" : "'") + subPattern.replaceAll("(?<!y)y(?!y)", "yy"));
                first = false;
            } else {
                result.append("'" + subPattern);
            }
            quote = !quote;
        }
        if (p.endsWith("'")) {
            result.append("'");
        }
        return result.toString();
    }

    @Override
    public Object clone() {
        SimpleDateFormat clone = (SimpleDateFormat) super.clone();
        clone.formatData = (DateFormatSymbols) formatData.clone();
        clone.defaultCenturyStart = new Date(defaultCenturyStart.getTime());
        clone.tzId = tzId;
        return clone;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof SimpleDateFormat)) {
            return false;
        }
        SimpleDateFormat simple = (SimpleDateFormat) object;
        return super.equals(object) && pattern.equals(simple.pattern) && formatData.equals(simple.formatData);
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object object) {
        if (object == null) {
            throw new NullPointerException();
        }
        if (object instanceof Date) {
            return formatToCharacterIteratorImpl((Date) object);
        }
        if (object instanceof Number) {
            return formatToCharacterIteratorImpl(new Date(((Number) object).longValue()));
        }
        throw new IllegalArgumentException();

    }

    private AttributedCharacterIterator formatToCharacterIteratorImpl(Date date) {
        StringBuffer buffer = new StringBuffer();
        TList<FieldPosition> fields = new TArrayList<>();

        // format the date, and find fields
        formatImpl(date, buffer, null, fields);

        // create and AttributedString with the formatted buffer
        AttributedString as = new AttributedString(buffer.toString());

        // add DateFormat field attributes to the AttributedString
        for (int i = 0; i < fields.size(); i++) {
            FieldPosition pos = fields.get(i);
            Format.Field attribute = pos.getFieldAttribute();
            as.addAttribute(attribute, attribute, pos.getBeginIndex(), pos.getEndIndex());
        }

        // return the CharacterIterator from AttributedString
        return as.getIterator();
    }

    private StringBuffer formatImpl(Date date, StringBuffer buffer, FieldPosition field, TList<FieldPosition> fields) {

        boolean quote = false;
        int next, last = -1, count = 0;
        calendar.setTime(date);
        if (field != null) {
            field.clear();
        }

        final int patternLength = pattern.length();
        for (int i = 0; i < patternLength; i++) {
            next = (pattern.charAt(i));
            if (next == '\'') {
                if (count > 0) {
                    append(buffer, field, fields, (char) last, count);
                    count = 0;
                }
                if (last == next) {
                    buffer.append('\'');
                    last = -1;
                } else {
                    last = next;
                }
                quote = !quote;
                continue;
            }
            if (!quote && (last == next || (next >= 'a' && next <= 'z') || (next >= 'A' && next <= 'Z'))) {
                if (last == next) {
                    count++;
                } else {
                    if (count > 0) {
                        append(buffer, field, fields, (char) last, count);
                    }
                    last = next;
                    count = 1;
                }
            } else {
                if (count > 0) {
                    append(buffer, field, fields, (char) last, count);
                    count = 0;
                }
                last = -1;
                buffer.append((char) next);
            }
        }
        if (count > 0) {
            append(buffer, field, fields, (char) last, count);
        }
        return buffer;
    }

    private void append(StringBuffer buffer, FieldPosition position, TList<FieldPosition> fields, char format, int count) {
        int field = -1;
        int index = patternChars.indexOf(format);
        if (index == -1) {
            throw new IllegalArgumentException("Unknown pattern character - " + format);
        }

        int beginPosition = buffer.length();
        Field dateFormatField = null;
        switch (index) {
            case ERA_FIELD:
                dateFormatField = Field.ERA;
                buffer.append(formatData.eras[calendar.get(Calendar.ERA)]);
                break;
            case YEAR_FIELD:
                dateFormatField = Field.YEAR;
                int year = calendar.get(Calendar.YEAR);
                if (count < 4) {
                    appendNumber(buffer, 2, year % 100);
                } else {
                    appendNumber(buffer, count, year);
                }
                break;
            case MONTH_FIELD:
                dateFormatField = Field.MONTH;
                int month = calendar.get(Calendar.MONTH);
                if (count <= 2) {
                    appendNumber(buffer, count, month + 1);
                } else if (count == 3) {
                    buffer.append(formatData.shortMonths[month]);
                } else {
                    buffer.append(formatData.months[month]);
                }
                break;
            case DATE_FIELD:
                dateFormatField = Field.DAY_OF_MONTH;
                field = Calendar.DATE;
                break;
            case HOUR_OF_DAY1_FIELD: // k
                dateFormatField = Field.HOUR_OF_DAY1;
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                appendNumber(buffer, count, hour == 0 ? 24 : hour);
                break;
            case HOUR_OF_DAY0_FIELD: // H
                dateFormatField = Field.HOUR_OF_DAY0;
                field = Calendar.HOUR_OF_DAY;
                break;
            case MINUTE_FIELD:
                dateFormatField = Field.MINUTE;
                field = Calendar.MINUTE;
                break;
            case SECOND_FIELD:
                dateFormatField = Field.SECOND;
                field = Calendar.SECOND;
                break;
            case MILLISECOND_FIELD:
                dateFormatField = Field.MILLISECOND;
                int value = calendar.get(Calendar.MILLISECOND);
                appendNumber(buffer, count, value);
                break;
            case DAY_OF_WEEK_FIELD:
                dateFormatField = Field.DAY_OF_WEEK;
                int day = calendar.get(Calendar.DAY_OF_WEEK);
                if (count < 4) {
                    buffer.append(formatData.shortWeekdays[day]);
                } else {
                    buffer.append(formatData.weekdays[day]);
                }
                break;
            case DAY_OF_YEAR_FIELD:
                dateFormatField = Field.DAY_OF_YEAR;
                field = Calendar.DAY_OF_YEAR;
                break;
            case DAY_OF_WEEK_IN_MONTH_FIELD:
                dateFormatField = Field.DAY_OF_WEEK_IN_MONTH;
                field = Calendar.DAY_OF_WEEK_IN_MONTH;
                break;
            case WEEK_OF_YEAR_FIELD:
                dateFormatField = Field.WEEK_OF_YEAR;
                field = Calendar.WEEK_OF_YEAR;
                break;
            case WEEK_OF_MONTH_FIELD:
                dateFormatField = Field.WEEK_OF_MONTH;
                field = Calendar.WEEK_OF_MONTH;
                break;
            case AM_PM_FIELD:
                dateFormatField = Field.AM_PM;
                buffer.append(formatData.ampms[calendar.get(Calendar.AM_PM)]);
                break;
            case HOUR1_FIELD: // h
                dateFormatField = Field.HOUR1;
                hour = calendar.get(Calendar.HOUR);
                appendNumber(buffer, count, hour == 0 ? 12 : hour);
                break;
            case HOUR0_FIELD: // K
                dateFormatField = Field.HOUR0;
                field = Calendar.HOUR;
                break;
            case TIMEZONE_FIELD: // z
                dateFormatField = Field.TIME_ZONE;
                appendTimeZone(buffer, count, true);
                break;
            case com.ibm.icu.text.DateFormat.TIMEZONE_RFC_FIELD: // Z
                dateFormatField = Field.TIME_ZONE;
                appendTimeZone(buffer, count, false);
                break;
        }
        if (field != -1) {
            appendNumber(buffer, count, calendar.get(field));
        }

        if (fields != null) {
            position = new FieldPosition(dateFormatField);
            position.setBeginIndex(beginPosition);
            position.setEndIndex(buffer.length());
            fields.add(position);
        } else {
            // Set to the first occurrence
            if ((position.getFieldAttribute() == dateFormatField || (position.getFieldAttribute() == null && position
                    .getField() == index)) && position.getEndIndex() == 0) {
                position.setBeginIndex(beginPosition);
                position.setEndIndex(buffer.length());
            }
        }
    }

    private void appendTimeZone(StringBuffer buffer, int count, boolean generalTimezone) {
        // cannot call TimeZone.getDisplayName() because it would not use
        // the DateFormatSymbols of this SimpleDateFormat

        if (generalTimezone) {
            String id = calendar.getTimeZone().getID();
            String[][] zones = formatData.getZoneStrings();
            String[] zone = null;
            for (String[] element : zones) {
                if (id.equals(element[0])) {
                    zone = element;
                    break;
                }
            }
            if (zone == null) {
                int offset = calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);
                char sign = '+';
                if (offset < 0) {
                    sign = '-';
                    offset = -offset;
                }
                buffer.append("GMT");
                buffer.append(sign);
                appendNumber(buffer, 2, offset / 3600000);
                buffer.append(':');
                appendNumber(buffer, 2, (offset % 3600000) / 60000);
            } else {
                int daylight = calendar.get(Calendar.DST_OFFSET) == 0 ? 0 : 2;
                if (count < 4) {
                    buffer.append(zone[2 + daylight]);
                } else {
                    buffer.append(zone[1 + daylight]);
                }
            }
        } else {
            int offset = calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);
            char sign = '+';
            if (offset < 0) {
                sign = '-';
                offset = -offset;
            }
            buffer.append(sign);
            appendNumber(buffer, 2, offset / 3600000);
            appendNumber(buffer, 2, (offset % 3600000) / 60000);
        }
    }

    private void appendNumber(StringBuffer buffer, int count, int value) {
        int minimumIntegerDigits = numberFormat.getMinimumIntegerDigits();
        numberFormat.setMinimumIntegerDigits(count);
        numberFormat.format(new Integer(value), buffer, new FieldPosition(0));
        numberFormat.setMinimumIntegerDigits(minimumIntegerDigits);
    }

    @Override
    public StringBuffer format(Date date, StringBuffer buffer, FieldPosition fieldPos) {
        String id = calendar.getTimeZone().getID();
        if (!tzId.equals(id)) {
            tzId = id;
            icuFormat.setTimeZone(com.ibm.icu.util.TimeZone.getTimeZone(tzId));
        }
        // As ICU has its own implementation for DateFormat.Field, we need to
        // pass an ICU instance of DateFormat.Field to the FieldPosition to get
        // the begin and end index.
        StringBuffer result = null;
        Format.Field attribute = fieldPos.getFieldAttribute();
        if (attribute instanceof DateFormat.Field) {
            com.ibm.icu.text.DateFormat.Field icuAttribute = toICUField((DateFormat.Field) attribute);
            int field = fieldPos.getField();
            FieldPosition icuFieldPos = new FieldPosition(icuAttribute, field);
            result = icuFormat.format(date, buffer, icuFieldPos);
            fieldPos.setBeginIndex(icuFieldPos.getBeginIndex());
            fieldPos.setEndIndex(icuFieldPos.getEndIndex());
            return result;
        }
        return icuFormat.format(date, buffer, fieldPos);
    }

    private com.ibm.icu.text.DateFormat.Field toICUField(DateFormat.Field attribute) {
        com.ibm.icu.text.DateFormat.Field icuAttribute = null;

        if (attribute == DateFormat.Field.ERA) {
            icuAttribute = com.ibm.icu.text.DateFormat.Field.ERA;
        } else if (attribute == DateFormat.Field.YEAR) {
            icuAttribute = com.ibm.icu.text.DateFormat.Field.YEAR;
        } else if (attribute == DateFormat.Field.MONTH) {
            icuAttribute = com.ibm.icu.text.DateFormat.Field.MONTH;
        } else if (attribute == DateFormat.Field.HOUR_OF_DAY0) {
            icuAttribute = com.ibm.icu.text.DateFormat.Field.HOUR_OF_DAY0;
        } else if (attribute == DateFormat.Field.HOUR_OF_DAY1) {
            icuAttribute = com.ibm.icu.text.DateFormat.Field.HOUR_OF_DAY1;
        } else if (attribute == DateFormat.Field.MINUTE) {
            icuAttribute = com.ibm.icu.text.DateFormat.Field.MINUTE;
        } else if (attribute == DateFormat.Field.SECOND) {
            icuAttribute = com.ibm.icu.text.DateFormat.Field.SECOND;
        } else if (attribute == DateFormat.Field.MILLISECOND) {
            icuAttribute = com.ibm.icu.text.DateFormat.Field.MILLISECOND;
        } else if (attribute == DateFormat.Field.DAY_OF_WEEK) {
            icuAttribute = com.ibm.icu.text.DateFormat.Field.DAY_OF_WEEK;
        } else if (attribute == DateFormat.Field.DAY_OF_MONTH) {
            icuAttribute = com.ibm.icu.text.DateFormat.Field.DAY_OF_MONTH;
        } else if (attribute == DateFormat.Field.DAY_OF_YEAR) {
            icuAttribute = com.ibm.icu.text.DateFormat.Field.DAY_OF_YEAR;
        } else if (attribute == DateFormat.Field.DAY_OF_WEEK_IN_MONTH) {
            icuAttribute = com.ibm.icu.text.DateFormat.Field.DAY_OF_WEEK_IN_MONTH;
        } else if (attribute == DateFormat.Field.WEEK_OF_YEAR) {
            icuAttribute = com.ibm.icu.text.DateFormat.Field.WEEK_OF_YEAR;
        } else if (attribute == DateFormat.Field.WEEK_OF_MONTH) {
            icuAttribute = com.ibm.icu.text.DateFormat.Field.WEEK_OF_MONTH;
        } else if (attribute == DateFormat.Field.AM_PM) {
            icuAttribute = com.ibm.icu.text.DateFormat.Field.AM_PM;
        } else if (attribute == DateFormat.Field.HOUR0) {
            icuAttribute = com.ibm.icu.text.DateFormat.Field.HOUR0;
        } else if (attribute == DateFormat.Field.HOUR1) {
            icuAttribute = com.ibm.icu.text.DateFormat.Field.HOUR1;
        } else if (attribute == DateFormat.Field.TIME_ZONE) {
            icuAttribute = com.ibm.icu.text.DateFormat.Field.TIME_ZONE;
        }

        return icuAttribute;
    }

    public Date get2DigitYearStart() {
        return defaultCenturyStart;
    }

    public DateFormatSymbols getDateFormatSymbols() {
        // Return a clone so the arrays in the ResourceBundle are not modified
        return (DateFormatSymbols) formatData.clone();
    }

    @Override
    public int hashCode() {
        return super.hashCode() + pattern.hashCode() + formatData.hashCode() + creationYear;
    }

    @Override
    public Date parse(String string, ParsePosition position) {
        String id = calendar.getTimeZone().getID();
        if (!tzId.equals(id)) {
            tzId = id;
            icuFormat.setTimeZone(com.ibm.icu.util.TimeZone.getTimeZone(tzId));
        }
        icuFormat.setLenient(calendar.isLenient());
        return icuFormat.parse(string, position);
    }

    public void set2DigitYearStart(Date date) {
        icuFormat.set2DigitYearStart(date);
        defaultCenturyStart = date;
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        creationYear = cal.get(Calendar.YEAR);
    }

    public void setDateFormatSymbols(DateFormatSymbols value) {
        com.ibm.icu.text.DateFormatSymbols icuSymbols = new com.ibm.icu.text.DateFormatSymbols();
        copySymbols(value, icuSymbols);
        icuFormat.setDateFormatSymbols(icuSymbols);
        formatData = (DateFormatSymbols) value.clone();
    }

    public String toLocalizedPattern() {
        return icuFormat.toLocalizedPattern();
    }

    public String toPattern() {
        return pattern;
    }
}
