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
package org.teavm.classlib.java.lang.invoke;

import org.teavm.classlib.java.lang.TObject;

/**
 * A SwitchPoint is an object which can publish state transitions to other threads.
 * A SwitchPoint starts in the "valid" state and can be invalidated (switched)
 * exactly once, after which it remains permanently in the "invalid" state.
 *
 * <p>In TeaVM's single-threaded JavaScript environment, the state transitions
 * are immediate and there is no need for memory barriers or synchronization.</p>
 */
public class TSwitchPoint extends TObject {
    private volatile boolean invalid;

    public TSwitchPoint() {
        invalid = false;
    }

    public TMethodHandle guardWithTest(TMethodHandle target, TMethodHandle fallback) {
        return invalid ? fallback : target;
    }

    public boolean hasBeenInvalidated() {
        return invalid;
    }

    public void invalidateAll() {
        invalid = true;
    }
}
