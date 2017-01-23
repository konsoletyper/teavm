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

    public abstract void suspend();

    public abstract void resume();

    public abstract void close();

    public abstract AudioBuffer createBuffer(int numberOfChannels, int length, float sampleRate);

    public abstract AudioBuffer decodeAudioData(ArrayBuffer audioData, DecodeSuccessCallback successCallback,
            DecodeErrorCallback errorCallback);

    public abstract AudioBuffer decodeAudioData(ArrayBuffer audioData, DecodeSuccessCallback successCallback);

    public abstract AudioBuffer decodeAudioData(ArrayBuffer audioData);

    public abstract AudioBufferSourceNode createBufferSource();

    public abstract MediaElementAudioSourceNode createMediaElementSource(HTMLMediaElement mediaElement);

    public abstract MediaStreamAudioSourceNode createMediaStreamSource(MediaStream mediaStream);

    public abstract MediaStreamAudioDestinationNode createMediaStreamDestination();

    public abstract AudioWorker createAudioWorker();

    public abstract ScriptProcessorNode createScriptProcessor(int bufferSize, int numberOfInputChannels,
            int numberOfOutputChannels);

    public abstract ScriptProcessorNode createScriptProcessor(int bufferSize, int numberOfInputChannels);

    public abstract ScriptProcessorNode createScriptProcessor(int bufferSize);

    public abstract ScriptProcessorNode createScriptProcessor();

    public abstract AnalyserNode createAnalyser();

    public abstract GainNode createGain();

    public abstract DelayNode createDelay(double maxDelayTime);

    public abstract DelayNode createDelay();

    public abstract BiquadFilterNode createBiquadFilter();

    public abstract IIRFilterNode createIIRFilter(Float32Array feedforward, Float32Array feedback);

    public abstract WaveShaperNode createWaveShaper();

    public abstract PannerNode createPanner();

    public abstract StereoPannerNode createStereoPanner();

    public abstract ConvolverNode createConvolver();

    public abstract ChannelSplitterNode createChannelSplitter(int numberOfOutputs);

    public abstract ChannelSplitterNode createChannelSplitter();

    public abstract ChannelMergerNode createChannelMerger(int numberOfInputs);

    public abstract ChannelMergerNode createChannelMerger();

    public abstract DynamicsCompressorNode createDynamicsCompressor();

    public abstract OscillatorNode createOscillator();

    public abstract PeriodicWave createPeriodicWave(Float32Array real, Float32Array image,
            PeriodicWaveConstraints constraints);

    public abstract PeriodicWave createPeriodicWave(@JSByRef float[] real, @JSByRef float[] image,
            PeriodicWaveConstraints constraints);

    public abstract PeriodicWave createPeriodicWave(Float32Array real, Float32Array image);

    public abstract PeriodicWave createPeriodicWave(@JSByRef float[] real, @JSByRef float[] image);

    @JSBody(script = "var Context = window.AudioContext || window.webkitAudioContext; return new Context();")
    public static native AudioContext create();
}
