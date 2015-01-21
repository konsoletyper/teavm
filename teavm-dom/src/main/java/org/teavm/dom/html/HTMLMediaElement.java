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

    public static final int HAVE_NOTHING = 0;
    public static final int HAVE_METADATA = 1;
    public static final int HAVE_CURRENT_DATA = 2;
    public static final int HAVE_FUTURE_DATA = 3;
    public static final int HAVE_ENOUGH_DATA = 4;

    public static final int NETWORK_EMPTY = 0;
    public static final int NETWORK_IDLE = 1;
    public static final int NETWORK_LOADING = 2;
    public static final int NETWORK_NO_SOURCE = 3;

    @JSProperty
    MediaError getError();

    @JSProperty
    String getSrc();

    @JSProperty
    void setSrc(String src);

    @JSProperty
    String getCurrentSrc();

    @JSProperty
    void setCurrentSrc(String currentSrc);

    @JSProperty
    String getCrossOrigin();

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
    Date getStartOffsetTime();

    @JSProperty
    boolean getPaused();

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
    boolean getEnded();

    @JSProperty
    boolean isAutoplay();

    @JSProperty
    void setAutoplay(boolean autoplay);
    
    boolean isLoop();
    
    void setLoop(boolean loop);
    
    @JSProperty
    String getMediaGroup();

    @JSProperty
    MediaController getController();

    @JSProperty
    void setController(MediaController controller);    

    @JSProperty
    boolean getControlls();

    @JSProperty
    void setControlls(boolean controlls);

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
