package org.teavm.classlib.java.time.temporal;

import org.teavm.classlib.java.lang.TRuntimeException;
import org.teavm.classlib.java.time.TDuration;
import org.teavm.classlib.java.time.TLocalTime;

public interface TTemporalUnit {

  TDuration getDuration();

  boolean isDurationEstimated();

  boolean isDateBased();

  boolean isTimeBased();

  default boolean isSupportedBy(TTemporal temporal) {

    if (temporal instanceof TLocalTime) {
      return isTimeBased();
    }
    // if (temporal instanceof TChronoLocalDate) {
    // return isDateBased();
    // }
    // if (temporal instanceof TChronoLocalDateTime || temporal instanceof TChronoZonedDateTime) {
    // return true;
    // }
    try {
      temporal.plus(1, this);
      return true;
    } catch (TUnsupportedTemporalTypeException ex) {
      return false;
    } catch (TRuntimeException ex) {
      try {
        temporal.plus(-1, this);
        return true;
      } catch (TRuntimeException ex2) {
        return false;
      }
    }
  }

  <R extends TTemporal> R addTo(R temporal, long amount);

  long between(TTemporal temporal1Inclusive, TTemporal temporal2Exclusive);

}
