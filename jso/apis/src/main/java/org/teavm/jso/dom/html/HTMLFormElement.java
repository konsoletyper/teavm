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

import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSProperty;

public interface HTMLFormElement extends HTMLElement {
    @JSProperty
    String getAcceptCharset();

    @JSProperty
    void setAcceptCharset(String value);

    @JSProperty
    String getAction();

    @JSProperty
    void setAction(String value);

    @JSProperty
    String getAutocomplete();

    @JSProperty
    void setAutocomplete(String value);

    @JSProperty
    String getEnctype();

    @JSProperty
    void setEnctype(String enctype);

    @JSProperty
    String getEncoding();

    @JSProperty
    void setEncoding(String value);

    @JSProperty
    String getMethod();

    @JSProperty
    void setMethod(String value);

    @JSProperty
    String getName();

    @JSProperty
    void setName(String name);

    @JSProperty
    boolean isNoValidate();

    @JSProperty
    void setNoValidate(boolean value);

    @JSProperty
    String getTarget();

    @JSProperty
    void setTarget(String value);

    @JSIndexer
    HTMLElement get(String name);

    @JSIndexer
    HTMLElement get(int index);

    @JSProperty
    int getLength();

    void submit();

    void reset();

    boolean checkValidity();
}
