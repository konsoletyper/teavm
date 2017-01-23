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

import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.EventListener;

public interface AudioBufferSourceNode extends AudioNode {
    @JSProperty
    AudioBuffer getBuffer();

    @JSProperty
    void setBuffer(AudioBuffer buffer);

    @JSProperty
    AudioParam getPlaybackRate();

    @JSProperty
    AudioParam getDetune();

    @JSProperty
    boolean getLoop();

    @JSProperty
    void setLoop(boolean loop);

    @JSProperty
    double getLoopStart();

    @JSProperty
    void setLoopStart(double start);

    @JSProperty
    double getLoopEnd();

    @JSProperty
    void setLoopEnd(double end);

    @JSProperty("onended")
    void setOnEnded(EventListener<MediaEvent> ent);

    @JSProperty("onended")
    EventListener<MediaEvent> getOnEnded();

    void start(double when, double offset, double duration);

    void start(double when, double offset);

    void start(double when);

    void start();

    void stop(double when);

    void stop();
}

