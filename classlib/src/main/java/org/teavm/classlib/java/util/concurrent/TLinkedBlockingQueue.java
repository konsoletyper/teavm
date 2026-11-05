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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.teavm.classlib.java.util.TAbstractQueue;
import org.teavm.classlib.java.util.TCollection;
import org.teavm.classlib.java.util.TIterator;

/**
 * Linked FIFO blocking queue with an optional capacity bound.
 *
 * <p>Blocking operations wait on a TeaVM monitor. This lets the cooperative
 * thread runtime suspend the caller while retaining the queue's state
 * transitions and interruption behavior.</p>
 */
public class TLinkedBlockingQueue<E> extends TAbstractQueue<E> implements TBlockingQueue<E> {
    private final Object monitor = new Object();
    private final int capacity;
    private Node<E> first;
    private Node<E> last;
    private int count;

    public TLinkedBlockingQueue() {
        this(Integer.MAX_VALUE);
    }

    public TLinkedBlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero");
        }
        this.capacity = capacity;
    }

    public TLinkedBlockingQueue(Collection<? extends E> collection) {
        this(Integer.MAX_VALUE);
        Objects.requireNonNull(collection);
        for (E element : collection) {
            enqueue(Objects.requireNonNull(element));
        }
    }

    @Override
    public void put(E element) throws InterruptedException {
        Objects.requireNonNull(element);
        checkInterrupted();
        synchronized (monitor) {
            while (count == capacity) {
                waitInterruptibly(0, 0);
            }
            enqueue(element);
            monitor.notifyAll();
        }
    }

    @Override
    public boolean offer(E element, long timeout, TTimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(element);
        long timeoutNanos = Objects.requireNonNull(unit).toNanos(timeout);
        checkInterrupted();
        long start = System.nanoTime();
        synchronized (monitor) {
            while (count == capacity) {
                long elapsed = System.nanoTime() - start;
                long remaining = timeoutNanos <= elapsed ? 0 : timeoutNanos - elapsed;
                if (remaining <= 0) {
                    return false;
                }
                waitInterruptibly(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
            }
            enqueue(element);
            monitor.notifyAll();
            return true;
        }
    }

    @Override
    public boolean offer(E element) {
        Objects.requireNonNull(element);
        synchronized (monitor) {
            if (count == capacity) {
                return false;
            }
            enqueue(element);
            monitor.notifyAll();
            return true;
        }
    }

    @Override
    public E take() throws InterruptedException {
        checkInterrupted();
        synchronized (monitor) {
            while (count == 0) {
                waitInterruptibly(0, 0);
            }
            E result = dequeue();
            monitor.notifyAll();
            return result;
        }
    }

    @Override
    public E poll(long timeout, TTimeUnit unit) throws InterruptedException {
        long timeoutNanos = Objects.requireNonNull(unit).toNanos(timeout);
        checkInterrupted();
        long start = System.nanoTime();
        synchronized (monitor) {
            while (count == 0) {
                long elapsed = System.nanoTime() - start;
                long remaining = timeoutNanos <= elapsed ? 0 : timeoutNanos - elapsed;
                if (remaining <= 0) {
                    return null;
                }
                waitInterruptibly(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
            }
            E result = dequeue();
            monitor.notifyAll();
            return result;
        }
    }

    @Override
    public E poll() {
        synchronized (monitor) {
            if (count == 0) {
                return null;
            }
            E result = dequeue();
            monitor.notifyAll();
            return result;
        }
    }

    @Override
    public E peek() {
        synchronized (monitor) {
            return first == null ? null : first.element;
        }
    }

    @Override
    public int size() {
        synchronized (monitor) {
            return count;
        }
    }

    @Override
    public int remainingCapacity() {
        synchronized (monitor) {
            return capacity - count;
        }
    }

    @Override
    public boolean contains(Object value) {
        synchronized (monitor) {
            for (Node<E> node = first; node != null; node = node.next) {
                if (Objects.equals(node.element, value)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public boolean remove(Object value) {
        synchronized (monitor) {
            Node<E> previous = null;
            for (Node<E> node = first; node != null; node = node.next) {
                if (Objects.equals(node.element, value)) {
                    unlink(previous, node);
                    monitor.notifyAll();
                    return true;
                }
                previous = node;
            }
            return false;
        }
    }

    @Override
    public Object[] toArray() {
        synchronized (monitor) {
            Object[] result = new Object[count];
            int index = 0;
            for (Node<E> node = first; node != null; node = node.next) {
                result[index++] = node.element;
            }
            return result;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] array) {
        synchronized (monitor) {
            int size = count;
            T[] result = array.length >= size
                    ? array
                    : (T[]) Array.newInstance(array.getClass().getComponentType(), size);
            int index = 0;
            for (Node<E> node = first; node != null; node = node.next) {
                result[index++] = (T) node.element;
            }
            if (result.length > size) {
                result[size] = null;
            }
            return result;
        }
    }

    @Override
    public void clear() {
        synchronized (monitor) {
            first = null;
            last = null;
            count = 0;
            monitor.notifyAll();
        }
    }

    @Override
    public int drainTo(TCollection<? super E> destination) {
        return drainTo(destination, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(TCollection<? super E> destination, int maxElements) {
        Objects.requireNonNull(destination);
        if (destination == this) {
            throw new IllegalArgumentException("The destination collection is this queue");
        }
        if (maxElements <= 0) {
            return 0;
        }
        synchronized (monitor) {
            int drained = 0;
            while (first != null && drained < maxElements) {
                E element = first.element;
                destination.add(element);
                dequeue();
                drained++;
            }
            if (drained > 0) {
                monitor.notifyAll();
            }
            return drained;
        }
    }

    @Override
    public TIterator<E> iterator() {
        List<Node<E>> snapshot = new ArrayList<>();
        synchronized (monitor) {
            for (Node<E> node = first; node != null; node = node.next) {
                snapshot.add(node);
            }
        }
        return new TIterator<E>() {
            private int index;
            private E lastReturned;
            private boolean canRemove;

            @Override
            public boolean hasNext() {
                return index < snapshot.size();
            }

            @Override
            @SuppressWarnings("unchecked")
            public E next() {
                if (!hasNext()) {
                    throw new java.util.NoSuchElementException();
                }
                lastReturned = snapshot.get(index++).element;
                canRemove = true;
                return lastReturned;
            }

            @Override
            public void remove() {
                if (!canRemove) {
                    throw new IllegalStateException();
                }
                removeNode(snapshot.get(index - 1));
                canRemove = false;
            }
        };
    }

    private void removeNode(Node<E> target) {
        synchronized (monitor) {
            Node<E> previous = null;
            for (Node<E> node = first; node != null; node = node.next) {
                if (node == target) {
                    unlink(previous, node);
                    monitor.notifyAll();
                    return;
                }
                previous = node;
            }
        }
    }

    private void checkInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    private void waitInterruptibly(long millis, int nanos) throws InterruptedException {
        try {
            monitor.wait(millis, nanos);
        } catch (InterruptedException e) {
            Thread.interrupted();
            throw e;
        }
    }

    private void enqueue(E element) {
        Node<E> node = new Node<>(element);
        if (last == null) {
            first = node;
        } else {
            last.next = node;
        }
        last = node;
        count++;
    }

    private E dequeue() {
        Node<E> node = first;
        first = node.next;
        if (first == null) {
            last = null;
        }
        count--;
        node.next = null;
        return node.element;
    }

    private void unlink(Node<E> previous, Node<E> node) {
        if (previous == null) {
            first = node.next;
        } else {
            previous.next = node.next;
        }
        if (last == node) {
            last = previous;
        }
        node.next = null;
        count--;
    }

    private static final class Node<E> {
        final E element;
        Node<E> next;

        Node(E element) {
            this.element = element;
        }
    }
}
