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
import org.teavm.jso.dom.xml.DocumentFragment;

/**
 *
 * @author Junji Takakura
 */
public interface TextTrackCue extends EventTarget {

    @JSProperty
    TextTrack getTrack();

    @JSProperty
    String getId();

    @JSProperty
    void setId(String id);

    @JSProperty
    double getStartTime();

    @JSProperty
    void setStartTime(double startTime);

    @JSProperty
    double getEndTime();

    @JSProperty
    void setEndTime(double endTime);

    @JSProperty
    boolean isPauseOnExit();

    @JSProperty
    void setPauseOnExit(boolean pauseOnExit);

    @JSProperty
    String getVertical();

    @JSProperty
    void setVertical(String vertical);

    @JSProperty
    boolean isSnapToLines();

    @JSProperty
    void setSnapToLines(boolean snapToLines);

    @JSProperty
    int getLine();

    @JSProperty
    void setLine(int line);

    @JSProperty
    int getPosition();

    @JSProperty
    void setPosition(int position);

    @JSProperty
    int getSize();

    @JSProperty
    void setSize(int size);

    @JSProperty
    String getAlign();

    @JSProperty
    void setAlign(String align);

    @JSProperty
    String getText();

    @JSProperty
    void setText(String text);

    DocumentFragment getCueAsHTML();
}
