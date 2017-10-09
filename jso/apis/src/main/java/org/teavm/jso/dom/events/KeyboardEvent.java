/*
 *  Copyright 2015 Alexey Andreev.
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

import org.teavm.jso.JSProperty;

public interface KeyboardEvent extends Event {
    int DOM_KEY_LOCATION_STANDARD = 0x00;

    int DOM_KEY_LOCATION_LEFT = 0x01;

    int DOM_KEY_LOCATION_RIGHT = 0x02;

    int DOM_KEY_LOCATION_NUMPAD = 0x03;

    @JSProperty
    String getKey();

    @JSProperty
    int getKeyCode();

    @JSProperty
    String getCode();

    @JSProperty
    int getCharCode();

    @JSProperty
    int getLocation();

    @JSProperty
    boolean isCtrlKey();

    @JSProperty
    boolean isShiftKey();

    @JSProperty
    boolean isAltKey();

    @JSProperty
    boolean isMetaKey();

    @JSProperty
    boolean isRepeat();

    @JSProperty("isComposing")
    boolean isComposing();

    boolean getModifierState(String keyArg);
}
