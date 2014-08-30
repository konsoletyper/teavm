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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
public final class DateTime {
    private DateTime() {
    }

    private static Window window = (Window)JS.getGlobal();
    private static HTMLDocument document = window.getDocument();
    private static HTMLSelectElement localeElem = (HTMLSelectElement)document.getElementById("locale");
    private static HTMLSelectElement formatElem = (HTMLSelectElement)document.getElementById("format");
    private static HTMLSelectElement customFormatElem = (HTMLSelectElement)document.getElementById("custom-format");
    private static Date currentDate = new Date();
    private static Locale[] locales;
    private static Locale currentLocale;
    private static DateFormat dateFormat;

    public static void main(String[] args) {
        fillLocales();
        window.setInterval(new TimerHandler() {
            @Override public void onTimer() {
                updateCurrentTime();
            }
        }, 250);
        updateCurrentLocale();
        updateFormat();
        formatElem.addEventListener("change", new EventListener() {
            @Override public void handleEvent(Event evt) {
                updateFormat();
                updateCurrentTimeText();
            }
        });
        customFormatElem.addEventListener("change", new EventListener() {
            @Override public void handleEvent(Event evt) {
                updateFormat();
                updateCurrentTimeText();
            }
        });
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
                updateFormat();
                updateCurrentTimeText();
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
    }

    private static void updateCurrentTimeText() {
        HTMLInputElement timeElem = (HTMLInputElement)document.getElementById("current-time");
        try {
            timeElem.setValue(dateFormat.format(currentDate));
        } catch (RuntimeException e) {
            timeElem.setValue("Error formatting date");
        }
    }

    private static void updateFormat() {
        customFormatElem.setDisabled(true);
        switch (formatElem.getValue()) {
            case "short-date":
                dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, currentLocale);
                break;
            case "medium-date":
                dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, currentLocale);
                break;
            case "long-date":
                dateFormat = DateFormat.getDateInstance(DateFormat.LONG, currentLocale);
                break;
            case "full-date":
                dateFormat = DateFormat.getDateInstance(DateFormat.FULL, currentLocale);
                break;
            case "short-time":
                dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT, currentLocale);
                break;
            case "medium-time":
                dateFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM, currentLocale);
                break;
            case "long-time":
                dateFormat = DateFormat.getTimeInstance(DateFormat.LONG, currentLocale);
                break;
            case "full-time":
                dateFormat = DateFormat.getTimeInstance(DateFormat.FULL, currentLocale);
                break;
            case "short-datetime":
                dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, currentLocale);
                break;
            case "medium-datetime":
                dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, currentLocale);
                break;
            case "long-datetime":
                dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, currentLocale);
                break;
            case "full-datetime":
                dateFormat = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, currentLocale);
                break;
            case "custom":
                customFormatElem.setDisabled(false);
                try {
                    dateFormat = new SimpleDateFormat(customFormatElem.getValue(), currentLocale);
                } catch (IllegalArgumentException e) {
                    dateFormat = new SimpleDateFormat("'Invalid pattern'");
                }
                break;
        }
    }
}
