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

import org.teavm.jso.JSClass;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.EventListener;

@JSClass
public class OfflineAudioContext extends AudioContext {
    @JSProperty("oncomplete")
    public native void setOnComplete(EventListener<OfflineAudioCompletionEvent> event);

    @JSProperty("oncomplete")
    public native EventListener<OfflineAudioCompletionEvent> getOnComplete();

    public native AudioBuffer startRendering();

    @Override
    public native void resume();

    public native void suspend(double suspendTime);
}
