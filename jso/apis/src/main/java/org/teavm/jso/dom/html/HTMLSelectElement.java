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

public abstract class HTMLSelectElement extends HTMLElement {
    @JSProperty
    public abstract boolean isDisabled();

    @JSProperty
    public abstract void setDisabled(boolean disabled);

    @JSProperty
    public abstract boolean isMultiple();

    @JSProperty
    public abstract void setMultiple(boolean multiple);

    @JSProperty
    public abstract HTMLOptionsCollection getOptions();

    @JSProperty
    public abstract String getName();

    @JSProperty
    public abstract void setName(String name);

    @JSProperty
    public abstract int getSize();

    @JSProperty
    public abstract void setSize(int size);

    @JSProperty
    public abstract int getSelectedIndex();

    @JSProperty
    public abstract void setSelectedIndex(int selectedIndex);

    @JSProperty
    public abstract String getValue();

    @JSProperty
    public abstract void setValue(String value);

    public abstract void setCustomValidity(String validationFailure);
    
    public abstract boolean reportValidity();
}
