/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.common;

import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author Alexey Andreev
 */
public class SimpleFiniteExecutor implements FiniteExecutor {
    private Queue<Runnable> queue = new LinkedList<>();

    @Override
    public void execute(Runnable command) {
        queue.add(command);
    }

    @Override
    public void executeFast(Runnable runnable) {
        execute(runnable);
    }

    @Override
    public void complete() {
        while (!queue.isEmpty()) {
            queue.remove().run();
        }
    }
}
