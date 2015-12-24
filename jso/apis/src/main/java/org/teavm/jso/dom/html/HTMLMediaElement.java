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
package org.teavm.jso.dom.html;

import java.util.Date;
import org.teavm.jso.JSProperty;
import org.teavm.jso.media.AudioTrackList;
import org.teavm.jso.media.MediaController;
import org.teavm.jso.media.MediaError;
import org.teavm.jso.media.TextTrack;
import org.teavm.jso.media.TextTrackList;
import org.teavm.jso.media.TimeRanges;
import org.teavm.jso.media.VideoTrackList;

/**
 *
 * @author Junji Takakura
 */
public interface HTMLMediaElement extends HTMLElement {
    int HAVE_NOTHING = 0;
    int HAVE_METADATA = 1;
    int HAVE_CURRENT_DATA = 2;
    int HAVE_FUTURE_DATA = 3;
    int HAVE_ENOUGH_DATA = 4;

    int NETWORK_EMPTY = 0;
    int NETWORK_IDLE = 1;
    int NETWORK_LOADING = 2;
    int NETWORK_NO_SOURCE = 3;

    @JSProperty
    MediaError getError();

    @JSProperty
    String getSrc();

    @JSProperty
    void setSrc(String src);

    @JSProperty
    String getCurrentSrc();

    @JSProperty
    String getCrossOrigin();

    @JSProperty
    void setCrossOrigin(String crossOrigin);

    @JSProperty
    int getNetworkState();

    @JSProperty
    String getPreload();

    @JSProperty
    void setPreload(String preload);

    @JSProperty
    TimeRanges getBuffered();

    @JSProperty
    int getReadyState();

    @JSProperty
    boolean isSeeking();

    @JSProperty
    double getCurrentTime();

    @JSProperty
    void setCurrentTime(double currentTime);

    default void addCurrentTime(double delta) {
        setCurrentTime(getCurrentTime() + delta);
    }

    @JSProperty
    double getDuration();

    @JSProperty
    Date getStartDate();

    @JSProperty
    boolean isPaused();

    @JSProperty
    double getDefaultPlaybackRate();

    @JSProperty
    void setDefaultPlaybackRate(double defaultPlaybackRate);

    @JSProperty
    double getPlaybackRate();

    @JSProperty
    void setPlaybackRate(double playbackRate);

    default void addPlaybackRate(double delta) {
        setPlaybackRate(getPlaybackRate() + delta);
    }

    @JSProperty
    TimeRanges getPlayed();

    @JSProperty
    TimeRanges getSeekable();

    @JSProperty
    boolean isEnded();

    @JSProperty
    boolean isAutoplay();

    @JSProperty
    void setAutoplay(boolean autoplay);

    @JSProperty
    boolean isLoop();

    @JSProperty
    void setLoop(boolean loop);

    @JSProperty
    String getMediaGroup();

    @JSProperty
    void setMediaGroup(String mediaGroup);

    @JSProperty
    MediaController getController();

    @JSProperty
    void setController(MediaController controller);

    @JSProperty
    boolean isControls();

    @JSProperty
    void setControls(boolean controls);

    @JSProperty
    float getVolume();

    @JSProperty
    void setVolume(float volume);

    default void addVolume(float delta) {
        setVolume(getVolume() + delta);
    }

    @JSProperty
    boolean isMuted();

    @JSProperty
    void setMuted(boolean muted);

    @JSProperty
    boolean isDefaultMuted();

    @JSProperty
    void setDefaultMuted(boolean defaultMuted);

    AudioTrackList getAudioTracks();

    VideoTrackList getVideoTracks();

    TextTrackList getTextTracks();

    TextTrack addTextTrack(String kind);

    TextTrack addTextTrack(String kind, String label);

    TextTrack addTextTrack(String kind, String label, String language);

    void play();

    void pause();

    void load();

    String canPlayType(String type);
}
