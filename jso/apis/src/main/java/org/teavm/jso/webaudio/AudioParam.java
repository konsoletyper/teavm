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
import org.teavm.jso.typedarrays.Float32Array;

public interface AudioParam extends JSObject {
    @JSProperty
    float getValue();

    @JSProperty
    void setValue(float value);

    @JSProperty
    float getDefaultValue();

    void setValueAtTime(float value, double startTime);

    void linearRampToValueAtTime(float value, double endTime);

    void exponentialRampToValueAtTime(float value, double endTime);

    void setTargetAtTime(float target, double startTime, float timeConstant);

    void setValueCurveAtTime(Float32Array values, double startTime, double duration);

    void setValueCurveAtTime(@JSByRef float[] values, double startTime, double duration);

    void cancelScheduledValues(double startTime);
}
