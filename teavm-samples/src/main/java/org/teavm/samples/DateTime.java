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

import java.util.Date;
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
    private static Date currentDate;

    public static void main(String[] args) {
        fillLocales();
        window.setInterval(new TimerHandler() {
            @Override
            public void onTimer() {
                updateCurrentTime();
            }
        }, 250);
    }

    private static void fillLocales() {
        final HTMLSelectElement localeElem = (HTMLSelectElement)document.getElementById("locale");
        for (Locale locale : Locale.getAvailableLocales()) {
            HTMLOptionElement option = (HTMLOptionElement)document.createElement("option");
            option.setValue(locale.toString());
            option.setLabel(locale.getDisplayName(Locale.getDefault()));
            localeElem.getOptions().add(option);
        }
        localeElem.addEventListener("change", new EventListener() {
            @Override public void handleEvent(Event evt) {
                // Don't do anything
            }
        });
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
        timeElem.setValue(currentDate.toString());
    }
}
