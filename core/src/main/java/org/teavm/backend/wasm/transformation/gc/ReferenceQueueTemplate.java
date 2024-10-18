/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.transformation.gc;

import java.lang.ref.Reference;

class ReferenceQueueTemplate<T> {
    private ReferenceQueueEntry<T> start;
    private ReferenceQueueEntry<T> end;

    public Reference<T> poll() {
        var result = start;
        if (result == null) {
            return null;
        }
        start = result.next;
        if (start == null) {
            end = null;
        }
        return result.reference;
    }

    public void supply(Reference<T> reference) {
        var entry = new ReferenceQueueEntry<>(reference);
        if (start == null) {
            start = entry;
        } else {
            end.next = entry;
        }
        end = entry;
    }
}
