package org.teavm.classlib.java.time.temporal;

import java.util.List;

public interface TTemporalAmount {

  long get(TTemporalUnit unit);

  List<TTemporalUnit> getUnits();

  TTemporal addTo(TTemporal temporal);

  TTemporal subtractFrom(TTemporal temporal);
}
