/*
 *  Copyright 2026 TeaVM contributors.
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

import java.util.Objects;
import org.teavm.classlib.java.lang.TRunnable;
import org.teavm.classlib.java.util.TAbstractQueue;
import org.teavm.classlib.java.util.TArrayList;
import org.teavm.classlib.java.util.TCollection;
import org.teavm.classlib.java.util.TIterator;
import org.teavm.classlib.java.util.TPriorityQueue;

final class TDelayedWorkQueue<E extends TRunnable> extends TAbstractQueue<E> implements TBlockingQueue<E> {
    private final TPriorityQueue<E> tasks = new TPriorityQueue<>();

    @Override
    public boolean offer(E task) {
        Objects.requireNonNull(task);
        synchronized (this) {
            boolean added = tasks.offer(task);
            notifyAll();
            return added;
        }
    }

    @Override
    public void put(E task) {
        offer(task);
    }

    @Override
    public boolean offer(E task, long timeout, TTimeUnit unit) {
        Objects.requireNonNull(unit);
        return offer(task);
    }

    @Override
    public synchronized E poll() {
        E first = tasks.peek();
        if (first == null || ((TDelayed) first).getDelay(TTimeUnit.NANOSECONDS) > 0) {
            return null;
        }
        return tasks.poll();
    }

    @Override
    public E take() throws InterruptedException {
        synchronized (this) {
            while (true) {
                E first = tasks.peek();
                if (first == null) {
                    wait();
                    continue;
                }
                long delay = ((TDelayed) first).getDelay(TTimeUnit.NANOSECONDS);
                if (delay <= 0) {
                    return tasks.poll();
                }
                waitNanos(delay);
            }
        }
    }

    @Override
    public E poll(long timeout, TTimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(unit);
        long timeoutNanos = unit.toNanos(timeout);
        long start = System.nanoTime();
        synchronized (this) {
            while (true) {
                E first = tasks.peek();
                if (first != null) {
                    long delay = ((TDelayed) first).getDelay(TTimeUnit.NANOSECONDS);
                    if (delay <= 0) {
                        return tasks.poll();
                    }
                    long remaining = timeoutNanos - (System.nanoTime() - start);
                    if (remaining <= 0) {
                        return null;
                    }
                    waitNanos(Math.min(delay, remaining));
                } else {
                    long remaining = timeoutNanos - (System.nanoTime() - start);
                    if (remaining <= 0) {
                        return null;
                    }
                    waitNanos(remaining);
                }
            }
        }
    }

    private void waitNanos(long nanos) throws InterruptedException {
        long millis = nanos / 1_000_000L;
        int remainder = (int) (nanos % 1_000_000L);
        wait(millis, remainder);
    }

    @Override
    public synchronized E peek() {
        return tasks.peek();
    }

    @Override
    public synchronized int size() {
        return tasks.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return tasks.isEmpty();
    }

    @Override
    public synchronized boolean remove(Object task) {
        boolean removed = tasks.remove(task);
        if (removed) {
            notifyAll();
        }
        return removed;
    }

    @Override
    public synchronized void clear() {
        tasks.clear();
        notifyAll();
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int drainTo(TCollection<? super E> target) {
        return drainTo(target, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(TCollection<? super E> target, int maxElements) {
        Objects.requireNonNull(target);
        if (target == this) {
            throw new IllegalArgumentException();
        }
        int drained = 0;
        synchronized (this) {
            while (drained < maxElements) {
                E task = poll();
                if (task == null) {
                    break;
                }
                target.add(task);
                ++drained;
            }
        }
        return drained;
    }

    @Override
    public synchronized TIterator<E> iterator() {
        TArrayList<E> snapshot = new TArrayList<>(tasks.size());
        for (TIterator<E> iterator = tasks.iterator(); iterator.hasNext();) {
            snapshot.add(iterator.next());
        }
        TIterator<E> iterator = snapshot.iterator();
        return new TIterator<E>() {
            private E current;
            private boolean canRemove;

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public E next() {
                current = iterator.next();
                canRemove = true;
                return current;
            }

            @Override
            public void remove() {
                if (!canRemove) {
                    throw new IllegalStateException();
                }
                TDelayedWorkQueue.this.remove(current);
                canRemove = false;
            }
        };
    }
}
