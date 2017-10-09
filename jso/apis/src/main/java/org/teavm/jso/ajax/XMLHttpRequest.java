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
package org.teavm.jso.ajax;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.xml.Document;

public abstract class XMLHttpRequest implements JSObject {
    public static final int UNSET = 0;

    public static final int OPENED = 1;

    public static final int HEADERS_RECEIVED = 2;

    public static final int LOADING = 3;

    public static final int DONE = 4;

    public abstract void open(String method, String url);

    public abstract void open(String method, String url, boolean async);

    public abstract void open(String method, String url, boolean async, String user);

    public abstract void open(String method, String url, boolean async, String user, String password);

    public abstract void send();

    public abstract void send(String data);

    public abstract void setRequestHeader(String name, String value);

    public abstract String getAllResponseHeaders();

    public abstract String getResponseHeader(String name);

    @JSProperty("onreadystatechange")
    public abstract void setOnReadyStateChange(ReadyStateChangeHandler handler);

    public final void onComplete(Runnable runnable) {
        setOnReadyStateChange(() -> {
            if (getReadyState() == DONE) {
                runnable.run();
            }
        });
    }

    public abstract void overrideMimeType(String mimeType);

    @JSProperty
    public abstract int getReadyState();

    @JSProperty
    public abstract String getResponseText();

    @JSProperty
    public abstract Document getResponseXML();

    @JSProperty
    public abstract JSObject getResponse();

    @JSProperty
    public abstract int getStatus();

    @JSProperty
    public abstract String getStatusText();

    @JSProperty
    public abstract void setResponseType(String type);

    @JSProperty
    public abstract String getResponseType();

    @JSBody(script = "return new XMLHttpRequest();")
    public static native XMLHttpRequest create();
}
