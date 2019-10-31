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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Objects;
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
import org.teavm.platform.PlatformQueue;
import org.teavm.platform.PlatformRunnable;
import org.teavm.runtime.EventQueue;

public class TArrayBlockingQueue<E> extends TAbstractQueue<E> implements TBlockingQueue<E> {
    private Object[] array;
    private int head;
    private int tail;
    private PlatformQueue<WaitHandler> waitHandlers;

    public TArrayBlockingQueue(int capacity) {
        this(capacity, false);
    }

    public TArrayBlockingQueue(int capacity, boolean fair) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be >= 1");
        }
        array = new Object[capacity];
    }

    public TArrayBlockingQueue(int capacity, boolean fair, Collection<? extends E> c) {
        if (capacity < 1 || capacity < Objects.requireNonNull(c).size()) {
            throw new IllegalArgumentException("Capacity must be at least 1 or collection's size");
        }
        if (c.size() > capacity) {
            throw new IllegalArgumentException();
        }
        array = new Object[capacity];

        int index = 0;
        for (E e : c) {
            array[index++] = Objects.requireNonNull(e);
        }
        tail = c.size();
    }

    @Override
    public boolean add(E e) {
        Objects.requireNonNull(e);
        if (isFull()) {
            throw new IllegalStateException("This blocking queue is full");
        }
        addImpl(e);
        return true;
    }

    @Override
    public boolean offer(E e) {
        Objects.requireNonNull(e);
        if (isFull()) {
            return false;
        }
        addImpl(e);
        return true;
    }

    @Override
    public void put(E e) throws InterruptedException {
        Objects.requireNonNull(e);
        while (isFull()) {
            waitForChange(0);
        }
        addImpl(e);
    }

    @Override
    public boolean offer(E e, long timeout, TTimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(e);
        if (isFull()) {
            long timeLimit = System.currentTimeMillis() + unit.toMillis(timeout);
            while (isFull()) {
                if (!waitForChange(timeLimit)) {
                    return false;
                }
            }
        }
        addImpl(e);
        return true;
    }

    @Override
    public E poll() {
        if (isEmpty()) {
            return null;
        }
        return removeImpl();
    }

    @Override
    public E take() throws InterruptedException {
        while (isEmpty()) {
            waitForChange(0);
        }
        return removeImpl();
    }

    @Override
    public E poll(long timeout, TTimeUnit unit) throws InterruptedException {
        if (isEmpty()) {
            long timeLimit = System.currentTimeMillis() + unit.toMillis(timeout);
            while (isEmpty()) {
                if (!waitForChange(timeLimit)) {
                    return null;
                }
            }
        }
        return removeImpl();
    }

    @Override
    @SuppressWarnings("unchecked")
    public E peek() {
        if (isEmpty()) {
            return null;
        }
        return (E) array[head];
    }

    @Override
    public int size() {
        if (head < tail) {
            return tail - head;
        } else if (array[head] != null) {
            return tail + array.length - head;
        } else {
            return 0;
        }
    }

    @Override
    public boolean isEmpty() {
        return head == tail && array[head] == null;
    }

    @Override
    public int remainingCapacity() {
        return array.length - size();
    }

    @Override
    public boolean remove(Object o) {
        if (isEmpty()) {
            return false;
        } else if (head < tail) {
            for (int i = head; i < tail; ++i) {
                if (array[i].equals(o)) {
                    removeAt(i);
                    return true;
                }
            }
            return false;
        } else {
            for (int i = head; i < array.length; ++i) {
                if (array[i].equals(o)) {
                    removeAt(i);
                    return true;
                }
            }

            for (int i = 0; i < tail; ++i) {
                if (array[i].equals(o)) {
                    removeAt(i);
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public boolean contains(Object o) {
        if (isEmpty()) {
            return false;
        } else if (head < tail) {
            for (int i = head; i < tail; ++i) {
                if (array[i].equals(o)) {
                    return true;
                }
            }
            return false;
        } else {
            for (int i = head; i < array.length; ++i) {
                if (array[i].equals(o)) {
                    return true;
                }
            }
            for (int i = 0; i < tail; ++i) {
                if (array[i].equals(o)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public Object[] toArray() {
        if (isEmpty()) {
            return new Object[0];
        } else if (head < tail) {
            return Arrays.copyOfRange(array, head, tail);
        } else {
            Object[] result = new Object[size()];
            System.arraycopy(array, head, result, 0, array.length - head);
            System.arraycopy(array, 0, result, array.length - head, tail);
            return result;
        }
    }

    @Override
    @SuppressWarnings({ "unchecked", "SuspiciousSystemArraycopy" })
    public <T> T[] toArray(T[] a) {
        if (isEmpty()) {
            if (a.length > 0) {
                a[0] = null;
            }
            return a;
        }

        int size = size();
        if (size > a.length) {
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
        } else if (size < a.length) {
            a[size] = null;
        }

        if (head < tail) {
            System.arraycopy(array, head, a, 0, size);
        } else {
            System.arraycopy(array, head, a, 0, array.length - head);
            System.arraycopy(array, 0, a, array.length - head, tail);
        }

        return a;
    }

    @Override
    public void clear() {
        if (isEmpty()) {
            return;
        }
        if (head < tail) {
            for (int i = head; i < tail; ++i) {
                array[i] = null;
            }
        } else {
            for (int i = head; i < array.length; ++i) {
                array[i] = null;
            }
            for (int i = 0; i < tail; ++i) {
                array[i] = null;
            }
        }

        head = 0;
        tail = 0;
        notifyChange();
    }

    @Override
    public int drainTo(TCollection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(TCollection<? super E> c, int maxElements) {
        if (c == this) {
            throw new IllegalArgumentException("The specified collection is this queue");
        }
        if (isEmpty()) {
            return 0;
        }
        Objects.requireNonNull(c);
        maxElements = Math.min(maxElements, size());
        if (maxElements < tail) {
            for (int i = 0; i < maxElements; ++i) {
                drainSingleTo(c);
            }
            notifyChange();
        } else {
            int remaining = maxElements - (array.length - head);
            while (head < array.length) {
                drainSingleTo(c);
            }
            head = 0;
            while (remaining-- > 0) {
                drainSingleTo(c);
            }
            notifyChange();
        }
        return maxElements;
    }

    @Override
    public TIterator<E> iterator() {
        return new TIterator<E>() {
            int index = head;
            int removeIndex = -1;

            @Override
            public boolean hasNext() {
                return array[index] != null;
            }

            @Override
            public E next() {
                removeIndex = index;
                @SuppressWarnings("unchecked")
                E result = (E) array[index++];
                if (result == null) {
                    throw new NoSuchElementException();
                }
                if (index == array.length) {
                    index = 0;
                }
                return result;
            }

            @Override
            public void remove() {
                if (removeIndex < 0 || array[removeIndex] == null) {
                    throw new IllegalStateException();
                }
                removeAt(removeIndex);
                removeIndex = -1;
            }
        };
    }

    private void removeAt(int index) {
        if (index < tail) {
            shiftElements(index, tail);
            --tail;
        } else {
            shiftElements(index, array.length);
            array[array.length - 1] = array[0];
            shiftElements(0, tail);
            if (--tail < 0) {
                tail = array.length;
            }
        }
        array[tail] = null;
        notifyChange();
    }

    private void drainSingleTo(TCollection<? super E> c) {
        @SuppressWarnings("unchecked")
        E e = (E) array[head];
        array[head++] = null;
        c.add(e);
    }

    private void shiftElements(int from, int to) {
        int remaining = to - from - 1;
        if (remaining > 0) {
            System.arraycopy(array, from + 1, array, from, remaining);
        }
    }

    private void addImpl(E e) {
        array[tail++] = e;
        if (tail == array.length) {
            tail = 0;
        }
        notifyChange();
    }

    private E removeImpl() {
        @SuppressWarnings("unchecked")
        E result = (E) array[head];
        array[head++] = null;
        if (head == array.length) {
            head = 0;
        }
        notifyChange();
        return result;
    }

    private void notifyChange() {
        if (waitHandlers == null) {
            return;
        }
        while (!waitHandlers.isEmpty()) {
            WaitHandler handler = waitHandlers.remove();
            if (PlatformDetector.isLowLevel()) {
                EventQueue.offer(handler::changed);
            } else {
                Platform.postpone(handler::changed);
            }
        }
        waitHandlers = null;
    }

    @Async
    private native Boolean waitForChange(long timeLimit) throws InterruptedException;

    private void waitForChange(long timeLimit, AsyncCallback<Boolean> callback) {
        if (waitHandlers == null) {
            waitHandlers = Platform.createQueue();
        }

        WaitHandler handler = new WaitHandler(callback);
        waitHandlers.add(handler);
        if (timeLimit > 0) {
            int timeout = Math.max(0, (int) (timeLimit - System.currentTimeMillis()));
            handler.timerId = PlatformDetector.isLowLevel()
                    ? EventQueue.offer(handler, timeLimit)
                    : Platform.schedule(handler, timeout);
        } else {
            handler.timerId = -1;
        }

        TThread.currentThread().interruptHandler = handler;
    }

    class WaitHandler implements PlatformRunnable, TThreadInterruptHandler, EventQueue.Event {
        AsyncCallback<Boolean> callback;
        boolean complete;
        int timerId;

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
            if (complete) {
                return true;
            }
            complete = true;
            if (timerId >= 0) {
                if (PlatformDetector.isLowLevel()) {
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

    private boolean isFull() {
        return head == tail && array[head] != null;
    }
}
