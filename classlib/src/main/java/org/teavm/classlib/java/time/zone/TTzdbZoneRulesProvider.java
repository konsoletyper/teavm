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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.teavm.classlib.java.time.jdk8.TJdk8Methods;

public final class TTzdbZoneRulesProvider extends TZoneRulesProvider {
    // TODO: can this be private/hidden in any way?
    // service loader seems to need it to be public

    private List<String> regionIds;
    private final ConcurrentNavigableMap<String, Version> versions = new ConcurrentSkipListMap<String, Version>();
    private Set<String> loadedUrls = new CopyOnWriteArraySet<String>();

    public TTzdbZoneRulesProvider() {
        super();
        if (load(TZoneRulesProvider.class.getClassLoader()) == false) {
            throw new TZoneRulesException("No time-zone rules found for 'TZDB'");
        }
    }

    public TTzdbZoneRulesProvider(URL url) {
        super();
        try {
            if (load(url) == false) {
                throw new TZoneRulesException("No time-zone rules found: " + url);
            }
        } catch (Exception ex) {
            throw new TZoneRulesException("Unable to load TZDB time-zone rules: " + url, ex);
        }
    }

    public TTzdbZoneRulesProvider(InputStream stream) {
        super();
        try {
            load(stream);
        } catch (Exception ex) {
            throw new TZoneRulesException("Unable to load TZDB time-zone rules", ex);
        }
    }

    //-----------------------------------------------------------------------
    @Override
    protected Set<String> provideZoneIds() {
        return new HashSet<String>(regionIds);
    }

    @Override
    protected TZoneRules provideRules(String zoneId, boolean forCaching) {
        TJdk8Methods.requireNonNull(zoneId, "zoneId");
        TZoneRules rules = versions.lastEntry().getValue().getRules(zoneId);
        if (rules == null) {
            throw new TZoneRulesException("Unknown time-zone ID: " + zoneId);
        }
        return rules;
    }

    @Override
    protected NavigableMap<String, TZoneRules> provideVersions(String zoneId) {
        TreeMap<String, TZoneRules> map = new TreeMap<String, TZoneRules>();
        for (Version version : versions.values()) {
            TZoneRules rules = version.getRules(zoneId);
            if (rules != null) {
                map.put(version.versionId, rules);
            }
        }
        return map;
    }

    //-------------------------------------------------------------------------
    private boolean load(ClassLoader classLoader) {
        boolean updated = false;
        URL url = null;
        try {
            Enumeration<URL> en = classLoader.getResources("org/threeten/bp/TZDB.dat");
            while (en.hasMoreElements()) {
                url = en.nextElement();
                updated |= load(url);
            }
        } catch (Exception ex) {
            throw new TZoneRulesException("Unable to load TZDB time-zone rules: " + url, ex);
        }
        return updated;
    }

    private boolean load(URL url) throws ClassNotFoundException, IOException, TZoneRulesException {
        boolean updated = false;
        if (loadedUrls.add(url.toExternalForm())) {
            InputStream in = null;
            try {
                in = url.openStream();
                updated |= load(in);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }
        return updated;
    }

    private boolean load(InputStream in) throws IOException, StreamCorruptedException {
        boolean updated = false;
        Iterable<Version> loadedVersions = loadData(in);
        for (Version loadedVersion : loadedVersions) {
            // see https://github.com/ThreeTen/threetenbp/pull/28 for issue wrt
            // multiple versions of lib on classpath
            Version existing = versions.putIfAbsent(loadedVersion.versionId, loadedVersion);
            if (existing != null && !existing.versionId.equals(loadedVersion.versionId)) {
                throw new TZoneRulesException("Data already loaded for TZDB time-zone rules version: " + loadedVersion.versionId);
            }
            updated = true;
        }
        return updated;
    }

    private Iterable<Version> loadData(InputStream in) throws IOException, StreamCorruptedException {
        DataInputStream dis = new DataInputStream(in);
        if (dis.readByte() != 1) {
            throw new StreamCorruptedException("File format not recognised");
        }
        // group
        String groupId = dis.readUTF();
        if ("TZDB".equals(groupId) == false) {
            throw new StreamCorruptedException("File format not recognised");
        }
        // versions
        int versionCount = dis.readShort();
        String[] versionArray = new String[versionCount];
        for (int i = 0; i < versionCount; i++) {
            versionArray[i] = dis.readUTF();
        }
        // regions
        int regionCount = dis.readShort();
        String[] regionArray = new String[regionCount];
        for (int i = 0; i < regionCount; i++) {
            regionArray[i] = dis.readUTF();
        }
        regionIds = Arrays.asList(regionArray);
        // rules
        int ruleCount = dis.readShort();
        Object[] ruleArray = new Object[ruleCount];
        for (int i = 0; i < ruleCount; i++) {
            byte[] bytes = new byte[dis.readShort()];
            dis.readFully(bytes);
            ruleArray[i] = bytes;
        }
        AtomicReferenceArray<Object> ruleData = new AtomicReferenceArray<Object>(ruleArray);
        // link version-region-rules
        Set<Version> versionSet = new HashSet<Version>(versionCount);
        for (int i = 0; i < versionCount; i++) {
            int versionRegionCount = dis.readShort();
            String[] versionRegionArray = new String[versionRegionCount];
            short[] versionRulesArray = new short[versionRegionCount];
            for (int j = 0; j < versionRegionCount; j++) {
                versionRegionArray[j] = regionArray[dis.readShort()];
                versionRulesArray[j] = dis.readShort();
            }
            versionSet.add(new Version(versionArray[i], versionRegionArray, versionRulesArray, ruleData));
        }
        return versionSet;
    }

    @Override
    public String toString() {
        return "TZDB";
    }

    //-----------------------------------------------------------------------
    static class Version {
        private final String versionId;
        private final String[] regionArray;
        private final short[] ruleIndices;
        private final AtomicReferenceArray<Object> ruleData;

        Version(String versionId, String[] regionIds, short[] ruleIndices, AtomicReferenceArray<Object> ruleData) {
            this.ruleData = ruleData;
            this.versionId = versionId;
            this.regionArray = regionIds;
            this.ruleIndices = ruleIndices;
        }

        TZoneRules getRules(String regionId) {
            int regionIndex = Arrays.binarySearch(regionArray, regionId);
            if (regionIndex < 0) {
                return null;
            }
            try {
                return createRule(ruleIndices[regionIndex]);
            } catch (Exception ex) {
                throw new TZoneRulesException("Invalid binary time-zone data: TZDB:" + regionId + ", version: " + versionId, ex);
            }
        }

        TZoneRules createRule(short index) throws Exception {
            Object obj = ruleData.get(index);
            if (obj instanceof byte[]) {
                byte[] bytes = (byte[]) obj;
                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
                obj = Ser.read(dis);
                ruleData.set(index, obj);
            }
            return (TZoneRules) obj;
        }

        @Override
        public String toString() {
            return versionId;
        }
    }

}
