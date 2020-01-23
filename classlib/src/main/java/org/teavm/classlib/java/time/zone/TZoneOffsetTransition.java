package org.teavm.classlib.java.time.zone;

import java.util.List;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TComparable;
import org.teavm.classlib.java.time.TDuration;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.util.TObjects;

public class TZoneOffsetTransition implements TComparable<TZoneOffsetTransition>, TSerializable {

  private final long epochSecond;

  private final TLocalDateTime transition;

  private final TZoneOffset offsetBefore;

  private final TZoneOffset offsetAfter;

  public static TZoneOffsetTransition of(TLocalDateTime transition, TZoneOffset offsetBefore, TZoneOffset offsetAfter) {

    TObjects.requireNonNull(transition, "transition");
    TObjects.requireNonNull(offsetBefore, "offsetBefore");
    TObjects.requireNonNull(offsetAfter, "offsetAfter");
    if (offsetBefore.equals(offsetAfter)) {
      throw new IllegalArgumentException("Offsets must not be equal");
    }
    if (transition.getNano() != 0) {
      throw new IllegalArgumentException("Nano-of-second must be zero");
    }
    return new TZoneOffsetTransition(transition, offsetBefore, offsetAfter);
  }

  TZoneOffsetTransition(TLocalDateTime transition, TZoneOffset offsetBefore, TZoneOffset offsetAfter) {

    assert transition.getNano() == 0;
    this.epochSecond = transition.toEpochSecond(offsetBefore);
    this.transition = transition;
    this.offsetBefore = offsetBefore;
    this.offsetAfter = offsetAfter;
  }

  TZoneOffsetTransition(long epochSecond, TZoneOffset offsetBefore, TZoneOffset offsetAfter) {

    this.epochSecond = epochSecond;
    this.transition = TLocalDateTime.ofEpochSecond(epochSecond, 0, offsetBefore);
    this.offsetBefore = offsetBefore;
    this.offsetAfter = offsetAfter;
  }

  public TInstant getInstant() {

    return TInstant.ofEpochSecond(this.epochSecond);
  }

  public long toEpochSecond() {

    return this.epochSecond;
  }

  public TLocalDateTime getDateTimeBefore() {

    return this.transition;
  }

  public TLocalDateTime getDateTimeAfter() {

    return this.transition.plusSeconds(getDurationSeconds());
  }

  public TZoneOffset getOffsetBefore() {

    return this.offsetBefore;
  }

  public TZoneOffset getOffsetAfter() {

    return this.offsetAfter;
  }

  public TDuration getDuration() {

    return TDuration.ofSeconds(getDurationSeconds());
  }

  private int getDurationSeconds() {

    return getOffsetAfter().getTotalSeconds() - getOffsetBefore().getTotalSeconds();
  }

  public boolean isGap() {

    return getOffsetAfter().getTotalSeconds() > getOffsetBefore().getTotalSeconds();
  }

  public boolean isOverlap() {

    return getOffsetAfter().getTotalSeconds() < getOffsetBefore().getTotalSeconds();
  }

  public boolean isValidOffset(TZoneOffset offset) {

    return isGap() ? false : (getOffsetBefore().equals(offset) || getOffsetAfter().equals(offset));
  }

  List<TZoneOffset> getValidOffsets() {

    if (isGap()) {
      return List.of();
    }
    return List.of(getOffsetBefore(), getOffsetAfter());
  }

  @Override
  public int compareTo(TZoneOffsetTransition transition) {

    return Long.compare(this.epochSecond, transition.epochSecond);
  }

  @Override
  public boolean equals(Object other) {

    if (other == this) {
      return true;
    }
    if (other instanceof TZoneOffsetTransition) {
      TZoneOffsetTransition d = (TZoneOffsetTransition) other;
      return this.epochSecond == d.epochSecond && this.offsetBefore.equals(d.offsetBefore)
          && this.offsetAfter.equals(d.offsetAfter);
    }
    return false;
  }

  @Override
  public int hashCode() {

    return this.transition.hashCode() ^ this.offsetBefore.hashCode()
        ^ Integer.rotateLeft(this.offsetAfter.hashCode(), 16);
  }

  @Override
  public String toString() {

    StringBuilder buf = new StringBuilder();
    buf.append("Transition[").append(isGap() ? "Gap" : "Overlap").append(" at ").append(this.transition)
        .append(this.offsetBefore).append(" to ").append(this.offsetAfter).append(']');
    return buf.toString();
  }

}
