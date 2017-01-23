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

import org.teavm.jso.JSByRef;
import org.teavm.jso.JSProperty;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Uint8Array;

public interface AnalyserNode extends AudioNode {
    @JSProperty
    void setFftSize(int size);

    @JSProperty
    int getFftSize();

    @JSProperty
    int getFrequencyBinCount();

    @JSProperty
    void setMinDecibels(float value);

    @JSProperty
    float getMinDecibels();

    @JSProperty
    void setMaxDecibels(float value);

    @JSProperty
    float getMaxDecibels();

    @JSProperty
    void setSmoothingTimeConstant(float value);

    @JSProperty
    float getSmoothingTimeConstant();

    void getFloatFrequencyData(Float32Array array);

    void getFloatFrequencyData(@JSByRef float[] array);

    void getByteFrequencyData(Uint8Array array);

    void getFloatTimeDomainData(Float32Array array);

    void getFloatTimeDomainData(@JSByRef float[] array);

    void getByteTimeDomainData(Uint8Array array);
}

