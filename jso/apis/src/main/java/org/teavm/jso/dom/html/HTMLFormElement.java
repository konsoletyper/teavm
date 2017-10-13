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
import org.teavm.jso.dom.html.use.UseHTMLValue;

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLFormElement
 */
public interface HTMLFormElement extends HTMLElement {

    @JSProperty("acceptCharset")
    String getAcceptCharSet();

    @JSProperty("acceptCharset")
    void setAcceptCharSet(String acceptCharSet);

    @JSProperty
    String getAction();

    @JSProperty
    void setAction(String action);

    @JSProperty("autocomplete")
    String getAutoComplete();

    @JSProperty("autocomplete")
    void setAutoComplete(String autoComplete);

    @JSProperty("enctype")
    String getEncType();

    @JSProperty("enctype")
    void setEncType(String encType);

    default Encoding getEncoding() {
        return UseHTMLValue.toEnumValue(Encoding.class, innerGetEncoding());
    }

    @JSProperty("encoding")
    String innerGetEncoding();

    default void setEncoding(Encoding encoding) {
        innerSetEncoding(UseHTMLValue.getHtmlValue(encoding));
    }

    @JSProperty("encoding")
    void innerSetEncoding(String encoding);

    default Method getMethod() {
        return UseHTMLValue.toEnumValue(Method.class, innerGetMethod());
    }

    @JSProperty("method")
    String innerGetMethod();

    default void setMethod(Method method) {
        innerSetMethod(UseHTMLValue.getHtmlValue(method));
    }

    @JSProperty("method")
    void innerSetMethod(String method);

    @JSProperty
    String getName();

    @JSProperty
    void setName(String name);

    @JSProperty
    boolean isNoValidate();

    @JSProperty
    void setNoValidate(boolean noValidate);

    @JSProperty
    String getTarget();

    @JSProperty
    void setTarget(String target);

    @JSProperty
    <E extends HTMLElement> HTMLCollection<E> getElements();

    @JSMethod
    void submit();

    @JSMethod
    void reset();

    @JSMethod
    boolean checkValidity();

    @JSMethod
    boolean reportValidity();

    @JSMethod
    void requestAutocomplete();
}
