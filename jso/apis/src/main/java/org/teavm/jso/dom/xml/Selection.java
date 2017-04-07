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
package org.teavm.jso.dom.xml;

import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.range.Range;

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/Selection
 */
public interface Selection extends JSObject {

    @JSProperty
    Node getAnchorNode();

    @JSProperty
    int getAnchorOffset();

    @JSProperty
    Node getFocusNode();

    @JSProperty
    int getFocusOffset();

    @JSProperty
    boolean isCollapsed();

    @JSProperty
    int getRangeCount();

    @JSProperty
    String getType();

    @JSMethod
    void addRange(Range range);

    @JSMethod
    void collapse(Node node, int index);

    @JSMethod
    void collapseToEnd();

    @JSMethod
    void collapseToStart();

    @JSMethod
    boolean containsNode(Node node, boolean allowPartial);

    @JSMethod
    void deleteFromDocument();

    @JSMethod
    void empty();

    @JSMethod
    void extend(Node node, int offset);

    @JSMethod
    Range getRangeAt(int index);

    @JSMethod
    void removeRange(Range range);

    @JSMethod
    void removeAllRanges();

    @JSMethod
    void selectAllChildren(Node node);

    @JSMethod
    void setBaseAndExtent(Node baseNode, int baseOffset, Node extentNode, int extentOffset);

    @JSMethod
    void setPosition(Node node, int offset);
}
