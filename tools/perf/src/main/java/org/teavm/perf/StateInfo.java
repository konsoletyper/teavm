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
package org.teavm.perf;

import java.util.ArrayList;
import java.util.List;
import org.teavm.model.MethodReference;

/**
 * Describes either a class marked with JMH's {@code @State} annotation, or a benchmark class itself.
 * Scopes are ignored, since benchmarks always run in a single thread.
 */
public final class StateInfo {
    private final int index;
    private final String className;
    final List<MethodReference> setupTrial = new ArrayList<>();
    final List<MethodReference> setupIteration = new ArrayList<>();
    final List<MethodReference> tearDownIteration = new ArrayList<>();
    final List<MethodReference> tearDownTrial = new ArrayList<>();

    StateInfo(int index, String className) {
        this.index = index;
        this.className = className;
    }

    public int getIndex() {
        return index;
    }

    public String getClassName() {
        return className;
    }

    public List<MethodReference> getSetupTrial() {
        return setupTrial;
    }

    public List<MethodReference> getSetupIteration() {
        return setupIteration;
    }

    public List<MethodReference> getTearDownIteration() {
        return tearDownIteration;
    }

    public List<MethodReference> getTearDownTrial() {
        return tearDownTrial;
    }
}
