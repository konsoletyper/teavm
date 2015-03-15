package org.teavm.classlib.java.util;

/**
 *
 * @author Alexey Andreev
 * @param <E>
 */
public class TTreeSet<E> extends TAbstractSet<E> implements TNavigableSet<E> {
    private static final Object VALUE = new Object();
    private TTreeMap<E, Object> map;

    public TTreeSet() {
        map = new TTreeMap<>();
    }

    public TTreeSet(TComparator<? super E> comparator) {
        map = new TTreeMap<>(comparator);
    }

    public TTreeSet(TCollection<? extends E> coll) {
        map = new TTreeMap<>();
        for (TIterator<? extends E> iter = coll.iterator(); iter.hasNext();) {
            map.put(iter.next(), VALUE);
        }
    }

    public TTreeSet(TSortedSet<E> s) {
        map = new TTreeMap<>(s.comparator());
        for (TIterator<? extends E> iter = s.iterator(); iter.hasNext();) {
            map.put(iter.next(), VALUE);
        }
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public TIterator<E> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public boolean add(E e) {
        return map.put(e, e) != VALUE;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) == VALUE;
    }

    @Override
    public TComparator<? super E> comparator() {
        return map.comparator();
    }

    @Override
    public TSortedSet<E> subSet(E fromElement, E toElement) {
        return map.subMap(fromElement, true, toElement, false).navigableKeySet();
    }

    @Override
    public TSortedSet<E> headSet(E toElement) {
        return map.headMap(toElement, false).navigableKeySet();
    }

    @Override
    public TSortedSet<E> tailSet(E fromElement) {
        return map.tailMap(fromElement, true).navigableKeySet();
    }

    @Override
    public E first() {
        return map.firstKey();
    }

    @Override
    public E last() {
        return map.lastKey();
    }

    @Override
    public E lower(E e) {
        return map.lowerKey(e);
    }

    @Override
    public E floor(E e) {
        return map.floorKey(e);
    }

    @Override
    public E ceiling(E e) {
        return map.ceilingKey(e);
    }

    @Override
    public E higher(E e) {
        return map.higherKey(e);
    }

    @Override
    public E pollFirst() {
        TMap.Entry<E, Object> entry = map.pollFirstEntry();
        return entry != null ? entry.getKey() : null;
    }

    @Override
    public E pollLast() {
        TMap.Entry<E, Object> entry = map.pollLastEntry();
        return entry != null ? entry.getKey() : null;
    }

    @Override
    public TNavigableSet<E> descendingSet() {
        return map.descendingKeySet();
    }

    @Override
    public TIterator<E> descendingIterator() {
        return map.descendingKeySet().iterator();
    }

    @Override
    public TNavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return map.subMap(fromElement, true, toElement, false).navigableKeySet();
    }

    @Override
    public TNavigableSet<E> headSet(E toElement, boolean inclusive) {
        return map.headMap(toElement, inclusive).navigableKeySet();
    }

    @Override
    public TNavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return map.tailMap(fromElement, inclusive).navigableKeySet();
    }
}
