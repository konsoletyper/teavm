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

public abstract class HTMLButtonElement extends HTMLElement {
    public static final String TYPE_BUTTON = "button";

    public static final String TYPE_RESET = "reset";

    public static final String TYPE_SUBMIT = "submit";

    @JSProperty
    public abstract boolean isAutofocus();

    @JSProperty
    public abstract void setAutofocus(boolean autofocus);

    @JSProperty
    public abstract boolean isDisabled();

    @JSProperty
    public abstract void setDisabled(boolean disabled);

    @JSProperty
    public abstract HTMLElement getForm();

    @JSProperty
    public abstract String getName();

    @JSProperty
    public abstract void setName(String name);

    @JSProperty
    public abstract String getValue();

    @JSProperty
    public abstract void setValue(String value);

    @JSProperty
    public abstract String getType();

    @JSProperty
    public abstract void setType(String type);
}
