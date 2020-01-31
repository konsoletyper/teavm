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
package org.teavm.classlib.java.time.zone;

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

import org.teavm.classlib.java.time.TDuration;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TYear;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;

final class TStandardZoneRules extends TZoneRules implements Serializable {

    private static final long serialVersionUID = 3044319355680032515L;
    private static final int LAST_CACHED_YEAR = 2100;

    private final long[] standardTransitions;
    private final TZoneOffset[] standardOffsets;
    private final long[] savingsInstantTransitions;
    private final TLocalDateTime[] savingsLocalTransitions;
    private final TZoneOffset[] wallOffsets;
    private final TZoneOffsetTransitionRule[] lastRules;
    private final ConcurrentMap<Integer, TZoneOffsetTransition[]> lastRulesCache =
                new ConcurrentHashMap<Integer, TZoneOffsetTransition[]>();

    TStandardZoneRules(
            TZoneOffset baseStandardOffset,
            TZoneOffset baseWallOffset,
            List<TZoneOffsetTransition> standardOffsetTransitionList,
            List<TZoneOffsetTransition> transitionList,
            List<TZoneOffsetTransitionRule> lastRules) {
        super();

        // convert standard transitions
        this.standardTransitions = new long[standardOffsetTransitionList.size()];
        this.standardOffsets = new TZoneOffset[standardOffsetTransitionList.size() + 1];
        this.standardOffsets[0] = baseStandardOffset;
        for (int i = 0; i < standardOffsetTransitionList.size(); i++) {
            this.standardTransitions[i] = standardOffsetTransitionList.get(i).toEpochSecond();
            this.standardOffsets[i + 1] = standardOffsetTransitionList.get(i).getOffsetAfter();
        }

        // convert savings transitions to locals
        List<TLocalDateTime> localTransitionList = new ArrayList<TLocalDateTime>();
        List<TZoneOffset> localTransitionOffsetList = new ArrayList<TZoneOffset>();
        localTransitionOffsetList.add(baseWallOffset);
        for (TZoneOffsetTransition trans : transitionList) {
            if (trans.isGap()) {
                localTransitionList.add(trans.getDateTimeBefore());
                localTransitionList.add(trans.getDateTimeAfter());
            } else {
                localTransitionList.add(trans.getDateTimeAfter());
                localTransitionList.add(trans.getDateTimeBefore());
            }
            localTransitionOffsetList.add(trans.getOffsetAfter());
        }
        this.savingsLocalTransitions = localTransitionList.toArray(new TLocalDateTime[localTransitionList.size()]);
        this.wallOffsets = localTransitionOffsetList.toArray(new TZoneOffset[localTransitionOffsetList.size()]);

        // convert savings transitions to instants
        this.savingsInstantTransitions = new long[transitionList.size()];
        for (int i = 0; i < transitionList.size(); i++) {
            this.savingsInstantTransitions[i] = transitionList.get(i).getInstant().getEpochSecond();
        }

        // last rules
        if (lastRules.size() > 15) {
            throw new IllegalArgumentException("Too many transition rules");
        }
        this.lastRules = lastRules.toArray(new TZoneOffsetTransitionRule[lastRules.size()]);
    }

    private TStandardZoneRules(
            long[] standardTransitions,
            TZoneOffset[] standardOffsets,
            long[] savingsInstantTransitions,
            TZoneOffset[] wallOffsets,
            TZoneOffsetTransitionRule[] lastRules) {
        super();

        this.standardTransitions = standardTransitions;
        this.standardOffsets = standardOffsets;
        this.savingsInstantTransitions = savingsInstantTransitions;
        this.wallOffsets = wallOffsets;
        this.lastRules = lastRules;

        // convert savings transitions to locals
        List<TLocalDateTime> localTransitionList = new ArrayList<TLocalDateTime>();
        for (int i = 0; i < savingsInstantTransitions.length; i++) {
            TZoneOffset before = wallOffsets[i];
            TZoneOffset after = wallOffsets[i + 1];
            TZoneOffsetTransition trans = new TZoneOffsetTransition(savingsInstantTransitions[i], before, after);
            if (trans.isGap()) {
                localTransitionList.add(trans.getDateTimeBefore());
                localTransitionList.add(trans.getDateTimeAfter());
            } else {
                localTransitionList.add(trans.getDateTimeAfter());
                localTransitionList.add(trans.getDateTimeBefore());
            }
        }
        this.savingsLocalTransitions = localTransitionList.toArray(new TLocalDateTime[localTransitionList.size()]);
    }

    //-----------------------------------------------------------------------
    private Object writeReplace() {
        return new Ser(Ser.SZR, this);
    }

    void writeExternal(DataOutput out) throws IOException {
        out.writeInt(standardTransitions.length);
        for (long trans : standardTransitions) {
            Ser.writeEpochSec(trans, out);
        }
        for (TZoneOffset offset : standardOffsets) {
            Ser.writeOffset(offset, out);
        }
        out.writeInt(savingsInstantTransitions.length);
        for (long trans : savingsInstantTransitions) {
            Ser.writeEpochSec(trans, out);
        }
        for (TZoneOffset offset : wallOffsets) {
            Ser.writeOffset(offset, out);
        }
        out.writeByte(lastRules.length);
        for (TZoneOffsetTransitionRule rule : lastRules) {
            rule.writeExternal(out);
        }
    }

    static TStandardZoneRules readExternal(DataInput in) throws IOException, ClassNotFoundException {
        int stdSize = in.readInt();
        long[] stdTrans = new long[stdSize];
        for (int i = 0; i < stdSize; i++) {
            stdTrans[i] = Ser.readEpochSec(in);
        }
        TZoneOffset[] stdOffsets = new TZoneOffset[stdSize + 1];
        for (int i = 0; i < stdOffsets.length; i++) {
            stdOffsets[i] = Ser.readOffset(in);
        }
        int savSize = in.readInt();
        long[] savTrans = new long[savSize];
        for (int i = 0; i < savSize; i++) {
            savTrans[i] = Ser.readEpochSec(in);
        }
        TZoneOffset[] savOffsets = new TZoneOffset[savSize + 1];
        for (int i = 0; i < savOffsets.length; i++) {
            savOffsets[i] = Ser.readOffset(in);
        }
        int ruleSize = in.readByte();
        TZoneOffsetTransitionRule[] rules = new TZoneOffsetTransitionRule[ruleSize];
        for (int i = 0; i < ruleSize; i++) {
            rules[i] = TZoneOffsetTransitionRule.readExternal(in);
        }
        return new TStandardZoneRules(stdTrans, stdOffsets, savTrans, savOffsets, rules);
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean isFixedOffset() {
        return savingsInstantTransitions.length == 0;
    }

    //-----------------------------------------------------------------------
    @Override
    public TZoneOffset getOffset(TInstant instant) {
        long epochSec = instant.getEpochSecond();

        // check if using last rules
        if (lastRules.length > 0 &&
                epochSec > savingsInstantTransitions[savingsInstantTransitions.length - 1]) {
            int year = findYear(epochSec, wallOffsets[wallOffsets.length - 1]);
            TZoneOffsetTransition[] transArray = findTransitionArray(year);
            TZoneOffsetTransition trans = null;
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
    public TZoneOffset getOffset(TLocalDateTime localDateTime) {
        Object info = getOffsetInfo(localDateTime);
        if (info instanceof TZoneOffsetTransition) {
            return ((TZoneOffsetTransition) info).getOffsetBefore();
        }
        return (TZoneOffset) info;
    }

    @Override
    public List<TZoneOffset> getValidOffsets(TLocalDateTime localDateTime) {
        // should probably be optimized
        Object info = getOffsetInfo(localDateTime);
        if (info instanceof TZoneOffsetTransition) {
            return ((TZoneOffsetTransition) info).getValidOffsets();
        }
        return Collections.singletonList((TZoneOffset) info);
    }

    @Override
    public TZoneOffsetTransition getTransition(TLocalDateTime localDateTime) {
        Object info = getOffsetInfo(localDateTime);
        return (info instanceof TZoneOffsetTransition ? (TZoneOffsetTransition) info : null);
    }

    private Object getOffsetInfo(TLocalDateTime dt) {
        // check if using last rules
        if (lastRules.length > 0 &&
                dt.isAfter(savingsLocalTransitions[savingsLocalTransitions.length - 1])) {
            TZoneOffsetTransition[] transArray = findTransitionArray(dt.getYear());
            Object info = null;
            for (TZoneOffsetTransition trans : transArray) {
                info = findOffsetInfo(dt, trans);
                if (info instanceof TZoneOffsetTransition || info.equals(trans.getOffsetBefore())) {
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
            TLocalDateTime dtBefore = savingsLocalTransitions[index];
            TLocalDateTime dtAfter = savingsLocalTransitions[index + 1];
            TZoneOffset offsetBefore = wallOffsets[index / 2];
            TZoneOffset offsetAfter = wallOffsets[index / 2 + 1];
            if (offsetAfter.getTotalSeconds() > offsetBefore.getTotalSeconds()) {
                // gap
                return new TZoneOffsetTransition(dtBefore, offsetBefore, offsetAfter);
            } else {
                // overlap
                return new TZoneOffsetTransition(dtAfter, offsetBefore, offsetAfter);
            }
        } else {
            // normal (neither gap or overlap)
            return wallOffsets[index / 2 + 1];
        }
    }

    private Object findOffsetInfo(TLocalDateTime dt, TZoneOffsetTransition trans) {
        TLocalDateTime localTransition = trans.getDateTimeBefore();
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
    public boolean isValidOffset(TLocalDateTime localDateTime, TZoneOffset offset) {
        return getValidOffsets(localDateTime).contains(offset);
    }

    //-----------------------------------------------------------------------
    private TZoneOffsetTransition[] findTransitionArray(int year) {
        Integer yearObj = year;  // should use TYear class, but this saves a class load
        TZoneOffsetTransition[] transArray = lastRulesCache.get(yearObj);
        if (transArray != null) {
            return transArray;
        }
        TZoneOffsetTransitionRule[] ruleArray = lastRules;
        transArray  = new TZoneOffsetTransition[ruleArray.length];
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
    public TZoneOffset getStandardOffset(TInstant instant) {
        long epochSec = instant.getEpochSecond();
        int index  = Arrays.binarySearch(standardTransitions, epochSec);
        if (index < 0) {
            // switch negative insert position to start of matched range
            index = -index - 2;
        }
        return standardOffsets[index + 1];
    }

    @Override
    public TDuration getDaylightSavings(TInstant instant) {
        TZoneOffset standardOffset = getStandardOffset(instant);
        TZoneOffset actualOffset = getOffset(instant);
        return TDuration.ofSeconds(actualOffset.getTotalSeconds() - standardOffset.getTotalSeconds());
    }

    @Override
    public boolean isDaylightSavings(TInstant instant) {
        return (getStandardOffset(instant).equals(getOffset(instant)) == false);
    }

    //-----------------------------------------------------------------------
    @Override
    public TZoneOffsetTransition nextTransition(TInstant instant) {
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
            TZoneOffsetTransition[] transArray = findTransitionArray(year);
            for (TZoneOffsetTransition trans : transArray) {
                if (epochSec < trans.toEpochSecond()) {
                    return trans;
                }
            }
            // use first from following year
            if (year < TYear.MAX_VALUE) {
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
        return new TZoneOffsetTransition(savingsInstantTransitions[index], wallOffsets[index], wallOffsets[index + 1]);
    }

    @Override
    public TZoneOffsetTransition previousTransition(TInstant instant) {
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
            TZoneOffset lastHistoricOffset = wallOffsets[wallOffsets.length - 1];
            int year = findYear(epochSec, lastHistoricOffset);
            TZoneOffsetTransition[] transArray = findTransitionArray(year);
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
        return new TZoneOffsetTransition(savingsInstantTransitions[index - 1], wallOffsets[index - 1], wallOffsets[index]);
    }

    private int findYear(long epochSecond, TZoneOffset offset) {
        // inline for performance
        long localSecond = epochSecond + offset.getTotalSeconds();
        long localEpochDay = TJdk8Methods.floorDiv(localSecond, 86400);
        return TLocalDate.ofEpochDay(localEpochDay).getYear();
    }

    //-------------------------------------------------------------------------
    @Override
    public List<TZoneOffsetTransition> getTransitions() {
        List<TZoneOffsetTransition> list = new ArrayList<TZoneOffsetTransition>();
        for (int i = 0; i < savingsInstantTransitions.length; i++) {
            list.add(new TZoneOffsetTransition(savingsInstantTransitions[i], wallOffsets[i], wallOffsets[i + 1]));
        }
        return Collections.unmodifiableList(list);
    }

    @Override
    public List<TZoneOffsetTransitionRule> getTransitionRules() {
        return Collections.unmodifiableList(Arrays.asList(lastRules));
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
           return true;
        }
        if (obj instanceof TStandardZoneRules) {
            TStandardZoneRules other = (TStandardZoneRules) obj;
            return Arrays.equals(standardTransitions, other.standardTransitions) &&
                    Arrays.equals(standardOffsets, other.standardOffsets) &&
                    Arrays.equals(savingsInstantTransitions, other.savingsInstantTransitions) &&
                    Arrays.equals(wallOffsets, other.wallOffsets) &&
                    Arrays.equals(lastRules, other.lastRules);
        }
        if (obj instanceof Fixed) {
            return isFixedOffset() && getOffset(TInstant.EPOCH).equals(((Fixed) obj).getOffset(TInstant.EPOCH));
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
    @Override
    public String toString() {
        return "TStandardZoneRules[currentStandardOffset=" + standardOffsets[standardOffsets.length - 1] + "]";
    }

}
