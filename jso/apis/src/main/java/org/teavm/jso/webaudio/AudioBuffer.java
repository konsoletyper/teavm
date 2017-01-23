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

public interface AudioBuffer extends JSObject {
    @JSProperty
    float getSampleRate();

    @JSProperty
    int getLength();

    @JSProperty
    double getDuration();

    @JSProperty
    int getNumberOfChannels();

    Float32Array getChannelData(int channel);

    void copyFromChannel(Float32Array destination, int channelNumber);

    void copyFromChannel(@JSByRef float[] destination, int channelNumber);

    void copyFromChannel(Float32Array destination, int channelNumber, int startInChannel);

    void copyFromChannel(@JSByRef float[] destination, int channelNumber, int startInChannel);

    void copyToChannel(Float32Array source, int channelNumber);

    void copyToChannel(@JSByRef float[] source, int channelNumber);

    void copyToChannel(Float32Array source, int channelNumber, int startInChannel);

    void copyToChannel(@JSByRef float[] source, int channelNumber, int startInChannel);
}
