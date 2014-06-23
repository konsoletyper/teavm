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

import org.teavm.classlib.java.util.TCalendar;
import org.teavm.classlib.java.util.TDate;
import org.teavm.classlib.java.util.TGregorianCalendar;

/**
 *
 * @author Alexey Andreev
 */
public class TSimpleDateFormat extends TDateFormat {
    private DateFormatElement[] elements;

    @Override
    public StringBuffer format(TDate date, StringBuffer buffer, TFieldPosition field) {
        TCalendar calendar = new TGregorianCalendar();
        calendar.setTime(date);
        for (DateFormatElement element : elements) {
            element.format(calendar, buffer);
        }
        return buffer;
    }

    @Override
    public TDate parse(String string, TParsePosition position) {
        TCalendar calendar = new TGregorianCalendar();
        calendar.set(0, 0, 0, 0, 0, 0);
        for (DateFormatElement element : elements) {
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
}
