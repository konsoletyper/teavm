/*
 *  Copyright 2026 Carl Stainton.
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

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TNullPointerException;
import org.teavm.classlib.java.util.TAbstractQueue;
import org.teavm.classlib.java.util.TArrayDeque;
import org.teavm.classlib.java.util.TCollection;
import org.teavm.classlib.java.util.TIterator;
import org.teavm.classlib.java.util.TQueue;

/**
 * A first-in, first-out queue for code compiled by TeaVM in a single-threaded runtime.
 *
 * <p>This class supplies TeaVM's implementation of {@link java.util.concurrent.ConcurrentLinkedQueue}
 * in the class library. Use the standard Java queue type in application and test code rather
 * than this class directly.
 *
 * <p>Elements are added at the tail and read or removed from the head. The queue grows as needed
 * and does not accept {@code null} elements. Reading or polling an empty queue returns {@code null}.
 *
 * <p>The backing storage is a {@link TArrayDeque}. Despite the standard Java type's name, this
 * implementation does not provide thread safety or its weakly consistent iterator behaviour.
 * Use it only where access is single-threaded, such as the browser test runner.
 *
 * @param <E> the type of elements held in the queue
 */
public class TConcurrentLinkedQueue<E> extends TAbstractQueue<E> implements TQueue<E>, TSerializable {
    private final TArrayDeque<E> elements = new TArrayDeque<>();

    /** Creates an empty queue. */
    public TConcurrentLinkedQueue() {
    }

    /**
     * Creates a queue by copying the supplied collection in iteration order.
     *
     * <p>The first element returned by the collection's iterator becomes the queue's head.
     * Later changes to the collection do not change the queue.
     *
     * @param c the collection to copy; neither the collection nor its elements may be {@code null}
     * @throws NullPointerException if the collection is {@code null}
     * @throws TNullPointerException if the collection contains a {@code null} element
     */
    public TConcurrentLinkedQueue(TCollection<? extends E> c) {
        for (TIterator<? extends E> iterator = c.iterator(); iterator.hasNext();) {
            offer(iterator.next());
        }
    }

    /**
     * Adds an element at the tail of the queue.
     *
     * @param e the non-null element to add
     * @return {@code true} when the element has been added
     * @throws TNullPointerException if the element is {@code null}
     */
    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new TNullPointerException();
        }
        return elements.offer(e);
    }

    /**
     * Removes and returns the element at the head of the queue.
     *
     * @return the oldest queued element, or {@code null} if the queue is empty
     */
    @Override
    public E poll() {
        return elements.poll();
    }

    /**
     * Returns the element at the head without removing it.
     *
     * @return the oldest queued element, or {@code null} if the queue is empty
     */
    @Override
    public E peek() {
        return elements.peek();
    }

    /**
     * Returns an iterator over the elements from head to tail.
     *
     * <p>This is the backing deque's iterator, not a snapshot or a weakly consistent iterator.
     * Do not change the queue while traversing it except through the iterator's own remove method.
     *
     * @return an iterator in queue order
     */
    @Override
    public TIterator<E> iterator() {
        return elements.iterator();
    }

    /**
     * Returns the number of elements currently in the queue.
     *
     * @return the element count, or zero if the queue is empty
     */
    @Override
    public int size() {
        return elements.size();
    }

    /**
     * Reports whether the queue contains no elements.
     *
     * @return {@code true} if the queue is empty
     */
    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }
}
