/*
 *  Copyright 2020 adopted to TeaVM by Joerg Hohwiller
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
package org.teavm.classlib.java.time.chrono;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;

import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalField;

public abstract class TAbstractChronology implements TChronology {

    private static final Map<String, TChronology> CHRONOS_BY_ID = new HashMap<>();

    private static final Map<String, TChronology> CHRONOS_BY_TYPE = new HashMap<>();

    public static TChronology ofLocale(Locale locale) {

        init();
        Objects.requireNonNull(locale, "locale");
        String type = "iso";
        if (locale.equals(TJapaneseChronology.LOCALE)) {
            type = "japanese";
        }
        if (type == null || "iso".equals(type) || "iso8601".equals(type)) {
            return TIsoChronology.INSTANCE;
        } else {
            TChronology chrono = CHRONOS_BY_TYPE.get(type);
            if (chrono == null) {
                throw new TDateTimeException("Unknown calendar system: " + type);
            }
            return chrono;
        }
    }

    public static TChronology of(String id) {

        init();
        TChronology chrono = CHRONOS_BY_ID.get(id);
        if (chrono != null) {
            return chrono;
        }
        chrono = CHRONOS_BY_TYPE.get(id);
        if (chrono != null) {
            return chrono;
        }
        throw new TDateTimeException("Unknown chronology: " + id);
    }

    public static Set<TChronology> getAvailableChronologies() {

        init();
        return new HashSet<>(CHRONOS_BY_ID.values());
    }

    private static void init() {

        if (CHRONOS_BY_ID.isEmpty()) {
            register(TIsoChronology.INSTANCE);
            register(TThaiBuddhistChronology.INSTANCE);
            register(TMinguoChronology.INSTANCE);
            register(TJapaneseChronology.INSTANCE);
            register(THijrahChronology.INSTANCE);
            CHRONOS_BY_ID.putIfAbsent("Hijrah", THijrahChronology.INSTANCE);
            CHRONOS_BY_TYPE.putIfAbsent("islamic", THijrahChronology.INSTANCE);
            ServiceLoader<TChronology> loader = ServiceLoader.load(TChronology.class,
                    TChronology.class.getClassLoader());
            for (TChronology chrono : loader) {
                CHRONOS_BY_ID.putIfAbsent(chrono.getId(), chrono);
                String type = chrono.getCalendarType();
                if (type != null) {
                    CHRONOS_BY_TYPE.putIfAbsent(type, chrono);
                }
            }
        }
    }

    private static void register(TChronology chrono) {

        CHRONOS_BY_ID.putIfAbsent(chrono.getId(), chrono);
        String type = chrono.getCalendarType();
        if (type != null) {
            CHRONOS_BY_TYPE.putIfAbsent(type, chrono);
        }
    }

    protected TAbstractChronology() {

    }

    <D extends TChronoLocalDate> D ensureChronoLocalDate(TTemporal temporal) {

        @SuppressWarnings("unchecked")
        D other = (D) temporal;
        if (equals(other.getChronology()) == false) {
            throw new ClassCastException(
                    "Chrono mismatch, expected: " + getId() + ", actual: " + other.getChronology().getId());
        }
        return other;
    }

    <D extends TChronoLocalDate> TChronoLocalDateTimeImpl<D> ensureChronoLocalDateTime(TTemporal temporal) {

        @SuppressWarnings("unchecked")
        TChronoLocalDateTimeImpl<D> other = (TChronoLocalDateTimeImpl<D>) temporal;
        if (equals(other.toLocalDate().getChronology()) == false) {
            throw new ClassCastException("Chrono mismatch, required: " + getId() + ", supplied: "
                    + other.toLocalDate().getChronology().getId());
        }
        return other;
    }

    <D extends TChronoLocalDate> TChronoZonedDateTimeImpl<D> ensureChronoZonedDateTime(TTemporal temporal) {

        @SuppressWarnings("unchecked")
        TChronoZonedDateTimeImpl<D> other = (TChronoZonedDateTimeImpl<D>) temporal;
        if (equals(other.toLocalDate().getChronology()) == false) {
            throw new ClassCastException("Chrono mismatch, required: " + getId() + ", supplied: "
                    + other.toLocalDate().getChronology().getId());
        }
        return other;
    }

    void updateResolveMap(Map<TTemporalField, Long> fieldValues, TChronoField field, long value) {

        Long current = fieldValues.get(field);
        if (current != null && current.longValue() != value) {
            throw new TDateTimeException(
                    "Invalid state, field: " + field + " " + current + " conflicts with " + field + " " + value);
        }
        fieldValues.put(field, value);
    }

    @Override
    public int compareTo(TChronology other) {

        return getId().compareTo(other.getId());
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj instanceof TChronology) {
            return compareTo((TChronology) obj) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {

        return getClass().hashCode() ^ getId().hashCode();
    }

    @Override
    public String toString() {

        return getId();
    }

}
