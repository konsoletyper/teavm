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
package org.teavm.jso.browser;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.JSTopLevel;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSArrayReader;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLIFrameElement;

public abstract class Window implements JSObject, WindowEventTarget, StorageProvider, JSArrayReader<HTMLIFrameElement> {
    private Window() {
    }

    @JSProperty
    public abstract HTMLDocument getDocument();

    @JSProperty
    public abstract Screen getScreen();

    @JSProperty
    public abstract int getScreenX();

    @JSProperty
    public abstract int getScreenY();
    @JSProperty
    public abstract Location getLocation();

    @JSProperty
    public abstract History getHistory();

    @JSProperty
    public abstract HTMLElement getFrameElement();

    @JSProperty
    public abstract JSArrayReader<HTMLIFrameElement> getFrames();

    @JSProperty
    public abstract int getInnerWidth();

    @JSProperty
    public abstract int getInnerHeight();

    @JSProperty
    public abstract int getOuterWidth();

    @JSProperty
    public abstract int getOuterHeight();

    @JSProperty
    public abstract int getScrollX();

    @JSProperty
    public abstract int getScrollY();

    @JSProperty
    public abstract String getName();

    @JSProperty
    public abstract void setName(String name);

    @JSProperty
    public abstract Window getParent();

    @JSProperty
    public abstract Window getTop();

    @JSTopLevel
    public static native void alert(JSObject message);

    @JSTopLevel
    public static native void alert(String message);

    @JSTopLevel
    public static native boolean confirm(JSObject message);

    @JSTopLevel
    public static native boolean confirm(String message);

    public static String prompt(String message) {
        return prompt(message, "");
    }

    @JSTopLevel
    public static native String prompt(String message, String defaultValue);

    @JSTopLevel
    public static native int setTimeout(TimerHandler handler, int delay);

    @JSTopLevel
    public static native int setTimeout(TimerHandler handler, double delay);

    @JSTopLevel
    public static native void clearTimeout(int timeoutId);

    @JSTopLevel
    public static native int setInterval(TimerHandler handler, int delay);

    @JSTopLevel
    public static native int setInterval(TimerHandler handler, double delay);

    @JSTopLevel
    public static native void clearInterval(int timeoutId);

    @JSTopLevel
    public static native int requestAnimationFrame(AnimationFrameCallback callback);

    @JSTopLevel
    public static native void cancelAnimationFrame(int requestId);

    public abstract void blur();

    public abstract void focus();

    public abstract void close();

    public abstract void moveBy(int deltaX, int deltaY);

    public abstract void moveTo(int x, int y);

    public abstract void resizeBy(int deltaX, int deltaY);

    public abstract void resizeTo(int x, int y);

    public abstract void scrollBy(int deltaX, int deltaY);

    public abstract void scrollTo(int x, int y);

    public abstract Window open(String url, String name);

    public final Window open(String url, String name, WindowFeatures features) {
        return open(url, name, features.sb.toString());
    }

    public abstract Window open(String url, String name, String features);

    public abstract void print();

    public abstract void stop();

    public abstract void postMessage(JSObject message);

    public abstract void postMessage(JSObject message, JSArrayReader<JSObject> transfer);

    public abstract void postMessage(JSObject message, String targetOrigin);

    public abstract void postMessage(JSObject message, String targetOrigin, JSArrayReader<JSObject> transfer);

    public final void postMessage(JSObject message, String targetOrigin, JSObject... transfer) {
        postMessage(message, targetOrigin, JSArray.of(transfer));
    }

    public final void postMessage(JSObject message, JSObject... transfer) {
        postMessage(message, JSArray.of(transfer));
    }

    @JSTopLevel
    @JSProperty("window")
    public static native Window current();

    @JSTopLevel
    @JSProperty("self")
    public static native Window worker();

    @JSTopLevel
    public static native String encodeURI(String uri);

    @JSTopLevel
    public static native String encodeURIComponent(String uri);

    @JSTopLevel
    public static native String decodeURI(String uri);

    @JSTopLevel
    public static native String decodeURIComponent(String uri);

    @JSProperty
    public abstract double getDevicePixelRatio();

    @JSTopLevel
    public static native String atob(String s);

    @JSTopLevel
    public static native String btoa(String s);
}
