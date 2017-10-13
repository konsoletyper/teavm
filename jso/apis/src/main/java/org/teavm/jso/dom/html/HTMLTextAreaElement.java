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
import org.teavm.jso.dom.html.use.UseHTMLFormControl;

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLTextAreaElement
 */
public interface HTMLTextAreaElement extends HTMLElement, UseHTMLFormControl {

    @JSProperty("autocomplete")
    String getAutoComplete();

    @JSProperty("autocomplete")
    void setAutoComplete(String autoComplete);

    @JSProperty
    int getCols();

    @JSProperty
    void setCols(int cols);

    @JSProperty
    String getDirName();

    @JSProperty
    void setDirName(String dirName);

    @JSProperty
    String getInputMode();

    @JSProperty
    void setInputMode(String inputMode);

    @JSProperty
    int getMaxLength();

    @JSProperty
    void setMaxLength(int maxLength);

    @JSProperty
    int getMinLength();

    @JSProperty
    void setMinLength(int minLength);

    @JSProperty
    boolean isRequired();

    @JSProperty
    void setRequired(boolean required);

    @JSProperty
    int getRows();

    @JSProperty
    void setRows(int rows);

    @JSProperty
    String getWrap();

    @JSProperty
    void setWrap(String wrap);

    @JSProperty
    String getDefaultValue();

    @JSProperty
    void setDefaultValue(String defaultValue);

    @JSProperty
    int getTextLength();

    @JSMethod
    void select();

    @JSProperty
    int getSelectionStart();

    @JSProperty
    void setSelectionStart(int selectionStart);

    @JSProperty
    int getSelectionEnd();

    @JSProperty
    void setSelectionEnd(int selectionEnd);

    @JSProperty
    String getSelectionDirection();

    @JSProperty
    void setSelectionDirection(String selectionDirection);

    @JSMethod
    void setRangeText(String replacement);

    @JSMethod
    void setRangeText(String replacement, int start, int end);

    @JSMethod
    void setRangeText(String replacement, int start, int end, String selectionMode);

    @JSMethod
    void setSelectionRange(int start, int end);

    @JSMethod
    void setSelectionRange(int start, int end, String direction);
}
