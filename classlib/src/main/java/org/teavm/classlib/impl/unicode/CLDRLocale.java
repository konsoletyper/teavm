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
package org.teavm.classlib.impl.unicode;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class CLDRLocale {
    final Map<String, String> languages = new LinkedHashMap<>();
    final Map<String, String> territories = new LinkedHashMap<>();
    final Map<String, CLDRCurrency> currencies = new LinkedHashMap<>();
    String[] eras;
    String[] dayPeriods;
    String[] months;
    String[] shortMonths;
    String[] weekdays;
    String[] shortWeekdays;
    CLDRDateFormats dateFormats;
    CLDRDateFormats timeFormats;
    CLDRDateFormats dateTimeFormats;
    CLDRTimeZone[] timeZones;
    CLDRDecimalData decimalData = new CLDRDecimalData();
    String numberFormat;
    String percentFormat;
    String currencyFormat;

    public Map<String, String> getLanguages() {
        return Collections.unmodifiableMap(languages);
    }

    public Map<String, String> getTerritories() {
        return Collections.unmodifiableMap(territories);
    }

    public Map<String, CLDRCurrency> getCurrencies() {
        return Collections.unmodifiableMap(currencies);
    }

    public String[] getEras() {
        return Arrays.copyOf(eras, eras.length);
    }

    public String[] getDayPeriods() {
        return Arrays.copyOf(dayPeriods, dayPeriods.length);
    }

    public String[] getMonths() {
        return Arrays.copyOf(months, months.length);
    }

    public String[] getShortMonths() {
        return Arrays.copyOf(shortMonths, shortMonths.length);
    }

    public String[] getWeekdays() {
        return Arrays.copyOf(weekdays, weekdays.length);
    }

    public String[] getShortWeekdays() {
        return Arrays.copyOf(shortWeekdays, shortWeekdays.length);
    }

    public CLDRDateFormats getDateFormats() {
        return dateFormats;
    }

    public CLDRDateFormats getTimeFormats() {
        return timeFormats;
    }

    public CLDRDateFormats getDateTimeFormats() {
        return dateTimeFormats;
    }

    public CLDRTimeZone[] getTimeZones() {
        return timeZones.clone();
    }

    public CLDRDecimalData getDecimalData() {
        return decimalData;
    }

    public String getNumberFormat() {
        return numberFormat;
    }

    public String getPercentFormat() {
        return percentFormat;
    }

    public String getCurrencyFormat() {
        return currencyFormat;
    }
}
