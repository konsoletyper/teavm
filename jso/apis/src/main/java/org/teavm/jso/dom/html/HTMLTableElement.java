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

import org.teavm.jso.JSMethod;
import org.teavm.jso.JSProperty;

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLTableElement
 */
public interface HTMLTableElement extends HTMLElement {

    @JSProperty
    void setCellPadding(int padding);

    @JSProperty
    void setCellSpacing(int spacing);

    @JSProperty
    void setBorder(int border);

    @JSProperty
    HTMLTableCaptionElement getCaption();

    @JSProperty
    void setCaption(HTMLTableCaptionElement caption);

    @JSMethod
    HTMLTableCaptionElement createCaption();

    @JSMethod
    void deleteCaption();

    @JSProperty("tHead")
    HTMLTableSectionElement getTHead();

    @JSProperty("tHead")
    void setTHead(HTMLTableSectionElement tHead);

    @JSMethod
    HTMLTableSectionElement createTHead();

    @JSMethod
    void deleteTHead();

    @JSProperty("tFoot")
    HTMLTableSectionElement getTFoot();

    @JSProperty("tFoot")
    void setTFoot(HTMLTableSectionElement tFoot);

    @JSMethod
    HTMLTableSectionElement createTFoot();

    @JSMethod
    void deleteTFoot();

    @JSProperty("tBodies")
    HTMLCollection<HTMLTableSectionElement> getTBodies();

    @JSMethod
    HTMLTableSectionElement createTBody();

    @JSProperty
    HTMLCollection<HTMLTableRowElement> getRows();

    @JSMethod
    HTMLTableRowElement insertRow();

    @JSMethod
    HTMLTableRowElement insertRow(int index);

    @JSMethod
    void deleteRow(int index);
}
