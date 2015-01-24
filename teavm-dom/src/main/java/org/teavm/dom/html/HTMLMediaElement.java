/*
 * Copyright 2015 Alexey Andreev.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teavm.dom.html;

import org.teavm.dom.media.VideoTrackList;
import java.util.Date;
import org.teavm.dom.media.AudioTrackList;
import org.teavm.dom.media.MediaController;
import org.teavm.dom.media.MediaError;
import org.teavm.dom.media.TextTrack;
import org.teavm.dom.media.TextTrackList;
import org.teavm.dom.media.TimeRanges;
import org.teavm.jso.JSProperty;

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
