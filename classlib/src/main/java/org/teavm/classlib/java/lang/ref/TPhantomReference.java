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

/**
 * Phantom reference objects, which are enqueued after the collector
 * determines that their referents may otherwise be reclaimed.
 *
 * <p>In TeaVM's JavaScript environment, garbage collection is handled by the
 * JS engine, and phantom references cannot be reliably detected. This
 * implementation provides the API surface but always returns null from
 * {@link #get()} and never enqueues.</p>
 */
public class TPhantomReference<T> extends TReference<T> {
    public TPhantomReference(T referent, TReferenceQueue<? super T> q) {
        // Phantom references always return null from get()
        // The queue is not functional in TeaVM's environment
    }

    @Override
    public T get() {
        return null;
    }
}
