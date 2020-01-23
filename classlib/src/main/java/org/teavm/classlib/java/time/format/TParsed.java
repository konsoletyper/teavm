package org.teavm.classlib.java.time.format;

import static org.teavm.classlib.java.time.temporal.TChronoField.AMPM_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.CLOCK_HOUR_OF_AMPM;
import static org.teavm.classlib.java.time.temporal.TChronoField.CLOCK_HOUR_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.HOUR_OF_AMPM;
import static org.teavm.classlib.java.time.temporal.TChronoField.HOUR_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.INSTANT_SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoField.MICRO_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.MICRO_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.MILLI_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.MILLI_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.MINUTE_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.MINUTE_OF_HOUR;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_SECOND;
import static org.teavm.classlib.java.time.temporal.TChronoField.OFFSET_SECONDS;
import static org.teavm.classlib.java.time.temporal.TChronoField.SECOND_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.SECOND_OF_MINUTE;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TPeriod;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.chrono.TChronoLocalDate;
import org.teavm.classlib.java.time.chrono.TChronoLocalDateTime;
import org.teavm.classlib.java.time.chrono.TChronoZonedDateTime;
import org.teavm.classlib.java.time.chrono.TChronology;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.util.TObjects;

public final class TParsed implements TTemporalAccessor {
  final Map<TTemporalField, Long> fieldValues = new HashMap<>();

  TZoneId zone;

  TChronology chrono;

  boolean leapSecond;

  private TResolverStyle resolverStyle;

  private TChronoLocalDate date;

  private TLocalTime time;

  TPeriod excessDays = TPeriod.ZERO;

  TParsed() {

  }

  TParsed copy() {

    TParsed cloned = new TParsed();
    cloned.fieldValues.putAll(this.fieldValues);
    cloned.zone = this.zone;
    cloned.chrono = this.chrono;
    cloned.leapSecond = this.leapSecond;
    return cloned;
  }

  @Override
  public boolean isSupported(TTemporalField field) {

    if (this.fieldValues.containsKey(field) || (this.date != null && this.date.isSupported(field))
        || (this.time != null && this.time.isSupported(field))) {
      return true;
    }
    return field != null && (field instanceof TChronoField == false) && field.isSupportedBy(this);
  }

  @Override
  public long getLong(TTemporalField field) {

    TObjects.requireNonNull(field, "field");
    Long value = this.fieldValues.get(field);
    if (value != null) {
      return value.longValue();
    }
    if (this.date != null && this.date.isSupported(field)) {
      return this.date.getLong(field);
    }
    if (this.time != null && this.time.isSupported(field)) {
      return this.time.getLong(field);
    }
    if (field instanceof TChronoField) {
      throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
    }
    return field.getFrom(this);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <R> R query(TTemporalQuery<R> query) {

    if (query == TTemporalQueries.zoneId()) {
      return (R) this.zone;
    } else if (query == TTemporalQueries.chronology()) {
      return (R) this.chrono;
    } else if (query == TTemporalQueries.localDate()) {
      return (R) (this.date != null ? TLocalDate.from(this.date) : null);
    } else if (query == TTemporalQueries.localTime()) {
      return (R) this.time;
    } else if (query == TTemporalQueries.offset()) {
      Long offsetSecs = this.fieldValues.get(OFFSET_SECONDS);
      if (offsetSecs != null) {
        return (R) TZoneOffset.ofTotalSeconds(offsetSecs.intValue());
      }
      if (this.zone instanceof TZoneOffset) {
        return (R) this.zone;
      }
      return query.queryFrom(this);
    } else if (query == TTemporalQueries.zone()) {
      return query.queryFrom(this);
    } else if (query == TTemporalQueries.precision()) {
      return null;
    }
    return query.queryFrom(this);
  }

  TTemporalAccessor resolve(TResolverStyle resolverStyle, Set<TTemporalField> resolverFields) {

    if (resolverFields != null) {
      this.fieldValues.keySet().retainAll(resolverFields);
    }
    this.resolverStyle = resolverStyle;
    resolveFields();
    resolveTimeLenient();
    crossCheck();
    resolvePeriod();
    resolveFractional();
    resolveInstant();
    return this;
  }

  private void resolveFields() {

    resolveInstantFields();
    resolveDateFields();
    resolveTimeFields();

    if (this.fieldValues.size() > 0) {
      int changedCount = 0;
      outer: while (changedCount < 50) {
        for (Map.Entry<TTemporalField, Long> entry : this.fieldValues.entrySet()) {
          TTemporalField targetField = entry.getKey();
          TTemporalAccessor resolvedObject = targetField.resolve(this.fieldValues, this, this.resolverStyle);
          if (resolvedObject != null) {
            if (resolvedObject instanceof TChronoZonedDateTime) {
              TChronoZonedDateTime<?> czdt = (TChronoZonedDateTime<?>) resolvedObject;
              if (this.zone == null) {
                this.zone = czdt.getZone();
              } else if (this.zone.equals(czdt.getZone()) == false) {
                throw new TDateTimeException("ChronoZonedDateTime must use the effective parsed zone: " + this.zone);
              }
              resolvedObject = czdt.toLocalDateTime();
            }
            if (resolvedObject instanceof TChronoLocalDateTime) {
              TChronoLocalDateTime<?> cldt = (TChronoLocalDateTime<?>) resolvedObject;
              updateCheckConflict(cldt.toLocalTime(), TPeriod.ZERO);
              updateCheckConflict(cldt.toLocalDate());
              changedCount++;
              continue outer;
            }
            if (resolvedObject instanceof TChronoLocalDate) {
              updateCheckConflict((TChronoLocalDate) resolvedObject);
              changedCount++;
              continue outer;
            }
            if (resolvedObject instanceof TLocalTime) {
              updateCheckConflict((TLocalTime) resolvedObject, TPeriod.ZERO);
              changedCount++;
              continue outer;
            }
            throw new TDateTimeException("Method resolve() can only return ChronoZonedDateTime, "
                + "ChronoLocalDateTime, ChronoLocalDate or LocalTime");
          } else if (this.fieldValues.containsKey(targetField) == false) {
            changedCount++;
            continue outer;
          }
        }
        break;
      }
      if (changedCount == 50) {
        throw new TDateTimeException("One of the parsed fields has an incorrectly implemented resolve method");
      }
      if (changedCount > 0) {
        resolveInstantFields();
        resolveDateFields();
        resolveTimeFields();
      }
    }
  }

  private void updateCheckConflict(TTemporalField targetField, TTemporalField changeField, Long changeValue) {

    Long old = this.fieldValues.put(changeField, changeValue);
    if (old != null && old.longValue() != changeValue.longValue()) {
      throw new TDateTimeException("Conflict found: " + changeField + " " + old + " differs from " + changeField + " "
          + changeValue + " while resolving  " + targetField);
    }
  }

  // -----------------------------------------------------------------------
  private void resolveInstantFields() {

    // resolve parsed instant seconds to date and time if zone available
    if (this.fieldValues.containsKey(INSTANT_SECONDS)) {
      if (this.zone != null) {
        resolveInstantFields0(this.zone);
      } else {
        Long offsetSecs = this.fieldValues.get(OFFSET_SECONDS);
        if (offsetSecs != null) {
          TZoneOffset offset = TZoneOffset.ofTotalSeconds(offsetSecs.intValue());
          resolveInstantFields0(offset);
        }
      }
    }
  }

  private void resolveInstantFields0(TZoneId selectedZone) {

    TInstant instant = TInstant.ofEpochSecond(this.fieldValues.remove(INSTANT_SECONDS));
    TChronoZonedDateTime<?> zdt = this.chrono.zonedDateTime(instant, selectedZone);
    updateCheckConflict(zdt.toLocalDate());
    updateCheckConflict(INSTANT_SECONDS, SECOND_OF_DAY, (long) zdt.toLocalTime().toSecondOfDay());
  }

  private void resolveDateFields() {

    updateCheckConflict(this.chrono.resolveDate(this.fieldValues, this.resolverStyle));
  }

  private void updateCheckConflict(TChronoLocalDate cld) {

    if (this.date != null) {
      if (cld != null && this.date.equals(cld) == false) {
        throw new TDateTimeException(
            "Conflict found: Fields resolved to two different dates: " + this.date + " " + cld);
      }
    } else if (cld != null) {
      if (this.chrono.equals(cld.getChronology()) == false) {
        throw new TDateTimeException("ChronoLocalDate must use the effective parsed chronology: " + this.chrono);
      }
      this.date = cld;
    }
  }

  private void resolveTimeFields() {

    // simplify fields
    if (this.fieldValues.containsKey(CLOCK_HOUR_OF_DAY)) {
      // lenient allows anything, smart allows 0-24, strict allows 1-24
      long ch = this.fieldValues.remove(CLOCK_HOUR_OF_DAY);
      if (this.resolverStyle == TResolverStyle.STRICT || (this.resolverStyle == TResolverStyle.SMART && ch != 0)) {
        CLOCK_HOUR_OF_DAY.checkValidValue(ch);
      }
      updateCheckConflict(CLOCK_HOUR_OF_DAY, HOUR_OF_DAY, ch == 24 ? 0 : ch);
    }
    if (this.fieldValues.containsKey(CLOCK_HOUR_OF_AMPM)) {
      // lenient allows anything, smart allows 0-12, strict allows 1-12
      long ch = this.fieldValues.remove(CLOCK_HOUR_OF_AMPM);
      if (this.resolverStyle == TResolverStyle.STRICT || (this.resolverStyle == TResolverStyle.SMART && ch != 0)) {
        CLOCK_HOUR_OF_AMPM.checkValidValue(ch);
      }
      updateCheckConflict(CLOCK_HOUR_OF_AMPM, HOUR_OF_AMPM, ch == 12 ? 0 : ch);
    }
    if (this.fieldValues.containsKey(AMPM_OF_DAY) && this.fieldValues.containsKey(HOUR_OF_AMPM)) {
      long ap = this.fieldValues.remove(AMPM_OF_DAY);
      long hap = this.fieldValues.remove(HOUR_OF_AMPM);
      if (this.resolverStyle == TResolverStyle.LENIENT) {
        updateCheckConflict(AMPM_OF_DAY, HOUR_OF_DAY, Math.addExact(Math.multiplyExact(ap, 12), hap));
      } else {
        AMPM_OF_DAY.checkValidValue(ap);
        HOUR_OF_AMPM.checkValidValue(ap);
        updateCheckConflict(AMPM_OF_DAY, HOUR_OF_DAY, ap * 12 + hap);
      }
    }
    if (this.fieldValues.containsKey(NANO_OF_DAY)) {
      long nod = this.fieldValues.remove(NANO_OF_DAY);
      if (this.resolverStyle != TResolverStyle.LENIENT) {
        NANO_OF_DAY.checkValidValue(nod);
      }
      updateCheckConflict(NANO_OF_DAY, HOUR_OF_DAY, nod / 3600_000_000_000L);
      updateCheckConflict(NANO_OF_DAY, MINUTE_OF_HOUR, (nod / 60_000_000_000L) % 60);
      updateCheckConflict(NANO_OF_DAY, SECOND_OF_MINUTE, (nod / 1_000_000_000L) % 60);
      updateCheckConflict(NANO_OF_DAY, NANO_OF_SECOND, nod % 1_000_000_000L);
    }
    if (this.fieldValues.containsKey(MICRO_OF_DAY)) {
      long cod = this.fieldValues.remove(MICRO_OF_DAY);
      if (this.resolverStyle != TResolverStyle.LENIENT) {
        MICRO_OF_DAY.checkValidValue(cod);
      }
      updateCheckConflict(MICRO_OF_DAY, SECOND_OF_DAY, cod / 1_000_000L);
      updateCheckConflict(MICRO_OF_DAY, MICRO_OF_SECOND, cod % 1_000_000L);
    }
    if (this.fieldValues.containsKey(MILLI_OF_DAY)) {
      long lod = this.fieldValues.remove(MILLI_OF_DAY);
      if (this.resolverStyle != TResolverStyle.LENIENT) {
        MILLI_OF_DAY.checkValidValue(lod);
      }
      updateCheckConflict(MILLI_OF_DAY, SECOND_OF_DAY, lod / 1_000);
      updateCheckConflict(MILLI_OF_DAY, MILLI_OF_SECOND, lod % 1_000);
    }
    if (this.fieldValues.containsKey(SECOND_OF_DAY)) {
      long sod = this.fieldValues.remove(SECOND_OF_DAY);
      if (this.resolverStyle != TResolverStyle.LENIENT) {
        SECOND_OF_DAY.checkValidValue(sod);
      }
      updateCheckConflict(SECOND_OF_DAY, HOUR_OF_DAY, sod / 3600);
      updateCheckConflict(SECOND_OF_DAY, MINUTE_OF_HOUR, (sod / 60) % 60);
      updateCheckConflict(SECOND_OF_DAY, SECOND_OF_MINUTE, sod % 60);
    }
    if (this.fieldValues.containsKey(MINUTE_OF_DAY)) {
      long mod = this.fieldValues.remove(MINUTE_OF_DAY);
      if (this.resolverStyle != TResolverStyle.LENIENT) {
        MINUTE_OF_DAY.checkValidValue(mod);
      }
      updateCheckConflict(MINUTE_OF_DAY, HOUR_OF_DAY, mod / 60);
      updateCheckConflict(MINUTE_OF_DAY, MINUTE_OF_HOUR, mod % 60);
    }

    // combine partial second fields strictly, leaving lenient expansion to later
    if (this.fieldValues.containsKey(NANO_OF_SECOND)) {
      long nos = this.fieldValues.get(NANO_OF_SECOND);
      if (this.resolverStyle != TResolverStyle.LENIENT) {
        NANO_OF_SECOND.checkValidValue(nos);
      }
      if (this.fieldValues.containsKey(MICRO_OF_SECOND)) {
        long cos = this.fieldValues.remove(MICRO_OF_SECOND);
        if (this.resolverStyle != TResolverStyle.LENIENT) {
          MICRO_OF_SECOND.checkValidValue(cos);
        }
        nos = cos * 1000 + (nos % 1000);
        updateCheckConflict(MICRO_OF_SECOND, NANO_OF_SECOND, nos);
      }
      if (this.fieldValues.containsKey(MILLI_OF_SECOND)) {
        long los = this.fieldValues.remove(MILLI_OF_SECOND);
        if (this.resolverStyle != TResolverStyle.LENIENT) {
          MILLI_OF_SECOND.checkValidValue(los);
        }
        updateCheckConflict(MILLI_OF_SECOND, NANO_OF_SECOND, los * 1_000_000L + (nos % 1_000_000L));
      }
    }

    // convert to time if all four fields available (optimization)
    if (this.fieldValues.containsKey(HOUR_OF_DAY) && this.fieldValues.containsKey(MINUTE_OF_HOUR)
        && this.fieldValues.containsKey(SECOND_OF_MINUTE) && this.fieldValues.containsKey(NANO_OF_SECOND)) {
      long hod = this.fieldValues.remove(HOUR_OF_DAY);
      long moh = this.fieldValues.remove(MINUTE_OF_HOUR);
      long som = this.fieldValues.remove(SECOND_OF_MINUTE);
      long nos = this.fieldValues.remove(NANO_OF_SECOND);
      resolveTime(hod, moh, som, nos);
    }
  }

  private void resolveTimeLenient() {

    if (this.time == null) {
      if (this.fieldValues.containsKey(MILLI_OF_SECOND)) {
        long los = this.fieldValues.remove(MILLI_OF_SECOND);
        if (this.fieldValues.containsKey(MICRO_OF_SECOND)) {
          long cos = los * 1_000 + (this.fieldValues.get(MICRO_OF_SECOND) % 1_000);
          updateCheckConflict(MILLI_OF_SECOND, MICRO_OF_SECOND, cos);
          this.fieldValues.remove(MICRO_OF_SECOND);
          this.fieldValues.put(NANO_OF_SECOND, cos * 1_000L);
        } else {
          this.fieldValues.put(NANO_OF_SECOND, los * 1_000_000L);
        }
      } else if (this.fieldValues.containsKey(MICRO_OF_SECOND)) {
        long cos = this.fieldValues.remove(MICRO_OF_SECOND);
        this.fieldValues.put(NANO_OF_SECOND, cos * 1_000L);
      }

      Long hod = this.fieldValues.get(HOUR_OF_DAY);
      if (hod != null) {
        Long moh = this.fieldValues.get(MINUTE_OF_HOUR);
        Long som = this.fieldValues.get(SECOND_OF_MINUTE);
        Long nos = this.fieldValues.get(NANO_OF_SECOND);

        if ((moh == null && (som != null || nos != null)) || (moh != null && som == null && nos != null)) {
          return;
        }

        long mohVal = (moh != null ? moh : 0);
        long somVal = (som != null ? som : 0);
        long nosVal = (nos != null ? nos : 0);
        resolveTime(hod, mohVal, somVal, nosVal);
        this.fieldValues.remove(HOUR_OF_DAY);
        this.fieldValues.remove(MINUTE_OF_HOUR);
        this.fieldValues.remove(SECOND_OF_MINUTE);
        this.fieldValues.remove(NANO_OF_SECOND);
      }
    }

    if (this.resolverStyle != TResolverStyle.LENIENT && this.fieldValues.size() > 0) {
      for (Entry<TTemporalField, Long> entry : this.fieldValues.entrySet()) {
        TTemporalField field = entry.getKey();
        if (field instanceof TChronoField && field.isTimeBased()) {
          ((TChronoField) field).checkValidValue(entry.getValue());
        }
      }
    }
  }

  private void resolveTime(long hod, long moh, long som, long nos) {

    if (this.resolverStyle == TResolverStyle.LENIENT) {
      long totalNanos = Math.multiplyExact(hod, 3600_000_000_000L);
      totalNanos = Math.addExact(totalNanos, Math.multiplyExact(moh, 60_000_000_000L));
      totalNanos = Math.addExact(totalNanos, Math.multiplyExact(som, 1_000_000_000L));
      totalNanos = Math.addExact(totalNanos, nos);
      int days = (int) Math.floorDiv(totalNanos, 86400_000_000_000L);
      long nod = Math.floorMod(totalNanos, 86400_000_000_000L);
      updateCheckConflict(TLocalTime.ofNanoOfDay(nod), TPeriod.ofDays(days));
    } else {
      int mohVal = MINUTE_OF_HOUR.checkValidIntValue(moh);
      int nosVal = NANO_OF_SECOND.checkValidIntValue(nos);
      if (this.resolverStyle == TResolverStyle.SMART && hod == 24 && mohVal == 0 && som == 0 && nosVal == 0) {
        updateCheckConflict(TLocalTime.MIDNIGHT, TPeriod.ofDays(1));
      } else {
        int hodVal = HOUR_OF_DAY.checkValidIntValue(hod);
        int somVal = SECOND_OF_MINUTE.checkValidIntValue(som);
        updateCheckConflict(TLocalTime.of(hodVal, mohVal, somVal, nosVal), TPeriod.ZERO);
      }
    }
  }

  private void resolvePeriod() {

    // add whole days if we have both date and time
    if (this.date != null && this.time != null && this.excessDays.isZero() == false) {
      this.date = this.date.plus(this.excessDays);
      this.excessDays = TPeriod.ZERO;
    }
  }

  private void resolveFractional() {

    if (this.time == null && (this.fieldValues.containsKey(INSTANT_SECONDS)
        || this.fieldValues.containsKey(SECOND_OF_DAY) || this.fieldValues.containsKey(SECOND_OF_MINUTE))) {
      if (this.fieldValues.containsKey(NANO_OF_SECOND)) {
        long nos = this.fieldValues.get(NANO_OF_SECOND);
        this.fieldValues.put(MICRO_OF_SECOND, nos / 1000);
        this.fieldValues.put(MILLI_OF_SECOND, nos / 1000000);
      } else {
        this.fieldValues.put(NANO_OF_SECOND, 0L);
        this.fieldValues.put(MICRO_OF_SECOND, 0L);
        this.fieldValues.put(MILLI_OF_SECOND, 0L);
      }
    }
  }

  private void resolveInstant() {

    if (this.date != null && this.time != null) {
      Long offsetSecs = this.fieldValues.get(OFFSET_SECONDS);
      if (offsetSecs != null) {
        TZoneOffset offset = TZoneOffset.ofTotalSeconds(offsetSecs.intValue());
        long instant = this.date.atTime(this.time).atZone(offset).toEpochSecond();
        this.fieldValues.put(INSTANT_SECONDS, instant);
      } else {
        if (this.zone != null) {
          long instant = this.date.atTime(this.time).atZone(this.zone).toEpochSecond();
          this.fieldValues.put(INSTANT_SECONDS, instant);
        }
      }
    }
  }

  private void updateCheckConflict(TLocalTime timeToSet, TPeriod periodToSet) {

    if (this.time != null) {
      if (this.time.equals(timeToSet) == false) {
        throw new TDateTimeException(
            "Conflict found: Fields resolved to different times: " + this.time + " " + timeToSet);
      }
      if (this.excessDays.isZero() == false && periodToSet.isZero() == false
          && this.excessDays.equals(periodToSet) == false) {
        throw new TDateTimeException(
            "Conflict found: Fields resolved to different excess periods: " + this.excessDays + " " + periodToSet);
      } else {
        this.excessDays = periodToSet;
      }
    } else {
      this.time = timeToSet;
      this.excessDays = periodToSet;
    }
  }

  private void crossCheck() {

    if (this.date != null) {
      crossCheck(this.date);
    }
    if (this.time != null) {
      crossCheck(this.time);
      if (this.date != null && this.fieldValues.size() > 0) {
        crossCheck(this.date.atTime(this.time));
      }
    }
  }

  private void crossCheck(TTemporalAccessor target) {

    for (Iterator<Entry<TTemporalField, Long>> it = this.fieldValues.entrySet().iterator(); it.hasNext();) {
      Entry<TTemporalField, Long> entry = it.next();
      TTemporalField field = entry.getKey();
      if (target.isSupported(field)) {
        long val1;
        try {
          val1 = target.getLong(field);
        } catch (RuntimeException ex) {
          continue;
        }
        long val2 = entry.getValue();
        if (val1 != val2) {
          throw new TDateTimeException("Conflict found: Field " + field + " " + val1 + " differs from " + field + " "
              + val2 + " derived from " + target);
        }
        it.remove();
      }
    }
  }

  @Override
  public String toString() {

    StringBuilder buf = new StringBuilder(64);
    buf.append(this.fieldValues).append(',').append(this.chrono);
    if (this.zone != null) {
      buf.append(',').append(this.zone);
    }
    if (this.date != null || this.time != null) {
      buf.append(" resolved to ");
      if (this.date != null) {
        buf.append(this.date);
        if (this.time != null) {
          buf.append('T').append(this.time);
        }
      } else {
        buf.append(this.time);
      }
    }
    return buf.toString();
  }

}
