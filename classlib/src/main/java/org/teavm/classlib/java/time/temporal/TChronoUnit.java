package org.teavm.classlib.java.time.temporal;

import org.teavm.classlib.java.time.TDuration;

public enum TChronoUnit implements TTemporalUnit {

  NANOS("Nanos", TDuration.ofNanos(1)), //
  MICROS("Micros", TDuration.ofNanos(1000)), //
  MILLIS("Millis", TDuration.ofNanos(1000_000)), //
  SECONDS("Seconds", TDuration.ofSeconds(1)), //
  MINUTES("Minutes", TDuration.ofSeconds(60)), //
  HOURS("Hours", TDuration.ofSeconds(3600)), //
  HALF_DAYS("HalfDays", TDuration.ofSeconds(43200)), //
  DAYS("Days", TDuration.ofSeconds(86400)), //
  WEEKS("Weeks", TDuration.ofSeconds(7 * 86400L)), //
  MONTHS("Months", TDuration.ofSeconds(31556952L / 12)), //
  YEARS("Years", TDuration.ofSeconds(31556952L)), //
  DECADES("Decades", TDuration.ofSeconds(31556952L * 10L)), //
  CENTURIES("Centuries", TDuration.ofSeconds(31556952L * 100L)), //
  MILLENNIA("Millennia", TDuration.ofSeconds(31556952L * 1000L)), //
  ERAS("Eras", TDuration.ofSeconds(31556952L * 1000_000_000L)), //
  FOREVER("Forever", TDuration.ofSeconds(Long.MAX_VALUE, 999_999_999));

  private final String name;

  private final TDuration duration;

  private TChronoUnit(String name, TDuration estimatedDuration) {

    this.name = name;
    this.duration = estimatedDuration;
  }

  @Override
  public TDuration getDuration() {

    return this.duration;
  }

  @Override
  public boolean isDurationEstimated() {

    return compareTo(DAYS) >= 0;
  }

  @Override
  public boolean isDateBased() {

    return compareTo(DAYS) >= 0 && this != FOREVER;
  }

  @Override
  public boolean isTimeBased() {

    return compareTo(DAYS) < 0;
  }

  @Override
  public boolean isSupportedBy(TTemporal temporal) {

    return temporal.isSupported(this);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <R extends TTemporal> R addTo(R temporal, long amount) {

    return (R) temporal.plus(amount, this);
  }

  @Override
  public long between(TTemporal temporal1Inclusive, TTemporal temporal2Exclusive) {

    return temporal1Inclusive.until(temporal2Exclusive, this);
  }

  @Override
  public String toString() {

    return this.name;
  }

}
