package org.teavm.classlib.java.time.format;

public enum TSignStyle {
  NORMAL, ALWAYS, NEVER, NOT_NEGATIVE, EXCEEDS_PAD;

  boolean parse(boolean positive, boolean strict, boolean fixedWidth) {

    switch (ordinal()) {
      case 0:
        return !positive || !strict;
      case 1:
      case 4:
        return true;
      default:
        return !strict && !fixedWidth;
    }
  }

}
