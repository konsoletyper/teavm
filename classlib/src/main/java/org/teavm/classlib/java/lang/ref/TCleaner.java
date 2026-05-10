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
package org.teavm.classlib.java.lang.ref;

import org.teavm.interop.Platforms;
import org.teavm.interop.UnsupportedOn;
import org.teavm.jso.core.JSFinalizationRegistry;
import org.teavm.jso.core.JSObjects;

@UnsupportedOn(Platforms.C)
public final class TCleaner {
    private static final JSFinalizationRegistry registry = new JSFinalizationRegistry(held -> ((Runnable) held).run());

    private TCleaner() {
    }

    public static TCleaner create() {
        return new TCleaner();
    }

    public Cleanable register(Object obj, Runnable action) {
        var token = JSObjects.create();
        registry.register(obj, action, token);
        return () -> {
            if (registry.unregister(token)) {
                action.run();
            }
        };
    }

    public interface Cleanable {
        void clean();
    }
}
