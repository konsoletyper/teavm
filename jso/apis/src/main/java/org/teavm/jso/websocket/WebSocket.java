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
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MessageEvent;

public abstract class WebSocket implements JSObject {
  @JSProperty("onclose")
  public abstract void onClose(EventListener<CloseEvent> eventListener);

  @JSProperty("onerror")
  public abstract void onError(EventListener<Event> eventListener);

  @JSProperty("onmessage")
  public abstract void onMessage(EventListener<MessageEvent> eventListener);

  @JSProperty("onopen")
  public abstract void onOpen(EventListener<MessageEvent> eventListener);

  @JSBody(params = "url", script = "return new WebSocket(url);")
  public static native WebSocket create(String url);

  @JSBody(params = { "url", "protocols" }, script = "return new WebSocket(url, protocols);")
  public static native WebSocket create(String url, String protocols);

  @JSBody(params = { "url", "protocols" }, script = "return new WebSocket(url, protocols);")
  public static native WebSocket create(String url, String[] protocols);

  public abstract void close();

  public abstract void close(int code);

  public abstract void close(int code, String reason);

  public abstract void send(String data);

  @JSProperty
  public abstract String getBinaryType();

  @JSProperty
  public abstract void setBinaryType(String binaryType);

  @JSProperty
  public abstract int getBufferedAmount();

  @JSProperty
  public abstract String getExtensions();

  @JSProperty
  public abstract String getProtocol();

  @JSProperty
  public abstract int getReadyState();

  @JSProperty
  public abstract String getUrl();

  @JSBody(script = "return typeof WebSocket !== 'undefined';")
  protected static native boolean isSupported();
}
