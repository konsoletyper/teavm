package org.teavm.classlib.java.time.temporal;

@FunctionalInterface
public interface TTemporalQuery<R> {
  R queryFrom(TTemporalAccessor temporal);
}
