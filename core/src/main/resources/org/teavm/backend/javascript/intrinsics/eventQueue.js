/*
 *  Copyright 2026 Alexey Andreev.
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

function offer(event, time) {
    let delay = Math.max(0, time - Date.now());
    // Node.js returns a Timeout object, which converts to a numeric id that clearTimeout accepts
    return teavm_globals.setTimeout(() => {
        if ($rt_exited()) {
            return;
        }
        try {
            teavm_javaMethod("org.teavm.runtime.EventQueue", "run(Lorg/teavm/runtime/EventQueue$Event;)V")(event);
        } catch (e) {
            // System.exit was called by this event: there's no JS caller to report to, so just stop
            if (!$rt_exited()) {
                throw e;
            }
        }
    }, delay) | 0;
}

function kill(id) {
    teavm_globals.clearTimeout(id);
}
