package org.teavm.classlib.java.time.temporal;

import java.util.Map;
import java.util.Objects;

import org.teavm.classlib.java.time.format.TResolverStyle;
import org.teavm.classlib.java.util.TLocale;

public interface TTemporalField {

  default String getDisplayName(TLocale locale) {

    Objects.requireNonNull(locale, "locale");
    return toString();
  }

  TTemporalUnit getBaseUnit();

  TTemporalUnit getRangeUnit();

  TValueRange range();

  boolean isDateBased();

  boolean isTimeBased();

  boolean isSupportedBy(TTemporalAccessor temporal);

  TValueRange rangeRefinedBy(TTemporalAccessor temporal);

  long getFrom(TTemporalAccessor temporal);

  <R extends TTemporal> R adjustInto(R temporal, long newValue);

  default TTemporalAccessor resolve(Map<TTemporalField, Long> fieldValues, TTemporalAccessor partialTemporal,
      TResolverStyle resolverStyle) {

    return null;
  }

}
