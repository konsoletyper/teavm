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

// From https://developer.mozilla.org/en-US/docs/Web/API/Touch

public interface Touch extends JSObject {

    @JSProperty
    long getIdentifier();
    
    @JSProperty
    int getScreenX();

    @JSProperty
    int getScreenY();

    @JSProperty
    int getClientX();

    @JSProperty
    int getClientY();
    
    @JSProperty
    int getPageX();

    @JSProperty
    int getPageY();
    
    @JSProperty
    EventTarget getTarget();

    @JSProperty
    int getRadiusX();
 
    @JSProperty
    int getRadiusY();

    // Not sure if this is an int or a float. This is angle in Degrees
    @JSProperty
    int getRotationAngle();
    
    // between 0.0 (no pressure) and 1.0 (max pressure)
    @JSProperty
    float getForce();
}
