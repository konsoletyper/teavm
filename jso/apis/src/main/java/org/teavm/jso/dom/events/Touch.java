/*
 *  Copyright 2023 ihromant.
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

public interface Touch extends JSObject {
    @JSProperty
    int getIdentifier();

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
    int getRadiusX();

    @JSProperty
    int getRadiusY();

    @JSProperty
    int getRotationAngle();

    @JSProperty
    double getForce();
}
