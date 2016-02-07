/*
 *  Copyright 2001-2014 Stephen Colebourne
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
 * DateTimeZone represents a time zone.
 * <p>
 * A time zone is a system of rules to convert time from one geographic
 * location to another. For example, Paris, France is one hour ahead of
 * London, England. Thus when it is 10:00 in London, it is 11:00 in Paris.
 * <p>
 * All time zone rules are expressed, for historical reasons, relative to
 * Greenwich, London. Local time in Greenwich is referred to as Greenwich Mean
 * Time (GMT).  This is similar, but not precisely identical, to Universal
 * Coordinated Time, or UTC. This library only uses the term UTC.
 * <p>
 * Using this system, America/Los_Angeles is expressed as UTC-08:00, or UTC-07:00
 * in the summer. The offset -08:00 indicates that America/Los_Angeles time is
 * obtained from UTC by adding -08:00, that is, by subtracting 8 hours.
 * <p>
 * The offset differs in the summer because of daylight saving time, or DST.
 * The following definitions of time are generally used:
 * <ul>
 * <li>UTC - The reference time.
 * <li>Standard Time - The local time without a daylight saving time offset.
 * For example, in Paris, standard time is UTC+01:00.
 * <li>Daylight Saving Time - The local time with a daylight saving time
 * offset. This offset is typically one hour, but not always. It is typically
 * used in most countries away from the equator.  In Paris, daylight saving
 * time is UTC+02:00.
 * <li>Wall Time - This is what a local clock on the wall reads. This will be
 * either Standard Time or Daylight Saving Time depending on the time of year
 * and whether the location uses Daylight Saving Time.
 * </ul>
 * <p>
 * Unlike the Java TimeZone class, DateTimeZone is immutable. It also only
 * supports long format time zone ids. Thus EST and ECT are not accepted.
 * However, the factory that accepts a TimeZone will attempt to convert from
 * the old short id to a suitable long id.
 * <p>
 * Unless you override the standard behaviour, the default if the third approach.
 * <p>
 * DateTimeZone is thread-safe and immutable, and all subclasses must be as
 * well.
 *
 * @author Brian S O'Neill
 * @author Stephen Colebourne
 * @since 1.0
 */
public abstract class DateTimeZone {
    static final long MILLIS_PER_HOUR = 3600_000;

    // Instance fields and methods
    //--------------------------------------------------------------------

    private final String iID;

    /**
     * Constructor.
     *
     * @param id  the id to use
     * @throws IllegalArgumentException if the id is null
     */
    protected DateTimeZone(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Id must not be null");
        }
        iID = id;
    }

    // Principal methods
    //--------------------------------------------------------------------

    /**
     * Gets the ID of this datetime zone.
     *
     * @return the ID of this datetime zone
     */
    public final String getID() {
        return iID;
    }

    /**
     * Gets the millisecond offset to add to UTC to get local time.
     *
     * @param instant  milliseconds from 1970-01-01T00:00:00Z to get the offset for
     * @return the millisecond offset to add to UTC to get local time
     */
    public abstract int getOffset(long instant);

    /**
     * Gets the standard millisecond offset to add to UTC to get local time,
     * when standard time is in effect.
     *
     * @param instant  milliseconds from 1970-01-01T00:00:00Z to get the offset for
     * @return the millisecond offset to add to UTC to get local time
     */
    public abstract int getStandardOffset(long instant);

    /**
     * Checks whether, at a particular instant, the offset is standard or not.
     * <p>
     * This method can be used to determine whether Summer Time (DST) applies.
     * As a general rule, if the offset at the specified instant is standard,
     * then either Winter time applies, or there is no Summer Time. If the
     * instant is not standard, then Summer Time applies.
     * <p>
     * The implementation of the method is simply whether {@link #getOffset(long)}
     * equals {@link #getStandardOffset(long)} at the specified instant.
     *
     * @param instant  milliseconds from 1970-01-01T00:00:00Z to get the offset for
     * @return true if the offset at the given instant is the standard offset
     * @since 1.5
     */
    public boolean isStandardOffset(long instant) {
        return getOffset(instant) == getStandardOffset(instant);
    }

    /**
     * Gets the millisecond offset to subtract from local time to get UTC time.
     * This offset can be used to undo adding the offset obtained by getOffset.
     *
     * <pre>
     * millisLocal == millisUTC   + getOffset(millisUTC)
     * millisUTC   == millisLocal - getOffsetFromLocal(millisLocal)
     * </pre>
     *
     * NOTE: After calculating millisLocal, some error may be introduced. At
     * offset transitions (due to DST or other historical changes), ranges of
     * local times may map to different UTC times.
     * <p>
     * For overlaps (where the local time is ambiguous), this method returns the
     * offset applicable before the gap. The effect of this is that any instant
     * calculated using the offset from an overlap will be in "summer" time.
     * <p>
     * For gaps, this method returns the offset applicable before the gap, ie "winter" offset.
     * However, the effect of this is that any instant calculated using the offset
     * from a gap will be after the gap, in "summer" time.
     * <p>
     * For example, consider a zone with a gap from 01:00 to 01:59:<br />
     * Input: 00:00 (before gap) Output: Offset applicable before gap  DateTime: 00:00<br />
     * Input: 00:30 (before gap) Output: Offset applicable before gap  DateTime: 00:30<br />
     * Input: 01:00 (in gap)     Output: Offset applicable before gap  DateTime: 02:00<br />
     * Input: 01:30 (in gap)     Output: Offset applicable before gap  DateTime: 02:30<br />
     * Input: 02:00 (after gap)  Output: Offset applicable after gap   DateTime: 02:00<br />
     * Input: 02:30 (after gap)  Output: Offset applicable after gap   DateTime: 02:30<br />
     * <p>
     * NOTE: Prior to v2.0, the DST overlap behaviour was not defined and varied by hemisphere.
     * Prior to v1.5, the DST gap behaviour was also not defined.
     * In v2.4, the documentation was clarified again.
     *
     * @param instantLocal  the millisecond instant, relative to this time zone, to get the offset for
     * @return the millisecond offset to subtract from local time to get UTC time
     */
    public int getOffsetFromLocal(long instantLocal) {
        // get the offset at instantLocal (first estimate)
        final int offsetLocal = getOffset(instantLocal);
        // adjust instantLocal using the estimate and recalc the offset
        final long instantAdjusted = instantLocal - offsetLocal;
        final int offsetAdjusted = getOffset(instantAdjusted);
        // if the offsets differ, we must be near a DST boundary
        if (offsetLocal != offsetAdjusted) {
            // we need to ensure that time is always after the DST gap
            // this happens naturally for positive offsets, but not for negative
            if ((offsetLocal - offsetAdjusted) < 0) {
                // if we just return offsetAdjusted then the time is pushed
                // back before the transition, whereas it should be
                // on or after the transition
                long nextLocal = nextTransition(instantAdjusted);
                if (nextLocal == (instantLocal - offsetLocal)) {
                    nextLocal = Long.MAX_VALUE;
                }
                long nextAdjusted = nextTransition(instantLocal - offsetAdjusted);
                if (nextAdjusted == (instantLocal - offsetAdjusted)) {
                    nextAdjusted = Long.MAX_VALUE;
                }
                if (nextLocal != nextAdjusted) {
                    return offsetLocal;
                }
            }
        } else if (offsetLocal >= 0) {
            long prev = previousTransition(instantAdjusted);
            if (prev < instantAdjusted) {
                int offsetPrev = getOffset(prev);
                int diff = offsetPrev - offsetLocal;
                if (instantAdjusted - prev <= diff) {
                    return offsetPrev;
                }
            }
        }
        return offsetAdjusted;
    }

    /**
     * Converts a standard UTC instant to a local instant with the same
     * local time. This conversion is used before performing a calculation
     * so that the calculation can be done using a simple local zone.
     *
     * @param instantUTC  the UTC instant to convert to local
     * @return the local instant with the same local time
     * @throws ArithmeticException if the result overflows a long
     * @since 1.5
     */
    public long convertUTCToLocal(long instantUTC) {
        int offset = getOffset(instantUTC);
        long instantLocal = instantUTC + offset;
        // If there is a sign change, but the two values have the same sign...
        if ((instantUTC ^ instantLocal) < 0 && (instantUTC ^ offset) >= 0) {
            throw new ArithmeticException("Adding time zone offset caused overflow");
        }
        return instantLocal;
    }

    /**
     * Converts a local instant to a standard UTC instant with the same
     * local time attempting to use the same offset as the original.
     * <p>
     * This conversion is used after performing a calculation
     * where the calculation was done using a simple local zone.
     * Whenever possible, the same offset as the original offset will be used.
     * This is most significant during a daylight savings overlap.
     *
     * @param instantLocal  the local instant to convert to UTC
     * @param strict  whether the conversion should reject non-existent local times
     * @param originalInstantUTC  the original instant that the calculation is based on
     * @return the UTC instant with the same local time,
     * @throws ArithmeticException if the result overflows a long
     * @throws IllegalArgumentException if the zone has no equivalent local time
     * @since 2.0
     */
    public long convertLocalToUTC(long instantLocal, boolean strict, long originalInstantUTC) {
        int offsetOriginal = getOffset(originalInstantUTC);
        long instantUTC = instantLocal - offsetOriginal;
        int offsetLocalFromOriginal = getOffset(instantUTC);
        if (offsetLocalFromOriginal == offsetOriginal) {
            return instantUTC;
        }
        return convertLocalToUTC(instantLocal, strict);
    }

    /**
     * Converts a local instant to a standard UTC instant with the same
     * local time. This conversion is used after performing a calculation
     * where the calculation was done using a simple local zone.
     *
     * @param instantLocal  the local instant to convert to UTC
     * @param strict  whether the conversion should reject non-existent local times
     * @return the UTC instant with the same local time,
     * @throws ArithmeticException if the result overflows a long
     * @since 1.5
     */
    public long convertLocalToUTC(long instantLocal, boolean strict) {
        // get the offset at instantLocal (first estimate)
        int offsetLocal = getOffset(instantLocal);
        // adjust instantLocal using the estimate and recalc the offset
        int offset = getOffset(instantLocal - offsetLocal);
        // if the offsets differ, we must be near a DST boundary
        if (offsetLocal != offset) {
            // if strict then always check if in DST gap
            // otherwise only check if zone in Western hemisphere (as the
            // value of offset is already correct for Eastern hemisphere)
            if (strict || offsetLocal < 0) {
                // determine if we are in the DST gap
                long nextLocal = nextTransition(instantLocal - offsetLocal);
                if (nextLocal == (instantLocal - offsetLocal)) {
                    nextLocal = Long.MAX_VALUE;
                }
                long nextAdjusted = nextTransition(instantLocal - offset);
                if (nextAdjusted == (instantLocal - offset)) {
                    nextAdjusted = Long.MAX_VALUE;
                }
                if (nextLocal != nextAdjusted) {
                    // yes we are in the DST gap
                    if (strict) {
                        // DST gap is not acceptable
                        throw new RuntimeException(getID());
                    } else {
                        // DST gap is acceptable, but for the Western hemisphere
                        // the offset is wrong and will result in local times
                        // before the cutover so use the offsetLocal instead
                        offset = offsetLocal;
                    }
                }
            }
        }
        // check for overflow
        long instantUTC = instantLocal - offset;
        // If there is a sign change, but the two values have different signs...
        if ((instantLocal ^ instantUTC) < 0 && (instantLocal ^ offset) < 0) {
            throw new ArithmeticException("Subtracting time zone offset caused overflow");
        }
        return instantUTC;
    }

    /**
     * Gets the millisecond instant in another zone keeping the same local time.
     * <p>
     * The conversion is performed by converting the specified UTC millis to local
     * millis in this zone, then converting back to UTC millis in the new zone.
     *
     * @param newZone  the new zone, null means default
     * @param oldInstant  the UTC millisecond instant to convert
     * @return the UTC millisecond instant with the same local time in the new zone
     */
    public long getMillisKeepLocal(DateTimeZone newZone, long oldInstant) {
        if (newZone == this) {
            return oldInstant;
        }
        long instantLocal = convertUTCToLocal(oldInstant);
        return newZone.convertLocalToUTC(instantLocal, false, oldInstant);
    }

//    //-----------------------------------------------------------------------
//    /**
//     * Checks if the given {@link LocalDateTime} is within an overlap.
//     * <p>
//     * When switching from Daylight Savings Time to standard time there is
//     * typically an overlap where the same clock hour occurs twice. This
//     * method identifies whether the local datetime refers to such an overlap.
//     *
//     * @param localDateTime  the time to check, not null
//     * @return true if the given datetime refers to an overlap
//     */
//    public boolean isLocalDateTimeOverlap(LocalDateTime localDateTime) {
//        if (isFixed()) {
//            return false;
//        }
//        long instantLocal = localDateTime.toDateTime(DateTimeZone.UTC).getMillis();
//        // get the offset at instantLocal (first estimate)
//        int offsetLocal = getOffset(instantLocal);
//        // adjust instantLocal using the estimate and recalc the offset
//        int offset = getOffset(instantLocal - offsetLocal);
//        // if the offsets differ, we must be near a DST boundary
//        if (offsetLocal != offset) {
//            long nextLocal = nextTransition(instantLocal - offsetLocal);
//            long nextAdjusted = nextTransition(instantLocal - offset);
//            if (nextLocal != nextAdjusted) {
//                // in DST gap
//                return false;
//            }
//            long diff = Math.abs(offset - offsetLocal);
//            DateTime dateTime = localDateTime.toDateTime(this);
//            DateTime adjusted = dateTime.plus(diff);
//            if (dateTime.getHourOfDay() == adjusted.getHourOfDay() &&
//                    dateTime.getMinuteOfHour() == adjusted.getMinuteOfHour() &&
//                    dateTime.getSecondOfMinute() == adjusted.getSecondOfMinute()) {
//                return true;
//            }
//            adjusted = dateTime.minus(diff);
//            if (dateTime.getHourOfDay() == adjusted.getHourOfDay() &&
//                    dateTime.getMinuteOfHour() == adjusted.getMinuteOfHour() &&
//                    dateTime.getSecondOfMinute() == adjusted.getSecondOfMinute()) {
//                return true;
//            }
//            return false;
//        }
//        return false;
//    }
//
//
//        DateTime dateTime = null;
//        try {
//            dateTime = localDateTime.toDateTime(this);
//        } catch (IllegalArgumentException ex) {
//            return false;  // it is a gap, not an overlap
//        }
//        long offset1 = Math.abs(getOffset(dateTime.getMillis() + 1) - getStandardOffset(dateTime.getMillis() + 1));
//        long offset2 = Math.abs(getOffset(dateTime.getMillis() - 1) - getStandardOffset(dateTime.getMillis() - 1));
//        long offset = Math.max(offset1, offset2);
//        if (offset == 0) {
//            return false;
//        }
//        DateTime adjusted = dateTime.plus(offset);
//        if (dateTime.getHourOfDay() == adjusted.getHourOfDay() &&
//                dateTime.getMinuteOfHour() == adjusted.getMinuteOfHour() &&
//                dateTime.getSecondOfMinute() == adjusted.getSecondOfMinute()) {
//            return true;
//        }
//        adjusted = dateTime.minus(offset);
//        if (dateTime.getHourOfDay() == adjusted.getHourOfDay() &&
//                dateTime.getMinuteOfHour() == adjusted.getMinuteOfHour() &&
//                dateTime.getSecondOfMinute() == adjusted.getSecondOfMinute()) {
//            return true;
//        }
//        return false;

//        long millis = dateTime.getMillis();
//        long nextTransition = nextTransition(millis);
//        long previousTransition = previousTransition(millis);
//        long deltaToPreviousTransition = millis - previousTransition;
//        long deltaToNextTransition = nextTransition - millis;
//        if (deltaToNextTransition < deltaToPreviousTransition) {
//            int offset = getOffset(nextTransition);
//            int standardOffset = getStandardOffset(nextTransition);
//            if (Math.abs(offset - standardOffset) >= deltaToNextTransition) {
//                return true;
//            }
//        } else  {
//            int offset = getOffset(previousTransition);
//            int standardOffset = getStandardOffset(previousTransition);
//            if (Math.abs(offset - standardOffset) >= deltaToPreviousTransition) {
//                return true;
//            }
//        }
//        return false;
//    }

    /**
     * Adjusts the offset to be the earlier or later one during an overlap.
     *
     * @param instant  the instant to adjust
     * @param earlierOrLater  false for earlier, true for later
     * @return the adjusted instant millis
     */
    public long adjustOffset(long instant, boolean earlierOrLater) {
        // a bit messy, but will work in all non-pathological cases

        // evaluate 3 hours before and after to work out if anything is happening
        long instantBefore = instant - 3 * MILLIS_PER_HOUR;
        long instantAfter = instant + 3 * MILLIS_PER_HOUR;
        long offsetBefore = getOffset(instantBefore);
        long offsetAfter = getOffset(instantAfter);
        if (offsetBefore <= offsetAfter) {
            return instant;  // not an overlap (less than is a gap, equal is normal case)
        }

        // work out range of instants that have duplicate local times
        long diff = offsetBefore - offsetAfter;
        long transition = nextTransition(instantBefore);
        long overlapStart = transition - diff;
        long overlapEnd = transition + diff;
        if (instant < overlapStart || instant >= overlapEnd) {
          return instant;  // not an overlap
        }

        // calculate result
        long afterStart = instant - overlapStart;
        if (afterStart >= diff) {
          // currently in later offset
          return earlierOrLater ? instant : instant - diff;
        } else {
          // currently in earlier offset
          return earlierOrLater ? instant + diff : instant;
        }
    }
//    System.out.println(new DateTime(transitionStart, DateTimeZone.UTC) + " " + new DateTime(transitionStart, this));

    //-----------------------------------------------------------------------
    /**
     * Returns true if this time zone has no transitions.
     *
     * @return true if no transitions
     */
    public abstract boolean isFixed();

    /**
     * Advances the given instant to where the time zone offset or name changes.
     * If the instant returned is exactly the same as passed in, then
     * no changes occur after the given instant.
     *
     * @param instant  milliseconds from 1970-01-01T00:00:00Z
     * @return milliseconds from 1970-01-01T00:00:00Z
     */
    public abstract long nextTransition(long instant);

    /**
     * Retreats the given instant to where the time zone offset or name changes.
     * If the instant returned is exactly the same as passed in, then
     * no changes occur before the given instant.
     *
     * @param instant  milliseconds from 1970-01-01T00:00:00Z
     * @return milliseconds from 1970-01-01T00:00:00Z
     */
    public abstract long previousTransition(long instant);
}
