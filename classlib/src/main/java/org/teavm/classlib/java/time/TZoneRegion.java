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
package org.teavm.classlib.java.time;

import java.util.Objects;

import org.teavm.classlib.impl.tz.DateTimeZone;
import org.teavm.classlib.impl.tz.DateTimeZoneProvider;
import org.teavm.classlib.java.time.zone.TZoneRules;
import org.teavm.classlib.java.time.zone.TZoneRulesProvider;

public final class TZoneRegion extends TZoneId {

    private final String id;

    private final transient TZoneRules rules;

    private static TZoneRegion ofLenient(String zoneId) {

        if (zoneId.equals("Z") || zoneId.startsWith("+") || zoneId.startsWith("-")) {
            throw new TDateTimeException("Invalid ID for region-based TZoneId, invalid format: " + zoneId);
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
        return ofId(zoneId, false);
    }

    static TZoneRegion ofId(String zoneId, boolean checkAvailable) {

        Objects.requireNonNull(zoneId, "zoneId");
        if (zoneId.length() < 2) {
            throw new TDateTimeException("Invalid ID for region-based TZoneId, invalid format: " + zoneId);
        }
        DateTimeZone timeZone = DateTimeZoneProvider.getTimeZone(zoneId);
        if (timeZone == null) {
            throw new IllegalArgumentException(zoneId);
        }
        TZoneId result = timeZone.getZoneId();
        if (result instanceof TZoneRegion) {
            return (TZoneRegion) result;
        }
        return new TZoneRegion(zoneId, result.getRules());
    }

    public TZoneRegion(String id, TZoneRules rules) {

        this.id = id;
        this.rules = rules;
    }

    @Override
    public String getId() {

        return this.id;
    }

    @Override
    public TZoneRules getRules() {

        // additional query for group provider when null allows for possibility
        // that the provider was added after the TZoneId was created
        return (this.rules != null ? this.rules : TZoneRulesProvider.getRules(this.id, false));
    }

}
