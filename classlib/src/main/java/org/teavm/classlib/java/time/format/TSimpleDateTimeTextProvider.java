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
package org.teavm.classlib.java.time.format;

import static org.teavm.classlib.java.time.temporal.TChronoField.AMPM_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;
import static org.teavm.classlib.java.time.temporal.TChronoField.ERA;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;

import java.text.DateFormatSymbols;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import org.teavm.classlib.java.util.TCalendar;
import java.util.Collections;
import java.util.Comparator;
import org.teavm.classlib.java.util.TGregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.teavm.classlib.java.util.TLocale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.teavm.classlib.java.time.temporal.TIsoFields;
import org.teavm.classlib.java.time.temporal.TTemporalField;

final class TSimpleDateTimeTextProvider extends TDateTimeTextProvider {
     // TODO: Better implementation based on CLDR

    private static final Comparator<Entry<String, Long>> COMPARATOR = new Comparator<Entry<String, Long>>() {
        @Override
        public int compare(Entry<String, Long> obj1, Entry<String, Long> obj2) {
            return obj2.getKey().length() - obj1.getKey().length();  // longest to shortest
        }
    };

    private final ConcurrentMap<Entry<TTemporalField, TLocale>, Object> cache =
            new ConcurrentHashMap<Entry<TTemporalField, TLocale>, Object>(16, 0.75f, 2);

    //-----------------------------------------------------------------------
    @Override
    public String getText(TTemporalField field, long value, TTextStyle style, TLocale locale) {
        Object store = findStore(field, locale);
        if (store instanceof LocaleStore) {
            return ((LocaleStore) store).getText(value, style);
        }
        return null;
    }

    @Override
    public Iterator<Entry<String, Long>> getTextIterator(TTemporalField field, TTextStyle style, TLocale locale) {
        Object store = findStore(field, locale);
        if (store instanceof LocaleStore) {
            return ((LocaleStore) store).getTextIterator(style);
        }
        return null;
    }

    //-----------------------------------------------------------------------
    private Object findStore(TTemporalField field, TLocale locale) {
        Entry<TTemporalField, TLocale> key = createEntry(field, locale);
        Object store = cache.get(key);
        if (store == null) {
            store = createStore(field, locale);
            cache.putIfAbsent(key, store);
            store = cache.get(key);
        }
        return store;
    }

    private Object createStore(TTemporalField field, TLocale locale) {
        if (field == MONTH_OF_YEAR) {
            DateFormatSymbols oldSymbols = DateFormatSymbols.getInstance(locale);
            Map<TTextStyle, Map<Long, String>> styleMap = new HashMap<TTextStyle, Map<Long,String>>();
            Long f1 = 1L;
            Long f2 = 2L;
            Long f3 = 3L;
            Long f4 = 4L;
            Long f5 = 5L;
            Long f6 = 6L;
            Long f7 = 7L;
            Long f8 = 8L;
            Long f9 = 9L;
            Long f10 = 10L;
            Long f11 = 11L;
            Long f12 = 12L;
            String[] array = oldSymbols.getMonths();
            Map<Long, String> map = new HashMap<Long, String>();
            map.put(f1, array[TCalendar.JANUARY]);
            map.put(f2, array[TCalendar.FEBRUARY]);
            map.put(f3, array[TCalendar.MARCH]);
            map.put(f4, array[TCalendar.APRIL]);
            map.put(f5, array[TCalendar.MAY]);
            map.put(f6, array[TCalendar.JUNE]);
            map.put(f7, array[TCalendar.JULY]);
            map.put(f8, array[TCalendar.AUGUST]);
            map.put(f9, array[TCalendar.SEPTEMBER]);
            map.put(f10, array[TCalendar.OCTOBER]);
            map.put(f11, array[TCalendar.NOVEMBER]);
            map.put(f12, array[TCalendar.DECEMBER]);
            styleMap.put(TTextStyle.FULL, map);
            
            map = new HashMap<Long, String>();
            map.put(f1, array[TCalendar.JANUARY].substring(0, 1));
            map.put(f2, array[TCalendar.FEBRUARY].substring(0, 1));
            map.put(f3, array[TCalendar.MARCH].substring(0, 1));
            map.put(f4, array[TCalendar.APRIL].substring(0, 1));
            map.put(f5, array[TCalendar.MAY].substring(0, 1));
            map.put(f6, array[TCalendar.JUNE].substring(0, 1));
            map.put(f7, array[TCalendar.JULY].substring(0, 1));
            map.put(f8, array[TCalendar.AUGUST].substring(0, 1));
            map.put(f9, array[TCalendar.SEPTEMBER].substring(0, 1));
            map.put(f10, array[TCalendar.OCTOBER].substring(0, 1));
            map.put(f11, array[TCalendar.NOVEMBER].substring(0, 1));
            map.put(f12, array[TCalendar.DECEMBER].substring(0, 1));
            styleMap.put(TTextStyle.NARROW, map);
            
            array = oldSymbols.getShortMonths();
            map = new HashMap<Long, String>();
            map.put(f1, array[TCalendar.JANUARY]);
            map.put(f2, array[TCalendar.FEBRUARY]);
            map.put(f3, array[TCalendar.MARCH]);
            map.put(f4, array[TCalendar.APRIL]);
            map.put(f5, array[TCalendar.MAY]);
            map.put(f6, array[TCalendar.JUNE]);
            map.put(f7, array[TCalendar.JULY]);
            map.put(f8, array[TCalendar.AUGUST]);
            map.put(f9, array[TCalendar.SEPTEMBER]);
            map.put(f10, array[TCalendar.OCTOBER]);
            map.put(f11, array[TCalendar.NOVEMBER]);
            map.put(f12, array[TCalendar.DECEMBER]);
            styleMap.put(TTextStyle.SHORT, map);
            return createLocaleStore(styleMap);
        }
        if (field == DAY_OF_WEEK) {
            DateFormatSymbols oldSymbols = DateFormatSymbols.getInstance(locale);
            Map<TTextStyle, Map<Long, String>> styleMap = new HashMap<TTextStyle, Map<Long,String>>();
            Long f1 = 1L;
            Long f2 = 2L;
            Long f3 = 3L;
            Long f4 = 4L;
            Long f5 = 5L;
            Long f6 = 6L;
            Long f7 = 7L;
            String[] array = oldSymbols.getWeekdays();
            Map<Long, String> map = new HashMap<Long, String>();
            map.put(f1, array[TCalendar.MONDAY]);
            map.put(f2, array[TCalendar.TUESDAY]);
            map.put(f3, array[TCalendar.WEDNESDAY]);
            map.put(f4, array[TCalendar.THURSDAY]);
            map.put(f5, array[TCalendar.FRIDAY]);
            map.put(f6, array[TCalendar.SATURDAY]);
            map.put(f7, array[TCalendar.SUNDAY]);
            styleMap.put(TTextStyle.FULL, map);
            
            map = new HashMap<Long, String>();
            map.put(f1, array[TCalendar.MONDAY].substring(0, 1));
            map.put(f2, array[TCalendar.TUESDAY].substring(0, 1));
            map.put(f3, array[TCalendar.WEDNESDAY].substring(0, 1));
            map.put(f4, array[TCalendar.THURSDAY].substring(0, 1));
            map.put(f5, array[TCalendar.FRIDAY].substring(0, 1));
            map.put(f6, array[TCalendar.SATURDAY].substring(0, 1));
            map.put(f7, array[TCalendar.SUNDAY].substring(0, 1));
            styleMap.put(TTextStyle.NARROW, map);
            
            array = oldSymbols.getShortWeekdays();
            map = new HashMap<Long, String>();
            map.put(f1, array[TCalendar.MONDAY]);
            map.put(f2, array[TCalendar.TUESDAY]);
            map.put(f3, array[TCalendar.WEDNESDAY]);
            map.put(f4, array[TCalendar.THURSDAY]);
            map.put(f5, array[TCalendar.FRIDAY]);
            map.put(f6, array[TCalendar.SATURDAY]);
            map.put(f7, array[TCalendar.SUNDAY]);
            styleMap.put(TTextStyle.SHORT, map);
            return createLocaleStore(styleMap);
        }
        if (field == AMPM_OF_DAY) {
            DateFormatSymbols oldSymbols = DateFormatSymbols.getInstance(locale);
            Map<TTextStyle, Map<Long, String>> styleMap = new HashMap<TTextStyle, Map<Long,String>>();
            String[] array = oldSymbols.getAmPmStrings();
            Map<Long, String> map = new HashMap<Long, String>();
            map.put(0L, array[TCalendar.AM]);
            map.put(1L, array[TCalendar.PM]);
            styleMap.put(TTextStyle.FULL, map);
            styleMap.put(TTextStyle.SHORT, map);  // re-use, as we don't have different data
            return createLocaleStore(styleMap);
        }
        if (field == ERA) {
            DateFormatSymbols oldSymbols = DateFormatSymbols.getInstance(locale);
            Map<TTextStyle, Map<Long, String>> styleMap = new HashMap<TTextStyle, Map<Long,String>>();
            String[] array = oldSymbols.getEras();
            Map<Long, String> map = new HashMap<Long, String>();
            map.put(0L, array[TGregorianCalendar.BC]);
            map.put(1L, array[TGregorianCalendar.AD]);
            styleMap.put(TTextStyle.SHORT, map);
            if (locale.getLanguage().equals(TLocale.ENGLISH.getLanguage())) {
                map = new HashMap<Long, String>();
                map.put(0L, "Before Christ");
                map.put(1L, "Anno Domini");
                styleMap.put(TTextStyle.FULL, map);
            } else {
                // re-use, as we don't have different data
                styleMap.put(TTextStyle.FULL, map);
            }
            map = new HashMap<Long, String>();
            map.put(0L, array[TGregorianCalendar.BC].substring(0, 1));
            map.put(1L, array[TGregorianCalendar.AD].substring(0, 1));
            styleMap.put(TTextStyle.NARROW, map);
            return createLocaleStore(styleMap);
        }
        // hard code English quarter text
        if (field == TIsoFields.QUARTER_OF_YEAR) {
            Map<TTextStyle, Map<Long, String>> styleMap = new HashMap<TTextStyle, Map<Long,String>>();
            Map<Long, String> map = new HashMap<Long, String>();
            map.put(1L, "Q1");
            map.put(2L, "Q2");
            map.put(3L, "Q3");
            map.put(4L, "Q4");
            styleMap.put(TTextStyle.SHORT, map);
            map = new HashMap<Long, String>();
            map.put(1L, "1st quarter");
            map.put(2L, "2nd quarter");
            map.put(3L, "3rd quarter");
            map.put(4L, "4th quarter");
            styleMap.put(TTextStyle.FULL, map);
            return createLocaleStore(styleMap);
        }
        return "";  // null marker for map
    }

    //-----------------------------------------------------------------------
    private static <A, B> Entry<A, B> createEntry(A text, B field) {
        return new SimpleImmutableEntry<A, B>(text, field);
    }

    //-----------------------------------------------------------------------
    private static LocaleStore createLocaleStore(Map<TTextStyle, Map<Long, String>> valueTextMap) {
        valueTextMap.put(TTextStyle.FULL_STANDALONE, valueTextMap.get(TTextStyle.FULL));
        valueTextMap.put(TTextStyle.SHORT_STANDALONE, valueTextMap.get(TTextStyle.SHORT));
        if (valueTextMap.containsKey(TTextStyle.NARROW) && valueTextMap.containsKey(TTextStyle.NARROW_STANDALONE) == false) {
            valueTextMap.put(TTextStyle.NARROW_STANDALONE, valueTextMap.get(TTextStyle.NARROW));
        }
        return new LocaleStore(valueTextMap);
    }

    static final class LocaleStore {
        private final Map<TTextStyle, Map<Long, String>> valueTextMap;
        private final Map<TTextStyle, List<Entry<String, Long>>> parsable;

        //-----------------------------------------------------------------------
        LocaleStore(Map<TTextStyle, Map<Long, String>> valueTextMap) {
            this.valueTextMap = valueTextMap;
            Map<TTextStyle, List<Entry<String, Long>>> map = new HashMap<TTextStyle, List<Entry<String,Long>>>();
            List<Entry<String, Long>> allList = new ArrayList<Map.Entry<String,Long>>();
            for (TTextStyle style : valueTextMap.keySet()) {
                Map<String, Entry<String, Long>> reverse = new HashMap<String, Map.Entry<String,Long>>();
                for (Map.Entry<Long, String> entry : valueTextMap.get(style).entrySet()) {
                    if (reverse.put(entry.getValue(), createEntry(entry.getValue(), entry.getKey())) != null) {
                        continue;  // not parsable, try next style
                    }
                }
                List<Entry<String, Long>> list = new ArrayList<Map.Entry<String,Long>>(reverse.values());
                Collections.sort(list, COMPARATOR);
                map.put(style, list);
                allList.addAll(list);
                map.put(null, allList);
            }
            Collections.sort(allList, COMPARATOR);
            this.parsable = map;
        }

        //-----------------------------------------------------------------------
        String getText(long value, TTextStyle style) {
            Map<Long, String> map = valueTextMap.get(style);
            return map != null ? map.get(value) : null;
        }

        Iterator<Entry<String, Long>> getTextIterator(TTextStyle style) {
            List<Entry<String, Long>> list = parsable.get(style);
            return list != null ? list.iterator() : null;
        }
    }

}
