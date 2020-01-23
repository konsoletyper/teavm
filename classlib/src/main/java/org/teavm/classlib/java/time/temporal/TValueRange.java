package org.teavm.classlib.java.time.temporal;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.time.TDateTimeException;

public final class TValueRange implements TSerializable {

  private final long minSmallest;

  private final long minLargest;

  private final long maxSmallest;

  private final long maxLargest;

  private TValueRange(long minSmallest, long minLargest, long maxSmallest, long maxLargest) {

    this.minSmallest = minSmallest;
    this.minLargest = minLargest;
    this.maxSmallest = maxSmallest;
    this.maxLargest = maxLargest;
  }

  public boolean isFixed() {

    return this.minSmallest == this.minLargest && this.maxSmallest == this.maxLargest;
  }

  public long getMinimum() {

    return this.minSmallest;
  }

  public long getLargestMinimum() {

    return this.minLargest;
  }

  public long getSmallestMaximum() {

    return this.maxSmallest;
  }

  public long getMaximum() {

    return this.maxLargest;
  }

  public boolean isIntValue() {

    return getMinimum() >= Integer.MIN_VALUE && getMaximum() <= Integer.MAX_VALUE;
  }

  public boolean isValidValue(long value) {

    return (value >= getMinimum() && value <= getMaximum());
  }

  public boolean isValidIntValue(long value) {

    return isIntValue() && isValidValue(value);
  }

  public long checkValidValue(long value, TTemporalField field) {

    if (isValidValue(value) == false) {
      throw new TDateTimeException(genInvalidFieldMessage(field, value));
    }
    return value;
  }

  public int checkValidIntValue(long value, TTemporalField field) {

    if (isValidIntValue(value) == false) {
      throw new TDateTimeException(genInvalidFieldMessage(field, value));
    }
    return (int) value;
  }

  private String genInvalidFieldMessage(TTemporalField field, long value) {

    if (field != null) {
      return "Invalid value for " + field + " (valid values " + this + "): " + value;
    } else {
      return "Invalid value (valid values " + this + "): " + value;
    }
  }

  @Override
  public boolean equals(Object obj) {

    if (obj == this) {
      return true;
    }
    if (obj instanceof TValueRange) {
      TValueRange other = (TValueRange) obj;
      return this.minSmallest == other.minSmallest && this.minLargest == other.minLargest
          && this.maxSmallest == other.maxSmallest && this.maxLargest == other.maxLargest;
    }
    return false;
  }

  @Override
  public int hashCode() {

    long hash = this.minSmallest + (this.minLargest << 16) + (this.minLargest >> 48) + (this.maxSmallest << 32)
        + (this.maxSmallest >> 32) + (this.maxLargest << 48) + (this.maxLargest >> 16);
    return (int) (hash ^ (hash >>> 32));
  }

  @Override
  public String toString() {

    StringBuilder buf = new StringBuilder();
    buf.append(this.minSmallest);
    if (this.minSmallest != this.minLargest) {
      buf.append('/').append(this.minLargest);
    }
    buf.append(" - ").append(this.maxSmallest);
    if (this.maxSmallest != this.maxLargest) {
      buf.append('/').append(this.maxLargest);
    }
    return buf.toString();
  }

  public static TValueRange of(long min, long max) {

    if (min > max) {
      throw new IllegalArgumentException("Minimum value must be less than maximum value");
    }
    return new TValueRange(min, min, max, max);
  }

  public static TValueRange of(long min, long maxSmallest, long maxLargest) {

    return of(min, min, maxSmallest, maxLargest);
  }

  public static TValueRange of(long minSmallest, long minLargest, long maxSmallest, long maxLargest) {

    if (minSmallest > minLargest) {
      throw new IllegalArgumentException("Smallest minimum value must be less than largest minimum value");
    }
    if (maxSmallest > maxLargest) {
      throw new IllegalArgumentException("Smallest maximum value must be less than largest maximum value");
    }
    if (minLargest > maxLargest) {
      throw new IllegalArgumentException("Minimum value must be less than maximum value");
    }
    return new TValueRange(minSmallest, minLargest, maxSmallest, maxLargest);
  }

}
