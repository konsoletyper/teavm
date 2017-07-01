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

public interface EventTarget extends JSObject {
    void addEventListener(String type, EventListener<?> listener, boolean useCapture);

    void addEventListener(String type, EventListener<?> listener);

    void removeEventListener(String type, EventListener<?> listener, boolean useCapture);

    void removeEventListener(String type, EventListener<?> listener);

    boolean dispatchEvent(Event evt);
}
