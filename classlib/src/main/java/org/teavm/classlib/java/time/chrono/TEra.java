package org.teavm.classlib.java.time.chrono;

import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAdjuster;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;

public interface TEra extends TTemporalAccessor, TTemporalAdjuster {

  int getValue();

  @Override
  default boolean isSupported(TTemporalField field) {

    if (field instanceof TChronoField) {
      return field == TChronoField.ERA;
    }
    return field != null && field.isSupportedBy(this);
  }

  @Override
  default int get(TTemporalField field) {

    if (field == TChronoField.ERA) {
      return getValue();
    }
    return TTemporalAccessor.super.get(field);
  }

  @Override
  default long getLong(TTemporalField field) {

    if (field == TChronoField.ERA) {
      return getValue();
    } else if (field instanceof TChronoField) {
      throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
    }
    return field.getFrom(this);
  }

  @SuppressWarnings("unchecked")
  @Override
  default <R> R query(TTemporalQuery<R> query) {

    if (query == TTemporalQueries.precision()) {
      return (R) TChronoUnit.ERAS;
    }
    return TTemporalAccessor.super.query(query);
  }

  @Override
  default TTemporal adjustInto(TTemporal temporal) {

    return temporal.with(TChronoField.ERA, getValue());
  }

  // default String getDisplayName(TTextStyle style, TLocale locale) {
  //
  // return new TDateTimeFormatterBuilder().appendText(TChronoField.ERA, style).toFormatter(locale).format(this);
  // }

}
