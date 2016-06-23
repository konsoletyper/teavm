/*
 *  Copyright 2016 Alexey Andreev.
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

public interface WheelEvent extends MouseEvent {
    int DOM_DELTA_PIXEL = 0;
    int DOM_DELTA_LINE = 1;
    int DOM_DELTA_PAGE = 2;

    @JSProperty
    double getDeltaX();

    @JSProperty
    double getDeltaY();

    @JSProperty
    double getDeltaZ();

    @JSProperty
    int getDeltaMode();
}
