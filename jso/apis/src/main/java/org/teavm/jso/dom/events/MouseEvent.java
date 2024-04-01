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
package org.teavm.jso.dom.events;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface MouseEvent extends Event {
    short LEFT_BUTTON = 0;
    short MIDDLE_BUTTON = 1;
    short RIGHT_BUTTON = 2;
    String CLICK = "click";
    String MOUSEDOWN = "mousedown";
    String MOUSEUP = "mouseup";
    String MOUSEOVER = "mouseover";
    String MOUSEMOVE = "mousemove";
    String MOUSEOUT = "mouseout";

    @JSProperty
    int getScreenX();

    @JSProperty
    int getScreenY();

    @JSProperty
    int getClientX();

    @JSProperty
    int getClientY();

    @JSProperty
    int getOffsetX();

    @JSProperty
    int getOffsetY();

    @JSProperty
    int getPageX();

    @JSProperty
    int getPageY();

    @JSProperty
    boolean getCtrlKey();

    @JSProperty
    boolean getShiftKey();

    @JSProperty
    boolean getAltKey();

    @JSProperty
    boolean getMetaKey();

    @JSProperty
    short getButton();

    @JSProperty
    short getButtons();

    @JSProperty
    EventTarget getRelatedTarget();

    @JSProperty
    double getMovementX();

    @JSProperty
    double getMovementY();

    void initMouseEvent(String type, boolean canBubble, boolean cancelable, JSObject view, int detail, int screenX,
            int screenY, int clientX, int clientY, boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
            short button, EventTarget relatedTarget);
}
