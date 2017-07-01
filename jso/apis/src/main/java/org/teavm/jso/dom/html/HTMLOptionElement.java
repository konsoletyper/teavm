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

public interface HTMLOptionElement extends HTMLElement {
    @JSProperty
    boolean isDisabled();

    @JSProperty
    void setDisabled(boolean disabled);

    @JSProperty
    String getLabel();

    @JSProperty
    void setLabel(String label);

    @JSProperty
    boolean isDefaultSelected();

    @JSProperty
    void setDefaultSelected(boolean defaultSelected);

    @JSProperty
    boolean isSelected();

    @JSProperty
    void setSelected(boolean selected);

    @JSProperty
    String getValue();

    @JSProperty
    void setValue(String value);

    @JSProperty
    String getText();

    @JSProperty
    void setText(String text);

    @JSProperty
    int getIndex();
}
