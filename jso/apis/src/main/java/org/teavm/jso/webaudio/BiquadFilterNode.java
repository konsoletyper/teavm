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
import org.teavm.jso.JSProperty;
import org.teavm.jso.typedarrays.Float32Array;

public interface BiquadFilterNode extends AudioNode {
    String TYPE_LOW_PASS = "lowpass";
    String TYPE_LOW_SHELF = "lowshelf";
    String TYPE_HIGH_SHELF = "highshelf";
    String TYPE_HIGH_PASS = "highpass";
    String TYPE_BAND_PASS = "bandpass";
    String TYPE_PEAKING = "peaking";
    String TYPE_NOTCH = "notch";
    String TYPE_ALL_PASS = "allpass";

    @JSProperty
    void setType(String type);

    @JSProperty
    String getType();

    @JSProperty
    AudioParam getFrequency();

    @JSProperty
    AudioParam getDetune();

    @JSProperty("Q")
    AudioParam getQ();

    @JSProperty
    AudioParam getGain();

    void getFrequencyResponse(Float32Array frequencyHz, Float32Array magResponse, Float32Array phaseResponse);

    void getFrequencyResponse(@JSByRef float[] frequencyHz, @JSByRef float[] magResponse,
            @JSByRef float[] phaseResponse);
}
