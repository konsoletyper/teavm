/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.model.transformation;

import java.util.Arrays;

/**
 * Operations on sets of integers represented as unions of closed ranges.
 *
 * <p>A set is represented by an array of bounds {@code [lo0, hi0, lo1, hi1, ...]}, where ranges are sorted,
 * disjoint and non-adjacent. {@code null} represents the full set of 64-bit integers, an empty array represents
 * an empty set. Number of ranges is limited by {@link #MAX_RANGES}; when there are more ranges, the closest
 * ones are merged, so that the result is always a superset of the exact result.</p>
 */
final class IntegerRanges {
    static final int MAX_RANGES = 4;
    static final long[] EMPTY = new long[0];
    static final long[] INT_FULL = { Integer.MIN_VALUE, Integer.MAX_VALUE };

    private IntegerRanges() {
    }

    static long[] of(long lo, long hi) {
        if (lo > hi) {
            return EMPTY;
        }
        if (lo == Long.MIN_VALUE && hi == Long.MAX_VALUE) {
            return null;
        }
        return new long[] { lo, hi };
    }

    static long[] point(long value) {
        return new long[] { value, value };
    }

    static boolean isEmpty(long[] ranges) {
        return ranges != null && ranges.length == 0;
    }

    static boolean isPoint(long[] ranges) {
        return ranges != null && ranges.length == 2 && ranges[0] == ranges[1];
    }

    static long min(long[] ranges) {
        return ranges == null ? Long.MIN_VALUE : ranges[0];
    }

    static long max(long[] ranges) {
        return ranges == null ? Long.MAX_VALUE : ranges[ranges.length - 1];
    }

    static boolean contains(long[] ranges, long value) {
        if (ranges == null) {
            return true;
        }
        for (int i = 0; i < ranges.length; i += 2) {
            if (value < ranges[i]) {
                return false;
            }
            if (value <= ranges[i + 1]) {
                return true;
            }
        }
        return false;
    }

    static boolean isSubset(long[] ranges, long lo, long hi) {
        return !isEmpty(ranges) && min(ranges) >= lo && max(ranges) <= hi;
    }

    static boolean equals(long[] a, long[] b) {
        if (a == null || b == null) {
            return a == b;
        }
        return Arrays.equals(a, b);
    }

    static long[] union(long[] a, long[] b) {
        if (a == null || b == null) {
            return null;
        }
        if (a.length == 0) {
            return b;
        }
        if (b.length == 0) {
            return a;
        }
        long[] bounds = new long[a.length + b.length];
        System.arraycopy(a, 0, bounds, 0, a.length);
        System.arraycopy(b, 0, bounds, a.length, b.length);
        return normalize(bounds, bounds.length);
    }

    static long[] intersect(long[] a, long[] b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        long[] result = new long[a.length + b.length];
        int size = 0;
        int i = 0;
        int j = 0;
        while (i < a.length && j < b.length) {
            long lo = Math.max(a[i], b[j]);
            long hi = Math.min(a[i + 1], b[j + 1]);
            if (lo <= hi) {
                result[size++] = lo;
                result[size++] = hi;
            }
            if (a[i + 1] < b[j + 1]) {
                i += 2;
            } else {
                j += 2;
            }
        }
        return size == result.length ? result : Arrays.copyOf(result, size);
    }

    static long[] intersect(long[] a, long lo, long hi) {
        if (lo > hi) {
            return EMPTY;
        }
        return intersect(a, of(lo, hi));
    }

    static long[] exclude(long[] a, long value) {
        if (!contains(a, value)) {
            return a;
        }
        if (a == null) {
            a = new long[] { Long.MIN_VALUE, Long.MAX_VALUE };
        }
        long[] bounds = new long[a.length + 2];
        int size = 0;
        for (int i = 0; i < a.length; i += 2) {
            long lo = a[i];
            long hi = a[i + 1];
            if (value < lo || value > hi) {
                bounds[size++] = lo;
                bounds[size++] = hi;
            } else {
                if (lo < value) {
                    bounds[size++] = lo;
                    bounds[size++] = value - 1;
                }
                if (value < hi) {
                    bounds[size++] = value + 1;
                    bounds[size++] = hi;
                }
            }
        }
        return normalize(bounds, size);
    }

    /**
     * Takes arbitrary list of ranges (possibly unsorted and overlapping) and produces normalized representation.
     */
    static long[] normalize(long[] bounds, int size) {
        int count = size / 2;
        if (count == 0) {
            return EMPTY;
        }
        long[][] pairs = new long[count][];
        for (int i = 0; i < count; ++i) {
            pairs[i] = new long[] { bounds[i * 2], bounds[i * 2 + 1] };
        }
        Arrays.sort(pairs, (p, q) -> Long.compare(p[0], q[0]));

        long[] result = new long[count * 2];
        int resultSize = 0;
        for (long[] pair : pairs) {
            if (resultSize > 0) {
                long lastHi = result[resultSize - 1];
                if (lastHi == Long.MAX_VALUE || pair[0] <= lastHi + 1) {
                    result[resultSize - 1] = Math.max(lastHi, pair[1]);
                    continue;
                }
            }
            result[resultSize++] = pair[0];
            result[resultSize++] = pair[1];
        }

        while (resultSize > MAX_RANGES * 2) {
            // Merge two ranges with the smallest gap between them.
            // Gaps are positive, but may exceed Long.MAX_VALUE, so compare them as unsigned numbers.
            int best = 0;
            long bestGap = result[2] - result[1];
            for (int i = 2; i < resultSize - 2; i += 2) {
                long gap = result[i + 2] - result[i + 1];
                if (Long.compareUnsigned(gap, bestGap) < 0) {
                    bestGap = gap;
                    best = i;
                }
            }
            result[best + 1] = result[best + 3];
            System.arraycopy(result, best + 4, result, best + 2, resultSize - best - 4);
            resultSize -= 2;
        }

        if (resultSize == 2 && result[0] == Long.MIN_VALUE && result[1] == Long.MAX_VALUE) {
            return null;
        }
        return resultSize == result.length ? result : Arrays.copyOf(result, resultSize);
    }

    static String toString(long[] ranges) {
        if (ranges == null) {
            return "full";
        }
        var sb = new StringBuilder("{");
        for (int i = 0; i < ranges.length; i += 2) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('[').append(ranges[i]).append(", ").append(ranges[i + 1]).append(']');
        }
        return sb.append('}').toString();
    }
}
