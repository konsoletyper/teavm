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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.regex.Pattern;

import org.teavm.classlib.java.time.jdk8.TJdk8Methods;
import org.teavm.classlib.java.time.zone.TZoneRules;
import org.teavm.classlib.java.time.zone.TZoneRulesException;
import org.teavm.classlib.java.time.zone.TZoneRulesProvider;

final class TZoneRegion extends TZoneId implements Serializable {

    private static final long serialVersionUID = 8386373296231747096L;
    private static final Pattern PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9~/._+-]+");

    private final String id;
    private final transient TZoneRules rules;

    private static TZoneRegion ofLenient(String zoneId) {
        if (zoneId.equals("Z") || zoneId.startsWith("+") || zoneId.startsWith("-")) {
            throw new TDateTimeException("Invalid ID for region-based TZoneId, invalid format: " + zoneId);
        }
        if (zoneId.equals("UTC") || zoneId.equals("GMT") || zoneId.equals("UT")) {
            return new TZoneRegion(zoneId, TZoneOffset.UTC.getRules());
        }
        if (zoneId.startsWith("UTC+") || zoneId.startsWith("GMT+") ||
                zoneId.startsWith("UTC-") || zoneId.startsWith("GMT-")) {
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
        return ofId(zoneId, false);
    }

    static TZoneRegion ofId(String zoneId, boolean checkAvailable) {
        TJdk8Methods.requireNonNull(zoneId, "zoneId");
        if (zoneId.length() < 2 || PATTERN.matcher(zoneId).matches() == false) {
            throw new TDateTimeException("Invalid ID for region-based TZoneId, invalid format: " + zoneId);
        }
        TZoneRules rules = null;
        try {
            // always attempt load for better behavior after deserialization
            rules = TZoneRulesProvider.getRules(zoneId, true);
        } catch (TZoneRulesException ex) {
            // special case as removed from data file
            if (zoneId.equals("GMT0")) {
                rules = TZoneOffset.UTC.getRules();
            } else if (checkAvailable) {
                throw ex;
            }
        }
        return new TZoneRegion(zoneId, rules);
    }

    //-------------------------------------------------------------------------
    TZoneRegion(String id, TZoneRules rules) {
        this.id = id;
        this.rules = rules;
    }

    //-----------------------------------------------------------------------
    @Override
    public String getId() {
        return id;
    }

    @Override
    public TZoneRules getRules() {
        // additional query for group provider when null allows for possibility
        // that the provider was added after the TZoneId was created
        return (rules != null ? rules : TZoneRulesProvider.getRules(id, false));
    }

    //-----------------------------------------------------------------------
    private Object writeReplace() {
        return new Ser(Ser.ZONE_REGION_TYPE, this);
    }

    private Object readResolve() throws ObjectStreamException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    @Override
    void write(DataOutput out) throws IOException {
        out.writeByte(Ser.ZONE_REGION_TYPE);
        writeExternal(out);
    }

    void writeExternal(DataOutput out) throws IOException {
        out.writeUTF(id);
    }

    static TZoneId readExternal(DataInput in) throws IOException {
        String id = in.readUTF();
        return ofLenient(id);
    }

}
