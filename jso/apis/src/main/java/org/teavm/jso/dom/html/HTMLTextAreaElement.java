/*
 *  Copyright 2018 Alexey Andreev.
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

public abstract class HTMLTextAreaElement extends HTMLElement {
    @JSProperty
    public abstract String getAutocomplete();

    @JSProperty
    public abstract void setAutocomplete(String value);

    @JSProperty
    public abstract boolean isAutofocus();

    @JSProperty
    public abstract void setAutofocus(boolean value);

    @JSProperty
    public abstract int getCols();

    @JSProperty
    public abstract void setCols(int cols);

    @JSProperty
    public abstract String getDirName();

    @JSProperty
    public abstract void setDirName(String value);

    @JSProperty
    public abstract boolean isDisabled();

    @JSProperty
    public abstract void setDisabled(boolean value);

    @JSProperty
    public abstract HTMLFormElement getForm();

    @JSProperty
    public abstract int getMaxLength();

    @JSProperty
    public abstract void setMaxLength(int value);

    @JSProperty
    public abstract int getMinLength();

    @JSProperty
    public abstract void setMinLength(int value);

    @JSProperty
    public abstract String getName();

    @JSProperty
    public abstract void setName(String value);

    @JSProperty
    public abstract String getPlaceholder();

    @JSProperty
    public abstract void setPlaceholder(String value);

    @JSProperty
    public abstract boolean isReadOnly();

    @JSProperty
    public abstract void setReadOnly(boolean value);

    @JSProperty
    public abstract int getRows();

    @JSProperty
    public abstract void setRows(int rows);

    @JSProperty
    public abstract String getWrap();

    @JSProperty
    public abstract void setWrap(String value);

    @JSProperty
    public abstract String getType();

    @JSProperty
    public abstract String getDefaultValue();

    @JSProperty
    public abstract void setDefaultValue(String value);

    @JSProperty
    public abstract String getValue();

    @JSProperty
    public abstract void setValue(String value);

    @JSProperty
    public abstract int getTextLength();

    public abstract void setCustomValidity(String validationFailure);
    
    public abstract boolean checkValidity();
    
    public abstract boolean reportValidity();

    public abstract void select();

    @JSProperty
    public abstract int getSelectionStart();

    @JSProperty
    public abstract void setSelectionStart(int value);

    @JSProperty
    public abstract int getSelectionEnd();

    @JSProperty
    public abstract void setSelectionEnd(int value);

    @JSProperty
    public abstract String getSelectionDirection();

    @JSProperty
    public abstract void setSelectionDirection(String value);

    public abstract void setRangeText(String replacement);

    public abstract void setRangeText(String replacement, int start, int end, String selectionMode);

    public abstract void setRangeText(String replacement, int start, int end);

    public abstract void setSelectionRange(int start, int end, String direction);

    public abstract void setSelectionRange(int start, int end);
}
