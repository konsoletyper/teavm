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

function asyncMethod() {
    let thread = $rt_nativeThread();
    let javaThread = $rt_getThread();
    if (thread.isResuming()) {
        thread.status = 0;
        let result = thread.attribute;
        if (result instanceof teavm_globals.Error) {
            throw result;
        }
        return result;
    }

    let callback = function() {};
    callback[teavm_javaVirtualMethod("complete(Ljava/lang/Object;)V")] = val => {
        thread.attribute = val;
        $rt_setThread(javaThread);
        thread.resume();
    }
    callback[teavm_javaVirtualMethod("error(Ljava/lang/Throwable;)V")] = e => {
        thread.attribute = $rt_exception(e);
        $rt_setThread(javaThread);
        thread.resume();
    }
    callback = teavm_javaMethod("org.teavm.platform.plugin.AsyncCallbackWrapper",
        "create(Lorg/teavm/interop/AsyncCallback;)Lorg/teavm/platform/plugin/AsyncCallbackWrapper;")(callback);
    thread.suspend(() => {
       try {
           teavm_fragment("callMethod");
       } catch ($e) {
           callback[teavm_javaVirtualMethod("error(Ljava/lang/Throwable;)V")]($e);
       }
    });
    return null;
}