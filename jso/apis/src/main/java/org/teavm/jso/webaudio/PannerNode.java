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

public interface PannerNode extends AudioNode {
    String MODEL_EQUALPOWER = "equalpower";
    String MODEL_HRTF = "HRTF";

    String DISTANCE_MODEL_LINEAR = "linear";
    String DISTANCE_MODEL_INVERSE = "inverse";
    String DISTANCE_MODEL_EXPONENTIAL = "exponential";

    @JSProperty
    void setPanningModel(String value);

    @JSProperty
    String getPanningModel();

    @JSProperty
    void setDistanceModel(String value);

    @JSProperty
    String getDistanceModel();

    @JSProperty
    void setRefDistance(float value);

    @JSProperty
    float getRefDistance();

    @JSProperty
    void setMaxDistance(float value);

    @JSProperty
    float getMaxDistance();

    @JSProperty
    void setRolloffFactor(float value);

    @JSProperty
    float getRolloffFactor();

    @JSProperty
    void setConeInnerAngle(float value);

    @JSProperty
    float getConeInnerAngle();

    @JSProperty
    void setConeOuterAngle(float value);

    @JSProperty
    float getConeOuterAngle();

    @JSProperty
    void setConeOuterGain(float value);

    @JSProperty
    float getConeOuterGain();

    void setPosition(float x, float y, float z);

    void setOrientation(float x, float y, float z);

    void setVelocity(float x, float y, float z);
}

