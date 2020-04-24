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
package org.threeten.bp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.regex.Pattern;

import org.threeten.bp.jdk8.Jdk8Methods;
import org.threeten.bp.zone.ZoneRules;
import org.threeten.bp.zone.ZoneRulesException;
import org.threeten.bp.zone.ZoneRulesProvider;

/**
 * A geographical region where the same time-zone rules apply.
 * <p>
 * Time-zone information is categorized as a set of rules defining when and
 * how the offset from UTC/Greenwich changes. These rules are accessed using
 * identifiers based on geographical regions, such as countries or states.
 * The most common region classification is the Time Zone Database (TZDB),
 * which defines regions such as 'Europe/Paris' and 'Asia/Tokyo'.
 * <p>
 * The region identifier, modeled by this class, is distinct from the
 * underlying rules, modeled by {@link ZoneRules}.
 * The rules are defined by governments and change frequently.
 * By contrast, the region identifier is well-defined and long-lived.
 * This separation also allows rules to be shared between regions if appropriate.
 *
 * <h3>Specification for implementors</h3>
 * This class is immutable and thread-safe.
 */
final class ZoneRegion extends ZoneId implements Serializable {

    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 8386373296231747096L;
    /**
     * The regex pattern for region IDs.
     */
    private static final Pattern PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9~/._+-]+");

    /**
     * The time-zone ID, not null.
     */
    private final String id;
    /**
     * The time-zone rules, null if zone ID was loaded leniently.
     */
    private final transient ZoneRules rules;

    /**
     * Obtains an instance of {@code ZoneRegion} from an identifier without checking
     * if the time-zone has available rules.
     * <p>
     * This method parses the ID and applies any appropriate normalization.
     * It does not validate the ID against the known set of IDsfor which rules are available.
     * <p>
     * This method is intended for advanced use cases.
     * For example, consider a system that always retrieves time-zone rules from a remote server.
     * Using this factory would allow a {@code ZoneRegion}, and thus a {@code ZonedDateTime},
     * to be created without loading the rules from the remote server.
     *
     * @param zoneId  the time-zone ID, not null
     * @return the zone ID, not null
     * @throws DateTimeException if the ID format is invalid
     */
    private static ZoneRegion ofLenient(String zoneId) {
        if (zoneId.equals("Z") || zoneId.startsWith("+") || zoneId.startsWith("-")) {
            throw new DateTimeException("Invalid ID for region-based ZoneId, invalid format: " + zoneId);
        }
        if (zoneId.equals("UTC") || zoneId.equals("GMT") || zoneId.equals("UT")) {
            return new ZoneRegion(zoneId, ZoneOffset.UTC.getRules());
        }
        if (zoneId.startsWith("UTC+") || zoneId.startsWith("GMT+") ||
                zoneId.startsWith("UTC-") || zoneId.startsWith("GMT-")) {
            ZoneOffset offset = ZoneOffset.of(zoneId.substring(3));
            if (offset.getTotalSeconds() == 0) {
                return new ZoneRegion(zoneId.substring(0, 3), offset.getRules());
            }
            return new ZoneRegion(zoneId.substring(0, 3) + offset.getId(), offset.getRules());
        }
        if (zoneId.startsWith("UT+") || zoneId.startsWith("UT-")) {
            ZoneOffset offset = ZoneOffset.of(zoneId.substring(2));
            if (offset.getTotalSeconds() == 0) {
                return new ZoneRegion("UT", offset.getRules());
            }
            return new ZoneRegion("UT" + offset.getId(), offset.getRules());
        }
        return ofId(zoneId, false);
    }

    /**
     * Obtains an instance of {@code ZoneId} from an identifier.
     *
     * @param zoneId  the time-zone ID, not null
     * @param checkAvailable  whether to check if the zone ID is available
     * @return the zone ID, not null
     * @throws DateTimeException if the ID format is invalid
     * @throws DateTimeException if checking availability and the ID cannot be found
     */
    static ZoneRegion ofId(String zoneId, boolean checkAvailable) {
        Jdk8Methods.requireNonNull(zoneId, "zoneId");
        if (zoneId.length() < 2 || PATTERN.matcher(zoneId).matches() == false) {
            throw new DateTimeException("Invalid ID for region-based ZoneId, invalid format: " + zoneId);
        }
        ZoneRules rules = null;
        try {
            // always attempt load for better behavior after deserialization
            rules = ZoneRulesProvider.getRules(zoneId, true);
        } catch (ZoneRulesException ex) {
            // special case as removed from data file
            if (zoneId.equals("GMT0")) {
                rules = ZoneOffset.UTC.getRules();
            } else if (checkAvailable) {
                throw ex;
            }
        }
        return new ZoneRegion(zoneId, rules);
    }

    //-------------------------------------------------------------------------
    /**
     * Constructor.
     *
     * @param id  the time-zone ID, not null
     * @param rules  the rules, null for lazy lookup
     */
    ZoneRegion(String id, ZoneRules rules) {
        this.id = id;
        this.rules = rules;
    }

    //-----------------------------------------------------------------------
    @Override
    public String getId() {
        return id;
    }

    @Override
    public ZoneRules getRules() {
        // additional query for group provider when null allows for possibility
        // that the provider was added after the ZoneId was created
        return (rules != null ? rules : ZoneRulesProvider.getRules(id, false));
    }

    //-----------------------------------------------------------------------
    private Object writeReplace() {
        return new Ser(Ser.ZONE_REGION_TYPE, this);
    }

    /**
     * Defend against malicious streams.
     * @return never
     * @throws InvalidObjectException always
     */
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

    static ZoneId readExternal(DataInput in) throws IOException {
        String id = in.readUTF();
        return ofLenient(id);
    }

}
