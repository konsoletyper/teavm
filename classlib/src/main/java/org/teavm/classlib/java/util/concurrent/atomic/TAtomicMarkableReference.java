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
 * An {@code AtomicMarkableReference} maintains an object reference
 * along with a mark bit, that can be updated atomically.
 *
 * <p>In TeaVM's single-threaded JavaScript environment, all operations
 * are inherently atomic since there is no concurrent access.</p>
 */
public class TAtomicMarkableReference<V> {
    private V reference;
    private boolean mark;

    public TAtomicMarkableReference(V initialRef, boolean initialMark) {
        reference = initialRef;
        mark = initialMark;
    }

    public V getReference() {
        return reference;
    }

    public boolean isMarked() {
        return mark;
    }

    public V get(boolean[] markHolder) {
        if (markHolder != null && markHolder.length > 0) {
            markHolder[0] = mark;
        }
        return reference;
    }

    public boolean weakCompareAndSet(V expectedReference, V newReference,
            boolean expectedMark, boolean newMark) {
        return compareAndSet(expectedReference, newReference, expectedMark, newMark);
    }

    public boolean compareAndSet(V expectedReference, V newReference,
            boolean expectedMark, boolean newMark) {
        if (reference == expectedReference && mark == expectedMark) {
            reference = newReference;
            mark = newMark;
            return true;
        }
        return false;
    }

    public void set(V newReference, boolean newMark) {
        reference = newReference;
        mark = newMark;
    }

    public boolean attemptMark(V expectedReference, boolean newMark) {
        if (reference == expectedReference) {
            mark = newMark;
            return true;
        }
        return false;
    }
}
