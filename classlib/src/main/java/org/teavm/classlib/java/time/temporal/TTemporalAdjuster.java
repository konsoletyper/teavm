package org.teavm.classlib.java.time.temporal;

@FunctionalInterface
public interface TTemporalAdjuster {
  TTemporal adjustInto(TTemporal temporal);
}
