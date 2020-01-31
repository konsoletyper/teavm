/*
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.threeten.bp.zone;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.threeten.bp.Duration;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.Year;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.jdk8.Jdk8Methods;

/**
 * The rules describing how the zone offset varies through the year and historically.
 * <p>
 * This class is used by the TZDB time-zone rules.
 *
 * <h3>Specification for implementors</h3>
 * This class is immutable and thread-safe.
 */
final class StandardZoneRules extends ZoneRules implements Serializable {

    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 3044319355680032515L;
    /**
     * The last year to have its transitions cached.
     */
    private static final int LAST_CACHED_YEAR = 2100;

    /**
     * The transitions between standard offsets (epoch seconds), sorted.
     */
    private final long[] standardTransitions;
    /**
     * The standard offsets.
     */
    private final ZoneOffset[] standardOffsets;
    /**
     * The transitions between instants (epoch seconds), sorted.
     */
    private final long[] savingsInstantTransitions;
    /**
     * The transitions between local date-times, sorted.
     * This is a paired array, where the first entry is the start of the transition
     * and the second entry is the end of the transition.
     */
    private final LocalDateTime[] savingsLocalTransitions;
    /**
     * The wall offsets.
     */
    private final ZoneOffset[] wallOffsets;
    /**
     * The last rule.
     */
    private final ZoneOffsetTransitionRule[] lastRules;
    /**
     * The map of recent transitions.
     */
    private final ConcurrentMap<Integer, ZoneOffsetTransition[]> lastRulesCache =
                new ConcurrentHashMap<Integer, ZoneOffsetTransition[]>();

    /**
     * Creates an instance.
     *
     * @param baseStandardOffset  the standard offset to use before legal rules were set, not null
     * @param baseWallOffset  the wall offset to use before legal rules were set, not null
     * @param standardOffsetTransitionList  the list of changes to the standard offset, not null
     * @param transitionList  the list of transitions, not null
     * @param lastRules  the recurring last rules, size 15 or less, not null
     */
    StandardZoneRules(
            ZoneOffset baseStandardOffset,
            ZoneOffset baseWallOffset,
            List<ZoneOffsetTransition> standardOffsetTransitionList,
            List<ZoneOffsetTransition> transitionList,
            List<ZoneOffsetTransitionRule> lastRules) {
        super();

        // convert standard transitions
        this.standardTransitions = new long[standardOffsetTransitionList.size()];
        this.standardOffsets = new ZoneOffset[standardOffsetTransitionList.size() + 1];
        this.standardOffsets[0] = baseStandardOffset;
        for (int i = 0; i < standardOffsetTransitionList.size(); i++) {
            this.standardTransitions[i] = standardOffsetTransitionList.get(i).toEpochSecond();
            this.standardOffsets[i + 1] = standardOffsetTransitionList.get(i).getOffsetAfter();
        }

        // convert savings transitions to locals
        List<LocalDateTime> localTransitionList = new ArrayList<LocalDateTime>();
        List<ZoneOffset> localTransitionOffsetList = new ArrayList<ZoneOffset>();
        localTransitionOffsetList.add(baseWallOffset);
        for (ZoneOffsetTransition trans : transitionList) {
            if (trans.isGap()) {
                localTransitionList.add(trans.getDateTimeBefore());
                localTransitionList.add(trans.getDateTimeAfter());
            } else {
                localTransitionList.add(trans.getDateTimeAfter());
                localTransitionList.add(trans.getDateTimeBefore());
            }
            localTransitionOffsetList.add(trans.getOffsetAfter());
        }
        this.savingsLocalTransitions = localTransitionList.toArray(new LocalDateTime[localTransitionList.size()]);
        this.wallOffsets = localTransitionOffsetList.toArray(new ZoneOffset[localTransitionOffsetList.size()]);

        // convert savings transitions to instants
        this.savingsInstantTransitions = new long[transitionList.size()];
        for (int i = 0; i < transitionList.size(); i++) {
            this.savingsInstantTransitions[i] = transitionList.get(i).getInstant().getEpochSecond();
        }

        // last rules
        if (lastRules.size() > 15) {
            throw new IllegalArgumentException("Too many transition rules");
        }
        this.lastRules = lastRules.toArray(new ZoneOffsetTransitionRule[lastRules.size()]);
    }

    /**
     * Constructor.
     *
     * @param standardTransitions  the standard transitions, not null
     * @param standardOffsets  the standard offsets, not null
     * @param savingsInstantTransitions  the standard transitions, not null
     * @param wallOffsets  the wall offsets, not null
     * @param lastRules  the recurring last rules, size 15 or less, not null
     */
    private StandardZoneRules(
            long[] standardTransitions,
            ZoneOffset[] standardOffsets,
            long[] savingsInstantTransitions,
            ZoneOffset[] wallOffsets,
            ZoneOffsetTransitionRule[] lastRules) {
        super();

        this.standardTransitions = standardTransitions;
        this.standardOffsets = standardOffsets;
        this.savingsInstantTransitions = savingsInstantTransitions;
        this.wallOffsets = wallOffsets;
        this.lastRules = lastRules;

        // convert savings transitions to locals
        List<LocalDateTime> localTransitionList = new ArrayList<LocalDateTime>();
        for (int i = 0; i < savingsInstantTransitions.length; i++) {
            ZoneOffset before = wallOffsets[i];
            ZoneOffset after = wallOffsets[i + 1];
            ZoneOffsetTransition trans = new ZoneOffsetTransition(savingsInstantTransitions[i], before, after);
            if (trans.isGap()) {
                localTransitionList.add(trans.getDateTimeBefore());
                localTransitionList.add(trans.getDateTimeAfter());
            } else {
                localTransitionList.add(trans.getDateTimeAfter());
                localTransitionList.add(trans.getDateTimeBefore());
            }
        }
        this.savingsLocalTransitions = localTransitionList.toArray(new LocalDateTime[localTransitionList.size()]);
    }

    //-----------------------------------------------------------------------
    /**
     * Uses a serialization delegate.
     *
     * @return the replacing object, not null
     */
    private Object writeReplace() {
        return new Ser(Ser.SZR, this);
    }

    /**
     * Writes the state to the stream.
     *
     * @param out  the output stream, not null
     * @throws IOException if an error occurs
     */
    void writeExternal(DataOutput out) throws IOException {
        out.writeInt(standardTransitions.length);
        for (long trans : standardTransitions) {
            Ser.writeEpochSec(trans, out);
        }
        for (ZoneOffset offset : standardOffsets) {
            Ser.writeOffset(offset, out);
        }
        out.writeInt(savingsInstantTransitions.length);
        for (long trans : savingsInstantTransitions) {
            Ser.writeEpochSec(trans, out);
        }
        for (ZoneOffset offset : wallOffsets) {
            Ser.writeOffset(offset, out);
        }
        out.writeByte(lastRules.length);
        for (ZoneOffsetTransitionRule rule : lastRules) {
            rule.writeExternal(out);
        }
    }

    /**
     * Reads the state from the stream.
     *
     * @param in  the input stream, not null
     * @return the created object, not null
     * @throws IOException if an error occurs
     */
    static StandardZoneRules readExternal(DataInput in) throws IOException, ClassNotFoundException {
        int stdSize = in.readInt();
        long[] stdTrans = new long[stdSize];
        for (int i = 0; i < stdSize; i++) {
            stdTrans[i] = Ser.readEpochSec(in);
        }
        ZoneOffset[] stdOffsets = new ZoneOffset[stdSize + 1];
        for (int i = 0; i < stdOffsets.length; i++) {
            stdOffsets[i] = Ser.readOffset(in);
        }
        int savSize = in.readInt();
        long[] savTrans = new long[savSize];
        for (int i = 0; i < savSize; i++) {
            savTrans[i] = Ser.readEpochSec(in);
        }
        ZoneOffset[] savOffsets = new ZoneOffset[savSize + 1];
        for (int i = 0; i < savOffsets.length; i++) {
            savOffsets[i] = Ser.readOffset(in);
        }
        int ruleSize = in.readByte();
        ZoneOffsetTransitionRule[] rules = new ZoneOffsetTransitionRule[ruleSize];
        for (int i = 0; i < ruleSize; i++) {
            rules[i] = ZoneOffsetTransitionRule.readExternal(in);
        }
        return new StandardZoneRules(stdTrans, stdOffsets, savTrans, savOffsets, rules);
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean isFixedOffset() {
        return savingsInstantTransitions.length == 0;
    }

    //-----------------------------------------------------------------------
    @Override
    public ZoneOffset getOffset(Instant instant) {
        long epochSec = instant.getEpochSecond();

        // check if using last rules
        if (lastRules.length > 0 &&
                epochSec > savingsInstantTransitions[savingsInstantTransitions.length - 1]) {
            int year = findYear(epochSec, wallOffsets[wallOffsets.length - 1]);
            ZoneOffsetTransition[] transArray = findTransitionArray(year);
            ZoneOffsetTransition trans = null;
            for (int i = 0; i < transArray.length; i++) {
                trans = transArray[i];
                if (epochSec < trans.toEpochSecond()) {
                    return trans.getOffsetBefore();
                }
            }
            return trans.getOffsetAfter();
        }

        // using historic rules
        int index  = Arrays.binarySearch(savingsInstantTransitions, epochSec);
        if (index < 0) {
            // switch negative insert position to start of matched range
            index = -index - 2;
        }
        return wallOffsets[index + 1];
    }

    //-----------------------------------------------------------------------
    @Override
    public ZoneOffset getOffset(LocalDateTime localDateTime) {
        Object info = getOffsetInfo(localDateTime);
        if (info instanceof ZoneOffsetTransition) {
            return ((ZoneOffsetTransition) info).getOffsetBefore();
        }
        return (ZoneOffset) info;
    }

    @Override
    public List<ZoneOffset> getValidOffsets(LocalDateTime localDateTime) {
        // should probably be optimized
        Object info = getOffsetInfo(localDateTime);
        if (info instanceof ZoneOffsetTransition) {
            return ((ZoneOffsetTransition) info).getValidOffsets();
        }
        return Collections.singletonList((ZoneOffset) info);
    }

    @Override
    public ZoneOffsetTransition getTransition(LocalDateTime localDateTime) {
        Object info = getOffsetInfo(localDateTime);
        return (info instanceof ZoneOffsetTransition ? (ZoneOffsetTransition) info : null);
    }

    private Object getOffsetInfo(LocalDateTime dt) {
        // check if using last rules
        if (lastRules.length > 0 &&
                dt.isAfter(savingsLocalTransitions[savingsLocalTransitions.length - 1])) {
            ZoneOffsetTransition[] transArray = findTransitionArray(dt.getYear());
            Object info = null;
            for (ZoneOffsetTransition trans : transArray) {
                info = findOffsetInfo(dt, trans);
                if (info instanceof ZoneOffsetTransition || info.equals(trans.getOffsetBefore())) {
                    return info;
                }
            }
            return info;
        }

        // using historic rules
        int index  = Arrays.binarySearch(savingsLocalTransitions, dt);
        if (index == -1) {
            // before first transition
            return wallOffsets[0];
        }
        if (index < 0) {
            // switch negative insert position to start of matched range
            index = -index - 2;
        } else if (index < savingsLocalTransitions.length - 1 &&
                savingsLocalTransitions[index].equals(savingsLocalTransitions[index + 1])) {
            // handle overlap immediately following gap
            index++;
        }
        if ((index & 1) == 0) {
            // gap or overlap
            LocalDateTime dtBefore = savingsLocalTransitions[index];
            LocalDateTime dtAfter = savingsLocalTransitions[index + 1];
            ZoneOffset offsetBefore = wallOffsets[index / 2];
            ZoneOffset offsetAfter = wallOffsets[index / 2 + 1];
            if (offsetAfter.getTotalSeconds() > offsetBefore.getTotalSeconds()) {
                // gap
                return new ZoneOffsetTransition(dtBefore, offsetBefore, offsetAfter);
            } else {
                // overlap
                return new ZoneOffsetTransition(dtAfter, offsetBefore, offsetAfter);
            }
        } else {
            // normal (neither gap or overlap)
            return wallOffsets[index / 2 + 1];
        }
    }

    /**
     * Finds the offset info for a local date-time and transition.
     *
     * @param dt  the date-time, not null
     * @param trans  the transition, not null
     * @return the offset info, not null
     */
    private Object findOffsetInfo(LocalDateTime dt, ZoneOffsetTransition trans) {
        LocalDateTime localTransition = trans.getDateTimeBefore();
        if (trans.isGap()) {
            if (dt.isBefore(localTransition)) {
                return trans.getOffsetBefore();
            }
            if (dt.isBefore(trans.getDateTimeAfter())) {
                return trans;
            } else {
                return trans.getOffsetAfter();
            }
        } else {
            if (dt.isBefore(localTransition) == false) {
                return trans.getOffsetAfter();
            }
            if (dt.isBefore(trans.getDateTimeAfter())) {
                return trans.getOffsetBefore();
            } else {
                return trans;
            }
        }
    }

    @Override
    public boolean isValidOffset(LocalDateTime localDateTime, ZoneOffset offset) {
        return getValidOffsets(localDateTime).contains(offset);
    }

    //-----------------------------------------------------------------------
    /**
     * Finds the appropriate transition array for the given year.
     *
     * @param year  the year, not null
     * @return the transition array, not null
     */
    private ZoneOffsetTransition[] findTransitionArray(int year) {
        Integer yearObj = year;  // should use Year class, but this saves a class load
        ZoneOffsetTransition[] transArray = lastRulesCache.get(yearObj);
        if (transArray != null) {
            return transArray;
        }
        ZoneOffsetTransitionRule[] ruleArray = lastRules;
        transArray  = new ZoneOffsetTransition[ruleArray.length];
        for (int i = 0; i < ruleArray.length; i++) {
            transArray[i] = ruleArray[i].createTransition(year);
        }
        if (year < LAST_CACHED_YEAR) {
            lastRulesCache.putIfAbsent(yearObj, transArray);
        }
        return transArray;
    }

    //-----------------------------------------------------------------------
    @Override
    public ZoneOffset getStandardOffset(Instant instant) {
        long epochSec = instant.getEpochSecond();
        int index  = Arrays.binarySearch(standardTransitions, epochSec);
        if (index < 0) {
            // switch negative insert position to start of matched range
            index = -index - 2;
        }
        return standardOffsets[index + 1];
    }

    @Override
    public Duration getDaylightSavings(Instant instant) {
        ZoneOffset standardOffset = getStandardOffset(instant);
        ZoneOffset actualOffset = getOffset(instant);
        return Duration.ofSeconds(actualOffset.getTotalSeconds() - standardOffset.getTotalSeconds());
    }

    @Override
    public boolean isDaylightSavings(Instant instant) {
        return (getStandardOffset(instant).equals(getOffset(instant)) == false);
    }

    //-----------------------------------------------------------------------
    @Override
    public ZoneOffsetTransition nextTransition(Instant instant) {
        if (savingsInstantTransitions.length == 0) {
            return null;
        }
        
        long epochSec = instant.getEpochSecond();

        // check if using last rules
        if (epochSec >= savingsInstantTransitions[savingsInstantTransitions.length - 1]) {
            if (lastRules.length == 0) {
                return null;
            }
            // search year the instant is in
            int year = findYear(epochSec, wallOffsets[wallOffsets.length - 1]);
            ZoneOffsetTransition[] transArray = findTransitionArray(year);
            for (ZoneOffsetTransition trans : transArray) {
                if (epochSec < trans.toEpochSecond()) {
                    return trans;
                }
            }
            // use first from following year
            if (year < Year.MAX_VALUE) {
                transArray = findTransitionArray(year + 1);
                return transArray[0];
            }
            return null;
        }

        // using historic rules
        int index  = Arrays.binarySearch(savingsInstantTransitions, epochSec);
        if (index < 0) {
            index = -index - 1;  // switched value is the next transition
        } else {
            index += 1;  // exact match, so need to add one to get the next
        }
        return new ZoneOffsetTransition(savingsInstantTransitions[index], wallOffsets[index], wallOffsets[index + 1]);
    }

    @Override
    public ZoneOffsetTransition previousTransition(Instant instant) {
        if (savingsInstantTransitions.length == 0) {
            return null;
        }
        
        long epochSec = instant.getEpochSecond();
        if (instant.getNano() > 0 && epochSec < Long.MAX_VALUE) {
            epochSec += 1;  // allow rest of method to only use seconds
        }

        // check if using last rules
        long lastHistoric = savingsInstantTransitions[savingsInstantTransitions.length - 1];
        if (lastRules.length > 0 && epochSec > lastHistoric) {
            // search year the instant is in
            ZoneOffset lastHistoricOffset = wallOffsets[wallOffsets.length - 1];
            int year = findYear(epochSec, lastHistoricOffset);
            ZoneOffsetTransition[] transArray = findTransitionArray(year);
            for (int i = transArray.length - 1; i >= 0; i--) {
                if (epochSec > transArray[i].toEpochSecond()) {
                    return transArray[i];
                }
            }
            // use last from preceeding year
            int lastHistoricYear = findYear(lastHistoric, lastHistoricOffset);
            if (--year > lastHistoricYear) {
                transArray = findTransitionArray(year);
                return transArray[transArray.length - 1];
            }
            // drop through
        }

        // using historic rules
        int index  = Arrays.binarySearch(savingsInstantTransitions, epochSec);
        if (index < 0) {
            index = -index - 1;
        }
        if (index <= 0) {
            return null;
        }
        return new ZoneOffsetTransition(savingsInstantTransitions[index - 1], wallOffsets[index - 1], wallOffsets[index]);
    }

    private int findYear(long epochSecond, ZoneOffset offset) {
        // inline for performance
        long localSecond = epochSecond + offset.getTotalSeconds();
        long localEpochDay = Jdk8Methods.floorDiv(localSecond, 86400);
        return LocalDate.ofEpochDay(localEpochDay).getYear();
    }

    //-------------------------------------------------------------------------
    @Override
    public List<ZoneOffsetTransition> getTransitions() {
        List<ZoneOffsetTransition> list = new ArrayList<ZoneOffsetTransition>();
        for (int i = 0; i < savingsInstantTransitions.length; i++) {
            list.add(new ZoneOffsetTransition(savingsInstantTransitions[i], wallOffsets[i], wallOffsets[i + 1]));
        }
        return Collections.unmodifiableList(list);
    }

    @Override
    public List<ZoneOffsetTransitionRule> getTransitionRules() {
        return Collections.unmodifiableList(Arrays.asList(lastRules));
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
           return true;
        }
        if (obj instanceof StandardZoneRules) {
            StandardZoneRules other = (StandardZoneRules) obj;
            return Arrays.equals(standardTransitions, other.standardTransitions) &&
                    Arrays.equals(standardOffsets, other.standardOffsets) &&
                    Arrays.equals(savingsInstantTransitions, other.savingsInstantTransitions) &&
                    Arrays.equals(wallOffsets, other.wallOffsets) &&
                    Arrays.equals(lastRules, other.lastRules);
        }
        if (obj instanceof Fixed) {
            return isFixedOffset() && getOffset(Instant.EPOCH).equals(((Fixed) obj).getOffset(Instant.EPOCH));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(standardTransitions) ^
                Arrays.hashCode(standardOffsets) ^
                Arrays.hashCode(savingsInstantTransitions) ^
                Arrays.hashCode(wallOffsets) ^
                Arrays.hashCode(lastRules);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a string describing this object.
     *
     * @return a string for debugging, not null
     */
    @Override
    public String toString() {
        return "StandardZoneRules[currentStandardOffset=" + standardOffsets[standardOffsets.length - 1] + "]";
    }

}
