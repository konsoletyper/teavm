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

import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;

import org.teavm.classlib.java.util.TObjects;

public abstract class TZoneRulesProvider {

    public static Set<String> getAvailableZoneIds() {

        return TTzdbZoneRulesProvider.INSTANCE.provideZoneIds();
    }

    public static TZoneRules getRules(String zoneId, boolean forCaching) {

        Objects.requireNonNull(zoneId, "zoneId");
        return getProvider(zoneId).provideRules(zoneId, forCaching);
    }

    public static NavigableMap<String, TZoneRules> getVersions(String zoneId) {

        TObjects.requireNonNull(zoneId, "zoneId");
        return getProvider(zoneId).provideVersions(zoneId);
    }

    private static TZoneRulesProvider getProvider(String zoneId) {

        return TTzdbZoneRulesProvider.INSTANCE;
    }

    public static void registerProvider(TZoneRulesProvider provider) {

        throw new UnsupportedOperationException();
    }

    public static boolean refresh() {

        return false;
    }

    protected TZoneRulesProvider() {

    }

    protected abstract Set<String> provideZoneIds();

    protected abstract TZoneRules provideRules(String regionId, boolean forCaching);

    protected abstract NavigableMap<String, TZoneRules> provideVersions(String zoneId);

    protected boolean provideRefresh() {

        return false;
    }

}
