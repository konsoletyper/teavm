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

public abstract class HTMLDocument implements Document, EventTarget {
    @JSProperty
    @Override
    public abstract HTMLHtmlElement getDocumentElement();

    @Override
    public abstract HTMLElement createElement(String tagName);

    public final HTMLElement createElement(String tagName, Consumer<HTMLElement> consumer) {
        HTMLElement result = createElement(tagName);
        consumer.accept(result);
        return result;
    }

    @Override
    public abstract HTMLElement getElementById(String elementId);

    @JSProperty
    public abstract HTMLBodyElement getBody();

    @JSProperty
    public abstract HTMLHeadElement getHead();

    @JSProperty
    public abstract int getScrollLeft();

    @JSProperty
    public abstract int getScrollTop();

    public static HTMLDocument current() {
        return Window.current().getDocument();
    }

    @Override
    public abstract HTMLElement querySelector(String selectors);

    @Override
    public abstract NodeList<? extends HTMLElement> querySelectorAll(String selectors);

    @JSProperty
    public abstract HTMLElement getActiveElement();

    public abstract HTMLElement elementFromPoint(int x, int y);

    @JSProperty
    public abstract boolean isDesignMode();

    @JSProperty
    public abstract void setDesignMode(boolean value);

    public abstract void execCommand(String commandName, boolean showDefaultUI, String valueArgument);

    public abstract void execCommand(String commandName);

    @JSProperty
    public abstract String getCookie();

    @JSProperty
    public abstract void setCookie(String cookie);

    @JSProperty
    public abstract String getTitle();

    @JSProperty
    public abstract void setTitle(String title);

    @JSProperty
    public abstract HTMLElement getPointerLockElement();

    public abstract void exitPointerLock();
}
