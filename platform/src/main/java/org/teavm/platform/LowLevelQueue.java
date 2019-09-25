/*
 *  Copyright 2019 konsoletyper.
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
package org.teavm.platform;

import java.util.ArrayDeque;
import java.util.Queue;

public class LowLevelQueue<T> extends PlatformQueue<T> {
    Queue<T> q = new ArrayDeque<>();

    @Override
    public int getLength() {
        return q.size();
    }

    @Override
    void push(PlatformObject obj) {
    }

    @Override
    PlatformObject shift() {
        return null;
    }

    @Override
    public void add(T e) {
        q.add(e);
    }

    @Override
    public T remove() {
        return q.remove();
    }
}
