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
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.use.UseHTMLFormValidation;
import org.teavm.jso.dom.xml.Document;

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLObjectElement
 */
public interface HTMLObjectElement extends HTMLElement, UseHTMLFormValidation {

    @JSProperty
    Document getContentDocument();

    @JSProperty
    Window getContentWindow();

    @JSProperty
    HTMLFormElement getForm();

    @JSProperty
    String getData();

    @JSProperty
    void setData(String data);

    @JSProperty
    String getType();

    @JSProperty
    void setType(String type);

    @JSProperty
    boolean isTypeMustMatch();

    @JSProperty
    void setTypeMustMatch(boolean typeMustMatch);

    @JSProperty
    String getName();

    @JSProperty
    void setName(String name);

    @JSProperty("useMap")
    String getMap();

    @JSProperty("useMap")
    void setMap(String map);
}
