/*
 *  Copyright 2014 Alexey Andreev.
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

import java.util.Arrays;
import org.teavm.classlib.impl.unicode.CLDRHelper;
import org.teavm.classlib.java.util.TCalendar;
import org.teavm.classlib.java.util.TDate;
import org.teavm.classlib.java.util.TGregorianCalendar;
import org.teavm.classlib.java.util.TLocale;

public class TSimpleDateFormat extends TDateFormat {
    private TDateFormatSymbols dateFormatSymbols;
    private TDateFormatElement[] elements;
    private String pattern;
    private TLocale locale;

    public TSimpleDateFormat() {
        this(getDefaultPattern());
    }

    private static String getDefaultPattern() {
        TLocale locale = TLocale.getDefault();
        return CLDRHelper.resolveDateFormats(locale.getLanguage(), locale.getCountry()).getMediumFormat();
    }

    public TSimpleDateFormat(String pattern) {
        this(pattern, TLocale.getDefault());
    }

    public TSimpleDateFormat(String pattern, TLocale locale) {
        this(pattern, new TDateFormatSymbols(locale));
        this.locale = locale;
    }

    public TSimpleDateFormat(String pattern, TDateFormatSymbols dateFormatSymbols) {
        this.dateFormatSymbols = (TDateFormatSymbols) dateFormatSymbols.clone();
        locale = TLocale.getDefault();
        applyPattern(pattern);
    }

    @Override
    public StringBuffer format(TDate date, StringBuffer buffer, TFieldPosition field) {
        TCalendar calendar = new TGregorianCalendar(locale);
        calendar.setTime(date);
        calendar.setTimeZone(this.calendar.getTimeZone());
        for (TDateFormatElement element : elements) {
            element.format(calendar, buffer);
        }
        return buffer;
    }

    public void applyPattern(String pattern) {
        this.pattern = pattern;
        reparsePattern();
    }

    private void reparsePattern() {
        TSimpleDatePatternParser parser = new TSimpleDatePatternParser(dateFormatSymbols, locale);
        parser.parsePattern(pattern);
        elements = parser.getElements().toArray(new TDateFormatElement[0]);
    }

    @Override
    public TDate parse(String string, TParsePosition position) {
        TCalendar calendar = (TCalendar) this.calendar.clone();
        calendar.clear();
        for (TDateFormatElement element : elements) {
            if (position.getIndex() > string.length()) {
                position.setErrorIndex(position.getErrorIndex());
                return null;
            }
            element.parse(string, calendar, position);
            if (position.getErrorIndex() >= 0) {
                return null;
            }
        }
        return calendar.getTime();
    }

    @Override
    public Object clone() {
        TSimpleDateFormat copy = (TSimpleDateFormat) super.clone();
        copy.dateFormatSymbols = (TDateFormatSymbols) dateFormatSymbols.clone();
        copy.elements = elements.clone();
        return copy;
    }

    public TDateFormatSymbols getDateFormatSymbols() {
        return (TDateFormatSymbols) dateFormatSymbols.clone();
    }

    public void setDateFormatSymbols(TDateFormatSymbols newFormatSymbols) {
        dateFormatSymbols = (TDateFormatSymbols) newFormatSymbols.clone();
        reparsePattern();
    }

    public String toPattern() {
        return pattern;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof TSimpleDateFormat)) {
            return false;
        }

        TSimpleDateFormat other = (TSimpleDateFormat) object;
        if (!super.equals(other)) {
            return false;
        }

        return Arrays.equals(elements, other.elements)
                && dateFormatSymbols.equals(other.dateFormatSymbols)
                && locale.equals(other.locale);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new int[] {
                super.hashCode(),
                dateFormatSymbols.hashCode(),
                Arrays.hashCode(elements),
                locale.hashCode() });
    }
}
