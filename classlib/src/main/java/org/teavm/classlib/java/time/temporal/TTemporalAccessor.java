package org.teavm.classlib.java.time.temporal;

import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.util.TObjects;

public interface TTemporalAccessor {
  boolean isSupported(TTemporalField field);

  default TValueRange range(TTemporalField field) {

    if (field instanceof TChronoField) {
      if (isSupported(field)) {
        return field.range();
      }
      throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
    }
    TObjects.requireNonNull(field, "field");
    return field.rangeRefinedBy(this);
  }

  default int get(TTemporalField field) {

    TValueRange range = range(field);
    if (range.isIntValue() == false) {
      throw new TUnsupportedTemporalTypeException(
          "Invalid field " + field + " for get() method, use getLong() instead");
    }
    long value = getLong(field);
    if (range.isValidValue(value) == false) {
      throw new TDateTimeException("Invalid value for " + field + " (valid values " + range + "): " + value);
    }
    return (int) value;
  }

  long getLong(TTemporalField field);

  default <R> R query(TTemporalQuery<R> query) {

    if (query == TTemporalQueries.zoneId() || query == TTemporalQueries.chronology()
        || query == TTemporalQueries.precision()) {
      return null;
    }
    return query.queryFrom(this);
  }

}
