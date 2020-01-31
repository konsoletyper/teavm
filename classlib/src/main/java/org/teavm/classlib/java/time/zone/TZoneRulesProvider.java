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
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.teavm.classlib.java.time.TDateTimeException;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZonedDateTime;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;

public abstract class TZoneRulesProvider {

    private static final CopyOnWriteArrayList<TZoneRulesProvider> PROVIDERS = new CopyOnWriteArrayList<TZoneRulesProvider>();
    private static final ConcurrentMap<String, TZoneRulesProvider> ZONES = new ConcurrentHashMap<String, TZoneRulesProvider>(512, 0.75f, 2);
    static {
        TZoneRulesInitializer.initialize();
    }

    //-------------------------------------------------------------------------
    public static Set<String> getAvailableZoneIds() {
        return Collections.unmodifiableSet(ZONES.keySet());
    }

    public static TZoneRules getRules(String zoneId, boolean forCaching) {
        TJdk8Methods.requireNonNull(zoneId, "zoneId");
        return getProvider(zoneId).provideRules(zoneId, forCaching);
    }

    public static NavigableMap<String, TZoneRules> getVersions(String zoneId) {
        TJdk8Methods.requireNonNull(zoneId, "zoneId");
        return getProvider(zoneId).provideVersions(zoneId);
    }

    private static TZoneRulesProvider getProvider(String zoneId) {
        TZoneRulesProvider provider = ZONES.get(zoneId);
        if (provider == null) {
            if (ZONES.isEmpty()) {
                throw new TZoneRulesException("No time-zone data files registered");
            }
            throw new TZoneRulesException("Unknown time-zone ID: " + zoneId);
        }
        return provider;
    }

    //-------------------------------------------------------------------------
    public static void registerProvider(TZoneRulesProvider provider) {
        TJdk8Methods.requireNonNull(provider, "provider");
        registerProvider0(provider);
        PROVIDERS.add(provider);
    }

    private static void registerProvider0(TZoneRulesProvider provider) {
        for (String zoneId : provider.provideZoneIds()) {
            TJdk8Methods.requireNonNull(zoneId, "zoneId");
            TZoneRulesProvider old = ZONES.putIfAbsent(zoneId, provider);
            if (old != null) {
                throw new TZoneRulesException(
                    "Unable to register zone as one already registered with that ID: " + zoneId +
                    ", currently loading from provider: " + provider);
            }
        }
    }

    //-------------------------------------------------------------------------
    public static boolean refresh() {
        boolean changed = false;
        for (TZoneRulesProvider provider : PROVIDERS) {
            changed |= provider.provideRefresh();
        }
        return changed;
    }

    //-----------------------------------------------------------------------
    protected TZoneRulesProvider() {
    }

    //-----------------------------------------------------------------------
    protected abstract Set<String> provideZoneIds();

    protected abstract TZoneRules provideRules(String regionId, boolean forCaching);

    protected abstract NavigableMap<String, TZoneRules> provideVersions(String zoneId);

    protected boolean provideRefresh() {
        return false;
    }

}
