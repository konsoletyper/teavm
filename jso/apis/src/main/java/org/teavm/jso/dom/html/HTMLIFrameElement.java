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
import org.teavm.jso.browser.Window;

public abstract class HTMLIFrameElement extends HTMLElement {
    @JSProperty
    public abstract HTMLDocument getContentDocument();

    @JSProperty
    public abstract Window getContentWindow();

    @JSProperty
    public abstract String getWidth();

    @JSProperty
    public abstract void setWidth(String width);

    @JSProperty
    public abstract String getHeight();

    @JSProperty
    public abstract void setHeight(String height);

    @JSProperty
    public abstract String getName();

    @JSProperty
    public abstract void setName(String name);

    @JSProperty("src")
    public abstract String getSourceAddress();

    @JSProperty("src")
    public abstract void setSourceAddress(String src);

    @JSProperty("srcdoc")
    public abstract String getSourceDocument();

    @JSProperty("srcdoc")
    public abstract void setSourceDocument(String srcdoc);
}
