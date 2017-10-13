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
import org.teavm.jso.dom.html.use.UseHTMLHRef;
import org.teavm.jso.dom.xml.DOMTokenList;

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLHyperlinkElementUtils
 */
public interface HTMLHyperlinkElementUtils extends UseHTMLHRef {

    @JSProperty
    String getDownload();

    @JSProperty
    void setDownload(String arg);

    @JSProperty
    String getHash();

    @JSProperty
    void setHash(String hash);

    @JSProperty
    String getHost();

    @JSProperty
    void setHost(String host);

    @JSProperty
    String getHostname();

    @JSProperty
    void setHostname(String hostname);

    @JSProperty("hreflang")
    String getHRefLang();

    @JSProperty("hreflang")
    void setHRefLang(String hRefLang);

    @JSProperty
    String getOrigin();

    @JSProperty
    String getPassword();

    @JSProperty
    void setPassword(String password);

    @JSProperty
    DOMTokenList getPing();

    @JSProperty
    String getPort();

    @JSProperty
    void setPort(String port);

    @JSProperty
    String getProtocol();

    @JSProperty
    void setProtocol(String protocol);

    @JSProperty
    String getRel();

    @JSProperty
    void setRel(String rel);

    @JSProperty
    DOMTokenList getRelList();

    @JSProperty
    String getSearch();

    @JSProperty
    void setSearch(String search);

    @JSProperty
    String getText();

    @JSProperty
    void setText(String text);

    @JSProperty
    String getType();

    @JSProperty
    void setType(String type);

    @JSProperty
    String getUsername();

    @JSProperty
    void setUsername(String username);
}
