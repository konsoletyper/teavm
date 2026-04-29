/*
 *  Copyright 2025 konsoletyper.
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

import java.util.function.Consumer;
import org.teavm.classlib.java.lang.TObject;

/**
 * Cleaner provides a way to register cleanup actions that are invoked when
 * objects become phantom reachable.
 *
 * <p>In TeaVM's JavaScript environment, the Cleaner uses {@code FinalizationRegistry}
 * when available to provide best-effort cleanup. On backends without
 * FinalizationRegistry support, cleanup actions are never invoked automatically.</p>
 */
public class TCleaner extends TObject {
    private TCleaner() {
    }

    public static TCleaner create() {
        return new TCleaner();
    }

    public static TCleaner create(ThreadFactory threadFactory) {
        return new TCleaner();
    }

    public Cleanable register(Object obj, Runnable action) {
        if (obj == null || action == null) {
            throw new NullPointerException();
        }
        TCleanable cleanable = new TCleanable(action);
        registerCleanup(obj, cleanable);
        return cleanable;
    }

    /**
     * Registers the cleanable with the JS FinalizationRegistry if available.
     */
    private void registerCleanup(Object obj, TCleanable cleanable) {
        // In TeaVM, we attempt to use FinalizationRegistry through JS interop
        // If not available, the cleanable will only run when explicitly cleaned
        registerFinalization(obj, cleanable);
    }

    @SuppressWarnings("unused")
    private static void registerFinalization(Object obj, TCleanable cleanable) {
        // Best-effort: attempt to schedule cleanup via JS FinalizationRegistry
        // On backends without this support, cleanup is only manual
        tryScheduleCleanup(obj, cleanable);
    }

    private static native void tryScheduleCleanup(Object obj, TCleanable cleanable);

    /**
     * A Cleanable represents an object that can be cleaned.
     */
    public interface Cleanable {
        void clean();
    }

    static class TCleanable implements Cleanable {
        private Runnable action;
        private boolean cleaned;

        TCleanable(Runnable action) {
            this.action = action;
        }

        @Override
        public void clean() {
            if (!cleaned) {
                cleaned = true;
                Runnable a = action;
                action = null;
                if (a != null) {
                    a.run();
                }
            }
        }
    }

    /**
     * ThreadFactory interface for Cleaner compatibility.
     * Not used in TeaVM's single-threaded model but provided for API compatibility.
     */
    public interface ThreadFactory {
        Thread newThread(Runnable r);
    }
}
