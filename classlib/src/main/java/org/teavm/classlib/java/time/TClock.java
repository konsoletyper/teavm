package org.teavm.classlib.java.time;

import static org.teavm.classlib.java.time.TLocalTime.NANOS_PER_MILLI;
import static org.teavm.classlib.java.time.TLocalTime.NANOS_PER_MINUTE;
import static org.teavm.classlib.java.time.TLocalTime.NANOS_PER_SECOND;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.TSystem;
import org.teavm.classlib.java.util.TObjects;

public abstract class TClock {

  public static TClock systemUTC() {

    return TSystemClock.UTC;
  }

  public static TClock systemDefaultZone() {

    return new TSystemClock(TZoneId.systemDefault());
  }

  public static TClock system(TZoneId zone) {

    TObjects.requireNonNull(zone, "zone");
    if (zone == TZoneOffset.UTC) {
      return TSystemClock.UTC;
    }
    return new TSystemClock(zone);
  }

  public static TClock tickMillis(TZoneId zone) {

    return new TTickClock(system(zone), NANOS_PER_MILLI);
  }

  public static TClock tickSeconds(TZoneId zone) {

    return new TTickClock(system(zone), NANOS_PER_SECOND);
  }

  public static TClock tickMinutes(TZoneId zone) {

    return new TTickClock(system(zone), NANOS_PER_MINUTE);
  }

  public static TClock tick(TClock baseClock, TDuration tickDuration) {

    TObjects.requireNonNull(baseClock, "baseClock");
    TObjects.requireNonNull(tickDuration, "tickDuration");
    if (tickDuration.isNegative()) {
      throw new TIllegalArgumentException("Tick duration must not be negative");
    }
    long tickNanos = tickDuration.toNanos();
    if (tickNanos % 1000_000 == 0) {
    } else if (1000_000_000 % tickNanos == 0) {
    } else {
      throw new TIllegalArgumentException("Invalid tick duration");
    }
    if (tickNanos <= 1) {
      return baseClock;
    }
    return new TTickClock(baseClock, tickNanos);
  }

  public static TClock fixed(TInstant fixedInstant, TZoneId zone) {

    TObjects.requireNonNull(fixedInstant, "fixedInstant");
    TObjects.requireNonNull(zone, "zone");
    return new TFixedClock(fixedInstant, zone);
  }

  public static TClock offset(TClock baseClock, TDuration offsetDuration) {

    TObjects.requireNonNull(baseClock, "baseClock");
    TObjects.requireNonNull(offsetDuration, "offsetDuration");
    if (offsetDuration.equals(TDuration.ZERO)) {
      return baseClock;
    }
    return new TOffsetClock(baseClock, offsetDuration);
  }

  protected TClock() {

  }

  public abstract TZoneId getZone();

  public abstract TClock withZone(TZoneId zone);

  public long millis() {

    return instant().toEpochMilli();
  }

  public abstract TInstant instant();

  static final class TSystemClock extends TClock implements TSerializable {

    static final TSystemClock UTC = new TSystemClock(TZoneOffset.UTC);

    private final TZoneId zone;

    TSystemClock(TZoneId zone) {

      this.zone = zone;
    }

    @Override
    public TZoneId getZone() {

      return this.zone;
    }

    @Override
    public TClock withZone(TZoneId zone) {

      if (zone.equals(this.zone)) {
        return this;
      }
      return new TSystemClock(zone);
    }

    @Override
    public long millis() {

      return TSystem.currentTimeMillis();
    }

    @Override
    public TInstant instant() {

      long localOffset = TSystem.currentTimeMillis();
      // long TSystem.nanoTime() % 1000000000;
      // long adjustment = TSystem.nanoTime() & 0x1FFFFFF;
      // who needs nano precision in the browser after all?
      long adjustment = 0;

      return TInstant.ofEpochSecond(localOffset, adjustment);
    }

    @Override
    public boolean equals(Object obj) {

      if (obj instanceof TSystemClock) {
        return this.zone.equals(((TSystemClock) obj).zone);
      }
      return false;
    }

    @Override
    public int hashCode() {

      return this.zone.hashCode() + 1;
    }

    @Override
    public String toString() {

      return "SystemClock[" + this.zone + "]";
    }

  }

  static final class TFixedClock extends TClock implements TSerializable {

    private final TInstant instant;

    private final TZoneId zone;

    TFixedClock(TInstant fixedInstant, TZoneId zone) {

      this.instant = fixedInstant;
      this.zone = zone;
    }

    @Override
    public TZoneId getZone() {

      return this.zone;
    }

    @Override
    public TClock withZone(TZoneId zone) {

      if (zone.equals(this.zone)) {
        return this;
      }
      return new TFixedClock(this.instant, zone);
    }

    @Override
    public long millis() {

      return this.instant.toEpochMilli();
    }

    @Override
    public TInstant instant() {

      return this.instant;
    }

    @Override
    public boolean equals(Object obj) {

      if (obj instanceof TFixedClock) {
        TFixedClock other = (TFixedClock) obj;
        return this.instant.equals(other.instant) && this.zone.equals(other.zone);
      }
      return false;
    }

    @Override
    public int hashCode() {

      return this.instant.hashCode() ^ this.zone.hashCode();
    }

    @Override
    public String toString() {

      return "FixedClock[" + this.instant + "," + this.zone + "]";
    }
  }

  static final class TOffsetClock extends TClock implements TSerializable {

    private final TClock baseClock;

    private final TDuration offset;

    TOffsetClock(TClock baseClock, TDuration offset) {

      this.baseClock = baseClock;
      this.offset = offset;
    }

    @Override
    public TZoneId getZone() {

      return this.baseClock.getZone();
    }

    @Override
    public TClock withZone(TZoneId zone) {

      if (zone.equals(this.baseClock.getZone())) {
        return this;
      }
      return new TOffsetClock(this.baseClock.withZone(zone), this.offset);
    }

    @Override
    public long millis() {

      return Math.addExact(this.baseClock.millis(), this.offset.toMillis());
    }

    @Override
    public TInstant instant() {

      return this.baseClock.instant().plus(this.offset);
    }

    @Override
    public boolean equals(Object obj) {

      if (obj instanceof TOffsetClock) {
        TOffsetClock other = (TOffsetClock) obj;
        return this.baseClock.equals(other.baseClock) && this.offset.equals(other.offset);
      }
      return false;
    }

    @Override
    public int hashCode() {

      return this.baseClock.hashCode() ^ this.offset.hashCode();
    }

    @Override
    public String toString() {

      return "OffsetClock[" + this.baseClock + "," + this.offset + "]";
    }
  }

  static final class TTickClock extends TClock implements TSerializable {

    private final TClock baseClock;

    private final long tickNanos;

    TTickClock(TClock baseClock, long tickNanos) {

      this.baseClock = baseClock;
      this.tickNanos = tickNanos;
    }

    @Override
    public TZoneId getZone() {

      return this.baseClock.getZone();
    }

    @Override
    public TClock withZone(TZoneId zone) {

      if (zone.equals(this.baseClock.getZone())) {
        return this;
      }
      return new TTickClock(this.baseClock.withZone(zone), this.tickNanos);
    }

    @Override
    public long millis() {

      long millis = this.baseClock.millis();
      return millis - Math.floorMod(millis, this.tickNanos / 1000_000L);
    }

    @Override
    public TInstant instant() {

      if ((this.tickNanos % 1000_000) == 0) {
        long millis = this.baseClock.millis();
        return TInstant.ofEpochMilli(millis - Math.floorMod(millis, this.tickNanos / 1000_000L));
      }
      TInstant instant = this.baseClock.instant();
      long nanos = instant.getNano();
      long adjust = Math.floorMod(nanos, this.tickNanos);
      return instant.minusNanos(adjust);
    }

    @Override
    public boolean equals(Object obj) {

      if (obj instanceof TTickClock) {
        TTickClock other = (TTickClock) obj;
        return this.baseClock.equals(other.baseClock) && this.tickNanos == other.tickNanos;
      }
      return false;
    }

    @Override
    public int hashCode() {

      return this.baseClock.hashCode() ^ ((int) (this.tickNanos ^ (this.tickNanos >>> 32)));
    }

    @Override
    public String toString() {

      return "TickClock[" + this.baseClock + "," + TDuration.ofNanos(this.tickNanos) + "]";
    }
  }

}
