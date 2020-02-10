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
package org.teavm.classlib.java.time.zone;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.teavm.classlib.java.util.TObjects;

public final class TTzdbZoneRulesProvider extends TZoneRulesProvider {

    static final TTzdbZoneRulesProvider INSTANCE = new TTzdbZoneRulesProvider();

    private static final String VERSION_ID = "v1";

    private final Map<String, TZoneRules> zoneId2RulesMap;

    private final Set<String> regionIds;

    public TTzdbZoneRulesProvider() {

        super();
        HashMap<String, TZoneRules> map = new HashMap<>();
        TTzdbInternalAfrica.init(map);
        TTzdbInternalAmerica.init(map);
        TTzdbInternalAntarctica.init(map);
        TTzdbInternalAsia.init(map);
        TTzdbInternalAtlantic.init(map);
        TTzdbInternalAustralia.init(map);
        TTzdbInternalCanada.init(map);
        TTzdbInternalEtc.init(map);
        TTzdbInternalEurope.init(map);
        TTzdbInternalIndian.init(map);
        TTzdbInternalMisc.init(map);
        TTzdbInternalPacific.init(map);
        TTzdbInternalSystemV.init(map);
        TTzdbInternalUS.init(map);
        this.zoneId2RulesMap = map;
        this.regionIds = Collections.unmodifiableSet(this.zoneId2RulesMap.keySet());
    }

    @Override
    protected Set<String> provideZoneIds() {

        return this.regionIds;
    }

    @Override
    protected TZoneRules provideRules(String zoneId, boolean forCaching) {

        TObjects.requireNonNull(zoneId, "zoneId");
        TZoneRules rules = this.zoneId2RulesMap.get(zoneId);
        if (rules == null) {
            throw new TZoneRulesException("Unknown time-zone ID: " + zoneId);
        }
        return rules;
    }

    @Override
    protected NavigableMap<String, TZoneRules> provideVersions(String zoneId) {

        TreeMap<String, TZoneRules> map = new TreeMap<>();
        TZoneRules rules = this.zoneId2RulesMap.get(zoneId);
        if (rules == null) {
            throw new TZoneRulesException("Unknown time-zone ID: " + zoneId);
        }
        map.put(VERSION_ID, rules);
        return map;
    }

    @Override
    public String toString() {

        return "TZDB";
    }

}
