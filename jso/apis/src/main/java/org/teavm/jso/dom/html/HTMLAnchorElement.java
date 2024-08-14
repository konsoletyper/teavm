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
import org.teavm.jso.dom.types.DOMTokenList;

public abstract class HTMLAnchorElement extends HTMLElement {
    @JSProperty
    public abstract String getHref();

    @JSProperty
    public abstract void setHref(String value);

    @JSProperty
    public abstract String getTarget();

    @JSProperty
    public abstract void setTarget(String value);

    @JSProperty
    public abstract String getRel();

    @JSProperty
    public abstract void setRel(String value);

    @JSProperty
    public abstract DOMTokenList getTokenList();

    @JSProperty
    public abstract String getMedia();

    @JSProperty
    public abstract void setMedia(String value);

    @JSProperty
    public abstract String getHreflang();

    @JSProperty
    public abstract void setHreflang(String value);

    @JSProperty
    public abstract String getType();

    @JSProperty
    public abstract void setType(String value);

    @JSProperty
    public abstract String getText();

    @JSProperty
    public abstract void setText(String value);

    @JSProperty
    public abstract String getDownload();

    @JSProperty
    public abstract void setDownload(String download);
}
