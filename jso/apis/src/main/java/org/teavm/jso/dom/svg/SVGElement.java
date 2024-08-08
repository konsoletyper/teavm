/*
 *  Copyright 2024 ihromant.
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
package org.teavm.jso.dom.svg;

import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.css.ElementCSSInlineStyle;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.events.FocusEventTarget;
import org.teavm.jso.dom.events.KeyboardEventTarget;
import org.teavm.jso.dom.events.LoadEventTarget;
import org.teavm.jso.dom.events.MouseEventTarget;
import org.teavm.jso.dom.events.TouchEventTarget;
import org.teavm.jso.dom.events.WheelEventTarget;
import org.teavm.jso.dom.html.HTMLCollection;
import org.teavm.jso.dom.html.TextRectangle;
import org.teavm.jso.dom.types.DOMTokenList;
import org.teavm.jso.dom.xml.Element;

public interface SVGElement extends Element, ElementCSSInlineStyle, EventTarget, FocusEventTarget,
        MouseEventTarget, WheelEventTarget, KeyboardEventTarget, LoadEventTarget, TouchEventTarget {
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
    HTMLCollection getChildren();

    @JSProperty
    String getInnerHTML();

    @JSProperty
    void setInnerHTML(String content);

    TextRectangle getBoundingClientRect();

    @JSProperty
    String getClassName();

    @JSProperty
    void setClassName(String className);

    @JSProperty
    DOMTokenList getClassList();
}
