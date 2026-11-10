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
package org.teavm.classlib.support;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import org.teavm.classlib.PlatformDetector;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.browser.TimerHandler;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;

public final class GCSupport {
    private GCSupport() {
    }

    public static void tryToTriggerGC() {
        tryToTriggerGC(null);
    }

    public static void tryToTriggerGC(Reference<?> ref) {
        if (PlatformDetector.isC()) {
            System.gc();
            return;
        }
        if ((PlatformDetector.isJavaScript() || PlatformDetector.isWebAssemblyGC()) && isGCExposed()) {
            triggerAsyncGC(ref);
            return;
        }
        var weakReferences = new ArrayList<WeakReference<Object>>();
        for (var i = 0; i < 25; ++i) {
            System.out.println("GC trigger attempt " + i);
            weakReferences.add(generateGarbage());
            waitInJS();
            if (weakReferences.stream().anyMatch(s -> s.get() == null)) {
                if (ref != null) {
                    if (ref.get() == null) {
                        break;
                    }
                } else if (i > 5) {
                    break;
                }
            }
        }
    }
    
    // Runs GC as a separate task, when there are no JS/Wasm frames on the stack. Synchronous gc() call scans stack
    // conservatively, so stale stack slots may keep objects alive.
    private static void triggerAsyncGC(Reference<?> ref) {
        for (var i = 0; i < 5; ++i) {
            asyncGC();
            // ref.get() keeps the referent alive until the end of current job only, next GC runs in a new task
            if (ref == null ? i >= 1 : ref.get() == null) {
                break;
            }
        }
    }

    @Async
    private static native void asyncGC();
    private static void asyncGC(AsyncCallback<Void> callback) {
        asyncGCImpl(() -> callback.complete(null));
    }

    // Old V8 versions ignore options and return undefined. Wait a tick after GC so that FinalizationRegistry
    // callbacks have a chance to run.
    @JSBody(params = "callback", script = "Promise.resolve(gc({ type: 'major', execution: 'async' }))"
            + ".then(() => setTimeout(callback, 0));")
    private static native void asyncGCImpl(TimerHandler callback);

    @JSBody(script = "return typeof gc === 'function';")
    private static native boolean isGCExposed();

    private static WeakReference<Object> generateGarbage() {
        return new WeakReference<>(generateTree("R"));
    }

    private static void waitInJS() {
        if (PlatformDetector.isJavaScript() || PlatformDetector.isWebAssemblyGC()) {
            var doc = HTMLDocument.current();
            var div = doc.createElement("div");
            div.appendChild(doc.createTextNode("hello"));
            doc.getBody().appendChild(div);
            waitImpl();
            // A JS WeakRef keeps its target until the end of the job that created or dereferenced it,
            // so collect in a new job, before the caller checks its references again
            triggerGCInJS();
        } else {
            Runtime.getRuntime().gc();
            Runtime.getRuntime().gc();
        }
    }

    @Async
    private static native void waitImpl();
    private static void waitImpl(AsyncCallback<Void> callback) {
        Window.setTimeout(() -> callback.complete(null), 500);
    }

    @JSBody(script = "if (typeof window.gc === 'function') { window.gc(); }")
    private static native void triggerGCInJS();

    private static Tree generateTree(String path) {
        var result = new Tree();
        result.s = path;
        if (path.length() < 18) {
            result.a = generateTree(path + "l");
            result.b = generateTree(path + "r");
        }
        return result;
    }

    private static class Tree {
        String s;
        Tree a;
        Tree b;
    }
}
