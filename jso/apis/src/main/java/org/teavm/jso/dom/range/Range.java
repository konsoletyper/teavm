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
package org.teavm.jso.dom.range;

import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.html.use.UseHTMLValue;
import org.teavm.jso.dom.xml.DOMRect;
import org.teavm.jso.dom.xml.DOMRectList;
import org.teavm.jso.dom.xml.DocumentFragment;
import org.teavm.jso.dom.xml.Node;

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/Range
 */
public interface Range extends JSObject {

    @JSProperty
    boolean isCollapsed();

    @JSProperty
    Node getCommonAncestorContainer();

    @JSProperty
    Node getEndContainer();

    @JSProperty
    int getEndOffset();

    @JSProperty
    Node getStartContainer();

    @JSProperty
    int getStartOffset();

    @JSMethod
    DocumentFragment cloneContents();

    @JSMethod
    Range cloneRange();

    @JSMethod
    void collapse(boolean toStart);

    @JSMethod
    short comparePoint(Node refNode, int offset);

    default short compareBoundaryPoints(RangeCompare how, Range sourceRange) {
        return innerCompareBoundaryPoints(UseHTMLValue.getHtmlValue(how), sourceRange);
    }

    @JSMethod("compareBoundaryPoints")
    short innerCompareBoundaryPoints(short how, Range sourceRange);

    @JSMethod
    DocumentFragment createContextualFragment(String html);

    @JSMethod
    void deleteContents();

    @JSMethod
    void detach();

    @JSMethod
    DocumentFragment extractContents();

    @JSMethod
    DOMRect getBoundingClientRect();

    @JSMethod
    DOMRectList getClientRects();

    @JSMethod
    void insertNode(Node newNode);

    @JSMethod
    boolean intersectsNode(Node refNode);

    @JSMethod
    boolean isPointInRange(Node refNode, int offset);

    @JSMethod
    void selectNode(Node refNode);

    @JSMethod
    void selectNodeContents(Node refNode);

    @JSMethod
    void setEnd(Node refNode, int offset);

    @JSMethod
    void setEndAfter(Node refNode);

    @JSMethod
    void setEndBefore(Node refNode);

    @JSMethod
    void setStart(Node refNode, int offset);

    @JSMethod
    void setStartAfter(Node refNode);

    @JSMethod
    void setStartBefore(Node refNode);

    @JSMethod
    void surroundContents(Node newParent);
}
