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

import java.util.function.Consumer;
import org.teavm.jso.JSProperty;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.xml.Document;
import org.teavm.jso.dom.xml.NodeList;

public interface HTMLDocument extends Document, EventTarget {
    @JSProperty
    @Override
    HTMLHtmlElement getDocumentElement();

    @Override
    HTMLElement createElement(String tagName);

    default HTMLElement createElement(String tagName, Consumer<HTMLElement> consumer) {
        HTMLElement result = createElement(tagName);
        consumer.accept(result);
        return result;
    }

    @Override
    HTMLElement getElementById(String elementId);

    @JSProperty
    HTMLBodyElement getBody();

    @JSProperty
    HTMLElement getHead();

    @JSProperty
    int getScrollLeft();

    @JSProperty
    int getScrollTop();

    static HTMLDocument current() {
        return Window.current().getDocument();
    }

    @Override
    HTMLElement querySelector(String selectors);

    @Override
    NodeList<? extends HTMLElement> querySelectorAll(String selectors);
}
