/*
 *  Copyright 2024 pizzadox9999, Alexey Andreev.
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
import org.teavm.jso.core.JSArrayReader;

/**
 * <p></p>The <strong>TouchEvent</strong> interface represents an UIEvent which is sent when the state
 * of contacts with a touch-sensitive surface changes.
 * This surface can be a touch screen or trackpad, for example.
 * The event can describe one or more points of contact with the screen and includes support
 * for detecting movement, addition and removal of contact points, and so forth.</p>
 *
 * <p>Touches are represented by the {@link Touch} object; each touch is described by a position,
 * size and shape, amount of pressure, and target element.</p>
 */
public interface TouchEvent extends Event {
    /**
     * A {@code boolean} value indicating whether the alt key was down when the touch
     * event was fired.
     */
    @JSProperty
    boolean isAltKey();

    /**
     * A list of all the {@link Touch} objects representing individual points of
     * contact whose states changed between the previous touch event and this one.
     */
    @JSProperty
    JSArrayReader<Touch> getChangedTouches();

    /**
     * A {@code boolean} value indicating whether the control key was down when the
     * touch event was fired.
     */
    @JSProperty
    boolean isCtrlKey();

    /**
     * A {@code boolean} value indicating whether the meta key was down when the
     * touch event was fired.
     */
    @JSProperty
    boolean isMetaKey();

    /**
     * A {@code boolean} value indicating whether the shift key was down when the
     * touch event was fired.
     */
    @JSProperty
    boolean isShiftKey();

    /**
     * A list of all the {@link Touch} objects that are both currently in contact with
     * the touch surface and were also started on the same element that is the
     * target of the event.
     */
    @JSProperty
    JSArrayReader<Touch> getTargetTouches();

    /**
     * A list of all the  {@link Touch} objects representing all current points of
     * contact with the surface, regardless of target or changed status.
     */
    @JSProperty
    JSArrayReader<Touch> getTouches();
}