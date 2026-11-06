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
package org.teavm.jso.export;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSExport;
import org.teavm.jso.JSExportClasses;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSString;

@JSExportClasses(ModuleWithExit.C.class)
public final class ModuleWithExit {
    private static JSArray<JSString> log;
    private static JSArray<JSObject> lateErrors;

    private ModuleWithExit() {
    }

    @JSExport
    public static void init(JSArray<JSString> log, JSArray<JSObject> lateErrors) {
        ModuleWithExit.log = log;
        ModuleWithExit.lateErrors = lateErrors;
    }

    @JSExport
    public static int exitNow(int code) {
        try {
            System.exit(code);
        } catch (Throwable e) {
            log("caught " + e);
        } finally {
            log("finally");
        }
        log("after exit");
        return 1;
    }

    @JSExport
    public static void exitLater(int delay, int code) {
        new Thread(() -> {
            sleep(delay);
            log("exiting");
            exitNow(code);
        }).start();
    }

    @JSExport
    public static void startThenLog(String message) {
        new Thread(() -> log(message)).start();
    }

    @JSExport
    public static void sleepThenLog(int delay, String message) {
        new Thread(() -> {
            sleep(delay);
            log(message);
        }).start();
    }

    @JSExport
    public static void waitForJsThenLog(int delay, String message) {
        new Thread(() -> {
            waitForJs(delay);
            log(message);
        }).start();
    }

    @JSExport
    public static void throwCustomException() {
        throw new CustomException();
    }

    @JSExport
    public static Callback createCallback(String message) {
        return () -> log(message);
    }

    @JSExport
    public static C createObject() {
        return new C();
    }

    private static void log(String message) {
        log.push(JSString.valueOf(message));
    }

    private static void sleep(int delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Async
    private static native void waitForJs(int delay);

    private static void waitForJs(int delay, AsyncCallback<Void> callback) {
        setTimeout(() -> callback.complete(null), delay, lateErrors);
    }

    // Calling into module after exit throws, so record errors to let test check them
    @JSBody(params = { "callback", "delay", "errors" }, script = ""
            + "setTimeout(() => {"
            + "    try {"
            + "        callback();"
            + "    } catch (e) {"
            + "        errors.push(e);"
            + "    }"
            + "}, delay);")
    private static native void setTimeout(Callback callback, int delay, JSArray<JSObject> errors);

    @JSFunctor
    public interface Callback extends JSObject {
        void run();
    }

    static class CustomException extends RuntimeException {
        CustomException() {
            super("custom message");
        }

        // JS backend reads message without calling overridden method, Wasm GC calls it
        @Override
        public String getMessage() {
            log("getMessage");
            return "custom message";
        }
    }

    public static class C {
        @JSExport
        public C() {
            log("constructor");
        }

        @JSExport
        public int foo() {
            log("foo");
            return 23;
        }

        @JSExport
        @JSProperty
        public int getBar() {
            log("bar");
            return 42;
        }

        @JSExport
        public static int baz() {
            log("baz");
            return 99;
        }
    }
}
