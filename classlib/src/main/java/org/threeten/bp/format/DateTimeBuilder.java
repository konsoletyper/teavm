/*
 *  Copyright 2020 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
package org.threeten.bp.format;

import static org.threeten.bp.temporal.ChronoField.AMPM_OF_DAY;
import static org.threeten.bp.temporal.ChronoField.CLOCK_HOUR_OF_AMPM;
import static org.threeten.bp.temporal.ChronoField.CLOCK_HOUR_OF_DAY;
import static org.threeten.bp.temporal.ChronoField.EPOCH_DAY;
import static org.threeten.bp.temporal.ChronoField.HOUR_OF_AMPM;
import static org.threeten.bp.temporal.ChronoField.HOUR_OF_DAY;
import static org.threeten.bp.temporal.ChronoField.INSTANT_SECONDS;
import static org.threeten.bp.temporal.ChronoField.MICRO_OF_DAY;
import static org.threeten.bp.temporal.ChronoField.MICRO_OF_SECOND;
import static org.threeten.bp.temporal.ChronoField.MILLI_OF_DAY;
import static org.threeten.bp.temporal.ChronoField.MILLI_OF_SECOND;
import static org.threeten.bp.temporal.ChronoField.MINUTE_OF_DAY;
import static org.threeten.bp.temporal.ChronoField.MINUTE_OF_HOUR;
import static org.threeten.bp.temporal.ChronoField.NANO_OF_DAY;
import static org.threeten.bp.temporal.ChronoField.NANO_OF_SECOND;
import static org.threeten.bp.temporal.ChronoField.OFFSET_SECONDS;
import static org.threeten.bp.temporal.ChronoField.SECOND_OF_DAY;
import static org.threeten.bp.temporal.ChronoField.SECOND_OF_MINUTE;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.threeten.bp.DateTimeException;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.Period;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.chrono.ChronoLocalDate;
import org.threeten.bp.chrono.ChronoLocalDateTime;
import org.threeten.bp.chrono.ChronoZonedDateTime;
import org.threeten.bp.chrono.Chronology;
import org.threeten.bp.chrono.IsoChronology;
import org.threeten.bp.jdk8.Jdk8Methods;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.TemporalAccessor;
import org.threeten.bp.temporal.TemporalField;
import org.threeten.bp.temporal.TemporalQueries;
import org.threeten.bp.temporal.TemporalQuery;

/**
 * Builder that can holds date and time fields and related date and time objects.
 * <p>
 * The builder is used to hold onto different elements of date and time.
 * It is designed as two separate maps:
 * <p><ul>
 * <li>from {@link TemporalField} to {@code long} value, where the value may be
 * outside the valid range for the field
 * <li>from {@code Class} to {@link TemporalAccessor}, holding larger scale objects
 * like {@code LocalDateTime}.
 * </ul><p>
 *
 * <h3>Specification for implementors</h3>
 * This class is mutable and not thread-safe.
 * It should only be used from a single thread.
 */
public final class DateTimeBuilder
        implements TemporalAccessor, Cloneable {

    /**
     * The map of other fields.
     */
    final Map<TemporalField, Long> fieldValues = new HashMap<>();
    /**
     * The chronology.
     */
    Chronology chrono;
    /**
     * The zone.
     */
    ZoneId zone;
    /**
     * The date.
     */
    ChronoLocalDate date;
    /**
     * The time.
     */
    LocalTime time;
    /**
     * The leap second flag.
     */
    boolean leapSecond;
    /**
     * The excess days.
     */
    Period excessDays;

    //-----------------------------------------------------------------------
    /**
     * Creates an empty instance of the builder.
     */
    public DateTimeBuilder() {
    }

    /**
     * Creates a new instance of the builder with a single field-value.
     * <p>
     * This is equivalent to using {@link #addFieldValue(TemporalField, long)} on an empty builder.
     *
     * @param field  the field to add, not null
     * @param value  the value to add, not null
     */
    public DateTimeBuilder(TemporalField field, long value) {
        addFieldValue(field, value);
    }

    //-----------------------------------------------------------------------
    private Long getFieldValue0(TemporalField field) {
        return fieldValues.get(field);
    }

    /**
     * Adds a field-value pair to the builder.
     * <p>
     * This adds a field to the builder.
     * If the field is not already present, then the field-value pair is added to the map.
     * If the field is already present and it has the same value as that specified, no action occurs.
     * If the field is already present and it has a different value to that specified, then
     * an exception is thrown.
     *
     * @param field  the field to add, not null
     * @param value  the value to add, not null
     * @return {@code this}, for method chaining
     * @throws DateTimeException if the field is already present with a different value
     */
    DateTimeBuilder addFieldValue(TemporalField field, long value) {
        Objects.requireNonNull(field, "field");
        Long old = getFieldValue0(field);  // check first for better error message
        if (old != null && old != value) {
            throw new DateTimeException("Conflict found: " + field + " " + old + " differs from " + field + " "
                    + value + ": " + this);
        }
        return putFieldValue0(field, value);
    }

    private DateTimeBuilder putFieldValue0(TemporalField field, long value) {
        fieldValues.put(field, value);
        return this;
    }

    //-----------------------------------------------------------------------
    void addObject(ChronoLocalDate date) {
        this.date = date;
    }

    void addObject(LocalTime time) {
        this.time = time;
    }

    //-----------------------------------------------------------------------
    /**
     * Resolves the builder, evaluating the date and time.
     * <p>
     * This examines the contents of the builder and resolves it to produce the best
     * available date and time, throwing an exception if a problem occurs.
     * Calling this method changes the state of the builder.
     *
     * @param resolverStyle how to resolve
     * @return {@code this}, for method chaining
     */
    public DateTimeBuilder resolve(ResolverStyle resolverStyle, Set<TemporalField> resolverFields) {
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
        if (excessDays != null && !excessDays.isZero() && date != null && time != null) {
            date = date.plus(excessDays);
            excessDays = Period.ZERO;
        }
        resolveFractional();
        resolveInstant();
        return this;
    }

    private boolean resolveFields(ResolverStyle resolverStyle) {
        int changes = 0;
        outer:
        while (changes < 100) {
            for (Map.Entry<TemporalField, Long> entry : fieldValues.entrySet()) {
                TemporalField targetField = entry.getKey();
                TemporalAccessor resolvedObject = targetField.resolve(fieldValues, this, resolverStyle);
                if (resolvedObject != null) {
                    if (resolvedObject instanceof ChronoZonedDateTime) {
                        ChronoZonedDateTime<?> czdt = (ChronoZonedDateTime<?>) resolvedObject;
                        if (zone == null) {
                            zone = czdt.getZone();
                        } else if (!zone.equals(czdt.getZone())) {
                            throw new DateTimeException("ChronoZonedDateTime must use the effective parsed zone: "
                                    + zone);
                        }
                        resolvedObject = czdt.toLocalDateTime();
                    }
                    if (resolvedObject instanceof ChronoLocalDate) {
                        resolveMakeChanges(targetField, (ChronoLocalDate) resolvedObject);
                        changes++;
                        continue outer;  // have to restart to avoid concurrent modification
                    }
                    if (resolvedObject instanceof LocalTime) {
                        resolveMakeChanges(targetField, (LocalTime) resolvedObject);
                        changes++;
                        continue outer;  // have to restart to avoid concurrent modification
                    }
                    if (resolvedObject instanceof ChronoLocalDateTime<?>) {
                        ChronoLocalDateTime<?> cldt = (ChronoLocalDateTime<?>) resolvedObject;
                        resolveMakeChanges(targetField, cldt.toLocalDate());
                        resolveMakeChanges(targetField, cldt.toLocalTime());
                        changes++;
                        continue outer;  // have to restart to avoid concurrent modification
                    }
                    throw new DateTimeException("Unknown type: " + resolvedObject.getClass().getName());
                } else if (!fieldValues.containsKey(targetField)) {
                    changes++;
                    continue outer;  // have to restart to avoid concurrent modification
                }
            }
            break;
        }
        if (changes == 100) {
            throw new DateTimeException("Badly written field");
        }
        return changes > 0;
    }

    private void resolveMakeChanges(TemporalField targetField, ChronoLocalDate date) {
        if (!chrono.equals(date.getChronology())) {
            throw new DateTimeException("ChronoLocalDate must use the effective parsed chronology: " + chrono);
        }
        long epochDay = date.toEpochDay();
        Long old = fieldValues.put(ChronoField.EPOCH_DAY, epochDay);
        if (old != null && old != epochDay) {
            throw new DateTimeException("Conflict found: " + LocalDate.ofEpochDay(old)
                    + " differs from " + LocalDate.ofEpochDay(epochDay)
                    + " while resolving  " + targetField);
        }
    }

    private void resolveMakeChanges(TemporalField targetField, LocalTime time) {
        long nanOfDay = time.toNanoOfDay();
        Long old = fieldValues.put(ChronoField.NANO_OF_DAY, nanOfDay);
        if (old != null && old != nanOfDay) {
            throw new DateTimeException("Conflict found: " + LocalTime.ofNanoOfDay(old)
                    + " differs from " + time
                    + " while resolving  " + targetField);
        }
    }

    private void mergeDate(ResolverStyle resolverStyle) {
        if (chrono instanceof IsoChronology) {
            checkDate(IsoChronology.INSTANCE.resolveDate(fieldValues, resolverStyle));
        } else {
            if (fieldValues.containsKey(EPOCH_DAY)) {
                checkDate(LocalDate.ofEpochDay(fieldValues.remove(EPOCH_DAY)));
            }
        }
    }

    private void checkDate(LocalDate date) {
        if (date != null) {
            addObject(date);
            for (TemporalField field : fieldValues.keySet()) {
                if (field instanceof ChronoField) {
                    if (field.isDateBased()) {
                        long val1;
                        try {
                            val1 = date.getLong(field);
                        } catch (DateTimeException ex) {
                            continue;
                        }
                        Long val2 = fieldValues.get(field);
                        if (val1 != val2) {
                            throw new DateTimeException("Conflict found: Field " + field + " " + val1
                                    + " differs from " + field + " " + val2 + " derived from " + date);
                        }
                    }
                }
            }
        }
    }

    private void mergeTime(ResolverStyle resolverStyle) {
        if (fieldValues.containsKey(CLOCK_HOUR_OF_DAY)) {
            long ch = fieldValues.remove(CLOCK_HOUR_OF_DAY);
            if (resolverStyle != ResolverStyle.LENIENT) {
                if (resolverStyle == ResolverStyle.SMART && ch == 0) {
                    // ok
                } else {
                    CLOCK_HOUR_OF_DAY.checkValidValue(ch);
                }
            }
            addFieldValue(HOUR_OF_DAY, ch == 24 ? 0 : ch);
        }
        if (fieldValues.containsKey(CLOCK_HOUR_OF_AMPM)) {
            long ch = fieldValues.remove(CLOCK_HOUR_OF_AMPM);
            if (resolverStyle != ResolverStyle.LENIENT) {
                if (resolverStyle == ResolverStyle.SMART && ch == 0) {
                    // ok
                } else {
                    CLOCK_HOUR_OF_AMPM.checkValidValue(ch);
                }
            }
            addFieldValue(HOUR_OF_AMPM, ch == 12 ? 0 : ch);
        }
        if (resolverStyle != ResolverStyle.LENIENT) {
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
            if (resolverStyle != ResolverStyle.LENIENT) {
                NANO_OF_DAY.checkValidValue(nod);
            }
            addFieldValue(SECOND_OF_DAY, nod / 1000000000L);
            addFieldValue(NANO_OF_SECOND, nod % 1000000000L);
        }
        if (fieldValues.containsKey(MICRO_OF_DAY)) {
            long cod = fieldValues.remove(MICRO_OF_DAY);
            if (resolverStyle != ResolverStyle.LENIENT) {
                MICRO_OF_DAY.checkValidValue(cod);
            }
            addFieldValue(SECOND_OF_DAY, cod / 1000000L);
            addFieldValue(MICRO_OF_SECOND, cod % 1000000L);
        }
        if (fieldValues.containsKey(MILLI_OF_DAY)) {
            long lod = fieldValues.remove(MILLI_OF_DAY);
            if (resolverStyle != ResolverStyle.LENIENT) {
                MILLI_OF_DAY.checkValidValue(lod);
            }
            addFieldValue(SECOND_OF_DAY, lod / 1000);
            addFieldValue(MILLI_OF_SECOND, lod % 1000);
        }
        if (fieldValues.containsKey(SECOND_OF_DAY)) {
            long sod = fieldValues.remove(SECOND_OF_DAY);
            if (resolverStyle != ResolverStyle.LENIENT) {
                SECOND_OF_DAY.checkValidValue(sod);
            }
            addFieldValue(HOUR_OF_DAY, sod / 3600);
            addFieldValue(MINUTE_OF_HOUR, (sod / 60) % 60);
            addFieldValue(SECOND_OF_MINUTE, sod % 60);
        }
        if (fieldValues.containsKey(MINUTE_OF_DAY)) {
            long mod = fieldValues.remove(MINUTE_OF_DAY);
            if (resolverStyle != ResolverStyle.LENIENT) {
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
        if (resolverStyle != ResolverStyle.LENIENT) {
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

    private void resolveTimeInferZeroes(ResolverStyle resolverStyle) {
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
        if (resolverStyle != ResolverStyle.LENIENT) {
            if (hod != null) {
                if (resolverStyle == ResolverStyle.SMART
                        && hod == 24
                        && (moh == null || moh == 0)
                        && (som == null || som == 0)
                        && (nos == null || nos == 0)) {
                    hod = 0L;
                    excessDays = Period.ofDays(1);
                }
                int hodVal = HOUR_OF_DAY.checkValidIntValue(hod);
                if (moh != null) {
                    int mohVal = MINUTE_OF_HOUR.checkValidIntValue(moh);
                    if (som != null) {
                        int somVal = SECOND_OF_MINUTE.checkValidIntValue(som);
                        if (nos != null) {
                            int nosVal = NANO_OF_SECOND.checkValidIntValue(nos);
                            addObject(LocalTime.of(hodVal, mohVal, somVal, nosVal));
                        } else {
                            addObject(LocalTime.of(hodVal, mohVal, somVal));
                        }
                    } else {
                        if (nos == null) {
                            addObject(LocalTime.of(hodVal, mohVal));
                        }
                    }
                } else {
                    if (som == null && nos == null) {
                        addObject(LocalTime.of(hodVal, 0));
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
                        long totalNanos = Jdk8Methods.safeMultiply(hodVal, 3600000000000L);
                        totalNanos = Jdk8Methods.safeAdd(totalNanos, Jdk8Methods.safeMultiply(moh, 60000000000L));
                        totalNanos = Jdk8Methods.safeAdd(totalNanos, Jdk8Methods.safeMultiply(som, 1000000000L));
                        totalNanos = Jdk8Methods.safeAdd(totalNanos, nos);
                        int excessDays = (int) Jdk8Methods.floorDiv(totalNanos, 86400000000000L);  // safe int cast
                        long nod = Jdk8Methods.floorMod(totalNanos, 86400000000000L);
                        addObject(LocalTime.ofNanoOfDay(nod));
                        this.excessDays = Period.ofDays(excessDays);
                    } else {
                        long totalSecs = Jdk8Methods.safeMultiply(hodVal, 3600L);
                        totalSecs = Jdk8Methods.safeAdd(totalSecs, Jdk8Methods.safeMultiply(moh, 60L));
                        int excessDays = (int) Jdk8Methods.floorDiv(totalSecs, 86400L);  // safe int cast
                        long sod = Jdk8Methods.floorMod(totalSecs, 86400L);
                        addObject(LocalTime.ofSecondOfDay(sod));
                        this.excessDays = Period.ofDays(excessDays);
                    }
                } else {
                    int excessDays = Jdk8Methods.safeToInt(Jdk8Methods.floorDiv(hodVal, 24L));
                    hodVal = Jdk8Methods.floorMod(hodVal, 24);
                    addObject(LocalTime.of((int) hodVal, 0));
                    this.excessDays = Period.ofDays(excessDays);
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
                    ZoneOffset offset = ZoneOffset.ofTotalSeconds(offsetSecs.intValue());
                    mergeInstantFields0(offset);
                }
            }
        }
    }

    private void mergeInstantFields0(ZoneId selectedZone) {
        Instant instant = Instant.ofEpochSecond(fieldValues.remove(INSTANT_SECONDS));
        ChronoZonedDateTime<?> zdt = chrono.zonedDateTime(instant, selectedZone);
        if (date == null) {
            addObject(zdt.toLocalDate());
        } else {
            resolveMakeChanges(INSTANT_SECONDS, zdt.toLocalDate());
        }
        addFieldValue(SECOND_OF_DAY, zdt.toLocalTime().toSecondOfDay());
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

    private void crossCheck(TemporalAccessor temporal) {
        Iterator<Entry<TemporalField, Long>> it = fieldValues.entrySet().iterator();
        while (it.hasNext()) {
            Entry<TemporalField, Long> entry = it.next();
            TemporalField field = entry.getKey();
            long value = entry.getValue();
            if (temporal.isSupported(field)) {
                long temporalValue;
                try {
                    temporalValue = temporal.getLong(field);
                } catch (RuntimeException ex) {
                    continue;
                }
                if (temporalValue != value) {
                    throw new DateTimeException("Cross check failed: "
                            + field + " " + temporalValue + " vs " + field + " " + value);
                }
                it.remove();
            }
        }
    }

    private void resolveFractional() {
        if (time == null
                && (fieldValues.containsKey(INSTANT_SECONDS)
                    || fieldValues.containsKey(SECOND_OF_DAY)
                    || fieldValues.containsKey(SECOND_OF_MINUTE))) {
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
                ZoneOffset offset = ZoneOffset.ofTotalSeconds(offsetSecs.intValue());
                long instant = date.atTime(time).atZone(offset).getLong(ChronoField.INSTANT_SECONDS);
                fieldValues.put(INSTANT_SECONDS, instant);
            }  else if (zone != null) {
                long instant = date.atTime(time).atZone(zone).getLong(ChronoField.INSTANT_SECONDS);
                fieldValues.put(INSTANT_SECONDS, instant);
            }
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Builds the specified type from the values in this builder.
     * <p>
     * This attempts to build the specified type from this builder.
     * If the builder cannot return the type, an exception is thrown.
     *
     * @param <R>  the type to return
     * @param type  the type to invoke {@code from} on, not null
     * @return the extracted value, not null
     * @throws DateTimeException if an error occurs
     */
    public <R> R build(TemporalQuery<R> type) {
        return type.queryFrom(this);
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean isSupported(TemporalField field) {
        if (field == null) {
            return false;
        }
        return fieldValues.containsKey(field)
                || (date != null && date.isSupported(field))
                || (time != null && time.isSupported(field));
    }

    @Override
    public long getLong(TemporalField field) {
        Objects.requireNonNull(field, "field");
        Long value = getFieldValue0(field);
        if (value == null) {
            if (date != null && date.isSupported(field)) {
                return date.getLong(field);
            }
            if (time != null && time.isSupported(field)) {
                return time.getLong(field);
            }
            throw new DateTimeException("Field not found: " + field);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.zoneId()) {
            return (R) zone;
        } else if (query == TemporalQueries.chronology()) {
            return (R) chrono;
        } else if (query == TemporalQueries.localDate()) {
            return date != null ? (R) LocalDate.from(date) : null;
        } else if (query == TemporalQueries.localTime()) {
            return (R) time;
        } else if (query == TemporalQueries.zone() || query == TemporalQueries.offset()) {
            return query.queryFrom(this);
        } else if (query == TemporalQueries.precision()) {
            return null;  // not a complete date/time
        }
        // inline TemporalAccessor.super.query(query) as an optimization
        // non-JDK classes are not permitted to make this optimization
        return query.queryFrom(this);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(128);
        buf.append("DateTimeBuilder[");
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
