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
import org.teavm.jso.dom.css.ElementCSSInlineStyle;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.events.FocusEventTarget;
import org.teavm.jso.dom.events.KeyboardEventTarget;
import org.teavm.jso.dom.events.LoadEventTarget;
import org.teavm.jso.dom.events.MouseEventTarget;
import org.teavm.jso.dom.events.TouchEventTarget;
import org.teavm.jso.dom.events.WheelEventTarget;
import org.teavm.jso.dom.types.DOMTokenList;
import org.teavm.jso.dom.xml.Element;
import org.teavm.jso.dom.xml.Node;
import org.teavm.jso.dom.xml.NodeList;
import org.teavm.jso.popover.ToggleEventTarget;

public abstract class HTMLElement implements Element, ElementCSSInlineStyle, EventTarget, FocusEventTarget,
        MouseEventTarget, WheelEventTarget, KeyboardEventTarget, LoadEventTarget, TouchEventTarget, ToggleEventTarget {
    @Override
    public abstract NodeList<? extends HTMLElement> getElementsByTagName(String name);

    @JSProperty
    public abstract String getTitle();

    @JSProperty
    public abstract void setTitle(String title);

    @JSProperty
    public abstract String getLang();

    @JSProperty
    public abstract void setLang(String lang);

    @JSProperty
    public abstract boolean isTranslate();

    @JSProperty
    public abstract void setTranslate(boolean translate);

    @JSProperty
    public abstract String getDir();

    @JSProperty
    public abstract void setDir(String dir);

    @JSProperty
    public abstract boolean isHidden();

    @JSProperty
    public abstract void setHidden(boolean hidden);

    public abstract void click();

    @JSProperty
    public abstract int getTabIndex();

    @JSProperty
    public abstract void setTabIndex(int tabIndex);

    public abstract void focus();

    public abstract void blur();

    @JSProperty
    public abstract String getAccessKey();

    @JSProperty
    public abstract void setAccessKey(String accessKey);

    @JSProperty
    public abstract String getAccessKeyLabel();

    @JSProperty
    public abstract int getClientWidth();

    @JSProperty
    public abstract int getClientHeight();

    @JSProperty
    public abstract int getAbsoluteLeft();

    @JSProperty
    public abstract int getAbsoluteTop();

    @JSProperty
    public abstract int getScrollLeft();

    @JSProperty
    public abstract void setScrollLeft(int scrollLeft);

    @JSProperty
    public abstract int getScrollTop();

    @JSProperty
    public abstract void setScrollTop(int scrollTop);

    @JSProperty
    public abstract int getScrollWidth();

    @JSProperty
    public abstract int getScrollHeight();

    @JSProperty
    public abstract int getOffsetWidth();

    @JSProperty
    public abstract int getOffsetHeight();

    @JSProperty
    public abstract int getOffsetTop();

    @JSProperty
    public abstract int getOffsetLeft();

    @JSProperty
    @Override
    public abstract HTMLDocument getOwnerDocument();

    @JSProperty
    public abstract HTMLCollection getChildren();

    @JSProperty
    public abstract String getInnerHTML();

    @JSProperty
    public abstract void setInnerHTML(String content);

    @JSProperty
    public abstract String getInnerText();

    @JSProperty
    public abstract void setInnerText(String content);

    public abstract TextRectangle getBoundingClientRect();

    @JSProperty
    public abstract String getClassName();

    @JSProperty
    public abstract void setClassName(String className);

    @JSProperty
    public abstract DOMTokenList getClassList();

    @JSProperty
    public abstract String getPopover();

    @JSProperty
    public abstract void setPopover(String popover);

    public abstract void hidePopover();

    public abstract void showPopover();

    public abstract boolean togglePopover();

    public abstract boolean togglePopover(boolean force);

    public final HTMLElement withAttr(String name, String value) {
        setAttribute(name, value);
        return this;
    }

    public final HTMLElement withChild(String tagName) {
        HTMLElement result = getOwnerDocument().createElement(tagName);
        appendChild(result);
        return this;
    }

    public final HTMLElement withChild(Node node) {
        appendChild(node);
        return this;
    }

    public final HTMLElement withChild(String tagName, Consumer<HTMLElement> consumer) {
        HTMLElement result = getOwnerDocument().createElement(tagName);
        appendChild(result);
        consumer.accept(result);
        return this;
    }

    public final HTMLElement clear() {
        Node node = getLastChild();
        while (node != null) {
            Node prev = node.getPreviousSibling();
            if (node.getNodeType() != ATTRIBUTE_NODE) {
                removeChild(node);
            }
            node = prev;
        }
        return this;
    }

    public final HTMLElement withText(String content) {
        clear().appendChild(getOwnerDocument().createTextNode(content));
        return this;
    }

    @Override
    public abstract HTMLElement querySelector(String selectors);

    @Override
    public abstract NodeList<? extends HTMLElement> querySelectorAll(String selectors);

    public abstract void requestPointerLock();
}
