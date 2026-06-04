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
package org.teavm.runtime;

import org.teavm.backend.c.intrinsic.RuntimeInclude;
import org.teavm.interop.Address;
import org.teavm.interop.AsyncCallback;
import org.teavm.interop.Function;
import org.teavm.interop.Import;

public final class CFiber {
    private static Address edtFiber;
    private static int nonDaemonFibers;
    private static AsyncCallWrapper pendingAsyncCall;
    private static FiberInfo currentlyRunningFiber;

    private CFiber() {
    }

    public static void startMain(String[] args) {
        EventQueue.offer(() -> Fiber.startMain(args));
        bootstrap();
    }

    public static void bootstrap() {
        var action = Function.get(CFiberAction.class, CFiber.class, "runEdt");
        edtFiber = fiberCreate(action);
        fiberSwitch(edtFiber);
        fiberDestroy(edtFiber);
        edtFiber = null;
    }

    static void startFiber(Fiber.FiberRunner runner, boolean daemon) {
        var fiber = new FiberInfo();
        fiber.runner = runner;
        fiber.fiber = fiberCreate(Function.get(CFiberAction.class, CFiber.class, "runFiber"));
        fiber.thread = Thread.currentThread();
        fiber.daemon = daemon;
        if (!daemon) {
            ++nonDaemonFibers;
        }
        currentlyRunningFiber = fiber;
        fiberSwitch(fiber.fiber);
    }

    @SuppressWarnings("unused")
    private static void runEdt(Address actionData, Address data) {
        do {
            processCalls();
        } while (EventQueue.processSingle());
    }

    private static void processCalls() {
        while (pendingAsyncCall != null) {
            var callWrapper = pendingAsyncCall;
            pendingAsyncCall = null;
            callWrapper.call.run(new AsyncCallback<>() {
                @Override
                public void complete(Object result) {
                    callWrapper.fiberInfo.value = result;
                    continueFiber();
                }

                @Override
                public void error(Throwable e) {
                    callWrapper.fiberInfo.error = e;
                    continueFiber();
                }

                private void continueFiber() {
                    currentlyRunningFiber = callWrapper.fiberInfo;
                    Fiber.setCurrentThread(callWrapper.fiberInfo.thread);
                    fiberSwitch(callWrapper.fiberInfo.fiber);
                }
            });
        }
    }

    @SuppressWarnings("unused")
    private static void runFiber(Address actionData, Address data) {
        doRunFiber();
    }

    private static void doRunFiber() {
        var fiberInfo = currentlyRunningFiber;
        var runner = fiberInfo.runner;
        fiberInfo.runner = null;
        runner.run();
        EventQueue.offer(() -> {
            fiberDestroy(fiberInfo.fiber);
            if (!fiberInfo.daemon && --nonDaemonFibers == 0) {
                EventQueue.stop();
            }
        });
        fiberSwitch(edtFiber);
    }

    public static Object suspend(Fiber.AsyncCall call) throws Throwable {
        var fiberInfo = currentlyRunningFiber;
        pendingAsyncCall = new AsyncCallWrapper(call, currentlyRunningFiber);
        fiberSwitch(edtFiber);
        if (fiberInfo.error != null) {
            var error = fiberInfo.error;
            error.fillInStackTrace();
            fiberInfo.error = null;
            throw error;
        }
        var value = fiberInfo.value;
        fiberInfo.value = null;
        return value;
    }

    static final class AsyncCallWrapper {
        private final Fiber.AsyncCall call;
        private final FiberInfo fiberInfo;

        AsyncCallWrapper(Fiber.AsyncCall call, FiberInfo fiberInfo) {
            this.call = call;
            this.fiberInfo = fiberInfo;
        }
    }

    static final class FiberInfo {
        Fiber.FiberRunner runner;
        Address fiber;
        Thread thread;
        boolean daemon;
        Throwable error;
        Object value;
    }

    @RuntimeInclude("fiber.h")
    @Import(name = "teavm_fiber_switch")
    private static native void fiberSwitch(Address fiber);

    @RuntimeInclude("fiber.h")
    @Import(name = "teavm_fiber_create")
    private static native Address fiberCreate(CFiberAction action);

    @RuntimeInclude("fiber.h")
    @Import(name = "teavm_fiber_create")
    private static native void fiberDestroy(Address fiber);

    @RuntimeInclude("fiber.h")
    @Import(name = "teavm_fiber_current")
    private static native Address currentFiber();
}
