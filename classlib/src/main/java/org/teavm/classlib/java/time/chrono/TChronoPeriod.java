package org.teavm.classlib.java.time.chrono;

import org.teavm.classlib.java.time.temporal.TTemporalAmount;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;

public interface TChronoPeriod extends TTemporalAmount {
  TChronology getChronology();

  default boolean isZero() {

    for (TTemporalUnit unit : getUnits()) {
      if (get(unit) != 0) {
        return false;
      }
    }
    return true;
  }

  default boolean isNegative() {

    for (TTemporalUnit unit : getUnits()) {
      if (get(unit) < 0) {
        return true;
      }
    }
    return false;
  }

  TChronoPeriod plus(TTemporalAmount amountToAdd);

  TChronoPeriod minus(TTemporalAmount amountToSubtract);

  TChronoPeriod multipliedBy(int scalar);

  default TChronoPeriod negated() {

    return multipliedBy(-1);
  }

  TChronoPeriod normalized();

}
