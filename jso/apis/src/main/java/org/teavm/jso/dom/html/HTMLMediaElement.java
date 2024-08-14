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
import org.teavm.jso.JSClass;
import org.teavm.jso.JSProperty;
import org.teavm.jso.media.AudioTrackList;
import org.teavm.jso.media.MediaController;
import org.teavm.jso.media.MediaError;
import org.teavm.jso.media.TextTrack;
import org.teavm.jso.media.TextTrackList;
import org.teavm.jso.media.TimeRanges;
import org.teavm.jso.media.VideoTrackList;

@JSClass
public abstract class HTMLMediaElement extends HTMLElement {
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
    public abstract MediaError getError();

    @JSProperty
    public abstract String getSrc();

    @JSProperty
    public abstract void setSrc(String src);

    @JSProperty
    public abstract String getCurrentSrc();

    @JSProperty
    public abstract String getCrossOrigin();

    @JSProperty
    public abstract void setCrossOrigin(String crossOrigin);

    @JSProperty
    public abstract int getNetworkState();

    @JSProperty
    public abstract String getPreload();

    @JSProperty
    public abstract void setPreload(String preload);

    @JSProperty
    public abstract TimeRanges getBuffered();

    @JSProperty
    public abstract int getReadyState();

    @JSProperty
    public abstract boolean isSeeking();

    @JSProperty
    public abstract double getCurrentTime();

    @JSProperty
    public abstract void setCurrentTime(double currentTime);

    public final void addCurrentTime(double delta) {
        setCurrentTime(getCurrentTime() + delta);
    }

    @JSProperty
    public abstract double getDuration();

    @JSProperty
    public abstract Date getStartDate();

    @JSProperty
    public abstract boolean isPaused();

    @JSProperty
    public abstract double getDefaultPlaybackRate();

    @JSProperty
    public abstract void setDefaultPlaybackRate(double defaultPlaybackRate);

    @JSProperty
    public abstract double getPlaybackRate();

    @JSProperty
    public abstract void setPlaybackRate(double playbackRate);

    public final void addPlaybackRate(double delta) {
        setPlaybackRate(getPlaybackRate() + delta);
    }

    @JSProperty
    public abstract TimeRanges getPlayed();

    @JSProperty
    public abstract TimeRanges getSeekable();

    @JSProperty
    public abstract boolean isEnded();

    @JSProperty
    public abstract boolean isAutoplay();

    @JSProperty
    public abstract void setAutoplay(boolean autoplay);

    @JSProperty
    public abstract boolean isLoop();

    @JSProperty
    public abstract void setLoop(boolean loop);

    @JSProperty
    public abstract String getMediaGroup();

    @JSProperty
    public abstract void setMediaGroup(String mediaGroup);

    @JSProperty
    public abstract MediaController getController();

    @JSProperty
    public abstract void setController(MediaController controller);

    @JSProperty
    public abstract boolean isControls();

    @JSProperty
    public abstract void setControls(boolean controls);

    @JSProperty
    public abstract float getVolume();

    @JSProperty
    public abstract void setVolume(float volume);

    public final void addVolume(float delta) {
        setVolume(getVolume() + delta);
    }

    @JSProperty
    public abstract boolean isMuted();

    @JSProperty
    public abstract void setMuted(boolean muted);

    @JSProperty
    public abstract boolean isDefaultMuted();

    @JSProperty
    public abstract void setDefaultMuted(boolean defaultMuted);

    public abstract AudioTrackList getAudioTracks();

    public abstract VideoTrackList getVideoTracks();

    public abstract TextTrackList getTextTracks();

    public abstract TextTrack addTextTrack(String kind);

    public abstract TextTrack addTextTrack(String kind, String label);

    public abstract TextTrack addTextTrack(String kind, String label, String language);

    public abstract void play();

    public abstract void pause();

    public abstract void load();

    public abstract String canPlayType(String type);
}
