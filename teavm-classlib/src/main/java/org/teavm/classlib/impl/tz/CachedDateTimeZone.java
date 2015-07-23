/*
 *  Copyright 2001-2012 Stephen Colebourne
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
package org.teavm.classlib.impl.tz;

/**
 * Improves the performance of requesting time zone offsets and name keys by
 * caching the results. Time zones that have simple rules or are fixed should
 * not be cached, as it is unlikely to improve performance.
 * <p>
 * CachedDateTimeZone is thread-safe and immutable.
 *
 * @author Brian S O'Neill
 * @since 1.0
 */
public final class CachedDateTimeZone extends StorableDateTimeZone {

    private static final int cInfoCacheMask;

    static {
        cInfoCacheMask = 511;
    }

    /**
     * Returns a new CachedDateTimeZone unless given zone is already cached.
     */
    public static CachedDateTimeZone forZone(StorableDateTimeZone zone) {
        if (zone instanceof CachedDateTimeZone) {
            return (CachedDateTimeZone) zone;
        }
        return new CachedDateTimeZone(zone);
    }

    /*
     * Caching is performed by breaking timeline down into periods of 2^32
     * milliseconds, or about 49.7 days. A year has about 7.3 periods, usually
     * with only 2 time zone offset periods. Most of the 49.7 day periods will
     * have no transition, about one quarter have one transition, and very rare
     * cases have multiple transitions.
     */

    private final StorableDateTimeZone iZone;

    private final transient Info[] iInfoCache = new Info[cInfoCacheMask + 1];

    private CachedDateTimeZone(StorableDateTimeZone zone) {
        super(zone.getID());
        iZone = zone;
    }

    @Override
    public void write(StringBuilder sb) {
        iZone.write(sb);
    }

    /**
     * Returns the DateTimeZone being wrapped.
     */
    public DateTimeZone getUncachedZone() {
        return iZone;
    }

    @Override
    public int getOffset(long instant) {
        return getInfo(instant).getOffset(instant);
    }

    @Override
    public int getStandardOffset(long instant) {
        return getInfo(instant).getStandardOffset(instant);
    }

    @Override
    public boolean isFixed() {
        return iZone.isFixed();
    }

    @Override
    public long nextTransition(long instant) {
        return iZone.nextTransition(instant);
    }

    @Override
    public long previousTransition(long instant) {
        return iZone.previousTransition(instant);
    }

    // Although accessed by multiple threads, this method doesn't need to be
    // synchronized.

    private Info getInfo(long millis) {
        int period = (int) (millis >> 32);
        Info[] cache = iInfoCache;
        int index = period & cInfoCacheMask;
        Info info = cache[index];
        if (info == null || (int) ((info.iPeriodStart >> 32)) != period) {
            info = createInfo(millis);
            cache[index] = info;
        }
        return info;
    }

    private Info createInfo(long millis) {
        long periodStart = millis & (0xffffffffL << 32);
        Info info = new Info(iZone, periodStart);

        long end = periodStart | 0xffffffffL;
        Info chain = info;
        while (true) {
            long next = iZone.nextTransition(periodStart);
            if (next == periodStart || next > end) {
                break;
            }
            periodStart = next;
            chain.iNextInfo = new Info(iZone, periodStart);
            chain = chain.iNextInfo;
        }

        return info;
    }

    private final static class Info {
        // For first Info in chain, iPeriodStart's lower 32 bits are clear.
        public final long iPeriodStart;
        public final DateTimeZone iZoneRef;

        Info iNextInfo;

        private int iOffset = Integer.MIN_VALUE;
        private int iStandardOffset = Integer.MIN_VALUE;

        Info(DateTimeZone zone, long periodStart) {
            iPeriodStart = periodStart;
            iZoneRef = zone;
        }

        public int getOffset(long millis) {
            if (iNextInfo == null || millis < iNextInfo.iPeriodStart) {
                if (iOffset == Integer.MIN_VALUE) {
                    iOffset = iZoneRef.getOffset(iPeriodStart);
                }
                return iOffset;
            }
            return iNextInfo.getOffset(millis);
        }

        public int getStandardOffset(long millis) {
            if (iNextInfo == null || millis < iNextInfo.iPeriodStart) {
                if (iStandardOffset == Integer.MIN_VALUE) {
                    iStandardOffset = iZoneRef.getStandardOffset(iPeriodStart);
                }
                return iStandardOffset;
            }
            return iNextInfo.getStandardOffset(millis);
        }
    }
}
