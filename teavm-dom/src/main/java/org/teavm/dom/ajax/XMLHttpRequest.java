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
package org.teavm.dom.ajax;

import org.teavm.dom.core.Document;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Alexey Andreev
 */
public interface XMLHttpRequest extends JSObject {
    int UNSET = 0;

    int OPENED = 1;

    int HEADERS_RECEIVED = 2;

    int LOADING = 3;

    int DONE = 4;

    void open(String method, String url);

    void open(String method, String url, boolean async);

    void open(String method, String url, boolean async, String user);

    void open(String method, String url, boolean async, String user, String password);

    void send();

    void send(String data);

    void setRequestHeader(String name, String value);

    String getAllResponseHeaders();

    String getResponseHeader(String name);

    @JSProperty("onreadystatechange")
    void setOnReadyStateChange(ReadyStateChangeHandler handler);

    void overrideMimeType(String mimeType);

    @JSProperty
    int getReadyState();

    @JSProperty
    String getResponseText();

    @JSProperty
    Document getResponseXML();
    
    @JSProperty
    JSObject getResponse();

    @JSProperty
    int getStatus();

    @JSProperty
    String getStatusText();
    
    @JSProperty
    void setResponseType(String type);
    
    @JSProperty
    String getResponseType();
}
