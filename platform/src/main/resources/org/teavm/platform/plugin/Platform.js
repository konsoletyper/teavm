/*
 *  Copyright 2023 Alexey Andreev.
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

function newInstanceImpl(cls) {
    let thread = $rt_nativeThread();
    if ($rt_resuming()) {
        let r = thread.pop();
        cls.$$constructor$$(r);
        if ($rt_suspending()) {
            return thread.push(r);
        }
        return r;
    }

    if (!cls.hasOwnProperty("$$constructor$$")) {
        return null;
    }

    let r = new cls();
    cls.$$constructor$$(r);
    if ($rt_suspending()) {
        thread.push(r);
    }
    return r;
}

function clone(obj) {
    let copy = new obj.constructor();
    for (let field in obj) {
        if (obj.hasOwnProperty(field)) {
            copy[field] = obj[field];
        }
    }
    return copy;
}

function startThread(runnable) {
    teavm_globals.setTimeout(() => {
        $rt_threadStarter(teavm_javaMethod("org.teavm.platform.Platform",
            "launchThread(Lorg/teavm/platform/PlatformRunnable;)V"))(runnable);
    }, 0);
}

function schedule(runnable, timeout) {
    teavm_globals.setTimeout(() => {
        teavm_javaMethod("org.teavm.platform.Platform",
                "launchThread(Lorg/teavm/platform/PlatformRunnable;)V")(runnable);
    }, timeout);
}