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

public interface HTMLInputElement extends HTMLElement {
    @JSProperty
    boolean isChecked();

    @JSProperty
    void setChecked(boolean checked);

    @JSProperty
    boolean isDisabled();

    @JSProperty
    void setDisabled(boolean disabled);

    @JSProperty
    int getMaxLength();

    @JSProperty
    void setMaxLength(int maxLength);

    @JSProperty
    String getName();

    @JSProperty
    void setName(String name);

    @JSProperty
    boolean isReadOnly();

    @JSProperty
    void setReadOnly(boolean readOnly);

    @JSProperty
    int getSize();

    @JSProperty
    void setSize(int size);

    @JSProperty
    String getType();

    @JSProperty
    void setType(String type);

    @JSProperty
    String getValue();

    @JSProperty
    void setValue(String value);

    void select();
}
