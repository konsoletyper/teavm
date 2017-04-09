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

import org.teavm.jso.JSBody;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.html.use.UseHTMLValue;

/**
 * @author Alexey Andreev
 */
public interface Event extends JSObject {
    short CAPTURING_PHASE = 1;
    short AT_TARGET = 2;
    short BUBBLING_PHASE = 3;

    @JSProperty
    EventTarget getCurrentTarget();

    default EventClass getEventClass() {
        return UseHTMLValue.toEnumValue(EventClass.class, innerGetEventClass());
    }

    @JSBody(script = "return this.constructor.name;")
    String innerGetEventClass();

    default EventPhase getEventPhase() {
        return UseHTMLValue.toEnumValue(EventPhase.class, innerGetEventPhase());
    }

    @JSBody(script = "return this.eventPhase;")
    int innerGetEventPhase();

    @JSProperty
    EventTarget getSrcElement();

    @JSProperty
    EventTarget getTarget();

    @JSProperty
    double getTimeStamp();

    default EventType getType() {
        return UseHTMLValue.toEnumValue(EventType.class, innerGetType());
    }

    @JSBody(script = "return this.type;")
    String innerGetType();

    @JSProperty
    boolean isBubbles();

    @JSProperty
    boolean isCancelBubble();

    @JSProperty
    boolean isCancelable();

    @JSProperty
    boolean isDefaultPrevented();

    @JSProperty
    boolean isReturnValue();

    @JSProperty
    void setCancelBubble(boolean cancelBubble);

    @JSProperty
    void setReturnValue(boolean returnValue);

    default void initEvent(EventType eventType, boolean canBubble, boolean cancelable) {
        innerInitEvent(UseHTMLValue.getHtmlValue(eventType), canBubble, cancelable);
    }

    @JSMethod("initEvent")
    void innerInitEvent(String eventType, boolean canBubble, boolean cancelable);

    @JSMethod
    void preventDefault();

    @JSMethod
    void stopImmediatePropagation();

    @JSMethod
    void stopPropagation();
}
