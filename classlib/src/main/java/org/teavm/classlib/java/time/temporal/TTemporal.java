package org.teavm.classlib.java.time.temporal;

public interface TTemporal extends TTemporalAccessor {

  boolean isSupported(TTemporalUnit unit);

  default TTemporal with(TTemporalAdjuster adjuster) {

    return adjuster.adjustInto(this);
  }

  TTemporal with(TTemporalField field, long newValue);

  default TTemporal plus(TTemporalAmount amount) {

    return amount.addTo(this);
  }

  TTemporal plus(long amountToAdd, TTemporalUnit unit);

  default TTemporal minus(TTemporalAmount amount) {

    return amount.subtractFrom(this);
  }

  default TTemporal minus(long amountToSubtract, TTemporalUnit unit) {

    return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit)
        : plus(-amountToSubtract, unit));
  }

  long until(TTemporal endExclusive, TTemporalUnit unit);

}
