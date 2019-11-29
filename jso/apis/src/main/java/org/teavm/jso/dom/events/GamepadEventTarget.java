/*
 *  Copyright 2019 devnewton.
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

import org.teavm.jso.gamepad.GamepadEvent;

public interface GamepadEventTarget extends EventTarget {
    default void listenGamepadConnected(EventListener<GamepadEvent> listener) {
        addEventListener("gamepadconnected", listener);
    }

    default void neglectGamepadConnected(EventListener<GamepadEvent> listener) {
        removeEventListener("gamepadconnected", listener);
    }

    default void listenGamepadDisconnected(EventListener<GamepadEvent> listener) {
        addEventListener("gamepaddisconnected", listener);
    }

    default void neglectGamepadDisconnected(EventListener<GamepadEvent> listener) {
        removeEventListener("gamepaddisconnected", listener);
    }

}
