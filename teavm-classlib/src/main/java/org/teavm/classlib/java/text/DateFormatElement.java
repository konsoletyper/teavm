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

/**
 *
 * @author Alexey Andreev
 */
abstract class DateFormatElement {
    public abstract void format(TCalendar date, StringBuffer buffer);

    public abstract void parse(String text, TCalendar date, TParsePosition position);

    static boolean matches(String text, int position, String pattern) {
        if (pattern.length() + position > text.length()) {
            return false;
        }
        for (int i = 0; i < pattern.length(); ++i) {
            if (Character.toLowerCase(pattern.charAt(i)) != Character.toLowerCase(text.charAt(position++))) {
                return false;
            }
        }
        return true;
    }

    static int whichMatches(String text, TParsePosition position, String[] patterns) {
        for (int i = 0; i < patterns.length; ++i) {
            if (matches(text, position.getIndex(), patterns[i])) {
                position.setIndex(position.getIndex() + patterns[i].length());
                return i;
            }
        }
        return -1;
    }

    public static class MonthText extends DateFormatElement {
        String[] months;
        String[] shortMonths;
        boolean abbreviated;

        public MonthText(TDateFormatSymbols symbols, boolean abbreviated) {
            months = symbols.getMonths();
            shortMonths = symbols.getShortMonths();
            this.abbreviated = abbreviated;
        }

        @Override
        public void format(TCalendar date, StringBuffer buffer) {
            int month = date.get(TCalendar.MONTH);
            buffer.append(abbreviated ? shortMonths[month] : months[month]);
        }

        @Override
        public void parse(String text, TCalendar date, TParsePosition position) {
            int month = whichMatches(text, position, months);
            if (month < 0) {
                month = whichMatches(text, position, shortMonths);
            }
            if (month < 0) {
                position.setErrorIndex(position.getIndex());
            } else {
                date.set(TCalendar.MONTH, month);
            }
        }
    }

    public static class WeekdayText extends DateFormatElement {
        String[] weeks;
        String[] shortWeeks;
        boolean abbreviated;

        public WeekdayText(TDateFormatSymbols symbols, boolean abbreviated) {
            weeks = symbols.getWeekdays();
            shortWeeks = symbols.getShortWeekdays();
            this.abbreviated = abbreviated;
        }

        @Override
        public void format(TCalendar date, StringBuffer buffer) {
            int weekday = date.get(TCalendar.DAY_OF_WEEK);
            buffer.append(abbreviated ? shortWeeks[weekday] : weeks[weekday]);
        }

        @Override
        public void parse(String text, TCalendar date, TParsePosition position) {
            int weekday = whichMatches(text, position, weeks);
            if (weekday < 0) {
                weekday = whichMatches(text, position, shortWeeks);
            }
            if (weekday < 0) {
                position.setErrorIndex(position.getIndex());
            } else {
                date.set(TCalendar.WEEK_OF_MONTH, weekday + 1);
            }
        }
    }

    public static class EraText extends DateFormatElement {
        String[] eras;

        public EraText(TDateFormatSymbols symbols) {
            eras = symbols.getEras();
        }

        @Override
        public void format(TCalendar date, StringBuffer buffer) {
            int era = date.get(TCalendar.ERA);
            buffer.append(eras[era]);
        }

        @Override
        public void parse(String text, TCalendar date, TParsePosition position) {
            int era = whichMatches(text, position, eras);
            if (era < 0) {
                position.setErrorIndex(position.getIndex());
            } else {
                date.set(TCalendar.ERA, era);
            }
        }
    }

    public static class AmPmText extends DateFormatElement {
        String[] ampms;

        public AmPmText(TDateFormatSymbols symbols) {
            ampms = symbols.getAmPmStrings();
        }

        @Override
        public void format(TCalendar date, StringBuffer buffer) {
            int ampm = date.get(TCalendar.AM_PM);
            buffer.append(ampms[ampm]);
        }

        @Override
        public void parse(String text, TCalendar date, TParsePosition position) {
            int ampm = whichMatches(text, position, ampms);
            if (ampm < 0) {
                position.setErrorIndex(position.getIndex());
            } else {
                date.set(TCalendar.AM_PM, ampm);
            }
        }
    }
}
