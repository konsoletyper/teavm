package org.teavm.classlib.java.time.chrono;

import org.teavm.classlib.java.time.TDateTimeException;

public enum TIsoEra implements TEra {

  BCE, CE;

  public static TIsoEra of(int isoEra) {

    switch (isoEra) {
      case 0:
        return BCE;
      case 1:
        return CE;
      default:
        throw new TDateTimeException("Invalid era: " + isoEra);
    }
  }

  @Override
  public int getValue() {

    return ordinal();
  }

}
