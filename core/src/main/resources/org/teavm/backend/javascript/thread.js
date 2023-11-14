/*
 *  Copyright 2018 Alexey Andreev.
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
"use strict";

function TeaVMThread(runner) {
    this.status = 3;
    this.stack = [];
    this.suspendCallback = null;
    this.runner = runner;
    this.attribute = null;
    this.completeCallback = null;
}
TeaVMThread.prototype.push = function() {
    for (let i = 0; i < arguments.length; ++i) {
        this.stack.push(arguments[i]);
    }
    return this;
};
TeaVMThread.prototype.s = TeaVMThread.prototype.push;
TeaVMThread.prototype.pop = function() {
    return this.stack.pop();
};
TeaVMThread.prototype.l = TeaVMThread.prototype.pop;
TeaVMThread.prototype.isResuming = function() {
    return this.status === 2;
};
TeaVMThread.prototype.isSuspending = function() {
    return this.status === 1;
};
TeaVMThread.prototype.suspend = function(callback) {
    this.suspendCallback = callback;
    this.status = 1;
};
TeaVMThread.prototype.start = function(callback) {
    if (this.status !== 3) {
        throw new teavm_globals.Error("Thread already started");
    }
    if ($rt_currentNativeThread !== null) {
        throw new teavm_globals.Error("Another thread is running");
    }
    this.status = 0;
    this.completeCallback = callback ? callback : (result) => {
        if (result instanceof teavm_globals.Error) {
            throw result;
        }
    };
    this.run();
};
TeaVMThread.prototype.resume = function() {
    if ($rt_currentNativeThread !== null) {
        throw new teavm_globals.Error("Another thread is running");
    }
    this.status = 2;
    this.run();
};
TeaVMThread.prototype.run = function() {
    $rt_currentNativeThread = this;
    let result;
    try {
        result = this.runner();
    } catch (e) {
        result = e;
    } finally {
        $rt_currentNativeThread = null;
    }
    if (this.suspendCallback !== null) {
        let self = this;
        let callback = this.suspendCallback;
        this.suspendCallback = null;
        callback(() => self.resume());
    } else if (this.status === 0) {
        this.completeCallback(result);
    }
};

let $rt_suspending = () => {
    let thread = $rt_nativeThread();
    return thread != null && thread.isSuspending();
}
let $rt_resuming = () => {
    let thread = $rt_nativeThread();
    return thread != null && thread.isResuming();
}
let $rt_suspend = callback => {
    let nativeThread = $rt_nativeThread();
    if (nativeThread === null) {
        throw new teavm_globals.Error("Suspension point reached from non-threading context (perhaps, from native JS method).");
    }
    return nativeThread.suspend(callback);
}
let $rt_startThread = (runner, callback) => new TeaVMThread(runner).start(callback);
let $rt_currentNativeThread = null;
let $rt_nativeThread = () => $rt_currentNativeThread;
let $rt_invalidPointer = () => {
    throw new teavm_globals.Error("Invalid recorded state");
}