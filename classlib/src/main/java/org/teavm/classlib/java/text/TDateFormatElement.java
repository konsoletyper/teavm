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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.teavm.classlib.java.util.TCalendar;
import org.teavm.classlib.java.util.TGregorianCalendar;
import org.teavm.classlib.java.util.TLocale;
import org.teavm.classlib.java.util.TTimeZone;

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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MonthText monthText = (MonthText) o;
            return abbreviated == monthText.abbreviated
                    && Arrays.equals(months, monthText.months)
                    && Arrays.equals(shortMonths, monthText.shortMonths);
        }

        @Override
        public int hashCode() {
            return Objects.hash(months, shortMonths, abbreviated);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            WeekdayText that = (WeekdayText) o;
            return abbreviated == that.abbreviated
                    && Arrays.equals(weeks, that.weeks)
                    && Arrays.equals(shortWeeks, that.shortWeeks);
        }

        @Override
        public int hashCode() {
            return Objects.hash(weeks, shortWeeks, abbreviated);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            EraText eraText = (EraText) o;
            return Arrays.equals(eras, eraText.eras);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(eras);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AmPmText amPmText = (AmPmText) o;
            return Arrays.equals(ampms, amPmText.ampms);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(ampms);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Numeric numeric = (Numeric) o;
            return field == numeric.field && length == numeric.length;
        }

        @Override
        public int hashCode() {
            return Objects.hash(field, length);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            NumericHour that = (NumericHour) o;
            return limit == that.limit;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), limit);
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
                buffer.append((char) ((number % 100 / 10) + '0'));
                buffer.append((char) ((number % 10) + '0'));
            }
        }

        @Override
        public void parse(String text, TCalendar date, TParsePosition position) {
            int num;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Year year = (Year) o;
            return field == year.field;
        }

        @Override
        public int hashCode() {
            return Objects.hash(field);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ConstantText that = (ConstantText) o;
            return Objects.equals(textConstant, that.textConstant);
        }

        @Override
        public int hashCode() {
            return Objects.hash(textConstant);
        }
    }

    public static abstract class BaseTimezone extends TDateFormatElement {
        private static Map<TLocale, TrieNode> cache;
        private static TrieNode idSearchTrie;
        protected TLocale locale;
        private TrieNode searchTrie;

        public BaseTimezone(TLocale locale) {
            this.locale = locale;
        }

        @Override
        public void parse(String text, TCalendar date, TParsePosition position) {
            if (tryParseFixedTimeZone(text, date, position)) {
                return;
            }
            prepareTrie();
            TTimeZone tz = match(searchTrie, text, position);
            if (tz != null) {
                date.setTimeZone(tz);
            } else {
                prepareIdTrie();
                tz = match(idSearchTrie, text, position);
                if (tz != null) {
                    date.setTimeZone(tz);
                } else {
                    position.setErrorIndex(position.getIndex());
                }
            }
        }

        public TTimeZone match(TrieNode node, String text, TParsePosition position) {
            int start = position.getIndex();
            int index = start;
            int lastMatch = start;
            TTimeZone tz = null;
            while (node.childNodes != null && node.childNodes.length > 0) {
                if (node.tz != null) {
                    lastMatch = index;
                    tz = node.tz;
                }
                if (index >= text.length()) {
                    break;
                }
                int next = Arrays.binarySearch(node.chars, Character.toLowerCase(text.charAt(index++)));
                if (next < 0) {
                    return null;
                }
                node = node.childNodes[next];
            }
            if (node.tz != null) {
                lastMatch = index;
                tz = node.tz;
            }
            position.setIndex(lastMatch);
            return tz;
        }

        private void prepareTrie() {
            if (searchTrie != null) {
                return;
            }
            if (cache == null) {
                cache = new HashMap<>();
            }
            searchTrie = cache.get(locale);
            if (searchTrie != null) {
                return;
            }
            TrieBuilder builder = new TrieBuilder();
            for (String tzId : TTimeZone.getAvailableIDs()) {
                TTimeZone tz = TTimeZone.getTimeZone(tzId);
                builder.add(tz.getDisplayName(locale), tz);
            }
            searchTrie = builder.build();
            cache.put(locale, searchTrie);
        }

        private static void prepareIdTrie() {
            if (idSearchTrie != null) {
                return;
            }
            TrieBuilder builder = new TrieBuilder();
            for (String tzId : TTimeZone.getAvailableIDs()) {
                TTimeZone tz = TTimeZone.getTimeZone(tzId);
                builder.add(tz.getID(), tz);
            }
            idSearchTrie = builder.build();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BaseTimezone that = (BaseTimezone) o;
            return Objects.equals(locale, that.locale) && Objects.equals(searchTrie, that.searchTrie);
        }

        @Override
        public int hashCode() {
            return Objects.hash(locale, searchTrie);
        }
    }

    public static class GeneralTimezone extends BaseTimezone {
        public GeneralTimezone(TLocale locale) {
            super(locale);
        }

        @Override
        public void format(TCalendar date, StringBuffer buffer) {
            TTimeZone tz = date.getTimeZone();
            if (tz.getID().startsWith("GMT")) {
                int minutes = tz.getRawOffset() / 60_000;
                buffer.append("GMT");
                if (minutes >= 0) {
                    buffer.append('+');
                } else {
                    minutes = -minutes;
                    buffer.append('-');
                }
                int hours = minutes / 60;
                minutes %= 60;
                buffer.append(hours / 10).append(hours % 10).append(':').append(minutes / 10).append(minutes % 10);
            } else {
                buffer.append(tz.getDisplayName(locale));
            }
        }
    }

    public static class Rfc822Timezone extends BaseTimezone {
        public Rfc822Timezone(TLocale locale) {
            super(locale);
        }

        @Override
        public void format(TCalendar date, StringBuffer buffer) {
            TTimeZone tz = date.getTimeZone();
            int minutes = tz.getOffset(date.getTimeInMillis()) / 60_000;
            if (minutes >= 0) {
                buffer.append('+');
            } else {
                minutes = -minutes;
                buffer.append('-');
            }
            int hours = minutes / 60;
            minutes %= 60;
            buffer.append(hours / 10).append(hours % 10).append(minutes / 10).append(minutes % 10);
        }
    }

    public static class Iso8601Timezone extends TDateFormatElement {
        private int size;

        public Iso8601Timezone(int size) {
            this.size = size;
        }

        @Override
        public void format(TCalendar date, StringBuffer buffer) {
            int minutes = date.getTimeZone().getOffset(date.getTimeInMillis()) / 60_000;
            if (minutes == 0) {
                buffer.append('Z');
                return;
            } else if (minutes > 0) {
                buffer.append('+');
            } else {
                minutes = -minutes;
                buffer.append('-');
            }
            int hours = minutes / 60;
            minutes %= 60;
            buffer.append(hours / 10).append(hours % 10);
            if (size >= 3) {
                buffer.append(':');
            }
            if (size > 1) {
                buffer.append(minutes / 10).append(minutes % 10);
            }
        }

        @Override
        public void parse(String text, TCalendar date, TParsePosition position) {
            int index = position.getIndex();
            char signChar = text.charAt(index++);
            int sign;
            if (signChar == '+') {
                sign = 1;
            } else if (signChar == '-') {
                sign = -1;
            } else if (signChar == 'Z') {
                date.setTimeZone(TTimeZone.getTimeZone("GMT"));
                return;
            } else {
                position.setErrorIndex(index);
                return;
            }

            int expectedSize = 2;
            if (size > 1) {
                expectedSize += 2;
            }
            if (size >= 3) {
                ++expectedSize;
            }
            if (index + expectedSize > text.length()) {
                position.setErrorIndex(index);
                return;
            }

            if (!Character.isDigit(text.charAt(index)) || !Character.isDigit(text.charAt(index + 1))) {
                position.setErrorIndex(index);
                return;
            }
            int hours = Character.digit(text.charAt(index++), 10) * 10 + Character.digit(text.charAt(index++), 10);
            if (size >= 3) {
                if (text.charAt(index++) != ':') {
                    position.setErrorIndex(index);
                    return;
                }
            }
            int minutes = 0;
            if (size > 1) {
                if (!Character.isDigit(text.charAt(index)) || !Character.isDigit(text.charAt(index + 1))) {
                    position.setErrorIndex(index);
                    return;
                }
                minutes = Character.digit(text.charAt(index++), 10) * 10 + Character.digit(text.charAt(index++), 10);
            }
            position.setIndex(index);
            date.setTimeZone(getStaticTimeZone(sign * hours, minutes));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Iso8601Timezone that = (Iso8601Timezone) o;
            return size == that.size;
        }

        @Override
        public int hashCode() {
            return Objects.hash(size);
        }
    }

    static boolean tryParseFixedTimeZone(String text, TCalendar date, TParsePosition position) {
        general: if (position.getIndex() + 4 < text.length()) {
            int signIndex = position.getIndex() + 3;
            if (!text.substring(position.getIndex(), signIndex).equals("GMT")) {
                break general;
            }
            char signChar = text.charAt(signIndex);
            if (signChar != '+' && signChar != '-') {
                break general;
            }
            parseHoursMinutes(text, date, position);
            return true;
        }
        rfc822: if (position.getIndex() + 5 <= text.length()) {
            int index = position.getIndex();
            char signChar = text.charAt(index++);
            if (signChar != '+' && signChar != '-') {
                break rfc822;
            }
            for (int i = 0; i < 4; ++i) {
                if (!Character.isDigit(text.charAt(index + i))) {
                    break rfc822;
                }
            }
            int sign = signChar == '-' ? -1 : 1;
            int hours = 10 * Character.digit(text.charAt(index), 10) + Character.digit(text.charAt(index + 1), 10);
            int minutes = 10 * Character.digit(text.charAt(index + 2), 10)
                    + Character.digit(text.charAt(index + 3), 10);
            date.setTimeZone(getStaticTimeZone(sign * hours, minutes));
            return true;
        }
        return false;
    }

    static void parseHoursMinutes(String text, TCalendar date, TParsePosition position) {
        int index = position.getIndex() + 3;
        int sign = text.charAt(index++) == '-' ? -1 : 1;
        if (index >= text.length() || !Character.isDigit(text.charAt(index))) {
            position.setErrorIndex(index);
            return;
        }
        int hours = Character.digit(text.charAt(index++), 10);
        if (index >= text.length()) {
            position.setErrorIndex(index);
            return;
        }
        if (text.charAt(index) != ':') {
            if (!Character.isDigit(text.charAt(index))) {
                position.setErrorIndex(index);
                return;
            }
            hours = 10 * hours + Character.digit(text.charAt(index), 10);
        }
        if (index >= text.length() || text.charAt(index) != ':') {
            position.setErrorIndex(index);
            return;
        }
        ++index;

        if (index + 2 > text.length() || !Character.isDigit(text.charAt(index))
                || !Character.isDigit(text.charAt(index + 1))) {
            position.setErrorIndex(index);
            return;
        }
        int minutes = Character.digit(text.charAt(index), 10) * 10 + Character.digit(text.charAt(index), 10);
        position.setIndex(index + 2);
        TTimeZone tz = getStaticTimeZone(sign * hours, minutes);
        date.setTimeZone(tz);
    }

    static TTimeZone getStaticTimeZone(int hours, int minutes) {
        return TTimeZone.getTimeZone("GMT" + (hours > 0 ? '+' : '-') + Math.abs(hours)
                + ":" + (minutes / 10) + (minutes % 10));
    }

    static class TrieNode {
        char[] chars;
        TrieNode[] childNodes;
        TTimeZone tz;
    }

    static class TrieBuilder {
        TrieNodeBuilder root = new TrieNodeBuilder();

        public void add(String text, TTimeZone tz) {
            TrieNodeBuilder node = root;
            for (int i = 0; i < text.length(); ++i) {
                char c = Character.toLowerCase(text.charAt(i));
                while (node.ch != c) {
                    if (node.ch == '\0') {
                        node.ch = c;
                        node.sibling = new TrieNodeBuilder();
                        break;
                    }
                    node = node.sibling;
                }
                if (node.next == null) {
                    node.next = new TrieNodeBuilder();
                }
                node = node.next;
            }
            node.tz = tz;
        }

        public TrieNode build() {
            return build(root);
        }

        TrieNode build(TrieNodeBuilder builder) {
            TrieNode node = new TrieNode();
            if (builder == null) {
                return node;
            }
            node.tz = builder.tz;
            List<TrieNodeBuilder> builders = new ArrayList<>();
            TrieNodeBuilder tmp = builder;
            while (tmp.ch != '\0') {
                builders.add(tmp);
                tmp = tmp.sibling;
            }
            Collections.sort(builders, (o1, o2) -> Character.compare(o1.ch, o2.ch));
            node.chars = new char[builders.size()];
            node.childNodes = new TrieNode[builders.size()];
            for (int i = 0; i < node.chars.length; ++i) {
                node.chars[i] = builders.get(i).ch;
                node.childNodes[i] = build(builders.get(i).next);
            }
            return node;
        }
    }

    static class TrieNodeBuilder {
        char ch;
        TrieNodeBuilder next;
        TTimeZone tz;
        TrieNodeBuilder sibling;
    }
}
