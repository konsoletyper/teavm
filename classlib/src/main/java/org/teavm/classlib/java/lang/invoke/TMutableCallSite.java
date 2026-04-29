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
 * A CallSite whose target can be changed dynamically.
 * A MutableCallSite allows its target MethodHandle to be set
 * multiple times.
 */
public class TMutableCallSite extends TCallSite {
    private volatile TMethodHandle target;

    public TMutableCallSite(TMethodHandle target) {
        this.target = target;
    }

    public TMutableCallSite(TMethodType type) {
        this.target = null;
    }

    @Override
    public TMethodHandle getTarget() {
        return target;
    }

    @Override
    public void setTarget(TMethodHandle newTarget) {
        this.target = newTarget;
    }
}
