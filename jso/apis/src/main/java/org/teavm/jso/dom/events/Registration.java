/*
 *  Copyright 2024 konsoletyper.
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

public class Registration {
    private EventTarget target;
    private String type;
    private EventListener<?> listener;
    private Boolean useCapture;

    Registration(EventTarget target, String type, EventListener<?> listener, Boolean useCapture) {
        this.target = target;
        this.type = type;
        this.listener = listener;
        this.useCapture = useCapture;
    }

    public void dispose() {
        if (target != null) {
            if (useCapture != null) {
                target.removeEventListener(type, listener, useCapture);
            } else {
                target.removeEventListener(type, listener);
            }
            target = null;
            type = null;
            listener = null;
            useCapture = null;
        }
    }
}
