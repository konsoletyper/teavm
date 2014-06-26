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
import org.teavm.classlib.java.util.TGregorianCalendar;

/**
 *
 * @author Alexey Andreev
 */
abstract class TDateFormatElement {
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

    public static class MonthText extends TDateFormatElement {
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

    public static class WeekdayText extends TDateFormatElement {
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
            int weekday = date.get(TCalendar.DAY_OF_WEEK) - 1;
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

    public static class EraText extends TDateFormatElement {
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

    public static class AmPmText extends TDateFormatElement {
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

    public static class Numeric extends TDateFormatElement {
        private int field;
        private int length;

        public Numeric(int field, int length) {
            this.field = field;
            this.length = length;
        }

        @Override
        public void format(TCalendar date, StringBuffer buffer) {
            int number = processBeforeFormat(date.get(field));
            String str = Integer.toString(number);
            for (int i = str.length(); i < length; ++i) {
                buffer.append('0');
            }
            buffer.append(str);
        }

        @Override
        public void parse(String text, TCalendar date, TParsePosition position) {
            int num = 0;
            int i = 0;
            int pos = position.getIndex();
            while (pos < text.length()) {
                char c = text.charAt(pos);
                if (c >= '0' && c <= '9') {
                    num = num * 10 + (c - '0');
                    ++pos;
                    ++i;
                } else {
                    break;
                }
            }
            if (i < length) {
                position.setErrorIndex(position.getIndex());
                return;
            }
            position.setIndex(pos);
            date.set(field, processAfterParse(num));
        }

        protected int processBeforeFormat(int num) {
            return num;
        }

        protected int processAfterParse(int num) {
            return num;
        }
    }

    public static class NumericMonth extends Numeric {
        public NumericMonth(int length) {
            super(TCalendar.MONTH, length);
        }

        @Override
        protected int processBeforeFormat(int num) {
            return num + 1;
        }

        @Override
        protected int processAfterParse(int num) {
            return num - 1;
        }
    }

    public static class NumericWeekday extends Numeric {
        public NumericWeekday(int length) {
            super(TCalendar.DAY_OF_WEEK, length);
        }

        @Override
        protected int processBeforeFormat(int num) {
            return num == 1 ? 7 : num - 1;
        }

        @Override
        protected int processAfterParse(int num) {
            return num == 7 ? 1 : num + 1;
        }
    }

    public static class NumericHour extends Numeric {
        private int limit;

        public NumericHour(int field, int length, int limit) {
            super(field, length);
            this.limit = limit;
        }

        @Override
        protected int processBeforeFormat(int num) {
            return num == 0 ? limit : num;
        }

        @Override
        protected int processAfterParse(int num) {
            return num == limit ? 0 : num;
        }
    }

    public static class Year extends TDateFormatElement {
        private int field;

        public Year(int field) {
            this.field = field;
        }

        @Override
        public void format(TCalendar date, StringBuffer buffer) {
            int number = date.get(field);
            if (number < 10) {
                buffer.append(number);
            } else {
                buffer.append((char)((number % 100 / 10) + '0'));
                buffer.append((char)((number % 10) + '0'));
            }
        }

        @Override
        public void parse(String text, TCalendar date, TParsePosition position) {
            int num = 0;
            int pos = position.getIndex();
            char c = text.charAt(pos++);
            if (c < '0' || c > '9') {
                position.setErrorIndex(position.getErrorIndex());
                return;
            }
            num = c - '0';
            c = text.charAt(pos);
            if (c >= '0' && c <= '9') {
                num = num * 10 + (c - '0');
                ++pos;
            }
            position.setIndex(pos);
            TCalendar calendar = new TGregorianCalendar();
            int currentYear = calendar.get(TCalendar.YEAR);
            int currentShortYear = currentYear % 100;
            int century = currentYear / 100;
            if (currentShortYear > 80) {
                if (num < currentShortYear - 80) {
                    century++;
                }
            } else {
                if (num > currentShortYear + 20) {
                    --century;
                }
            }
            date.set(field, num + century * 100);
        }
    }

    public static class ConstantText extends TDateFormatElement {
        private String textConstant;

        public ConstantText(String textConstant) {
            this.textConstant = textConstant;
        }

        @Override
        public void format(TCalendar date, StringBuffer buffer) {
            buffer.append(textConstant);
        }

        @Override
        public void parse(String text, TCalendar date, TParsePosition position) {
            if (matches(text, position.getIndex(), textConstant)) {
                position.setIndex(position.getIndex() + textConstant.length());
            } else {
                position.setErrorIndex(position.getIndex());
            }
        }
    }
}
