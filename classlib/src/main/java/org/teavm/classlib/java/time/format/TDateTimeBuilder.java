/*
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.teavm.classlib.java.time.format;

import static org.teavm.classlib.java.time.temporal.TChronoField.AMPM_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.CLOCK_HOUR_OF_AMPM;
import static org.teavm.classlib.java.time.temporal.TChronoField.CLOCK_HOUR_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;
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
import org.teavm.classlib.java.time.chrono.TIsoChronology;
import org.teavm.classlib.java.time.jdk8.TDefaultInterfaceTemporalAccessor;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;

final class TDateTimeBuilder
        extends TDefaultInterfaceTemporalAccessor
        implements TTemporalAccessor, Cloneable {

    final Map<TTemporalField, Long> fieldValues = new HashMap<TTemporalField, Long>();
    TChronology chrono;
    TZoneId zone;
    TChronoLocalDate date;
    TLocalTime time;
    boolean leapSecond;
    TPeriod excessDays;

    //-----------------------------------------------------------------------
    public TDateTimeBuilder() {
    }

    public TDateTimeBuilder(TTemporalField field, long value) {
        addFieldValue(field, value);
    }

    //-----------------------------------------------------------------------
    private Long getFieldValue0(TTemporalField field) {
        return fieldValues.get(field);
    }

    TDateTimeBuilder addFieldValue(TTemporalField field, long value) {
        TJdk8Methods.requireNonNull(field, "field");
        Long old = getFieldValue0(field);  // check first for better error message
        if (old != null && old.longValue() != value) {
            throw new TDateTimeException("Conflict found: " + field + " " + old + " differs from " + field + " " + value + ": " + this);
        }
        return putFieldValue0(field, value);
    }

    private TDateTimeBuilder putFieldValue0(TTemporalField field, long value) {
        fieldValues.put(field, value);
        return this;
    }

    //-----------------------------------------------------------------------
    void addObject(TChronoLocalDate date) {
        this.date = date;
    }

    void addObject(TLocalTime time) {
        this.time = time;
    }

    //-----------------------------------------------------------------------
    public TDateTimeBuilder resolve(TResolverStyle resolverStyle, Set<TTemporalField> resolverFields) {
        if (resolverFields != null) {
            fieldValues.keySet().retainAll(resolverFields);
        }
        // handle standard fields
        mergeInstantFields();
        mergeDate(resolverStyle);
        mergeTime(resolverStyle);
        if (resolveFields(resolverStyle)) {
            mergeInstantFields();
            mergeDate(resolverStyle);
            mergeTime(resolverStyle);
        }
        resolveTimeInferZeroes(resolverStyle);
        crossCheck();
        if (excessDays != null && excessDays.isZero() == false && date != null && time != null) {
            date = date.plus(excessDays);
            excessDays = TPeriod.ZERO;
        }
        resolveFractional();
        resolveInstant();
        return this;
    }

    private boolean resolveFields(TResolverStyle resolverStyle) {
        int changes = 0;
        outer:
        while (changes < 100) {
            for (Map.Entry<TTemporalField, Long> entry : fieldValues.entrySet()) {
                TTemporalField targetField = entry.getKey();
                TTemporalAccessor resolvedObject = targetField.resolve(fieldValues, this, resolverStyle);
                if (resolvedObject != null) {
                    if (resolvedObject instanceof TChronoZonedDateTime) {
                        TChronoZonedDateTime<?> czdt = (TChronoZonedDateTime<?>) resolvedObject;
                        if (zone == null) {
                            zone = czdt.getZone();
                        } else if (zone.equals(czdt.getZone()) == false) {
                            throw new TDateTimeException("TChronoZonedDateTime must use the effective parsed zone: " + zone);
                        }
                        resolvedObject = czdt.toLocalDateTime();
                    }
                    if (resolvedObject instanceof TChronoLocalDate) {
                        resolveMakeChanges(targetField, (TChronoLocalDate) resolvedObject);
                        changes++;
                        continue outer;  // have to restart to avoid concurrent modification
                    }
                    if (resolvedObject instanceof TLocalTime) {
                        resolveMakeChanges(targetField, (TLocalTime) resolvedObject);
                        changes++;
                        continue outer;  // have to restart to avoid concurrent modification
                    }
                    if (resolvedObject instanceof TChronoLocalDateTime<?>) {
                        TChronoLocalDateTime<?> cldt = (TChronoLocalDateTime<?>) resolvedObject;
                        resolveMakeChanges(targetField, cldt.toLocalDate());
                        resolveMakeChanges(targetField, cldt.toLocalTime());
                        changes++;
                        continue outer;  // have to restart to avoid concurrent modification
                    }
                    throw new TDateTimeException("Unknown type: " + resolvedObject.getClass().getName());
                } else if (fieldValues.containsKey(targetField) == false) {
                    changes++;
                    continue outer;  // have to restart to avoid concurrent modification
                }
            }
            break;
        }
        if (changes == 100) {
            throw new TDateTimeException("Badly written field");
        }
        return changes > 0;
    }

    private void resolveMakeChanges(TTemporalField targetField, TChronoLocalDate date) {
        if (chrono.equals(date.getChronology()) == false) {
            throw new TDateTimeException("TChronoLocalDate must use the effective parsed chronology: " + chrono);
        }
        long epochDay = date.toEpochDay();
        Long old = fieldValues.put(TChronoField.EPOCH_DAY, epochDay);
        if (old != null && old.longValue() != epochDay) {
            throw new TDateTimeException("Conflict found: " + TLocalDate.ofEpochDay(old) +
                    " differs from " + TLocalDate.ofEpochDay(epochDay) +
                    " while resolving  " + targetField);
        }
    }

    private void resolveMakeChanges(TTemporalField targetField, TLocalTime time) {
        long nanOfDay = time.toNanoOfDay();
        Long old = fieldValues.put(TChronoField.NANO_OF_DAY, nanOfDay);
        if (old != null && old.longValue() != nanOfDay) {
            throw new TDateTimeException("Conflict found: " + TLocalTime.ofNanoOfDay(old) +
                    " differs from " + time +
                    " while resolving  " + targetField);
        }
    }

    private void mergeDate(TResolverStyle resolverStyle) {
        if (chrono instanceof TIsoChronology) {
            checkDate(TIsoChronology.INSTANCE.resolveDate(fieldValues, resolverStyle));
        } else {
            if (fieldValues.containsKey(EPOCH_DAY)) {
                checkDate(TLocalDate.ofEpochDay(fieldValues.remove(EPOCH_DAY)));
                return;
            }
        }
    }

    private void checkDate(TLocalDate date) {
        if (date != null) {
            addObject(date);
            for (TTemporalField field : fieldValues.keySet()) {
                if (field instanceof TChronoField) {
                    if (field.isDateBased()) {
                        long val1;
                        try {
                            val1 = date.getLong(field);
                        } catch (TDateTimeException ex) {
                            continue;
                        }
                        Long val2 = fieldValues.get(field);
                        if (val1 != val2) {
                            throw new TDateTimeException("Conflict found: Field " + field + " " + val1 + " differs from " + field + " " + val2 + " derived from " + date);
                        }
                    }
                }
            }
        }
    }

    private void mergeTime(TResolverStyle resolverStyle) {
        if (fieldValues.containsKey(CLOCK_HOUR_OF_DAY)) {
            long ch = fieldValues.remove(CLOCK_HOUR_OF_DAY);
            if (resolverStyle != TResolverStyle.LENIENT) {
                if (resolverStyle == TResolverStyle.SMART && ch == 0) {
                    // ok
                } else {
                    CLOCK_HOUR_OF_DAY.checkValidValue(ch);
                }
            }
            addFieldValue(HOUR_OF_DAY, ch == 24 ? 0 : ch);
        }
        if (fieldValues.containsKey(CLOCK_HOUR_OF_AMPM)) {
            long ch = fieldValues.remove(CLOCK_HOUR_OF_AMPM);
            if (resolverStyle != TResolverStyle.LENIENT) {
                if (resolverStyle == TResolverStyle.SMART && ch == 0) {
                    // ok
                } else {
                    CLOCK_HOUR_OF_AMPM.checkValidValue(ch);
                }
            }
            addFieldValue(HOUR_OF_AMPM, ch == 12 ? 0 : ch);
        }
        if (resolverStyle != TResolverStyle.LENIENT) {
            if (fieldValues.containsKey(AMPM_OF_DAY)) {
                AMPM_OF_DAY.checkValidValue(fieldValues.get(AMPM_OF_DAY));
            }
            if (fieldValues.containsKey(HOUR_OF_AMPM)) {
                HOUR_OF_AMPM.checkValidValue(fieldValues.get(HOUR_OF_AMPM));
            }
        }
        if (fieldValues.containsKey(AMPM_OF_DAY) && fieldValues.containsKey(HOUR_OF_AMPM)) {
            long ap = fieldValues.remove(AMPM_OF_DAY);
            long hap = fieldValues.remove(HOUR_OF_AMPM);
            addFieldValue(HOUR_OF_DAY, ap * 12 + hap);
        }
//        if (timeFields.containsKey(HOUR_OF_DAY) && timeFields.containsKey(MINUTE_OF_HOUR)) {
//            long hod = timeFields.remove(HOUR_OF_DAY);
//            long moh = timeFields.remove(MINUTE_OF_HOUR);
//            addFieldValue(MINUTE_OF_DAY, hod * 60 + moh);
//        }
//        if (timeFields.containsKey(MINUTE_OF_DAY) && timeFields.containsKey(SECOND_OF_MINUTE)) {
//            long mod = timeFields.remove(MINUTE_OF_DAY);
//            long som = timeFields.remove(SECOND_OF_MINUTE);
//            addFieldValue(SECOND_OF_DAY, mod * 60 + som);
//        }
        if (fieldValues.containsKey(NANO_OF_DAY)) {
            long nod = fieldValues.remove(NANO_OF_DAY);
            if (resolverStyle != TResolverStyle.LENIENT) {
                NANO_OF_DAY.checkValidValue(nod);
            }
            addFieldValue(SECOND_OF_DAY, nod / 1000000000L);
            addFieldValue(NANO_OF_SECOND, nod % 1000000000L);
        }
        if (fieldValues.containsKey(MICRO_OF_DAY)) {
            long cod = fieldValues.remove(MICRO_OF_DAY);
            if (resolverStyle != TResolverStyle.LENIENT) {
                MICRO_OF_DAY.checkValidValue(cod);
            }
            addFieldValue(SECOND_OF_DAY, cod / 1000000L);
            addFieldValue(MICRO_OF_SECOND, cod % 1000000L);
        }
        if (fieldValues.containsKey(MILLI_OF_DAY)) {
            long lod = fieldValues.remove(MILLI_OF_DAY);
            if (resolverStyle != TResolverStyle.LENIENT) {
                MILLI_OF_DAY.checkValidValue(lod);
            }
            addFieldValue(SECOND_OF_DAY, lod / 1000);
            addFieldValue(MILLI_OF_SECOND, lod % 1000);
        }
        if (fieldValues.containsKey(SECOND_OF_DAY)) {
            long sod = fieldValues.remove(SECOND_OF_DAY);
            if (resolverStyle != TResolverStyle.LENIENT) {
                SECOND_OF_DAY.checkValidValue(sod);
            }
            addFieldValue(HOUR_OF_DAY, sod / 3600);
            addFieldValue(MINUTE_OF_HOUR, (sod / 60) % 60);
            addFieldValue(SECOND_OF_MINUTE, sod % 60);
        }
        if (fieldValues.containsKey(MINUTE_OF_DAY)) {
            long mod = fieldValues.remove(MINUTE_OF_DAY);
            if (resolverStyle != TResolverStyle.LENIENT) {
                MINUTE_OF_DAY.checkValidValue(mod);
            }
            addFieldValue(HOUR_OF_DAY, mod / 60);
            addFieldValue(MINUTE_OF_HOUR, mod % 60);
        }

//            long sod = nod / 1000000000L;
//            addFieldValue(HOUR_OF_DAY, sod / 3600);
//            addFieldValue(MINUTE_OF_HOUR, (sod / 60) % 60);
//            addFieldValue(SECOND_OF_MINUTE, sod % 60);
//            addFieldValue(NANO_OF_SECOND, nod % 1000000000L);
        if (resolverStyle != TResolverStyle.LENIENT) {
            if (fieldValues.containsKey(MILLI_OF_SECOND)) {
                MILLI_OF_SECOND.checkValidValue(fieldValues.get(MILLI_OF_SECOND));
            }
            if (fieldValues.containsKey(MICRO_OF_SECOND)) {
                MICRO_OF_SECOND.checkValidValue(fieldValues.get(MICRO_OF_SECOND));
            }
        }
        if (fieldValues.containsKey(MILLI_OF_SECOND) && fieldValues.containsKey(MICRO_OF_SECOND)) {
            long los = fieldValues.remove(MILLI_OF_SECOND);
            long cos = fieldValues.get(MICRO_OF_SECOND);
            addFieldValue(MICRO_OF_SECOND, los * 1000 + (cos % 1000));
        }
        if (fieldValues.containsKey(MICRO_OF_SECOND) && fieldValues.containsKey(NANO_OF_SECOND)) {
            long nos = fieldValues.get(NANO_OF_SECOND);
            addFieldValue(MICRO_OF_SECOND, nos / 1000);
            fieldValues.remove(MICRO_OF_SECOND);
        }
        if (fieldValues.containsKey(MILLI_OF_SECOND) && fieldValues.containsKey(NANO_OF_SECOND)) {
            long nos = fieldValues.get(NANO_OF_SECOND);
            addFieldValue(MILLI_OF_SECOND, nos / 1000000);
            fieldValues.remove(MILLI_OF_SECOND);
        }
        if (fieldValues.containsKey(MICRO_OF_SECOND)) {
            long cos = fieldValues.remove(MICRO_OF_SECOND);
            addFieldValue(NANO_OF_SECOND, cos * 1000);
        } else if (fieldValues.containsKey(MILLI_OF_SECOND)) {
            long los = fieldValues.remove(MILLI_OF_SECOND);
            addFieldValue(NANO_OF_SECOND, los * 1000000);
        }
    }

    private void resolveTimeInferZeroes(TResolverStyle resolverStyle) {
        Long hod = fieldValues.get(HOUR_OF_DAY);
        Long moh = fieldValues.get(MINUTE_OF_HOUR);
        Long som = fieldValues.get(SECOND_OF_MINUTE);
        Long nos = fieldValues.get(NANO_OF_SECOND);
        if (hod == null) {
            return;
        }
        if (moh == null && (som != null || nos != null)) {
            return;
        }
        if (moh != null && som == null && nos != null) {
            return;
        }
        if (resolverStyle != TResolverStyle.LENIENT) {
            if (hod != null) {
                if (resolverStyle == TResolverStyle.SMART &&
                                hod.longValue() == 24 && 
                                (moh == null || moh.longValue() == 0) && 
                                (som == null || som.longValue() == 0) && 
                                (nos == null || nos.longValue() == 0)) {
                    hod = 0L;
                    excessDays = TPeriod.ofDays(1);
                }
                int hodVal = HOUR_OF_DAY.checkValidIntValue(hod);
                if (moh != null) {
                    int mohVal = MINUTE_OF_HOUR.checkValidIntValue(moh);
                    if (som != null) {
                        int somVal = SECOND_OF_MINUTE.checkValidIntValue(som);
                        if (nos != null) {
                            int nosVal = NANO_OF_SECOND.checkValidIntValue(nos);
                            addObject(TLocalTime.of(hodVal, mohVal, somVal, nosVal));
                        } else {
                            addObject(TLocalTime.of(hodVal, mohVal, somVal));
                        }
                    } else {
                        if (nos == null) {
                            addObject(TLocalTime.of(hodVal, mohVal));
                        }
                    }
                } else {
                    if (som == null && nos == null) {
                        addObject(TLocalTime.of(hodVal, 0));
                    }
                }
            }
        } else {
            if (hod != null) {
                long hodVal = hod;
                if (moh != null) {
                    if (som != null) {
                        if (nos == null) {
                            nos = 0L;
                        }
                        long totalNanos = TJdk8Methods.safeMultiply(hodVal, 3600000000000L);
                        totalNanos = TJdk8Methods.safeAdd(totalNanos, TJdk8Methods.safeMultiply(moh, 60000000000L));
                        totalNanos = TJdk8Methods.safeAdd(totalNanos, TJdk8Methods.safeMultiply(som, 1000000000L));
                        totalNanos = TJdk8Methods.safeAdd(totalNanos, nos);
                        int excessDays = (int) TJdk8Methods.floorDiv(totalNanos, 86400000000000L);  // safe int cast
                        long nod = TJdk8Methods.floorMod(totalNanos, 86400000000000L);
                        addObject(TLocalTime.ofNanoOfDay(nod));
                        this.excessDays = TPeriod.ofDays(excessDays);
                    } else {
                        long totalSecs = TJdk8Methods.safeMultiply(hodVal, 3600L);
                        totalSecs = TJdk8Methods.safeAdd(totalSecs, TJdk8Methods.safeMultiply(moh, 60L));
                        int excessDays = (int) TJdk8Methods.floorDiv(totalSecs, 86400L);  // safe int cast
                        long sod = TJdk8Methods.floorMod(totalSecs, 86400L);
                        addObject(TLocalTime.ofSecondOfDay(sod));
                        this.excessDays = TPeriod.ofDays(excessDays);
                    }
                } else {
                    int excessDays = TJdk8Methods.safeToInt(TJdk8Methods.floorDiv(hodVal, 24L));
                    hodVal = TJdk8Methods.floorMod(hodVal, 24);
                    addObject(TLocalTime.of((int) hodVal, 0));
                    this.excessDays = TPeriod.ofDays(excessDays);
                }
            }
        }
        fieldValues.remove(HOUR_OF_DAY);
        fieldValues.remove(MINUTE_OF_HOUR);
        fieldValues.remove(SECOND_OF_MINUTE);
        fieldValues.remove(NANO_OF_SECOND);
    }

    //-----------------------------------------------------------------------
    private void mergeInstantFields() {
        if (fieldValues.containsKey(INSTANT_SECONDS)) {
            if (zone != null) {
                mergeInstantFields0(zone);
            } else {
                Long offsetSecs = fieldValues.get(OFFSET_SECONDS);
                if (offsetSecs != null) {
                    TZoneOffset offset = TZoneOffset.ofTotalSeconds(offsetSecs.intValue());
                    mergeInstantFields0(offset);
                }
            }
        }
    }

    private void mergeInstantFields0(TZoneId selectedZone) {
        TInstant instant = TInstant.ofEpochSecond(fieldValues.remove(INSTANT_SECONDS));
        TChronoZonedDateTime<?> zdt = chrono.zonedDateTime(instant, selectedZone);
        if (date == null) {
            addObject(zdt.toLocalDate());
        } else {
            resolveMakeChanges(INSTANT_SECONDS, zdt.toLocalDate());
        }
        addFieldValue(SECOND_OF_DAY, (long) zdt.toLocalTime().toSecondOfDay());
    }

    //-----------------------------------------------------------------------
    private void crossCheck() {
        if (fieldValues.size() > 0) {
            if (date != null && time != null) {
                crossCheck(date.atTime(time));
            } else if (date != null) {
                crossCheck(date);
            } else if (time != null) {
                crossCheck(time);
            }
        }
    }

    private void crossCheck(TTemporalAccessor temporal) {
        Iterator<Entry<TTemporalField, Long>> it = fieldValues.entrySet().iterator();
        while (it.hasNext()) {
            Entry<TTemporalField, Long> entry = it.next();
            TTemporalField field = entry.getKey();
            long value = entry.getValue();
            if (temporal.isSupported(field)) {
                long temporalValue;
                try {
                    temporalValue = temporal.getLong(field);
                } catch (RuntimeException ex) {
                    continue;
                }
                if (temporalValue != value) {
                    throw new TDateTimeException("Cross check failed: " +
                            field + " " + temporalValue + " vs " + field + " " + value);
                }
                it.remove();
            }
        }
    }

    private void resolveFractional() {
        if (time == null &&
                (fieldValues.containsKey(INSTANT_SECONDS) ||
                    fieldValues.containsKey(SECOND_OF_DAY) ||
                    fieldValues.containsKey(SECOND_OF_MINUTE))) {
            if (fieldValues.containsKey(NANO_OF_SECOND)) {
                long nos = fieldValues.get(NANO_OF_SECOND);
                fieldValues.put(MICRO_OF_SECOND, nos / 1000);
                fieldValues.put(MILLI_OF_SECOND, nos / 1000000);
            } else {
                fieldValues.put(NANO_OF_SECOND, 0L);
                fieldValues.put(MICRO_OF_SECOND, 0L);
                fieldValues.put(MILLI_OF_SECOND, 0L);
            }
        }
    }

    private void resolveInstant() {
        if (date != null && time != null) {
            Long offsetSecs = fieldValues.get(OFFSET_SECONDS);
            if (offsetSecs != null) {
                TZoneOffset offset = TZoneOffset.ofTotalSeconds(offsetSecs.intValue());
                long instant = date.atTime(time).atZone(offset).getLong(TChronoField.INSTANT_SECONDS);
                fieldValues.put(INSTANT_SECONDS, instant);
            }  else if (zone != null) {
                long instant = date.atTime(time).atZone(zone).getLong(TChronoField.INSTANT_SECONDS);
                fieldValues.put(INSTANT_SECONDS, instant);
            }
        }
    }

    //-----------------------------------------------------------------------
    public <R> R build(TTemporalQuery<R> type) {
        return type.queryFrom(this);
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean isSupported(TTemporalField field) {
        if (field == null) {
            return false;
        }
        return fieldValues.containsKey(field) ||
                (date != null && date.isSupported(field)) ||
                (time != null && time.isSupported(field));
    }

    @Override
    public long getLong(TTemporalField field) {
        TJdk8Methods.requireNonNull(field, "field");
        Long value = getFieldValue0(field);
        if (value == null) {
            if (date != null && date.isSupported(field)) {
                return date.getLong(field);
            }
            if (time != null && time.isSupported(field)) {
                return time.getLong(field);
            }
            throw new TDateTimeException("Field not found: " + field);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TTemporalQuery<R> query) {
        if (query == TTemporalQueries.zoneId()) {
            return (R) zone;
        } else if (query == TTemporalQueries.chronology()) {
            return (R) chrono;
        } else if (query == TTemporalQueries.localDate()) {
            return date != null ? (R) TLocalDate.from(date) : null;
        } else if (query == TTemporalQueries.localTime()) {
            return (R) time;
        } else if (query == TTemporalQueries.zone() || query == TTemporalQueries.offset()) {
            return query.queryFrom(this);
        } else if (query == TTemporalQueries.precision()) {
            return null;  // not a complete date/time
        }
        // inline TTemporalAccessor.super.query(query) as an optimization
        // non-JDK classes are not permitted to make this optimization
        return query.queryFrom(this);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(128);
        buf.append("TDateTimeBuilder[");
        if (fieldValues.size() > 0) {
            buf.append("fields=").append(fieldValues);
        }
        buf.append(", ").append(chrono);
        buf.append(", ").append(zone);
        buf.append(", ").append(date);
        buf.append(", ").append(time);
        buf.append(']');
        return buf.toString();
    }

}
