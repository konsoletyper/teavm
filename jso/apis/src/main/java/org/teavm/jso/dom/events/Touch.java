package org.teavm.jso.dom.events;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.html.HTMLElement;

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
    int getScreenX();

    /**
     * Returns the Y coordinate of the touch point relative to the top edge of the
     * screen.
     */
    @JSProperty
    int getScreenY();

    /**
     * Returns the X coordinate of the touch point relative to the left edge of the
     * browser viewport, not including any scroll offset.
     */
    @JSProperty
    int getClientX();

    /**
     * Returns the Y coordinate of the touch point relative to the top edge of the
     * browser viewport, not including any scroll offset.
     */
    @JSProperty
    int getClientY();

    /**
     * Returns the X coordinate of the touch point relative to the left edge of the
     * document. Unlike clientX, this value includes the horizontal scroll offset,
     * if any.
     */
    @JSProperty
    int getPpageX();

    /**
     * Returns the Y coordinate of the touch point relative to the top of the
     * document. Unlike clientY, this value includes the vertical scroll offset, if
     * any.
     */
    @JSProperty
    int getPageY();

    /**
     * Returns the Element on which the touch point started when it was first placed
     * on the surface, even if the touch point has since moved outside the
     * interactive area of that element or even been removed from the document.
     */
    @JSProperty
    HTMLElement getTarget();

    /**
     * Returns the X radius of the ellipse that most closely circumscribes the area
     * of contact with the screen. The value is in pixels of the same scale as
     * screenX.
     */
    @JSProperty
    int getRadiusX();

    /**
     * Returns the Y radius of the ellipse that most closely circumscribes the area
     * of contact with the screen. The value is in pixels of the same scale as
     * screenY.
     */
    @JSProperty
    int getRadiusY();

    /**
     * Returns the angle (in degrees) that the ellipse described by radiusX and
     * radiusY must be rotated, clockwise, to most accurately cover the area of
     * contact between the user and the surface.
     */
    @JSProperty
    int getRotationAngle();

    /**
     * Returns the amount of pressure being applied to the surface by the user, as a
     * float between 0.0 (no pressure) and 1.0 (maximum pressure).
     */
    @JSProperty
    float getForce();

}