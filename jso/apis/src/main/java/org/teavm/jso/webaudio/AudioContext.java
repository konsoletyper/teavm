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

import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.events.Registration;
import org.teavm.jso.dom.html.HTMLMediaElement;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Float32Array;

@JSClass
public class AudioContext implements JSObject, EventTarget {
    public static final String STATE_SUSPENDED = "suspended";
    public static final String STATE_RUNNING = "running";
    public static final String STATE_CLOSE = "close";

    @JSProperty
    public native AudioDestinationNode getDestination();

    @JSProperty
    public native float getSampleRate();

    @JSProperty
    public native double getCurrentTime();

    @JSProperty
    public native AudioListener getListener();

    @JSProperty
    public native String getState();

    @JSProperty("onstatechange")
    public native void setOnStateChange(EventListener<MediaEvent> listener);

    @JSProperty("onstatechange")
    public native EventListener<MediaEvent> getOnStateChange();

    public final Registration onStateChange(EventListener<MediaEvent> listener) {
        return onEvent("statechange", listener);
    }

    public native void suspend();

    public native void resume();

    public native void close();

    public native AudioBuffer createBuffer(int numberOfChannels, int length, float sampleRate);

    public native AudioBuffer decodeAudioData(ArrayBuffer audioData, DecodeSuccessCallback successCallback,
            DecodeErrorCallback errorCallback);

    public native AudioBuffer decodeAudioData(ArrayBuffer audioData, DecodeSuccessCallback successCallback);

    public native AudioBuffer decodeAudioData(ArrayBuffer audioData);

    public native AudioBufferSourceNode createBufferSource();

    public native MediaElementAudioSourceNode createMediaElementSource(HTMLMediaElement mediaElement);

    public native MediaStreamAudioSourceNode createMediaStreamSource(MediaStream mediaStream);

    public native MediaStreamAudioDestinationNode createMediaStreamDestination();

    public native AudioWorker createAudioWorker();

    public native ScriptProcessorNode createScriptProcessor(int bufferSize, int numberOfInputChannels,
            int numberOfOutputChannels);

    public native ScriptProcessorNode createScriptProcessor(int bufferSize, int numberOfInputChannels);

    public native ScriptProcessorNode createScriptProcessor(int bufferSize);

    public native ScriptProcessorNode createScriptProcessor();

    public native AnalyserNode createAnalyser();

    public native GainNode createGain();

    public native DelayNode createDelay(double maxDelayTime);

    public native DelayNode createDelay();

    public native BiquadFilterNode createBiquadFilter();

    public native IIRFilterNode createIIRFilter(Float32Array feedforward, Float32Array feedback);

    public native WaveShaperNode createWaveShaper();

    public native PannerNode createPanner();

    public native StereoPannerNode createStereoPanner();

    public native ConvolverNode createConvolver();

    public native ChannelSplitterNode createChannelSplitter(int numberOfOutputs);

    public native ChannelSplitterNode createChannelSplitter();

    public native ChannelMergerNode createChannelMerger(int numberOfInputs);

    public native ChannelMergerNode createChannelMerger();

    public native DynamicsCompressorNode createDynamicsCompressor();

    public native OscillatorNode createOscillator();

    public native PeriodicWave createPeriodicWave(Float32Array real, Float32Array image,
            PeriodicWaveConstraints constraints);

    public native PeriodicWave createPeriodicWave(@JSByRef(optional = true) float[] real,
            @JSByRef(optional = true) float[] image,
            PeriodicWaveConstraints constraints);

    public native PeriodicWave createPeriodicWave(Float32Array real, Float32Array image);

    public native PeriodicWave createPeriodicWave(@JSByRef(optional = true) float[] real,
            @JSByRef(optional = true) float[] image);

    @JSBody(script = "return new Context();")
    @Deprecated
    public static native AudioContext create();

    @Override
    public native void addEventListener(String type, EventListener<?> listener, boolean useCapture);

    @Override
    public native void addEventListener(String type, EventListener<?> listener);

    @Override
    public native void removeEventListener(String type, EventListener<?> listener, boolean useCapture);

    @Override
    public native void removeEventListener(String type, EventListener<?> listener);

    @Override
    public native boolean dispatchEvent(Event evt);
}
