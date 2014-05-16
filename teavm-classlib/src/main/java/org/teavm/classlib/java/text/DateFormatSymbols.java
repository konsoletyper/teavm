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

import java.util.Arrays;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.util.TLocale;

public class DateFormatSymbols implements TSerializable, Cloneable {

    private static final long serialVersionUID = -5987973545549424702L;

    private String localPatternChars;

    String[] ampms, eras, months, shortMonths, shortWeekdays, weekdays;

    String[][] zoneStrings;

    transient private com.ibm.icu.text.DateFormatSymbols icuSymbols;

    public DateFormatSymbols() {
        this(TLocale.getDefault());
    }

    public DateFormatSymbols(TLocale locale) {
        this(locale, new com.ibm.icu.text.DateFormatSymbols(locale));
    }

    DateFormatSymbols(TLocale locale, com.ibm.icu.text.DateFormatSymbols icuSymbols) {
        this.icuSymbols = icuSymbols;
        localPatternChars = icuSymbols.getLocalPatternChars();
        ampms = icuSymbols.getAmPmStrings();
        eras = icuSymbols.getEras();
        months = icuSymbols.getMonths();
        shortMonths = icuSymbols.getShortMonths();
        shortWeekdays = icuSymbols.getShortWeekdays();
        weekdays = icuSymbols.getWeekdays();
    }

    @Override
    public Object clone() {
        if (zoneStrings == null) {
            zoneStrings = icuSymbols.getZoneStrings();
        }
        try {
            DateFormatSymbols symbols = (DateFormatSymbols) super.clone();
            symbols.ampms = ampms.clone();
            symbols.eras = eras.clone();
            symbols.months = months.clone();
            symbols.shortMonths = shortMonths.clone();
            symbols.shortWeekdays = shortWeekdays.clone();
            symbols.weekdays = weekdays.clone();
            symbols.zoneStrings = new String[zoneStrings.length][];
            for (int i = 0; i < zoneStrings.length; i++) {
                symbols.zoneStrings[i] = zoneStrings[i].clone();
            }
            return symbols;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DateFormatSymbols)) {
            return false;
        }
        if (zoneStrings == null) {
            zoneStrings = icuSymbols.getZoneStrings();
        }

        DateFormatSymbols obj = (DateFormatSymbols) object;

        if (obj.zoneStrings == null) {
            obj.zoneStrings = obj.icuSymbols.getZoneStrings();
        }
        if (!localPatternChars.equals(obj.localPatternChars)) {
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
        if (zoneStrings.length != obj.zoneStrings.length) {
            return false;
        }
        for (String[] element : zoneStrings) {
            if (element.length != element.length) {
                return false;
            }
            for (int j = 0; j < element.length; j++) {
                if (element[j] != element[j] && !(element[j].equals(element[j]))) {
                    return false;
                }
            }
        }
        return true;
    }

    public String[] getAmPmStrings() {
        return ampms.clone();
    }

    public String[] getEras() {
        return eras.clone();
    }

    public String getLocalPatternChars() {
        return localPatternChars;
    }

    public String[] getMonths() {
        return months.clone();
    }

    public String[] getShortMonths() {
        return shortMonths.clone();
    }

    public String[] getShortWeekdays() {
        return shortWeekdays.clone();
    }

    public String[] getWeekdays() {
        return weekdays.clone();
    }

    public String[][] getZoneStrings() {
        if (zoneStrings == null) {
            zoneStrings = icuSymbols.getZoneStrings();
        }
        String[][] clone = new String[zoneStrings.length][];
        for (int i = zoneStrings.length; --i >= 0;) {
            clone[i] = zoneStrings[i].clone();
        }
        return clone;
    }

    @Override
    public int hashCode() {
        if (zoneStrings == null) {
            zoneStrings = icuSymbols.getZoneStrings();
        }
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
