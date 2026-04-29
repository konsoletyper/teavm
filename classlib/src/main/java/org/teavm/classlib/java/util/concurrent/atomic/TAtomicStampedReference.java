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
package org.teavm.classlib.java.util.concurrent.atomic;

/**
 * An {@code AtomicStampedReference} maintains an object reference
 * along with an integer "stamp", that can be updated atomically.
 *
 * <p>In TeaVM's single-threaded JavaScript environment, all operations
 * are inherently atomic since there is no concurrent access.</p>
 */
public class TAtomicStampedReference<V> {
    private V reference;
    private int stamp;

    public TAtomicStampedReference(V initialRef, int initialStamp) {
        reference = initialRef;
        stamp = initialStamp;
    }

    public V getReference() {
        return reference;
    }

    public int getStamp() {
        return stamp;
    }

    public V get(int[] stampHolder) {
        if (stampHolder != null && stampHolder.length > 0) {
            stampHolder[0] = stamp;
        }
        return reference;
    }

    public boolean weakCompareAndSet(V expectedReference, V newReference,
            int expectedStamp, int newStamp) {
        return compareAndSet(expectedReference, newReference, expectedStamp, newStamp);
    }

    public boolean compareAndSet(V expectedReference, V newReference,
            int expectedStamp, int newStamp) {
        if (reference == expectedReference && stamp == expectedStamp) {
            reference = newReference;
            stamp = newStamp;
            return true;
        }
        return false;
    }

    public void set(V newReference, int newStamp) {
        reference = newReference;
        stamp = newStamp;
    }

    public boolean attemptStamp(V expectedReference, int newStamp) {
        if (reference == expectedReference) {
            stamp = newStamp;
            return true;
        }
        return false;
    }
}
