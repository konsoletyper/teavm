package org.teavm.classlib.java.time.zone;

import java.time.LocalDate;
import java.time.zone.ZoneOffsetTransition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.teavm.classlib.java.time.TDuration;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TYear;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.util.TObjects;

public class TZoneRules {
  private static final int LAST_CACHED_YEAR = 2100;

  private final long[] standardTransitions;

  private final TZoneOffset[] standardOffsets;

  private final long[] savingsInstantTransitions;

  private final TLocalDateTime[] savingsLocalTransitions;

  private final TZoneOffset[] wallOffsets;

  private final TZoneOffsetTransitionRule[] lastRules;

  private final transient ConcurrentMap<Integer, TZoneOffsetTransition[]> lastRulesCache = new ConcurrentHashMap<Integer, TZoneOffsetTransition[]>();

  private static final long[] EMPTY_LONG_ARRAY = new long[0];

  private static final TZoneOffsetTransitionRule[] EMPTY_LASTRULES = new TZoneOffsetTransitionRule[0];

  private static final TLocalDateTime[] EMPTY_LDT_ARRAY = new TLocalDateTime[0];

  public static TZoneRules of(TZoneOffset baseStandardOffset, TZoneOffset baseWallOffset,
      List<TZoneOffsetTransition> standardOffsetTransitionList, List<TZoneOffsetTransition> transitionList,
      List<TZoneOffsetTransitionRule> lastRules) {

    TObjects.requireNonNull(baseStandardOffset, "baseStandardOffset");
    TObjects.requireNonNull(baseWallOffset, "baseWallOffset");
    TObjects.requireNonNull(standardOffsetTransitionList, "standardOffsetTransitionList");
    TObjects.requireNonNull(transitionList, "transitionList");
    TObjects.requireNonNull(lastRules, "lastRules");
    return new TZoneRules(baseStandardOffset, baseWallOffset, standardOffsetTransitionList, transitionList, lastRules);
  }

  public static TZoneRules of(TZoneOffset offset) {

    TObjects.requireNonNull(offset, "offset");
    return new TZoneRules(offset);
  }

  TZoneRules(TZoneOffset baseStandardOffset, TZoneOffset baseWallOffset,
      List<TZoneOffsetTransition> standardOffsetTransitionList, List<TZoneOffsetTransition> transitionList,
      List<TZoneOffsetTransitionRule> lastRules) {

    super();

    this.standardTransitions = new long[standardOffsetTransitionList.size()];

    this.standardOffsets = new TZoneOffset[standardOffsetTransitionList.size() + 1];
    this.standardOffsets[0] = baseStandardOffset;
    for (int i = 0; i < standardOffsetTransitionList.size(); i++) {
      this.standardTransitions[i] = standardOffsetTransitionList.get(i).toEpochSecond();
      this.standardOffsets[i + 1] = standardOffsetTransitionList.get(i).getOffsetAfter();
    }

    List<TLocalDateTime> localTransitionList = new ArrayList<>();
    List<TZoneOffset> localTransitionOffsetList = new ArrayList<>();
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

    this.savingsInstantTransitions = new long[transitionList.size()];
    for (int i = 0; i < transitionList.size(); i++) {
      this.savingsInstantTransitions[i] = transitionList.get(i).toEpochSecond();
    }

    if (lastRules.size() > 16) {
      throw new IllegalArgumentException("Too many transition rules");
    }
    this.lastRules = lastRules.toArray(new TZoneOffsetTransitionRule[lastRules.size()]);
  }

  private TZoneRules(long[] standardTransitions, TZoneOffset[] standardOffsets, long[] savingsInstantTransitions,
      TZoneOffset[] wallOffsets, TZoneOffsetTransitionRule[] lastRules) {

    super();

    this.standardTransitions = standardTransitions;
    this.standardOffsets = standardOffsets;
    this.savingsInstantTransitions = savingsInstantTransitions;
    this.wallOffsets = wallOffsets;
    this.lastRules = lastRules;

    if (savingsInstantTransitions.length == 0) {
      this.savingsLocalTransitions = EMPTY_LDT_ARRAY;
    } else {
      // convert savings transitions to locals
      List<TLocalDateTime> localTransitionList = new ArrayList<>();
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
  }

  private TZoneRules(TZoneOffset offset) {

    this.standardOffsets = new TZoneOffset[1];
    this.standardOffsets[0] = offset;
    this.standardTransitions = EMPTY_LONG_ARRAY;
    this.savingsInstantTransitions = EMPTY_LONG_ARRAY;
    this.savingsLocalTransitions = EMPTY_LDT_ARRAY;
    this.wallOffsets = this.standardOffsets;
    this.lastRules = EMPTY_LASTRULES;
  }

  public boolean isFixedOffset() {

    return this.savingsInstantTransitions.length == 0;
  }

  public TZoneOffset getOffset(TInstant instant) {

    if (this.savingsInstantTransitions.length == 0) {
      return this.standardOffsets[0];
    }
    long epochSec = instant.getEpochSecond();
    // check if using last rules
    if (this.lastRules.length > 0
        && epochSec > this.savingsInstantTransitions[this.savingsInstantTransitions.length - 1]) {
      int year = findYear(epochSec, this.wallOffsets[this.wallOffsets.length - 1]);
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

    int index = Arrays.binarySearch(this.savingsInstantTransitions, epochSec);
    if (index < 0) {
      index = -index - 2;
    }
    return this.wallOffsets[index + 1];
  }

  public TZoneOffset getOffset(TLocalDateTime localDateTime) {

    Object info = getOffsetInfo(localDateTime);
    if (info instanceof TZoneOffsetTransition) {
      return ((TZoneOffsetTransition) info).getOffsetBefore();
    }
    return (TZoneOffset) info;
  }

  public List<TZoneOffset> getValidOffsets(TLocalDateTime localDateTime) {

    Object info = getOffsetInfo(localDateTime);
    if (info instanceof TZoneOffsetTransition) {
      return ((TZoneOffsetTransition) info).getValidOffsets();
    }
    return Collections.singletonList((TZoneOffset) info);
  }

  public TZoneOffsetTransition getTransition(TLocalDateTime localDateTime) {

    Object info = getOffsetInfo(localDateTime);
    return (info instanceof TZoneOffsetTransition ? (TZoneOffsetTransition) info : null);
  }

  private Object getOffsetInfo(TLocalDateTime dt) {

    if (this.savingsInstantTransitions.length == 0) {
      return this.standardOffsets[0];
    }
    if (this.lastRules.length > 0
        && dt.isAfter(this.savingsLocalTransitions[this.savingsLocalTransitions.length - 1])) {
      TZoneOffsetTransition[] transArray = findTransitionArray(dt.getYear());
      Object info = null;
      for (TZoneOffsetTransition trans : transArray) {
        info = findOffsetInfo(dt, trans);
        if (info instanceof ZoneOffsetTransition || info.equals(trans.getOffsetBefore())) {
          return info;
        }
      }
      return info;
    }

    int index = Arrays.binarySearch(this.savingsLocalTransitions, dt);
    if (index == -1) {
      return this.wallOffsets[0];
    }
    if (index < 0) {
      index = -index - 2;
    } else if (index < this.savingsLocalTransitions.length - 1
        && this.savingsLocalTransitions[index].equals(this.savingsLocalTransitions[index + 1])) {
      index++;
    }
    if ((index & 1) == 0) {
      TLocalDateTime dtBefore = this.savingsLocalTransitions[index];
      TLocalDateTime dtAfter = this.savingsLocalTransitions[index + 1];
      TZoneOffset offsetBefore = this.wallOffsets[index / 2];
      TZoneOffset offsetAfter = this.wallOffsets[index / 2 + 1];
      if (offsetAfter.getTotalSeconds() > offsetBefore.getTotalSeconds()) {
        return new TZoneOffsetTransition(dtBefore, offsetBefore, offsetAfter);
      } else {
        return new TZoneOffsetTransition(dtAfter, offsetBefore, offsetAfter);
      }
    } else {
      return this.wallOffsets[index / 2 + 1];
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

  private TZoneOffsetTransition[] findTransitionArray(int year) {

    Integer yearObj = year;
    TZoneOffsetTransition[] transArray = this.lastRulesCache.get(yearObj);
    if (transArray != null) {
      return transArray;
    }
    TZoneOffsetTransitionRule[] ruleArray = this.lastRules;
    transArray = new TZoneOffsetTransition[ruleArray.length];
    for (int i = 0; i < ruleArray.length; i++) {
      transArray[i] = ruleArray[i].createTransition(year);
    }
    if (year < LAST_CACHED_YEAR) {
      this.lastRulesCache.putIfAbsent(yearObj, transArray);
    }
    return transArray;
  }

  public TZoneOffset getStandardOffset(TInstant instant) {

    if (this.savingsInstantTransitions.length == 0) {
      return this.standardOffsets[0];
    }
    long epochSec = instant.getEpochSecond();
    int index = Arrays.binarySearch(this.standardTransitions, epochSec);
    if (index < 0) {
      index = -index - 2;
    }
    return this.standardOffsets[index + 1];
  }

  public TDuration getDaylightSavings(TInstant instant) {

    if (this.savingsInstantTransitions.length == 0) {
      return TDuration.ZERO;
    }
    TZoneOffset standardOffset = getStandardOffset(instant);
    TZoneOffset actualOffset = getOffset(instant);
    return TDuration.ofSeconds(actualOffset.getTotalSeconds() - standardOffset.getTotalSeconds());
  }

  public boolean isDaylightSavings(TInstant instant) {

    return (getStandardOffset(instant).equals(getOffset(instant)) == false);
  }

  public boolean isValidOffset(TLocalDateTime localDateTime, TZoneOffset offset) {

    return getValidOffsets(localDateTime).contains(offset);
  }

  public TZoneOffsetTransition nextTransition(TInstant instant) {

    if (this.savingsInstantTransitions.length == 0) {
      return null;
    }
    long epochSec = instant.getEpochSecond();
    if (epochSec >= this.savingsInstantTransitions[this.savingsInstantTransitions.length - 1]) {
      if (this.lastRules.length == 0) {
        return null;
      }
      int year = findYear(epochSec, this.wallOffsets[this.wallOffsets.length - 1]);
      TZoneOffsetTransition[] transArray = findTransitionArray(year);
      for (TZoneOffsetTransition trans : transArray) {
        if (epochSec < trans.toEpochSecond()) {
          return trans;
        }
      }
      if (year < TYear.MAX_VALUE) {
        transArray = findTransitionArray(year + 1);
        return transArray[0];
      }
      return null;
    }

    int index = Arrays.binarySearch(this.savingsInstantTransitions, epochSec);
    if (index < 0) {
      index = -index - 1;
    } else {
      index += 1;
    }
    return new TZoneOffsetTransition(this.savingsInstantTransitions[index], this.wallOffsets[index],
        this.wallOffsets[index + 1]);
  }

  public TZoneOffsetTransition previousTransition(TInstant instant) {

    if (this.savingsInstantTransitions.length == 0) {
      return null;
    }
    long epochSec = instant.getEpochSecond();
    if (instant.getNano() > 0 && epochSec < Long.MAX_VALUE) {
      epochSec += 1;
    }

    long lastHistoric = this.savingsInstantTransitions[this.savingsInstantTransitions.length - 1];
    if (this.lastRules.length > 0 && epochSec > lastHistoric) {
      TZoneOffset lastHistoricOffset = this.wallOffsets[this.wallOffsets.length - 1];
      int year = findYear(epochSec, lastHistoricOffset);
      TZoneOffsetTransition[] transArray = findTransitionArray(year);
      for (int i = transArray.length - 1; i >= 0; i--) {
        if (epochSec > transArray[i].toEpochSecond()) {
          return transArray[i];
        }
      }
      int lastHistoricYear = findYear(lastHistoric, lastHistoricOffset);
      if (--year > lastHistoricYear) {
        transArray = findTransitionArray(year);
        return transArray[transArray.length - 1];
      }
    }

    int index = Arrays.binarySearch(this.savingsInstantTransitions, epochSec);
    if (index < 0) {
      index = -index - 1;
    }
    if (index <= 0) {
      return null;
    }
    return new TZoneOffsetTransition(this.savingsInstantTransitions[index - 1], this.wallOffsets[index - 1],
        this.wallOffsets[index]);
  }

  private int findYear(long epochSecond, TZoneOffset offset) {

    long localSecond = epochSecond + offset.getTotalSeconds();
    long localEpochDay = Math.floorDiv(localSecond, 86400);
    return LocalDate.ofEpochDay(localEpochDay).getYear();
  }

  public List<TZoneOffsetTransition> getTransitions() {

    List<TZoneOffsetTransition> list = new ArrayList<>();
    for (int i = 0; i < this.savingsInstantTransitions.length; i++) {
      list.add(
          new TZoneOffsetTransition(this.savingsInstantTransitions[i], this.wallOffsets[i], this.wallOffsets[i + 1]));
    }
    return Collections.unmodifiableList(list);
  }

  public List<TZoneOffsetTransitionRule> getTransitionRules() {

    return List.of(this.lastRules);
  }

  @Override
  public boolean equals(Object otherRules) {

    if (this == otherRules) {
      return true;
    }
    if (otherRules instanceof TZoneRules) {
      TZoneRules other = (TZoneRules) otherRules;
      return Arrays.equals(this.standardTransitions, other.standardTransitions)
          && Arrays.equals(this.standardOffsets, other.standardOffsets)
          && Arrays.equals(this.savingsInstantTransitions, other.savingsInstantTransitions)
          && Arrays.equals(this.wallOffsets, other.wallOffsets) && Arrays.equals(this.lastRules, other.lastRules);
    }
    return false;
  }

  @Override
  public int hashCode() {

    return Arrays.hashCode(this.standardTransitions) ^ Arrays.hashCode(this.standardOffsets)
        ^ Arrays.hashCode(this.savingsInstantTransitions) ^ Arrays.hashCode(this.wallOffsets)
        ^ Arrays.hashCode(this.lastRules);
  }

  @Override
  public String toString() {

    return "ZoneRules[currentStandardOffset=" + this.standardOffsets[this.standardOffsets.length - 1] + "]";
  }

}
