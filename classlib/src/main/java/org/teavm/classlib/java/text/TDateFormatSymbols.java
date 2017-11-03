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
import java.util.Objects;
import org.teavm.classlib.impl.unicode.CLDRHelper;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TCloneable;
import org.teavm.classlib.java.util.TLocale;

public class TDateFormatSymbols implements TSerializable, TCloneable {
    private TLocale locale;
    private String localPatternChars;
    String[] ampms;
    String[] eras;
    String[] months;
    String[] shortMonths;
    String[] shortWeekdays;
    String[] weekdays;
    String[][] zoneStrings;


    public TDateFormatSymbols() {
        this(TLocale.getDefault());
    }

    public TDateFormatSymbols(TLocale locale) {
        this.locale = locale;
    }

    @Override
    public Object clone() {
        TDateFormatSymbols symbols = new TDateFormatSymbols(locale);
        if (ampms != null) {
            symbols.ampms = Arrays.copyOf(ampms, ampms.length);
        }
        if (eras != null) {
            symbols.eras = Arrays.copyOf(eras, eras.length);
        }
        if (months != null) {
            symbols.months = Arrays.copyOf(months, months.length);
        }
        if (shortMonths != null) {
            symbols.shortMonths = Arrays.copyOf(shortMonths, shortMonths.length);
        }
        if (shortWeekdays != null) {
            symbols.shortWeekdays = Arrays.copyOf(shortWeekdays.clone(), shortWeekdays.length);
        }
        if (weekdays != null) {
            symbols.weekdays = Arrays.copyOf(weekdays, weekdays.length);
        }
        if (zoneStrings != null) {
            symbols.zoneStrings = new String[zoneStrings.length][];
            for (int i = 0; i < zoneStrings.length; i++) {
                symbols.zoneStrings[i] = Arrays.copyOf(zoneStrings[i], zoneStrings[i].length);
            }
        }
        return symbols;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TDateFormatSymbols)) {
            return false;
        }

        TDateFormatSymbols obj = (TDateFormatSymbols) object;
        if (!locale.equals(obj.locale)) {
            return false;
        }
        if (!Objects.equals(localPatternChars, obj.localPatternChars)) {
            return false;
        }
        if (!Arrays.equals(ampms, obj.ampms)) {
            return false;
        }
        if (!Arrays.equals(eras, obj.eras)) {
            return false;
        }
        if (!Arrays.equals(months, obj.months)) {
            return false;
        }
        if (!Arrays.equals(shortMonths, obj.shortMonths)) {
            return false;
        }
        if (!Arrays.equals(shortWeekdays, obj.shortWeekdays)) {
            return false;
        }
        if (!Arrays.equals(weekdays, obj.weekdays)) {
            return false;
        }
        return Arrays.equals(zoneStrings, obj.zoneStrings);
    }

    public String[] getAmPmStrings() {
        if (ampms == null) {
            ampms = CLDRHelper.resolveAmPm(locale.getLanguage(), locale.getCountry());
        }
        return ampms.clone();
    }

    public String[] getEras() {
        if (eras == null) {
            eras = CLDRHelper.resolveEras(locale.getLanguage(), locale.getCountry());
        }
        return eras.clone();
    }

    public String getLocalPatternChars() {
        if (localPatternChars == null) {
            localPatternChars = "";
        }
        return localPatternChars;
    }

    public String[] getMonths() {
        if (months == null) {
            months = CLDRHelper.resolveMonths(locale.getLanguage(), locale.getCountry());
        }
        return months.clone();
    }

    public String[] getShortMonths() {
        if (shortMonths == null) {
            shortMonths = CLDRHelper.resolveShortMonths(locale.getLanguage(), locale.getCountry());
        }
        return shortMonths.clone();
    }

    public String[] getShortWeekdays() {
        if (shortWeekdays == null) {
            shortWeekdays = CLDRHelper.resolveShortWeekdays(locale.getLanguage(), locale.getCountry());
        }
        return shortWeekdays.clone();
    }

    public String[] getWeekdays() {
        if (weekdays == null) {
            weekdays = CLDRHelper.resolveWeekdays(locale.getLanguage(), locale.getCountry());
        }
        return weekdays.clone();
    }

    public String[][] getZoneStrings() {
        if (zoneStrings == null) {
            return new String[0][];
        }
        String[][] clone = new String[zoneStrings.length][];
        for (int i = zoneStrings.length; --i >= 0;) {
            clone[i] = zoneStrings[i].clone();
        }
        return clone;
    }

    @Override
    public int hashCode() {
        int hashCode;
        hashCode = localPatternChars.hashCode();
        for (String element : ampms) {
            hashCode += element.hashCode();
        }
        for (String element : eras) {
            hashCode += element.hashCode();
        }
        for (String element : months) {
            hashCode += element.hashCode();
        }
        for (String element : shortMonths) {
            hashCode += element.hashCode();
        }
        for (String element : shortWeekdays) {
            hashCode += element.hashCode();
        }
        for (String element : weekdays) {
            hashCode += element.hashCode();
        }
        for (String[] element : zoneStrings) {
            for (int j = 0; j < element.length; j++) {
                if (element[j] != null) {
                    hashCode += element[j].hashCode();
                }
            }
        }
        return hashCode;
    }

    public void setAmPmStrings(String[] data) {
        ampms = data.clone();
    }

    public void setEras(String[] data) {
        eras = data.clone();
    }

    public void setLocalPatternChars(String data) {
        if (data == null) {
            throw new NullPointerException();
        }
        localPatternChars = data;
    }

    public void setMonths(String[] data) {
        months = data.clone();
    }

    public void setShortMonths(String[] data) {
        shortMonths = data.clone();
    }

    public void setShortWeekdays(String[] data) {
        shortWeekdays = data.clone();
    }

    public void setWeekdays(String[] data) {
        weekdays = data.clone();
    }

    public void setZoneStrings(String[][] data) {
        zoneStrings = data.clone();
    }
}
