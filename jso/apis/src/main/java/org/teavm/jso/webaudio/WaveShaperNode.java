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
import org.teavm.jso.typedarrays.Float32Array;

public interface WaveShaperNode extends AudioNode {
    String OVERSAMPLE_NONE = "none";
    String OVERSAMPLE_2X = "2x";
    String OVERSAMPLE_4X = "4x";

    @JSProperty
    void setCurve(Float32Array value);

    @JSProperty
    Float32Array getCurve();

    @JSProperty
    void setOversample(String value);

    @JSProperty
    String getOversample();
}
