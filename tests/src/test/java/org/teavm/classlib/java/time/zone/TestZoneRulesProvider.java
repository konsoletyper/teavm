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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Test;
import org.teavm.classlib.java.time.TZoneOffset;

public class TestZoneRulesProvider {

    @Test
    public void test_getAvailableGroupIds() {

        Set<String> zoneIds = TZoneRulesProvider.getAvailableZoneIds();
        assertEquals(zoneIds.contains("Europe/London"), true);
        try {
            zoneIds.clear();
            fail();
        } catch (UnsupportedOperationException ex) {
            // ignore
        }
        Set<String> zoneIds2 = TZoneRulesProvider.getAvailableZoneIds();
        assertEquals(zoneIds2.contains("Europe/London"), true);
    }

    @Test
    public void test_getRules_String() {

        TZoneRules rules = TZoneRulesProvider.getRules("Europe/London", false);
        assertNotNull(rules);
        TZoneRules rules2 = TZoneRulesProvider.getRules("Europe/London", false);
        assertEquals(rules2, rules);
    }

    @Test(expected = TZoneRulesException.class)
    public void test_getRules_String_unknownId() {

        TZoneRulesProvider.getRules("Europe/Lon", false);
    }

    @Test(expected = NullPointerException.class)
    public void test_getRules_String_null() {

        TZoneRulesProvider.getRules(null, false);
    }

    @Test
    public void test_getVersions_String() {

        NavigableMap<String, TZoneRules> versions = TZoneRulesProvider.getVersions("Europe/London");
        assertTrue(versions.size() >= 1);
        TZoneRules rules = TZoneRulesProvider.getRules("Europe/London", false);
        assertEquals(versions.lastEntry().getValue(), rules);

        NavigableMap<String, TZoneRules> copy = new TreeMap<>(versions);
        versions.clear();
        assertEquals(versions.size(), 0);
        NavigableMap<String, TZoneRules> versions2 = TZoneRulesProvider.getVersions("Europe/London");
        assertEquals(versions2, copy);
    }

    @Test(expected = TZoneRulesException.class)
    public void test_getVersions_String_unknownId() {

        TZoneRulesProvider.getVersions("Europe/Lon");
    }

    @Test(expected = NullPointerException.class)
    public void test_getVersions_String_null() {

        TZoneRulesProvider.getVersions(null);
    }

    @Test
    public void test_refresh() {

        assertEquals(TZoneRulesProvider.refresh(), false);
    }

    // @Test
    // public void test_registerProvider() {
    //
    // Set<String> pre = TZoneRulesProvider.getAvailableZoneIds();
    // assertEquals(pre.contains("FooLocation"), false);
    // TZoneRulesProvider.registerProvider(new MockTempProvider());
    // Set<String> post = TZoneRulesProvider.getAvailableZoneIds();
    // assertEquals(post.contains("FooLocation"), true);
    //
    // assertEquals(TZoneRulesProvider.getRules("FooLocation", false), TZoneOffset.of("+01:45").getRules());
    // }

    static class MockTempProvider extends TZoneRulesProvider {
        final TZoneRules rules = TZoneOffset.of("+01:45").getRules();

        @Override
        public Set<String> provideZoneIds() {

            return new HashSet<>(Collections.singleton("FooLocation"));
        }

        @Override
        protected NavigableMap<String, TZoneRules> provideVersions(String zoneId) {

            NavigableMap<String, TZoneRules> result = new TreeMap<>();
            result.put("BarVersion", this.rules);
            return result;
        }

        @Override
        protected TZoneRules provideRules(String zoneId, boolean forCaching) {

            if (zoneId.equals("FooLocation")) {
                return this.rules;
            }
            throw new TZoneRulesException("Invalid");
        }
    }

}
