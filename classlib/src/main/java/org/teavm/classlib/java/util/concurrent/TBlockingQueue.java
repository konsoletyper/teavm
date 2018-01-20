/*
 *  Copyright 2018 konsoletyper.
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
package org.teavm.classlib.java.util.concurrent;

import org.teavm.classlib.java.util.TCollection;
import org.teavm.classlib.java.util.TQueue;

public interface TBlockingQueue<E> extends TQueue<E> {
    void put(E e) throws InterruptedException;

    boolean offer(E e, long timeout, TTimeUnit unit) throws InterruptedException;

    E take() throws InterruptedException;

    E poll(long timeout, TTimeUnit unit) throws InterruptedException;

    int remainingCapacity();

    int drainTo(TCollection<? super E> c);

    int drainTo(TCollection<? super E> c, int maxElements);
}
