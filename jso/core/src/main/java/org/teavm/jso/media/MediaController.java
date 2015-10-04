/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.jso.media;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Junji Takakura
 */
public interface MediaController extends JSObject {

    @JSProperty
    TimeRanges getBuffered();

    @JSProperty
    double getCurrentTime();

    @JSProperty
    void setCurrentTime(double currentTime);

    @JSProperty
    double getDefaultPlaybackRate();

    @JSProperty
    void setDefaultPlaybackRate(double defaultPlaybackRate);

    @JSProperty
    double getDuration();

    @JSProperty
    boolean isMuted();

    @JSProperty
    void setMuted(boolean muted);

    @JSProperty
    boolean isPaused();

    @JSProperty
    double getPlaybackRate();

    @JSProperty
    void setPlaybackRate(double playbackRate);

    @JSProperty
    String getPlaybackState();

    @JSProperty
    TimeRanges getPlayed();

    @JSProperty
    int getReadyState();

    @JSProperty
    TimeRanges getSeekable();

    @JSProperty
    float getVolume();

    @JSProperty
    void setVolume(float volume);

    void play();

    void pause();
}
