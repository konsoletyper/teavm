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

public interface HTMLAnchorElement extends HTMLElement {
    @JSProperty
    String getHref();

    @JSProperty
    void setHref(String value);

    @JSProperty
    String getTarget();

    @JSProperty
    void setTarget(String value);

    @JSProperty
    String getRel();

    @JSProperty
    void setRel(String value);

    @JSProperty
    DOMTokenList getTokenList();

    @JSProperty
    String getMedia();

    @JSProperty
    void setMedia(String value);

    @JSProperty
    String getHreflang();

    @JSProperty
    void setHreflang(String value);

    @JSProperty
    String getType();

    @JSProperty
    void setType(String value);

    @JSProperty
    String getText();

    @JSProperty
    void setText(String value);

    @JSProperty
    String getDownload();

    @JSProperty
    void setDownload(String download);
}
