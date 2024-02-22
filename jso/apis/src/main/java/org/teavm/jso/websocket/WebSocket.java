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
import org.teavm.jso.dom.events.MessageEvent;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.ArrayBufferView;

@JSClass
public class WebSocket implements JSObject {
  public WebSocket(String url) {
  }

  public WebSocket(String url, String protocols) {
  }

  public WebSocket(String url, String[] protocols) {
  }

  @JSProperty("onclose")
  public native void onClose(EventListener<CloseEvent> eventListener);

  @JSProperty("onerror")
  public native void onError(EventListener<Event> eventListener);

  @JSProperty("onmessage")
  public native void onMessage(EventListener<MessageEvent> eventListener);

  @JSProperty("onopen")
  public native void onOpen(EventListener<Event> eventListener);

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
}
