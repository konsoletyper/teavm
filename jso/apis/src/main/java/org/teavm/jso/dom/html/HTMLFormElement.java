/*
 *  Copyright 2015 Alexey Andreev.
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

/**
*
* @author Jan-Felix Wittmann
*/
public interface HTMLFormElement extends HTMLElement {

    @JSProperty
    void setAcceptCharset(String acceptCharset);

    @JSProperty
    String getAcceptCharset();

    @JSProperty
    void setAction(String action);

    @JSProperty
    String getAction();

    @JSProperty
    void setAutocomplete(String autocomplete);

    @JSProperty
    String getAutocomplete();

    @JSProperty
    void setEnctype(String enctype);

    @JSProperty
    String getEnctype();

    @JSProperty
    void setEncoding(String encoding);

    @JSProperty
    String getEncoding();

    @JSProperty
    void setMethod(String method);

    @JSProperty
    void getMethod();

    @JSProperty
    void setName(String name);

    @JSProperty
    String getName();

    @JSProperty
    void setNoValidate(boolean noValidate);

    @JSProperty
    boolean isNoValidate();

    @JSProperty
    void setTarget(String target);

    @JSProperty
    String getTarget();

    @JSProperty
    HTMLFormControlsCollection getElements();

    @JSProperty
    int getLength();

    void submit();

    void reset();

    void reportValidity();
}
