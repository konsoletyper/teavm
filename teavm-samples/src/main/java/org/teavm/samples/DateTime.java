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
package org.teavm.samples;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import org.teavm.dom.browser.TimerHandler;
import org.teavm.dom.browser.Window;
import org.teavm.dom.events.Event;
import org.teavm.dom.events.EventListener;
import org.teavm.dom.html.HTMLDocument;
import org.teavm.dom.html.HTMLInputElement;
import org.teavm.dom.html.HTMLOptionElement;
import org.teavm.dom.html.HTMLSelectElement;
import org.teavm.jso.JS;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class DateTime {
    private static Window window = (Window)JS.getGlobal();
    private static HTMLDocument document = window.getDocument();
    private static HTMLSelectElement localeElem = (HTMLSelectElement)document.getElementById("locale");
    private static HTMLSelectElement fieldElem = (HTMLSelectElement)document.getElementById("field");
    private static Date currentDate = new Date();
    private static Locale[] locales;
    private static Locale currentLocale;
    private static int currentField;

    public static void main(String[] args) {
        fillLocales();
        bindFieldEvent();
        window.setInterval(new TimerHandler() {
            @Override
            public void onTimer() {
                updateCurrentTime();
            }
        }, 250);
        updateCurrentLocale();
        updateCurrentField();
    }

    private static void fillLocales() {
        locales = Locale.getAvailableLocales();
        for (Locale locale : locales) {
            HTMLOptionElement option = (HTMLOptionElement)document.createElement("option");
            option.setValue(locale.toString());
            option.setLabel(locale.getDisplayName(Locale.getDefault()));
            localeElem.getOptions().add(option);
        }
        localeElem.addEventListener("change", new EventListener() {
            @Override public void handleEvent(Event evt) {
                updateCurrentLocale();
                updateCurrentTimeText();
            }
        });
    }

    private static void bindFieldEvent() {
        fieldElem.addEventListener("change", new EventListener() {
            @Override public void handleEvent(Event evt) {
                updateCurrentField();
            }
        });
    }

    private static void updateCurrentLocale() {
        currentLocale = locales[localeElem.getSelectedIndex()];
        GregorianCalendar calendar = new GregorianCalendar(currentLocale);
        HTMLInputElement weekStartElem = (HTMLInputElement)document.getElementById("week-start");
        weekStartElem.setValue(String.valueOf(calendar.getFirstDayOfWeek()));
        HTMLInputElement weekLengthElem = (HTMLInputElement)document.getElementById("week-length");
        weekLengthElem.setValue(String.valueOf(calendar.getMinimalDaysInFirstWeek()));
    }

    private static void updateCurrentTime() {
        setCurrentTime(new Date());
    }

    private static void setCurrentTime(Date date) {
        currentDate = date;
        updateCurrentTimeText();
        updateFieldText();
    }

    private static void updateCurrentTimeText() {
        HTMLInputElement timeElem = (HTMLInputElement)document.getElementById("current-time");
        timeElem.setValue(currentDate.toString());
    }

    private static void updateCurrentField() {
        switch (fieldElem.getValue()) {
            case "era":
                currentField = Calendar.ERA;
                break;
            case "year":
                currentField = Calendar.YEAR;
                break;
            case "month":
                currentField = Calendar.MONTH;
                break;
            case "week-of-year":
                currentField = Calendar.WEEK_OF_YEAR;
                break;
            case "week-of-month":
                currentField = Calendar.WEEK_OF_MONTH;
                break;
            case "date":
                currentField = Calendar.DATE;
                break;
            case "day-of-year":
                currentField = Calendar.DAY_OF_YEAR;
                break;
            case "day-of-week":
                currentField = Calendar.DAY_OF_WEEK;
                break;
            case "am-pm":
                currentField = Calendar.AM_PM;
                break;
            case "hour":
                currentField = Calendar.HOUR;
                break;
            case "hour-of-day":
                currentField = Calendar.HOUR_OF_DAY;
                break;
            case "minute":
                currentField = Calendar.MINUTE;
                break;
            case "second":
                currentField = Calendar.SECOND;
                break;
            case "zone-offset":
                currentField = Calendar.ZONE_OFFSET;
                break;
        }
        updateFieldText();
    }

    private static void updateFieldText() {
        HTMLInputElement fieldValueElem = (HTMLInputElement)document.getElementById("field-value");
        Calendar calendar = new GregorianCalendar(currentLocale);
        calendar.setTime(currentDate);
        int value = calendar.get(currentField);
        fieldValueElem.setValue(String.valueOf(value));

        DateFormatSymbols symbols = new DateFormatSymbols(currentLocale);
        String text;
        switch (currentField) {
            case Calendar.ERA:
                text = symbols.getEras()[value];
                break;
            case Calendar.AM_PM:
                text = symbols.getAmPmStrings()[value];
                break;
            case Calendar.MONTH:
                text = symbols.getMonths()[value] + "/" + symbols.getShortMonths()[value];
                break;
            case Calendar.DAY_OF_WEEK:
                text = symbols.getShortWeekdays()[value - 1];
                break;
            default:
                text = "";
                break;
        }
        HTMLInputElement fieldTextElem = (HTMLInputElement)document.getElementById("field-value-text");
        fieldTextElem.setValue(text);
    }
}
