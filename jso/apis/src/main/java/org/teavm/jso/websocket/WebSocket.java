/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.jso.websocket;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.events.MessageEvent;
import org.teavm.jso.dom.events.Registration;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.ArrayBufferView;

@JSClass
public class WebSocket implements JSObject, EventTarget {
  public WebSocket(String url) {
  }

  public WebSocket(String url, String protocols) {
  }

  public WebSocket(String url, String[] protocols) {
  }

  public final Registration onClose(EventListener<CloseEvent> eventListener) {
    return onEvent("close", eventListener);
  }

  public final Registration onError(EventListener<Event> eventListener) {
    return onEvent("error", eventListener);
  }

  public final Registration onMessage(EventListener<MessageEvent> eventListener) {
    return onEvent("message", eventListener);
  }

  public final Registration onOpen(EventListener<Event> eventListener) {
    return onEvent("open", eventListener);
  }

  @JSBody(params = "url", script = "return new WebSocket(url);")
  @Deprecated
  public static native WebSocket create(String url);

  @JSBody(params = { "url", "protocols" }, script = "return new WebSocket(url, protocols);")
  @Deprecated
  public static native WebSocket create(String url, String protocols);

  @JSBody(params = { "url", "protocols" }, script = "return new WebSocket(url, protocols);")
  @Deprecated
  public static native WebSocket create(String url, String[] protocols);

  public native void close();

  public native void close(int code);

  public native void close(int code, String reason);

  public native void send(String data);

  public native void send(ArrayBuffer data);

  public native void send(ArrayBufferView data);

  @JSProperty
  public native String getBinaryType();

  @JSProperty
  public native void setBinaryType(String binaryType);

  @JSProperty
  public native int getBufferedAmount();

  @JSProperty
  public native String getExtensions();

  @JSProperty
  public native String getProtocol();

  @JSProperty
  public native int getReadyState();

  @JSProperty
  public native String getUrl();

  @JSBody(script = "return typeof WebSocket !== 'undefined';")
  public static native boolean isSupported();

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
