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
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLMediaElement;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Float32Array;

public abstract class AudioContext implements JSObject {
    public static final String STATE_SUSPENDED = "suspended";
    public static final String STATE_RUNNING = "running";
    public static final String STATE_CLOSE = "close";

    @JSProperty
    public abstract AudioDestinationNode getDestination();

    @JSProperty
    public abstract float getSampleRate();

    @JSProperty
    public abstract double getCurrentTime();

    @JSProperty
    public abstract AudioListener getListener();

    @JSProperty
    public abstract String getState();

    @JSProperty("onstatechange")
    public abstract void setOnStateChange(EventListener<MediaEvent> listener);

    @JSProperty("onstatechange")
    public abstract EventListener<MediaEvent> getOnStateChange();

    @JSMethod
    public abstract void suspend();

    @JSMethod
    public abstract void resume();

    @JSMethod
    public abstract void close();

    @JSMethod
    public abstract AudioBuffer createBuffer(int numberOfChannels, int length, float sampleRate);

    @JSMethod
    public abstract AudioBuffer decodeAudioData(ArrayBuffer audioData, DecodeSuccessCallback successCallback,
            DecodeErrorCallback errorCallback);

    @JSMethod
    public abstract AudioBuffer decodeAudioData(ArrayBuffer audioData, DecodeSuccessCallback successCallback);

    @JSMethod
    public abstract AudioBuffer decodeAudioData(ArrayBuffer audioData);

    @JSMethod
    public abstract AudioBufferSourceNode createBufferSource();

    @JSMethod
    public abstract MediaElementAudioSourceNode createMediaElementSource(HTMLMediaElement mediaElement);

    @JSMethod
    public abstract MediaStreamAudioSourceNode createMediaStreamSource(MediaStream mediaStream);

    @JSMethod
    public abstract MediaStreamAudioDestinationNode createMediaStreamDestination();

    @JSMethod
    public abstract AudioWorker createAudioWorker();

    @JSMethod
    public abstract ScriptProcessorNode createScriptProcessor(int bufferSize, int numberOfInputChannels,
            int numberOfOutputChannels);

    @JSMethod
    public abstract ScriptProcessorNode createScriptProcessor(int bufferSize, int numberOfInputChannels);

    @JSMethod
    public abstract ScriptProcessorNode createScriptProcessor(int bufferSize);

    @JSMethod
    public abstract ScriptProcessorNode createScriptProcessor();

    @JSMethod
    public abstract AnalyserNode createAnalyser();

    @JSMethod
    public abstract GainNode createGain();

    @JSMethod
    public abstract DelayNode createDelay(double maxDelayTime);

    @JSMethod
    public abstract DelayNode createDelay();

    @JSMethod
    public abstract BiquadFilterNode createBiquadFilter();

    @JSMethod
    public abstract IIRFilterNode createIIRFilter(Float32Array feedforward, Float32Array feedback);

    @JSMethod
    public abstract WaveShaperNode createWaveShaper();

    @JSMethod
    public abstract PannerNode createPanner();

    @JSMethod
    public abstract StereoPannerNode createStereoPanner();

    @JSMethod
    public abstract ConvolverNode createConvolver();

    @JSMethod
    public abstract ChannelSplitterNode createChannelSplitter(int numberOfOutputs);

    @JSMethod
    public abstract ChannelSplitterNode createChannelSplitter();

    @JSMethod
    public abstract ChannelMergerNode createChannelMerger(int numberOfInputs);

    @JSMethod
    public abstract ChannelMergerNode createChannelMerger();

    @JSMethod
    public abstract DynamicsCompressorNode createDynamicsCompressor();

    @JSMethod
    public abstract OscillatorNode createOscillator();

    @JSMethod
    public abstract PeriodicWave createPeriodicWave(Float32Array real, Float32Array image,
            PeriodicWaveConstraints constraints);

    @JSMethod
    public abstract PeriodicWave createPeriodicWave(Float32Array real, Float32Array image);

    @JSBody(params = {},
            script = "var Context = window.AudioContext || window.webkitAudioContext; return new Context();")
    public static native AudioContext create();
}
