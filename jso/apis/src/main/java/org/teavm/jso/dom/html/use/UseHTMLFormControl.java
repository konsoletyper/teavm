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
package org.teavm.jso.dom.html.use;

import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.html.HTMLLabelElement;
import org.teavm.jso.dom.html.InputType;
import org.teavm.jso.dom.xml.NodeList;

/**
 * All elements containing html form control methods - instead of doing duplicated code.
 * Not a real element.
 */
public interface UseHTMLFormControl extends UseHTMLFormValidation {

    @JSProperty("autofocus")
    boolean isAutoFocus();

    @JSProperty("autofocus")
    void setAutoFocus(boolean autoFocus);

    @JSProperty
    boolean isDisabled();

    @JSProperty
    void setDisabled(boolean disabled);

    @JSProperty
    String getFormAction();

    @JSProperty
    void setFormAction(String formAction);

    @JSProperty("formEnctype")
    String getFormEncType();

    @JSProperty("formEnctype")
    void setFormEncType(String formEncType);

    @JSProperty
    String getFormMethod();

    @JSProperty
    void setFormMethod(String formMethod);

    @JSProperty
    boolean isFormNoValidate();

    @JSProperty
    void setFormNoValidate(boolean formNoValidate);

    @JSProperty
    String getFormTarget();

    @JSProperty
    void setFormTarget(String formTarget);

    @JSProperty
    NodeList<HTMLLabelElement> getLabels();

    @JSProperty
    String getName();

    @JSProperty
    void setName(String name);

    default InputType getType() {
        return UseHTMLValue.toEnumValue(InputType.class, innerGetType());
    }

    @JSProperty("type")
    String innerGetType();

    default void setType(InputType type) {
        innerSetType(UseHTMLValue.getHtmlValue(type));
    }

    @JSProperty("type")
    void innerSetType(String type);

    @JSProperty
    String getValue();

    @JSProperty
    void setValue(String value);

    @JSProperty("placeholder")
    String getPlaceHolder();

    @JSProperty("placeholder")
    void setPlaceHolder(String placeholder);

    @JSProperty
    boolean isReadOnly();

    @JSProperty
    void setReadOnly(boolean readOnly);
}
