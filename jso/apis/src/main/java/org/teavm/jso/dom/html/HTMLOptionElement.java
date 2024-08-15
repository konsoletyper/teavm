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

public abstract class HTMLOptionElement extends HTMLElement {
    @JSProperty
    public abstract boolean isDisabled();

    @JSProperty
    public abstract void setDisabled(boolean disabled);

    @JSProperty
    public abstract String getLabel();

    @JSProperty
    public abstract void setLabel(String label);

    @JSProperty
    public abstract boolean isDefaultSelected();

    @JSProperty
    public abstract void setDefaultSelected(boolean defaultSelected);

    @JSProperty
    public abstract boolean isSelected();

    @JSProperty
    public abstract void setSelected(boolean selected);

    @JSProperty
    public abstract String getValue();

    @JSProperty
    public abstract void setValue(String value);

    @JSProperty
    public abstract String getText();

    @JSProperty
    public abstract void setText(String text);

    @JSProperty
    public abstract int getIndex();
}
