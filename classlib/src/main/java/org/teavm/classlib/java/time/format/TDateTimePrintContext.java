package org.teavm.classlib.java.time.format;

import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.INSTANT_SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoField.OFFSET_SECONDS;

import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.chrono.TChronoLocalDate;
import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TValueRange;
import org.teavm.classlib.java.util.TLocale;
import org.teavm.classlib.java.util.TObjects;

final class TDateTimePrintContext {

  private TTemporalAccessor temporal;

  private TDateTimeFormatter formatter;

  private int optional;

  TDateTimePrintContext(TTemporalAccessor temporal, TDateTimeFormatter formatter) {

    super();
    this.temporal = adjust(temporal, formatter);
    this.formatter = formatter;
  }

  private static TTemporalAccessor adjust(final TTemporalAccessor temporal, TDateTimeFormatter formatter) {

    TChronology overrideChrono = formatter.getChronology();
    TZoneId overrideZone = formatter.getZone();
    if (overrideChrono == null && overrideZone == null) {
      return temporal;
    }

    TChronology temporalChrono = temporal.query(TTemporalQueries.chronology());
    TZoneId temporalZone = temporal.query(TTemporalQueries.zoneId());
    if (TObjects.equals(overrideChrono, temporalChrono)) {
      overrideChrono = null;
    }
    if (TObjects.equals(overrideZone, temporalZone)) {
      overrideZone = null;
    }
    if (overrideChrono == null && overrideZone == null) {
      return temporal;
    }

    final TChronology effectiveChrono = (overrideChrono != null ? overrideChrono : temporalChrono);
    if (overrideZone != null) {
      if (temporal.isSupported(INSTANT_SECONDS)) {
        TChronology chrono = (effectiveChrono != null) ? effectiveChrono : TIsoChronology.INSTANCE;
        return chrono.zonedDateTime(TInstant.from(temporal), overrideZone);
      }
      if (overrideZone.normalized() instanceof TZoneOffset && temporal.isSupported(OFFSET_SECONDS)
          && temporal.get(OFFSET_SECONDS) != overrideZone.getRules().getOffset(TInstant.EPOCH).getTotalSeconds()) {
        throw new TDateTimeException("Unable to apply override zone '" + overrideZone
            + "' because the temporal object being formatted has a different offset but"
            + " does not represent an instant: " + temporal);
      }
    }
    final TZoneId effectiveZone = (overrideZone != null ? overrideZone : temporalZone);
    final TChronoLocalDate effectiveDate;
    if (overrideChrono != null) {
      if (temporal.isSupported(EPOCH_DAY)) {
        effectiveDate = effectiveChrono.date(temporal);
      } else {
        if (!(overrideChrono == TIsoChronology.INSTANCE && temporalChrono == null)) {
          for (TChronoField f : TChronoField.values()) {
            if (f.isDateBased() && temporal.isSupported(f)) {
              throw new TDateTimeException("Unable to apply override chronology '" + overrideChrono
                  + "' because the temporal object being formatted contains date fields but"
                  + " does not represent a whole date: " + temporal);
            }
          }
        }
        effectiveDate = null;
      }
    } else {
      effectiveDate = null;
    }

    return new TTemporalAccessor() {
      @Override
      public boolean isSupported(TTemporalField field) {

        if (effectiveDate != null && field.isDateBased()) {
          return effectiveDate.isSupported(field);
        }
        return temporal.isSupported(field);
      }

      @Override
      public TValueRange range(TTemporalField field) {

        if (effectiveDate != null && field.isDateBased()) {
          return effectiveDate.range(field);
        }
        return temporal.range(field);
      }

      @Override
      public long getLong(TTemporalField field) {

        if (effectiveDate != null && field.isDateBased()) {
          return effectiveDate.getLong(field);
        }
        return temporal.getLong(field);
      }

      @SuppressWarnings("unchecked")
      @Override
      public <R> R query(TTemporalQuery<R> query) {

        if (query == TTemporalQueries.chronology()) {
          return (R) effectiveChrono;
        }
        if (query == TTemporalQueries.zoneId()) {
          return (R) effectiveZone;
        }
        if (query == TTemporalQueries.precision()) {
          return temporal.query(query);
        }
        return query.queryFrom(this);
      }

      @Override
      public String toString() {

        return temporal + (effectiveChrono != null ? " with chronology " + effectiveChrono : "")
            + (effectiveZone != null ? " with zone " + effectiveZone : "");
      }
    };
  }

  TTemporalAccessor getTemporal() {

    return this.temporal;
  }

  TLocale getLocale() {

    return this.formatter.getLocale();
  }

  TDecimalStyle getDecimalStyle() {

    return this.formatter.getDecimalStyle();
  }

  void startOptional() {

    this.optional++;
  }

  void endOptional() {

    this.optional--;
  }

  <R> R getValue(TTemporalQuery<R> query) {

    R result = this.temporal.query(query);
    if (result == null && this.optional == 0) {
      throw new TDateTimeException("Unable to extract " + query + " from temporal " + this.temporal);
    }
    return result;
  }

  Long getValue(TTemporalField field) {

    if (this.optional > 0 && !this.temporal.isSupported(field)) {
      return null;
    }
    return this.temporal.getLong(field);
  }

  @Override
  public String toString() {

    return this.temporal.toString();
  }

}
