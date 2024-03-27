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
import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.xml.Document;
import org.teavm.jso.file.JSBlob;

@JSClass
public class XMLHttpRequest implements JSObject, EventTarget {
    public static final int UNSET = 0;

    public static final int OPENED = 1;

    public static final int HEADERS_RECEIVED = 2;

    public static final int LOADING = 3;

    public static final int DONE = 4;

    public XMLHttpRequest() {
    }

    public native void open(String method, String url);

    public native void open(String method, String url, boolean async);

    public native void open(String method, String url, boolean async, String user);

    public native void open(String method, String url, boolean async, String user, String password);

    public native void send();

    public native void send(String data);

    public native void send(JSBlob blob);

    public native void send(JSObject data);

    public native void setRequestHeader(String name, String value);

    public native String getAllResponseHeaders();

    public native String getResponseHeader(String name);

    @JSProperty("onreadystatechange")
    public native void setOnReadyStateChange(ReadyStateChangeHandler handler);

    @JSProperty("onreadystatechange")
    public native void setOnReadyStateChange(EventListener<Event> handler);

    @JSProperty("onabort")
    public native void onAbort(EventListener<ProgressEvent> eventListener);

    @JSProperty("onerror")
    public native void onError(EventListener<ProgressEvent> eventListener);

    @JSProperty("onload")
    public native void onLoad(EventListener<ProgressEvent> eventListener);

    @JSProperty("onloadstart")
    public native void onLoadStart(EventListener<ProgressEvent> eventListener);

    @JSProperty("onloadend")
    public native void onLoadEnd(EventListener<ProgressEvent> eventListener);

    @JSProperty("onprogress")
    public native void onProgress(EventListener<ProgressEvent> eventListener);

    @JSProperty("ontimeout")
    public native void onTimeout(EventListener<ProgressEvent> eventListener);

    public final void onComplete(Runnable runnable) {
        setOnReadyStateChange(() -> {
            if (getReadyState() == DONE) {
                runnable.run();
            }
        });
    }

    public native void overrideMimeType(String mimeType);

    @JSProperty
    public native int getReadyState();

    @JSProperty
    public native String getResponseText();

    @JSProperty
    public native Document getResponseXML();

    @JSProperty
    public native JSObject getResponse();

    @JSProperty
    public native int getStatus();

    @JSProperty
    public native String getStatusText();

    @JSProperty
    public native void setResponseType(String type);

    @JSProperty
    public native String getResponseType();

    @JSBody(script = "return new XMLHttpRequest();")
    @Deprecated
    public static native XMLHttpRequest create();

    public native void abort();

    @JSProperty
    public native String getResponseURL();

    @Override
    public native void addEventListener(String type, EventListener<?> listener, boolean useCapture);

    @Override
    public native void addEventListener(String type, EventListener<?> listener);

    @Override
    public native void removeEventListener(String type, EventListener<?> listener, boolean useCapture);

    @Override
    public native void removeEventListener(String type, EventListener<?> listener);

    @Override
    public native boolean dispatchEvent(Event evt);
}
