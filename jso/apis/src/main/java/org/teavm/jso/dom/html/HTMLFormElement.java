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

public abstract class HTMLFormElement extends HTMLElement {
    @JSProperty
    public abstract String getAcceptCharset();

    @JSProperty
    public abstract void setAcceptCharset(String value);

    @JSProperty
    public abstract String getAction();

    @JSProperty
    public abstract void setAction(String value);

    @JSProperty
    public abstract String getAutocomplete();

    @JSProperty
    public abstract void setAutocomplete(String value);

    @JSProperty
    public abstract String getEnctype();

    @JSProperty
    public abstract void setEnctype(String enctype);

    @JSProperty
    public abstract String getEncoding();

    @JSProperty
    public abstract void setEncoding(String value);

    @JSProperty
    public abstract String getMethod();

    @JSProperty
    public abstract void setMethod(String value);

    @JSProperty
    public abstract String getName();

    @JSProperty
    public abstract void setName(String name);

    @JSProperty
    public abstract boolean isNoValidate();

    @JSProperty
    public abstract void setNoValidate(boolean value);

    @JSProperty
    public abstract String getTarget();

    @JSProperty
    public abstract void setTarget(String value);

    @JSIndexer
    public abstract HTMLElement get(String name);

    @JSIndexer
    public abstract HTMLElement get(int index);

    @JSProperty
    public abstract int getLength();

    public abstract void submit();

    public abstract void reset();

    public abstract boolean checkValidity();
}
