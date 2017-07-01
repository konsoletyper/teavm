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

import java.util.ArrayList;
import java.util.List;
import org.teavm.classlib.java.util.TCalendar;
import org.teavm.classlib.java.util.TLocale;

class TSimpleDatePatternParser {
    private TDateFormatSymbols symbols;
    private TLocale locale;
    private List<TDateFormatElement> elements = new ArrayList<>();
    private int index;
    private String pattern;

    public TSimpleDatePatternParser(TDateFormatSymbols symbols, TLocale locale) {
        this.symbols = symbols;
        this.locale = locale;
    }

    public List<TDateFormatElement> getElements() {
        return elements;
    }

    public void parsePattern(String pattern) {
        elements.clear();
        this.pattern = pattern;
        for (index = 0; index < pattern.length();) {
            char c = pattern.charAt(index);
            switch (c) {
                case '\'': {
                    ++index;
                    parseQuoted();
                    break;
                }
                case 'G':
                    parseRepetitions();
                    elements.add(new TDateFormatElement.EraText(symbols));
                    break;
                case 'y':
                case 'Y': {
                    int rep = parseRepetitions();
                    if (rep == 2) {
                        elements.add(new TDateFormatElement.Year(TCalendar.YEAR));
                    } else {
                        elements.add(new TDateFormatElement.Numeric(TCalendar.YEAR, rep));
                    }
                    break;
                }
                case 'M':
                case 'L': {
                    int rep = parseRepetitions();
                    if (rep <= 2) {
                        elements.add(new TDateFormatElement.NumericMonth(rep));
                    } else {
                        elements.add(new TDateFormatElement.MonthText(symbols, rep == 3));
                    }
                    break;
                }
                case 'w': {
                    int rep = parseRepetitions();
                    elements.add(new TDateFormatElement.Numeric(TCalendar.WEEK_OF_YEAR, rep));
                    break;
                }
                case 'W': {
                    int rep = parseRepetitions();
                    elements.add(new TDateFormatElement.Numeric(TCalendar.WEEK_OF_MONTH, rep));
                    break;
                }
                case 'D': {
                    int rep = parseRepetitions();
                    elements.add(new TDateFormatElement.Numeric(TCalendar.DAY_OF_YEAR, rep));
                    break;
                }
                case 'd': {
                    int rep = parseRepetitions();
                    elements.add(new TDateFormatElement.Numeric(TCalendar.DAY_OF_MONTH, rep));
                    break;
                }
                case 'F': {
                    int rep = parseRepetitions();
                    elements.add(new TDateFormatElement.Numeric(TCalendar.DAY_OF_WEEK_IN_MONTH, rep));
                    break;
                }
                case 'E':
                case 'c': {
                    int rep = parseRepetitions();
                    elements.add(new TDateFormatElement.WeekdayText(symbols, rep <= 3));
                    break;
                }
                case 'u': {
                    int rep = parseRepetitions();
                    elements.add(new TDateFormatElement.NumericWeekday(rep));
                    break;
                }
                case 'a': {
                    parseRepetitions();
                    elements.add(new TDateFormatElement.AmPmText(symbols));
                    break;
                }
                case 'H': {
                    int rep = parseRepetitions();
                    elements.add(new TDateFormatElement.Numeric(TCalendar.HOUR_OF_DAY, rep));
                    break;
                }
                case 'k': {
                    int rep = parseRepetitions();
                    elements.add(new TDateFormatElement.NumericHour(TCalendar.HOUR_OF_DAY, rep, 24));
                    break;
                }
                case 'K': {
                    int rep = parseRepetitions();
                    elements.add(new TDateFormatElement.Numeric(TCalendar.HOUR, rep));
                    break;
                }
                case 'h': {
                    int rep = parseRepetitions();
                    elements.add(new TDateFormatElement.NumericHour(TCalendar.HOUR, rep, 12));
                    break;
                }
                case 'm': {
                    int rep = parseRepetitions();
                    elements.add(new TDateFormatElement.Numeric(TCalendar.MINUTE, rep));
                    break;
                }
                case 's': {
                    int rep = parseRepetitions();
                    elements.add(new TDateFormatElement.Numeric(TCalendar.SECOND, rep));
                    break;
                }
                case 'S': {
                    int rep = parseRepetitions();
                    elements.add(new TDateFormatElement.Numeric(TCalendar.MILLISECOND, rep));
                    break;
                }
                case 'z': {
                    parseRepetitions();
                    elements.add(new TDateFormatElement.GeneralTimezone(locale));
                    break;
                }
                case 'Z': {
                    parseRepetitions();
                    elements.add(new TDateFormatElement.Rfc822Timezone(locale));
                    break;
                }
                case 'X': {
                    int rep = parseRepetitions();
                    if (rep > 3) {
                        throw new IllegalArgumentException("Wrong number of repetitions of X pattern at " + index);
                    }
                    elements.add(new TDateFormatElement.Iso8601Timezone(rep));
                    break;
                }
                default:
                    if (isControl(c)) {
                        parseRepetitions();
                    } else {
                        StringBuilder sb = new StringBuilder();
                        while (index < pattern.length() && !isControl(pattern.charAt(index))) {
                            sb.append(pattern.charAt(index++));
                        }
                        elements.add(new TDateFormatElement.ConstantText(sb.toString()));
                    }
                    break;
            }
        }
    }

    private boolean isControl(char c) {
        return c == '\'' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    private void parseQuoted() {
        StringBuilder sb = new StringBuilder();
        while (index < pattern.length()) {
            char c = pattern.charAt(index++);
            if (c == '\'') {
                if (index < pattern.length() && pattern.charAt(index) == '\'') {
                    sb.append('\'');
                    ++index;
                } else {
                    break;
                }
            } else {
                sb.append(c);
            }
        }
        elements.add(new TDateFormatElement.ConstantText(sb.toString()));
    }

    private int parseRepetitions() {
        int count = 1;
        char orig = pattern.charAt(index++);
        while (index < pattern.length() && pattern.charAt(index) == orig) {
            ++index;
            ++count;
        }
        return count;
    }
}
