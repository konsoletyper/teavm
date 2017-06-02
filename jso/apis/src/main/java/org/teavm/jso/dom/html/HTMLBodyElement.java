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
package org.teavm.jso.dom.html;

import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;

/**
 * @author Alexey Andreev
 */
public interface HTMLBodyElement extends HTMLElement {
    @JSProperty("onerror")
    void setOnError(EventListener<Event> listener);

    @JSProperty("onload")
    void setOnLoad(EventListener<Event> listener);

    @JSProperty("onmessage")
    void setOnMessage(EventListener<Event> listener);

    @JSProperty("onoffline")
    void setOnOffline(EventListener<Event> listener);

    @JSProperty("ononline")
    void setOnOnline(EventListener<Event> listener);

    @JSProperty("onafterprint")
    void setOnAfterPrint(EventListener<Event> listener);

    @JSProperty("onbeforeprint")
    void setOnBeforePrint(EventListener<Event> listener);

    @JSProperty("onbeforeunload")
    void setOnBeforeUnload(EventListener<Event> listener);

    @JSProperty("onhashchange")
    void setOnHashChange(EventListener<Event> listener);

    @JSProperty("onlanguagechange")
    void setOnLanguageChange(EventListener<Event> listener);

    @JSProperty("onpopstate")
    void setOnPopState(EventListener<Event> listener);

    @JSProperty("onstorage")
    void setOnStorage(EventListener<Event> listener);

    @JSProperty("onunload")
    void setOnUnload(EventListener<Event> listener);

    @JSProperty("onafterprint")
    EventListener<Event> getOnAfterPrint();

    @JSProperty("onbeforeprint")
    EventListener<Event> getOnBeforePrint();

    @JSProperty("onbeforeunload")
    EventListener<Event> getOnBeforeUnload();

    @JSProperty("onhashchange")
    EventListener<Event> getOnHashChange();

    @JSProperty("onlanguagechange")
    EventListener<Event> getOnLanguageChange();

    @JSProperty("onpopstate")
    EventListener<Event> getOnPopState();

    @JSProperty("onstorage")
    EventListener<Event> getOnStorage();

    @JSProperty("onunload")
    EventListener<Event> getOnUnload();
}

