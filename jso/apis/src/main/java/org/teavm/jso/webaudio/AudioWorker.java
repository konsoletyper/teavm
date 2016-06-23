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
package org.teavm.jso.webaudio;

import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.EventListener;

/**
 *
 */
public interface AudioWorker extends JSObject {
    @JSProperty
    AudioWorkerParamDescriptor[] getParameters();

    @JSProperty(value = "onmessage")
    void setOnMessage(EventListener event);

    @JSProperty(value = "onmessage")
    EventListener getOnMessage();

    @JSProperty(value = "onloaded")
    void setOnLoaded(EventListener event);

    @JSProperty(value = "onloaded")
    EventListener getOnLoaded();

    @JSMethod
    void terminate();

    @JSMethod
    void postMessage(Object message, Object... transfer);

    @JSMethod
    void postMessage(Object message);

    @JSMethod
    AudioWorkerNode createNode(int numberOfInputs, int numberOfOutputs);

    @JSMethod
    AudioParam addParameter(String name, float defaultValue);

    @JSMethod
    void removeParameter(String name);
}
