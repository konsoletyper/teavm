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
import org.teavm.jso.dom.xml.DOMTokenList;

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLTableCellElement
 */
public interface HTMLTableCellElement extends HTMLElement {

    @JSProperty
    int getColSpan();

    @JSProperty
    void setColSpan(int colSpan);

    default void removeColSpan() {
        removeAttribute("colSpan");
    }

    @JSProperty
    int getRowSpan();

    @JSProperty
    void setRowSpan(int rowSpan);

    default void removeRowSpan() {
        removeAttribute("rowSpan");
    }

    @JSProperty
    DOMTokenList getHeaders();

    @JSProperty
    int getCellIndex();

    @JSProperty
    String getScope();

    @JSProperty
    void setScope(String scope);

    @JSProperty
    String getAbbr();

    @JSProperty
    void setAbbr(String abbr);
}
