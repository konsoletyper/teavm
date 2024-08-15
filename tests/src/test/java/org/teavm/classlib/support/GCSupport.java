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
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;

public final class GCSupport {
    private GCSupport() {
    }

    public static void tryToTriggerGC() {
        tryToTriggerGC(null);
    }

    public static void tryToTriggerGC(Reference<?> ref) {
        if (PlatformDetector.isC() || PlatformDetector.isWebAssembly()) {
            System.gc();
            return;
        }
        var weakReferences = new ArrayList<WeakReference<Object>>();
        for (var i = 0; i < 100; ++i) {
            System.out.println("GC trigger attempt " + i);
            weakReferences.add(new WeakReference<>(generateTree("R")));
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

    private static void waitInJS() {
        if (PlatformDetector.isJavaScript()) {
            var doc = HTMLDocument.current();
            var div = doc.createElement("div");
            div.appendChild(doc.createTextNode("hello"));
            doc.getBody().appendChild(div);
            triggerGCInJS();
            waitImpl();
            triggerGCInJS();
            waitImpl();
        } else {
            Runtime.getRuntime().gc();
            Runtime.getRuntime().gc();
        }
    }

    @Async
    private static native void waitImpl();
    private static void waitImpl(AsyncCallback<Void> callback) {
        Window.setTimeout(() -> callback.complete(null), 0);
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
