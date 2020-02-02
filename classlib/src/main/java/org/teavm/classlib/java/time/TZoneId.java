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
package org.teavm.classlib.java.time;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder;
import org.teavm.classlib.java.time.format.TTextStyle;
import org.teavm.classlib.java.time.jdk8.TDefaultInterfaceTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.zone.TZoneRules;
import org.teavm.classlib.java.time.zone.TZoneRulesException;
import org.teavm.classlib.java.time.zone.TZoneRulesProvider;

public abstract class TZoneId implements TSerializable {

    public static final TTemporalQuery<TZoneId> FROM = new TTemporalQuery<TZoneId>() {
        @Override
        public TZoneId queryFrom(TTemporalAccessor temporal) {

            return TZoneId.from(temporal);
        }
    };

    public static final Map<String, String> SHORT_IDS;
    static {
        Map<String, String> base = new HashMap<String, String>();
        base.put("ACT", "Australia/Darwin");
        base.put("AET", "Australia/Sydney");
        base.put("AGT", "America/Argentina/Buenos_Aires");
        base.put("ART", "Africa/Cairo");
        base.put("AST", "America/Anchorage");
        base.put("BET", "America/Sao_Paulo");
        base.put("BST", "Asia/Dhaka");
        base.put("CAT", "Africa/Harare");
        base.put("CNT", "America/St_Johns");
        base.put("CST", "America/Chicago");
        base.put("CTT", "Asia/Shanghai");
        base.put("EAT", "Africa/Addis_Ababa");
        base.put("ECT", "Europe/Paris");
        base.put("IET", "America/Indiana/Indianapolis");
        base.put("IST", "Asia/Kolkata");
        base.put("JST", "Asia/Tokyo");
        base.put("MIT", "Pacific/Apia");
        base.put("NET", "Asia/Yerevan");
        base.put("NST", "Pacific/Auckland");
        base.put("PLT", "Asia/Karachi");
        base.put("PNT", "America/Phoenix");
        base.put("PRT", "America/Puerto_Rico");
        base.put("PST", "America/Los_Angeles");
        base.put("SST", "Pacific/Guadalcanal");
        base.put("VST", "Asia/Ho_Chi_Minh");
        base.put("EST", "-05:00");
        base.put("MST", "-07:00");
        base.put("HST", "-10:00");
        SHORT_IDS = Collections.unmodifiableMap(base);
    }

    public static TZoneId systemDefault() {

        return TZoneId.of(TimeZone.getDefault().getID(), SHORT_IDS);
    }

    public static Set<String> getAvailableZoneIds() {

        return new HashSet<>(TZoneRulesProvider.getAvailableZoneIds());
    }

    public static TZoneId of(String zoneId, Map<String, String> aliasMap) {

        Objects.requireNonNull(zoneId, "zoneId");
        Objects.requireNonNull(aliasMap, "aliasMap");
        String id = aliasMap.get(zoneId);
        id = (id != null ? id : zoneId);
        return of(id);
    }

    public static TZoneId of(String zoneId) {

        Objects.requireNonNull(zoneId, "zoneId");
        if (zoneId.equals("Z")) {
            return TZoneOffset.UTC;
        }
        if (zoneId.length() == 1) {
            throw new TDateTimeException("Invalid zone: " + zoneId);
        }
        if (zoneId.startsWith("+") || zoneId.startsWith("-")) {
            return TZoneOffset.of(zoneId);
        }
        if (zoneId.equals("UTC") || zoneId.equals("GMT") || zoneId.equals("UT")) {
            return new TZoneRegion(zoneId, TZoneOffset.UTC.getRules());
        }
        if (zoneId.startsWith("UTC+") || zoneId.startsWith("GMT+") || zoneId.startsWith("UTC-")
                || zoneId.startsWith("GMT-")) {
            TZoneOffset offset = TZoneOffset.of(zoneId.substring(3));
            if (offset.getTotalSeconds() == 0) {
                return new TZoneRegion(zoneId.substring(0, 3), offset.getRules());
            }
            return new TZoneRegion(zoneId.substring(0, 3) + offset.getId(), offset.getRules());
        }
        if (zoneId.startsWith("UT+") || zoneId.startsWith("UT-")) {
            TZoneOffset offset = TZoneOffset.of(zoneId.substring(2));
            if (offset.getTotalSeconds() == 0) {
                return new TZoneRegion("UT", offset.getRules());
            }
            return new TZoneRegion("UT" + offset.getId(), offset.getRules());
        }
        return TZoneRegion.ofId(zoneId, true);
    }

    public static TZoneId ofOffset(String prefix, TZoneOffset offset) {

        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(offset, "offset");
        if (prefix.length() == 0) {
            return offset;
        }
        if (prefix.equals("GMT") || prefix.equals("UTC") || prefix.equals("UT")) {
            if (offset.getTotalSeconds() == 0) {
                return new TZoneRegion(prefix, offset.getRules());
            }
            return new TZoneRegion(prefix + offset.getId(), offset.getRules());
        }
        throw new IllegalArgumentException("Invalid prefix, must be GMT, UTC or UT: " + prefix);
    }

    public static TZoneId from(TTemporalAccessor temporal) {

        TZoneId obj = temporal.query(TTemporalQueries.zone());
        if (obj == null) {
            throw new TDateTimeException("Unable to obtain TZoneId from TTemporalAccessor: " + temporal + ", type "
                    + temporal.getClass().getName());
        }
        return obj;
    }

    TZoneId() {

        if (getClass() != TZoneOffset.class && getClass() != TZoneRegion.class) {
            throw new AssertionError("Invalid subclass");
        }
    }

    public abstract String getId();

    public abstract TZoneRules getRules();

    public String getDisplayName(TTextStyle style, Locale locale) {

        return new TDateTimeFormatterBuilder().appendZoneText(style).toFormatter(locale)
                .format(new TDefaultInterfaceTemporalAccessor() {
                    @Override
                    public boolean isSupported(TTemporalField field) {

                        return false;
                    }

                    @Override
                    public long getLong(TTemporalField field) {

                        throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public <R> R query(TTemporalQuery<R> query) {

                        if (query == TTemporalQueries.zoneId()) {
                            return (R) TZoneId.this;
                        }
                        return super.query(query);
                    }
                });
    }

    public TZoneId normalized() {

        try {
            TZoneRules rules = getRules();
            if (rules.isFixedOffset()) {
                return rules.getOffset(TInstant.EPOCH);
            }
        } catch (TZoneRulesException ex) {
            // ignore invalid objects
        }
        return this;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj instanceof TZoneId) {
            TZoneId other = (TZoneId) obj;
            return getId().equals(other.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {

        return getId().hashCode();
    }

    @Override
    public String toString() {

        return getId();
    }

}
