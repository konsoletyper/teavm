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
import org.teavm.jso.dom.events.WheelEventTarget;
import org.teavm.jso.dom.xml.Element;
import org.teavm.jso.dom.xml.Node;
import org.teavm.jso.dom.xml.NodeList;

public interface HTMLElement extends Element, ElementCSSInlineStyle, EventTarget, FocusEventTarget, MouseEventTarget,
        WheelEventTarget, KeyboardEventTarget, LoadEventTarget {
    @Override
    NodeList<? extends HTMLElement> getElementsByTagName(String name);

    @JSProperty
    String getTitle();

    @JSProperty
    void setTitle(String title);

    @JSProperty
    String getLang();

    @JSProperty
    void setLang(String lang);

    @JSProperty
    boolean isTranslate();

    @JSProperty
    void setTranslate(boolean translate);

    @JSProperty
    String getDir();

    @JSProperty
    void setDir(String dir);

    @JSProperty
    boolean isHidden();

    @JSProperty
    void setHidden(boolean hidden);

    void click();

    @JSProperty
    int getTabIndex();

    @JSProperty
    void setTabIndex(int tabIndex);

    void focus();

    void blur();

    @JSProperty
    String getAccessKey();

    @JSProperty
    void setAccessKey(String accessKey);

    @JSProperty
    String getAccessKeyLabel();

    @JSProperty
    int getClientWidth();

    @JSProperty
    int getClientHeight();

    @JSProperty
    int getAbsoluteLeft();

    @JSProperty
    int getAbsoluteTop();

    @JSProperty
    int getScrollLeft();

    @JSProperty
    void setScrollLeft(int scrollLeft);

    @JSProperty
    int getScrollTop();

    @JSProperty
    void setScrollTop(int scrollTop);

    @JSProperty
    int getScrollWidth();

    @JSProperty
    int getScrollHeight();

    @JSProperty
    @Override
    HTMLDocument getOwnerDocument();

    @JSProperty
    String getInnerHTML();

    @JSProperty
    void setInnerHTML(String content);

    TextRectangle getBoundingClientRect();

    @JSProperty
    String getClassName();

    @JSProperty
    void setClassName(String className);

    default HTMLElement withAttr(String name, String value) {
        setAttribute(name, value);
        return this;
    }

    default HTMLElement withChild(String tagName) {
        HTMLElement result = getOwnerDocument().createElement(tagName);
        appendChild(result);
        return this;
    }

    default HTMLElement withChild(Node node) {
        appendChild(node);
        return this;
    }

    default HTMLElement withChild(String tagName, Consumer<HTMLElement> consumer) {
        HTMLElement result = getOwnerDocument().createElement(tagName);
        appendChild(result);
        consumer.accept(result);
        return this;
    }

    default HTMLElement clear() {
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

    default HTMLElement withText(String content) {
        clear().appendChild(getOwnerDocument().createTextNode(content));
        return this;
    }

    @Override
    HTMLElement querySelector(String selectors);

    @Override
    NodeList<? extends HTMLElement> querySelectorAll(String selectors);
}
