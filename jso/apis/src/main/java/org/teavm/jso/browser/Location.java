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
package org.teavm.jso.browser;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface Location extends JSObject {
    @JSProperty("href")
    String getFullURL();

    @JSProperty("href")
    void setFullURL(String url);

    @JSProperty
    String getProtocol();

    @JSProperty
    void setProtocol(String protocol);

    @JSProperty
    String getHost();

    @JSProperty
    void setHost(String host);

    @JSProperty("hostname")
    String getHostName();

    @JSProperty("hostname")
    void setHostName(String hostName);

    @JSProperty
    String getPort();

    @JSProperty
    void setPort(String port);

    @JSProperty("pathname")
    String getPathName();

    @JSProperty("pathname")
    void setPathName(String pathName);

    @JSProperty
    String getSearch();

    @JSProperty
    void setSearch(String search);

    @JSProperty
    String getHash();

    @JSProperty
    void setHash(String hash);

    void assign(String url);

    void reload();

    void reload(boolean force);

    void replace(String url);

    static Location current() {
        return Window.current().getLocation();
    }
}
