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

import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface AudioNode extends JSObject {
    String CHANNEL_COUNT_MODE_MAX = "max";
    String CHANNEL_COUNT_MODE_CLAMPED_MAX = "clamped-max";
    String CHANNEL_COUNT_MODE_EXPLICIT = "explicit";

    String CHANNEL_INTERPRETATION_SPEAKERS = "speakers";
    String CHANNEL_INTERPRETATION_DISCRETE = "discrete";

    @JSMethod
    void connect(AudioNode destination, int output, int input);

    @JSMethod
    void connect(AudioNode destination, int output);

    @JSMethod
    void connect(AudioNode destination);

    @JSMethod
    void connect(AudioParam destination, int output);

    @JSMethod
    void connect(AudioParam destination);

    @JSMethod
    void disconnect();

    @JSMethod
    void disconnect(int output);

    @JSMethod
    void disconnect(AudioNode destination);

    @JSMethod
    void disconnect(AudioNode destination, int output);

    @JSMethod
    void disconnect(AudioNode destination, int output, int input);

    @JSMethod
    void disconnect(AudioParam destination);

    @JSMethod
    void disconnect(AudioParam destination, int output);

    @JSProperty
    AudioContext getContext();

    @JSProperty
    int getNumberOfInputs();

    @JSProperty
    int getNumberOfOutputs();

    @JSProperty
    int getChannelCount();

    @JSProperty
    String getChannelCountMode();

    @JSProperty
    String getChannelInterpretation();
}
