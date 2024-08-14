/*
 *  Copyright 2024 Alexey Andreev.
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

public interface TouchEventTarget extends EventTarget {
    /**
     * The touchstart event is fired when one or more touch points are placed on the touch surface.
     */
    default Registration onTouchStart(EventListener<TouchEvent> listener) {
        return onEvent("touchstart", listener);
    }

    /**
     * The touchend event fires when one or more touch points are removed from the touch surface.
     * Remember that it is possible to get a touchcancel event instead.
     */
    default Registration onTouchEnd(EventListener<TouchEvent> listener) {
        return onEvent("touchend", listener);
    }

    /**
     * <p>The touchcancel event is fired when one or more touch points have been disrupted in an
     * implementation-specific manner.</p>
     *
     * <p>Some examples of situations that will trigger a touchcancel event:</p>
     *
     * <ul>
     *  <li>A hardware event occurs that cancels the touch activities.
     *      This may include, for example, the user switching applications using an application switcher
     *      interface or the "home" button on a mobile device.</li>
     *  <li>The device's screen orientation is changed while the touch is active.</li>
     *  <li>The browser decides that the user started touch input accidentally.
     *      This can happen if, for example, the hardware supports palm rejection to prevent a hand resting on
     *      the display while using a stylus from accidentally triggering events.</li>
     *  <li>The {@code touch-action} CSS property prevents the input from continuing.
     *      When the user interacts with too many fingers simultaneously, the browser can fire this event
     *      for all existing pointers (even if the user is still touching the screen).</li>
     * </ul>
     */
    default Registration onTouchCancel(EventListener<TouchEvent> listener) {
        return onEvent("touchcancel", listener);
    }

    /**
     * The touchmove event is fired when one or more touch points are moved along the touch surface.
     */
    default Registration onTouchMove(EventListener<TouchEvent> listener) {
        return onEvent("touchmove", listener);
    }
}
