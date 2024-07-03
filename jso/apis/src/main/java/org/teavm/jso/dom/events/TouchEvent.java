package org.teavm.jso.dom.events;

import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.Event;

public interface TouchEvent extends Event {
    public static final String TOUCHSTART = "touchstart";
    public static final String TOUCHEND = "touchend";
    public static final String TOUCHMOVE = "touchmove";

    /**
     * A Boolean value indicating whether or not the alt key was down when the touch
     * event was fired.
     */
    @JSProperty
    boolean getAltKey();

    /**
     * A TouchList of all the Touch objects representing individual points of
     * contact whose states changed between the previous touch event and this one.
     */
    @JSProperty
    TouchList getChangedTouches();

    /**
     * A Boolean value indicating whether or not the control key was down when the
     * touch event was fired.
     */
    @JSProperty
    boolean getCtrlKey();

    /**
     * A Boolean value indicating whether or not the meta key was down when the
     * touch event was fired.
     */
    @JSProperty
    boolean getMetaKey();

    /**
     * A Boolean value indicating whether or not the shift key was down when the
     * touch event was fired.
     */
    @JSProperty
    boolean getShiftKey();

    /**
     * A TouchList of all the Touch objects that are both currently in contact with
     * the touch surface and were also started on the same element that is the
     * target of the event.
     */
    @JSProperty
    TouchList getTargetTouches();

    /**
     * A TouchList of all the Touch objects representing all current points of
     * contact with the surface, regardless of target or changed status.
     */
    @JSProperty
    TouchList getTouches();

}