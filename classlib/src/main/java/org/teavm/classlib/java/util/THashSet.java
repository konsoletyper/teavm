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
package org.teavm.classlib.java.util;

import java.util.Iterator;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TCloneNotSupportedException;
import org.teavm.classlib.java.lang.TCloneable;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.interop.Rename;

/**
 *
 * @author Alexey Andreev
 * @param <E>
 */
public class THashSet<E> extends TAbstractSet<E> implements TCloneable, TSerializable {
    transient THashMap<E, THashSet<E>> backingMap;

    /**
     * Constructs a new empty instance of {@code HashSet}.
     */
    public THashSet() {
        this(new THashMap<E, THashSet<E>>());
    }

    /**
     * Constructs a new instance of {@code HashSet} with the specified capacity.
     *
     * @param capacity
     *            the initial capacity of this {@code HashSet}.
     */
    public THashSet(int capacity) {
        this(new THashMap<E, THashSet<E>>(capacity));
    }

    /**
     * Constructs a new instance of {@code HashSet} with the specified capacity
     * and load factor.
     *
     * @param capacity
     *            the initial capacity.
     * @param loadFactor
     *            the initial load factor.
     */
    public THashSet(int capacity, float loadFactor) {
        this(new THashMap<E, THashSet<E>>(capacity, loadFactor));
    }

    /**
     * Constructs a new instance of {@code HashSet} containing the unique
     * elements in the specified collection.
     *
     * @param collection
     *            the collection of elements to add.
     */
    public THashSet(TCollection<? extends E> collection) {
        this(new THashMap<E, THashSet<E>>(collection.size() < 6 ? 11 : collection.size() * 2));
        for (TIterator<? extends E> iter = collection.iterator(); iter.hasNext();) {
            add(iter.next());
        }
    }

    THashSet(THashMap<E, THashSet<E>> backingMap) {
        this.backingMap = backingMap;
    }

    /**
     * Adds the specified object to this {@code HashSet} if not already present.
     *
     * @param object
     *            the object to add.
     * @return {@code true} when this {@code HashSet} did not already contain
     *         the object, {@code false} otherwise
     */
    @Override
    public boolean add(E object) {
        return backingMap.put(object, this) == null;
    }

    /**
     * Removes all elements from this {@code HashSet}, leaving it empty.
     *
     * @see #isEmpty
     * @see #size
     */
    @Override
    public void clear() {
        backingMap.clear();
    }

    /**
     * Returns a new {@code HashSet} with the same elements and size as this
     * {@code HashSet}.
     *
     * @return a shallow copy of this {@code HashSet}.
     * @see java.lang.Cloneable
     */
    @Rename("clone")
    @SuppressWarnings("unchecked")
    public TObject clone0() {
        try {
            THashSet<E> clone = (THashSet<E>) super.clone();
            clone.backingMap = (THashMap<E, THashSet<E>>) backingMap.clone();
            return clone;
        } catch (TCloneNotSupportedException e) {
            return null;
        }
    }

    /**
     * Searches this {@code HashSet} for the specified object.
     *
     * @param object
     *            the object to search for.
     * @return {@code true} if {@code object} is an element of this
     *         {@code HashSet}, {@code false} otherwise.
     */
    @Override
    public boolean contains(Object object) {
        return backingMap.containsKey(object);
    }

    /**
     * Returns true if this {@code HashSet} has no elements, false otherwise.
     *
     * @return {@code true} if this {@code HashSet} has no elements,
     *         {@code false} otherwise.
     * @see #size
     */
    @Override
    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    /**
     * Returns an Iterator on the elements of this {@code HashSet}.
     *
     * @return an Iterator on the elements of this {@code HashSet}.
     * @see Iterator
     */
    @Override
    public TIterator<E> iterator() {
        return backingMap.keySet().iterator();
    }

    /**
     * Removes the specified object from this {@code HashSet}.
     *
     * @param object
     *            the object to remove.
     * @return {@code true} if the object was removed, {@code false} otherwise.
     */
    @Override
    public boolean remove(Object object) {
        return backingMap.remove(object) != null;
    }

    /**
     * Returns the number of elements in this {@code HashSet}.
     *
     * @return the number of elements in this {@code HashSet}.
     */
    @Override
    public int size() {
        return backingMap.size();
    }

    THashMap<E, THashSet<E>> createBackingMap(int capacity, float loadFactor) {
        return new THashMap<>(capacity, loadFactor);
    }

    @Override
    public Object clone() {
        return new THashSet<>(this);
    }
}
