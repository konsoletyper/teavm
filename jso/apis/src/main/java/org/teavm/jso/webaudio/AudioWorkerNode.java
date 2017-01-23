/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.jso.webaudio;

import org.teavm.jso.JSByRef;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.EventListener;

public interface AudioWorkerNode extends AudioNode {
    @JSProperty("onmessage")
    void setOnMessage(EventListener<MediaEvent> event);

    @JSProperty("onmessage")
    EventListener<MediaEvent> getOnMessage();

    void postMessage(JSObject message, @JSByRef JSObject[] transfer);

    void postMessage(JSObject message, JSObject transfer);

    void postMessage(JSObject message);
}

