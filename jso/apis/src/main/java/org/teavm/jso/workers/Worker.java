/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.jso.workers;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSClass;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MessageEvent;
import org.teavm.jso.dom.events.Registration;

@JSClass
public class Worker implements AbstractWorker {
    public Worker(String url) {
    }

    @JSBody(params = "url", script = "return new Worker(url);")
    @Deprecated
    public static native Worker create(String url);

    public Registration onMessage(EventListener<MessageEvent> listener) {
        return onEvent("message", listener);
    }

    public native void postMessage(Object message);

    public native void terminate();

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
