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

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 * <p></p>The Touch interface represents a single contact point on a touch-sensitive device.
 * The contact point is commonly a finger or stylus and the device may be a touchscreen or trackpad.</p>
 *
 * <p>The {@link #getRadiusX()}, {@link #getRadiusY()}, and {@link #getRotationAngle()}
 * describe the area of contact between the user and the screen, the touch area.
 * This can be helpful when dealing with imprecise pointing devices such as fingers.
 * These values are set to describe an ellipse that as closely as possible matches
 * the entire area of contact (such as the user's fingertip).</p>
 */
public interface Touch extends JSObject {
    /**
     * Returns a unique identifier for this Touch object. A given touch point (say,
     * by a finger) will have the same identifier for the duration of its movement
     * around the surface. This lets you ensure that you're tracking the same touch
     * all the time.
     */
    @JSProperty
    int getIdentifier();

    /**
     * Returns the X coordinate of the touch point relative to the left edge of the
     * screen.
     */
    @JSProperty
    double getScreenX();

    /**
     * Returns the Y coordinate of the touch point relative to the top edge of the
     * screen.
     */
    @JSProperty
    double getScreenY();

    /**
     * Returns the X coordinate of the touch point relative to the left edge of the
     * browser viewport, not including any scroll offset.
     */
    @JSProperty
    double getClientX();

    /**
     * Returns the Y coordinate of the touch point relative to the top edge of the
     * browser viewport, not including any scroll offset.
     */
    @JSProperty
    double getClientY();

    /**
     * Returns the X coordinate of the touch point relative to the left edge of the
     * document. Unlike clientX, this value includes the horizontal scroll offset,
     * if any.
     */
    @JSProperty
    double getPageX();

    /**
     * Returns the Y coordinate of the touch point relative to the top of the
     * document. Unlike clientY, this value includes the vertical scroll offset, if
     * any.
     */
    @JSProperty
    double getPageY();

    /**
     * Returns the {@link TouchEventTarget} on which the touch point started when it was first placed
     * on the surface, even if the touch point has since moved outside the
     * interactive area of that element or even been removed from the document.
     */
    @JSProperty
    TouchEventTarget getTarget();

    /**
     * Returns the X radius of the ellipse that most closely circumscribes the area
     * of contact with the screen. The value is in pixels of the same scale as
     * screenX.
     */
    @JSProperty
    double getRadiusX();

    /**
     * Returns the Y radius of the ellipse that most closely circumscribes the area
     * of contact with the screen. The value is in pixels of the same scale as
     * screenY.
     */
    @JSProperty
    double getRadiusY();

    /**
     * Returns the angle (in degrees) that the ellipse described by radiusX and
     * radiusY must be rotated, clockwise, to most accurately cover the area of
     * contact between the user and the surface.
     */
    @JSProperty
    double getRotationAngle();

    /**
     * Returns the amount of pressure being applied to the surface by the user, as a
     * float between 0.0 (no pressure) and 1.0 (maximum pressure).
     */
    @JSProperty
    double getForce();
}