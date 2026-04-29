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
package org.teavm.classlib.java.util.concurrent;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.java.lang.TInterruptedException;
import org.teavm.classlib.java.lang.TThread;
import org.teavm.classlib.java.lang.TThreadInterruptHandler;
import org.teavm.classlib.java.util.TAbstractQueue;
import org.teavm.classlib.java.util.TCollection;
import org.teavm.classlib.java.util.TIterator;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformRunnable;
import org.teavm.runtime.EventQueue;

public class TLinkedBlockingQueue<E> extends TAbstractQueue<E> implements TBlockingQueue<E> {
    private final int capacity;
    private final java.util.ArrayDeque<E> queue;
    private java.util.Queue<WaitHandler> putWaiters;
    private java.util.Queue<WaitHandler> takeWaiters;

    public TLinkedBlockingQueue() {
        this(Integer.MAX_VALUE);
    }

    public TLinkedBlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
        this.queue = new java.util.ArrayDeque<>();
    }

    public TLinkedBlockingQueue(Collection<? extends E> c) {
        this(Integer.MAX_VALUE);
        Objects.requireNonNull(c);
        addAll(c);
    }

    @Override
    public boolean add(E e) {
        if (offer(e)) {
            return true;
        }
        throw new IllegalStateException("Queue full");
    }

    @Override
    public boolean offer(E e) {
        Objects.requireNonNull(e);
        if (queue.size() >= capacity) {
            return false;
        }
        queue.addLast(e);
        notifyTake();
        return true;
    }

    @Override
    public void put(E e) throws TInterruptedException {
        Objects.requireNonNull(e);
        while (queue.size() >= capacity) {
            waitForPut(0);
        }
        queue.addLast(e);
        notifyTake();
    }

    @Override
    public boolean offer(E e, long timeout, TTimeUnit unit) throws TInterruptedException {
        Objects.requireNonNull(e);
        if (queue.size() < capacity) {
            queue.addLast(e);
            notifyTake();
            return true;
        }
        long timeLimit = System.currentTimeMillis() + unit.toMillis(timeout);
        while (queue.size() >= capacity) {
            if (!waitForPut(timeLimit)) {
                return false;
            }
        }
        queue.addLast(e);
        notifyTake();
        return true;
    }

    @Override
    public E poll() {
        E e = queue.pollFirst();
        if (e != null) {
            notifyPut();
        }
        return e;
    }

    @Override
    public E take() throws TInterruptedException {
        while (queue.isEmpty()) {
            waitForTake(0);
        }
        E e = queue.pollFirst();
        notifyPut();
        return e;
    }

    @Override
    public E poll(long timeout, TTimeUnit unit) throws TInterruptedException {
        if (!queue.isEmpty()) {
            E e = queue.pollFirst();
            notifyPut();
            return e;
        }
        long timeLimit = System.currentTimeMillis() + unit.toMillis(timeout);
        while (queue.isEmpty()) {
            if (!waitForTake(timeLimit)) {
                return null;
            }
        }
        E e = queue.pollFirst();
        notifyPut();
        return e;
    }

    @Override
    public E peek() {
        return queue.peekFirst();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public int remainingCapacity() {
        return capacity - queue.size();
    }

    @Override
    public boolean remove(Object o) {
        boolean removed = queue.remove(o);
        if (removed) {
            notifyPut();
        }
        return removed;
    }

    @Override
    public boolean contains(Object o) {
        return queue.contains(o);
    }

    @Override
    public Object[] toArray() {
        return queue.toArray();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        return queue.toArray(a);
    }

    @Override
    public void clear() {
        queue.clear();
        notifyPut();
    }

    @Override
    public int drainTo(TCollection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(TCollection<? super E> c, int maxElements) {
        if (c == this) {
            throw new IllegalArgumentException();
        }
        Objects.requireNonNull(c);
        int count = 0;
        while (!queue.isEmpty() && count < maxElements) {
            c.add(queue.pollFirst());
            count++;
        }
        notifyPut();
        return count;
    }

    @Override
    public TIterator<E> iterator() {
        return new TIterator<E>() {
            private final Iterator<E> it = queue.iterator();
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }
            @Override
            public E next() {
                return it.next();
            }
            @Override
            public void remove() {
                it.remove();
                notifyPut();
            }
        };
    }

    private void notifyPut() {
        if (putWaiters != null) {
            WaitHandler handler;
            while ((handler = putWaiters.poll()) != null) {
                if (PlatformDetector.isLowLevel() || PlatformDetector.isWebAssemblyGC()) {
                    EventQueue.offer(handler::changed);
                } else {
                    Platform.postpone(handler::changed);
                }
            }
            putWaiters = null;
        }
    }

    private void notifyTake() {
        if (takeWaiters != null) {
            WaitHandler handler;
            while ((handler = takeWaiters.poll()) != null) {
                if (PlatformDetector.isLowLevel() || PlatformDetector.isWebAssemblyGC()) {
                    EventQueue.offer(handler::changed);
                } else {
                    Platform.postpone(handler::changed);
                }
            }
            takeWaiters = null;
        }
    }

    @Async
    private native boolean waitForPut(long timeLimit) throws TInterruptedException;

    private void waitForPut(long timeLimit, AsyncCallback<Boolean> callback) {
        if (putWaiters == null) {
            putWaiters = new java.util.ArrayDeque<>();
        }
        WaitHandler handler = new WaitHandler(callback);
        putWaiters.add(handler);
        if (timeLimit > 0) {
            int timeout = Math.max(0, (int) (timeLimit - System.currentTimeMillis()));
            handler.timerId = PlatformDetector.isLowLevel() || PlatformDetector.isWebAssemblyGC()
                    ? EventQueue.offer(handler, timeLimit)
                    : Platform.schedule(handler, timeout);
        }
        TThread.currentThread().interruptHandler = handler;
    }

    @Async
    private native boolean waitForTake(long timeLimit) throws TInterruptedException;

    private void waitForTake(long timeLimit, AsyncCallback<Boolean> callback) {
        if (takeWaiters == null) {
            takeWaiters = new java.util.ArrayDeque<>();
        }
        WaitHandler handler = new WaitHandler(callback);
        takeWaiters.add(handler);
        if (timeLimit > 0) {
            int timeout = Math.max(0, (int) (timeLimit - System.currentTimeMillis()));
            handler.timerId = PlatformDetector.isLowLevel() || PlatformDetector.isWebAssemblyGC()
                    ? EventQueue.offer(handler, timeLimit)
                    : Platform.schedule(handler, timeout);
        }
        TThread.currentThread().interruptHandler = handler;
    }

    static class WaitHandler implements PlatformRunnable, TThreadInterruptHandler, EventQueue.Event {
        AsyncCallback<Boolean> callback;
        boolean complete;
        int timerId = -1;

        WaitHandler(AsyncCallback<Boolean> callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            if (complete()) {
                return;
            }
            callback.complete(false);
        }

        @Override
        public void interrupted() {
            if (complete()) {
                return;
            }
            callback.error(new TInterruptedException());
        }

        private boolean complete() {
            if (this.complete) {
                return true;
            }
            this.complete = true;
            if (timerId >= 0) {
                if (PlatformDetector.isLowLevel() || PlatformDetector.isWebAssemblyGC()) {
                    EventQueue.kill(timerId);
                } else {
                    Platform.killSchedule(timerId);
                }
                timerId = -1;
            }
            TThread.currentThread().interruptHandler = null;
            return false;
        }

        void changed() {
            if (complete()) {
                return;
            }
            callback.complete(true);
        }
    }
}
