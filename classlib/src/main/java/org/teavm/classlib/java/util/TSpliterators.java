package org.teavm.classlib.java.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.IntConsumer;

public class TSpliterators {

    public static <T> TSpliterator<T> spliterator(Object[] array, int additionalCharacteristics) {
        TArrayList<T> list = new TArrayList<T>();
        for (Object element : array) {
            list.add((T) element);
        }

        return list.spliterator();
    }

    public static <T> TSpliterator<T> spliterator(Collection<? extends T> c, int characteristics) {
        return ((TCollection) c).spliterator();
    }

    public static TSpliterator.OfInt spliterator(int[] array, int additionalCharacteristics) {
        return spliterator(array, 0, array.length, additionalCharacteristics);
    }

    public static TSpliterator.OfInt spliterator(int[] array, int fromIndex, int toIndex,
                                                 int additionalCharacteristics) {
        return new TSpliterator.OfInt() {
            int index = fromIndex;

            @Override
            public TSpliterator.OfInt trySplit() {
                return null;
            }

            @Override
            public boolean tryAdvance(IntConsumer action) {
                index++;

                if (index < toIndex) {
                    action.accept(array[index]);
                    return true;
                } else {
                    return false;
                }
            }
        };
    }

    public static <T> TSpliterator<T> spliterator(Iterator<? extends T> iterator,
                                                  long size, int characteristics) {
        TArrayList<T> list = new TArrayList<T>();
        while (iterator.hasNext()) {
            list.add((T) iterator.next());
        }

        return list.spliterator();
    }
}