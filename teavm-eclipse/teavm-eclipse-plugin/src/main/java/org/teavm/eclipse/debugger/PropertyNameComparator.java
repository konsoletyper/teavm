package org.teavm.eclipse.debugger;

import java.util.Comparator;

/**
 *
 * @author Alexey Andreev
 */
abstract class PropertyNameComparator<T> implements Comparator<T> {
    abstract String getName(T value);

    @Override
    public int compare(T o1, T o2) {
        String s1 = getName(o1);
        String s2 = getName(o2);
        boolean n1 = isNumber(s1);
        boolean n2 = isNumber(s2);
        if (n1 && n2) {
            return Integer.compare(Integer.parseInt(s1), Integer.parseInt(s2));
        } else if (n1) {
            return -1;
        } else if (n2) {
            return 1;
        } else {
            return s1.compareTo(s2);
        }
    }

    private boolean isNumber(String str) {
        if (str.length() > 9) {
            return false;
        }
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
