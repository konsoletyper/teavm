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

public interface HTMLTextAreaElement extends HTMLElement {
    @JSProperty
    String getAutocomplete();

    @JSProperty
    void setAutocomplete(String value);

    @JSProperty
    boolean isAutofocus();

    @JSProperty
    void setAutofocus(boolean value);

    @JSProperty
    int getCols();

    @JSProperty
    void setCols(int cols);

    @JSProperty
    String getDirName();

    @JSProperty
    void setDirName(String value);

    @JSProperty
    boolean isDisabled();

    @JSProperty
    void setDisabled(boolean value);

    @JSProperty
    HTMLFormElement getForm();

    @JSProperty
    int getMaxLength();

    @JSProperty
    void setMaxLength(int value);

    @JSProperty
    int getMinLength();

    @JSProperty
    void setMinLength(int value);

    @JSProperty
    String getName();

    @JSProperty
    void setName(String value);

    @JSProperty
    String getPlaceholder();

    @JSProperty
    void setPlaceholder(String value);

    @JSProperty
    boolean isReadOnly();

    @JSProperty
    void setReadOnly(boolean value);

    @JSProperty
    int getRows();

    @JSProperty
    void setRows(int rows);

    @JSProperty
    String getWrap();

    @JSProperty
    void setWrap(String value);

    @JSProperty
    String getType();

    @JSProperty
    String getDefaultValue();

    @JSProperty
    void setDefaultValue(String value);

    @JSProperty
    String getValue();

    @JSProperty
    void setValue(String value);

    @JSProperty
    int getTextLength();


    void select();

    @JSProperty
    int getSelectionStart();

    @JSProperty
    void setSelectionStart(int value);

    @JSProperty
    int getSelectionEnd();

    @JSProperty
    void setSelectionEnd(int value);

    @JSProperty
    String getSelectionDirection();

    @JSProperty
    void setSelectionDirection(String value);

    void setRangeText(String replacement);

    void setRangeText(String replacement, int start, int end, String selectionMode);

    void setRangeText(String replacement, int start, int end);

    void setSelectionRange(int start, int end, String direction);

    void setSelectionRange(int start, int end);
}
