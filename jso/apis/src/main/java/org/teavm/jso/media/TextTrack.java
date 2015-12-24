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

import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.EventTarget;

/**
 *
 * @author Junji Takakura
 */
public interface TextTrack extends EventTarget {

    String DISABLED = "disabled";
    String HIDDEN = "hidden";
    String SHOWING = "showing";

    @JSProperty
    String getId();

    @JSProperty
    String getLabel();

    @JSProperty
    String getKind();

    @JSProperty
    String getLanguage();

    @JSProperty
    String getMode();

    @JSProperty
    void setMode(String mode);

    @JSProperty
    TextTrackCueList getCues();

    @JSProperty
    TextTrackCueList getActiveCues();

    void addCue(TextTrackCue cue);

    void removeCue(TextTrackCue cue);
}
